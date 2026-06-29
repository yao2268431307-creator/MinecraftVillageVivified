package com.livingvillages.core.economy;

import com.livingvillages.core.graph.GraphEdge;
import com.livingvillages.core.graph.RegionalGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PriceDiffusion {
    private final double alpha;
    private final double elasticity;

    public PriceDiffusion(double alpha, double elasticity) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be in [0, 1]");
        }
        this.alpha = alpha;
        this.elasticity = elasticity;
    }

    public void initializeLocalPrices(List<MarketState> markets, Good good) {
        for (MarketState market : markets) {
            market.setPrice(good, localPrice(market, good));
        }
    }

    public void diffuse(RegionalGraph graph, List<MarketState> markets, Good good, int iterations) {
        Map<Integer, MarketState> byId = new HashMap<Integer, MarketState>();
        for (MarketState market : markets) {
            byId.put(market.clusterId(), market);
        }

        for (int iter = 0; iter < iterations; iter++) {
            Map<Integer, Double> next = new HashMap<Integer, Double>();
            for (MarketState market : markets) {
                double local = localPrice(market, good);
                double weightedSum = 0.0;
                double totalWeight = 0.0;
                for (GraphEdge edge : graph.edgesFrom(market.clusterId())) {
                    MarketState neighbor = byId.get(edge.to());
                    if (neighbor == null) {
                        continue;
                    }
                    double weight = 1.0 / Math.max(1.0, edge.distance());
                    if (edge.activeCaravan()) {
                        weight *= 3.0;
                    }
                    weightedSum += weight * neighbor.price(good);
                    totalWeight += weight;
                }
                double price = totalWeight > 0.0
                        ? (1.0 - alpha) * local + alpha * weightedSum / totalWeight
                        : local;
                next.put(market.clusterId(), price);
            }
            for (MarketState market : markets) {
                market.setPrice(good, next.get(market.clusterId()));
            }
        }
    }

    private double localPrice(MarketState market, Good good) {
        return Math.pow(market.demand(good) / market.supply(good), elasticity);
    }
}

