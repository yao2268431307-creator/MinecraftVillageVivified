package com.livingvillages.adapter.road;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * Places roads between cluster centers based on RegionalGraph edges.
 * RoadWeaver-style: flat stone path with border, follows A* path with Bezier smoothing.
 */
public final class RoadPlacer {

    private static int currentEdgeIndex = 0;
    private static int currentPathIndex = 0;

    private RoadPlacer() {}

    /**
     * Place roads for all edges. Supports framed execution.
     * @return true if all roads placed
     */
    public static boolean placeRoads(ServerLevel level, VillageStateStore store,
                                      ModConfig cfg, int maxBlockOps) {
        List<EdgeRecord> edges = store.getInterClusterEdges();
        if (edges.isEmpty()) return true;

        Block roadBlock = getRoadBlock(cfg.roadMaterial());
        int roadWidth = cfg.roadWidth();
        int blockOps = 0;

        while (currentEdgeIndex < edges.size() && blockOps < maxBlockOps) {
            EdgeRecord edge = edges.get(currentEdgeIndex);
            List<Vec3i> path = edge.path();
            while (currentPathIndex < path.size() && blockOps < maxBlockOps) {
                Vec3i p = path.get(currentPathIndex);
                blockOps += placeRoadSegment(level, p, roadBlock, roadWidth);
                currentPathIndex++;
            }
            if (currentPathIndex >= path.size()) {
                currentEdgeIndex++; currentPathIndex = 0;
            }
        }
        if (currentEdgeIndex >= edges.size()) {
            currentEdgeIndex = 0; currentPathIndex = 0; return true;
        }
        return false;
    }

    private static int placeRoadSegment(ServerLevel level, Vec3i center, Block road, int width) {
        int ops = 0;
        int half = width / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                BlockPos bp = new BlockPos(center.x() + dx, center.y(), center.z() + dz);
                // Remove obstacles above road
                for (int y = 1; y <= 3; y++) {
                    BlockPos above = bp.above(y);
                    BlockState aboveState = level.getBlockState(above);
                    if (aboveState.isSolid()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                        ops++;
                    }
                }
                // Replace ground with road
                BlockPos ground = bp.below();
                if (!level.getBlockState(ground).isSolid()) {
                    level.setBlock(ground, Blocks.DIRT.defaultBlockState(), 3);
                    ops++;
                }
                level.setBlock(bp, road.defaultBlockState(), 3);
                ops++;
            }
        }
        return ops;
    }

    private static Block getRoadBlock(String materialId) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(materialId))
            .orElse(Blocks.STONE_BRICKS);
    }
}
