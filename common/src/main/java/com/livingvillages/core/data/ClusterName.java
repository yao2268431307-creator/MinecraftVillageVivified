package com.livingvillages.core.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chinese names for a cluster and its member villages.
 *
 * <p>Writer: §3 NameGenerator.</p>
 *
 * @param clusterName      the cluster's area name, e.g. "晴原"
 * @param centerTownName   the center town name, e.g. "晴原镇"
 * @param satelliteNames   map of villageId → satellite village name, e.g. "麦浪村"
 */
public record ClusterName(
        String clusterName,
        String centerTownName,
        Map<UUID, String> satelliteNames) {

    /** Compact constructor: defensive copy of mutable map. */
    public ClusterName {
        satelliteNames = Collections.unmodifiableMap(new HashMap<>(satelliteNames));
    }
}
