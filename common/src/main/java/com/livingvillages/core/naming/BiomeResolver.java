package com.livingvillages.core.naming;

/**
 * Resolves a world position to a biome category key.
 *
 * <p>Defined in the Core layer as a functional interface so NameGenerator
 * has zero Minecraft dependency. The MC Adapter implements this interface
 * by querying the actual {@code net.minecraft.world.level.biome.Biome} registry.</p>
 *
 * <p>Valid return values:</p>
 * <ul>
 *   <li>{@code "plains"}</li>
 *   <li>{@code "desert"}</li>
 *   <li>{@code "taiga"}</li>
 *   <li>{@code "snowy"}</li>
 *   <li>{@code "savanna"}</li>
 *   <li>{@code "swamp"}</li>
 *   <li>{@code "jungle"}</li>
 *   <li>{@code "other"} — fallback for unrecognized biomes</li>
 * </ul>
 */
@FunctionalInterface
public interface BiomeResolver {

    /**
     * Get the biome category for the given coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return biome category key, never null
     */
    String getBiomeCategory(int x, int y, int z);
}
