package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.terrain.TerrainModifier;
import com.livingvillages.regions.tier.SettlementTier;
import java.util.List;
import java.util.Random;

/**
 * Deterministic per-settlement name generator.
 *
 * <p>A settlement name is built as
 * {@code <biome prefix> + <terrain modifier> + <tier suffix>}, e.g.
 * "风沙城" (desert city), "霜白岭城" (snowy mountain city),
 * "暖阳滨镇" (plains waterside town). The biome prefix is drawn from
 * {@link RegionNamePool}, the terrain modifier from
 * {@link TerrainModifier}, and the tier suffix (村/镇/城) from
 * {@link SettlementTier}.</p>
 *
 * <p>Names are derived from the world seed and the village's
 * <em>generation chunk</em> coordinate, so the same village always
 * yields the same name. The hash is shared with
 * {@link SettlementTier} (via {@link SeedHash}) but the tier itself is
 * resolved on the grid cell while the name is hashed on the raw chunk,
 * so tier and name vary independently.</p>
 *
 * <p>Pure utility, no MC runtime dependency — unit-testable directly.</p>
 */
public final class SettlementName {

    /** Fallback name when the biome pool cannot be resolved. */
    private static final String FALLBACK = "无名村";

    private SettlementName() {
        // utility class
    }

    /**
     * Generate a deterministic settlement name.
     *
     * @param worldSeed the world seed
     * @param chunkX    the village generation chunk X
     * @param chunkZ    the village generation chunk Z
     * @param type      the biome region type at the settlement (may be {@code null})
     * @param tier      the settlement tier (may be {@code null}, treated as VILLAGE)
     * @param terrain   the terrain modifier kind (may be {@code null}, treated as NONE)
     * @return a settlement name, or {@link #FALLBACK} if the pool is unavailable
     */
    public static String format(long worldSeed, int chunkX, int chunkZ,
                                 RegionType type, SettlementTier tier,
                                 TerrainModifier.Kind terrain) {
        if (tier == null) {
            tier = SettlementTier.VILLAGE;
        }
        if (terrain == null) {
            terrain = TerrainModifier.Kind.NONE;
        }
        RegionNamePool.RegionWords pool = RegionNamePool.forType(type);
        if (pool == null) {
            return FALLBACK;
        }
        Random rng = new Random(SeedHash.hash(worldSeed, chunkX, chunkZ));
        String prefix = pick(rng, pool.prefixes());
        String modifier = TerrainModifier.pick(terrain, rng);
        return prefix + modifier + tier.suffix();
    }

    /**
     * Pick a random element from a non-empty list.
     */
    private static <T> T pick(Random rng, List<T> list) {
        return list.get(rng.nextInt(list.size()));
    }
}
