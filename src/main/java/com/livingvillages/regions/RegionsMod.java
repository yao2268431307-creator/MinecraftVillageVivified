package com.livingvillages.regions;

import com.livingvillages.regions.network.RoadWeaverIntegrator;
import com.livingvillages.regions.network.SeedSender;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LivingVillages Regions mod initializer.
 *
 * <p>Registers the server-side world-seed sender
 * ({@link SeedSender#register()}) and a throttled server-tick listener that
 * drives {@link RoadWeaverIntegrator#onTick(MinecraftServer)} once per second
 * (every {@value RoadWeaverIntegrator#TICK_THROTTLE} ticks) to scan for new
 * villages and register/connect them with RoadWeaver. The companion client
 * entrypoint ({@link RegionsClientMod}) handles the matching receiver and the
 * region title float-text listener.</p>
 */
public class RegionsMod implements ModInitializer {

    public static final String MOD_ID = "livingvillages-regions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("LivingVillages Regions initializing");
        SeedSender.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % RoadWeaverIntegrator.TICK_THROTTLE == 0) {
                RoadWeaverIntegrator.onTick(server);
            }
        });
    }
}
