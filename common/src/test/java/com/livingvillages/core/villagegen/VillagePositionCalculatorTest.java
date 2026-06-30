package com.livingvillages.core.villagegen;

import com.livingvillages.core.data.Vec3i;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VillagePositionCalculatorTest {
    @Test
    void producesExpectedCount() {
        List<Vec3i> positions = VillagePositionCalculator.computeVanillaPositions(42, 5000);
        assertFalse(positions.isEmpty());
        // Within 5000 blocks, vanilla spacing=34 chunks=544 blocks, we expect roughly
        // (10000/544)^2 ≈ 338 positions in the full square, minus biome exclusions
        assertTrue(positions.size() > 50, "Should have many villages: " + positions.size());
    }

    @Test
    void deterministic() {
        List<Vec3i> a = VillagePositionCalculator.computeVanillaPositions(42, 3000);
        List<Vec3i> b = VillagePositionCalculator.computeVanillaPositions(42, 3000);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) assertEquals(a.get(i), b.get(i));
    }

    @Test
    void withinRadius() {
        int radius = 2000;
        for (Vec3i v : VillagePositionCalculator.computeVanillaPositions(42, radius)) {
            assertTrue(Math.abs(v.x()) <= radius, "x out of bounds: " + v.x());
            assertTrue(Math.abs(v.z()) <= radius, "z out of bounds: " + v.z());
        }
    }
}
