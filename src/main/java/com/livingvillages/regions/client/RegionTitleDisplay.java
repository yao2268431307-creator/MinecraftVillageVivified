package com.livingvillages.regions.client;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.naming.RegionNameGenerator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

/**
 * Client-side region+village name float-text (action bar) display.
 *
 * <p>Every {@value #TICK_THROTTLE} ticks (0.5s) the player's current super-cell
 * is resolved into a {@link RegionType} via {@link BiomeRegionResolver} and the
 * deterministic region name is generated via {@link RegionNameGenerator}.
 * Additionally, nearby loaded chunks are searched for village {@link StructureStart}s;
 * when the player is inside a village bounding box the display shows
 * "&lang;region&rang;——&lang;village&rang;" (e.g. "风沙之地——赤焰村"), otherwise
 * only the region name is shown.</p>
 *
 * <h2>Village detection</h2>
 *
 * <p>A {@value #VILLAGE_SEARCH_CHUNKS}-chunk radius around the player is
 * scanned for village {@code StructureStart}s. Only already-loaded chunks
 * are inspected ({@link ChunkStatus#FULL}, no force-load), so detection
 * is best-effort and only fires once the player is close enough that the
 * chunk has been synced from the server. Village names are derived
 * deterministically from the world seed + the village start's chunk
 * coordinates via {@link RegionNameGenerator#villageName}.</p>
 *
 * <h2>World seed injection</h2>
 *
 * <p>Vanilla 1.20.1 {@code ClientLevel} does <em>not</em> expose the world
 * seed. The server sends it once on player join via a custom packet
 * (see {@code SeedSender} / {@code SeedReceiver}), calling
 * {@link #setWorldSeed(long)}. Until the seed arrives, the float-text is
 * suppressed.</p>
 */
public class RegionTitleDisplay implements ClientTickEvents.EndTick {

    /** Super-cell size in blocks (= {@code 7 * 34 * 16}). */
    public static final int SUPER_CELL_SIZE = 7 * 34 * 16;

    /** Tick interval between position checks (10 = 0.5s on a 20 tps server). */
    private static final int TICK_THROTTLE = 10;

    /** Chunk search radius for village detection. */
    private static final int VILLAGE_SEARCH_CHUNKS = 5;

    /** Sentinel value indicating the world seed has not yet been received. */
    private static final long SEED_UNSET = Long.MIN_VALUE;

    /** Last shown display string; used to suppress duplicate float-text. */
    private static String lastDisplay = "";

    /** World seed injected by the seed-sync packet; {@link #SEED_UNSET} until then. */
    private static long worldSeed = SEED_UNSET;

    /**
     * Inject the world seed from the server→client packet handler.
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
        int superX = Math.floorDiv(blockX, SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, SUPER_CELL_SIZE);

        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, blockX, blockZ);
        if (type == null) {
            // Cave / water / beach: no float-text.
            lastDisplay = "";
            return;
        }

        String regionName = RegionNameGenerator.regionName(worldSeed, superX, superZ, type);

        // Try to detect a nearby village for the combined display.
        ChunkPos villageChunk = findNearbyVillageChunk(mc, blockX, blockZ);
        String displayName;
        if (villageChunk != null) {
            String villageName = RegionNameGenerator.villageName(
                worldSeed, villageChunk.x, villageChunk.z, type);
            displayName = RegionNameGenerator.fullDisplay(regionName, villageName);
        } else {
            displayName = regionName;
        }

        if (!displayName.equals(lastDisplay)) {
            mc.player.displayClientMessage(Component.literal(displayName), true);
            lastDisplay = displayName;
        }
    }

    /**
     * Search loaded chunks around the player for a village {@link StructureStart}
     * whose bounding box contains the player's position.
     *
     * <p>Only chunks already at {@link ChunkStatus#FULL} are inspected
     * (no force-load), so detection fires as soon as the client has
     * received the chunk from the server. Village structure keys are
     * matched against the five vanilla village variants.</p>
     *
     * @param mc           the Minecraft client instance
     * @param playerBlockX the player's block X coordinate
     * @param playerBlockZ the player's block Z coordinate
     * @return the {@link ChunkPos} of the village start, or {@code null}
     */
    private static ChunkPos findNearbyVillageChunk(Minecraft mc, int playerBlockX, int playerBlockZ) {
        int playerChunkX = playerBlockX >> 4;
        int playerChunkZ = playerBlockZ >> 4;
        BlockPos playerPos = new BlockPos(playerBlockX, 64, playerBlockZ);

        for (int dx = -VILLAGE_SEARCH_CHUNKS; dx <= VILLAGE_SEARCH_CHUNKS; dx++) {
            for (int dz = -VILLAGE_SEARCH_CHUNKS; dz <= VILLAGE_SEARCH_CHUNKS; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                ChunkAccess chunk = mc.level.getChunkSource()
                    .getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                Map<Structure, StructureStart> starts = chunk.getAllStarts();
                if (starts == null || starts.isEmpty()) {
                    continue;
                }

                Registry<Structure> registry = mc.level.registryAccess()
                    .registry(Registries.STRUCTURE)
                    .orElse(null);
                if (registry == null) {
                    continue;
                }

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
                        StructureStart start = entry.getValue();
                        if (start.getBoundingBox().isInside(playerPos)) {
                            BlockPos center = start.getBoundingBox().getCenter();
                            return new ChunkPos(center.getX() >> 4, center.getZ() >> 4);
                        }
                    }
                }
            }
        }
        return null;
    }
}
