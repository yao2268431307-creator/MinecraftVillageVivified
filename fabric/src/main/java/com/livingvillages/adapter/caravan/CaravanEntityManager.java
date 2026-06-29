package com.livingvillages.adapter.caravan;

import com.livingvillages.core.data.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes CaravanState to MC minecart entities.
 * Only MOVING caravans have entities; others exist only as data.
 */
public final class CaravanEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("livingvillages");
    private static final Map<UUID, MinecartChest> entityMap = new HashMap<>();

    private CaravanEntityManager() {}

    public static void syncEntities(ServerLevel level, VillageStateStore store) {
        List<CaravanState> caravans = store.getCaravanStates();
        Set<UUID> seen = new HashSet<>();

        for (CaravanState cs : caravans) {
            seen.add(cs.caravanId());
            if (cs.phase() == CaravanPhase.MOVING) {
                if (!entityMap.containsKey(cs.caravanId())) {
                    MinecartChest cart = new MinecartChest(EntityType.CHEST_MINECART, level);
                    cart.setPos(cs.currentPathIndex(), 64, cs.currentPathIndex());
                    level.addFreshEntity(cart);
                    entityMap.put(cs.caravanId(), cart);
                }
            } else {
                MinecartChest cart = entityMap.remove(cs.caravanId());
                if (cart != null) cart.discard();
            }
        }
        // Clean up orphaned entities
        entityMap.keySet().removeIf(id -> !seen.contains(id));
    }

    public static void removeAllCaravanEntities(ServerLevel level) {
        for (MinecartChest cart : entityMap.values()) {
            cart.discard();
        }
        entityMap.clear();
    }

    public static boolean isRailIntact(ServerLevel level, EdgeRecord edge, int pathIndex) {
        // Check if block at path position is a rail
        return true; // TODO: check level.getBlockState()
    }
}
