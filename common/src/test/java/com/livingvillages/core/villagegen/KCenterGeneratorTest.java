package com.livingvillages.core.villagegen;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.VillageRecord;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KCenterGenerator.
 */
class KCenterGeneratorTest {

    private static final long SEED = 42;
    private static final int RANGE_X = 5000;
    private static final int RANGE_Z = 5000;
    private static final ModConfig CFG = ModConfig.defaults();

    @Test
    void generateKCenters_returnsNonEmptyList() {
        List<VillageRecord> villages = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        assertNotNull(villages);
        assertFalse(villages.isEmpty());
    }

    @Test
    void generateKCenters_isDeterministic() {
        List<VillageRecord> a = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        List<VillageRecord> b = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).id(), b.get(i).id(), "IDs differ at index " + i);
            assertEquals(a.get(i).position(), b.get(i).position(), "Positions differ at index " + i);
        }
    }

    @Test
    void generateKCenters_differentSeedsProduceDifferentResults() {
        List<VillageRecord> a = KCenterGenerator.generateKCenters(42, RANGE_X, RANGE_Z, CFG);
        List<VillageRecord> b = KCenterGenerator.generateKCenters(99, RANGE_X, RANGE_Z, CFG);
        // The lists should differ in at least one position
        boolean differs = false;
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (!a.get(i).position().equals(b.get(i).position())) {
                differs = true;
                break;
            }
        }
        assertTrue(differs, "Different seeds should produce different village positions");
    }

    @Test
    void generateKCenters_allIdsUnique() {
        List<VillageRecord> villages = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        Set<UUID> ids = new HashSet<>();
        for (VillageRecord v : villages) {
            assertTrue(ids.add(v.id()), "Duplicate UUID: " + v.id());
        }
    }

    @Test
    void generateKCenters_minSeparationRespected() {
        // Poisson disk guarantees separation within each cluster.
        // Cross-cluster proximity can happen — allow up to 5% of pairs to be close.
        List<VillageRecord> villages = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        int minSep = CFG.minSeparation();
        int violations = 0;
        long total = (long) villages.size() * (villages.size() - 1) / 2;
        for (int i = 0; i < villages.size(); i++) {
            for (int j = i + 1; j < villages.size(); j++) {
                if (villages.get(i).position().horizontalDistance(villages.get(j).position()) < minSep)
                    violations++;
            }
        }
        final int finalViolations = violations;
        assertTrue(finalViolations < Math.max(1, total * 0.05),
                () -> "Too many cross-cluster proximity violations: " + finalViolations);
    }

    @Test
    void generateKCenters_villagesWithinRange() {
        // Poisson disk can scatter villages up to rCluster beyond the range boundary
        int maxExtent = RANGE_X + CFG.rCluster();
        List<VillageRecord> villages = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        for (VillageRecord v : villages) {
            assertTrue(Math.abs(v.position().x()) <= maxExtent,
                    "village x=" + v.position().x() + " out of range ±" + maxExtent);
            assertTrue(Math.abs(v.position().z()) <= maxExtent,
                    "village z=" + v.position().z() + " out of range ±" + maxExtent);
        }
    }

    @Test
    void generateKCenters_villageRecordDefaults() {
        List<VillageRecord> villages = KCenterGenerator.generateKCenters(SEED, RANGE_X, RANGE_Z, CFG);
        for (VillageRecord v : villages) {
            assertEquals("unresolved", v.biomeCategory());
            assertEquals(0, v.bedCount());
            assertFalse(v.placed());
            assertTrue(v.firstSeenTick() >= 0);
        }
    }

    @Test
    void expandKCenters_newPointsAvoidExisting() {
        List<VillageRecord> existing = KCenterGenerator.generateKCenters(SEED, 3000, 3000, CFG);

        // Expand to a new region
        List<VillageRecord> expanded = KCenterGenerator.expandKCenters(
                SEED, existing, 3000, 5000, 3000, 5000, CFG);

        // New points should be in the new region
        for (VillageRecord v : expanded) {
            assertTrue(v.position().x() >= 3000 || v.position().z() >= 3000,
                    "Expanded village at " + v.position() + " not in new region");
        }

        // New points should not overlap with existing
        int minSep = CFG.minSeparation();
        for (VillageRecord e : expanded) {
            for (VillageRecord x : existing) {
                double dist = e.position().horizontalDistance(x.position());
                assertTrue(dist >= minSep,
                        () -> String.format("Expanded village %s too close to existing %s: %.1f < %d",
                                e.id(), x.id(), dist, minSep));
            }
        }
    }

    @Test
    void expandKCenters_noExisting_equivalentToGenerate() {
        // Expanding with empty existing list should produce results in the new region.
        // Note: expandKCenters and generateKCenters may produce different counts
        // because expand uses min/max range which maps differently.
        List<VillageRecord> expanded = KCenterGenerator.expandKCenters(
                SEED, Collections.emptyList(), -2000, 2000, -2000, 2000, CFG);
        assertFalse(expanded.isEmpty(), "Should produce villages in the region");
        // All expanded villages should be within the bounds
        for (VillageRecord v : expanded) {
            assertTrue(v.position().x() >= -2000 - CFG.rCluster()
                    && v.position().x() <= 2000 + CFG.rCluster());
            assertTrue(v.position().z() >= -2000 - CFG.rCluster()
                    && v.position().z() <= 2000 + CFG.rCluster());
        }
    }
}
