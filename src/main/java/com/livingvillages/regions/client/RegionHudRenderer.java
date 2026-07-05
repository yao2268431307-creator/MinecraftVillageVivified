package com.livingvillages.regions.client;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.naming.RegionNameGenerator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Map;

/**
 * Client-side region+village name HUD overlay.
 *
 * <p>Draws the current region name (optionally with village name) in small
 * text at the top-right corner of the screen every frame. When the player
 * is inside a village, the display shows "&lang;region&rang;——&lang;village&rang;"
 * (e.g. "风沙之地——赤焰村"). Village detection uses the same chunk-scan
 * logic as {@link RegionTitleDisplay}.</p>
 */
public class RegionHudRenderer implements HudRenderCallback {

    /** Chunk search radius for village detection (same as RegionTitleDisplay). */
    private static final int VILLAGE_SEARCH_CHUNKS = 5;

    @Override
    public void onHudRender(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        long seed = RegionTitleDisplay.getWorldSeed();
        if (seed == Long.MIN_VALUE) {
            return;
        }

        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, RegionTitleDisplay.SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, RegionTitleDisplay.SUPER_CELL_SIZE);

        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, blockX, blockZ);
        if (type == null) {
            return;
        }

        String regionName = RegionNameGenerator.regionName(seed, superX, superZ, type);

        // Check for nearby village to augment the HUD.
        ChunkPos villageChunk = findNearbyVillageChunk(mc, blockX, blockZ);
        String displayName = regionName;
        if (villageChunk != null) {
            String villageName = RegionNameGenerator.villageName(
                seed, villageChunk.x, villageChunk.z, type);
            displayName = RegionNameGenerator.fullDisplay(regionName, villageName);
        }

        int x = mc.getWindow().getGuiScaledWidth() - mc.font.width(displayName) - 8;
        int y = 8;
        g.drawString(mc.font, displayName, x, y, 0xFFFFFF, true);
    }

    /**
     * Search loaded chunks around the player for a village {@link StructureStart}
     * whose bounding box contains the player's position.
     */
    private static ChunkPos findNearbyVillageChunk(Minecraft mc, int playerBlockX, int playerBlockZ) {
        int playerChunkX = playerBlockX >> 4;
        int playerChunkZ = playerBlockZ >> 4;
        BlockPos playerPos = new BlockPos(playerBlockX, 64, playerBlockZ);

        for (int dx = -VILLAGE_SEARCH_CHUNKS; dx <= VILLAGE_SEARCH_CHUNKS; dx++) {
            for (int dz = -VILLAGE_SEARCH_CHUNKS; dz <= VILLAGE_SEARCH_CHUNKS; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;

                ChunkAccess chunk = mc.level.getChunkSource()
                    .getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                Map<Structure, StructureStart> starts = chunk.getAllStarts();
                if (starts == null || starts.isEmpty()) {
                    continue;
                }

                Registry<Structure> registry = mc.level.registryAccess()
                    .registry(Registries.STRUCTURE)
                    .orElse(null);
                if (registry == null) {
                    continue;
                }

                for (Map.Entry<Structure, StructureStart> entry : starts.entrySet()) {
                    ResourceLocation key = registry.getKey(entry.getKey());
                    if (key == null) {
                        continue;
                    }
                    String id = key.toString();
                    if (id.equals("minecraft:village_plains")
                        || id.equals("minecraft:village_desert")
                        || id.equals("minecraft:village_savanna")
                        || id.equals("minecraft:village_snowy")
                        || id.equals("minecraft:village_taiga")) {
                        StructureStart start = entry.getValue();
                        if (start.getBoundingBox().isInside(playerPos)) {
                            BlockPos center = start.getBoundingBox().getCenter();
                            return new ChunkPos(center.getX() >> 4, center.getZ() >> 4);
                        }
                    }
                }
            }
        }
        return null;
    }
}
