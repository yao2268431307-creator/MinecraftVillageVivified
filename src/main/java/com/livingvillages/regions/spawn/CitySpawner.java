package com.livingvillages.regions.spawn;

import com.livingvillages.regions.data.RegionStateStore;
import com.livingvillages.regions.tier.SettlementTier;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side settlement population spawner.
 *
 * <p>Makes towns and cities worth traveling to by topping up villagers to a
 * tier-based target and, at cities, spawning a permanent wandering-trader
 * resident at the centre so a city is a trade hub you can return to. Villages
 * are left at vanilla population; vanilla roaming wandering traders are
 * untouched.</p>
 *
 * <table>
 *   <tr><th>Tier</th><th>Villager target</th><th>Trader</th></tr>
 *   <tr><td>VILLAGE</td><td>vanilla (~10)</td><td>none</td></tr>
 *   <tr><td>TOWN</td><td>{@value #TOWN_TARGET}</td><td>none</td></tr>
 *   <tr><td>CITY</td><td>{@value #CITY_TARGET}</td><td>1 permanent resident</td></tr>
 * </table>
 *
 * <p>The set of settlements comes from {@link RegionStateStore#processedChunkKeys()}
 * (every village the {@code RoadWeaverIntegrator} has discovered); the tier is the
 * pure deterministic {@link SettlementTier#tierFor}, so no extra state is
 * persisted. Spawns only happen in loaded chunks near a player (no entity spam
 * in far-away or unloaded cities), are capped per pass, and run on a throttle
 * ({@link #TICK_THROTTLE}), so TPS impact is bounded.</p>
 *
 * <p>Spawned villagers and the city trader are marked persistent
 * ({@link net.minecraft.world.entity.Mob#setPersistenceRequired()}) so they do
 * not despawn; the trader also gets a very large despawn delay so it lingers
 * indefinitely as the city's trade hub.</p>
 */
public final class CitySpawner {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");

    /** Villager target for towns. */
    private static final int TOWN_TARGET = 20;
    /** Villager target for cities. */
    private static final int CITY_TARGET = 40;
    /** Radius (blocks) around a settlement centre for counting/spawning population. */
    private static final int POP_RADIUS = 48;
    /** Population count/spawn diameter (derived from {@link #POP_RADIUS}). */
    private static final int DIAMETER = POP_RADIUS * 2;
    /** Only spawn in settlements within this many blocks of an online player. */
    private static final int NEAR_PLAYER_BLOCKS = 128;
    /** Max villagers to spawn per settlement per pass (spreads the top-up over passes). */
    private static final int SPAWNS_PER_PASS = 5;
    /** Tick interval between spawn passes (400 = 20s on a 20 tps server). */
    public static final int TICK_THROTTLE = 400;

    private CitySpawner() {
        // utility class
    }

    /**
     * Called from {@code ServerTickEvents.END_SERVER_TICK} once per
     * {@value #TICK_THROTTLE} ticks. Tops up villagers in loaded, near-player
     * towns/cities and ensures each city has a persistent wandering trader.
     *
     * @param server the minecraft server (must not be {@code null})
     */
    public static void onTick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        long seed = overworld.getSeed();
        RegionStateStore store = RegionStateStore.load(overworld);
        Set<Long> settlements = store.processedChunkKeys();
        if (settlements.isEmpty()) {
            return;
        }

        for (long key : settlements) {
            int cx = RegionStateStore.chunkX(key);
            int cz = RegionStateStore.chunkZ(key);
            // Only spawn in loaded chunks.
            if (overworld.getChunkSource().getChunkNow(cx, cz) == null) {
                continue;
            }
            int centerX = (cx << 4) + 8;
            int centerZ = (cz << 4) + 8;

            // Only spawn in settlements near at least one online player.
            if (!isNearAnyPlayer(players, centerX, centerZ, NEAR_PLAYER_BLOCKS)) {
                continue;
            }

            SettlementTier tier = SettlementTier.tierFor(seed, cx, cz);
            if (tier == SettlementTier.VILLAGE) {
                continue;
            }

            AABB box = AABB.ofSize(Vec3.atCenterOf(new BlockPos(centerX, 64, centerZ)),
                DIAMETER, DIAMETER, DIAMETER);
            try {
                topUpVillagers(overworld, tier, centerX, centerZ, box);
                if (tier == SettlementTier.CITY) {
                    ensureCityTrader(overworld, centerX, centerZ, box);
                }
            } catch (Throwable t) {
                LOGGER.warn("CitySpawner failure at chunk=({}, {}): {}", cx, cz, t.getMessage());
            }
        }
    }

    private static void topUpVillagers(ServerLevel level, SettlementTier tier,
                                       int centerX, int centerZ, AABB box) {
        int target = tier == SettlementTier.CITY ? CITY_TARGET : TOWN_TARGET;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, box);
        int deficit = target - villagers.size();
        if (deficit <= 0) {
            return;
        }
        int toSpawn = Math.min(deficit, SPAWNS_PER_PASS);
        BlockPos surface = findSurface(level, centerX, centerZ);
        if (surface == null) {
            return;
        }
        for (int i = 0; i < toSpawn; i++) {
            try {
                Villager v = EntityType.VILLAGER.spawn(level, surface, MobSpawnType.NATURAL);
                if (v != null) {
                    v.setPersistenceRequired();
                }
            } catch (Throwable t) {
                LOGGER.warn("Villager spawn failed at ({}, {}): {}", centerX, centerZ, t.getMessage());
                return;
            }
        }
        if (DEBUG) {
            LOGGER.info("[LV-spawn] tier={} villagers {}/{} (spawned {}) at ({}, {})",
                tier, villagers.size(), target, toSpawn, centerX, centerZ);
        }
    }

    private static void ensureCityTrader(ServerLevel level, int centerX, int centerZ, AABB box) {
        List<WanderingTrader> traders = level.getEntitiesOfClass(WanderingTrader.class, box);
        if (!traders.isEmpty()) {
            return;
        }
        BlockPos surface = findSurface(level, centerX, centerZ);
        if (surface == null) {
            return;
        }
        try {
            WanderingTrader wt = EntityType.WANDERING_TRADER.spawn(level, surface, MobSpawnType.NATURAL);
            if (wt != null) {
                wt.setPersistenceRequired();
                wt.setDespawnDelay(Integer.MAX_VALUE / 2); // linger indefinitely
                if (DEBUG) {
                    LOGGER.info("[LV-spawn] city trader spawned at ({}, {})", centerX, centerZ);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Wandering trader spawn failed at ({}, {}): {}", centerX, centerZ, t.getMessage());
        }
    }

    /** Find a surface spawn position (air above solid) at the settlement centre. */
    private static BlockPos findSurface(ServerLevel level, int centerX, int centerZ) {
        try {
            return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(centerX, 0, centerZ)).above();
        } catch (Throwable t) {
            // Heightmap may be unavailable if the chunk column isn't ready; skip this pass.
            return null;
        }
    }

    private static boolean isNearAnyPlayer(List<ServerPlayer> players, int x, int z, int radius) {
        long r2 = (long) radius * radius;
        for (ServerPlayer p : players) {
            long dx = (long) p.getBlockX() - x;
            long dz = (long) p.getBlockZ() - z;
            if (dx * dx + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    /** Emit diagnostic logs while debugging; false for normal play. */
    private static final boolean DEBUG = false;
}
