package com.livingvillages.adapter.fabric;

import com.livingvillages.adapter.config.ConfigLoader;
import com.livingvillages.adapter.data.NbtVillageStateStore;
import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.VillageStateStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric entry point for Living Villages.
 *
 * <p>Bridges Fabric events to Core module calls.</p>
 */
public class LivingVillagesFabric implements ModInitializer {

    public static final String MOD_ID = "livingvillages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;
    private static VillageStateStore stateStore;

    @Override
    public void onInitialize() {
        LOGGER.info("Living Villages initializing...");

        // 1. Load config
        config = ConfigLoader.load();

        // 2. Register event hooks
        ServerWorldEvents.LOAD.register((server, level) -> {
            if (stateStore == null) {
                stateStore = NbtVillageStateStore.load(level);
            }
            LOGGER.info("Living Villages: world loaded, seed={}", level.getSeed());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (stateStore != null && config != null) {
                // TODO: Daily tick hook — call TickOrchestrator.runDailyCycle()
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Living Villages: server stopping, saving data...");
            if (stateStore != null) {
                stateStore.markDirty();
            }
        });

        LOGGER.info("Living Villages initialized.");
    }

    public static ModConfig getConfig() {
        return config;
    }

    public static VillageStateStore getStateStore() {
        return stateStore;
    }
}
