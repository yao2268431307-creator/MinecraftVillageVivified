package com.livingvillages.core.data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single interface for reading and writing all Village data.
 *
 * <p>Each field has exactly one writer module (constraint V1).
 * Thread safety is the implementer's responsibility; Core modules assume single-threaded
 * calls that are guaranteed by {@code TickOrchestrator}.</p>
 *
 * <h3>Writer assignments</h3>
 * <ul>
 *   <li>villages[] — §1 KCenterGenerator</li>
 *   <li>clusters[] — §2 ClusterDetector</li>
 *   <li>clusterNames[], villageNames[] — §3 NameGenerator</li>
 *   <li>interClusterEdges[] — §4 RegionalGraph</li>
 *   <li>prices[], warehouses[] — §6 MarketSimulator (+ §7 CaravanSimulator for warehouses)</li>
 *   <li>caravanStates[] — §7 CaravanSimulator</li>
 *   <li>villageLevels[], accumulatedConsumption[], specialities[] — §8 LevelProgression</li>
 * </ul>
 */
public interface VillageStateStore {

    // ── Villages (§1 writes) ──

    /** @return immutable view or defensive copy — caller must not rely on mutability */
    List<VillageRecord> getVillages();

    void setVillages(List<VillageRecord> villages);

    // ── Clusters (§2 writes) ──

    List<ClusterRecord> getClusters();

    void setClusters(List<ClusterRecord> clusters);

    // ── Names (§3 writes) ──

    /** @return map keyed by clusterId */
    Map<String, ClusterName> getClusterNames();

    void setClusterNames(Map<String, ClusterName> names);

    /** @return map keyed by villageId */
    Map<UUID, String> getVillageNames();

    void setVillageNames(Map<UUID, String> names);

    // ── Edges (§4 writes) ──

    List<EdgeRecord> getInterClusterEdges();

    void setInterClusterEdges(List<EdgeRecord> edges);

    // ── Economy (§6 writes) ──

    /** @return outer key = clusterId, inner key = itemId */
    Map<String, Map<String, Double>> getPrices();

    void setPrices(Map<String, Map<String, Double>> prices);

    /** @return outer key = villageId, inner key = itemId */
    Map<UUID, Map<String, Integer>> getWarehouses();

    void setWarehouses(Map<UUID, Map<String, Integer>> warehouses);

    // ── Caravans (§7 writes) ──

    List<CaravanState> getCaravanStates();

    void setCaravanStates(List<CaravanState> states);

    // ── Levels (§8 writes) ──

    Map<UUID, Integer> getVillageLevels();

    void setVillageLevels(Map<UUID, Integer> levels);

    Map<UUID, Map<String, Long>> getAccumulatedConsumption();

    void setAccumulatedConsumption(Map<UUID, Map<String, Long>> consumption);

    Map<UUID, String> getSpecialities();

    void setSpecialities(Map<UUID, String> specialities);

    // ── Lifecycle ──

    /**
     * Notify the persistence layer that data is dirty and needs saving.
     * Called by the orchestrator after every write.
     */
    void markDirty();
}
