package com.livingvillages.cluster;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.shiroha233.roadweaver.api.RoadNetworkApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Places a village piece + villagers at a super-cell center, then connects it
 * to nearby clusters via RoadWeaver.
 *
 * <p>Uses vanilla plains house NBT template (minecraft:village/plains/houses/plains_small_house_1).
 * Spawns 2 villagers per cluster with MobSpawnType.STRUCTURE.
 * Calls RoadNetworkApi.ensureConnection to link this cluster with previous ones.
 */
public final class VillagePlacer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterEntrypoint.MOD_ID);

    /** Vanilla plains house template. */
    private static final ResourceLocation HOUSE_TEMPLATE =
        new ResourceLocation("minecraft:village/plains/houses/plains_small_house_1");

    private static final List<BlockPos> generatedCenters = new ArrayList<>();

    private VillagePlacer() {}

    /**
     * Place a cluster at the given block position.
     *
     * @param level    server overworld
     * @param centerX  block X of super-cell center
     * @param centerZ  block Z of super-cell center
     * @param superX   super-cell X index (for logging)
     * @param superZ   super-cell Z index (for logging)
     */
    public static void placeCluster(ServerLevel level, int centerX, int centerZ,
                                     int superX, int superZ) {
        // Sample terrain height at center
        int terrainY = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
            centerX, centerZ);
        BlockPos anchor = new BlockPos(centerX, terrainY, centerZ);
        LOGGER.info("Placing cluster at ({},{},{}) terrainY={}", centerX, terrainY, centerZ, terrainY);

        // 1. Place the house NBT template
        boolean placed = placeHouseTemplate(level, anchor);
        if (!placed) {
            LOGGER.warn("House placement failed at {}", anchor);
            return;
        }

        // 2. Spawn 2 villagers near the house entrance
        spawnVillagers(level, anchor, 2);

        // 3. Tell RoadWeaver to connect this cluster with previous ones
        connectWithRoadWeaver(level, anchor);

        generatedCenters.add(anchor);
        ClusterEntrypoint.markGenerated(superX, superZ);
        LOGGER.info("Cluster placed: {} total centers", generatedCenters.size());
    }

    /**
     * Place the vanilla plains house template at the anchor position.
     */
    private static boolean placeHouseTemplate(ServerLevel level, BlockPos anchor) {
        StructureTemplateManager manager = level.getStructureManager();
        // 1.20.1: getOrCreate(ResourceLocation) returns a single StructureTemplate
        StructureTemplate template = manager.getOrCreate(HOUSE_TEMPLATE);
        if (template == null) {
            LOGGER.error("Template not found: {}", HOUSE_TEMPLATE);
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(Rotation.NONE)
            .setIgnoreEntities(false);

        RandomSource random = RandomSource.create(anchor.asLong());
        boolean success = template.placeInWorld(level, anchor, anchor, settings, random, 2);
        LOGGER.info("Template {} placed at {} success={}", HOUSE_TEMPLATE, anchor, success);
        return success;
    }

    /**
     * Spawn N villagers near the anchor position (in front of the house).
     */
    private static void spawnVillagers(ServerLevel level, BlockPos anchor, int count) {
        RandomSource random = RandomSource.create(anchor.asLong() ^ 0xDEADBEEFL);
        for (int i = 0; i < count; i++) {
            // Spawn position: offset by 1-2 blocks from anchor, on ground
            double x = anchor.getX() + 1.5 + (random.nextDouble() - 0.5) * 1.5;
            double z = anchor.getZ() + 1.5 + (random.nextDouble() - 0.5) * 1.5;
            double y = anchor.getY();

            Entity entity = EntityType.VILLAGER.create(level);
            if (entity == null) {
                LOGGER.warn("VILLAGER.create returned null");
                continue;
            }
            entity.moveTo(x, y, z, random.nextFloat() * 360.0f, 0.0f);
            if (entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(
                    (int) x, (int) y, (int) z)), MobSpawnType.STRUCTURE, null, null);
                mob.setPersistenceRequired();
            }
            if (level.addFreshEntity(entity)) {
                LOGGER.info("Spawned villager at ({},{},{})", x, y, z);
            } else {
                LOGGER.warn("addFreshEntity failed for villager");
            }
        }
    }

    /**
     * Connect this cluster with all previously generated clusters via RoadWeaver.
     * For minimum viable version: connect with the most recent previous center.
     */
    private static void connectWithRoadWeaver(ServerLevel level, BlockPos current) {
        if (generatedCenters.isEmpty()) {
            LOGGER.info("First cluster — nothing to connect to yet");
            return;
        }
        // Connect with most recent previous center
        BlockPos previous = generatedCenters.get(generatedCenters.size() - 1);
        try {
            RoadNetworkApi.ensureConnection(level, previous, current);
            LOGGER.info("RoadWeaver ensureConnection: {} → {}", previous, current);
        } catch (Throwable t) {
            // RoadWeaver might not be loaded or API might differ — don't crash
            LOGGER.warn("RoadWeaver ensureConnection failed (RoadWeaver not loaded?): {}", t.getMessage());
        }
    }
}
