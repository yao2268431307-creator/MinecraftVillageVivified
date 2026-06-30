package com.livingvillages.core.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModConfigTest {

    @Test
    void defaults_constructsWithoutException() {
        assertDoesNotThrow(ModConfig::defaults);
    }

    @Test
    void defaults_hasExpectedValues() {
        ModConfig cfg = ModConfig.defaults();
        assertEquals(0, cfg.clusterCount());
        assertEquals(7, cfg.villagesPerCluster());
        assertEquals(320, cfg.clusterRadius());
        assertEquals(96, cfg.minSeparation());
        assertEquals(2, cfg.knnDegree());
        assertEquals(3, cfg.roadWidth());
        assertEquals("minecraft:stone_bricks", cfg.roadMaterial());
    }

    @Test
    void villagesPerCluster_below3_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 2, 320, 96, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void villagesPerCluster_above15_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 16, 320, 96, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void clusterRadius_below64_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 7, 63, 96, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void clusterRadius_above800_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 7, 801, 96, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void minSeparation_below32_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 7, 320, 31, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void minSeparation_above256_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ModConfig(0, 7, 320, 257, 2, 1.0, 6, 3, "stone", 24000, 20, 250, 1, 0.7, 0.15, 1200));
    }

    @Test
    void boundaryValues_ok() {
        assertDoesNotThrow(() -> new ModConfig(0, 3, 64, 32, 1, 0.1, 3, 1, "a", 1, 1, 100, 1, 0.01, 0.01, 100));
        assertDoesNotThrow(() -> new ModConfig(0, 15, 800, 256, 5, 3.0, 20, 7, "b", Integer.MAX_VALUE, 500, 500, 1, 0.99, 1.0, 72000));
    }

    @Test
    void recordEquality() {
        assertEquals(ModConfig.defaults(), ModConfig.defaults());
    }
}
