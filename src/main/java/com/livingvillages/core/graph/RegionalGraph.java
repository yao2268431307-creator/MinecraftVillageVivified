package com.livingvillages.core.graph;

import com.livingvillages.core.cluster.SettlementCluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RegionalGraph {
    private final List<SettlementCluster> clusters;
    private final List<GraphEdge> edges;

    public RegionalGraph(List<SettlementCluster> clusters, List<GraphEdge> edges) {
        this.clusters = Collections.unmodifiableList(new ArrayList<SettlementCluster>(clusters));
        this.edges = Collections.unmodifiableList(new ArrayList<GraphEdge>(edges));
    }

    public List<SettlementCluster> clusters() {
        return clusters;
    }

    public List<GraphEdge> edges() {
        return edges;
    }

    public List<GraphEdge> edgesFrom(int clusterId) {
        List<GraphEdge> result = new ArrayList<GraphEdge>();
        for (GraphEdge edge : edges) {
            if (edge.from() == clusterId) {
                result.add(edge);
            }
        }
        return result;
    }
}

