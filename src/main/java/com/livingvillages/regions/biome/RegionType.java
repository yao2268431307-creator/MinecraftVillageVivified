package com.livingvillages.regions.biome;

/**
 * Biome region categories used by the LivingVillages cluster mod.
 *
 * <p>Each non-excluded biome resolves to exactly one of these types
 * via {@link BiomeRegionResolver}. {@link #OTHER} is a placeholder for
 * biomes that do not match any specific category and is currently unused
 * by the resolver (which falls back to {@link #PLAINS}).</p>
 */
public enum RegionType {
    DESERT,
    SNOWY,
    PLAINS,
    TAIGA,
    SAVANNA,
    SWAMP,
    JUNGLE,
    MOUNTAIN,
    FOREST,
    OTHER
}
