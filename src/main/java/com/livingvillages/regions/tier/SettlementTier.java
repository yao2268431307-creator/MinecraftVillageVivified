package com.livingvillages.regions.tier;

import com.livingvillages.regions.naming.SeedHash;
import java.util.Random;

/**
 * Settlement size tiers for the LivingVillages cluster mod.
 *
 * <p>Each village placement is assigned a tier deterministically from
 * the world seed and the village's grid-cell coordinate, so the same
 * world always yields the same cities. The tier drives BOTH the
 * per-placement jigsaw size multiplier (server worldgen Mixin) and the
 * settlement name suffix (client display), keeping the two in lockstep
 * without any server&rarr;client tier sync.</p>
 *
 * <p>Assignment is a probabilistic {@code city:town:village = 1:2:5}
 * draw (1/8, 2/8, 5/8) — "purely luck": a traveller may hit a city first
 * or pass 10+ villages before one. A soft min-gap guard then demotes a
 * city to a town if a lexicographically-earlier adjacent grid cell also
 * drew city-raw, so two cities never spawn in neighbouring cells. With
 * the default {@link #MIN_GAP} the realised city rate is somewhat below
 * 1/8 (cities feel rarer); set {@code MIN_GAP = 0} for the exact 1:2:5
 * raw ratio.</p>
 *
 * <p>Tier is computed on the village <em>grid cell</em>
 * ({@code chunk / VILLAGE_SPACING}) rather than the raw chunk, so the
 * min-gap scan counts real villages (one per cell) instead of phantom
 * chunks. {@link #VILLAGE_SPACING} mirrors the vanilla village
 * structure_set spacing — update it if village density is ever changed.</p>
 *
 * <p>Pure logic, no MC runtime dependency — unit-testable directly.</p>
 */
public enum SettlementTier {

    /** Vanilla-sized settlement. Suffix 村. */
    VILLAGE(1, 1, "村"),
    /** Mid-sized settlement. Suffix 镇. */
    TOWN(3, 3, "镇"),
    /** Mega settlement. Suffix 城. */
    CITY(8, 8, "城");

    /** Multiplier applied to {@code maxDepth} (jigsaw chain depth). */
    private final int depthFactor;
    /** Multiplier applied to {@code maxDistanceFromCenter} (footprint). */
    private final int distanceFactor;
    /** Name suffix for this tier. */
    private final String suffix;

    SettlementTier(int depthFactor, int distanceFactor, String suffix) {
        this.depthFactor = depthFactor;
        this.distanceFactor = distanceFactor;
        this.suffix = suffix;
    }

    public int depthFactor() {
        return depthFactor;
    }

    public int distanceFactor() {
        return distanceFactor;
    }

    public String suffix() {
        return suffix;
    }

    /** Lowercase English label for command/chat output (e.g. "city"). */
    public String suffixLabel() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Vanilla village structure_set spacing, in chunks. Tier is resolved
     * per grid cell so the min-gap guard counts villages, not chunks.
     */
    static final int VILLAGE_SPACING = 34;

    /**
     * Min-gap radius in grid cells. A city is demoted to a town if a
     * lexicographically-earlier cell within this Chebyshev radius also
     * drew city-raw. 1 = no two cities in adjacent (incl. diagonal) cells;
     * 0 disables the guard (exact 1:2:5 raw ratio).
     */
    static final int MIN_GAP = 1;

    /**
     * Resolve the tier for a village at the given chunk coordinate.
     *
     * <p>Both the server sizing Mixin and the client display call this
     * with the village's <em>generation chunk</em>, so they agree.
     *
     * @param worldSeed the world seed
     * @param chunkX    the village generation chunk X
     * @param chunkZ    the village generation chunk Z
     * @return the settlement tier, never {@code null}
     */
    public static SettlementTier tierFor(long worldSeed, int chunkX, int chunkZ) {
        return tierFor(worldSeed, chunkX, chunkZ, MIN_GAP);
    }

    /**
     * Testable overload allowing the min-gap radius to be specified.
     */
    static SettlementTier tierFor(long worldSeed, int chunkX, int chunkZ, int minGap) {
        int gx = Math.floorDiv(chunkX, VILLAGE_SPACING);
        int gz = Math.floorDiv(chunkZ, VILLAGE_SPACING);
        SettlementTier raw = rawRoll(worldSeed, gx, gz);
        if (raw == CITY && minGap > 0 && hasEarlierAdjacentCity(worldSeed, gx, gz, minGap)) {
            return TOWN;
        }
        return raw;
    }

    /**
     * Raw 1:2:5 draw for a single grid cell (no min-gap demotion).
     */
    private static SettlementTier rawRoll(long worldSeed, int gx, int gz) {
        Random rng = new Random(SeedHash.hash(worldSeed, gx, gz));
        int roll = rng.nextInt(8); // 0..7 inclusive
        if (roll < 1) {
            return CITY;     // 1/8
        }
        if (roll < 3) {
            return TOWN;     // 2/8
        }
        return VILLAGE;      // 5/8
    }

    /**
     * Returns {@code true} if any lexicographically-earlier grid cell
     * within the Chebyshev {@code minGap} radius drew city-raw.
     *
     * <p>Lexicographic ordering gives a deterministic total order on
     * cells, so for any adjacent city pair exactly one (the later) is
     * demoted — no recursion, no dependency on scan order. Only the
     * <em>raw</em> roll of neighbours is inspected, so the check is
     * finite and terminates after a bounded {(2*minGap+1)^2 - 1} lookups.
     */
    private static boolean hasEarlierAdjacentCity(long worldSeed, int gx, int gz, int minGap) {
        for (int dx = -minGap; dx <= minGap; dx++) {
            for (int dz = -minGap; dz <= minGap; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int nx = gx + dx;
                int nz = gz + dz;
                // lexicographically earlier: (nx, nz) < (gx, gz)
                if (nx < gx || (nx == gx && nz < gz)) {
                    if (rawRoll(worldSeed, nx, nz) == CITY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
