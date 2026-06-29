package com.livingvillages.core.graph;

public final class GraphEdge {
    private final int from;
    private final int to;
    private final double distance;
    private final boolean activeCaravan;

    public GraphEdge(int from, int to, double distance, boolean activeCaravan) {
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.activeCaravan = activeCaravan;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    public double distance() {
        return distance;
    }

    public boolean activeCaravan() {
        return activeCaravan;
    }

    public GraphEdge reversed() {
        return new GraphEdge(to, from, distance, activeCaravan);
    }
}

