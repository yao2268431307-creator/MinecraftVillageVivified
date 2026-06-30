package com.livingvillages.core.villagegen;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageRecord;

import java.util.*;

/**
 * Generates clustered village positions from raw vanilla positions.
 *
 * <p>New approach (v1.1): instead of generating villages from scratch,
 * we take the vanilla village positions (computed by the MC adapter),
 * cluster them with proximity-based grouping, and redirect each village
 * toward its cluster center — preserving the total village count that
 * vanilla would have generated.</p>
 *
 * <p>K = ceil(villageCount / villagesPerCluster). Each cluster center
 * becomes the anchor, and member villages are pulled toward it while
 * maintaining min_separation.</p>
 */
public final class KCenterGenerator {

    private KCenterGenerator() {}

    /**
     * Cluster vanilla village positions and redistribute them around centers.
     *
     * @param seed              world seed
     * @param vanillaPositions  raw village positions from vanilla structure system
     * @param cfg               config (uses clusterRadius, minSeparation, villagesPerCluster)
     * @return clustered VillageRecords ready for placement
     */
    public static List<VillageRecord> generateClusteredVillages(
            long seed,
            List<Vec3i> vanillaPositions,
            ModConfig cfg) {

        if (vanillaPositions.isEmpty()) return List.of();

        int villagesPerCluster = cfg.villagesPerCluster();
        int k = Math.max(1, (int) Math.ceil((double) vanillaPositions.size() / villagesPerCluster));
        int clusterRadius = cfg.clusterRadius();
        int minSeparation = cfg.minSeparation();

        // Step 1: Select K cluster centers from vanilla positions using greedy max-min
        List<Vec3i> centers = selectClusterCenters(vanillaPositions, k, seed);

        // Step 2: Assign each village to nearest center
        Map<Integer, List<Vec3i>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < k; i++) clusters.put(i, new ArrayList<>());
        for (Vec3i pos : vanillaPositions) {
            int nearest = 0;
            double minDist = Double.MAX_VALUE;
            for (int i = 0; i < centers.size(); i++) {
                double d = pos.horizontalDistance(centers.get(i));
                if (d < minDist) { minDist = d; nearest = i; }
            }
            clusters.get(nearest).add(pos);
        }

        // Step 3: Redirect each village toward its cluster center
        List<VillageRecord> result = new ArrayList<>();
        int globalIndex = 0;
        Random rng = new Random(seed);

        for (int ci = 0; ci < k; ci++) {
            Vec3i center = centers.get(ci);
            List<Vec3i> members = clusters.get(ci);

            // Center village (anchor)
            UUID centerId = UUID.nameUUIDFromBytes(
                    ("livingvillage:" + seed + ":c:" + ci).getBytes());
            result.add(new VillageRecord(centerId, center, "unresolved", 0, 0L, false));

            // Satellite villages: pull toward center
            for (Vec3i originalPos : members) {
                // Pull factor: how close to the center (0=far, 1=at center)
                double distance = originalPos.horizontalDistance(center);
                double pullFactor = Math.min(1.0, clusterRadius / Math.max(1, distance));
                // Blend: pulled position between original and center
                int nx = (int) (originalPos.x() + (center.x() - originalPos.x()) * pullFactor);
                int nz = (int) (originalPos.z() + (center.z() - originalPos.z()) * pullFactor);
                // Add jitter within minSeparation
                double angle = rng.nextDouble() * 2 * Math.PI;
                double jitter = rng.nextDouble() * minSeparation * 0.5;
                nx += (int) (Math.cos(angle) * jitter);
                nz += (int) (Math.sin(angle) * jitter);

                Vec3i redirected = new Vec3i(nx, 0, nz);
                UUID id = UUID.nameUUIDFromBytes(
                        ("livingvillage:" + seed + ":" + globalIndex).getBytes());
                result.add(new VillageRecord(id, redirected, "unresolved", 0, 0L, false));
                globalIndex++;
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Greedy max-min selection of K cluster centers from candidate positions.
     */
    private static List<Vec3i> selectClusterCenters(List<Vec3i> positions, int k, long seed) {
        if (positions.size() <= k) return new ArrayList<>(positions);

        Random rng = new Random(seed);
        List<Vec3i> centers = new ArrayList<>();
        Set<Integer> used = new HashSet<>();

        // First center: random
        int firstIdx = rng.nextInt(positions.size());
        centers.add(positions.get(firstIdx));
        used.add(firstIdx);

        // Remaining: greedy max-min from random candidates
        for (int i = 1; i < k; i++) {
            int best = -1;
            double bestMinDist = -1;
            int candidates = Math.min(50, positions.size());
            for (int c = 0; c < candidates; c++) {
                int idx = rng.nextInt(positions.size());
                if (used.contains(idx)) continue;
                Vec3i p = positions.get(idx);
                double minDist = Double.MAX_VALUE;
                for (Vec3i center : centers) {
                    minDist = Math.min(minDist, p.horizontalDistance(center));
                }
                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    best = idx;
                }
            }
            if (best >= 0) {
                centers.add(positions.get(best));
                used.add(best);
            }
        }

        return centers;
    }
}
