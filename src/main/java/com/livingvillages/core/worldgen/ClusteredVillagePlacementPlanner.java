package com.livingvillages.core.worldgen;

import com.livingvillages.core.cluster.ClusterConfig;
import com.livingvillages.core.cluster.KCenterClusterGenerator;
import com.livingvillages.core.cluster.SettlementCluster;
import com.livingvillages.core.cluster.VillageSite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClusteredVillagePlacementPlanner {
    private final KCenterClusterGenerator generator;

    public ClusteredVillagePlacementPlanner(KCenterClusterGenerator generator) {
        this.generator = generator;
    }

    public Plan plan(long worldSeed, ClusterConfig config) {
        List<SettlementCluster> clusters = generator.generate(worldSeed, config);
        Map<ChunkPos2, PlannedVillagePlacement> byChunk = new HashMap<ChunkPos2, PlannedVillagePlacement>();

        for (SettlementCluster cluster : clusters) {
            for (VillageSite village : cluster.villages()) {
                ChunkPos2 chunk = ChunkPos2.fromBlock(village.position().x(), village.position().z());
                byChunk.put(chunk, new PlannedVillagePlacement(cluster, village, chunk));
            }
        }
        return new Plan(clusters, byChunk);
    }

    public static final class Plan {
        private final List<SettlementCluster> clusters;
        private final Map<ChunkPos2, PlannedVillagePlacement> byChunk;

        private Plan(List<SettlementCluster> clusters, Map<ChunkPos2, PlannedVillagePlacement> byChunk) {
            this.clusters = Collections.unmodifiableList(new ArrayList<SettlementCluster>(clusters));
            this.byChunk = Collections.unmodifiableMap(new HashMap<ChunkPos2, PlannedVillagePlacement>(byChunk));
        }

        public List<SettlementCluster> clusters() {
            return clusters;
        }

        public List<PlannedVillagePlacement> placements() {
            return new ArrayList<PlannedVillagePlacement>(byChunk.values());
        }

        public boolean isVillageChunk(int chunkX, int chunkZ) {
            return byChunk.containsKey(new ChunkPos2(chunkX, chunkZ));
        }

        public PlannedVillagePlacement placementAt(int chunkX, int chunkZ) {
            return byChunk.get(new ChunkPos2(chunkX, chunkZ));
        }
    }
}

