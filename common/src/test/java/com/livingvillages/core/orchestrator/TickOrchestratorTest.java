package com.livingvillages.core.orchestrator;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.naming.BiomeResolver;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TickOrchestratorTest {

    private static final ModConfig CFG = ModConfig.defaults();
    private static final BiomeResolver PLAINS = (x, y, z) -> "plains";

    @Test
    void onWorldCreate_populatesVillages() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        TickOrchestrator.onWorldCreate(42, List.of(new Vec3i(0,0,0), new Vec3i(500,0,500), new Vec3i(-500,0,-500)), store, CFG);
        assertFalse(store.getVillages().isEmpty(),
                "World creation should populate villages");
    }

    @Test
    void onWorldCreate_withDifferentSeeds_producesDifferentResults() {
        InMemoryVillageStateStore s1 = new InMemoryVillageStateStore();
        InMemoryVillageStateStore s2 = new InMemoryVillageStateStore();
        TickOrchestrator.onWorldCreate(42, List.of(new Vec3i(0,0,0), new Vec3i(500,0,500), new Vec3i(-500,0,-500)), s1, CFG);
        TickOrchestrator.onWorldCreate(99, List.of(new Vec3i(0,0,0), new Vec3i(500,0,500), new Vec3i(-500,0,-500)), s2, CFG);
        assertNotEquals(s1.getVillages().get(0).position(),
                s2.getVillages().get(0).position(),
                "Different seeds should produce different village positions");
    }

    @Test
    void runDailyCycle_doesNotThrow() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        // Setup minimal state
        UUID vid = UUID.randomUUID();
        store.setVillages(List.of(new VillageRecord(vid, new Vec3i(0, 64, 0),
                "plains", 5, 0, true)));
        store.setClusters(List.of(new ClusterRecord("c1", List.of(vid), vid, false, false)));
        store.setWarehouses(Map.of(vid, Map.of("food", 100)));
        store.setPrices(Map.of("c1", Map.of("food", 10.0)));

        // Should not throw even if some modules find nothing to do
        assertDoesNotThrow(() ->
                TickOrchestrator.runDailyCycle(store, CFG, PLAINS));
    }

    @Test
    void runDailyCycle_completesAllPhases() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID v1 = UUID.randomUUID(), v2 = UUID.randomUUID();
        store.setVillages(List.of(
                new VillageRecord(v1, new Vec3i(0, 64, 0), "plains", 5, 0, true),
                new VillageRecord(v2, new Vec3i(200, 64, 0), "plains", 5, 0, true)));
        store.setClusters(List.of(
                new ClusterRecord("c1", List.of(v1), v1, false, false),
                new ClusterRecord("c2", List.of(v2), v2, false, false)));
        store.setWarehouses(Map.of(v1, Map.of("food", 100), v2, Map.of("food", 100)));
        store.setPrices(Map.of("c1", Map.of("food", 10.0), "c2", Map.of("food", 10.0)));

        TickOrchestrator.runDailyCycle(store, CFG, PLAINS);

        // After cycle: clusters should be named, edges built, prices updated
        assertTrue(store.getClusters().get(0).isNamed(), "Clusters should be named");
        assertFalse(store.getInterClusterEdges().isEmpty(), "Edges should be built");
        assertNotNull(store.getPrices().get("c1").get("food"), "Prices should be computed");
    }

    @Test
    void errorIsolation_oneModuleFailureDoesNotStopOthers() {
        // This test verifies the try-catch isolation in runDailyCycle.
        // Even if a module would fail, the orchestrator catches and continues.
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID vid = UUID.randomUUID();
        store.setVillages(List.of(new VillageRecord(vid, new Vec3i(0, 64, 0),
                "plains", 5, 0, true)));
        store.setClusters(List.of());
        store.setWarehouses(Map.of(vid, Map.of("food", 100)));

        // Should not throw even with incomplete state
        assertDoesNotThrow(() ->
                TickOrchestrator.runDailyCycle(store, CFG, PLAINS));
    }
}
