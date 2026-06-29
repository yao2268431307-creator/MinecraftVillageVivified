package com.livingvillages.core.data;

import java.util.UUID;

/**
 * Core data for one Living Village.
 *
 * <p>Writer: §1 KCenterGenerator (creation), §8 LevelProgression (bedCount indirect).</p>
 *
 * @param id             deterministic UUID from seed + index
 * @param position       village center coordinates
 * @param biomeCategory  "plains" | "desert" | "taiga" | "snowy" | "savanna" | "swamp" | "jungle" | "other"
 * @param bedCount       number of beds ≈ population
 * @param firstSeenTick  game tick when first detected
 * @param placed         whether MC Adapter has placed the structure in the world
 */
public record VillageRecord(
        UUID id,
        Vec3i position,
        String biomeCategory,
        int bedCount,
        long firstSeenTick,
        boolean placed) {

    /** Compact constructor: apply defaults for nullable/advisory fields. */
    public VillageRecord {
        if (biomeCategory == null || biomeCategory.isBlank()) {
            biomeCategory = "unresolved";
        }
    }
}
