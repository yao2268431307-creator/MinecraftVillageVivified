package com.livingvillages.core.graph;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegionalGraph.
 */
class RegionalGraphTest {

    private static final ModConfig CFG = ModConfig.defaults(); // knnK=2

    private static VillageRecord makeVillage(UUID id, int x, int y, int z, int beds) {
        return new VillageRecord(id, new Vec3i(x, y, z), "plains", beds, 0, true);
    }

    private static ClusterRecord makeCluster(String id, UUID centerId, List<UUID> members) {
        return new ClusterRecord(id, members, centerId, false, false);
    }

    @Test
    void singleCluster_returnsNoEdges() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        UUID vId = UUID.randomUUID();
        store.setVillages(List.of(makeVillage(vId, 0, 64, 0, 5)));
        store.setClusters(List.of(makeCluster("c1", vId, List.of(vId))));

        RegionalGraph.buildRegionalGraph(store, CFG);

        assertTrue(store.getInterClusterEdges().isEmpty(),
                "Single cluster should produce no edges");
    }

    @Test
    void twoClusters_producesOneEdge() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);

        assertFalse(store.getInterClusterEdges().isEmpty());
        assertEquals(1, store.getInterClusterEdges().size());
    }

    @Test
    void edgeConnectsCorrectClusters() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);

        EdgeRecord edge = store.getInterClusterEdges().get(0);
        assertTrue(
                (edge.fromClusterId().equals("c1") && edge.toClusterId().equals("c2"))
                        || (edge.fromClusterId().equals("c2") && edge.toClusterId().equals("c1")),
                "Edge should connect c1 and c2");
    }

    @Test
    void edgeType_isRail() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);

        for (EdgeRecord edge : store.getInterClusterEdges()) {
            assertEquals(EdgeType.RAIL, edge.type(), "All edges should be RAIL type");
        }
    }

    @Test
    void edgePath_isNotEmpty() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);

        EdgeRecord edge = store.getInterClusterEdges().get(0);
        assertNotNull(edge.path());
        assertFalse(edge.path().isEmpty());
        // Path should start near the from cluster and end near the to cluster
    }

    @Test
    void marksClustersAsEdgesBuilt() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);

        for (ClusterRecord c : store.getClusters()) {
            assertTrue(c.edgesBuilt(), "Cluster " + c.id() + " should be marked edgesBuilt");
        }
    }

    @Test
    void idempotent_secondCallNoDuplication() {
        InMemoryVillageStateStore store = setupTwoClusters();

        RegionalGraph.buildRegionalGraph(store, CFG);
        int firstCount = store.getInterClusterEdges().size();

        RegionalGraph.buildRegionalGraph(store, CFG);
        int secondCount = store.getInterClusterEdges().size();

        assertEquals(firstCount, secondCount,
                "Second call should not create duplicate edges");
    }

    @Test
    void multipleClusters_producesReasonableEdgeCount() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        Random rng = new Random(42);

        List<VillageRecord> villages = new ArrayList<>();
        List<ClusterRecord> clusters = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            UUID centerId = UUID.randomUUID();
            int x = rng.nextInt(4000) - 2000;
            int z = rng.nextInt(4000) - 2000;
            villages.add(makeVillage(centerId, x, 64, z, 10));
            UUID satId = UUID.randomUUID();
            villages.add(makeVillage(satId, x + rng.nextInt(200), 64, z + rng.nextInt(200), 3));
            clusters.add(makeCluster("c" + i, centerId, List.of(centerId, satId)));
        }

        store.setVillages(villages);
        store.setClusters(clusters);

        RegionalGraph.buildRegionalGraph(store, CFG);

        // With k=2, total edges ≈ K after dedup
        List<EdgeRecord> edges = store.getInterClusterEdges();
        assertFalse(edges.isEmpty());
        // Rough check: edges should be roughly K (6) to K*1.5 (9)
        assertTrue(edges.size() >= 3, "Should have at least some edges");
        assertTrue(edges.size() <= 15, "Should not have excessive edges");
    }

    @Test
    void bezierSmoothedPath_hasNoSharpAngles() {
        // Test BezierSmoother directly
        List<Vec3i> rawPath = List.of(
                new Vec3i(0, 64, 0),
                new Vec3i(50, 65, 0),
                new Vec3i(100, 64, 0),
                new Vec3i(150, 63, 0),
                new Vec3i(200, 64, 50));

        List<Vec3i> smoothed = BezierSmoother.smooth(rawPath);
        assertFalse(smoothed.isEmpty());

        // Verify no angle < 120°
        for (int i = 1; i < smoothed.size() - 1; i++) {
            final int idx = i;
            Vec3i prev = smoothed.get(idx - 1);
            Vec3i cur = smoothed.get(idx);
            Vec3i next = smoothed.get(idx + 1);
            double angle = BezierSmoother.angleBetween(
                    cur.x() - prev.x(), cur.y() - prev.y(), cur.z() - prev.z(),
                    next.x() - cur.x(), next.y() - cur.y(), next.z() - cur.z());
            assertTrue(angle >= Math.toRadians(120) - 1e-6,
                    () -> String.format("Angle at index %d is %.1f° < 120°",
                            idx, Math.toDegrees(angle)));
        }
    }

    // ── Helpers ──

    private InMemoryVillageStateStore setupTwoClusters() {
        UUID c1Center = UUID.randomUUID();
        UUID c1Sat = UUID.randomUUID();
        UUID c2Center = UUID.randomUUID();
        UUID c2Sat = UUID.randomUUID();

        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        store.setVillages(List.of(
                makeVillage(c1Center, 0, 64, 0, 10),
                makeVillage(c1Sat, 50, 65, 50, 3),
                makeVillage(c2Center, 300, 68, 200, 8),
                makeVillage(c2Sat, 350, 67, 250, 4)));
        store.setClusters(List.of(
                makeCluster("c1", c1Center, List.of(c1Center, c1Sat)),
                makeCluster("c2", c2Center, List.of(c2Center, c2Sat))));

        return store;
    }
}
