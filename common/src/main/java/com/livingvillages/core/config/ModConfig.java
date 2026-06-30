package com.livingvillages.core.config;

/**
 * Core-layer configuration schema.
 *
 * <p>All fields are immutable. Use {@link #defaults()} for factory defaults.
 * Validation runs in the compact constructor.</p>
 *
 * <p>TOML exposes only user-relevant params (~9). Algorithm params
 * (eps, cesElasticity, etc.) are hardcoded for now.</p>
 */
public record ModConfig(
        // ── World ──
        int clusterCount,           // 0 = auto (vanilla villages / villagesPerCluster)
        @Range(min = 3, max = 15) int villagesPerCluster,
        @Range(min = 64, max = 800) int clusterRadius,
        @Range(min = 32, max = 256) int minSeparation,

        // ── Graph (hardcoded for now) ──
        int knnDegree,
        @Range(min = 0.1, max = 3.0) double maxSlope,
        @Range(min = 3, max = 20) int tunnelThreshold,

        // ── Road ──
        int roadWidth,
        String roadMaterial,

        // ── Economy (hardcoded for now) ──
        int dailyTickInterval,
        int maxCaravans,

        // ── DBSCAN (hardcoded) ──
        int eps,
        int minPts,
        double cesElasticity,
        double diffusionRate,
        int fuelTicks) {

    /** Readable version. */
    public String modVersion() { return "1.1.0"; }

    public static ModConfig defaults() {
        return new ModConfig(
                0,      // clusterCount (auto)
                7,      // villagesPerCluster
                320,    // clusterRadius
                96,     // minSeparation
                2,      // knnDegree
                1.0,    // maxSlope
                6,      // tunnelThreshold
                3,      // roadWidth
                "minecraft:stone_bricks",  // roadMaterial
                24000,  // dailyTickInterval
                20,     // maxCaravans
                250,    // eps (hardcoded)
                1,      // minPts (hardcoded)
                0.7,    // cesElasticity (hardcoded)
                0.15,   // diffusionRate (hardcoded)
                1200    // fuelTicks (hardcoded)
        );
    }

    public ModConfig {
        if (villagesPerCluster < 3 || villagesPerCluster > 15)
            throw new IllegalArgumentException("villagesPerCluster must be 3-15: " + villagesPerCluster);
        if (clusterRadius < 64 || clusterRadius > 800)
            throw new IllegalArgumentException("clusterRadius must be 64-800: " + clusterRadius);
        if (minSeparation < 32 || minSeparation > 256)
            throw new IllegalArgumentException("minSeparation must be 32-256: " + minSeparation);
        if (maxSlope < 0.1 || maxSlope > 3.0)
            throw new IllegalArgumentException("maxSlope must be 0.1-3.0: " + maxSlope);
        if (tunnelThreshold < 3 || tunnelThreshold > 20)
            throw new IllegalArgumentException("tunnelThreshold must be 3-20: " + tunnelThreshold);
        if (roadWidth < 1 || roadWidth > 7)
            throw new IllegalArgumentException("roadWidth must be 1-7: " + roadWidth);
        if (dailyTickInterval < 1)
            throw new IllegalArgumentException("dailyTickInterval must be ≥1: " + dailyTickInterval);
        if (maxCaravans < 1)
            throw new IllegalArgumentException("maxCaravans must be ≥1: " + maxCaravans);
        if (eps < 100 || eps > 500)
            throw new IllegalArgumentException("eps must be 100-500: " + eps);
        if (minPts < 1)
            throw new IllegalArgumentException("minPts must be ≥1: " + minPts);
    }
}
