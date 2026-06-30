package com.livingvillages.adapter.client;

import com.livingvillages.adapter.data.NbtVillageStateStore;
import com.livingvillages.core.data.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import java.util.*;

/**
 * Displays the current region name (聚落——村庄) as a floating title
 * when the player enters a new cluster's Voronoi region.
 */
public final class RegionTitleDisplay {

    private static String lastRegion = "";
    private static VillageStateStore cachedStore;

    private RegionTitleDisplay() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null || player.level() == null) return;

            // Access state store via LivingVillagesFabric
            if (cachedStore == null) { try { cachedStore = com.livingvillages.adapter.fabric.LivingVillagesFabric.getStateStore(); } catch (Exception e) {} }
            if (cachedStore == null) return;

            String currentRegion = findRegion(player.getX(), player.getY(), player.getZ(), cachedStore);
            if (!currentRegion.isEmpty() && !currentRegion.equals(lastRegion)) {
                lastRegion = currentRegion;
                player.displayClientMessage(
                    Component.literal(currentRegion).withStyle(style ->
                        style.withBold(true).withItalic(false)),
                    true); // action bar
                player.displayClientMessage(
                    Component.literal("§6⛫ " + currentRegion),
                    false); // chat-like floating
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastRegion = "";
            cachedStore = null;
        });
    }

    public static void setStateStore(VillageStateStore store) {
        cachedStore = store;
    }

    /**
     * Find the closest cluster center and return "聚落名——村名".
     */
    private static String findRegion(double px, double py, double pz, VillageStateStore store) {
        Map<String, ClusterName> clusterNames = store.getClusterNames();
        Map<UUID, String> villageNames = store.getVillageNames();
        List<ClusterRecord> clusters = store.getClusters();
        List<VillageRecord> villages = store.getVillages();

        if (clusters.isEmpty() || villages.isEmpty()) return "";

        // Build lookup
        Map<UUID, VillageRecord> villageById = new HashMap<>();
        for (VillageRecord v : villages) villageById.put(v.id(), v);

        // Find nearest cluster center
        ClusterRecord nearestCluster = null;
        double minDist = Double.MAX_VALUE;
        for (ClusterRecord c : clusters) {
            VillageRecord cv = villageById.get(c.centerVillageId());
            if (cv == null) continue;
            double dx = px - cv.position().x();
            double dz = pz - cv.position().z();
            double dist = dx * dx + dz * dz;
            if (dist < minDist) { minDist = dist; nearestCluster = c; }
        }

        if (nearestCluster == null) return "";

        // Find nearest village within that cluster
        VillageRecord nearestVillage = null;
        minDist = Double.MAX_VALUE;
        for (UUID vid : nearestCluster.memberVillageIds()) {
            VillageRecord v = villageById.get(vid);
            if (v == null) continue;
            double dx = px - v.position().x();
            double dz = pz - v.position().z();
            double dist = dx * dx + dz * dz;
            if (dist < minDist) { minDist = dist; nearestVillage = v; }
        }

        if (nearestVillage == null) return "";

        ClusterName cn = clusterNames.get(nearestCluster.id());
        String clusterName = cn != null ? cn.clusterName() : nearestCluster.id();
        String villageName = villageNames.getOrDefault(nearestVillage.id(), "");

        if (villageName.isEmpty()) return clusterName;
        return clusterName + "——" + villageName;
    }
}
