package com.livingvillages.core.cluster;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClusterDetector.
 */
class ClusterDetectorTest {

    private static final ModConfig CFG = ModConfig.defaults(); // eps=250

    private static VillageRecord makeVillage(int x, int z, int beds) {
        return new VillageRecord(
                UUID.randomUUID(),
                new Vec3i(x, 64, z),
                "plains",
                beds,
                0,
                true  // placed=true so DBSCAN sees it
        );
    }

    @Test
    void villagesCloseTogether_sameCluster() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord a = makeVillage(0, 0, 5);
        VillageRecord b = makeVillage(200, 0, 3);  // distance 200 < eps 250
        store.setVillages(List.of(a, b));

        ClusterDetector.detectClusters(store, CFG);

        List<ClusterRecord> clusters = store.getClusters();
        assertEquals(1, clusters.size());
        assertEquals(2, clusters.get(0).memberVillageIds().size());
        assertTrue(clusters.get(0).memberVillageIds().contains(a.id()));
        assertTrue(clusters.get(0).memberVillageIds().contains(b.id()));
    }

    @Test
    void villagesFarApart_differentClusters() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord a = makeVillage(0, 0, 5);
        VillageRecord b = makeVillage(1000, 0, 3);  // distance 1000 > eps 250
        store.setVillages(List.of(a, b));

        ClusterDetector.detectClusters(store, CFG);

        List<ClusterRecord> clusters = store.getClusters();
        assertEquals(2, clusters.size());
    }

    @Test
    void centerVillageId_isMaxBedCount() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord a = makeVillage(0, 0, 1);
        VillageRecord b = makeVillage(100, 0, 10);  // highest bed count
        VillageRecord c = makeVillage(200, 0, 3);
        store.setVillages(List.of(a, b, c));

        ClusterDetector.detectClusters(store, CFG);

        List<ClusterRecord> clusters = store.getClusters();
        assertEquals(1, clusters.size());
        ClusterRecord cluster = clusters.get(0);
        assertEquals(b.id(), cluster.centerVillageId(),
                "Center should be village with max bedCount");
    }

    @Test
    void allVillagesAssigned_exactlyOneCluster() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        List<VillageRecord> villages = new ArrayList<>();
        Random rng = new Random(42);
        // Generate 20 villages within a 300x300 area (all should cluster together with eps=250)
        for (int i = 0; i < 20; i++) {
            villages.add(makeVillage(rng.nextInt(300), rng.nextInt(300), rng.nextInt(10) + 1));
        }
        store.setVillages(villages);

        ClusterDetector.detectClusters(store, CFG);

        List<ClusterRecord> clusters = store.getClusters();
        // Sum of all member village counts should equal total placed villages
        Set<UUID> assigned = new HashSet<>();
        for (ClusterRecord c : clusters) {
            assigned.addAll(c.memberVillageIds());
        }
        assertEquals(villages.size(), assigned.size(),
                "Every placed village should be assigned to exactly one cluster");
    }

    @Test
    void idempotent_repeatedCallsDoNotDuplicate() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord a = makeVillage(0, 0, 5);
        VillageRecord b = makeVillage(100, 0, 3);
        store.setVillages(List.of(a, b));

        ClusterDetector.detectClusters(store, CFG);
        int count1 = store.getClusters().size();

        ClusterDetector.detectClusters(store, CFG);
        int count2 = store.getClusters().size();

        assertEquals(count1, count2, "Second call should not create duplicate clusters");
    }

    @Test
    void unplacedVillages_areSkipped() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        // Unplaced village
        VillageRecord unplaced = new VillageRecord(
                UUID.randomUUID(), new Vec3i(0, 64, 0),
                "plains", 5, 0, false);
        store.setVillages(List.of(unplaced));

        ClusterDetector.detectClusters(store, CFG);

        // Should not cluster unplaced villages
        List<ClusterRecord> clusters = store.getClusters();
        for (ClusterRecord c : clusters) {
            for (UUID id : c.memberVillageIds()) {
                assertNotEquals(unplaced.id(), id,
                        "Unplaced village should not be in any cluster");
            }
        }
    }

    @Test
    void isolatedVillageCreatesSingleMemberCluster() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord isolated = makeVillage(0, 0, 5);
        store.setVillages(List.of(isolated));

        ClusterDetector.detectClusters(store, CFG);

        List<ClusterRecord> clusters = store.getClusters();
        assertEquals(1, clusters.size());
        ClusterRecord cluster = clusters.get(0);
        assertEquals(1, cluster.memberVillageIds().size());
        assertEquals(isolated.id(), cluster.memberVillageIds().get(0));
        assertEquals(isolated.id(), cluster.centerVillageId());
    }

    @Test
    void clusterRecordDefaults() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        store.setVillages(List.of(makeVillage(0, 0, 5)));

        ClusterDetector.detectClusters(store, CFG);

        ClusterRecord cluster = store.getClusters().get(0);
        assertFalse(cluster.isNamed(), "New cluster should not be named yet");
        assertFalse(cluster.edgesBuilt(), "New cluster should not have edges built yet");
        assertNotNull(cluster.id(), "Cluster id should not be null");
        assertFalse(cluster.id().isBlank());
    }

    @Test
    void preservesExistingClusters() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord a = makeVillage(0, 0, 5);
        store.setVillages(List.of(a));

        // Pre-populate with an existing cluster
        ClusterRecord existing = new ClusterRecord(
                "existing_cluster",
                List.of(a.id()),
                a.id(),
                true,
                true
        );
        store.setClusters(List.of(existing));

        // Now add a new village
        VillageRecord b = makeVillage(1000, 0, 3);
        store.setVillages(List.of(a, b));

        ClusterDetector.detectClusters(store, CFG);

        // Should still have the existing cluster + new one
        List<ClusterRecord> clusters = store.getClusters();
        boolean hasExisting = clusters.stream().anyMatch(c -> c.id().equals("existing_cluster"));
        assertTrue(hasExisting, "Existing cluster should be preserved");
    }
}
