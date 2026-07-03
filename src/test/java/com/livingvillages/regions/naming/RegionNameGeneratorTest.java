package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RegionNameGenerator}.
 *
 * <p>These tests exercise the pure deterministic naming helpers, so no
 * Minecraft runtime is required. They verify: determinism, coordinate /
 * seed sensitivity, output format, null-safety, and coverage of all
 * {@link RegionType} values.</p>
 */
class RegionNameGeneratorTest {

    private static final long SEED = 0x5EED_5EEDL;

    // ---------- 1. Determinism: regionName ----------

    @Test
    @DisplayName("regionName is deterministic for the same seed + coords + type")
    void regionNameIsDeterministic() {
        String first = RegionNameGenerator.regionName(SEED, 12, -7, RegionType.DESERT);
        String second = RegionNameGenerator.regionName(SEED, 12, -7, RegionType.DESERT);
        assertEquals(first, second, "same inputs must yield the same region name");
    }

    // ---------- 2. Different coords → different regionName ----------

    @Test
    @DisplayName("regionName differs when superX/superZ differ (same seed, same type)")
    void regionNameDiffersByCoords() {
        String a = RegionNameGenerator.regionName(SEED, 0, 0, RegionType.SNOWY);
        String b = RegionNameGenerator.regionName(SEED, 1, 0, RegionType.SNOWY);
        String c = RegionNameGenerator.regionName(SEED, 0, 1, RegionType.SNOWY);
        assertNotEquals(a, b, "(0,0) vs (1,0) should differ");
        assertNotEquals(a, c, "(0,0) vs (0,1) should differ");
        assertNotEquals(b, c, "(1,0) vs (0,1) should differ");
    }

    // ---------- 3. Different seed → different regionName ----------

    @Test
    @DisplayName("regionName differs when worldSeed differs (same coords, same type)")
    void regionNameDiffersBySeed() {
        String a = RegionNameGenerator.regionName(SEED, 42, 42, RegionType.PLAINS);
        String b = RegionNameGenerator.regionName(SEED + 1, 42, 42, RegionType.PLAINS);
        assertNotEquals(a, b, "different seed should (almost always) yield a different name");
    }

    // ---------- 4. villageName determinism ----------

    @Test
    @DisplayName("villageName is deterministic for the same seed + coords + type")
    void villageNameIsDeterministic() {
        String first = RegionNameGenerator.villageName(SEED, 100, -200, RegionType.JUNGLE);
        String second = RegionNameGenerator.villageName(SEED, 100, -200, RegionType.JUNGLE);
        assertEquals(first, second, "same inputs must yield the same village name");
    }

    // ---------- 5. villageName different coords → different result ----------

    @Test
    @DisplayName("villageName differs when chunkX/chunkZ differ (same seed, same type)")
    void villageNameDiffersByCoords() {
        String a = RegionNameGenerator.villageName(SEED, 0, 0, RegionType.MOUNTAIN);
        String b = RegionNameGenerator.villageName(SEED, 1, 0, RegionType.MOUNTAIN);
        String c = RegionNameGenerator.villageName(SEED, 0, 1, RegionType.MOUNTAIN);
        assertNotEquals(a, b, "(0,0) vs (1,0) should differ");
        assertNotEquals(a, c, "(0,0) vs (0,1) should differ");
        assertNotEquals(b, c, "(1,0) vs (0,1) should differ");
    }

    // ---------- 6. fullDisplay format ----------

    @Test
    @DisplayName("fullDisplay joins region and village with the Chinese em-dash separator")
    void fullDisplayFormat() {
        String full = RegionNameGenerator.fullDisplay("风沙之地", "赤焰村");
        assertEquals("风沙之地——赤焰村", full);
    }

    // ---------- 7. regionName format: contains "之" midfix ----------

    @Test
    @DisplayName("regionName contains the \"之\" midfix")
    void regionNameContainsZhiMidfix() {
        String name = RegionNameGenerator.regionName(SEED, 5, 5, RegionType.TAIGA);
        assertNotNull(name);
        assertTrue(name.contains("之"),
            "regionName should contain 之 midfix, got: " + name);
    }

    // ---------- 8. villageName format: ends with a village suffix ----------

    @Test
    @DisplayName("villageName ends with one of 村/镇/集/庄/堡")
    void villageNameEndsWithVillageSuffix() {
        String name = RegionNameGenerator.villageName(SEED, 7, -3, RegionType.FOREST);
        assertNotNull(name);
        assertFalse(name.isEmpty());
        char last = name.charAt(name.length() - 1);
        assertTrue(last == '村' || last == '镇' || last == '集' || last == '庄' || last == '堡',
            "villageName should end with a village suffix, got: " + name);
    }

    // ---------- 9. null safety ----------

    @Test
    @DisplayName("regionName with null type returns the fallback \"未知之地\"")
    void regionNameNullTypeFallback() {
        assertEquals("未知之地", RegionNameGenerator.regionName(SEED, 0, 0, null));
    }

    @Test
    @DisplayName("villageName with null type returns the fallback \"无名村\"")
    void villageNameNullTypeFallback() {
        assertEquals("无名村", RegionNameGenerator.villageName(SEED, 0, 0, null));
    }

    // ---------- 10. Every RegionType produces a valid name ----------

    @Test
    @DisplayName("Every RegionType yields a non-empty regionName with the 之 midfix")
    void everyTypeProducesValidRegionName() {
        for (RegionType type : RegionType.values()) {
            String name = RegionNameGenerator.regionName(SEED, 3, 9, type);
            assertNotNull(name, "regionName null for " + type);
            assertFalse(name.isEmpty(), "regionName empty for " + type);
            assertTrue(name.contains("之"),
                type + " regionName should contain 之 midfix, got: " + name);
        }
    }

    @Test
    @DisplayName("Every RegionType yields a non-empty villageName ending in a village suffix")
    void everyTypeProducesValidVillageName() {
        for (RegionType type : RegionType.values()) {
            String name = RegionNameGenerator.villageName(SEED, 11, 17, type);
            assertNotNull(name, "villageName null for " + type);
            assertFalse(name.isEmpty(), "villageName empty for " + type);
            char last = name.charAt(name.length() - 1);
            assertTrue(last == '村' || last == '镇' || last == '集' || last == '庄' || last == '堡',
                type + " villageName should end with a village suffix, got: " + name);
        }
    }
}
