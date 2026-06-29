package com.livingvillages.core.cluster;

import com.livingvillages.core.geom.WorldBounds;

public final class ClusterConfig {
    private final int clusterCount;
    private final int clusterRadius;
    private final int villagesPerCluster;
    private final int villagesPerClusterJitter;
    private final int minVillageSeparation;
    private final WorldBounds bounds;

    public ClusterConfig(
            int clusterCount,
            int clusterRadius,
            int villagesPerCluster,
            int villagesPerClusterJitter,
            int minVillageSeparation,
            WorldBounds bounds
    ) {
        if (clusterCount <= 0) {
            throw new IllegalArgumentException("clusterCount must be positive");
        }
        if (clusterRadius <= 0 || villagesPerCluster <= 0 || minVillageSeparation <= 0) {
            throw new IllegalArgumentException("radius, village count, and separation must be positive");
        }
        this.clusterCount = clusterCount;
        this.clusterRadius = clusterRadius;
        this.villagesPerCluster = villagesPerCluster;
        this.villagesPerClusterJitter = Math.max(0, villagesPerClusterJitter);
        this.minVillageSeparation = minVillageSeparation;
        this.bounds = bounds;
    }

    public int clusterCount() {
        return clusterCount;
    }

    public int clusterRadius() {
        return clusterRadius;
    }

    public int villagesPerCluster() {
        return villagesPerCluster;
    }

    public int villagesPerClusterJitter() {
        return villagesPerClusterJitter;
    }

    public int minVillageSeparation() {
        return minVillageSeparation;
    }

    public WorldBounds bounds() {
        return bounds;
    }
}

