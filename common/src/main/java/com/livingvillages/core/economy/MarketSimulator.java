package com.livingvillages.core.economy;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;

import java.util.*;

/**
 * Daily market economy simulation: CES pricing → graph heat diffusion → consumption / famine.
 *
 * <p>Phases:</p>
 * <ol>
 *   <li>{@link CESPricing} — local equilibrium prices per cluster</li>
 *   <li>{@link GraphHeatDiffusion} — price diffusion along rail network</li>
 *   <li>Consumption &amp; inventory update</li>
 *   <li>Famine penalty (consecutive food shortage)</li>
 * </ol>
 *
 * <p>Writer of: {@code prices[]}, {@code warehouses[]}.
 * Also writes: {@code accumulatedConsumption[]}, {@code villages[]} (bedCount adjustment).</p>
 */
public final class MarketSimulator {

    private static final int FAMINE_CONSECUTIVE_DAYS = 7;
    private static final double FAMINE_FOOD_THRESHOLD = 0.2;

    private MarketSimulator() {}

    /**
     * Run one daily market simulation cycle.
     *
     * @param store state store
     * @param cfg   config (uses cesElasticity, diffusionRate)
     */
    public static void simulateMarket(VillageStateStore store, ModConfig cfg) {
        List<ClusterRecord> clusters = store.getClusters();
        List<VillageRecord> villages = store.getVillages();
        List<EdgeRecord> edges = store.getInterClusterEdges();
        List<CaravanState> caravans = store.getCaravanStates();

        if (clusters.isEmpty()) return;

        double sigma = cfg.cesElasticity();
        double diffusionRate = cfg.diffusionRate();

        // Build lookup maps
        Map<UUID, VillageRecord> villageById = new HashMap<>();
        for (VillageRecord v : villages) {
            villageById.put(v.id(), v);
        }

        // Mutable warehouses (deep copy)
        Map<UUID, Map<String, Integer>> warehouses = deepCopyWarehouses(store.getWarehouses());
        Map<UUID, Map<String, Long>> accumulated = deepCopyAccumulated(store.getAccumulatedConsumption());

        // Phase 1: CES pricing per cluster
        List<String> clusterIds = new ArrayList<>();
        Map<String, double[]> clusterPrices = new LinkedHashMap<>();

        for (ClusterRecord cluster : clusters) {
            clusterIds.add(cluster.id());

            int population = 0;
            long totalFood = 0, totalWood = 0, totalStone = 0, totalIron = 0, totalLuxury = 0;

            for (UUID vid : cluster.memberVillageIds()) {
                VillageRecord v = villageById.get(vid);
                if (v != null) {
                    population += v.bedCount();
                }
                Map<String, Integer> wh = warehouses.getOrDefault(vid, Map.of());
                totalFood += wh.getOrDefault("food", 0);
                totalWood += wh.getOrDefault("wood", 0);
                totalStone += wh.getOrDefault("stone", 0);
                totalIron += wh.getOrDefault("iron", 0);
                totalLuxury += wh.getOrDefault("luxury", 0);
            }

            double[] prices = new double[CESPricing.COMMODITIES.size()];
            prices[0] = CESPricing.computePrice("food", population, totalFood, sigma);
            prices[1] = CESPricing.computePrice("wood", population, totalWood, sigma);
            prices[2] = CESPricing.computePrice("stone", population, totalStone, sigma);
            prices[3] = CESPricing.computePrice("iron", population, totalIron, sigma);
            prices[4] = CESPricing.computePrice("luxury", population, totalLuxury, sigma);

            clusterPrices.put(cluster.id(), prices);
        }

        // Phase 2: Graph heat diffusion for each commodity
        Map<String, Map<String, Double>> finalPrices = new LinkedHashMap<>();
        for (int c = 0; c < CESPricing.COMMODITIES.size(); c++) {
            String item = CESPricing.COMMODITIES.get(c);
            double[] initial = new double[clusterIds.size()];
            for (int i = 0; i < clusterIds.size(); i++) {
                initial[i] = clusterPrices.get(clusterIds.get(i))[c];
            }
            double[] diffused = GraphHeatDiffusion.diffuse(
                    clusterIds, initial, edges, caravans, diffusionRate);

            for (int i = 0; i < clusterIds.size(); i++) {
                finalPrices.computeIfAbsent(clusterIds.get(i), k -> new LinkedHashMap<>())
                        .put(item, diffused[i]);
            }
        }

        // Phase 3: Consumption and inventory update
        for (ClusterRecord cluster : clusters) {
            int population = 0;
            for (UUID vid : cluster.memberVillageIds()) {
                VillageRecord v = villageById.get(vid);
                if (v != null) population += v.bedCount();
            }

            Map<String, Double> prices = finalPrices.getOrDefault(cluster.id(), Map.of());

            for (UUID vid : cluster.memberVillageIds()) {
                VillageRecord v = villageById.get(vid);
                int localPop = v != null ? v.bedCount() : 0;
                double share = population > 0 ? (double) localPop / population : 0;

                Map<String, Integer> wh = new HashMap<>(warehouses.getOrDefault(vid, Map.of()));
                Map<String, Long> cons = new HashMap<>(accumulated.getOrDefault(vid, Map.of()));

                for (String item : CESPricing.COMMODITIES) {
                    double perCapita = CESPricing.PER_CAPITA_DEMAND.getOrDefault(item, 0.1);
                    double demand = localPop * perCapita;
                    double price = prices.getOrDefault(item, CESPricing.BASE_PRICES.get(item));

                    // Actual consumption: lower when price is high
                    double basePrice = CESPricing.BASE_PRICES.get(item);
                    double consumptionMultiplier = Math.max(0.1, basePrice / Math.max(price, 0.01));
                    long consumed = (long) Math.ceil(demand * consumptionMultiplier * share);

                    // Deduct from warehouse
                    int stock = wh.getOrDefault(item, 0);
                    long actualConsumed = Math.min(consumed, stock);
                    if (actualConsumed > 0) {
                        wh.put(item, stock - (int) actualConsumed);
                    }

                    // Accumulate consumption
                    String category = item.equals("food") ? "food" : "luxury";
                    cons.merge(category, actualConsumed, Long::sum);
                }

                warehouses.put(vid, wh);
                accumulated.put(vid, cons);
            }
        }

        // Phase 4: Famine penalty
        Map<UUID, Integer> famineTracker = loadFamineTracker(store);
        List<VillageRecord> updatedVillages = new ArrayList<>(villages);

        for (int i = 0; i < updatedVillages.size(); i++) {
            VillageRecord v = updatedVillages.get(i);
            Map<String, Integer> wh = warehouses.getOrDefault(v.id(), Map.of());
            int foodStock = wh.getOrDefault("food", 0);
            int population = v.bedCount();
            double demand = population * CESPricing.PER_CAPITA_DEMAND.getOrDefault("food", 0.5);

            if (foodStock < demand * FAMINE_FOOD_THRESHOLD) {
                int days = famineTracker.merge(v.id(), 1, Integer::sum);
                if (days >= FAMINE_CONSECUTIVE_DAYS) {
                    int newBedCount = Math.max(1, v.bedCount() - 1);
                    updatedVillages.set(i, new VillageRecord(
                            v.id(), v.position(), v.biomeCategory(),
                            newBedCount, v.firstSeenTick(), v.placed()));
                    famineTracker.put(v.id(), 0); // reset
                }
            } else {
                famineTracker.put(v.id(), 0); // reset
            }
        }

        // Write back
        store.setPrices(finalPrices);
        store.setWarehouses(warehouses);
        store.setAccumulatedConsumption(accumulated);
        store.setVillages(updatedVillages);
        saveFamineTracker(store, famineTracker);
    }

    // ── Helpers ──

    private static Map<UUID, Map<String, Integer>> deepCopyWarehouses(
            Map<UUID, Map<String, Integer>> src) {
        Map<UUID, Map<String, Integer>> copy = new HashMap<>();
        for (var e : src.entrySet()) {
            copy.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        return copy;
    }

    private static Map<UUID, Map<String, Long>> deepCopyAccumulated(
            Map<UUID, Map<String, Long>> src) {
        Map<UUID, Map<String, Long>> copy = new HashMap<>();
        for (var e : src.entrySet()) {
            copy.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        return copy;
    }

    // Famine tracker: we piggyback on a "hidden" field in the store
    // by encoding it in a special map accessible through a convention.
    // For now, we use an internal static map — in production this would be
    // stored in VillageStateStore or NBT.
    private static final Map<UUID, Integer> FAMINE_DAYS = new HashMap<>();

    private static Map<UUID, Integer> loadFamineTracker(VillageStateStore store) {
        return new HashMap<>(FAMINE_DAYS);
    }

    private static void saveFamineTracker(VillageStateStore store, Map<UUID, Integer> tracker) {
        FAMINE_DAYS.clear();
        FAMINE_DAYS.putAll(tracker);
    }
}
