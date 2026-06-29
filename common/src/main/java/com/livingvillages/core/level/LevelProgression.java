package com.livingvillages.core.level;

import com.livingvillages.core.data.VillageRecord;
import com.livingvillages.core.data.VillageStateStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Determines village levels based on accumulated consumption thresholds.
 *
 * <p>Pure threshold-based leveling (no XP formula):</p>
 * <ul>
 *   <li>Lv1: initial (default)</li>
 *   <li>Lv2: food ≥ 300</li>
 *   <li>Lv3: food ≥ 3000 AND luxury ≥ 100</li>
 *   <li>Lv4: food ≥ 15000 AND luxury ≥ 500 → marks specialty</li>
 * </ul>
 *
 * <p>Writer of: villageLevels[], specialities[], accumulatedConsumption[].</p>
 * <p>Reader of: villages[], warehouses[].</p>
 */
public final class LevelProgression {

    /** Threshold constants (tunable by implementation). */
    static final long LV2_FOOD_THRESHOLD = 300;
    static final long LV3_FOOD_THRESHOLD = 3000;
    static final long LV3_LUXURY_THRESHOLD = 100;
    static final long LV4_FOOD_THRESHOLD = 15000;
    static final long LV4_LUXURY_THRESHOLD = 500;

    private LevelProgression() {}

    /**
     * Update all village levels based on accumulated consumption.
     *
     * <p>Reads {@code villages[]}, {@code accumulatedConsumption[]}, {@code warehouses[]}.
     * Writes {@code villageLevels[]}, {@code specialities[]}, {@code accumulatedConsumption[]}.</p>
     *
     * @param store the state store
     */
    public static void updateLevels(VillageStateStore store) {
        Map<UUID, Integer> currentLevels = new HashMap<>(store.getVillageLevels());
        Map<UUID, Map<String, Long>> accumulated = new HashMap<>();
        // Deep copy accumulated consumption
        for (var entry : store.getAccumulatedConsumption().entrySet()) {
            accumulated.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        Map<UUID, String> specialities = new HashMap<>(store.getSpecialities());

        for (VillageRecord village : store.getVillages()) {
            UUID vid = village.id();
            Map<String, Long> cons = accumulated.getOrDefault(vid, new HashMap<>());

            long foodConsumed = cons.getOrDefault("food", 0L);
            long luxuryConsumed = cons.getOrDefault("luxury", 0L);

            int level = determineLevel(foodConsumed, luxuryConsumed);
            currentLevels.put(vid, level);

            // Lv4: mark specialty if not already set
            if (level == 4 && !specialities.containsKey(vid)) {
                String specialty = pickSpecialty(vid, store);
                if (specialty != null) {
                    specialities.put(vid, specialty);
                }
            }
        }

        store.setVillageLevels(currentLevels);
        store.setSpecialities(specialities);
        store.setAccumulatedConsumption(accumulated);
    }

    /**
     * Determine the level based on consumption thresholds.
     *
     * @param foodConsumed   total food consumed
     * @param luxuryConsumed total luxury goods consumed
     * @return level 1–4
     */
    static int determineLevel(long foodConsumed, long luxuryConsumed) {
        if (foodConsumed < LV2_FOOD_THRESHOLD) {
            return 1;
        }
        if (foodConsumed < LV3_FOOD_THRESHOLD || luxuryConsumed < LV3_LUXURY_THRESHOLD) {
            return 2;
        }
        if (foodConsumed < LV4_FOOD_THRESHOLD || luxuryConsumed < LV4_LUXURY_THRESHOLD) {
            return 3;
        }
        return 4;
    }

    /**
     * Pick the specialty item for a village: the item with the highest stock in its warehouse.
     *
     * @param villageId the village id
     * @param store     the state store (for reading warehouses)
     * @return the item id with the highest count, or null if warehouse is empty
     */
    private static String pickSpecialty(UUID villageId, VillageStateStore store) {
        Map<UUID, Map<String, Integer>> warehouses = store.getWarehouses();
        Map<String, Integer> wh = warehouses.get(villageId);
        if (wh == null || wh.isEmpty()) {
            return null;
        }
        String best = null;
        int maxQty = 0;
        for (var entry : wh.entrySet()) {
            if (entry.getValue() > maxQty) {
                maxQty = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }
}
