package com.livingvillages.regions.client.mixin;

import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.client.RegionTitleDisplay;
import com.livingvillages.regions.naming.RegionNameGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.shiroha233.roadweaver.client.map.RoadMapScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into RoadWeaver's {@link RoadMapScreen} to overlay the current biome
 * region name at the top of the screen when the player opens the map (H key).
 *
 * <p>The overlay is drawn after the original {@code render} returns so the
 * underlying map (terrain, roads, villages, toolbar) is fully painted first.
 * It mirrors the same resolution pipeline used by the action-bar float-text
 * ({@link RegionTitleDisplay}): resolve the player's super-cell, look up the
 * {@link RegionType}, and generate the deterministic region name via
 * {@link RegionNameGenerator}.</p>
 *
 * <p>If the world seed has not yet been received from the server, or the
 * player's current biome is excluded (cave / water / beach), the overlay is
 * suppressed silently.</p>
 *
 * <p>This is a client-only Mixin (registered under the {@code client} array
 * of {@code livingvillages.mixins.json}) and does not cancel the original
 * render call.</p>
 */
@Mixin(RoadMapScreen.class)
public abstract class RoadMapScreenMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At("RETURN")
    )
    private void livingvillages$renderRegionOverlay(
            GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        long seed = RegionTitleDisplay.getWorldSeed();
        if (seed == Long.MIN_VALUE) {
            // Seed not yet received from server: cannot resolve deterministic names.
            return;
        }

        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, RegionTitleDisplay.SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, RegionTitleDisplay.SUPER_CELL_SIZE);

        RegionType type = BiomeRegionResolver.resolveRegionType(mc.level, blockX, blockZ);
        if (type == null) {
            // Cave / water / beach: no region to name.
            return;
        }

        String regionName = RegionNameGenerator.regionName(seed, superX, superZ, type);

        // Center the region name at the top of the screen.
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int textWidth = mc.font.width(regionName);
        int x = (screenWidth - textWidth) / 2;
        int y = 16;
        g.drawString(mc.font, regionName, x, y, 0xFFFFAA, true);
    }
}
