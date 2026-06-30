package com.livingvillages.core.caravan;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CaravanSimulatorTest {

    private static final ModConfig CFG = ModConfig.defaults();

    @Test
    void caravanProgressesThroughStates() {
        InMemoryVillageStateStore store = setupCaravanStore(CaravanPhase.LOADING, 0, 50);
        store.setWarehouses(Map.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), Map.of("food", 30),
                UUID.fromString("00000000-0000-0000-0000-000000000002"), Map.of("food", 5)));
        store.setPrices(Map.of("c1", Map.of("food", 8.0), "c2", Map.of("food", 15.0)));

        // Tick: LOADING -> MOVING
        CaravanSimulator.simulateCaravans(store, CFG);
        assertEquals(CaravanPhase.MOVING, store.getCaravanStates().get(0).phase());
    }

    @Test
    void fuelDepletes_toIdle() {
        InMemoryVillageStateStore store = setupCaravanStore(CaravanPhase.MOVING, 0, 1); // 1 fuel
        // Tick: fuel -> 0, phase -> IDLE
        CaravanSimulator.simulateCaravans(store, CFG);
        assertEquals(CaravanPhase.IDLE, store.getCaravanStates().get(0).phase());
    }

    @Test
    void unload_transfersCargoToWarehouse() {
        UUID toVillageId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        InMemoryVillageStateStore store = setupCaravanStore(CaravanPhase.UNLOADING, 0, 50);
        store.setWarehouses(Map.of(
                UUID.fromString("00000000-0000-0000-0000-000000000001"), Map.of("food", 5),
                toVillageId, Map.of("food", 10)));

        CaravanSimulator.simulateCaravans(store, CFG);
        // Cargo should have been added to toCluster warehouse
        int newStock = store.getWarehouses().get(toVillageId).get("food");
        assertTrue(newStock > 10, "warehouse should receive unloaded cargo, got " + newStock);
    }

    @Test
    void maxActiveCaravans_limitsSpawning() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();

        UUID v1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID v2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        store.setVillages(List.of(
                new VillageRecord(v1, new Vec3i(0, 64, 0), "plains", 5, 0, true),
                new VillageRecord(v2, new Vec3i(200, 64, 0), "plains", 5, 0, true)));
        store.setClusters(List.of(
                new ClusterRecord("c1", List.of(v1), v1, true, true),
                new ClusterRecord("c2", List.of(v2), v2, true, true)));
        store.setInterClusterEdges(List.of(new EdgeRecord("c1", "c2",
                List.of(new Vec3i(0, 64, 0), new Vec3i(200, 64, 0)), EdgeType.RAIL)));
        store.setPrices(Map.of("c1", Map.of("food", 8.0), "c2", Map.of("food", 15.0)));
        store.setWarehouses(Map.of(v1, Map.of("food", 100), v2, Map.of("food", 100)));

        // Fill to max
        for (int i = 0; i < CFG.maxCaravans() + 10; i++) {
            CaravanSimulator.simulateCaravans(store, CFG);
        }

        long active = store.getCaravanStates().stream()
                .filter(cs -> cs.phase() != CaravanPhase.IDLE).count();
        assertTrue(active <= CFG.maxCaravans(),
                "active caravans " + active + " should not exceed max " + CFG.maxCaravans());
    }

    @Test
    void stuckPhase_stillConsumesFuel() {
        InMemoryVillageStateStore store = setupCaravanStore(CaravanPhase.STUCK, 0, 10);
        CaravanSimulator.simulateCaravans(store, CFG);
        CaravanState cs = store.getCaravanStates().get(0);
        assertTrue(cs.fuelTicks() < 10, "STUCK should consume fuel: " + cs.fuelTicks());
    }

    @Test
    void caravanMoves_alongPath() {
        InMemoryVillageStateStore store = setupCaravanStore(CaravanPhase.MOVING, 0, 100);
        int startIndex = store.getCaravanStates().get(0).currentPathIndex();
        CaravanSimulator.simulateCaravans(store, CFG);
        int newIndex = store.getCaravanStates().get(0).currentPathIndex();
        assertTrue(newIndex > startIndex, "MOVING caravan should advance path index");
    }

    // ── Helpers ──

    private InMemoryVillageStateStore setupCaravanStore(CaravanPhase phase, int pathIndex, int fuelTicks) {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID v1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID v2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        store.setVillages(List.of(
                new VillageRecord(v1, new Vec3i(0, 64, 0), "plains", 5, 0, true),
                new VillageRecord(v2, new Vec3i(200, 64, 0), "plains", 5, 0, true)));
        store.setClusters(List.of(
                new ClusterRecord("c1", List.of(v1), v1, true, true),
                new ClusterRecord("c2", List.of(v2), v2, true, true)));
        store.setInterClusterEdges(List.of(new EdgeRecord("c1", "c2",
                List.of(new Vec3i(0, 64, 0), new Vec3i(100, 64, 0), new Vec3i(200, 64, 0)), EdgeType.RAIL)));
        store.setPrices(Map.of("c1", Map.of("food", 8.0), "c2", Map.of("food", 15.0)));
        store.setWarehouses(Map.of(v1, Map.of("food", 100), v2, Map.of("food", 100)));

        CaravanState cs = new CaravanState(UUID.randomUUID(), "c1", "c2", pathIndex, fuelTicks,
                phase == CaravanPhase.UNLOADING ? Map.of("food", 10) : Map.of(), phase);
        store.setCaravanStates(List.of(cs));
        return store;
    }
}
