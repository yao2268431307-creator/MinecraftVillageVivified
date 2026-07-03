package com.livingvillages.regions.naming;

import com.livingvillages.regions.biome.RegionType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RegionNamePool}.
 *
 * <p>These tests verify the integrity of the static word pools: every
 * {@link RegionType} is present, each pool has the required minimum
 * entries, and the lookup helper behaves correctly. No MC runtime is
 * required since {@link RegionType} is a plain enum.</p>
 */
class RegionNamePoolTest {

    private static final int MIN_PREFIXES = 4;
    private static final int MIN_PREFIXES_SPECIFIC = 8;
    private static final int MIN_SUFFIXES = 3;
    private static final int EXPECTED_VILLAGE_SUFFIXES = 5;

    @Test
    @DisplayName("POOLS contains all 10 RegionType values")
    void poolsContainsAllRegionTypes() {
        EnumSet<RegionType> expected = EnumSet.allOf(RegionType.class);
        EnumSet<RegionType> actual = EnumSet.copyOf(RegionNamePool.POOLS.keySet());
        assertEquals(expected, actual, "POOLS should contain every RegionType");
    }

    @Test
    @DisplayName("Every pool has a non-empty prefix list with at least 4 entries")
    void everyPoolHasEnoughPrefixes() {
        for (Map.Entry<RegionType, RegionNamePool.RegionWords> entry : RegionNamePool.POOLS.entrySet()) {
            RegionNamePool.RegionWords words = entry.getValue();
            assertNotNull(words.prefixes(),
                "prefixes null for " + entry.getKey());
            assertFalse(words.prefixes().isEmpty(),
                "prefixes empty for " + entry.getKey());
            assertTrue(words.prefixes().size() >= MIN_PREFIXES,
                "prefixes for " + entry.getKey() + " has only "
                    + words.prefixes().size() + " entries, need ≥" + MIN_PREFIXES);
        }
    }

    @Test
    @DisplayName("Specific biome pools (non-OTHER) have at least 8 prefixes")
    void specificBiomesHaveEightPlusPrefixes() {
        for (Map.Entry<RegionType, RegionNamePool.RegionWords> entry : RegionNamePool.POOLS.entrySet()) {
            if (entry.getKey() == RegionType.OTHER) {
                continue;
            }
            int size = entry.getValue().prefixes().size();
            assertTrue(size >= MIN_PREFIXES_SPECIFIC,
                entry.getKey() + " has " + size + " prefixes, need ≥"
                    + MIN_PREFIXES_SPECIFIC);
        }
    }

    @Test
    @DisplayName("Every pool has a non-null, non-empty midfix")
    void everyPoolHasMidfix() {
        for (Map.Entry<RegionType, RegionNamePool.RegionWords> entry : RegionNamePool.POOLS.entrySet()) {
            String midfix = entry.getValue().midfix();
            assertNotNull(midfix, "midfix null for " + entry.getKey());
            assertFalse(midfix.isEmpty(), "midfix empty for " + entry.getKey());
        }
    }

    @Test
    @DisplayName("Every pool midfix is \"之\" (per DESIGN.md)")
    void everyPoolMidfixIsZhi() {
        for (Map.Entry<RegionType, RegionNamePool.RegionWords> entry : RegionNamePool.POOLS.entrySet()) {
            assertEquals("之", entry.getValue().midfix(),
                entry.getKey() + " midfix should be 之");
        }
    }

    @Test
    @DisplayName("Every pool has a non-empty suffix list with at least 3 entries")
    void everyPoolHasEnoughSuffixes() {
        for (Map.Entry<RegionType, RegionNamePool.RegionWords> entry : RegionNamePool.POOLS.entrySet()) {
            RegionNamePool.RegionWords words = entry.getValue();
            assertNotNull(words.suffixes(), "suffixes null for " + entry.getKey());
            assertFalse(words.suffixes().isEmpty(), "suffixes empty for " + entry.getKey());
            assertTrue(words.suffixes().size() >= MIN_SUFFIXES,
                "suffixes for " + entry.getKey() + " has only "
                    + words.suffixes().size() + " entries, need ≥" + MIN_SUFFIXES);
        }
    }

    @Test
    @DisplayName("VILLAGE_SUFFIXES contains exactly 5 entries: 村/镇/集/庄/堡")
    void villageSuffixesHasFiveEntries() {
        assertEquals(EXPECTED_VILLAGE_SUFFIXES, RegionNamePool.VILLAGE_SUFFIXES.size());
        assertEquals(List.of("村", "镇", "集", "庄", "堡"),
            RegionNamePool.VILLAGE_SUFFIXES);
    }

    @Test
    @DisplayName("forType(DESERT) returns a non-null pool")
    void forTypeDesertReturnsNonNull() {
        RegionNamePool.RegionWords words = RegionNamePool.forType(RegionType.DESERT);
        assertNotNull(words);
        assertFalse(words.prefixes().isEmpty());
        assertFalse(words.suffixes().isEmpty());
    }

    @Test
    @DisplayName("forType(null) returns null (no NPE)")
    void forTypeNullReturnsNull() {
        assertNull(RegionNamePool.forType(null));
    }

    @Test
    @DisplayName("forType returns the same instance as POOLS.get for each type")
    void forTypeMatchesPoolsGet() {
        for (RegionType type : RegionType.values()) {
            assertEquals(RegionNamePool.POOLS.get(type),
                RegionNamePool.forType(type),
                "forType mismatch for " + type);
        }
    }
}
