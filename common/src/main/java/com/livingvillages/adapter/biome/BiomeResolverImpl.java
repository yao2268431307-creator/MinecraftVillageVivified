package com.livingvillages.adapter.biome;

import com.livingvillages.core.naming.BiomeResolver;

/**
 * MC Adapter implementation of BiomeResolver.
 *
 * <p>Queries the actual Minecraft biome registry to determine the category
 * at a given position. Maps MC Biome to one of the 8 category keys.</p>
 *
 * <p>Note: Template stub. Full implementation requires:</p>
 * <ul>
 *   <li>{@code net.minecraft.core.Holder} for biome holder</li>
 *   <li>{@code net.minecraft.world.level.biome.Biome} for biome types</li>
 *   <li>{@code net.minecraft.world.level.LevelReader} for biome query</li>
 * </ul>
 */
public class BiomeResolverImpl implements BiomeResolver {

    // MC: store LevelReader reference for biome queries

    /**
     * Resolve biome category from MC biome at (x, y, z).
     *
     * <p>MC Implementation:</p>
     * <pre>
     *   Holder&lt;Biome&gt; biome = level.getBiome(BlockPos(x, y, z));
     *   return mapToCategory(biome);
     * </pre>
     *
     * <p>Mapping logic (MC-specific):</p>
     * <pre>
     *   if (biome is in BiomeTags.IS_FOREST)     → "taiga" or "jungle" based on temp
     *   else if (biome is in BiomeTags.HAS_DESERT_PYRAMID) → "desert"
     *   else if (biome temperature < 0.15)       → "snowy"
     *   else if (biome is in BiomeTags.HAS_SWAMP_HUT) → "swamp"
     *   else if (biome temperature > 1.5)        → "savanna"
     *   else                                      → "plains"
     *   if matches jungle variants                → "jungle"
     * </pre>
     */
    @Override
    public String getBiomeCategory(int x, int y, int z) {
        // Stub: always return "plains" for test compatibility
        return "plains";
    }
}
