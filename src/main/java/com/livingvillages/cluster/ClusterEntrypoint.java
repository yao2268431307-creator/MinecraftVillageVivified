package com.livingvillages.cluster;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * LivingVillages Cluster — hand-places village pieces at super-cell centers
 * after worldgen, spawns villagers, and connects them via RoadWeaver.
 *
 * <p>On each server tick, checks if the player is near an ungenerated
 * super-cell center. If so, places a plains house piece + 2 villagers there,
 * and tells RoadWeaver to connect it to the nearest existing cluster.</p>
 */
public class ClusterEntrypoint implements ModInitializer {

    public static final String MOD_ID = "livingvillages-cluster";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Super-cell size in blocks (= villagesPerCluster * spacing * 16 = 7 * 34 * 16 = 3808). */
    public static final int SUPER_CELL_SIZE = 7 * 34 * 16;

    /** Max distance (in blocks) from player to super-cell center to trigger generation. */
    public static final int TRIGGER_DISTANCE = 200;

    private static final Set<Long> generatedSuperCells = new HashSet<>();
    private static ServerLevel cachedOverworld = null;
    private static int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("=== LivingVillages Cluster initializing ===");

        ServerWorldEvents.LOAD.register((server, level) -> {
            if (level.dimension() == Level.OVERWORLD) {
                cachedOverworld = level;
                generatedSuperCells.clear();
                LOGGER.info("World loaded, cleared super-cell cache. seed={}", level.getSeed());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        LOGGER.info("=== LivingVillages Cluster initialized ===");
    }

    private void onServerTick(MinecraftServer server) {
        tickCounter++;
        // Only check every 20 ticks (1 second) to reduce overhead
        if (tickCounter % 20 != 0) return;
        if (cachedOverworld == null) return;

        // Find nearest player
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        var player = players.get(0);
        int px = player.getBlockX();
        int pz = player.getBlockZ();

        // Find nearest ungenerated super-cell center
        int superX = Math.floorDiv(px, SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(pz, SUPER_CELL_SIZE);

        // Check 3x3 neighborhood of super-cells
        for (int dsx = -1; dsx <= 1; dsx++) {
            for (int dsz = -1; dsz <= 1; dsz++) {
                int sx = superX + dsx;
                int sz = superZ + dsz;
                long key = ((long) sx << 32) | (sz & 0xFFFF_FFFFL);
                if (generatedSuperCells.contains(key)) continue;

                // Center of super-cell in block coords
                int centerX = sx * SUPER_CELL_SIZE + SUPER_CELL_SIZE / 2;
                int centerZ = sz * SUPER_CELL_SIZE + SUPER_CELL_SIZE / 2;

                double dist = Math.sqrt(
                    Math.pow(centerX - px, 2) + Math.pow(centerZ - pz, 2));
                if (dist > TRIGGER_DISTANCE) continue;

                // Generate!
                LOGGER.info("Generating cluster at super-cell ({},{}) block ({},{}) | dist={}",
                        sx, sz, centerX, centerZ, (int) dist);
                try {
                    VillagePlacer.placeCluster(cachedOverworld, centerX, centerZ, sx, sz);
                    generatedSuperCells.add(key);
                } catch (Exception e) {
                    LOGGER.error("Cluster generation failed at ({},{})", centerX, centerZ, e);
                    generatedSuperCells.add(key); // don't retry forever
                }
            }
        }
    }

    /** Called by VillagePlacer to register a generated cluster's position for RoadWeaver connection. */
    public static void markGenerated(int superX, int superZ) {
        long key = ((long) superX << 32) | (superZ & 0xFFFF_FFFFL);
        generatedSuperCells.add(key);
    }
}
