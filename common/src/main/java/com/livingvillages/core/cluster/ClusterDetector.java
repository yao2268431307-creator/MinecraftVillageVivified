package com.livingvillages.core.cluster;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.ClusterRecord;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageRecord;
import com.livingvillages.core.data.VillageStateStore;

import java.util.*;

/**
 * Detects clusters (聚落) from a flat list of villages using DBSCAN.
 *
 * <p>Only processes villages with {@code placed == true} that are not yet assigned to any
 * cluster. New clusters are appended to the existing {@code clusters[]} list.</p>
 *
 * <p>Uses a simple grid spatial index (cell size = eps) to accelerate region queries
 * from O(n²) to approximately O(n).</p>
 *
 * <p>Constraint V2: imports only {@code data} package types and Java standard library.</p>
 */
public final class ClusterDetector {

    private ClusterDetector() {
        // static utility
    }

    /**
     * Runs DBSCAN on unassigned placed villages, appending new clusters to the store.
     *
     * @param store VillageStateStore — reads {@code villages[]}, writes {@code clusters[]}
     * @param cfg   configuration providing {@code eps} and {@code minPts}
     */
    public static void detectClusters(VillageStateStore store, ModConfig cfg) {
        List<VillageRecord> allVillages = store.getVillages();
        List<ClusterRecord> existingClusters = store.getClusters();

        // 1. Collect already-assigned village IDs
        Set<UUID> assignedIds = new HashSet<>();
        for (ClusterRecord cr : existingClusters) {
            assignedIds.addAll(cr.memberVillageIds());
        }

        // 2. Filter: placed == true and not yet assigned
        List<VillageRecord> candidates = new ArrayList<>();
        for (VillageRecord v : allVillages) {
            if (v.placed() && !assignedIds.contains(v.id())) {
                candidates.add(v);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        int eps = cfg.eps();
        int minPts = cfg.minPts();

        // 3. Build spatial index
        GridIndex index = new GridIndex(candidates, eps);

        // 4. DBSCAN: track visited status
        Set<UUID> visited = new HashSet<>();
        List<ClusterRecord> newClusters = new ArrayList<>(existingClusters);

        for (VillageRecord v : candidates) {
            if (visited.contains(v.id())) {
                continue;
            }
            visited.add(v.id());

            List<VillageRecord> neighbors = index.regionQuery(v, eps);

            if (neighbors.size() >= minPts) {
                // Found a core point — expand cluster
                List<VillageRecord> clusterMembers = new ArrayList<>();
                expandCluster(v, neighbors, index, eps, minPts, visited, clusterMembers);
                ClusterRecord cr = buildClusterRecord(clusterMembers);
                newClusters.add(cr);
            } else if (minPts <= 1) {
                // No neighbors but minPts≤1: isolated village → single-member cluster
                newClusters.add(buildClusterRecord(List.of(v)));
            }
        }

        store.setClusters(newClusters);
    }

    /**
     * Recursively expands a cluster from a core point, collecting all density-reachable
     * villages.
     */
    private static void expandCluster(
            VillageRecord seed,
            List<VillageRecord> seedNeighbors,
            GridIndex index,
            int eps,
            int minPts,
            Set<UUID> visited,
            List<VillageRecord> members) {

        members.add(seed);

        // Use a queue for BFS expansion (avoid stack overflow on large clusters)
        Deque<VillageRecord> queue = new ArrayDeque<>();
        for (VillageRecord n : seedNeighbors) {
            if (!visited.contains(n.id())) {
                visited.add(n.id());
                queue.add(n);
            }
        }

        while (!queue.isEmpty()) {
            VillageRecord current = queue.poll();
            members.add(current);

            List<VillageRecord> currentNeighbors = index.regionQuery(current, eps);
            if (currentNeighbors.size() >= minPts) {
                for (VillageRecord n : currentNeighbors) {
                    if (!visited.contains(n.id())) {
                        visited.add(n.id());
                        queue.add(n);
                    }
                }
            }
        }
    }

    /**
     * Constructs a {@link ClusterRecord} from a list of member villages.
     * The center village is the member with the highest bedCount.
     * The cluster id is derived from the center village's position.
     */
    private static ClusterRecord buildClusterRecord(List<VillageRecord> members) {
        // Find center: max bedCount
        VillageRecord center = members.get(0);
        for (VillageRecord v : members) {
            if (v.bedCount() > center.bedCount()) {
                center = v;
            }
        }

        // Build id from center position
        String clusterId = "cluster_" + positionHash(center.position());

        // Collect member IDs
        List<UUID> memberIds = new ArrayList<>();
        for (VillageRecord v : members) {
            memberIds.add(v.id());
        }

        return new ClusterRecord(clusterId, memberIds, center.id(), false, false);
    }

    /**
     * Produces a short hex hash from a Vec3i position for cluster id generation.
     */
    private static String positionHash(Vec3i pos) {
        long hash = ((long) pos.x() * 73856093)
                ^ ((long) pos.y() * 19349663)
                ^ ((long) pos.z() * 83492791);
        return Long.toHexString(hash & 0xFFFF_FFFFL);
    }

    // ── Grid Spatial Index ──────────────────────────────────────────────

    /**
     * Simple uniform grid spatial index for 2D point queries.
     *
     * <p>Bins villages into cells of size {@code cellSize}. A range query
     * only examines the cell containing the query point and its 8 neighbors.</p>
     */
    private static class GridIndex {
        private final int cellSize;
        private final Map<Long, List<VillageRecord>> grid;

        GridIndex(List<VillageRecord> villages, int cellSize) {
            this.cellSize = cellSize;
            this.grid = new HashMap<>();
            for (VillageRecord v : villages) {
                long key = cellKey(v.position());
                grid.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
            }
        }

        /**
         * Finds all villages within {@code eps} blocks of the query village (excluding itself).
         */
        List<VillageRecord> regionQuery(VillageRecord query, int eps) {
            List<VillageRecord> result = new ArrayList<>();
            Vec3i pos = query.position();
            int cx = pos.x() / cellSize;
            int cz = pos.z() / cellSize;
            double epsSq = (double) eps * eps;

            // Check 3×3 neighborhood of cells
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long key = cellKey(cx + dx, cz + dz);
                    List<VillageRecord> cell = grid.get(key);
                    if (cell != null) {
                        for (VillageRecord v : cell) {
                            if (!v.id().equals(query.id())) {
                                // Use cheaper squared-distance check
                                if (pos.distanceSq(v.position()) <= epsSq) {
                                    result.add(v);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }

        private long cellKey(Vec3i pos) {
            return cellKey(pos.x() / cellSize, pos.z() / cellSize);
        }

        private static long cellKey(int cx, int cz) {
            return ((long) cx << 32) | (cz & 0xFFFF_FFFFL);
        }
    }
}
