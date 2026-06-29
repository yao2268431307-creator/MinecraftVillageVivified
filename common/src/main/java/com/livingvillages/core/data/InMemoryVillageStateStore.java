package com.livingvillages.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory implementation of {@link VillageStateStore} for testing.
 *
 * <p>All data is stored in {@link HashMap}s and {@link ArrayList}s.
 * {@link #markDirty()} is a no-op. Thread safety is <em>not</em> provided —
 * this class is intended for single-threaded JUnit tests.</p>
 */
public class InMemoryVillageStateStore implements VillageStateStore {

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

    // ── Villages ──

    @Override
    public List<VillageRecord> getVillages() {
        return villages;
    }

    @Override
    public void setVillages(List<VillageRecord> villages) {
        this.villages = Collections.unmodifiableList(new ArrayList<>(villages));
    }

    // ── Clusters ──

    @Override
    public List<ClusterRecord> getClusters() {
        return clusters;
    }

    @Override
    public void setClusters(List<ClusterRecord> clusters) {
        this.clusters = Collections.unmodifiableList(new ArrayList<>(clusters));
    }

    // ── Names ──

    @Override
    public Map<String, ClusterName> getClusterNames() {
        return clusterNames;
    }

    @Override
    public void setClusterNames(Map<String, ClusterName> names) {
        this.clusterNames = Collections.unmodifiableMap(new HashMap<>(names));
    }

    @Override
    public Map<UUID, String> getVillageNames() {
        return villageNames;
    }

    @Override
    public void setVillageNames(Map<UUID, String> names) {
        this.villageNames = Collections.unmodifiableMap(new HashMap<>(names));
    }

    // ── Edges ──

    @Override
    public List<EdgeRecord> getInterClusterEdges() {
        return interClusterEdges;
    }

    @Override
    public void setInterClusterEdges(List<EdgeRecord> edges) {
        this.interClusterEdges = Collections.unmodifiableList(new ArrayList<>(edges));
    }

    // ── Economy ──

    @Override
    public Map<String, Map<String, Double>> getPrices() {
        return prices;
    }

    @Override
    public void setPrices(Map<String, Map<String, Double>> prices) {
        // Deep defensive copy: each inner map is also copied
        Map<String, Map<String, Double>> copy = new HashMap<>();
        for (var entry : prices.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.prices = Collections.unmodifiableMap(copy);
    }

    @Override
    public Map<UUID, Map<String, Integer>> getWarehouses() {
        return warehouses;
    }

    @Override
    public void setWarehouses(Map<UUID, Map<String, Integer>> warehouses) {
        Map<UUID, Map<String, Integer>> copy = new HashMap<>();
        for (var entry : warehouses.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.warehouses = Collections.unmodifiableMap(copy);
    }

    // ── Caravans ──

    @Override
    public List<CaravanState> getCaravanStates() {
        return caravanStates;
    }

    @Override
    public void setCaravanStates(List<CaravanState> states) {
        this.caravanStates = Collections.unmodifiableList(new ArrayList<>(states));
    }

    // ── Levels ──

    @Override
    public Map<UUID, Integer> getVillageLevels() {
        return villageLevels;
    }

    @Override
    public void setVillageLevels(Map<UUID, Integer> levels) {
        this.villageLevels = Collections.unmodifiableMap(new HashMap<>(levels));
    }

    @Override
    public Map<UUID, Map<String, Long>> getAccumulatedConsumption() {
        return accumulatedConsumption;
    }

    @Override
    public void setAccumulatedConsumption(Map<UUID, Map<String, Long>> consumption) {
        Map<UUID, Map<String, Long>> copy = new HashMap<>();
        for (var entry : consumption.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.accumulatedConsumption = Collections.unmodifiableMap(copy);
    }

    @Override
    public Map<UUID, String> getSpecialities() {
        return specialities;
    }

    @Override
    public void setSpecialities(Map<UUID, String> specialities) {
        this.specialities = Collections.unmodifiableMap(new HashMap<>(specialities));
    }

    // ── Lifecycle ──

    @Override
    public void markDirty() {
        // no-op for in-memory store
    }
}
