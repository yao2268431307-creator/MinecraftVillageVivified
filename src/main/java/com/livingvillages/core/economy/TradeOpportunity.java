package com.livingvillages.core.economy;

public final class TradeOpportunity {
    private final int sourceClusterId;
    private final int targetClusterId;
    private final Good good;
    private final double profit;
    private final double distance;

    public TradeOpportunity(int sourceClusterId, int targetClusterId, Good good, double profit, double distance) {
        this.sourceClusterId = sourceClusterId;
        this.targetClusterId = targetClusterId;
        this.good = good;
        this.profit = profit;
        this.distance = distance;
    }

    public int sourceClusterId() {
        return sourceClusterId;
    }

    public int targetClusterId() {
        return targetClusterId;
    }

    public Good good() {
        return good;
    }

    public double profit() {
        return profit;
    }

    public double distance() {
        return distance;
    }
}

