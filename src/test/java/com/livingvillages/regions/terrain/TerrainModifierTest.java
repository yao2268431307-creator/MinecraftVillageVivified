package com.livingvillages.regions.terrain;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TerrainModifier}.
 *
 * <p>Exercises the pure modifier selector: NONE yields the empty
 * string, HIGH/WATER draw from their pools or the empty string, and
 * selection is deterministic for a fixed {@link Random}. No Minecraft
 * runtime required.</p>
 */
class TerrainModifierTest {

    // ---------- 1. NONE ----------

    @Test
    @DisplayName("NONE always yields the empty string")
    void noneYieldsEmpty() {
        assertEquals("", TerrainModifier.pick(TerrainModifier.Kind.NONE, new Random(0L)));
    }

    // ---------- 2. HIGH / WATER pools ----------

    @Test
    @DisplayName("HIGH yields a HIGH_POOL entry or the empty string")
    void highFromHighPool() {
        Random rng = new Random(12345L);
        for (int i = 0; i < 500; i++) {
            String m = TerrainModifier.pick(TerrainModifier.Kind.HIGH, rng);
            assertTrue(m.isEmpty() || TerrainModifier.HIGH_POOL.contains(m),
                "HIGH modifier must be empty or in HIGH_POOL: " + m);
        }
    }

    @Test
    @DisplayName("WATER yields a WATER_POOL entry or the empty string")
    void waterFromWaterPool() {
        Random rng = new Random(67890L);
        for (int i = 0; i < 500; i++) {
            String m = TerrainModifier.pick(TerrainModifier.Kind.WATER, rng);
            assertTrue(m.isEmpty() || TerrainModifier.WATER_POOL.contains(m),
                "WATER modifier must be empty or in WATER_POOL: " + m);
        }
    }

    @Test
    @DisplayName("HIGH and WATER pools contain the spec characters")
    void poolsContainSpecChars() {
        assertEquals(3, TerrainModifier.HIGH_POOL.size());
        assertEquals(3, TerrainModifier.WATER_POOL.size());
        assertTrue(TerrainModifier.HIGH_POOL.contains("岭"));
        assertTrue(TerrainModifier.HIGH_POOL.contains("崖"));
        assertTrue(TerrainModifier.HIGH_POOL.contains("峰"));
        assertTrue(TerrainModifier.WATER_POOL.contains("水"));
        assertTrue(TerrainModifier.WATER_POOL.contains("滨"));
        assertTrue(TerrainModifier.WATER_POOL.contains("渚"));
    }

    // ---------- 3. Determinism ----------

    @Test
    @DisplayName("pick is deterministic for a fixed Random seed")
    void pickIsDeterministic() {
        for (TerrainModifier.Kind kind : TerrainModifier.Kind.values()) {
            String a = TerrainModifier.pick(kind, new Random(42L));
            String b = TerrainModifier.pick(kind, new Random(42L));
            assertEquals(a, b, "deterministic for kind " + kind);
        }
    }
}
