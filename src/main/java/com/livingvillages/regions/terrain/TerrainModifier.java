package com.livingvillages.regions.terrain;

import java.util.List;
import java.util.Random;

/**
 * Terrain modifier pools for settlement naming.
 *
 * <p>A settlement name is {@code <biome prefix> + <terrain modifier> +
 * <tier suffix>}. The terrain modifier is optional and chosen from one
 * of two pools — {@link #HIGH_POOL} for elevated settlements,
 * {@link #WATER_POOL} for waterside ones — or the empty string when the
 * terrain is unremarkable.</p>
 *
 * <p>The {@link #pick(Kind, Random)} selector is pure (it only consumes
 * a {@link Random} stream), so it is unit-testable. The actual
 * high/waterside <em>detection</em> requires a level and lives in
 * {@code TerrainResolver} (MC-dependent, not tested here).</p>
 */
public final class TerrainModifier {

    /** Which terrain modifier, if any, applies to a settlement. */
    public enum Kind {
        /** No modifier — unremarkable terrain. */
        NONE,
        /** Elevated settlement — picks from {@link #HIGH_POOL}. */
        HIGH,
        /** Waterside settlement — picks from {@link #WATER_POOL}. */
        WATER
    }

    /** Modifier characters for elevated settlements. */
    public static final List<String> HIGH_POOL = List.of("岭", "崖", "峰");

    /** Modifier characters for waterside settlements. */
    public static final List<String> WATER_POOL = List.of("水", "滨", "渚");

    private TerrainModifier() {
        // utility class
    }

    /**
     * Pick a modifier string for the given kind, drawing from the
     * supplied {@link Random}. HIGH/WATER each have a 50% chance of
     * yielding the empty string (no modifier) and otherwise draw from
     * their pool; NONE always yields the empty string.
     *
     * @param kind the terrain kind
     * @param rng  the deterministic random source
     * @return a modifier character, or {@code ""}
     */
    public static String pick(Kind kind, Random rng) {
        if (kind == Kind.HIGH) {
            return rng.nextBoolean() ? HIGH_POOL.get(rng.nextInt(HIGH_POOL.size())) : "";
        }
        if (kind == Kind.WATER) {
            return rng.nextBoolean() ? WATER_POOL.get(rng.nextInt(WATER_POOL.size())) : "";
        }
        return "";
    }
}
