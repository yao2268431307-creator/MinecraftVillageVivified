package com.livingvillages.core.cluster;

import com.livingvillages.core.Hashing;
import com.livingvillages.core.geom.BlockPos2;
import com.livingvillages.core.geom.WorldBounds;
import com.livingvillages.core.naming.BiomeMood;
import com.livingvillages.core.naming.NameGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class KCenterClusterGenerator {
    private final NameGenerator nameGenerator;

    public KCenterClusterGenerator(NameGenerator nameGenerator) {
        this.nameGenerator = nameGenerator;
    }

    public List<SettlementCluster> generate(long worldSeed, ClusterConfig config) {
        List<BlockPos2> centers = generateCenters(worldSeed, config);
        List<SettlementCluster> clusters = new ArrayList<SettlementCluster>();
        for (int i = 0; i < centers.size(); i++) {
            clusters.add(generateCluster(worldSeed, config, i, centers.get(i)));
        }
        return clusters;
    }

    private List<BlockPos2> generateCenters(long worldSeed, ClusterConfig config) {
        int count = config.clusterCount();
        int columns = (int) Math.ceil(Math.sqrt(count));
        int rows = (int) Math.ceil(count / (double) columns);
        WorldBounds bounds = config.bounds();
        int cellWidth = Math.max(1, bounds.width() / columns);
        int cellDepth = Math.max(1, bounds.depth() / rows);
        List<BlockPos2> centers = new ArrayList<BlockPos2>();

        for (int i = 0; i < count; i++) {
            int col = i % columns;
            int row = i / columns;
            Random random = Hashing.random(worldSeed, 0xCEAA00L + i);
            int baseX = bounds.minX() + col * cellWidth;
            int baseZ = bounds.minZ() + row * cellDepth;
            int marginX = Math.max(16, cellWidth / 5);
            int marginZ = Math.max(16, cellDepth / 5);
            int spanX = Math.max(1, cellWidth - marginX * 2);
            int spanZ = Math.max(1, cellDepth - marginZ * 2);
            BlockPos2 center = new BlockPos2(
                    baseX + marginX + random.nextInt(spanX),
                    baseZ + marginZ + random.nextInt(spanZ)
            );
            centers.add(bounds.clamp(center));
        }
        return centers;
    }

    private SettlementCluster generateCluster(long worldSeed, ClusterConfig config, int clusterId, BlockPos2 center) {
        BiomeMood mood = nameGenerator.inferMood(center);
        String regionName = nameGenerator.regionName(worldSeed, clusterId, center, mood);
        int count = villageCount(worldSeed, config, clusterId);
        List<BlockPos2> positions = sampleVillagePositions(worldSeed, config, clusterId, center, count);
        List<VillageSite> villages = new ArrayList<VillageSite>();
        villages.add(new VillageSite(0, center, true, nameGenerator.centerVillageName(regionName, worldSeed, clusterId)));

        int nextId = 1;
        for (BlockPos2 position : positions) {
            if (!position.equals(center)) {
                BiomeMood villageMood = nameGenerator.inferMood(position);
                String name = nameGenerator.satelliteVillageName(regionName, worldSeed, clusterId, nextId, villageMood);
                villages.add(new VillageSite(nextId, position, false, name));
                nextId++;
            }
        }
        return new SettlementCluster(clusterId, center, regionName, villages);
    }

    private int villageCount(long worldSeed, ClusterConfig config, int clusterId) {
        int jitter = config.villagesPerClusterJitter();
        if (jitter == 0) {
            return config.villagesPerCluster();
        }
        Random random = Hashing.random(worldSeed, 0x517A00L + clusterId);
        return Math.max(1, config.villagesPerCluster() - jitter + random.nextInt(jitter * 2 + 1));
    }

    private List<BlockPos2> sampleVillagePositions(long worldSeed, ClusterConfig config, int clusterId, BlockPos2 center, int count) {
        Random random = Hashing.random(worldSeed, 0xF00D00L + clusterId);
        List<BlockPos2> positions = new ArrayList<BlockPos2>();
        int attempts = Math.max(64, count * 48);
        long minDistanceSq = (long) config.minVillageSeparation() * config.minVillageSeparation();

        for (int attempt = 0; attempt < attempts && positions.size() < count; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble()) * config.clusterRadius();
            BlockPos2 candidate = config.bounds().clamp(new BlockPos2(
                    center.x() + (int) Math.round(Math.cos(angle) * radius),
                    center.z() + (int) Math.round(Math.sin(angle) * radius)
            ));
            if (candidate.distanceSquaredTo(center) < minDistanceSq) {
                continue;
            }
            boolean accepted = true;
            for (BlockPos2 existing : positions) {
                if (candidate.distanceSquaredTo(existing) < minDistanceSq) {
                    accepted = false;
                    break;
                }
            }
            if (accepted) {
                positions.add(candidate);
            }
        }
        return positions;
    }
}

