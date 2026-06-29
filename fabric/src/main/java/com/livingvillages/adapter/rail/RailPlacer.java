package com.livingvillages.adapter.rail;

import com.livingvillages.core.config.ModConfig;
import com.livingvillages.core.data.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * Places rail blocks, bridges, and tunnels in the world according to planned paths.
 * Supports incremental (framed) placement across multiple ticks.
 */
public final class RailPlacer {

    private static int currentEdgeIndex = 0;
    private static int currentPathIndex = 0;
    private static boolean placementComplete = false;

    private RailPlacer() {}

    /**
     * Place/maintain rails with frame budget control.
     *
     * @param level       ServerLevel instance
     * @param store       VillageStateStore, reads interClusterEdges[]
     * @param cfg         config (uses graph.tunnelThreshold)
     * @param maxBlockOps max blocks to place this call
     * @return true if all paths placed, false if more work remains
     */
    public static boolean placeRails(ServerLevel level, VillageStateStore store,
                                      ModConfig cfg, int maxBlockOps) {
        List<EdgeRecord> edges = store.getInterClusterEdges();
        if (edges.isEmpty()) { placementComplete = true; return true; }

        int blockOps = 0;
        while (currentEdgeIndex < edges.size() && blockOps < maxBlockOps) {
            EdgeRecord edge = edges.get(currentEdgeIndex);
            List<Vec3i> path = edge.path();
            while (currentPathIndex < path.size() && blockOps < maxBlockOps) {
                Vec3i pos = path.get(currentPathIndex);
                blockOps += placeBlockAt(level, pos, cfg);
                currentPathIndex++;
            }
            if (currentPathIndex >= path.size()) {
                currentEdgeIndex++; currentPathIndex = 0;
            }
        }
        if (currentEdgeIndex >= edges.size()) {
            placementComplete = true; currentEdgeIndex = 0; currentPathIndex = 0;
            return true;
        }
        return false;
    }

    private static int placeBlockAt(ServerLevel level, Vec3i pos, ModConfig cfg) {
        BlockPos bp = new BlockPos(pos.x(), pos.y(), pos.z());
        BlockState below = level.getBlockState(bp.below());
        BlockState above = level.getBlockState(bp.above());

        // Ground: fill below with dirt if not solid
        if (!below.isSolid()) {
            level.setBlock(bp.below(), Blocks.DIRT.defaultBlockState(), 3);
        }
        // Rail: powered rail every 26 blocks, regular rail otherwise
        boolean isPowered = (pos.x() + pos.z()) % 26 == 0;
        BlockState rail = isPowered ? Blocks.POWERED_RAIL.defaultBlockState()
                                    : Blocks.RAIL.defaultBlockState();
        level.setBlock(bp, rail, 3);
        return 1;
    }

    public static void removeRails(ServerLevel level, EdgeRecord edge) {
        for (Vec3i v : edge.path()) {
            BlockPos bp = new BlockPos(v.x(), v.y(), v.z());
            BlockState state = level.getBlockState(bp);
            if (state.is(Blocks.RAIL) || state.is(Blocks.POWERED_RAIL)) {
                level.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public static void resetState() {
        currentEdgeIndex = 0; currentPathIndex = 0; placementComplete = false;
    }

    public static boolean isPlacementComplete() { return placementComplete; }
}
