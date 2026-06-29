package com.livingvillages.core.graph;

import com.livingvillages.core.data.ClusterRecord;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageStateStore;

import java.util.*;

/**
 * Plans the KNN connection graph between clusters.
 *
 * <p>Connects each cluster to its k nearest neighbors, deduplicates edges,
 * and ensures the resulting graph is connected via BFS + bridging.</p>
 */
final class NetworkPlanner {

    /**
     * A candidate edge between two clusters.
     */
    record CandidateEdge(String fromId, String toId, double distance) {}

    private NetworkPlanner() {}

    /**
     * Plan the KNN graph.
     *
     * @param clusters all clusters (including already-processed ones for distance calculation)
     * @param k        number of nearest neighbors to connect to (from config)
     * @return list of candidate edges (deduplicated)
     */
    static List<CandidateEdge> plan(List<ClusterRecord> clusters, int k, VillageStateStore store) {
        if (clusters.size() < 2) {
            return Collections.emptyList();
        }

        // Build cluster center lookup
        Map<String, Vec3i> centers = new HashMap<>();
        for (ClusterRecord c : clusters) {
            // Find center village record to get position
            Vec3i centerPos = findCenterPosition(c, store);
            if (centerPos != null) {
                centers.put(c.id(), centerPos);
            }
        }

        if (centers.size() < 2) {
            return Collections.emptyList();
        }

        List<String> clusterIds = new ArrayList<>(centers.keySet());

        // Build distance matrix
        int n = clusterIds.size();
        double[][] distMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Vec3i a = centers.get(clusterIds.get(i));
                Vec3i b = centers.get(clusterIds.get(j));
                distMatrix[i][j] = a.horizontalDistance(b);
                distMatrix[j][i] = distMatrix[i][j];
            }
        }

        // KNN: for each cluster, find k nearest neighbors
        Set<String> edgeSet = new HashSet<>();
        List<CandidateEdge> edges = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            final int idx = i; // effectively final for lambda
            // Sort by distance
            List<Integer> neighbors = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i != j) neighbors.add(j);
            }
            neighbors.sort(Comparator.comparingDouble(j -> distMatrix[idx][j]));

            for (int j = 0; j < Math.min(k, neighbors.size()); j++) {
                int nb = neighbors.get(j);
                String from = clusterIds.get(idx);
                String to = clusterIds.get(nb);

                // Deduplicate: A→B and B→A are the same edge
                String edgeKey = from.compareTo(to) < 0
                        ? from + "::" + to
                        : to + "::" + from;

                if (edgeSet.add(edgeKey)) {
                    edges.add(new CandidateEdge(from, to, distMatrix[i][nb]));
                }
            }
        }

        // Connectivity check: BFS
        if (!isConnected(clusterIds, edgeSet)) {
            // Bridge: find the closest pair between disconnected components
            List<Set<String>> components = findComponents(clusterIds, edgeSet);
            if (components.size() > 1) {
                for (int c = 1; c < components.size(); c++) {
                    double bestDist = Double.MAX_VALUE;
                    String bestFrom = null;
                    String bestTo = null;
                    for (String a : components.get(0)) {
                        for (String b : components.get(c)) {
                            int ai = clusterIds.indexOf(a);
                            int bi = clusterIds.indexOf(b);
                            if (ai >= 0 && bi >= 0 && distMatrix[ai][bi] < bestDist) {
                                bestDist = distMatrix[ai][bi];
                                bestFrom = a;
                                bestTo = b;
                            }
                        }
                    }
                    if (bestFrom != null) {
                        String edgeKey = bestFrom.compareTo(bestTo) < 0
                                ? bestFrom + "::" + bestTo
                                : bestTo + "::" + bestFrom;
                        edgeSet.add(edgeKey);
                        edges.add(new CandidateEdge(bestFrom, bestTo, bestDist));
                    }
                }
            }
        }

        return edges;
    }

    private static Vec3i findCenterPosition(ClusterRecord cluster, VillageStateStore store) {
        for (var v : store.getVillages()) {
            if (v.id().equals(cluster.centerVillageId())) {
                return v.position();
            }
        }
        return null;
    }

    private static boolean isConnected(List<String> clusterIds, Set<String> edgeSet) {
        Map<String, Set<String>> adj = buildAdjacency(clusterIds, edgeSet);
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(clusterIds.get(0));
        visited.add(clusterIds.get(0));

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String nb : adj.getOrDefault(cur, Set.of())) {
                if (visited.add(nb)) {
                    queue.add(nb);
                }
            }
        }

        return visited.size() == clusterIds.size();
    }

    private static List<Set<String>> findComponents(List<String> clusterIds, Set<String> edgeSet) {
        Map<String, Set<String>> adj = buildAdjacency(clusterIds, edgeSet);
        Set<String> remaining = new HashSet<>(clusterIds);
        List<Set<String>> components = new ArrayList<>();

        while (!remaining.isEmpty()) {
            String start = remaining.iterator().next();
            Set<String> component = new HashSet<>();
            Deque<String> queue = new ArrayDeque<>();
            queue.add(start);

            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (component.add(cur)) {
                    remaining.remove(cur);
                    for (String nb : adj.getOrDefault(cur, Set.of())) {
                        if (!component.contains(nb)) {
                            queue.add(nb);
                        }
                    }
                }
            }
            components.add(component);
        }

        return components;
    }

    private static Map<String, Set<String>> buildAdjacency(List<String> clusterIds, Set<String> edgeSet) {
        Map<String, Set<String>> adj = new HashMap<>();
        for (String id : clusterIds) {
            adj.put(id, new HashSet<>());
        }
        for (String edge : edgeSet) {
            String[] parts = edge.split("::");
            adj.get(parts[0]).add(parts[1]);
            adj.get(parts[1]).add(parts[0]);
        }
        return adj;
    }
}
