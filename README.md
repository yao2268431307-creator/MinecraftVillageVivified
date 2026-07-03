# LivingVillages Cluster

A Fabric mod for Minecraft 1.20.1 that **hand-places village pieces at super-cell centers** after world generation, spawns villagers, and connects the resulting clusters via [RoadWeaver](https://github.com/shiroha-233/RoadWeaver).

## Why

Vanilla villages are uniformly scattered every ~500 blocks. This mod creates **clusters** of villages — every ~3800 blocks (one "super-cell"), a plains house piece is placed at the center with 2 villagers, and RoadWeaver builds roads connecting consecutive clusters.

## How it works

1. On each server tick (throttled to 1/sec), checks if the player is within 200 blocks of an ungenerated super-cell center.
2. At the center, samples terrain height and places a vanilla plains house NBT template (`minecraft:village/plains/houses/plains_small_house_1`).
3. Spawns 2 villagers near the house with `MobSpawnType.STRUCTURE` + `setPersistenceRequired`.
4. Calls `RoadNetworkApi.ensureConnection` so RoadWeaver builds a road to the previous cluster.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.16+
- Fabric API 0.92.9+1.20.1
- RoadWeaver 2.2.2+ (for road generation)

## Build

```bash
# Java 17 required
./gradlew build
```

The output JAR is in `build/libs/`. Drop it into `.minecraft/mods/` alongside Fabric API and RoadWeaver.

## Status

Minimal viable version — single house piece per cluster, 2 villagers, linear connection between consecutive clusters. Future work:
- Multiple house pieces per cluster (jigsaw-style assembly)
- Biome-aware house selection
- Chinese names + region title display
- Cluster-internal road network

## License

MIT
