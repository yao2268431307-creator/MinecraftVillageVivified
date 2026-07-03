# LivingVillages Cluster

A Fabric mod for Minecraft 1.20.1 that turns the world's uniformly-scattered villages into **a network of clustered settlements with names, roads, and villagers** — so exploring feels like encountering living communities rather than isolated resource points.

## The gameplay this mod expresses

Vanilla Minecraft places villages roughly every ~500 blocks, uniformly scattered. A player walks for ages to find one lonely village, then ages more for the next. This mod reshapes that experience along several axes:

### 1. Villages physically cluster

Instead of uniform spread, villages gather at **super-cell centers** — one center every ~3800 blocks. Around each center, multiple villages sit close together. A player entering a region sees 5–7 villages bunched up, not a single isolated one. Village count is preserved (even supplemented); only positions shift toward the cluster.

### 2. Settlements have names

Each cluster gets a **deterministic Chinese name** derived from its biome. Plains clusters are "晴原镇"-family (Qíngyuán Town), with member villages named "麦浪村", "向阳集", "和风庄" — all sharing the prefix so a player recognizes they belong to one settlement. Desert clusters get "金沙镇"-family, taiga gets "雪岭镇", etc. The same world seed always produces the same names.

### 3. Roads connect settlements

Settlements are linked by **automatically generated roads** — not vanilla's roadless isolated villages. A stone-brick (or biome-appropriate) road runs between cluster centers, following terrain, bridging water, tunneling through mountains. Players can travel along roads between settlements. (Road generation is handled by [RoadWeaver](https://github.com/shiroha-233/RoadWeaver); this mod places villages and tells RoadWeaver which to connect.)

### 4. Villages have villagers

Every placed village has **living villagers** — not empty houses. Villagers spawn with beds and workstations, can breed, and can claim professions. The settlement is alive, not a museum diorama.

### 5. Region title display

When a player walks into a cluster's range, a title floats above the screen: `structures 晴原——麦浪村` — telling them they've entered the Qíngyuán cluster's Màilàng Village. RPG-style region prompts.

### 6. Economy & caravans (future, currently shelved)

The original vision includes dynamic trade between settlements — minecart caravans carrying goods along rails, prices fluctuating with supply and demand. Not implemented in this version.

## How it works (current implementation)

This mod **does not modify world generation**. Instead, after worldgen completes, it hand-places village pieces at super-cell centers:

1. On each server tick (throttled to 1/sec), checks whether the player is within 200 blocks of an ungenerated super-cell center.
2. At the center, samples terrain height and places a vanilla plains house NBT template (`minecraft:village/plains/houses/plains_small_house_1`).
3. Spawns 2 villagers near the house with `MobSpawnType.STRUCTURE` + `setPersistenceRequired` (so they persist).
4. Calls `RoadNetworkApi.ensureConnection` so RoadWeaver builds a road to the previous cluster.

This approach sidesteps all the hard constraints of vanilla worldgen (the cell grid, the `StructurePlacement` API, Mixin crashes) by treating settlements as a post-hoc overlay rather than a worldgen modification.

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

Drop the output JAR from `build/libs/` into `.minecraft/mods/` alongside Fabric API and RoadWeaver.

## Status

Minimal viable version:
- ✅ Single plains house piece per cluster
- ✅ 2 villagers per cluster
- ✅ Linear connection between consecutive clusters (via RoadWeaver)

Future work:
- Multiple house pieces per cluster (jigsaw-style assembly of 5–7 houses + streets + center)
- Biome-aware house/template selection (desert/savanna/taiga/snowy variants)
- Chinese cluster/village names (porting the NameGenerator from the 1.21.4 attempt)
- Region title display on enter
- Cluster-internal road network (not just inter-cluster)
- Economy & caravans (long-term)

## License

MIT
