package com.livingvillages.regions;

import com.livingvillages.regions.client.RegionHudRenderer;
import com.livingvillages.regions.client.RegionTitleDisplay;
import com.livingvillages.regions.network.SeedReceiver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/**
 * LivingVillages Regions client initializer.
 *
 * <p>Registers the world-seed packet receiver ({@link SeedReceiver#register()}),
 * the per-tick region-name float-text listener ({@link RegionTitleDisplay})
 * onto {@link ClientTickEvents#END_CLIENT_TICK}, and the top-right corner
 * region-name HUD ({@link RegionHudRenderer}) onto
 * {@link HudRenderCallback#EVENT}.</p>
 */
public class RegionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SeedReceiver.register();
        ClientTickEvents.END_CLIENT_TICK.register(new RegionTitleDisplay());
        HudRenderCallback.EVENT.register(new RegionHudRenderer());
    }
}
