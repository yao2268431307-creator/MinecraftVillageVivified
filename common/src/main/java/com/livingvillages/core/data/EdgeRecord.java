package com.livingvillages.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A planned path between two clusters.
 *
 * <p>Writer: §4 RegionalGraph.</p>
 *
 * @param fromClusterId  source cluster id
 * @param toClusterId    target cluster id
 * @param path           Bezier-smoothed waypoint sequence (includes Y coordinate)
 * @param type           always RAIL in v1.0
 */
public record EdgeRecord(
        String fromClusterId,
        String toClusterId,
        List<Vec3i> path,
        EdgeType type) {

    /** Compact constructor: defensive copy of mutable list. */
    public EdgeRecord {
        path = Collections.unmodifiableList(new ArrayList<>(path));
    }
}
