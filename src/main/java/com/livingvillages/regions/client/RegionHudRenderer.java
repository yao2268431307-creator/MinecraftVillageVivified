package com.livingvillages.regions.client;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.naming.RegionNameGenerator;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side region-name HUD overlay.
 *
 * <p>Draws the current region name in small text at the top-right corner of
 * the screen every frame via {@link HudRenderCallback#onHudRender(GuiGraphics, float)}.
 * The region is resolved from the player's block position using
 * {@link BiomeRegionResolver#resolveRegionType(Level, int, int)} and named via
 * {@link RegionNameGenerator#regionName(long, int, int, RegionType)}.</p>
 *
 * <p>Excluded biomes (cave / ocean / river / beach) yield a {@code null}
 * region type, in which case nothing is drawn. The world seed is obtained
 * from {@link RegionTitleDisplay#getWorldSeed()}; until the seed is injected
 * by the server-&gt;client packet handler (sentinel {@link Long#MIN_VALUE}),
 * the HUD stays suppressed.</p>
 *
 * <p>This class implements {@link HudRenderCallback} but does not self-register;
 * the caller ({@code ClientModInitializer}) is responsible for invoking
 * {@code HudRenderCallback.EVENT.register(new RegionHudRenderer())}.</p>
 */
public class RegionHudRenderer implements HudRenderCallback {

    @Override
    public void onHudRender(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        long seed = RegionTitleDisplay.getWorldSeed();
        if (seed == Long.MIN_VALUE) {
            // Seed not yet received from the server: cannot resolve names.
            return;
        }

        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, RegionTitleDisplay.SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, RegionTitleDisplay.SUPER_CELL_SIZE);

        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, blockX, blockZ);
        if (type == null) {
            // Cave / water / beach: no HUD.
            return;
        }

        String regionName = RegionNameGenerator.regionName(seed, superX, superZ, type);

        // Top-right corner, 8px inset from the edge.
        int x = mc.getWindow().getGuiScaledWidth() - mc.font.width(regionName) - 8;
        int y = 8;
        g.drawString(mc.font, regionName, x, y, 0xFFFFFF, true);
    }
}
