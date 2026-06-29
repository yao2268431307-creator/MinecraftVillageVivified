package com.livingvillages.adapter.data;

import com.livingvillages.core.data.*;
import java.util.*;

/**
 * NBT-persisted implementation of VillageStateStore.
 *
 * <p>MC Adapter layer. Extends Minecraft SavedData and implements
 * the Core-defined VillageStateStore interface.</p>
 *
 * <p>Note: Template stub. Full implementation requires:</p>
 * <ul>
 *   <li>{@code net.minecraft.nbt.CompoundTag} for NBT serialization</li>
 *   <li>{@code net.minecraft.world.level.saveddata.SavedData} as base class</li>
 *   <li>{@code net.minecraft.nbt.ListTag} for list fields</li>
 *   <li>{@code net.minecraft.nbt.Tag} types for primitives</li>
 * </ul>
 *
 * <p>All 11 fields are serialized. markDirty() delegates to setDirty()
 * so Minecraft auto-saves the data.</p>
 */
public class NbtVillageStateStore implements VillageStateStore {

    // ── Internal storage ──
    private List<VillageRecord> villages = Collections.emptyList();
    private List<ClusterRecord> clusters = Collections.emptyList();
    private Map<String, ClusterName> clusterNames = Collections.emptyMap();
    private Map<UUID, String> villageNames = Collections.emptyMap();
    private List<EdgeRecord> interClusterEdges = Collections.emptyList();
    private Map<String, Map<String, Double>> prices = Collections.emptyMap();
    private Map<UUID, Map<String, Integer>> warehouses = Collections.emptyMap();
    private List<CaravanState> caravanStates = Collections.emptyList();
    private Map<UUID, Integer> villageLevels = Collections.emptyMap();
    private Map<UUID, Map<String, Long>> accumulatedConsumption = Collections.emptyMap();
    private Map<UUID, String> specialities = Collections.emptyMap();

    private boolean dirty = false;

    // ── MC Integration (stubs) ──

    /**
     * Load from NBT. MC: read CompoundTag and deserialize all fields.
     */
    public static NbtVillageStateStore load(/* CompoundTag tag */) {
        // MC: new NbtVillageStateStore(); parse tag; populate fields; return
        return new NbtVillageStateStore();
    }

    /**
     * Save to NBT. MC: write all fields to CompoundTag.
     */
    public /* CompoundTag */ Object save(/* CompoundTag tag */) {
        // MC: serialize all fields to tag; return tag
        dirty = false;
        return null; // stub
    }

    // ── VillageStateStore implementation ──

    @Override public List<VillageRecord> getVillages() { return villages; }
    @Override public void setVillages(List<VillageRecord> v) { this.villages = List.copyOf(v); dirty = true; }

    @Override public List<ClusterRecord> getClusters() { return clusters; }
    @Override public void setClusters(List<ClusterRecord> c) { this.clusters = List.copyOf(c); dirty = true; }

    @Override public Map<String, ClusterName> getClusterNames() { return clusterNames; }
    @Override public void setClusterNames(Map<String, ClusterName> n) { this.clusterNames = Map.copyOf(n); dirty = true; }

    @Override public Map<UUID, String> getVillageNames() { return villageNames; }
    @Override public void setVillageNames(Map<UUID, String> n) { this.villageNames = Map.copyOf(n); dirty = true; }

    @Override public List<EdgeRecord> getInterClusterEdges() { return interClusterEdges; }
    @Override public void setInterClusterEdges(List<EdgeRecord> e) { this.interClusterEdges = List.copyOf(e); dirty = true; }

    @Override public Map<String, Map<String, Double>> getPrices() { return prices; }
    @Override public void setPrices(Map<String, Map<String, Double>> p) {
        Map<String, Map<String, Double>> copy = new HashMap<>();
        for (var e : p.entrySet()) copy.put(e.getKey(), Map.copyOf(e.getValue()));
        this.prices = Map.copyOf(copy); dirty = true;
    }

    @Override public Map<UUID, Map<String, Integer>> getWarehouses() { return warehouses; }
    @Override public void setWarehouses(Map<UUID, Map<String, Integer>> w) {
        Map<UUID, Map<String, Integer>> copy = new HashMap<>();
        for (var e : w.entrySet()) copy.put(e.getKey(), Map.copyOf(e.getValue()));
        this.warehouses = Map.copyOf(copy); dirty = true;
    }

    @Override public List<CaravanState> getCaravanStates() { return caravanStates; }
    @Override public void setCaravanStates(List<CaravanState> s) { this.caravanStates = List.copyOf(s); dirty = true; }

    @Override public Map<UUID, Integer> getVillageLevels() { return villageLevels; }
    @Override public void setVillageLevels(Map<UUID, Integer> l) { this.villageLevels = Map.copyOf(l); dirty = true; }

    @Override public Map<UUID, Map<String, Long>> getAccumulatedConsumption() { return accumulatedConsumption; }
    @Override public void setAccumulatedConsumption(Map<UUID, Map<String, Long>> c) {
        Map<UUID, Map<String, Long>> copy = new HashMap<>();
        for (var e : c.entrySet()) copy.put(e.getKey(), Map.copyOf(e.getValue()));
        this.accumulatedConsumption = Map.copyOf(copy); dirty = true;
    }

    @Override public Map<UUID, String> getSpecialities() { return specialities; }
    @Override public void setSpecialities(Map<UUID, String> s) { this.specialities = Map.copyOf(s); dirty = true; }

    @Override
    public void markDirty() {
        dirty = true;
        // MC: super.setDirty(); // SavedData auto-save trigger
    }
}
