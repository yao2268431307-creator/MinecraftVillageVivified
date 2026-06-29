package com.livingvillages.core;

import java.util.Random;

public final class Hashing {
    private Hashing() {
    }

    public static long mix(long value) {
        long z = value + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long mix(long a, long b) {
        return mix(a ^ Long.rotateLeft(mix(b), 17));
    }

    public static long mix(long a, long b, long c) {
        return mix(mix(a, b) ^ Long.rotateLeft(mix(c), 29));
    }

    public static Random random(long seed, long salt) {
        return new Random(mix(seed, salt));
    }
}

