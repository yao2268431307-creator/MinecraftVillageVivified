package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import java.util.List;
import java.util.Random;

/**
 * Deterministic name generator for biome regions and villages.
 *
 * <p>Names are derived from the world seed together with a spatial
 * coordinate (super-cell for regions, chunk for villages) so the same
 * inputs always yield the same name. The seed hash mirrors vanilla
 * Minecraft's chunk-position scrambling constants
 * ({@code 341873128712L} / {@code 132897987541L}) to keep names stable
 * across reloads without needing server-side synchronisation.</p>
 *
 * <p>Output formats (per {@code DESIGN.md}):
 * <ul>
 *   <li>{@code regionName} → {@code <prefix>之<suffix>} (e.g. "风沙之地")</li>
 *   <li>{@code villageName} → {@code <prefix><village suffix>} (e.g. "赤焰村")</li>
 *   <li>{@code fullDisplay} → {@code <region>——<village>} (e.g. "风沙之地——赤焰村")</li>
 * </ul>
 *
 * <p>This class is a pure utility with no MC runtime dependency, so it
 * can be unit-tested directly.</p>
 */
public final class RegionNameGenerator {

    /** Fallback region name when the pool cannot be resolved. */
    private static final String FALLBACK_REGION = "未知之地";
    /** Fallback village name when the pool cannot be resolved. */
    private static final String FALLBACK_VILLAGE = "无名村";
    /** Separator between region and village in the full display string. */
    private static final String FULL_SEPARATOR = "——";

    private RegionNameGenerator() {
        // utility class
    }

    /**
     * Generate a deterministic region name like "风沙之地".
     *
     * <p>Format: {@code prefix + midfix + suffix}.</p>
     *
     * @param worldSeed the world seed
     * @param superX    the super-cell X coordinate
     * @param superZ    the super-cell Z coordinate
     * @param type      the region type (may be {@code null})
     * @return a region name, or {@link #FALLBACK_REGION} if the pool is unavailable
     */
    public static String regionName(long worldSeed, int superX, int superZ, RegionType type) {
        RegionNamePool.RegionWords pool = RegionNamePool.forType(type);
        if (pool == null) {
            return FALLBACK_REGION;
        }
        Random rng = new Random(hashSeed(worldSeed, superX, superZ));
        return pick(rng, pool.prefixes()) + pool.midfix() + pick(rng, pool.suffixes());
    }

    /**
     * Generate a deterministic village name like "赤焰村".
     *
     * <p>Format: {@code prefix + village suffix}.</p>
     *
     * @param worldSeed the world seed
     * @param chunkX    the village chunk X coordinate
     * @param chunkZ    the village chunk Z coordinate
     * @param type      the region type (may be {@code null})
     * @return a village name, or {@link #FALLBACK_VILLAGE} if the pool is unavailable
     */
    public static String villageName(long worldSeed, int chunkX, int chunkZ, RegionType type) {
        RegionNamePool.RegionWords pool = RegionNamePool.forType(type);
        if (pool == null) {
            return FALLBACK_VILLAGE;
        }
        Random rng = new Random(hashSeed(worldSeed, chunkX, chunkZ));
        String prefix = pick(rng, pool.prefixes());
        String suffix = pick(rng, RegionNamePool.VILLAGE_SUFFIXES);
        return prefix + suffix;
    }

    /**
     * Compose the full display string like "风沙之地——赤焰村".
     *
     * @param regionName  the region name (e.g. from {@link #regionName})
     * @param villageName the village name (e.g. from {@link #villageName})
     * @return the combined display string
     */
    public static String fullDisplay(String regionName, String villageName) {
        return regionName + FULL_SEPARATOR + villageName;
    }

    /**
     * Hash the world seed with two spatial coordinates using vanilla's
     * scrambling constants.
     */
    private static long hashSeed(long worldSeed, int x, int z) {
        return worldSeed
            ^ ((long) x * 341873128712L)
            ^ ((long) z * 132897987541L);
    }

    /**
     * Pick a random element from a non-empty list.
     */
    private static <T> T pick(Random rng, List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }
}
