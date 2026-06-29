package com.livingvillages.core.naming;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;

import java.util.*;

/**
 * Generates deterministic Chinese names for clusters and their member villages.
 *
 * <p>Uses a hash of the center village position to seed a PRNG, then selects
 * from biome-specific word pools. Same coordinates always produce the same name.</p>
 *
 * <p>Writer of: {@code clusterNames[]}, {@code villageNames[]}.
 * Reader of: {@code clusters[]}, {@code villages[]}.</p>
 */
public final class NameGenerator {

    private NameGenerator() {}

    /**
     * Generate names for all unnamed clusters and their member villages.
     *
     * @param store         state store for reading clusters/villages, writing names
     * @param biomeResolver resolves biome category from coordinates (MC Adapter injects)
     * @param cfg           mod config (reserved for future naming params)
     */
    public static void generateNames(
            VillageStateStore store,
            BiomeResolver biomeResolver,
            ModConfig cfg) {

        List<ClusterRecord> clusters = new ArrayList<>(store.getClusters());
        List<VillageRecord> villages = store.getVillages();

        // Build lookup maps
        Map<UUID, VillageRecord> villageById = new HashMap<>();
        for (VillageRecord v : villages) {
            villageById.put(v.id(), v);
        }

        Map<String, ClusterName> clusterNames = new HashMap<>(store.getClusterNames());
        Map<UUID, String> villageNames = new HashMap<>(store.getVillageNames());

        boolean anyNamed = false;

        for (int i = 0; i < clusters.size(); i++) {
            ClusterRecord cluster = clusters.get(i);
            if (cluster.isNamed()) {
                continue;
            }

            // Get center village position
            VillageRecord centerVillage = villageById.get(cluster.centerVillageId());
            if (centerVillage == null) {
                continue;
            }

            Vec3i pos = centerVillage.position();

            // Resolve biome category
            String biomeCategory = biomeResolver.getBiomeCategory(pos.x(), pos.y(), pos.z());
            NamePool.BiomeWordPool pool = NamePool.forCategory(biomeCategory);

            // Deterministic seed from position
            long seed = hashPosition(pos.x(), pos.z());
            Random rng = new Random(seed);

            // Generate cluster name: prefix(2 chars) + midfix(1 char) + suffix(1 char) = 4 chars
            String clusterName = pick(rng, pool.prefixes())   // 1st prefix char
                    + pick(rng, pool.prefixes())              // 2nd prefix char
                    + pick(rng, pool.midfixes())              // midfix char
                    + pick(rng, pool.suffixes());             // suffix char → total 4 chars

            String centerTownName = clusterName + "镇";

            // Generate satellite names
            Map<UUID, String> satellites = new LinkedHashMap<>();
            Set<String> usedSatelliteNames = new HashSet<>();

            for (UUID memberId : cluster.memberVillageIds()) {
                if (memberId.equals(cluster.centerVillageId())) {
                    continue; // center village already named
                }

                String satName;
                int attempts = 0;
                do {
                    // satelliteName = prefix(2 chars) + suffix(1 char) + "村" = 4 chars
                    satName = pick(rng, pool.prefixes())        // 1st prefix char
                            + pick(rng, pool.prefixes())        // 2nd prefix char
                            + pick(rng, pool.suffixes())        // suffix char
                            + "村";
                    attempts++;
                } while (usedSatelliteNames.contains(satName) && attempts < 20);

                usedSatelliteNames.add(satName);
                satellites.put(memberId, satName);
                villageNames.put(memberId, satName);
            }

            // Save village name for center town
            villageNames.put(cluster.centerVillageId(), centerTownName);

            // Create ClusterName
            ClusterName cn = new ClusterName(clusterName, centerTownName, satellites);
            clusterNames.put(cluster.id(), cn);

            // Mark cluster as named
            clusters.set(i, new ClusterRecord(
                    cluster.id(),
                    cluster.memberVillageIds(),
                    cluster.centerVillageId(),
                    true,
                    cluster.edgesBuilt()));

            anyNamed = true;
        }

        if (anyNamed) {
            store.setClusterNames(clusterNames);
            store.setVillageNames(villageNames);
            store.setClusters(clusters);
        }
    }

    /**
     * Deterministic hash of (x, z) coordinates to a long seed.
     */
    static long hashPosition(int x, int z) {
        return ((long) x << 32) ^ (long) z ^ 0x9E3779B97F4A7C15L;
    }

    /**
     * Pick a random element from the list using the given RNG.
     */
    private static String pick(Random rng, List<String> list) {
        return list.get(rng.nextInt(list.size()));
    }
}
