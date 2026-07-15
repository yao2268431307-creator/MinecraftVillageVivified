package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.terrain.TerrainModifier;
import com.livingvillages.regions.tier.SettlementTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SettlementName}.
 *
 * <p>Exercises the pure deterministic name composition: format
 * (prefix + optional modifier + tier suffix), tier suffix correctness,
 * determinism, coordinate/seed sensitivity, and null-safety. No
 * Minecraft runtime required.</p>
 */
class SettlementNameTest {

    private static final long SEED = 0x5EED_5EEDL;

    // ---------- 1. Determinism ----------

    @Test
    @DisplayName("format is deterministic for the same inputs")
    void formatIsDeterministic() {
        String a = SettlementName.format(SEED, 12, -7, RegionType.DESERT,
            SettlementTier.CITY, TerrainModifier.Kind.HIGH);
        String b = SettlementName.format(SEED, 12, -7, RegionType.DESERT,
            SettlementTier.CITY, TerrainModifier.Kind.HIGH);
        assertEquals(a, b);
    }

    @Test
    @DisplayName("format differs when the chunk differs (same seed/type/tier/terrain)")
    void formatDiffersByChunk() {
        String a = SettlementName.format(SEED, 0, 0, RegionType.PLAINS,
            SettlementTier.VILLAGE, TerrainModifier.Kind.NONE);
        String b = SettlementName.format(SEED, 1, 0, RegionType.PLAINS,
            SettlementTier.VILLAGE, TerrainModifier.Kind.NONE);
        assertNotEquals(a, b);
    }

    // ---------- 2. Tier suffix ----------

    @Test
    @DisplayName("format ends with the tier suffix 村/镇/城")
    void formatEndsWithTierSuffix() {
        assertEquals("村", suffixOf(RegionType.DESERT, SettlementTier.VILLAGE));
        assertEquals("镇", suffixOf(RegionType.SNOWY, SettlementTier.TOWN));
        assertEquals("城", suffixOf(RegionType.MOUNTAIN, SettlementTier.CITY));
    }

    private static String suffixOf(RegionType type, SettlementTier tier) {
        String name = SettlementName.format(SEED, 3, 9, type, tier, TerrainModifier.Kind.NONE);
        assertNotNull(name);
        assertFalse(name.isEmpty());
        return name.substring(name.length() - 1);
    }

    // ---------- 3. Terrain modifier ----------

    @Test
    @DisplayName("NONE terrain yields no modifier (name = prefix + suffix, no extra char between)")
    void noneTerrainHasNoModifier() {
        String name = SettlementName.format(SEED, 5, 5, RegionType.TAIGA,
            SettlementTier.VILLAGE, TerrainModifier.Kind.NONE);
        assertNotNull(name);
        // 2-char biome prefix + 1-char tier suffix = 3 chars, no modifier inserted.
        assertEquals(3, name.length(), "NONE should produce a 3-char name, got: " + name);
        assertEquals("村", name.substring(2));
    }

    @Test
    @DisplayName("HIGH/WATER modifiers, when present, are drawn from their pools and sit between prefix and suffix")
    void modifierFromCorrectPool() {
        for (int cx = 0; cx < 200; cx++) {
            String high = SettlementName.format(SEED, cx, 0, RegionType.MOUNTAIN,
                SettlementTier.CITY, TerrainModifier.Kind.HIGH);
            String water = SettlementName.format(SEED, cx, 0, RegionType.PLAINS,
                SettlementTier.TOWN, TerrainModifier.Kind.WATER);
            assertTrue(endsWith(high, "城"), "HIGH city must end with 城: " + high);
            assertTrue(endsWith(water, "镇"), "WATER town must end with 镇: " + water);
            String highMod = high.substring(2, high.length() - 1);
            String waterMod = water.substring(2, water.length() - 1);
            assertTrue(highMod.isEmpty() || TerrainModifier.HIGH_POOL.contains(highMod),
                "HIGH modifier must be from HIGH_POOL or empty: " + highMod);
            assertTrue(waterMod.isEmpty() || TerrainModifier.WATER_POOL.contains(waterMod),
                "WATER modifier must be from WATER_POOL or empty: " + waterMod);
        }
    }

    private static boolean endsWith(String s, String suffix) {
        return s.endsWith(suffix);
    }

    // ---------- 4. Every RegionType + tier yields a valid name ----------

    @Test
    @DisplayName("Every RegionType × tier yields a non-empty name ending in the tier suffix")
    void everyCombinationProducesValidName() {
        for (RegionType type : RegionType.values()) {
            for (SettlementTier tier : SettlementTier.values()) {
                String name = SettlementName.format(SEED, 11, 17, type, tier, TerrainModifier.Kind.NONE);
                assertNotNull(name, "null for " + type + "/" + tier);
                assertFalse(name.isEmpty(), "empty for " + type + "/" + tier);
                assertTrue(name.endsWith(tier.suffix()),
                    type + "/" + tier + " should end with " + tier.suffix() + ", got: " + name);
            }
        }
    }

    // ---------- 5. Null safety ----------

    @Test
    @DisplayName("null biome type returns the fallback \"无名村\"")
    void nullTypeFallback() {
        assertEquals("无名村", SettlementName.format(SEED, 0, 0, null,
            SettlementTier.VILLAGE, TerrainModifier.Kind.NONE));
    }

    @Test
    @DisplayName("null tier is treated as VILLAGE (suffix 村)")
    void nullTierTreatedAsVillage() {
        String name = SettlementName.format(SEED, 7, 7, RegionType.PLAINS, null, TerrainModifier.Kind.NONE);
        assertNotNull(name);
        assertTrue(name.endsWith("村"), "null tier should default to VILLAGE/村: " + name);
    }

    @Test
    @DisplayName("null terrain is treated as NONE")
    void nullTerrainTreatedAsNone() {
        String name = SettlementName.format(SEED, 7, 7, RegionType.PLAINS,
            SettlementTier.VILLAGE, null);
        assertEquals(3, name.length(), "null terrain → no modifier → 3-char name: " + name);
    }
}
