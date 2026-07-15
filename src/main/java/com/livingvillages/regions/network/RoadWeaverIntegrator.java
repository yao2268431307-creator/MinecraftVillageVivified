package com.livingvillages.regions.network;

import com.livingvillages.regions.data.RegionStateStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.shiroha233.roadweaver.api.RoadNetworkApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side RoadWeaver integrator.
 *
 * <p>Every {@value #TICK_THROTTLE} ticks (1s on a 20 tps server) the overworld
 * is scanned around the first online player for village {@code StructureStart}s
 * that have not yet been processed. Each newly discovered village is registered
 * with RoadWeaver as a structure endpoint via
 * {@link RoadNetworkApi#registerStructureEndpoint(ServerLevel, BlockPos, String, boolean)}
 * (with {@code autoConnect = true}) and connected to the previously discovered
 * village via
 * {@link RoadNetworkApi#ensureConnection(ServerLevel, BlockPos, BlockPos)}.
 * The chunk is then recorded as processed in {@link RegionStateStore} so it is
 * not re-registered after a world reload.</p>
 *
 * <h2>Village discovery</h2>
 *
 * <p>{@link #findVillageInChunk(ServerLevel, int, int)} resolves a village
 * {@code StructureStart} by polling already-loaded chunks via
 * {@code getChunkNow} (no force-load, so it is safe to call on every tick),
 * walking {@link ChunkAccess#getAllStarts()}, and matching the
 * {@link Structure}'s registry key against the five vanilla village variants
 * ({@code minecraft:village_plains}, {@code village_desert}, {@code village_savanna},
 * {@code village_snowy}, {@code village_taiga}). The matched start's
 * {@link StructureStart#getBoundingBox()} center is used as the RoadWeaver
 * endpoint position.</p>
 *
 * <p>This class is stateless; all state lives in {@link RegionStateStore}, which
 * is loaded on demand from the overworld's {@code DataStorage}. Throttling is
 * performed by the caller (see {@code RegionsMod}) via
 * {@code server.getTickCount() % TICK_THROTTLE == 0}.</p>
 */
public final class RoadWeaverIntegrator {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    /** Scan radius around the player, in blocks. Covers city footprints (8&times; = 640 blocks). */
    private static final int SCAN_RADIUS_BLOCKS = 768;

    /** Scan radius around the player, in chunks (derived from {@link #SCAN_RADIUS_BLOCKS}). */
    private static final int SCAN_RADIUS_CHUNKS = SCAN_RADIUS_BLOCKS / 16;

    /** Tick interval between scans (20 = 1s on a 20 tps server). */
    public static final int TICK_THROTTLE = 20;

    /** RoadWeaver structure id used for every registered village endpoint. */
    private static final String STRUCTURE_ID = "village";

    private RoadWeaverIntegrator() {
    }

    /**
     * Called from {@code ServerTickEvents.END_SERVER_TICK} once per
     * {@value #TICK_THROTTLE} ticks. Scans the first online player's
     * surrounding chunks for unprocessed villages, registers each with
     * RoadWeaver, connects consecutive new villages, and persists the
     * processed-chunk set.
     *
     * @param server the minecraft server (must not be {@code null})
     */
    public static void onTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer player = players.get(0);

        RegionStateStore store = RegionStateStore.load(overworld);

        int playerChunkX = player.chunkPosition().x;
        int playerChunkZ = player.chunkPosition().z;

        List<BlockPos> newVillages = new ArrayList<>();

        for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                if (store.isProcessed(chunkX, chunkZ)) {
                    continue;
                }

                BlockPos villagePos = findVillageInChunk(overworld, chunkX, chunkZ);
                if (villagePos == null) {
                    continue;
                }

                try {
                    RoadNetworkApi.registerStructureEndpoint(
                        overworld, villagePos, STRUCTURE_ID, true);
                    LOGGER.info("Registered village at {} with RoadWeaver", villagePos);
                } catch (Throwable t) {
                    LOGGER.warn("RoadWeaver registerStructureEndpoint failed: {}",
                        t.getMessage());
                }

                newVillages.add(villagePos);
                store.markProcessed(chunkX, chunkZ);
                // Push this new settlement to all clients so they can name it.
                SettlementsSender.sendOne(server, chunkX, chunkZ);
            }
        }

        // Connect consecutive new villages so a freshly discovered cluster
        // forms a road chain. Cross-chunk connections to older villages are
        // left to RoadWeaver's autoConnect flag on registration.
        for (int i = 1; i < newVillages.size(); i++) {
            BlockPos from = newVillages.get(i - 1);
            BlockPos to = newVillages.get(i);
            try {
                RoadNetworkApi.ensureConnection(overworld, from, to);
            } catch (Throwable t) {
                LOGGER.warn("RoadWeaver ensureConnection failed: {}", t.getMessage());
            }
        }
    }

    /**
     * Locate a village {@link StructureStart} in the given chunk and return its
     * bounding-box center as a {@link BlockPos} for RoadWeaver registration.
     *
     * <p>Only already-loaded chunks are inspected ({@code getChunkNow}, no
     * force-load), so this is safe to call on every tick without stalling the
     * main thread on chunk generation. Village matching uses the five vanilla
     * village structure keys ({@code minecraft:village_plains}, {@code _desert},
     * {@code _savanna}, {@code _snowy}, {@code _taiga}).</p>
     *
     * @param level  the overworld
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the village center {@link BlockPos}, or {@code null} if the chunk
     *         is not loaded or contains no village {@code StructureStart}
     */
    private static BlockPos findVillageInChunk(ServerLevel level, int chunkX, int chunkZ) {
        // 1. Poll the chunk only if already loaded (no force-load).
        ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }

        // 2. Walk all structure starts in this chunk.
        Map<Structure, StructureStart> starts = chunk.getAllStarts();
        if (starts == null || starts.isEmpty()) {
            return null;
        }

        // 3. Resolve the Structure registry so we can match by ResourceLocation.
        Registry<Structure> registry = level.registryAccess()
                .registry(Registries.STRUCTURE)
                .orElse(null);
        if (registry == null) {
            return null;
        }

        // 4. Find the first start whose registry key is a vanilla village variant.
        for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
            ResourceLocation key = registry.getKey(entry.getKey());
            if (key == null) {
                continue;
            }
            String id = key.toString();
            if (id.equals("minecraft:village_plains")
                    || id.equals("minecraft:village_desert")
                    || id.equals("minecraft:village_savanna")
                    || id.equals("minecraft:village_snowy")
                    || id.equals("minecraft:village_taiga")) {
                return entry.getValue().getBoundingBox().getCenter();
            }
        }
        return null;
    }
}
