package com.livingvillages.core.data;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryVillageStateStore get/set round-trip consistency.
 */
class InMemoryStoreTest {

    @Test
    void villages_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID id = UUID.randomUUID();
        VillageRecord v = new VillageRecord(id, new Vec3i(100, 64, 200), "plains", 5, 0L, false);
        List<VillageRecord> list = List.of(v);

        store.setVillages(list);
        List<VillageRecord> result = store.getVillages();

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).id());
        assertEquals("plains", result.get(0).biomeCategory());
    }

    @Test
    void clusters_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        ClusterRecord c = new ClusterRecord(
                "cluster_test", List.of(villageId), villageId, false, false);
        List<ClusterRecord> list = List.of(c);

        store.setClusters(list);
        List<ClusterRecord> result = store.getClusters();

        assertEquals(1, result.size());
        assertEquals("cluster_test", result.get(0).id());
        assertEquals(villageId, result.get(0).centerVillageId());
    }

    @Test
    void clusterNames_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        ClusterName name = new ClusterName("晴原", "晴原镇", Map.of(villageId, "麦浪村"));
        Map<String, ClusterName> names = Map.of("cluster_1", name);

        store.setClusterNames(names);
        Map<String, ClusterName> result = store.getClusterNames();

        assertEquals(1, result.size());
        assertEquals("晴原", result.get("cluster_1").clusterName());
        assertEquals("麦浪村", result.get("cluster_1").satelliteNames().get(villageId));
    }

    @Test
    void villageNames_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        Map<UUID, String> names = Map.of(villageId, "麦浪村");

        store.setVillageNames(names);
        Map<UUID, String> result = store.getVillageNames();

        assertEquals(1, result.size());
        assertEquals("麦浪村", result.get(villageId));
    }

    @Test
    void edges_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        List<Vec3i> path = List.of(new Vec3i(0, 64, 0), new Vec3i(100, 64, 100));
        EdgeRecord edge = new EdgeRecord("c1", "c2", path, EdgeType.RAIL);
        List<EdgeRecord> edges = List.of(edge);

        store.setInterClusterEdges(edges);
        List<EdgeRecord> result = store.getInterClusterEdges();

        assertEquals(1, result.size());
        assertEquals("c1", result.get(0).fromClusterId());
        assertEquals(EdgeType.RAIL, result.get(0).type());
        assertEquals(2, result.get(0).path().size());
    }

    @Test
    void prices_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        Map<String, Double> inner = new HashMap<>();
        inner.put("wheat", 3.5);
        inner.put("iron", 10.0);
        Map<String, Map<String, Double>> prices = Map.of("cluster_1", Collections.unmodifiableMap(inner));

        store.setPrices(prices);
        Map<String, Map<String, Double>> result = store.getPrices();

        assertEquals(1, result.size());
        assertEquals(3.5, result.get("cluster_1").get("wheat"), 1e-9);
        assertEquals(10.0, result.get("cluster_1").get("iron"), 1e-9);
    }

    @Test
    void warehouses_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        Map<String, Integer> inner = Map.of("wheat", 64, "iron", 32);
        Map<UUID, Map<String, Integer>> warehouses = Map.of(villageId, inner);

        store.setWarehouses(warehouses);
        Map<UUID, Map<String, Integer>> result = store.getWarehouses();

        assertEquals(1, result.size());
        assertEquals(64, result.get(villageId).get("wheat"));
        assertEquals(32, result.get(villageId).get("iron"));
    }

    @Test
    void caravanStates_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        CaravanState cs = new CaravanState(
                UUID.randomUUID(), "c1", "c2", 5, 1000,
                Map.of("wheat", 16), CaravanPhase.MOVING);
        List<CaravanState> list = List.of(cs);

        store.setCaravanStates(list);
        List<CaravanState> result = store.getCaravanStates();

        assertEquals(1, result.size());
        assertEquals(CaravanPhase.MOVING, result.get(0).phase());
        assertEquals(16, result.get(0).cargo().get("wheat"));
        assertEquals(1000, result.get(0).fuelTicks());
    }

    @Test
    void villageLevels_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        Map<UUID, Integer> levels = Map.of(villageId, 3);

        store.setVillageLevels(levels);
        Map<UUID, Integer> result = store.getVillageLevels();

        assertEquals(1, result.size());
        assertEquals(3, result.get(villageId));
    }

    @Test
    void accumulatedConsumption_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        Map<String, Long> inner = Map.of("wheat", 1000L, "iron", 500L);
        Map<UUID, Map<String, Long>> consumption = Map.of(villageId, inner);

        store.setAccumulatedConsumption(consumption);
        Map<UUID, Map<String, Long>> result = store.getAccumulatedConsumption();

        assertEquals(1, result.size());
        assertEquals(1000L, result.get(villageId).get("wheat"));
        assertEquals(500L, result.get(villageId).get("iron"));
    }

    @Test
    void specialities_roundTrip() {
        VillageStateStore store = new InMemoryVillageStateStore();
        UUID villageId = UUID.randomUUID();
        Map<UUID, String> specs = Map.of(villageId, "wheat");

        store.setSpecialities(specs);
        Map<UUID, String> result = store.getSpecialities();

        assertEquals(1, result.size());
        assertEquals("wheat", result.get(villageId));
    }

    @Test
    void markDirty_noOp() {
        VillageStateStore store = new InMemoryVillageStateStore();
        // Should not throw
        assertDoesNotThrow(store::markDirty);
    }

    @Test
    void emptyState_returnsEmptyCollections() {
        VillageStateStore store = new InMemoryVillageStateStore();
        assertTrue(store.getVillages().isEmpty());
        assertTrue(store.getClusters().isEmpty());
        assertTrue(store.getClusterNames().isEmpty());
        assertTrue(store.getVillageNames().isEmpty());
        assertTrue(store.getInterClusterEdges().isEmpty());
        assertTrue(store.getPrices().isEmpty());
        assertTrue(store.getWarehouses().isEmpty());
        assertTrue(store.getCaravanStates().isEmpty());
        assertTrue(store.getVillageLevels().isEmpty());
        assertTrue(store.getAccumulatedConsumption().isEmpty());
        assertTrue(store.getSpecialities().isEmpty());
    }

    @Test
    void setVillages_defensiveImmutability() {
        VillageStateStore store = new InMemoryVillageStateStore();
        List<VillageRecord> mutable = new ArrayList<>();
        mutable.add(new VillageRecord(UUID.randomUUID(), new Vec3i(0, 0, 0),
                "plains", 3, 0L, false));

        store.setVillages(mutable);

        // Mutating the original list should not affect the store
        mutable.add(new VillageRecord(UUID.randomUUID(), new Vec3i(1, 1, 1),
                "desert", 1, 0L, false));

        assertEquals(1, store.getVillages().size());
    }

    @Test
    void setVillages_resultImmutable() {
        VillageStateStore store = new InMemoryVillageStateStore();
        store.setVillages(List.of(
                new VillageRecord(UUID.randomUUID(), new Vec3i(0, 0, 0),
                        "plains", 3, 0L, false)));

        List<VillageRecord> result = store.getVillages();
        assertThrows(UnsupportedOperationException.class,
                () -> result.add(new VillageRecord(UUID.randomUUID(), new Vec3i(1, 1, 1),
                        "desert", 1, 0L, false)));
    }
}
