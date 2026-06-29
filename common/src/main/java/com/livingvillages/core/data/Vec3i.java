package com.livingvillages.core.data;

/**
 * Immutable 3D integer vector. Pure-Core replacement for net.minecraft.core.BlockPos.
 *
 * <p>Used by all Core modules for position representation without Minecraft dependency.
 * Record semantics guarantee immutability and value-based equality.</p>
 */
public record Vec3i(int x, int y, int z) {

    /**
     * Squared Euclidean distance between this and another vector.
     * Avoids sqrt for efficient comparison operations.
     *
     * @param other the other vector
     * @return squared distance (dx² + dy² + dz²)
     */
    public double distanceSq(Vec3i other) {
        double dx = (double) this.x - other.x;
        double dy = (double) this.y - other.y;
        double dz = (double) this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Horizontal (XZ-plane) distance between this and another vector.
     *
     * @param other the other vector
     * @return Euclidean distance in the XZ plane, sqrt(dx² + dz²)
     */
    public double horizontalDistance(Vec3i other) {
        double dx = (double) this.x - other.x;
        double dz = (double) this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
