package com.livingvillages.regions.client;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.naming.SettlementName;
import com.livingvillages.regions.terrain.TerrainModifier;
import com.livingvillages.regions.terrain.TerrainResolver;
import com.livingvillages.regions.tier.SettlementTier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side settlement name float-text (action bar) display.
 *
 * <p>Every {@value #TICK_THROTTLE} ticks (0.5s) the player's position is checked
 * against the set of known settlement chunk coordinates (received from the
 * server via the settlements packet). When the player is within a settlement's
 * <em>generation footprint</em> (its "legal zone" &mdash; radius
 * {@code maxDistanceFromCenter &times; tier distance factor} from the
 * settlement's chunk centre), the settlement name is shown on the action bar;
 * it clears when the player leaves. The name is
 * {@code <biome prefix> + <terrain modifier> + <tier suffix>} (e.g. "风沙城",
 * "霜白岭城", "暖阳滨镇") and is derived deterministically from the world seed
 * and the settlement's generation chunk.</p>
 *
 * <p>Both the tier (and thus the in-world size) and the name come from the same
 * {@link SettlementTier#tierFor} function the server sizing Mixin uses, so what
 * the player reads matches what was generated &mdash; the server only ships the
 * settlement chunk coordinates, never the tier or name.</p>
 *
 * <h2>Stable, settlement-defined names (no flicker)</h2>
 *
 * <p>A settlement's biome and terrain are <em>attributes of the settlement</em>,
 * sampled once at its centre and cached, rather than re-sampled at the player's
 * moving feet every tick. This keeps the name coherent (the prefix belongs to
 * the village, not to whichever biome the player happens to stand in) and stable
 * (it never flips as chunks stream in after a teleport).</p>
 *
 * <p>Because terrain is read from live world data, the water-scan ring around a
 * freshly teleported-to village takes a second or two to load. The name is
 * therefore resolved only once {@link TerrainResolver#isReady} reports the
 * centre and scan ring loaded, then cached; until then the display stays blank
 * rather than showing a transient un-terrain-aware name that would then flip. A
 * time-out ({@link #RESOLVE_TIMEOUT_TICKS}) forces a best-effort resolve if the
 * area never fully loads (e.g. very small render distance), so a name always
 * appears eventually. After the one resolve, the cached name is reused for the
 * rest of the session.</p>
 *
 * <h2>Why server-pushed coordinates, not a client scan</h2>
 *
 * <p>1.20.1 clients do not reliably expose the structure dynamic registry or
 * complete {@code StructureStart} maps for loaded chunks, so a client-side
 * village scan cannot match village ids. The server's
 * {@code RoadWeaverIntegrator} discovers villages authoritatively and pushes
 * their chunk coordinates here; the client resolves tier/name from the seed.
 * This also makes multiplayer reliable (no best-effort client chunk cache).</p>
 *
 * <h2>World seed injection</h2>
 *
 * <p>Vanilla 1.20.1 {@code ClientLevel} does not expose the world seed. The
 * server sends it once on player join via the world-seed packet, calling
 * {@link #setWorldSeed(long)}. Until the seed arrives the display is
 * suppressed.</p>
 */
public class RegionTitleDisplay implements ClientTickEvents.EndTick {

    private static final Logger LOGGER = LoggerFactory.getLogger("RegionTitleDisplay");

    /** Tick interval between position checks (10 = 0.5s on a 20 tps server). */
    private static final int TICK_THROTTLE = 10;

    /** Vanilla {@code max_distance_from_center} baseline (blocks). */
    private static final int BASE_MAX_DISTANCE = 80;

    /**
     * If a settlement's centre + water-scan ring are not fully loaded within
     * this many ticks of first entering its footprint, force a best-effort
     * resolve so a name appears rather than waiting indefinitely.
     */
    private static final int RESOLVE_TIMEOUT_TICKS = 60;

    /** Sentinel value indicating the world seed has not yet been received. */
    private static final long SEED_UNSET = Long.MIN_VALUE;

    /** Emit diagnostic logs while debugging; false for normal play. */
    private static final boolean DEBUG = false;

    /** Last shown display string; used to suppress duplicates and detect zone entry/exit. */
    private static String lastDisplay = "";

    /** World seed injected by the seed-sync packet; {@link #SEED_UNSET} until then. */
    private static long worldSeed = SEED_UNSET;

    /** Settlement chunk coordinates pushed by the server; written on the net thread, read on render. */
    private static final Set<Long> knownSettlements = ConcurrentHashMap.newKeySet();

    /** Cached resolved name attributes per settlement chunk key; computed once, reused thereafter. */
    private static final Map<Long, ResolvedName> nameCache = new HashMap<>();

    /** Tick at which a settlement first became the nearest (for the resolve time-out). */
    private static final Map<Long, Integer> pendingSince = new HashMap<>();

    /** A detected settlement: its generation chunk (for tier/name) and centre (for proximity/biome). */
    private record VillageHit(ChunkPos genChunk, BlockPos center, SettlementTier tier) {
    }

    /** Resolved, cached name attributes for a settlement. */
    private record ResolvedName(RegionType type, TerrainModifier.Kind terrain, String name) {
    }

    /**
     * Inject the world seed from the server&rarr;client packet handler.
     *
     * @param seed the world seed received from the server
     */
    public static void setWorldSeed(long seed) {
        if (seed != worldSeed) {
            worldSeed = seed;
            lastDisplay = "";
        }
    }

    /**
     * Read the currently injected world seed.
     *
     * @return the world seed, or {@code Long.MIN_VALUE} if not yet set
     */
    public static long getWorldSeed() {
        return worldSeed;
    }

    /** Add a known settlement chunk (from the settlements packet). Thread-safe. */
    public static void addSettlement(int chunkX, int chunkZ) {
        knownSettlements.add(ChunkPos.asLong(chunkX, chunkZ));
    }

    /** Clear all known settlements (on disconnect). */
    public static void clearSettlements() {
        knownSettlements.clear();
        nameCache.clear();
        pendingSince.clear();
        lastDisplay = "";
    }

    /** @return the number of known settlements (for diagnostics) */
    public static int settlementCount() {
        return knownSettlements.size();
    }

    @Override
    public void onEndTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (worldSeed == SEED_UNSET) {
            return;
        }
        if (mc.player.tickCount % TICK_THROTTLE != 0) {
            return;
        }

        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();

        VillageHit hit = findNearestSettlement(blockX, blockZ);
        String displayName;
        if (hit == null) {
            displayName = "";
        } else {
            long key = ChunkPos.asLong(hit.genChunk().x, hit.genChunk().z);
            ResolvedName resolved = resolveName(mc, hit, key);
            displayName = resolved == null ? "" : resolved.name();
        }

        if (DEBUG) {
            if (hit == null) {
                if (!lastDisplay.isEmpty()) {
                    LOGGER.info("[LV] EXIT (cleared, was \"{}\")", lastDisplay);
                }
            } else if (!displayName.isEmpty() && !displayName.equals(lastDisplay)) {
                LOGGER.info("[LV] ENTER name=\"{}\" genChunk=({}, {})",
                    displayName, hit.genChunk().x, hit.genChunk().z);
            }
        }

        if (!displayName.equals(lastDisplay)) {
            if (!displayName.isEmpty()) {
                mc.player.displayClientMessage(Component.literal(displayName), true);
            }
            lastDisplay = displayName;
        }
    }

    /**
     * Return the cached resolved name for a settlement, or resolve+cache it.
     * Resolution samples biome and terrain at the settlement centre and only
     * proceeds once the centre + water-scan ring are loaded (or the time-out
     * fires), so the cached value is the terrain-aware one and never flips.
     *
     * @return the resolved name, or {@code null} if deferred (chunks not yet ready)
     */
    private static ResolvedName resolveName(Minecraft mc, VillageHit hit, long key) {
        ResolvedName cached = nameCache.get(key);
        if (cached != null) {
            return cached;
        }

        int tick = mc.player.tickCount;
        Integer since = pendingSince.get(key);
        if (since == null) {
            since = tick;
            pendingSince.put(key, since);
        }
        boolean ready = TerrainResolver.isReady(mc.level, hit.center());
        boolean timedOut = (tick - since) >= RESOLVE_TIMEOUT_TICKS;
        if (!ready && !timedOut) {
            // Defer: the water-scan ring is still streaming in. Showing now would
            // cache a transient un-terrain-aware name that later flips.
            if (DEBUG && (tick & 0x3) == 0) {
                LOGGER.info("[LV] deferring name for genChunk=({}, {}) (chunks loading, {}t elapsed)",
                    hit.genChunk().x, hit.genChunk().z, tick - since);
            }
            return null;
        }

        // Sample biome AND terrain at the settlement centre (attributes of the
        // settlement, not the player's moving feet) and cache the result.
        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, hit.center().getX(), hit.center().getZ());
        if (type == null) {
            // Centre sits in an excluded biome (cave/water/beach): fall back to the
            // player's own biome so the settlement still gets a name.
            type = BiomeRegionResolver.resolveRegionType(mc.level, mc.player.getBlockX(), mc.player.getBlockZ());
            if (type == null) {
                return null;
            }
        }
        TerrainModifier.Kind terrain = TerrainResolver.resolve(mc.level, hit.center(), type);
        String name = SettlementName.format(worldSeed, hit.genChunk().x, hit.genChunk().z, type, hit.tier(), terrain);
        ResolvedName resolved = new ResolvedName(type, terrain, name);
        nameCache.put(key, resolved);
        pendingSince.remove(key);
        if (DEBUG) {
            LOGGER.info("[LV] resolved name=\"{}\" tier={} type={} terrain={} genChunk=({}, {}){}",
                name, hit.tier(), type, terrain, hit.genChunk().x, hit.genChunk().z,
                timedOut ? " (forced after timeout)" : "");
        }
        return resolved;
    }

    /**
     * Find the nearest known settlement whose footprint contains the player.
     * Footprint radius = {@link #BASE_MAX_DISTANCE} &times; the tier's distance
     * factor; settlement centre is the chunk centre. The closest match wins.
     */
    private static VillageHit findNearestSettlement(int blockX, int blockZ) {
        VillageHit best = null;
        long bestDist2 = Long.MAX_VALUE;
        for (long key : knownSettlements) {
            int cx = (int) (key & 0xFFFFFFFFL);
            int cz = (int) (key >>> 32);
            SettlementTier tier = SettlementTier.tierFor(worldSeed, cx, cz);
            int threshold = BASE_MAX_DISTANCE * tier.distanceFactor();
            int centerX = (cx << 4) + 8;
            int centerZ = (cz << 4) + 8;
            long dx = (long) blockX - centerX;
            long dz = (long) blockZ - centerZ;
            long dist2 = dx * dx + dz * dz;
            long threshold2 = (long) threshold * threshold;
            if (dist2 <= threshold2 && dist2 < bestDist2) {
                bestDist2 = dist2;
                best = new VillageHit(new ChunkPos(cx, cz), new BlockPos(centerX, 64, centerZ), tier);
            }
        }
        if (DEBUG && best == null && (System.nanoTime() & 0x1F) == 0) {
            LOGGER.info("[LV-scan] player=({}, {}) known={} no settlement in footprint",
                blockX, blockZ, knownSettlements.size());
        }
        return best;
    }
}
