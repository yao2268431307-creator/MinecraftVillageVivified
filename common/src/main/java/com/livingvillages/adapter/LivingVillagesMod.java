package com.livingvillages.adapter;

import com.livingvillages.adapter.biome.BiomeResolverImpl;
import com.livingvillages.adapter.config.ConfigLoader;
import com.livingvillages.adapter.data.NbtVillageStateStore;
import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.VillageStateStore;
import com.livingvillages.core.naming.BiomeResolver;
import com.livingvillages.core.orchestrator.TickOrchestrator;

/**
 * Main mod entry point for Living Villages.
 *
 * <p>MC Adapter layer. This class bridges Minecraft events to Core module calls.
 * Uses Architectury (@ExpectPlatform) for cross-loader compatibility.</p>
 *
 * <p>Note: Template stub. Full implementation requires:</p>
 * <ul>
 *   <li>{@code com.architectury.event.events} for event registration</li>
 *   <li>{@code net.minecraft.server.level.ServerLevel} for world events</li>
 *   <li>WorldGen, Chunk, and Tick event hooks</li>
 * </ul>
 */
public class LivingVillagesMod {

    /** Mod ID constant. */
    public static final String MOD_ID = "livingvillages";

    private static ModConfig config;
    private static VillageStateStore stateStore;
    private static BiomeResolver biomeResolver;

    /**
     * Called by Architectury on mod initialization.
     */
    public static void init() {
        // 1. Load config
        config = ConfigLoader.load();

        // 2. Initialize biome resolver
        biomeResolver = new BiomeResolverImpl();

        // 3. Register event hooks (MC Adapter specific):
        //    WorldGenEvents.init()        → inject custom structure placement
        //    ServerChunkEvents.CHUNK_LOAD → place village structures
        //    ServerLevelEvents.TICK       → daily cycle trigger
        //    ServerLevelEvents.SAVE       → NBT persistence
        //    ServerLevelEvents.UNLOAD     → cleanup
    }

    /** Called on world creation: generate village coordinates. */
    public static void onWorldLoad(long seed, int rangeX, int rangeZ) {
        if (stateStore == null) {
            stateStore = new NbtVillageStateStore();
        }
        TickOrchestrator.onWorldCreate(seed, rangeX, rangeZ, stateStore, config);
    }

    /** Called every daily tick interval (24000 ticks). */
    public static void onDailyTick() {
        if (stateStore != null && config != null && biomeResolver != null) {
            TickOrchestrator.runDailyCycle(stateStore, config, biomeResolver);
        }
    }

    /** Get the current state store (for RailPlacer, CaravanEntityManager). */
    public static VillageStateStore getStateStore() {
        return stateStore;
    }

    /** Get the current config. */
    public static ModConfig getConfig() {
        return config;
    }

    /** Get the biome resolver. */
    public static BiomeResolver getBiomeResolver() {
        return biomeResolver;
    }
}
