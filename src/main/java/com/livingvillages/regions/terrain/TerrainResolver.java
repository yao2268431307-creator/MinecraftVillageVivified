package com.livingvillages.regions.terrain;

import com.livingvillages.regions.biome.RegionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkStatus;

/**
 * Detects the terrain modifier for a settlement.
 *
 * <p>Returns a {@link TerrainModifier.Kind} used in settlement naming:
 * {@link TerrainModifier.Kind#HIGH} for elevated settlements and
 * {@link TerrainModifier.Kind#WATER} for waterside ones. This is the
 * MC-dependent counterpart to the pure {@link TerrainModifier} selector;
 * it needs a {@link Level} and is therefore not unit-tested.</p>
 *
 * <p>v1 keeps detection robust: HIGH is signalled by a
 * {@link RegionType#MOUNTAIN} biome, and WATER by an ocean/river biome
 * within {@link #WATER_SCAN_CHUNKS} of the settlement. Elevation-based
 * HIGH (plateau/hill in non-mountain biomes) can be added later with
 * heightmap sampling.</p>
 *
 * <h2>Readiness</h2>
 *
 * <p>Terrain is sampled from live world data, so right after a teleport the
 * water-scan ring may still be streaming in and {@link #resolve} would
 * transiently return NONE. Callers that want a stable, terrain-aware value
 * should first check {@link #isReady(Level, BlockPos)} &mdash; true once the
 * settlement centre and its entire water-scan ring are loaded &mdash; and
 * only then call {@link #resolve}, caching the result so it is computed
 * exactly once per settlement.</p>
 */
public final class TerrainResolver {

    /** Radius in chunks to scan for nearby ocean/river biomes. */
    private static final int WATER_SCAN_CHUNKS = 6;

    private TerrainResolver() {
        // utility class
    }

    /**
     * Resolve the terrain modifier for a settlement.
     *
     * <p>Only meaningful once {@link #isReady(Level, BlockPos)} returns true;
     * before that, the water-scan ring may be unloaded and the result can
     * transiently read as NONE even when the settlement is waterside.</p>
     *
     * @param level  the level to sample
     * @param center the settlement's bounding-box centre
     * @param type   the settlement's biome region type (from {@link BiomeRegionResolver})
     * @return HIGH, WATER, or NONE
     */
    public static TerrainModifier.Kind resolve(Level level, BlockPos center, RegionType type) {
        if (type == RegionType.MOUNTAIN) {
            return TerrainModifier.Kind.HIGH;
        }
        if (type == null) {
            return TerrainModifier.Kind.NONE;
        }
        return hasNearbyWater(level, center) ? TerrainModifier.Kind.WATER : TerrainModifier.Kind.NONE;
    }

    /**
     * @return {@code true} once the settlement centre chunk and every chunk in
     *         the water-scan ring is loaded at {@link ChunkStatus#FULL}, so a
     *         terrain read will be stable rather than a load-transient.
     */
    public static boolean isReady(Level level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -WATER_SCAN_CHUNKS; dx <= WATER_SCAN_CHUNKS; dx++) {
            for (int dz = -WATER_SCAN_CHUNKS; dz <= WATER_SCAN_CHUNKS; dz++) {
                if (level.getChunk(cx + dx, cz + dz, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasNearbyWater(Level level, BlockPos center) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -WATER_SCAN_CHUNKS; dx <= WATER_SCAN_CHUNKS; dx++) {
            for (int dz = -WATER_SCAN_CHUNKS; dz <= WATER_SCAN_CHUNKS; dz++) {
                Holder<Biome> biome = level.getBiome(new BlockPos((cx + dx) << 4, 64, (cz + dz) << 4));
                if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
