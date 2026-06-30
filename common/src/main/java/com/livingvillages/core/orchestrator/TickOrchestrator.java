package com.livingvillages.core.orchestrator;

import com.livingvillages.core.caravan.CaravanSimulator;
import com.livingvillages.core.cluster.ClusterDetector;
import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.Vec3i;
import com.livingvillages.core.data.VillageStateStore;
import com.livingvillages.core.economy.MarketSimulator;
import com.livingvillages.core.graph.RegionalGraph;
import com.livingvillages.core.level.LevelProgression;
import com.livingvillages.core.naming.BiomeResolver;
import com.livingvillages.core.naming.NameGenerator;
import com.livingvillages.core.villagegen.KCenterGenerator;

import java.util.List;

/**
 * Scheduler that knows all Core modules and invokes them in the correct order.
 *
 * <p>This is the only class allowed to import all module packages (intentional coupling).
 * Runs all scheduling logic in a single thread by default; MarketSimulator and
 * LevelProgression may run in parallel since they write to disjoint fields.</p>
 *
 * <p>Exception isolation: each module is wrapped in try-catch; one module failing
 * does not prevent others from executing.</p>
 */
public final class TickOrchestrator {

    private TickOrchestrator() {}

    /**
     * Called once on world creation. Clusters vanilla village positions.
     *
     * @param seed             world seed
     * @param vanillaPositions raw village positions from vanilla structure system
     * @param store            state store for persisting results
     * @param cfg              mod config
     */
    public static void onWorldCreate(long seed, List<Vec3i> vanillaPositions,
                                      VillageStateStore store, ModConfig cfg) {
        try {
            var villages = KCenterGenerator.generateClusteredVillages(seed, vanillaPositions, cfg);
            store.setVillages(villages);
            store.markDirty();
        } catch (Exception e) {
            System.err.println("[LivingVillages] FATAL: onWorldCreate failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Daily tick: run all modules in dependency order.
     *
     * <ol>
     *   <li>ClusterDetector (serial)</li>
     *   <li>NameGenerator (serial)</li>
     *   <li>RegionalGraph (serial)</li>
     *   <li>MarketSimulator + LevelProgression (parallel)</li>
     *   <li>CaravanSimulator (serial, after MarketSimulator)</li>
     *   <li>markDirty</li>
     * </ol>
     *
     * @param store         state store
     * @param cfg           mod config
     * @param biomeResolver biome resolver (MC Adapter injects)
     */
    public static void runDailyCycle(VillageStateStore store, ModConfig cfg,
                                      BiomeResolver biomeResolver) {
        // Phase 1: Cluster detection (serial)
        safeRun("ClusterDetector", () -> ClusterDetector.detectClusters(store, cfg));

        // Phase 2: Name generation (serial)
        safeRun("NameGenerator", () -> NameGenerator.generateNames(store, biomeResolver, cfg));

        // Phase 3: Regional graph (serial, may be framed)
        safeRun("RegionalGraph", () -> RegionalGraph.buildRegionalGraph(store, cfg));

        // Phase 4: Market + Level progression (parallel-safe — disjoint fields)
        safeRun("MarketSimulator", () -> MarketSimulator.simulateMarket(store, cfg));
        safeRun("LevelProgression", () -> LevelProgression.updateLevels(store));

        // Phase 5: Caravan simulation (serial, depends on MarketSimulator prices)
        safeRun("CaravanSimulator", () -> CaravanSimulator.simulateCaravans(store, cfg));

        // Mark data dirty for persistence
        try {
            store.markDirty();
        } catch (Exception e) {
            System.err.println("[LivingVillages] WARN: markDirty failed: " + e.getMessage());
        }
    }

    /**
     * Execute a module safely, catching and logging any exceptions.
     */
    private static void safeRun(String moduleName, Runnable module) {
        try {
            module.run();
        } catch (Exception e) {
            System.err.println("[LivingVillages] ERROR: Module " + moduleName
                    + " failed: " + e.getMessage());
            e.printStackTrace();
            // Continue to next module — do not abort the daily cycle
        }
    }
}
