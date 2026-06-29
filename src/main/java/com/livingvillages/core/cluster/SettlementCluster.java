package com.livingvillages.core.cluster;

import com.livingvillages.core.geom.BlockPos2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettlementCluster {
    private final int id;
    private final BlockPos2 center;
    private final String regionName;
    private final List<VillageSite> villages;

    public SettlementCluster(int id, BlockPos2 center, String regionName, List<VillageSite> villages) {
        this.id = id;
        this.center = center;
        this.regionName = regionName;
        this.villages = Collections.unmodifiableList(new ArrayList<VillageSite>(villages));
    }

    public int id() {
        return id;
    }

    public BlockPos2 center() {
        return center;
    }

    public String regionName() {
        return regionName;
    }

    public List<VillageSite> villages() {
        return villages;
    }

    public VillageSite centerVillage() {
        for (VillageSite village : villages) {
            if (village.center()) {
                return village;
            }
        }
        return villages.isEmpty() ? null : villages.get(0);
    }
}

