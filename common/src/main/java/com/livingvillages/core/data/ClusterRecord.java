package com.livingvillages.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A cluster (聚落) produced by DBSCAN grouping of villages.
 *
 * <p>Writer: §2 ClusterDetector.</p>
 *
 * @param id                unique cluster id, derived as "cluster_" + hash(centerVillage.position)
 * @param memberVillageIds  village ids belonging to this cluster (defensive copy)
 * @param centerVillageId   member with highest bedCount
 * @param isNamed           whether §3 NameGenerator has processed this cluster
 * @param edgesBuilt        whether §4 RegionalGraph has processed this cluster
 */
public record ClusterRecord(
        String id,
        List<UUID> memberVillageIds,
        UUID centerVillageId,
        boolean isNamed,
        boolean edgesBuilt) {

    /** Compact constructor: defensive copy of mutable list, default flags. */
    public ClusterRecord {
        memberVillageIds = Collections.unmodifiableList(new ArrayList<>(memberVillageIds));
    }
}
