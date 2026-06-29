package com.livingvillages.core.geom;

public final class WorldBounds {
    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    public WorldBounds(int minX, int minZ, int maxX, int maxZ) {
        if (maxX <= minX || maxZ <= minZ) {
            throw new IllegalArgumentException("World bounds must have positive area");
        }
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public int minX() {
        return minX;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxZ() {
        return maxZ;
    }

    public int width() {
        return maxX - minX;
    }

    public int depth() {
        return maxZ - minZ;
    }

    public BlockPos2 clamp(BlockPos2 pos) {
        int x = Math.max(minX, Math.min(maxX - 1, pos.x()));
        int z = Math.max(minZ, Math.min(maxZ - 1, pos.z()));
        return new BlockPos2(x, z);
    }
}

