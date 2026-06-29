package com.livingvillages.core.economy;

import com.livingvillages.core.graph.GraphEdge;
import com.livingvillages.core.graph.RegionalGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TradeRouteFinder {
    public List<TradeOpportunity> findProfitableRoutes(RegionalGraph graph, List<MarketState> markets, double costPerBlock) {
        int n = graph.clusters().size();
        double[][] dist = floydWarshall(n, graph.edges());
        List<TradeOpportunity> routes = new ArrayList<TradeOpportunity>();

        for (MarketState source : markets) {
            for (MarketState target : markets) {
                if (source.clusterId() == target.clusterId()) {
                    continue;
                }
                double distance = dist[source.clusterId()][target.clusterId()];
                if (Double.isInfinite(distance)) {
                    continue;
                }
                for (Good good : Good.values()) {
                    double profit = target.price(good) - source.price(good) - distance * costPerBlock;
                    if (profit > 0.0) {
                        routes.add(new TradeOpportunity(source.clusterId(), target.clusterId(), good, profit, distance));
                    }
                }
            }
        }
        routes.sort(Comparator.comparingDouble(TradeOpportunity::profit).reversed());
        return routes;
    }

    private double[][] floydWarshall(int n, List<GraphEdge> edges) {
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dist[i][j] = i == j ? 0.0 : Double.POSITIVE_INFINITY;
            }
        }
        for (GraphEdge edge : edges) {
            dist[edge.from()][edge.to()] = Math.min(dist[edge.from()][edge.to()], edge.distance());
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double through = dist[i][k] + dist[k][j];
                    if (through < dist[i][j]) {
                        dist[i][j] = through;
                    }
                }
            }
        }
        return dist;
    }
}

