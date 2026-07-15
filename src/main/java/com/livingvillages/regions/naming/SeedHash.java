package com.livingvillages.regions.naming;

/**
 * Shared seed+coordinate hash used by both settlement tier resolution
 * and settlement naming.
 *
 * <p>The constants mirror vanilla Minecraft's chunk-position scrambling
 * ({@code 341873128712L} / {@code 132897987541L}) so values stay stable
 * across reloads. Centralising the formula here guarantees the per-placement
 * sizing Mixin (server worldgen) and the client display agree on a tier for
 * the same village — the linchpin of the no-packet design.</p>
 *
 * <p>Pure utility, no MC runtime dependency.</p>
 */
public final class SeedHash {

    private SeedHash() {
        // utility class
    }

    /**
     * Hash the world seed with two spatial coordinates using vanilla's
     * scrambling constants.
     *
     * @param worldSeed the world seed
     * @param x         a spatial coordinate (chunk or grid cell X)
     * @param z         a spatial coordinate (chunk or grid cell Z)
     * @return the hashed seed
     */
    public static long hash(long worldSeed, int x, int z) {
        return worldSeed
            ^ ((long) x * 341873128712L)
            ^ ((long) z * 132897987541L);
    }
}
