package com.livingvillages.core.level;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LevelProgression.
 */
class LevelProgressionTest {

    private static VillageRecord makeVillage() {
        return new VillageRecord(
                UUID.randomUUID(),
                new Vec3i(0, 64, 0),
                "plains",
                5,
                0,
                true
        );
    }

    @Test
    void zeroConsumption_level1() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of());

        LevelProgression.updateLevels(store);

        assertEquals(1, store.getVillageLevels().get(v.id()));
    }

    @Test
    void food300_level2() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 300L)
        ));

        LevelProgression.updateLevels(store);

        assertEquals(2, store.getVillageLevels().get(v.id()));
    }

    @Test
    void food3000_luxury100_level3() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 3000L, "luxury", 100L)
        ));

        LevelProgression.updateLevels(store);

        assertEquals(3, store.getVillageLevels().get(v.id()));
    }

    @Test
    void food3000_insufficientLuxury_level2() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 3000L, "luxury", 99L)
        ));

        LevelProgression.updateLevels(store);

        assertEquals(2, store.getVillageLevels().get(v.id()));
    }

    @Test
    void food15000_luxury500_level4() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 15000L, "luxury", 500L)
        ));

        LevelProgression.updateLevels(store);

        assertEquals(4, store.getVillageLevels().get(v.id()));
    }

    @Test
    void level4_registersSpecialty() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 15000L, "luxury", 500L)
        ));
        // Warehouse has wheat as highest stock
        store.setWarehouses(Map.of(
                v.id(), Map.of("wheat", 100, "potato", 50, "carrot", 25)
        ));

        LevelProgression.updateLevels(store);

        assertEquals("wheat", store.getSpecialities().get(v.id()));
    }

    @Test
    void specialtyIsIdempotent() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        store.setAccumulatedConsumption(Map.of(
                v.id(), Map.of("food", 15000L, "luxury", 500L)
        ));
        store.setWarehouses(Map.of(
                v.id(), Map.of("wheat", 100, "potato", 50)
        ));

        // First call sets specialty
        LevelProgression.updateLevels(store);
        String first = store.getSpecialities().get(v.id());
        assertEquals("wheat", first);

        // Change warehouse: potato now has more
        store.setWarehouses(Map.of(
                v.id(), Map.of("wheat", 10, "potato", 500)
        ));

        // Second call should NOT change specialty (idempotent)
        LevelProgression.updateLevels(store);
        String second = store.getSpecialities().get(v.id());
        assertEquals(first, second, "Specialty should not change once registered");
    }

    @Test
    void emptyAccumulatedConsumption_defaultsToLevel1() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v = makeVillage();
        store.setVillages(List.of(v));
        // No accumulated consumption set

        LevelProgression.updateLevels(store);

        assertEquals(1, store.getVillageLevels().get(v.id()));
    }

    @Test
    void multipleVillages_differentLevels() {
        InMemoryVillageStateStore store = new InMemoryVillageStateStore();
        VillageRecord v1 = makeVillage();
        VillageRecord v2 = makeVillage();
        VillageRecord v3 = makeVillage();
        store.setVillages(List.of(v1, v2, v3));
        store.setAccumulatedConsumption(Map.of(
                v1.id(), Map.of("food", 0L),
                v2.id(), Map.of("food", 300L),
                v3.id(), Map.of("food", 15000L, "luxury", 500L)
        ));
        store.setWarehouses(Map.of(
                v3.id(), Map.of("emerald", 10)
        ));

        LevelProgression.updateLevels(store);

        assertEquals(1, store.getVillageLevels().get(v1.id()));
        assertEquals(2, store.getVillageLevels().get(v2.id()));
        assertEquals(4, store.getVillageLevels().get(v3.id()));
    }

    @Test
    void determineLevel_boundaryValues() {
        // Just below Lv2
        assertEquals(1, LevelProgression.determineLevel(299, 0));
        // Exactly at Lv2
        assertEquals(2, LevelProgression.determineLevel(300, 0));
        // Exactly at Lv3
        assertEquals(3, LevelProgression.determineLevel(3000, 100));
        // Just below Lv3 luxury
        assertEquals(2, LevelProgression.determineLevel(3000, 99));
        // Exactly at Lv4
        assertEquals(4, LevelProgression.determineLevel(15000, 500));
        // Just below Lv4 luxury
        assertEquals(3, LevelProgression.determineLevel(15000, 499));
    }
}
