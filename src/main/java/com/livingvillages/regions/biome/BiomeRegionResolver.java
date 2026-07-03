package com.livingvillages.regions.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

/**
 * Resolves a vanilla {@link Biome} into one of the {@link RegionType} values
 * used by the LivingVillages cluster mod.
 *
 * <p>Resolution follows the priority order defined in {@code DESIGN.md}
 * (section "Biome 区位解析"):</p>
 * <ol>
 *   <li>Cave biomes are excluded (return {@code null}). Vanilla 1.20.1 has no
 *       {@code is_cave} tag, so a manual id list is used.</li>
 *   <li>Ocean and river biomes are excluded via {@link BiomeTags#IS_OCEAN} /
 *       {@link BiomeTags#IS_RIVER}.</li>
 *   <li>Beach biomes are excluded via {@link BiomeTags#IS_BEACH}.</li>
 *   <li>Remaining biomes are classified by: mountain tag, temperature,
 *       swamp-hut tag, jungle tag, taiga tag, temperature again (savanna),
 *       forest id list, and finally fall back to plains.</li>
 * </ol>
 *
 * <p>The cave and forest id lists are exposed as package-private
 * {@code String}-based helpers ({@link #isCaveBiomeId(String)} and
 * {@link #isForestBiomeId(String)}) so they can be unit-tested without
 * the Minecraft runtime.</p>
 */
public final class BiomeRegionResolver {

    private BiomeRegionResolver() {
        // utility class, no instances
    }

    /**
     * Resolve the region type of a biome holder.
     *
     * @param biomeHolder the biome holder (may be {@code null})
     * @return the resolved region type, or {@code null} if the biome is
     *         a cave / ocean / river / beach biome (excluded)
     */
    public static RegionType resolveRegionType(Holder<Biome> biomeHolder) {
        if (biomeHolder == null) {
            return null;
        }

        // 1. Exclude cave biomes (vanilla 1.20.1 has no is_cave tag, use id list)
        if (isCaveBiome(biomeHolder)) {
            return null;
        }

        // 2. Exclude water biomes
        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            return null;
        }

        // 3. Exclude beach biomes
        if (biomeHolder.is(BiomeTags.IS_BEACH)) {
            return null;
        }

        // 4. Classify by feature (priority order from DESIGN.md)
        if (biomeHolder.is(BiomeTags.IS_MOUNTAIN)) {
            return RegionType.MOUNTAIN;
        }
        float temp = biomeHolder.value().getBaseTemperature();
        if (temp < 0.15f) {
            return RegionType.SNOWY;
        }
        if (temp > 1.5f) {
            return RegionType.DESERT;
        }
        if (biomeHolder.is(BiomeTags.HAS_SWAMP_HUT)) {
            return RegionType.SWAMP;
        }
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return RegionType.JUNGLE;
        }
        if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return RegionType.TAIGA;
        }
        if (temp > 1.0f) {
            return RegionType.SAVANNA;
        }
        // No vanilla is_forest tag — classify by biome id.
        if (isForestBiome(biomeHolder)) {
            return RegionType.FOREST;
        }
        return RegionType.PLAINS;
    }

    /**
     * Server-side helper: resolve region type at a block coordinate.
     *
     * @param level  the server level
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return the resolved region type, or {@code null} for excluded biomes
     */
    public static RegionType resolveRegionType(ServerLevel level, int blockX, int blockZ) {
        Holder<Biome> biome = level.getBiome(new BlockPos(blockX, 64, blockZ));
        return resolveRegionType(biome);
    }

    /**
     * Client-side helper: resolve region type at a block coordinate.
     *
     * @param level  the level (client or server)
     * @param blockX the block X coordinate
     * @param blockZ the block Z coordinate
     * @return the resolved region type, or {@code null} for excluded biomes
     */
    public static RegionType resolveRegionType(Level level, int blockX, int blockZ) {
        Holder<Biome> biome = level.getBiome(new BlockPos(blockX, 64, blockZ));
        return resolveRegionType(biome);
    }

    // ------------------------------------------------------------------
    // Cave biome classification
    // ------------------------------------------------------------------

    /**
     * Check whether a biome holder is a cave biome (excluded from regions).
     *
     * @param holder the biome holder
     * @return {@code true} if the holder's biome is one of the cave biomes
     */
    private static boolean isCaveBiome(Holder<Biome> holder) {
        return isCaveBiomeId(holderId(holder));
    }

    /**
     * Check whether a biome id (e.g. {@code "minecraft:lush_caves"}) is a cave
     * biome. Package-private so unit tests can exercise this without the MC
     * runtime.
     *
     * @param biomeId the biome resource id, or {@code null}
     * @return {@code true} if the id names a cave biome
     */
    static boolean isCaveBiomeId(String biomeId) {
        if (biomeId == null) {
            return false;
        }
        return biomeId.equals("minecraft:lush_caves")
            || biomeId.equals("minecraft:dripstone_caves")
            || biomeId.equals("minecraft:deep_dark");
    }

    // ------------------------------------------------------------------
    // Forest biome classification
    // ------------------------------------------------------------------

    /**
     * Check whether a biome holder is a forest biome. Vanilla 1.20.1 has no
     * {@code is_forest} tag, so this falls back to an explicit id list.
     *
     * @param holder the biome holder
     * @return {@code true} if the holder's biome is one of the forest biomes
     */
    private static boolean isForestBiome(Holder<Biome> holder) {
        return isForestBiomeId(holderId(holder));
    }

    /**
     * Check whether a biome id (e.g. {@code "minecraft:forest"}) is a forest
     * biome. Package-private so unit tests can exercise this without the MC
     * runtime.
     *
     * <p>Note: {@code old_growth_pine_taiga} and {@code old_growth_spruce_taiga}
     * are taiga variants in vanilla and are matched earlier by the
     * {@link BiomeTags#IS_TAIGA} tag, so they are deliberately <em>not</em>
     * included here.</p>
     *
     * @param biomeId the biome resource id, or {@code null}
     * @return {@code true} if the id names a forest biome
     */
    static boolean isForestBiomeId(String biomeId) {
        if (biomeId == null) {
            return false;
        }
        return biomeId.equals("minecraft:forest")
            || biomeId.equals("minecraft:birch_forest")
            || biomeId.equals("minecraft:dark_forest")
            || biomeId.equals("minecraft:old_growth_birch_forest")
            || biomeId.equals("minecraft:flower_forest");
    }

    /**
     * Extract the resource id string from a biome holder, or {@code null}
     * if the holder is not bound to a registered biome key.
     */
    private static String holderId(Holder<Biome> holder) {
        Optional<ResourceKey<Biome>> key = holder.unwrapKey();
        return key.map(k -> k.location().toString()).orElse(null);
    }
}
