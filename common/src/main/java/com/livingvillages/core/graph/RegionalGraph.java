package com.livingvillages.core.graph;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;

import java.util.*;

/**
 * Builds the inter-cluster rail network.
 *
 * <p>Three-phase pipeline:</p>
 * <ol>
 *   <li>{@link NetworkPlanner} — KNN graph connection</li>
 *   <li>{@link ConstrainedAStar} — 3D pathfinding between connected clusters</li>
 *   <li>{@link BezierSmoother} — cubic Bezier path smoothing</li>
 * </ol>
 *
 * <p>Writer of: {@code interClusterEdges[]}.
 * Reader of: {@code clusters[]}, {@code clusterNames[]}.</p>
 */
public final class RegionalGraph {

    private RegionalGraph() {}

    /**
     * Build the regional rail graph for all clusters with edgesBuilt==false.
     *
     * @param store state store (reads clusters, writes edges)
     * @param cfg   config (uses graph.knnK, graph.maxSlope)
     */
    public static void buildRegionalGraph(VillageStateStore store, ModConfig cfg) {
        List<ClusterRecord> allClusters = new ArrayList<>(store.getClusters());
        if (allClusters.size() < 2) {
            return; // nothing to connect
        }

        // Only plan edges for clusters that haven't been built yet
        List<ClusterRecord> unbuilt = new ArrayList<>();
        for (ClusterRecord c : allClusters) {
            if (!c.edgesBuilt()) unbuilt.add(c);
        }
        if (unbuilt.size() < 2) {
            return; // all clusters already connected (or only 1 unbuilt)
        }

        int k = cfg.knnDegree();
        double maxSlope = cfg.maxSlope();

        // Phase 1: KNN graph planning
        List<NetworkPlanner.CandidateEdge> candidates = NetworkPlanner.plan(allClusters, k, store);
        if (candidates.isEmpty()) {
            return;
        }

        // Phase 2: A* pathfinding for each candidate edge
        // Build center position lookup
        Map<String, Vec3i> centers = new HashMap<>();
        for (ClusterRecord c : allClusters) {
            for (VillageRecord v : store.getVillages()) {
                if (v.id().equals(c.centerVillageId())) {
                    centers.put(c.id(), v.position());
                    break;
                }
            }
        }

        List<EdgeRecord> newEdges = new ArrayList<>(store.getInterClusterEdges());
        Set<String> processedClusters = new HashSet<>();

        for (NetworkPlanner.CandidateEdge candidate : candidates) {
            Vec3i fromPos = centers.get(candidate.fromId());
            Vec3i toPos = centers.get(candidate.toId());
            if (fromPos == null || toPos == null) {
                continue;
            }

            // Try A* with maxSlope
            ConstrainedAStar.PathResult result = ConstrainedAStar.findPath(fromPos, toPos, maxSlope);

            // If failed, retry with relaxed slope
            if (!result.found() || result.path().isEmpty()) {
                result = ConstrainedAStar.findPath(fromPos, toPos, maxSlope * 2.0);
            }

            if (!result.path().isEmpty()) {
                // Phase 3: Bezier smoothing
                List<Vec3i> smoothed = BezierSmoother.smooth(result.path());

                EdgeRecord edge = new EdgeRecord(
                        candidate.fromId(),
                        candidate.toId(),
                        smoothed,
                        EdgeType.RAIL);
                newEdges.add(edge);

                processedClusters.add(candidate.fromId());
                processedClusters.add(candidate.toId());
            }
            // else: skip this edge (log warning in production)
        }

        // Mark processed clusters as edgesBuilt
        for (int i = 0; i < allClusters.size(); i++) {
            ClusterRecord c = allClusters.get(i);
            if (processedClusters.contains(c.id()) && !c.edgesBuilt()) {
                allClusters.set(i, new ClusterRecord(
                        c.id(), c.memberVillageIds(), c.centerVillageId(),
                        c.isNamed(), true));
            }
        }

        store.setInterClusterEdges(newEdges);
        store.setClusters(allClusters);
    }
}
