package com.livingvillages.core.economy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class MarketState {
    private final int clusterId;
    private final EnumMap<Good, Double> supply;
    private final EnumMap<Good, Double> demand;
    private final EnumMap<Good, Double> prices;

    public MarketState(int clusterId) {
        this.clusterId = clusterId;
        this.supply = new EnumMap<Good, Double>(Good.class);
        this.demand = new EnumMap<Good, Double>(Good.class);
        this.prices = new EnumMap<Good, Double>(Good.class);
        for (Good good : Good.values()) {
            supply.put(good, 1.0);
            demand.put(good, 1.0);
            prices.put(good, 1.0);
        }
    }

    public int clusterId() {
        return clusterId;
    }

    public double supply(Good good) {
        return supply.get(good);
    }

    public double demand(Good good) {
        return demand.get(good);
    }

    public double price(Good good) {
        return prices.get(good);
    }

    public void setSupply(Good good, double value) {
        supply.put(good, Math.max(0.001, value));
    }

    public void setDemand(Good good, double value) {
        demand.put(good, Math.max(0.001, value));
    }

    public void setPrice(Good good, double value) {
        prices.put(good, Math.max(0.001, value));
    }

    public Map<Good, Double> prices() {
        return Collections.unmodifiableMap(prices);
    }

    public MarketState copy() {
        MarketState copy = new MarketState(clusterId);
        for (Good good : Good.values()) {
            copy.setSupply(good, supply(good));
            copy.setDemand(good, demand(good));
            copy.setPrice(good, price(good));
        }
        return copy;
    }
}

