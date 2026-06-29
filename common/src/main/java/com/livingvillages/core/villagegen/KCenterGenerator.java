package com.livingvillages.core.villagegen;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Generates village distribution coordinates using K-Center clustering
 * and Bridson Poisson Disk Sampling.
 *
 * <p>This is a pure function: same ({@code seed}, {@code rangeX}, {@code rangeZ}, {@code cfg})
 * produces identical output. No Minecraft types are referenced.</p>
 *
 * <p>Writer of: {@code villages[]} in {@code VillageStateStore}.</p>
 */
public final class KCenterGenerator {

    private KCenterGenerator() {
        // utility class
    }

    /**
     * Generate village distribution.
     *
     * @param seed   world seed
     * @param rangeX X half-extent in blocks, e.g. 5000 → [-5000, +5000]
     * @param rangeZ Z half-extent in blocks
     * @param cfg    module config (uses kcenter.k, kcenter.rCluster, kcenter.minSeparation)
     * @return unplaced village records — id is deterministic, placed=false
     */
    public static List<VillageRecord> generateKCenters(
            long seed, int rangeX, int rangeZ, ModConfig cfg) {
        Random rng = new Random(seed);
        int k = cfg.k();
        int rCluster = cfg.rCluster();
        int minSeparation = cfg.minSeparation();
        int globalIndex = 0;

        // Phase 1: K-Center — select K cluster centers
        List<Vec3i> centers = selectKCenters(rng, k, rangeX, rangeZ);

        // Phase 2: Poisson Disk Sampling within each cluster
        List<VillageRecord> allVillages = new ArrayList<>();
        for (Vec3i center : centers) {
            List<Vec3i> sampled = poissonDiskSample(rng, center, rCluster, minSeparation);
            for (Vec3i pos : sampled) {
                UUID id = UUID.nameUUIDFromBytes(
                        ("livingvillage:" + seed + ":" + globalIndex).getBytes());
                allVillages.add(new VillageRecord(
                        id, pos, "unresolved", 0, 0L, false));
                globalIndex++;
            }
        }

        return Collections.unmodifiableList(allVillages);
    }

    /**
     * Expand village generation into a new region.
     *
     * @param seed     world seed
     * @param existing already-placed villages (for avoidance)
     * @param newMinX  new region X min
     * @param newMaxX  new region X max
     * @param newMinZ  new region Z min
     * @param newMaxZ  new region Z max
     * @param cfg      module config
     * @return new village records that do not overlap with existing
     */
    public static List<VillageRecord> expandKCenters(
            long seed,
            List<VillageRecord> existing,
            int newMinX, int newMaxX,
            int newMinZ, int newMaxZ,
            ModConfig cfg) {
        Random rng = new Random(seed);
        int k = cfg.k();
        int rCluster = cfg.rCluster();
        int minSeparation = cfg.minSeparation();

        // Determine how many centers in new region (proportional to area)
        int fullRangeX = Math.max(Math.abs(newMinX), Math.abs(newMaxX));
        int fullRangeZ = Math.max(Math.abs(newMinZ), Math.abs(newMaxZ));
        List<Vec3i> centers = selectKCenters(rng, k, fullRangeX, fullRangeZ);

        // Filter centers to those within the new region
        List<Vec3i> newRegionCenters = new ArrayList<>();
        for (Vec3i c : centers) {
            if (c.x() >= newMinX && c.x() <= newMaxX
                    && c.z() >= newMinZ && c.z() <= newMaxZ) {
                newRegionCenters.add(c);
            }
        }

        // Generate villages, filtering out those too close to existing
        List<VillageRecord> newVillages = new ArrayList<>();
        int globalIndex = existing.size(); // continue indexing

        for (Vec3i center : newRegionCenters) {
            List<Vec3i> sampled = poissonDiskSample(rng, center, rCluster, minSeparation);
            for (Vec3i pos : sampled) {
                // Check against existing villages
                boolean tooClose = false;
                for (VillageRecord ev : existing) {
                    if (pos.horizontalDistance(ev.position()) < minSeparation) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                // Also check against already-generated new villages
                for (VillageRecord nv : newVillages) {
                    if (pos.horizontalDistance(nv.position()) < minSeparation) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                UUID id = UUID.nameUUIDFromBytes(
                        ("livingvillage:" + seed + ":" + globalIndex).getBytes());
                newVillages.add(new VillageRecord(
                        id, pos, "unresolved", 0, 0L, false));
                globalIndex++;
            }
        }

        return Collections.unmodifiableList(newVillages);
    }

    // ─── Private helpers ───

    /**
     * Greedy K-Center selection.
     * First center is random; subsequent centers maximize minimum distance
     * to already-selected centers, chosen from a pool of 200 random candidates.
     */
    static List<Vec3i> selectKCenters(Random rng, int k, int rangeX, int rangeZ) {
        List<Vec3i> centers = new ArrayList<>();
        if (k <= 0) return centers;

        // First center: uniform random
        Vec3i first = new Vec3i(
                rng.nextInt(-rangeX, rangeX + 1),
                0,
                rng.nextInt(-rangeZ, rangeZ + 1));
        centers.add(first);

        // Subsequent centers: greedy max-min
        for (int i = 1; i < k; i++) {
            Vec3i best = null;
            double bestMinDist = -1;

            // Sample 200 candidates
            for (int j = 0; j < 200; j++) {
                Vec3i candidate = new Vec3i(
                        rng.nextInt(-rangeX, rangeX + 1),
                        0,
                        rng.nextInt(-rangeZ, rangeZ + 1));

                double minDist = Double.MAX_VALUE;
                for (Vec3i c : centers) {
                    double d = c.horizontalDistance(candidate);
                    if (d < minDist) minDist = d;
                }

                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    best = candidate;
                }
            }

            if (best != null) {
                centers.add(best);
            }
        }

        return centers;
    }

    /**
     * Bridson Poisson Disk Sampling within radius rCluster around center.
     *
     * @param rng           deterministic PRNG
     * @param center        cluster center
     * @param rCluster      max scatter radius
     * @param minSeparation minimum distance between villages
     * @return sampled points (Vec3i with y=0)
     */
    static List<Vec3i> poissonDiskSample(
            Random rng, Vec3i center, int rCluster, int minSeparation) {

        List<Vec3i> points = new ArrayList<>();
        List<Vec3i> active = new ArrayList<>();

        int maxPoints = (int) Math.ceil(
                Math.PI * rCluster * rCluster / (minSeparation * minSeparation));
        if (maxPoints < 1) maxPoints = 1;

        // First point: at center
        points.add(center);
        active.add(center);

        int cx = center.x();
        int cz = center.z();

        while (!active.isEmpty() && points.size() < maxPoints) {
            // Pick a random active point
            int idx = rng.nextInt(active.size());
            Vec3i current = active.get(idx);

            boolean found = false;
            for (int attempt = 0; attempt < 30; attempt++) {
                // Random point in annulus [minSeparation, 2*minSeparation] around current
                double angle = rng.nextDouble() * 2.0 * Math.PI;
                double dist = minSeparation + rng.nextDouble() * minSeparation;
                int nx = (int) Math.round(current.x() + dist * Math.cos(angle));
                int nz = (int) Math.round(current.z() + dist * Math.sin(angle));

                // Check within cluster radius from center
                double dc = Math.sqrt(
                        (double) (nx - cx) * (nx - cx) + (double) (nz - cz) * (nz - cz));
                if (dc > rCluster) continue;

                // Check minSeparation against all existing points
                boolean valid = true;
                Vec3i candidate = new Vec3i(nx, 0, nz);
                for (Vec3i p : points) {
                    if (candidate.horizontalDistance(p) < minSeparation) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    points.add(candidate);
                    active.add(candidate);
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Remove exhausted active point
                active.remove(idx);
            }
        }

        return points;
    }
}
