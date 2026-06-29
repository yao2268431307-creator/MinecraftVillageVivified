package com.livingvillages.core.economy;

import com.livingvillages.core.data.CaravanState;
import com.livingvillages.core.data.CaravanPhase;
import com.livingvillages.core.data.EdgeRecord;
import com.livingvillages.core.data.EdgeType;
import com.livingvillages.core.data.Vec3i;

import java.util.*;

/**
 * Graph Laplacian heat diffusion for price equalization across connected clusters.
 *
 * <p>The railway network acts as a heat conduction medium: prices diffuse
 * from high-price clusters to low-price clusters. Edge weights depend on
 * distance, rail quality, and active caravans.</p>
 *
 * <p>Iteration: P_{t+1} = (I - αL) × P_t</p>
 * <p>Convergence: max|ΔP| < 0.01 or 100 iterations max.</p>
 */
final class GraphHeatDiffusion {

    private static final double CONVERGENCE_THRESHOLD = 0.01;
    private static final int MAX_ITERATIONS = 100;

    private GraphHeatDiffusion() {}

    /**
     * Diffuse prices across the cluster graph.
     *
     * @param clusterIds    ordered list of cluster IDs (defines vector index)
     * @param initialPrices initial prices per cluster for one commodity
     * @param edges         inter-cluster edges (rail network)
     * @param caravans      active caravans (for weight boosting)
     * @param diffusionRate α (from config)
     * @return converged price per cluster (same order as clusterIds)
     */
    static double[] diffuse(
            List<String> clusterIds,
            double[] initialPrices,
            List<EdgeRecord> edges,
            List<CaravanState> caravans,
            double diffusionRate) {

        int n = clusterIds.size();
        if (n <= 1) {
            return Arrays.copyOf(initialPrices, n);
        }

        // Build index map
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < n; i++) {
            index.put(clusterIds.get(i), i);
        }

        // Build adjacency matrix (weights)
        double[][] weights = buildWeightMatrix(n, index, edges, caravans);

        // Build normalized Laplacian: L = I - D^{-1/2} W D^{-1/2}
        double[] degree = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                degree[i] += weights[i][j];
            }
        }

        double[][] laplacian = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    laplacian[i][j] = 1.0;
                } else {
                    double denom = Math.sqrt(Math.max(degree[i], 1e-9) * Math.max(degree[j], 1e-9));
                    laplacian[i][j] = -weights[i][j] / denom;
                }
            }
        }

        // Iteration: P_{t+1} = (I - αL) × P_t
        double[] prices = Arrays.copyOf(initialPrices, n);
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double[] newPrices = new double[n];
            double maxDelta = 0;

            for (int i = 0; i < n; i++) {
                double sum = 0;
                for (int j = 0; j < n; j++) {
                    // (I - αL) operator
                    double operator = (i == j ? 1.0 : 0) - diffusionRate * laplacian[i][j];
                    sum += operator * prices[j];
                }
                newPrices[i] = Math.max(0, sum); // prices cannot go negative
                maxDelta = Math.max(maxDelta, Math.abs(newPrices[i] - prices[i]));
            }

            prices = newPrices;
            if (maxDelta < CONVERGENCE_THRESHOLD) {
                break;
            }
        }

        return prices;
    }

    /**
     * Build symmetric weight matrix.
     * w_ij = 1 / (1 + distance_ij) × railBonus × caravanBonus
     */
    private static double[][] buildWeightMatrix(
            int n,
            Map<String, Integer> index,
            List<EdgeRecord> edges,
            List<CaravanState> caravans) {

        double[][] w = new double[n][n];

        // Build caravan activity map: edgeKey -> count of active caravans
        Map<String, Integer> caravanOnEdge = new HashMap<>();
        for (CaravanState c : caravans) {
            if (c.phase() == CaravanPhase.MOVING) {
                String key = edgeKey(c.fromClusterId(), c.toClusterId());
                caravanOnEdge.merge(key, 1, Integer::sum);
            }
        }

        for (EdgeRecord edge : edges) {
            Integer fromIdx = index.get(edge.fromClusterId());
            Integer toIdx = index.get(edge.toClusterId());
            if (fromIdx == null || toIdx == null) continue;

            // Base weight: inverse distance
            double distance = computePathDistance(edge.path());
            double weight = 1.0 / (1.0 + distance);

            // Rail bonus
            if (edge.type() == EdgeType.RAIL) {
                weight *= 2.0;
            }

            // Caravan bonus
            String key = edgeKey(edge.fromClusterId(), edge.toClusterId());
            int activeCaravans = caravanOnEdge.getOrDefault(key, 0);
            if (activeCaravans > 0) {
                weight *= (1.0 + activeCaravans * 2.0); // each caravan adds 2x
            }

            w[fromIdx][toIdx] = weight;
            w[toIdx][fromIdx] = weight; // symmetric
        }

        return w;
    }

    /** Compute total path length from waypoints. */
    private static double computePathDistance(List<Vec3i> path) {
        if (path.size() < 2) return 0;
        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i - 1).horizontalDistance(path.get(i));
        }
        return total;
    }

    /** Canonical edge key (order-independent). */
    private static String edgeKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }
}
