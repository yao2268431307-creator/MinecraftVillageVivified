package com.livingvillages.core.naming;

import com.livingvillages.core.Hashing;
import com.livingvillages.core.geom.BlockPos2;

import java.util.List;

public final class NameGenerator {
    private final NamePool pool;

    public NameGenerator(NamePool pool) {
        this.pool = pool;
    }

    public BiomeMood inferMood(BlockPos2 position) {
        long h = Hashing.mix(position.x(), position.z(), 0xB10BEL);
        BiomeMood[] moods = BiomeMood.values();
        return moods[(int) Math.floorMod(h, moods.length)];
    }

    public String regionName(long worldSeed, int clusterId, BlockPos2 center, BiomeMood mood) {
        List<String> prefixes = pool.prefixes(mood);
        long h = Hashing.mix(worldSeed, Hashing.mix(center.x(), center.z()), clusterId);
        return prefixes.get((int) Math.floorMod(h, prefixes.size()));
    }

    public String centerVillageName(String regionName, long worldSeed, int clusterId) {
        List<String> suffixes = pool.centerSuffixes();
        long h = Hashing.mix(worldSeed, clusterId, 0xC371L);
        return regionName + suffixes.get((int) Math.floorMod(h, suffixes.size()));
    }

    public String satelliteVillageName(String regionName, long worldSeed, int clusterId, int villageIndex, BiomeMood mood) {
        List<String> prefixes = pool.prefixes(mood);
        int offset = 1 + (int) Math.floorMod(Hashing.mix(worldSeed, clusterId, villageIndex), Math.max(1, prefixes.size() - 1));
        String prefix = prefixes.get(offset % prefixes.size());
        if (prefix.equals(regionName) && prefixes.size() > 1) {
            prefix = prefixes.get((offset + 1) % prefixes.size());
        }
        List<String> suffixes = pool.smallSuffixes();
        long h = Hashing.mix(worldSeed, villageIndex, 0xA11E7L);
        return prefix + suffixes.get((int) Math.floorMod(h, suffixes.size()));
    }
}
