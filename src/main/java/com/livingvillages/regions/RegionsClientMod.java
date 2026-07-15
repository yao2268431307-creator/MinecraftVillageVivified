package com.livingvillages.regions;

import com.livingvillages.regions.client.RegionTitleDisplay;
import com.livingvillages.regions.network.SeedReceiver;
import com.livingvillages.regions.network.SettlementsReceiver;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * LivingVillages Regions client initializer.
 *
 * <p>Registers the world-seed packet receiver ({@link SeedReceiver#register()}),
 * the settlements packet receiver ({@link SettlementsReceiver#register()}),
 * and the per-tick settlement-name float-text listener
 * ({@link RegionTitleDisplay}) onto {@link ClientTickEvents#END_CLIENT_TICK}.</p>
 */
public class RegionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SeedReceiver.register();
        SettlementsReceiver.register();
        ClientTickEvents.END_CLIENT_TICK.register(new RegionTitleDisplay());
    }
}
