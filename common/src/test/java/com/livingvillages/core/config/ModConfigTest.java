package com.livingvillages.core.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModConfig record.
 */
class ModConfigTest {

    @Test
    void defaults_constructsWithoutException() {
        // defaults() must return a valid config without throwing
        ModConfig cfg = assertDoesNotThrow(ModConfig::defaults);
        assertNotNull(cfg);
    }

    @Test
    void defaults_allValuesInExpectedRange() {
        ModConfig cfg = ModConfig.defaults();
        assertEquals(8, cfg.k());
        assertEquals(3200, cfg.rCluster());
        assertEquals(96, cfg.minSeparation());
        assertEquals(250, cfg.eps());
        assertEquals(1, cfg.minPts());
        assertEquals(2, cfg.knnK());
        assertEquals(1.0, cfg.maxSlope(), 1e-9);
        assertEquals(6, cfg.tunnelThreshold());
        assertEquals(0.7, cfg.cesElasticity(), 1e-9);
        assertEquals(0.15, cfg.diffusionRate(), 1e-9);
        assertEquals(24000, cfg.dailyTickInterval());
        assertEquals(1200, cfg.fuelTicks());
        assertEquals(50, cfg.maxActiveCaravans());
    }

    // ── Invalid values must throw ──

    @Test
    void k_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(1, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void k_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(51, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void rCluster_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 799, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void rCluster_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 6401, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void minSeparation_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 31, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void minSeparation_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 513, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void eps_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 99, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void eps_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 501, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void minPts_below1_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 0, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void knnK_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 0, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void knnK_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 6, 1.0, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void maxSlope_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 0.09, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void maxSlope_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 3.1, 6, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void tunnelThreshold_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 2, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void tunnelThreshold_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 21, 0.7, 0.15, 24000, 1200, 50));
    }

    @Test
    void cesElasticity_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.009, 0.15, 24000, 1200, 50));
    }

    @Test
    void cesElasticity_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.991, 0.15, 24000, 1200, 50));
    }

    @Test
    void diffusionRate_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.009, 24000, 1200, 50));
    }

    @Test
    void diffusionRate_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 1.01, 24000, 1200, 50));
    }

    @Test
    void fuelTicks_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 99, 50));
    }

    @Test
    void fuelTicks_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 72001, 50));
    }

    @Test
    void maxActiveCaravans_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 0));
    }

    @Test
    void maxActiveCaravans_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 501));
    }

    @Test
    void dailyTickInterval_zeroOrNegative_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 0, 1200, 50));
        assertThrows(IllegalArgumentException.class, () ->
                new ModConfig(8, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, -1, 1200, 50));
    }

    // ── Edge/boundary values should be valid ──

    @Test
    void boundaryValues_constructWithoutException() {
        // Test min boundary values
        assertDoesNotThrow(() -> new ModConfig(
                2, 800, 32, 100, 1, 1, 0.1, 3, 0.01, 0.01, 1, 100, 1));
        // Test max boundary values
        assertDoesNotThrow(() -> new ModConfig(
                50, 6400, 512, 500, 1, 5, 3.0, 20, 0.99, 1.0, Integer.MAX_VALUE, 72000, 500));
    }

    @Test
    void recordEquality() {
        ModConfig a = ModConfig.defaults();
        ModConfig b = ModConfig.defaults();
        ModConfig c = new ModConfig(10, 3200, 96, 250, 1, 2, 1.0, 6, 0.7, 0.15, 24000, 1200, 50);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }
}
