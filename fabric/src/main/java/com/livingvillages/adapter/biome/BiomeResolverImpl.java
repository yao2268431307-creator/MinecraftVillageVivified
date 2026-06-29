package com.livingvillages.adapter.biome;

import com.livingvillages.core.naming.BiomeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/**
 * MC implementation of BiomeResolver. Queries actual Minecraft biome registry.
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

    private String mapToCategory(Holder<Biome> biome) {
        // TODO: proper biome tag mapping
        return "plains";
    }
}
