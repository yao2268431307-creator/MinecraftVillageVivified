package com.livingvillages.core;

import com.livingvillages.core.cluster.ClusterConfig;
import com.livingvillages.core.cluster.KCenterClusterGenerator;
import com.livingvillages.core.cluster.SettlementCluster;
import com.livingvillages.core.cluster.VillageSite;
import com.livingvillages.core.economy.Good;
import com.livingvillages.core.economy.MarketState;
import com.livingvillages.core.economy.PriceDiffusion;
import com.livingvillages.core.economy.TradeOpportunity;
import com.livingvillages.core.economy.TradeRouteFinder;
import com.livingvillages.core.geom.WorldBounds;
import com.livingvillages.core.graph.NetworkPlanner;
import com.livingvillages.core.graph.RegionalGraph;
import com.livingvillages.core.naming.NameGenerator;
import com.livingvillages.core.naming.NamePool;
import com.livingvillages.core.worldgen.ClusteredVillagePlacementPlanner;
import com.livingvillages.core.worldgen.PlannedVillagePlacement;

import java.util.ArrayList;
import java.util.List;

public final class CoreTestRunner {
    public static void main(String[] args) {
        deterministicClusters();
        villagesRespectClusterRadius();
        placementPlanMarksOnlyCandidateChunks();
        graphAndEconomyProduceRoutes();
        System.out.println("All Living Villages core tests passed.");
    }

    private static void deterministicClusters() {
        KCenterClusterGenerator generator = generator();
        ClusterConfig config = config();
        List<SettlementCluster> first = generator.generate(12345L, config);
        List<SettlementCluster> second = generator.generate(12345L, config);
        assertEquals(first.size(), second.size(), "cluster count");
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).center(), second.get(i).center(), "center at " + i);
            assertEquals(first.get(i).regionName(), second.get(i).regionName(), "region name at " + i);
            assertEquals(first.get(i).villages().size(), second.get(i).villages().size(), "village count at " + i);
        }
    }

    private static void villagesRespectClusterRadius() {
        KCenterClusterGenerator generator = generator();
        ClusterConfig config = config();
        List<SettlementCluster> clusters = generator.generate(42L, config);
        assertEquals(5, clusters.size(), "configured clusters");
        for (SettlementCluster cluster : clusters) {
            assertTrue(cluster.centerVillage() != null, "center village exists");
            for (VillageSite village : cluster.villages()) {
                assertTrue(village.position().distanceTo(cluster.center()) <= config.clusterRadius() + 1.0,
                        "village within cluster radius");
                assertTrue(village.name() != null && !village.name().isEmpty(), "village has name");
            }
        }
    }

    private static void graphAndEconomyProduceRoutes() {
        List<SettlementCluster> clusters = generator().generate(77L, config());
        RegionalGraph graph = new NetworkPlanner().planKNearest(clusters, 2);
        assertTrue(graph.edges().size() >= clusters.size() * 2, "knn graph has directed edges");

        List<MarketState> markets = new ArrayList<MarketState>();
        for (SettlementCluster cluster : clusters) {
            MarketState market = new MarketState(cluster.id());
            market.setSupply(Good.FOOD, cluster.id() == 0 ? 40.0 : 8.0);
            market.setDemand(Good.FOOD, cluster.id() == 0 ? 8.0 : 40.0);
            markets.add(market);
        }

        PriceDiffusion diffusion = new PriceDiffusion(0.25, 0.8);
        diffusion.initializeLocalPrices(markets, Good.FOOD);
        diffusion.diffuse(graph, markets, Good.FOOD, 4);

        List<TradeOpportunity> routes = new TradeRouteFinder().findProfitableRoutes(graph, markets, 0.0001);
        assertTrue(!routes.isEmpty(), "profitable trade routes exist");
        assertEquals(Good.FOOD, routes.get(0).good(), "top good");
    }

    private static void placementPlanMarksOnlyCandidateChunks() {
        ClusteredVillagePlacementPlanner planner = new ClusteredVillagePlacementPlanner(generator());
        ClusteredVillagePlacementPlanner.Plan plan = planner.plan(20260629L, config());
        assertTrue(!plan.placements().isEmpty(), "planned placements exist");
        for (PlannedVillagePlacement placement : plan.placements()) {
            assertTrue(plan.isVillageChunk(placement.chunk().x(), placement.chunk().z()), "candidate chunk is marked");
        }
        assertTrue(!plan.isVillageChunk(999_999, 999_999), "far chunk is not marked");
    }

    private static KCenterClusterGenerator generator() {
        return new KCenterClusterGenerator(new NameGenerator(new NamePool()));
    }

    private static ClusterConfig config() {
        return new ClusterConfig(5, 200, 7, 1, 32, new WorldBounds(-2400, -2400, 2400, 2400));
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
