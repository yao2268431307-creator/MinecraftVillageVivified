package com.livingvillages.regions.biome;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure-logic helpers in {@link BiomeRegionResolver}.
 *
 * <p>These tests exercise the {@code String}-based classification helpers
 * ({@link BiomeRegionResolver#isCaveBiomeId(String)} and
 * {@link BiomeRegionResolver#isForestBiomeId(String)}) so they do not require
 * the Minecraft runtime — only the MC classes need to be on the classpath
 * (which fabric-loom provides via {@code testCompileOnly} by default).</p>
 */
class BiomeRegionResolverTest {

    // ---------- isCaveBiomeId ----------

    @Test
    @DisplayName("isCaveBiomeId: lush_caves is a cave biome")
    void lushCavesIsCave() {
        assertTrue(BiomeRegionResolver.isCaveBiomeId("minecraft:lush_caves"));
    }

    @Test
    @DisplayName("isCaveBiomeId: dripstone_caves is a cave biome")
    void dripstoneCavesIsCave() {
        assertTrue(BiomeRegionResolver.isCaveBiomeId("minecraft:dripstone_caves"));
    }

    @Test
    @DisplayName("isCaveBiomeId: deep_dark is a cave biome")
    void deepDarkIsCave() {
        assertTrue(BiomeRegionResolver.isCaveBiomeId("minecraft:deep_dark"));
    }

    @Test
    @DisplayName("isCaveBiomeId: plains is NOT a cave biome")
    void plainsIsNotCave() {
        assertFalse(BiomeRegionResolver.isCaveBiomeId("minecraft:plains"));
    }

    @Test
    @DisplayName("isCaveBiomeId: null is NOT a cave biome")
    void nullIsNotCave() {
        assertFalse(BiomeRegionResolver.isCaveBiomeId(null));
    }

    // ---------- isForestBiomeId ----------

    @Test
    @DisplayName("isForestBiomeId: forest is a forest biome")
    void forestIsForest() {
        assertTrue(BiomeRegionResolver.isForestBiomeId("minecraft:forest"));
    }

    @Test
    @DisplayName("isForestBiomeId: birch_forest is a forest biome")
    void birchForestIsForest() {
        assertTrue(BiomeRegionResolver.isForestBiomeId("minecraft:birch_forest"));
    }

    @Test
    @DisplayName("isForestBiomeId: dark_forest is a forest biome")
    void darkForestIsForest() {
        assertTrue(BiomeRegionResolver.isForestBiomeId("minecraft:dark_forest"));
    }

    @Test
    @DisplayName("isForestBiomeId: old_growth_birch_forest is a forest biome")
    void oldGrowthBirchForestIsForest() {
        assertTrue(BiomeRegionResolver.isForestBiomeId("minecraft:old_growth_birch_forest"));
    }

    @Test
    @DisplayName("isForestBiomeId: flower_forest is a forest biome")
    void flowerForestIsForest() {
        assertTrue(BiomeRegionResolver.isForestBiomeId("minecraft:flower_forest"));
    }

    @Test
    @DisplayName("isForestBiomeId: plains is NOT a forest biome")
    void plainsIsNotForest() {
        assertFalse(BiomeRegionResolver.isForestBiomeId("minecraft:plains"));
    }

    @Test
    @DisplayName("isForestBiomeId: null is NOT a forest biome")
    void nullIsNotForest() {
        assertFalse(BiomeRegionResolver.isForestBiomeId(null));
    }

    @Test
    @DisplayName("isForestBiomeId: old_growth_pine_taiga is NOT classified as forest (matched by IS_TAIGA earlier)")
    void oldGrowthPineTaigaIsNotForest() {
        // Deliberately excluded: taiga variants are matched by the IS_TAIGA tag
        // before the forest id list is consulted.
        assertFalse(BiomeRegionResolver.isForestBiomeId("minecraft:old_growth_pine_taiga"));
    }
}
