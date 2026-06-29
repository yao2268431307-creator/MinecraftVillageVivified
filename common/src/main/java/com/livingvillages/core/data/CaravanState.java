package com.livingvillages.core.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Complete state of one active caravan.
 *
 * <p>Writer: §7a CaravanSimulator.</p>
 *
 * @param caravanId        unique caravan identifier
 * @param fromClusterId    origin cluster
 * @param toClusterId      destination cluster
 * @param currentPathIndex current waypoint index on the path
 * @param fuelTicks        remaining fuel in ticks
 * @param cargo            itemId → quantity being transported
 * @param phase            current state machine phase
 */
public record CaravanState(
        UUID caravanId,
        String fromClusterId,
        String toClusterId,
        int currentPathIndex,
        int fuelTicks,
        Map<String, Integer> cargo,
        CaravanPhase phase) {

    /** Compact constructor: defensive copy of mutable map. */
    public CaravanState {
        cargo = Collections.unmodifiableMap(new HashMap<>(cargo));
    }
}
