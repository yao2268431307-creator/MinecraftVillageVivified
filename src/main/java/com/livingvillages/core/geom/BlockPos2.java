package com.livingvillages.core.geom;

import java.util.Objects;

public final class BlockPos2 {
    private final int x;
    private final int z;

    public BlockPos2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    public double distanceTo(BlockPos2 other) {
        long dx = (long) x - other.x;
        long dz = (long) z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public long distanceSquaredTo(BlockPos2 other) {
        long dx = (long) x - other.x;
        long dz = (long) z - other.z;
        return dx * dx + dz * dz;
    }

    public BlockPos2 offset(int dx, int dz) {
        return new BlockPos2(x + dx, z + dz);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockPos2)) {
            return false;
        }
        BlockPos2 blockPos2 = (BlockPos2) o;
        return x == blockPos2.x && z == blockPos2.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + z + ")";
    }
}

