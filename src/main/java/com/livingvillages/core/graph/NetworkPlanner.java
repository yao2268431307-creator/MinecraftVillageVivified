package com.livingvillages.core.graph;

import com.livingvillages.core.cluster.SettlementCluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NetworkPlanner {
    public RegionalGraph planKNearest(List<SettlementCluster> clusters, int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be positive");
        }
        Set<String> undirected = new HashSet<String>();
        List<GraphEdge> directed = new ArrayList<GraphEdge>();

        for (SettlementCluster cluster : clusters) {
            List<SettlementCluster> neighbors = new ArrayList<SettlementCluster>(clusters);
            neighbors.remove(cluster);
            neighbors.sort(Comparator.comparingDouble(other -> cluster.center().distanceTo(other.center())));
            int limit = Math.min(k, neighbors.size());
            for (int i = 0; i < limit; i++) {
                SettlementCluster neighbor = neighbors.get(i);
                int a = Math.min(cluster.id(), neighbor.id());
                int b = Math.max(cluster.id(), neighbor.id());
                String key = a + ":" + b;
                if (undirected.add(key)) {
                    double distance = cluster.center().distanceTo(neighbor.center());
                    GraphEdge edge = new GraphEdge(a, b, distance, false);
                    directed.add(edge);
                    directed.add(edge.reversed());
                }
            }
        }
        return new RegionalGraph(clusters, directed);
    }
}

