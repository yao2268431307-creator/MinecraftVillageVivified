package com.livingvillages.core.economy;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketSimulatorTest {

    private static final ModConfig CFG = ModConfig.defaults();

    private static VillageRecord makeVillage(UUID id, int x, int z, int beds) {
        return new VillageRecord(id, new Vec3i(x, 64, z), "plains", beds, 0, true);
    }

    private static ClusterRecord makeCluster(String id, UUID centerId, List<UUID> members) {
        return new ClusterRecord(id, members, centerId, false, true);
    }

    @Test
    void supplyGreaterThanDemand_priceBelowBasePrice() {
        InMemoryVillageStateStore store = setupStore(10, 500); // high supply
        MarketSimulator.simulateMarket(store, CFG);
        Map<String, Map<String, Double>> prices = store.getPrices();
        double foodPrice = prices.get("c1").get("food");
        assertTrue(foodPrice < 10.0, "price should be below base 10.0 when supply exceeds demand: " + foodPrice);
    }

    @Test
    void demandGreaterThanSupply_priceAboveBasePrice() {
        InMemoryVillageStateStore store = setupStore(10, 2); // very low supply
        MarketSimulator.simulateMarket(store, CFG);
        double foodPrice = store.getPrices().get("c1").get("food");
        assertTrue(foodPrice > 10.0, "price should exceed base 10.0 when demand exceeds supply: " + foodPrice);
    }

    @Test
    void emptyWarehouse_priceCappedAtBaseTimes10() {
        InMemoryVillageStateStore store = setupStore(10, 0); // zero supply
        MarketSimulator.simulateMarket(store, CFG);
        double foodPrice = store.getPrices().get("c1").get("food");
        assertEquals(100.0, foodPrice, 0.01, "empty warehouse should cap price at basePrice × 10 = 100");
    }

    @Test
    void cesPricing_formula() {
        // Test CES production function directly
        double q = CESPricing.cesProduction(10, 100, 0.7, 0.6, 1.0);
        assertTrue(q > 0, "CES output should be positive");
    }

    @Test
    void railConnection_causesPriceDiffusion() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID v1 = UUID.randomUUID(), v2 = UUID.randomUUID();
        store.setVillages(List.of(makeVillage(v1, 0, 0, 10), makeVillage(v2, 500, 0, 10)));
        store.setClusters(List.of(makeCluster("c1", v1, List.of(v1)), makeCluster("c2", v2, List.of(v2))));
        store.setWarehouses(Map.of(v1, Map.of("food", 100), v2, Map.of("food", 20)));

        // Edge connecting them
        store.setInterClusterEdges(List.of(new EdgeRecord("c1", "c2",
                List.of(new Vec3i(0, 64, 0), new Vec3i(500, 64, 0)), EdgeType.RAIL)));

        MarketSimulator.simulateMarket(store, CFG);
        double p1 = store.getPrices().get("c1").get("food");
        double p2 = store.getPrices().get("c2").get("food");
        // With diffusion, prices should converge somewhat
        assertTrue(Math.abs(p1 - p2) < 50, "connected clusters should have similar prices: " + p1 + " vs " + p2);
    }

    @Test
    void foodShortage_reducesBedCount() {
        InMemoryVillageStateStore store = setupStore(5, 1); // low food

        // Run 7 ticks
        for (int i = 0; i < 7; i++) {
            MarketSimulator.simulateMarket(store, CFG);
        }

        // bedCount should have decreased
        int beds = store.getVillages().get(0).bedCount();
        assertTrue(beds < 5, "bedCount should decrease after 7 days of food shortage, got " + beds);
    }

    @Test
    void bedCount_neverBelowOne() {
        InMemoryVillageStateStore store = setupStore(1, 0); // smallest village, no food

        for (int i = 0; i < 14; i++) {
            MarketSimulator.simulateMarket(store, CFG);
        }

        int beds = store.getVillages().get(0).bedCount();
        assertEquals(1, beds, "bedCount should never go below 1");
    }

    @Test
    void accumulatesConsumption_forLevelProgression() {
        InMemoryVillageStateStore store = setupStore(5, 100);
        MarketSimulator.simulateMarket(store, CFG);

        Map<UUID, Map<String, Long>> acc = store.getAccumulatedConsumption();
        assertNotNull(acc);
        assertFalse(acc.isEmpty());
    }

    private InMemoryVillageStateStore setupStore(int beds, int foodSupply) {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID vId = UUID.randomUUID();
        store.setVillages(List.of(makeVillage(vId, 0, 0, beds)));
        store.setClusters(List.of(makeCluster("c1", vId, List.of(vId))));
        store.setWarehouses(Map.of(vId, Map.of("food", foodSupply)));
        return store;
    }
}
