package com.livingvillages.core.caravan;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;

import java.util.*;

/**
 * Caravan state machine (DES — Discrete Event Simulation).
 *
 * <p>Advances all active caravans by one tick. Updates fuel, position on path,
 * and undergoes state transitions. Handles loading/unloading cargo at warehouses.</p>
 *
 * <p>Writer of: {@code caravanStates[]}, {@code warehouses[]}.
 * Reader of: {@code interClusterEdges[]}, {@code prices[]}, {@code warehouses[]}.</p>
 */
public final class CaravanSimulator {

    private CaravanSimulator() {}

    /**
     * Advance all caravans by one tick.
     *
     * @param store state store
     * @param cfg   config (uses caravan.fuelTicks, caravan.maxActiveCaravans)
     */
    public static void simulateCaravans(VillageStateStore store, ModConfig cfg) {
        List<CaravanState> caravans = new ArrayList<>(store.getCaravanStates());
        List<EdgeRecord> edges = store.getInterClusterEdges();
        Map<String, Map<String, Double>> prices = store.getPrices();
        Map<UUID, Map<String, Integer>> warehouses = new HashMap<>();
        // Deep copy warehouses for mutation
        for (var entry : store.getWarehouses().entrySet()) {
            warehouses.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // Map from clusterId to list of villageIds
        Map<String, List<UUID>> clusterVillages = buildClusterVillageMap(store);

        // Edge lookup: (from, to) → EdgeRecord
        Map<String, EdgeRecord> edgeIndex = buildEdgeIndex(edges);
        // Neighbor map: clusterId → list of neighbor clusterIds
        Map<String, List<String>> neighbors = buildNeighborMap(edges);

        List<CaravanState> updated = new ArrayList<>();

        for (CaravanState c : caravans) {
            CaravanState next = tickCaravan(c, edgeIndex, neighbors, prices, warehouses,
                    clusterVillages, cfg);
            updated.add(next);
        }

        // Spawn new caravans
        int activeCount = (int) updated.stream()
                .filter(c -> c.phase() != CaravanPhase.IDLE).count();
        if (activeCount < cfg.maxCaravans() && !prices.isEmpty() && !edges.isEmpty()) {
            CaravanState spawned = spawnCaravan(prices, edges, cfg);
            if (spawned != null) {
                updated.add(spawned);
            }
        }

        store.setCaravanStates(updated);
        store.setWarehouses(warehouses);
    }

    // ── Per-caravan tick logic ──

    private static CaravanState tickCaravan(
            CaravanState c,
            Map<String, EdgeRecord> edgeIndex,
            Map<String, List<String>> neighbors,
            Map<String, Map<String, Double>> prices,
            Map<UUID, Map<String, Integer>> warehouses,
            Map<String, List<UUID>> clusterVillages,
            ModConfig cfg) {

        switch (c.phase()) {
            case IDLE:
                // Stay idle — may be spawned into LOADING by spawn logic
                return c;

            case LOADING:
                return handleLoading(c, edgeIndex, prices, warehouses, clusterVillages);

            case MOVING:
                return handleMoving(c, edgeIndex);

            case STUCK:
                return handleStuck(c);

            case UNLOADING:
                return handleUnloading(c, edgeIndex, neighbors, prices, warehouses,
                        clusterVillages, cfg);

            case RETURNING:
                return handleReturning(c, edgeIndex);

            default:
                return c;
        }
    }

    private static CaravanState handleLoading(
            CaravanState c,
            Map<String, EdgeRecord> edgeIndex,
            Map<String, Map<String, Double>> prices,
            Map<UUID, Map<String, Integer>> warehouses,
            Map<String, List<UUID>> clusterVillages) {

        // Find edge connecting from → to
        EdgeRecord edge = edgeIndex.get(edgeKey(c.fromClusterId(), c.toClusterId()));
        if (edge == null) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    0, c.fuelTicks(), Map.of(), CaravanPhase.IDLE);
        }

        // Load up to 3 commodities with largest price spread
        Map<String, Double> fromPrices = prices.getOrDefault(c.fromClusterId(), Map.of());
        Map<String, Double> toPrices = prices.getOrDefault(c.toClusterId(), Map.of());

        // Find best commodities to ship
        List<String> bestItems = findBestTradeItems(fromPrices, toPrices, 3);
        Map<String, Integer> cargo = new HashMap<>();
        Map<String, Integer> mutableCargo = new HashMap<>(c.cargo());

        for (String itemId : bestItems) {
            int loaded = takeFromWarehouse(c.fromClusterId(), itemId, 16, warehouses, clusterVillages);
            if (loaded > 0) {
                cargo.put(itemId, loaded);
            }
        }

        if (cargo.isEmpty()) {
            // Nothing to ship
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    0, c.fuelTicks(), mutableCargo, CaravanPhase.IDLE);
        }

        Map<String, Integer> combined = new HashMap<>(mutableCargo);
        cargo.forEach((k, v) -> combined.merge(k, v, Integer::sum));

        return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                0, c.fuelTicks(), combined, CaravanPhase.MOVING);
    }

    private static CaravanState handleMoving(
            CaravanState c,
            Map<String, EdgeRecord> edgeIndex) {

        int fuel = c.fuelTicks() - 1;
        if (fuel <= 0) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    c.currentPathIndex(), 0, c.cargo(), CaravanPhase.IDLE);
        }

        int nextIdx = c.currentPathIndex() + 1;
        EdgeRecord edge = edgeIndex.get(edgeKey(c.fromClusterId(), c.toClusterId()));

        if (edge != null && nextIdx >= edge.path().size()) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    nextIdx, fuel, c.cargo(), CaravanPhase.UNLOADING);
        }

        return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                nextIdx, fuel, c.cargo(), CaravanPhase.MOVING);
    }

    private static CaravanState handleStuck(CaravanState c) {
        int fuel = c.fuelTicks() - 1;
        if (fuel <= 0) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    c.currentPathIndex(), 0, c.cargo(), CaravanPhase.IDLE);
        }
        // Stay stuck — path recovery is checked by Adapter
        return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                c.currentPathIndex(), fuel, c.cargo(), CaravanPhase.STUCK);
    }

    private static CaravanState handleUnloading(
            CaravanState c,
            Map<String, EdgeRecord> edgeIndex,
            Map<String, List<String>> neighbors,
            Map<String, Map<String, Double>> prices,
            Map<UUID, Map<String, Integer>> warehouses,
            Map<String, List<UUID>> clusterVillages,
            ModConfig cfg) {

        // Deposit cargo into toCluster warehouse
        addToWarehouse(c.toClusterId(), c.cargo(), warehouses, clusterVillages);

        // Refuel
        int fuel = cfg.fuelTicks();

        // Choose next destination
        List<String> nbList = neighbors.getOrDefault(c.toClusterId(), List.of());
        Map<String, Double> fromPrices = prices.getOrDefault(c.toClusterId(), Map.of());

        // Find neighbor with largest price spread
        String bestNeighbor = null;
        double bestSpread = 0;
        for (String nb : nbList) {
            if (nb.equals(c.fromClusterId())) continue; // don't immediately go back
            Map<String, Double> nbPrices = prices.getOrDefault(nb, Map.of());
            double spread = maxPriceSpread(fromPrices, nbPrices);
            if (spread > bestSpread) {
                bestSpread = spread;
                bestNeighbor = nb;
            }
        }

        if (bestNeighbor != null) {
            // Continue to next cluster
            return new CaravanState(c.caravanId(), c.toClusterId(), bestNeighbor,
                    0, fuel, Map.of(), CaravanPhase.LOADING);
        }

        // No suitable neighbor: return to origin
        if (!c.toClusterId().equals(c.fromClusterId())) {
            return new CaravanState(c.caravanId(), c.toClusterId(), c.fromClusterId(),
                    0, fuel, Map.of(), CaravanPhase.RETURNING);
        }

        return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                0, fuel, Map.of(), CaravanPhase.IDLE);
    }

    private static CaravanState handleReturning(
            CaravanState c,
            Map<String, EdgeRecord> edgeIndex) {

        int fuel = c.fuelTicks() - 1;
        if (fuel <= 0) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    c.currentPathIndex(), 0, c.cargo(), CaravanPhase.IDLE);
        }

        int nextIdx = c.currentPathIndex() + 1;
        EdgeRecord edge = edgeIndex.get(edgeKey(c.fromClusterId(), c.toClusterId()));

        if (edge != null && nextIdx >= edge.path().size()) {
            return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                    0, fuel, c.cargo(), CaravanPhase.IDLE);
        }

        return new CaravanState(c.caravanId(), c.fromClusterId(), c.toClusterId(),
                nextIdx, fuel, c.cargo(), CaravanPhase.RETURNING);
    }

    // ── Spawning ──

    private static CaravanState spawnCaravan(
            Map<String, Map<String, Double>> prices,
            List<EdgeRecord> edges,
            ModConfig cfg) {

        // Find edge with largest price spread
        EdgeRecord bestEdge = null;
        double bestSpread = 0;

        for (EdgeRecord e : edges) {
            Map<String, Double> fromP = prices.getOrDefault(e.fromClusterId(), Map.of());
            Map<String, Double> toP = prices.getOrDefault(e.toClusterId(), Map.of());
            double spread = maxPriceSpread(fromP, toP);
            if (spread > bestSpread) {
                bestSpread = spread;
                bestEdge = e;
            }
        }

        if (bestEdge == null || bestSpread <= 0) return null;

        UUID id = UUID.randomUUID();
        return new CaravanState(id, bestEdge.fromClusterId(), bestEdge.toClusterId(),
                0, cfg.fuelTicks(), Map.of(), CaravanPhase.LOADING);
    }

    // ── Warehouse helpers ──

    private static int takeFromWarehouse(
            String clusterId, String itemId, int amount,
            Map<UUID, Map<String, Integer>> warehouses,
            Map<String, List<UUID>> clusterVillages) {

        List<UUID> villageIds = clusterVillages.getOrDefault(clusterId, List.of());
        int taken = 0;
        for (UUID vid : villageIds) {
            Map<String, Integer> wh = warehouses.get(vid);
            if (wh == null) continue;
            int available = wh.getOrDefault(itemId, 0);
            int take = Math.min(amount - taken, available);
            if (take > 0) {
                wh.put(itemId, available - take);
                taken += take;
            }
            if (taken >= amount) break;
        }
        return taken;
    }

    private static void addToWarehouse(
            String clusterId, Map<String, Integer> cargo,
            Map<UUID, Map<String, Integer>> warehouses,
            Map<String, List<UUID>> clusterVillages) {

        List<UUID> villageIds = clusterVillages.getOrDefault(clusterId, List.of());
        if (villageIds.isEmpty()) return;
        // Add to the center village (first in list)
        UUID centerId = villageIds.get(0);
        Map<String, Integer> wh = warehouses.computeIfAbsent(centerId, k -> new HashMap<>());
        for (var entry : cargo.entrySet()) {
            wh.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    // ── Price analysis ──

    private static List<String> findBestTradeItems(
            Map<String, Double> fromPrices, Map<String, Double> toPrices, int limit) {

        record ItemSpread(String itemId, double spread) {}
        List<ItemSpread> spreads = new ArrayList<>();

        for (String itemId : fromPrices.keySet()) {
            double from = fromPrices.getOrDefault(itemId, 0.0);
            double to = toPrices.getOrDefault(itemId, from);
            if (to > from) {
                spreads.add(new ItemSpread(itemId, to - from));
            }
        }

        spreads.sort((a, b) -> Double.compare(b.spread, a.spread));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, spreads.size()); i++) {
            result.add(spreads.get(i).itemId);
        }
        return result;
    }

    private static double maxPriceSpread(
            Map<String, Double> from, Map<String, Double> to) {
        double max = 0;
        for (String itemId : from.keySet()) {
            double f = from.getOrDefault(itemId, 0.0);
            double t = to.getOrDefault(itemId, f);
            max = Math.max(max, Math.abs(t - f));
        }
        return max;
    }

    // ── Index builders ──

    static Map<String, EdgeRecord> buildEdgeIndex(List<EdgeRecord> edges) {
        Map<String, EdgeRecord> index = new HashMap<>();
        for (EdgeRecord e : edges) {
            index.put(edgeKey(e.fromClusterId(), e.toClusterId()), e);
        }
        return index;
    }

    static Map<String, List<String>> buildNeighborMap(List<EdgeRecord> edges) {
        Map<String, List<String>> map = new HashMap<>();
        for (EdgeRecord e : edges) {
            map.computeIfAbsent(e.fromClusterId(), k -> new ArrayList<>()).add(e.toClusterId());
            map.computeIfAbsent(e.toClusterId(), k -> new ArrayList<>()).add(e.fromClusterId());
        }
        return map;
    }

    static Map<String, List<UUID>> buildClusterVillageMap(VillageStateStore store) {
        Map<String, List<UUID>> map = new HashMap<>();
        for (ClusterRecord c : store.getClusters()) {
            map.put(c.id(), new ArrayList<>(c.memberVillageIds()));
        }
        return map;
    }

    static String edgeKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
    }
}
