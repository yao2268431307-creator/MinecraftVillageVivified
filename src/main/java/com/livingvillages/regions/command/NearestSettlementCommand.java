package com.livingvillages.regions.command;

import com.livingvillages.regions.data.RegionStateStore;
import com.livingvillages.regions.naming.SettlementName;
import com.livingvillages.regions.terrain.TerrainModifier;
import com.livingvillages.regions.terrain.TerrainResolver;
import com.livingvillages.regions.biome.BiomeRegionResolver;
import com.livingvillages.regions.biome.RegionType;
import com.livingvillages.regions.tier.SettlementTier;
import com.mojang.brigadier.context.CommandContext;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Server-side commands to teleport to the nearest settlement of a given tier.
 *
 * <p>Cities are rare (~1 in 6 settlements) and finding one on foot is slow, so
 * this provides {@code /nearestcity} and {@code /nearesttown} that jump the
 * player to the closest discovered settlement of the requested tier. Only
 * settlements the {@code RoadWeaverIntegrator} has already discovered (in
 * {@link RegionStateStore}) are candidates &mdash; exploring more of the world
 * reveals more cities to teleport to.</p>
 *
 * <p>The tier is the pure deterministic {@link SettlementTier#tierFor}, so the
 * command just scans the known settlement chunks and picks the closest match;
 * it does not need any new persisted state. The destination lands on the
 * settlement's surface (heightmap) so the player arrives on solid ground.</p>
 */
public final class NearestSettlementCommand {

    private NearestSettlementCommand() {
        // utility class
    }

    /** Teleport the executing player to the nearest discovered CITY. */
    public static int nearestCity(CommandContext<CommandSourceStack> ctx) {
        return teleportToNearest(ctx, SettlementTier.CITY);
    }

    /** Teleport the executing player to the nearest discovered TOWN. */
    public static int nearestTown(CommandContext<CommandSourceStack> ctx) {
        return teleportToNearest(ctx, SettlementTier.TOWN);
    }

    /**
     * Find the nearest discovered settlement of {@code targetTier} to the
     * player and teleport them to its surface centre.
     *
     * @return 1 on success (teleported), 0 if no such settlement is known
     */
    private static int teleportToNearest(CommandContext<CommandSourceStack> ctx, SettlementTier targetTier) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer player;
        try {
            player = src.getPlayerOrException();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            src.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
        MinecraftServer server = player.getServer();
        ServerLevel overworld = server == null ? null : server.overworld();
        if (overworld == null) {
            src.sendFailure(Component.literal("Overworld is not available."));
            return 0;
        }
        long seed = overworld.getSeed();
        RegionStateStore store = RegionStateStore.load(overworld);
        Set<Long> settlements = store.processedChunkKeys();

        int px = player.getBlockX();
        int pz = player.getBlockZ();
        long bestDist2 = Long.MAX_VALUE;
        int bestCx = 0;
        int bestCz = 0;
        for (long key : settlements) {
            int cx = RegionStateStore.chunkX(key);
            int cz = RegionStateStore.chunkZ(key);
            SettlementTier tier = SettlementTier.tierFor(seed, cx, cz);
            if (tier != targetTier) {
                continue;
            }
            int centerX = (cx << 4) + 8;
            int centerZ = (cz << 4) + 8;
            long dx = (long) px - centerX;
            long dz = (long) pz - centerZ;
            long dist2 = dx * dx + dz * dz;
            if (dist2 < bestDist2) {
                bestDist2 = dist2;
                bestCx = cx;
                bestCz = cz;
            }
        }

        if (bestDist2 == Long.MAX_VALUE) {
            src.sendFailure(Component.literal(
                "No " + targetTier.suffixLabel() + " has been discovered yet. "
                + "Explore more to find one."));
            return 0;
        }

        int centerX = (bestCx << 4) + 8;
        int centerZ = (bestCz << 4) + 8;
        // Land on the surface so the player arrives on solid ground, not buried/above.
        BlockPos surface = overworld.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
            new BlockPos(centerX, 0, centerZ));
        double x = surface.getX() + 0.5;
        double y = surface.getY();
        double z = surface.getZ() + 0.5;
        // teleportTo(x,y,z) keeps the player's current facing; force-load first so they don't fall.
        forceLoadAround(overworld, bestCx, bestCz);
        player.teleportTo(x, y, z);

        String name = settlementName(overworld, seed, bestCx, bestCz, targetTier);
        final String destName = name;
        final int destCx = bestCx;
        final int destCz = bestCz;
        final String tierLabel = targetTier.suffixLabel();
        src.sendSuccess(() -> Component.literal(
            "Teleported to nearest " + tierLabel + ": " + destName
            + " (chunk " + destCx + ", " + destCz + ")"), false);
        return 1;
    }

    /** Resolve a settlement's display name (reuses the client naming path, server-side). */
    private static String settlementName(ServerLevel level, long seed, int chunkX, int chunkZ, SettlementTier tier) {
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        RegionType type = BiomeRegionResolver.resolveRegionType(level, centerX, centerZ);
        if (type == null) {
            return "unknown";
        }
        TerrainModifier.Kind terrain = TerrainResolver.resolve(level, new BlockPos(centerX, 64, centerZ), type);
        return SettlementName.format(seed, chunkX, chunkZ, type, tier, terrain);
    }

    /** Force-load a small ring of chunks around the destination so the player doesn't fall through. */
    private static void forceLoadAround(ServerLevel level, int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.getChunk(chunkX + dx, chunkZ + dz);
            }
        }
    }
}
