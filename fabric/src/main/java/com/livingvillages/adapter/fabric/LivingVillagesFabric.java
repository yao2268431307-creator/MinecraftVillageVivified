package com.livingvillages.adapter.fabric;

import com.livingvillages.adapter.biome.BiomeResolverImpl;
import com.livingvillages.adapter.client.RegionTitleDisplay;
import com.livingvillages.adapter.config.ConfigLoader;
import com.livingvillages.adapter.data.NbtVillageStateStore;
import com.livingvillages.adapter.road.RoadPlacer;
import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import com.livingvillages.core.naming.BiomeResolver;
import com.livingvillages.core.orchestrator.TickOrchestrator;
import com.livingvillages.core.villagegen.VillagePositionCalculator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LivingVillagesFabric implements ModInitializer {

    public static final String MOD_ID = "livingvillages";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ModConfig config;
    private static NbtVillageStateStore stateStore;
    private static BiomeResolver biomeResolver;
    private static boolean worldGenDone = false;
    private static long tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("=== Living Villages v{} initializing ===", ModConfig.defaults().modVersion());

        config = ConfigLoader.load();
        LOGGER.info("Config loaded: clusterRadius={}, villagesPerCluster={}",
                config.clusterRadius(), config.villagesPerCluster());

        // ── World load: initialize state + run world gen ──
        ServerWorldEvents.LOAD.register((server, level) -> {
            LOGGER.info("World loaded: seed={}", level.getSeed());

            stateStore = NbtVillageStateStore.load(level);
            biomeResolver = new BiomeResolverImpl(level);

            // Run world gen once per world
            if (!worldGenDone && stateStore.getVillages().isEmpty()) {
                runWorldGeneration(level);
                worldGenDone = true;
            }
        });

        // ── Daily tick: run economy/naming/road cycle ──
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % config.dailyTickInterval() != 0) return;
            if (stateStore == null || config == null || biomeResolver == null) return;

            ServerLevel overworld = server.overworld();
            if (overworld == null) return;

            LOGGER.debug("Daily tick {}: running cycle", tickCounter / config.dailyTickInterval());

            // 1. Cluster detection + naming + graph + roads (core logic)
            TickOrchestrator.runDailyCycle(stateStore, config, biomeResolver);

            // 2. Place roads (MC adapter, framed execution)
            RoadPlacer.placeRoads(overworld, stateStore, config, 50);
        });

        // ── Server stop: save ──
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving data...");
            if (stateStore != null) stateStore.markDirty();
        });

        // ── Client: region title display ──
        try {
            RegionTitleDisplay.register();
            LOGGER.info("Region title display registered");
        } catch (Exception e) {
            LOGGER.warn("Region title display not available (server-only environment)");
        }

        LOGGER.info("=== Living Villages initialized ===");
    }

    /**
     * Run world generation: compute vanilla positions → cluster → store.
     */
    private void runWorldGeneration(ServerLevel level) {
        LOGGER.info("Starting world generation...");
        long seed = level.getSeed();
        int radius = 10000; // TODO: get from world border or config

        try {
            // Step 1: Compute vanilla village positions
            List<Vec3i> vanillaPositions = VillagePositionCalculator.computeVanillaPositions(seed, radius);
            LOGGER.info("  Computed {} vanilla village positions (radius={})", vanillaPositions.size(), radius);

            // Step 2: Cluster them + redirect to centers
            TickOrchestrator.onWorldCreate(seed, vanillaPositions, stateStore, config);
            LOGGER.info("  Clustered into {} villages", stateStore.getVillages().size());

            // Step 3: Run first daily cycle (detect clusters, name them, build graph)
            TickOrchestrator.runDailyCycle(stateStore, config, biomeResolver);
            LOGGER.info("  First daily cycle complete: {} clusters, {} edges",
                    stateStore.getClusters().size(), stateStore.getInterClusterEdges().size());

            // Log some names
            stateStore.getClusterNames().forEach((id, name) ->
                    LOGGER.info("  {} → {}", name.centerTownName(), name.clusterName()));

        } catch (Exception e) {
            LOGGER.error("World generation failed", e);
        }
    }

    // ── Public accessors for other adapter classes ──

    public static ModConfig getConfig() { return config; }
    public static NbtVillageStateStore getStateStore() { return stateStore; }
}
