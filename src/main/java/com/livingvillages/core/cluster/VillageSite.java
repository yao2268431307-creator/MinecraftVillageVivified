package com.livingvillages.core.cluster;

import com.livingvillages.core.geom.BlockPos2;

public final class VillageSite {
    private final int id;
    private final BlockPos2 position;
    private final boolean center;
    private final String name;

    public VillageSite(int id, BlockPos2 position, boolean center, String name) {
        this.id = id;
        this.position = position;
        this.center = center;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public BlockPos2 position() {
        return position;
    }

    public boolean center() {
        return center;
    }

    public String name() {
        return name;
    }
}

