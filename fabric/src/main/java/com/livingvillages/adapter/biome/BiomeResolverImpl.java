package com.livingvillages.adapter.biome;

import com.livingvillages.core.naming.BiomeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * MC implementation of BiomeResolver using real biome tags.
 */
public class BiomeResolverImpl implements BiomeResolver {

    private final ServerLevel level;

    public BiomeResolverImpl(ServerLevel level) {
        this.level = level;
    }

    @Override
    public String getBiomeCategory(int x, int y, int z) {
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        return mapToCategory(biome);
    }

    static String mapToCategory(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_JUNGLE))       return "jungle";
        if (biome.is(BiomeTags.HAS_SWAMP_HUT))    return "swamp";
        if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) return "desert";
        if (biome.is(BiomeTags.IS_TAIGA))         return "taiga";

        float temp = biome.value().getBaseTemperature();
        if (temp < 0.15)                          return "snowy";
        if (temp > 1.5)                           return "savanna";

        return "plains";
    }
}
