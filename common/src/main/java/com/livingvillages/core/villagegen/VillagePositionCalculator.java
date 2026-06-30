package com.livingvillages.core.villagegen;

import com.livingvillages.core.data.Vec3i;

import java.util.*;

/**
 * Computes vanilla village positions using MC's random_spread algorithm.
 *
 * <p>Mirrors the deterministic position calculation that Minecraft uses
 * for structure sets with type=random_spread. Does NOT require MC types —
 * uses only the world seed and the known structure set parameters.</p>
 *
 * <p>Default parameters for villages: spacing=34 chunks, separation=8 chunks.</p>
 */
public final class VillagePositionCalculator {

    /** Vanilla village structure set defaults. */
    private static final int DEFAULT_SPACING = 34;   // chunks
    private static final int DEFAULT_SEPARATION = 8; // chunks
    private static final int DEFAULT_SALT = 10387312;
    private static final int CHUNK_SIZE = 16;

    private VillagePositionCalculator() {}

    /**
     * Compute vanilla village positions within a world radius.
     *
     * @param seed    world seed
     * @param radius  world half-extent in blocks
     * @return list of vanilla village positions (y=0 placeholder)
     */
    public static List<Vec3i> computeVanillaPositions(long seed, int radius) {
        return compute(seed, radius, DEFAULT_SPACING, DEFAULT_SEPARATION, DEFAULT_SALT);
    }

    /**
     * Full parameter version.
     *
     * @param seed       world seed
     * @param radius     world half-extent in blocks
     * @param spacing    structure set spacing (chunks)
     * @param separation structure set separation (chunks)
     * @param salt       structure set salt
     */
    public static List<Vec3i> compute(long seed, int radius, int spacing, int separation, int salt) {
        List<Vec3i> positions = new ArrayList<>();
        int chunkRadius = (radius / CHUNK_SIZE) + spacing;
        int range = spacing - separation;

        // Grid of potential positions
        for (int cx = -chunkRadius; cx <= chunkRadius; cx += spacing) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz += spacing) {
                // Deterministic offset (matches MC's random_spread)
                Random rng = new Random(seed + salt);
                long chunkSeed = (long) cx * 341873128712L + (long) cz * 132897987541L + seed + salt;
                rng.setSeed(chunkSeed);

                int offsetX = rng.nextInt(range);
                int offsetZ = rng.nextInt(range);

                int blockX = cx * CHUNK_SIZE + offsetX * CHUNK_SIZE;
                int blockZ = cz * CHUNK_SIZE + offsetZ * CHUNK_SIZE;

                // Within radius?
                if (Math.abs(blockX) <= radius && Math.abs(blockZ) <= radius) {
                    positions.add(new Vec3i(blockX, 0, blockZ));
                }
            }
        }
        return positions;
    }
}
