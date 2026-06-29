package com.livingvillages.core.config;

/**
 * Core-layer configuration schema.
 *
 * <p>All fields are immutable. Use {@link #defaults()} for factory defaults.
 * Validation runs in the compact constructor — any out-of-range value throws
 * {@link IllegalArgumentException} immediately.</p>
 *
 * <p>The MC Adapter layer reads this from TOML via {@code ConfigLoader} and
 * passes it to all Core modules. No Minecraft types are referenced.</p>
 *
 * @param k                  number of cluster centers (2–50)
 * @param rCluster           max village scatter radius within a cluster, blocks (800–6400)
 * @param minSeparation      minimum distance between villages, blocks (32–512)
 * @param eps                DBSCAN neighborhood radius, blocks (100–500)
 * @param minPts             DBSCAN minimum points (fixed at 1)
 * @param knnK               KNN graph connectivity degree (1–5)
 * @param maxSlope           A* max slope Δy/horizontal distance (0.1–3.0)
 * @param tunnelThreshold    depth threshold for tunnel digging, blocks (3–20)
 * @param cesElasticity      CES production function elasticity (0.01–0.99)
 * @param diffusionRate      price heat diffusion coefficient (0.01–1.0)
 * @param dailyTickInterval  game ticks per economic day (default 24000 = 1 MC day)
 * @param fuelTicks          coal minecart fuel duration, ticks (100–72000)
 * @param maxActiveCaravans  max concurrent caravans (1–500)
 */
public record ModConfig(
        // ── K-Center ──
        @Range(min = 2, max = 50) int k,
        @Range(min = 800, max = 6400) int rCluster,
        @Range(min = 32, max = 512) int minSeparation,

        // ── Cluster ──
        @Range(min = 100, max = 500) int eps,
        int minPts,

        // ── Graph ──
        @Range(min = 1, max = 5) int knnK,
        @Range(min = 0.1, max = 3.0) double maxSlope,
        @Range(min = 3, max = 20) int tunnelThreshold,

        // ── Economy ──
        @Range(min = 0.01, max = 0.99) double cesElasticity,
        @Range(min = 0.01, max = 1.0) double diffusionRate,
        int dailyTickInterval,

        // ── Caravan ──
        @Range(min = 100, max = 72000) int fuelTicks,
        @Range(min = 1, max = 500) int maxActiveCaravans) {

    /**
     * Returns a ModConfig with all factory-default values.
     * These defaults are validated against the same range constraints.
     */
    public static ModConfig defaults() {
        return new ModConfig(
                8,       // k
                3200,    // rCluster
                96,      // minSeparation
                250,     // eps
                1,       // minPts
                2,       // knnK
                1.0,     // maxSlope
                6,       // tunnelThreshold
                0.7,     // cesElasticity
                0.15,    // diffusionRate
                24000,   // dailyTickInterval
                1200,    // fuelTicks
                50       // maxActiveCaravans
        );
    }

    /**
     * Compact constructor: validates all @Range constraints.
     * Throws IllegalArgumentException with a descriptive message on the first violation.
     */
    public ModConfig {
        validateInt("k", k, 2, 50);
        validateInt("rCluster", rCluster, 800, 6400);
        validateInt("minSeparation", minSeparation, 32, 512);
        validateInt("eps", eps, 100, 500);
        // minPts is fixed at 1 — no range validation needed, but must be ≥1
        if (minPts < 1) {
            throw new IllegalArgumentException("minPts must be ≥ 1, got " + minPts);
        }
        validateInt("knnK", knnK, 1, 5);
        validateDouble("maxSlope", maxSlope, 0.1, 3.0);
        validateInt("tunnelThreshold", tunnelThreshold, 3, 20);
        validateDouble("cesElasticity", cesElasticity, 0.01, 0.99);
        validateDouble("diffusionRate", diffusionRate, 0.01, 1.0);
        validateInt("fuelTicks", fuelTicks, 100, 72000);
        validateInt("maxActiveCaravans", maxActiveCaravans, 1, 500);
        // dailyTickInterval: must be positive
        if (dailyTickInterval < 1) {
            throw new IllegalArgumentException("dailyTickInterval must be ≥ 1, got " + dailyTickInterval);
        }
    }

    private static void validateInt(String name, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " = " + value + " is out of range [" + min + ", " + max + "]");
        }
    }

    private static void validateDouble(String name, double value, double min, double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " = " + value + " is out of range [" + min + ", " + max + "]");
        }
    }
}
