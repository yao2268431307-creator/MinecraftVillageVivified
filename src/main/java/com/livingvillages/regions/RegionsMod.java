package com.livingvillages.regions;

import com.livingvillages.regions.network.SeedSender;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LivingVillages Regions mod initializer.
 *
 * <p>Registers the server-side world-seed sender
 * ({@link SeedSender#register()}). The companion client entrypoint
 * ({@link RegionsClientMod}) handles the matching receiver and the
 * region title float-text listener.</p>
 */
public class RegionsMod implements ModInitializer {

    public static final String MOD_ID = "livingvillages-regions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("LivingVillages Regions initializing");
        SeedSender.register();
    }
}
