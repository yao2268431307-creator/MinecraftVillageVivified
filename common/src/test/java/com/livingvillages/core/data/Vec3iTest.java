package com.livingvillages.core.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Vec3i record.
 */
class Vec3iTest {

    @Test
    void distanceSq_origin() {
        Vec3i a = new Vec3i(0, 0, 0);
        Vec3i b = new Vec3i(3, 4, 0);
        // 3² + 4² + 0² = 25
        assertEquals(25.0, a.distanceSq(b), 1e-9);
    }

    @Test
    void distanceSq_samePoint() {
        Vec3i a = new Vec3i(10, -5, 3);
        assertEquals(0.0, a.distanceSq(a), 1e-9);
    }

    @Test
    void distanceSq_offset() {
        Vec3i a = new Vec3i(1, 2, 3);
        Vec3i b = new Vec3i(4, 6, 3);
        // (1-4)² + (2-6)² + (3-3)² = 9 + 16 + 0 = 25
        assertEquals(25.0, a.distanceSq(b), 1e-9);
    }

    @Test
    void distanceSq_negative() {
        Vec3i a = new Vec3i(-1, -2, -3);
        Vec3i b = new Vec3i(2, 2, 1);
        // (-1-2)² + (-2-2)² + (-3-1)² = 9 + 16 + 16 = 41
        assertEquals(41.0, a.distanceSq(b), 1e-9);
    }

    @Test
    void horizontalDistance_simple() {
        Vec3i a = new Vec3i(0, 0, 0);
        Vec3i b = new Vec3i(3, 100, 4);  // Y is ignored in horizontal distance
        assertEquals(5.0, a.horizontalDistance(b), 1e-9);  // sqrt(3²+4²)
    }

    @Test
    void horizontalDistance_sameXZ() {
        Vec3i a = new Vec3i(5, 10, 6);
        Vec3i b = new Vec3i(5, 999, 6);
        assertEquals(0.0, a.horizontalDistance(b), 1e-9);
    }

    @Test
    void horizontalDistance_largeValues() {
        Vec3i a = new Vec3i(-5000, 64, -5000);
        Vec3i b = new Vec3i(5000, 64, 5000);
        double expected = Math.sqrt(10000.0 * 10000.0 + 10000.0 * 10000.0);  // ~14142
        assertEquals(expected, a.horizontalDistance(b), 1e-9);
    }

    @Test
    void recordEquality() {
        Vec3i a = new Vec3i(1, 2, 3);
        Vec3i b = new Vec3i(1, 2, 3);
        Vec3i c = new Vec3i(1, 2, 4);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
