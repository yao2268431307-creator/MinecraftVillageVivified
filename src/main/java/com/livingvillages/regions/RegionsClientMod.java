package com.livingvillages.regions;

import com.livingvillages.regions.client.RegionTitleDisplay;
import com.livingvillages.regions.network.SeedReceiver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * LivingVillages Regions client initializer.
 *
 * <p>Registers the world-seed packet receiver ({@link SeedReceiver#register()})
 * and the per-tick region-name float-text listener ({@link RegionTitleDisplay})
 * onto {@link ClientTickEvents#END_CLIENT_TICK}.</p>
 */
public class RegionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SeedReceiver.register();
        ClientTickEvents.END_CLIENT_TICK.register(new RegionTitleDisplay());
    }
}
