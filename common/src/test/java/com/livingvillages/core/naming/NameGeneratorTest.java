package com.livingvillages.core.naming;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for NameGenerator.
 */
class NameGeneratorTest {

    private static final ModConfig CFG = ModConfig.defaults();

    // Simple biome resolver that returns "plains" for everything
    private static final BiomeResolver PLAINS_RESOLVER = (x, y, z) -> "plains";

    // Desert resolver
    private static final BiomeResolver DESERT_RESOLVER = (x, y, z) -> "desert";

    private static VillageRecord makeVillage(UUID id, int x, int z, int beds) {
        return new VillageRecord(id, new Vec3i(x, 64, z), "plains", beds, 0, true);
    }

    @Test
    void deterministic_sameCoordinatesSameName() {
        InMemoryVillageStateStore store1 = setupStore(100, 200);
        NameGenerator.generateNames(store1, PLAINS_RESOLVER, CFG);
        String name1 = store1.getClusterNames().values().iterator().next().clusterName();

        InMemoryVillageStateStore store2 = setupStore(100, 200);
        NameGenerator.generateNames(store2, PLAINS_RESOLVER, CFG);
        String name2 = store2.getClusterNames().values().iterator().next().clusterName();

        assertEquals(name1, name2, "Same coordinates should produce same name");
    }

    @Test
    void differentCoordinates_differentNames() {
        InMemoryVillageStateStore store1 = setupStore(100, 200);
        NameGenerator.generateNames(store1, PLAINS_RESOLVER, CFG);
        String name1 = store1.getClusterNames().values().iterator().next().clusterName();

        InMemoryVillageStateStore store2 = setupStore(999, -555);
        NameGenerator.generateNames(store2, PLAINS_RESOLVER, CFG);
        String name2 = store2.getClusterNames().values().iterator().next().clusterName();

        assertNotEquals(name1, name2, "Different coordinates should produce different names");
    }

    @Test
    void clusterName_lengthIs4Chars() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);
        String name = store.getClusterNames().values().iterator().next().clusterName();
        assertEquals(4, name.length(), "clusterName should be exactly 4 characters: " + name);
    }

    @Test
    void centerTownName_endsWithTown() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);
        String townName = store.getClusterNames().values().iterator().next().centerTownName();
        assertTrue(townName.endsWith("镇"), "centerTownName should end with 镇: " + townName);
        // townName = clusterName + "镇", so length = 5
        assertEquals(5, townName.length());
    }

    @Test
    void satelliteName_endsWithVillage() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);
        Map<UUID, String> satellites = store.getClusterNames().values()
                .iterator().next().satelliteNames();
        for (String satName : satellites.values()) {
            assertTrue(satName.endsWith("村"), "satelliteName should end with 村: " + satName);
            assertEquals(4, satName.length(), "satelliteName should be 4 chars: " + satName);
        }
    }

    @Test
    void desertBiome_usesDesertWordPool() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        NameGenerator.generateNames(store, DESERT_RESOLVER, CFG);
        String name = store.getClusterNames().values().iterator().next().clusterName();

        // Desert pool chars: 金,沙,炎,驼,漠,泉,烈,阳,荒,旱,焰,赤,烁,煌,烽,热,烟,焦,干,戈
        Set<Character> desertChars = Set.of('金', '沙', '炎', '驼', '漠', '泉', '烈', '阳', '荒', '旱',
                '焰', '赤', '烁', '煌', '烽', '热', '烟', '焦', '干', '戈');

        boolean hasDesertChar = false;
        for (char c : name.toCharArray()) {
            if (desertChars.contains(c)) {
                hasDesertChar = true;
                break;
            }
        }
        assertTrue(hasDesertChar, "Desert cluster name should contain desert-pool characters: " + name);
    }

    @Test
    void idempotent_repeatedCallDoesNotChangeNames() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);
        String firstName = store.getClusterNames().values().iterator().next().clusterName();

        // Second call: cluster isNamed=true, should skip
        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);
        String secondName = store.getClusterNames().values().iterator().next().clusterName();

        assertEquals(firstName, secondName, "Name should not change on repeated calls");
    }

    @Test
    void marksClusterAsNamed() {
        InMemoryVillageStateStore store = setupStore(0, 0);
        assertFalse(store.getClusters().get(0).isNamed());

        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);

        assertTrue(store.getClusters().get(0).isNamed(), "Cluster should be marked as named");
    }

    @Test
    void satelliteNamesDoNotDuplicate() {
        // Create a cluster with 3 satellite villages
        UUID centerId = UUID.randomUUID();
        UUID sat1 = UUID.randomUUID();
        UUID sat2 = UUID.randomUUID();
        UUID sat3 = UUID.randomUUID();

        VillageRecord center = makeVillage(centerId, 100, 200, 10);
        VillageRecord s1 = makeVillage(sat1, 120, 220, 3);
        VillageRecord s2 = makeVillage(sat2, 80, 180, 4);
        VillageRecord s3 = makeVillage(sat3, 150, 250, 2);

        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        store.setVillages(List.of(center, s1, s2, s3));

        ClusterRecord cluster = new ClusterRecord(
                "test_cluster",
                List.of(centerId, sat1, sat2, sat3),
                centerId,
                false,
                false);
        store.setClusters(List.of(cluster));

        NameGenerator.generateNames(store, PLAINS_RESOLVER, CFG);

        ClusterName cn = store.getClusterNames().get("test_cluster");
        Set<String> satelliteNames = new HashSet<>(cn.satelliteNames().values());
        assertEquals(3, satelliteNames.size(),
                "All satellite names should be unique, got: " + cn.satelliteNames().values());
    }

    // ── Helper ──

    private InMemoryVillageStateStore setupStore(int x, int z) {
        UUID centerId = UUID.randomUUID();
        UUID satId = UUID.randomUUID();

        VillageRecord center = makeVillage(centerId, x, z, 10);
        VillageRecord satellite = makeVillage(satId, x + 20, z + 20, 3);

        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        store.setVillages(List.of(center, satellite));

        ClusterRecord cluster = new ClusterRecord(
                "test_cluster",
                List.of(centerId, satId),
                centerId,
                false,
                false);
        store.setClusters(List.of(cluster));

        return store;
    }
}
