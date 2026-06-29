package com.livingvillages.adapter.data;

import com.livingvillages.core.data.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/**
 * NBT-persisted VillageStateStore. Extends Minecraft SavedData.
 */
public class NbtVillageStateStore extends SavedData implements VillageStateStore {

    private static final String DATA_NAME = "livingvillages_data";

    private List<VillageRecord> villages = new ArrayList<>();
    private List<ClusterRecord> clusters = new ArrayList<>();
    private Map<String, ClusterName> clusterNames = new HashMap<>();
    private Map<UUID, String> villageNames = new HashMap<>();
    private List<EdgeRecord> interClusterEdges = new ArrayList<>();
    private Map<String, Map<String, Double>> prices = new HashMap<>();
    private Map<UUID, Map<String, Integer>> warehouses = new HashMap<>();
    private List<CaravanState> caravanStates = new ArrayList<>();
    private Map<UUID, Integer> villageLevels = new HashMap<>();
    private Map<UUID, Map<String, Long>> accumulatedConsumption = new HashMap<>();
    private Map<UUID, String> specialities = new HashMap<>();

    // ── SavedData lifecycle ──

    public static NbtVillageStateStore load(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(NbtVillageStateStore::new, NbtVillageStateStore::fromNbt, null),
            DATA_NAME
        );
    }

    private static NbtVillageStateStore fromNbt(CompoundTag tag,
                                                 net.minecraft.core.HolderLookup.Provider provider) {
        NbtVillageStateStore store = new NbtVillageStateStore();
        // TODO: deserialize all 11 fields from tag
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag,
                            net.minecraft.core.HolderLookup.Provider provider) {
        // TODO: serialize all 11 fields to tag
        return tag;
    }

    // ── VillageStateStore getters/setters ──

    @Override public List<VillageRecord> getVillages() { return Collections.unmodifiableList(villages); }
    @Override public void setVillages(List<VillageRecord> v) { villages = new ArrayList<>(v); setDirty(); }

    @Override public List<ClusterRecord> getClusters() { return Collections.unmodifiableList(clusters); }
    @Override public void setClusters(List<ClusterRecord> c) { clusters = new ArrayList<>(c); setDirty(); }

    @Override public Map<String, ClusterName> getClusterNames() { return Collections.unmodifiableMap(clusterNames); }
    @Override public void setClusterNames(Map<String, ClusterName> n) { clusterNames = new HashMap<>(n); setDirty(); }

    @Override public Map<UUID, String> getVillageNames() { return Collections.unmodifiableMap(villageNames); }
    @Override public void setVillageNames(Map<UUID, String> n) { villageNames = new HashMap<>(n); setDirty(); }

    @Override public List<EdgeRecord> getInterClusterEdges() { return Collections.unmodifiableList(interClusterEdges); }
    @Override public void setInterClusterEdges(List<EdgeRecord> e) { interClusterEdges = new ArrayList<>(e); setDirty(); }

    @Override public Map<String, Map<String, Double>> getPrices() { return Collections.unmodifiableMap(prices); }
    @Override public void setPrices(Map<String, Map<String, Double>> p) {
        prices = new HashMap<>(); p.forEach((k, v) -> prices.put(k, new HashMap<>(v))); setDirty();
    }

    @Override public Map<UUID, Map<String, Integer>> getWarehouses() { return Collections.unmodifiableMap(warehouses); }
    @Override public void setWarehouses(Map<UUID, Map<String, Integer>> w) {
        warehouses = new HashMap<>(); w.forEach((k, v) -> warehouses.put(k, new HashMap<>(v))); setDirty();
    }

    @Override public List<CaravanState> getCaravanStates() { return Collections.unmodifiableList(caravanStates); }
    @Override public void setCaravanStates(List<CaravanState> s) { caravanStates = new ArrayList<>(s); setDirty(); }

    @Override public Map<UUID, Integer> getVillageLevels() { return Collections.unmodifiableMap(villageLevels); }
    @Override public void setVillageLevels(Map<UUID, Integer> l) { villageLevels = new HashMap<>(l); setDirty(); }

    @Override public Map<UUID, Map<String, Long>> getAccumulatedConsumption() { return Collections.unmodifiableMap(accumulatedConsumption); }
    @Override public void setAccumulatedConsumption(Map<UUID, Map<String, Long>> c) {
        accumulatedConsumption = new HashMap<>(); c.forEach((k, v) -> accumulatedConsumption.put(k, new HashMap<>(v))); setDirty();
    }

    @Override public Map<UUID, String> getSpecialities() { return Collections.unmodifiableMap(specialities); }
    @Override public void setSpecialities(Map<UUID, String> s) { specialities = new HashMap<>(s); setDirty(); }

    @Override
    public void markDirty() {
        setDirty();
    }
}
