package com.livingvillages.core.worldgen;

import java.util.Objects;

public final class ChunkPos2 {
    private final int x;
    private final int z;

    public ChunkPos2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static ChunkPos2 fromBlock(int blockX, int blockZ) {
        return new ChunkPos2(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChunkPos2)) {
            return false;
        }
        ChunkPos2 chunkPos2 = (ChunkPos2) o;
        return x == chunkPos2.x && z == chunkPos2.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return "[" + x + ", " + z + "]";
    }
}

