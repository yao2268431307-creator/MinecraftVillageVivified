package com.livingvillages.core.worldgen;

import com.livingvillages.core.cluster.SettlementCluster;
import com.livingvillages.core.cluster.VillageSite;

public final class PlannedVillagePlacement {
    private final SettlementCluster cluster;
    private final VillageSite village;
    private final ChunkPos2 chunk;

    public PlannedVillagePlacement(SettlementCluster cluster, VillageSite village, ChunkPos2 chunk) {
        this.cluster = cluster;
        this.village = village;
        this.chunk = chunk;
    }

    public SettlementCluster cluster() {
        return cluster;
    }

    public VillageSite village() {
        return village;
    }

    public ChunkPos2 chunk() {
        return chunk;
    }
}

