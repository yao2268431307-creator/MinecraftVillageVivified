# LivingVillages Regions

A Fabric mod for Minecraft 1.20.1 that **reshapes the world's villages into a biome-region civilization network** — villages keep their vanilla mechanics, but the world gets ~7× more of them, each biome region gets a deterministic fantasy-medieval Chinese name, and [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) connects them with roads.

## The gameplay this mod expresses

Vanilla Minecraft scatters villages every ~500 blocks, uniformly, with no cultural identity. A player walks for ages to find one anonymous village, then ages more for the next. This mod reshapes that experience:

### 1. Village density ×7 (biome-native)

A custom `StructureSet` (`livingvillages:extra_village`, spacing=13 vs vanilla's 34) makes the world generate roughly **7× more villages**, all using vanilla's biome-aware village types (plains/desert/savanna/snowy/taiga). No Mixin, no worldgen hack — pure datapack JSON. Players who install [Better Villages](https://modrinth.com/mod/better-village) or [ChoiceTheorem's Overhauled Village](https://modrinth.com/mod/ct-overhaul-village) get upgraded templates for free.

### 2. Biome regions with fantasy-medieval names

The world is divided into **super-cells** (3808×3808 blocks each). Each super-cell's dominant biome determines its region type (10 categories: desert / snowy / plains / taiga / savanna / swamp / jungle / mountain / forest / other). Each region gets a **deterministic Chinese name** like `风沙之地` (Land of Windblown Sand), `霜白之境` (Realm of Frostwhite), `晴原之领` (Domain of Sunlit Plains). The same world seed always produces the same names — no server-client sync needed for the names themselves (only the seed).

**Excluded biomes**: cave biomes (`lush_caves`, `dripstone_caves`, `deep_dark`), water biomes (`is_ocean`, `is_river`), and beach biomes (`is_beach`) — these have no villages, so they get no region name.

### 3. Action-bar float-text on region entry

When a player enters a region, a name floats on the action bar: `风沙之地` (or `风沙之地——赤焰村` once village-name overlay lands). RPG-style region prompts. Throttled to every 10 ticks (0.5s) so it doesn't spam.

### 4. Top-right HUD with current region

A small text in the screen's top-right corner always shows the current region name — persistent, not just on entry. Suppressed in cave/water/beach.

### 5. H-key map region overlay

Mixin into RoadWeaver's `RoadMapScreen.render` overlays the current region name at the top-center of the screen when the map is open. Player's current region is highlighted.

### 6. RoadWeaver road network (server-side)

A server tick listener (throttled to 1/sec) scans the player's loaded chunks for village `StructureStart`s. Each newly discovered village is:
- Registered with RoadWeaver via `RoadNetworkApi.registerStructureEndpoint(level, pos, "village", true)` (autoConnect enabled)
- Connected to the previous village via `RoadNetworkApi.ensureConnection`
- Persisted in a `SavedData` so it isn't re-registered after world reload

RoadWeaver handles the actual road generation — terrain-aware A*, bridges, tunnels, decoration.

## Architecture: 10 modules (M0–M9)

This mod is built as **10 independent modules**, each with a single responsibility. See [`TASKS.md`](./TASKS.md) for the full module breakdown with status, dependencies, and acceptance criteria. See [`DESIGN.md`](./DESIGN.md) for the complete technical design.

| Module | Name | Responsibility | Unit tests |
|---|---|---|---|
| M0 | RegionSeedSync | server→client world seed packet (vanilla `ClientLevel` has no `getSeed()`) | — |
| M1 | StructureSet | `extra_village.json` datapack (spacing=13) | — |
| M2 | BiomeRegionResolver | biome → RegionType (10 categories, excludes cave/water/beach) | 13 |
| M3 | RegionNamePool | 10 region word pools (fantasy-medieval Chinese) + village suffixes | 10 |
| M4 | RegionNameGenerator | deterministic `regionName` / `villageName` / `fullDisplay` | 12 |
| M5 | RegionStateStore | `SavedData` persisting processed-village chunk keys | 13 |
| M6 | RegionTitleDisplay | client action-bar float-text on region entry | — |
| M7 | RegionHudRenderer | top-right HUD with current region name | — |
| M8 | RoadWeaverIntegrator | server tick: discover villages + register/connect with RoadWeaver | — |
| M9 | RoadMapScreenMixin | Mixin RoadWeaver's H-key map to overlay region name | — |

**48 unit tests** cover the pure-logic modules (M2/M3/M4/M5). GUI/network/Mixin modules have no tests by design (hard to test without MC runtime).

## How it works (technical)

### The seed-sync workaround (M0)

Vanilla 1.20.1 `ClientLevel` deliberately does **not** expose the world seed (anti-seed-extraction). But region names are derived deterministically from `worldSeed + superX + superZ`. So the server sends the seed to the client once on player join via a custom packet (`ServerPlayConnectionEvents.JOIN` → `ServerPlayNetworking.send`). The client receives it and injects into `RegionTitleDisplay.setWorldSeed()`, which unblocks M6/M7/M9.

### The biome region resolution (M2)

Each super-cell's region type is resolved by sampling the biome at the super-cell center and applying this priority:
1. Exclude cave biomes (manual id list — vanilla 1.20.1 has no `is_cave` tag)
2. Exclude water biomes (`is_ocean` / `is_river` tags)
3. Exclude beach biomes (`is_beach` tag)
4. Classify by: `is_mountain` → temperature < 0.15 → temperature > 1.5 → `has_swamp_hut` → `is_jungle` → `is_taiga` → temperature > 1.0 → forest id list → fallback plains

### The village discovery (M8)

`RoadWeaverIntegrator.onTick` runs every 20 ticks (1s):
1. Takes the first online player's chunk position
2. Scans a 500-block (31-chunk) radius around the player
3. For each unprocessed chunk: `level.getChunkSource().getChunkNow(x, z)` (no force-load, thread-safe)
4. `chunk.getAllStarts()` → `Map<Structure, StructureStart>`
5. Match the `Structure`'s registry key against the 5 vanilla village ids
6. If matched: register with RoadWeaver + connect to previous village + mark chunk as processed

This only inspects **already-loaded chunks** — players discover villages as they explore, not all at once.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.16+
- Fabric API 0.92.9+1.20.1
- [RoadWeaver 2.2.2+1.20.1](https://modrinth.com/mod/roadweaver) (for road generation + H-key map)

**Optional** (improves village visuals, no configuration needed):
- [Better Villages](https://modrinth.com/mod/better-village) — larger, more detailed village templates
- [ChoiceTheorem's Overhauled Village](https://modrinth.com/mod/ct-overhaul-village) — 23 village variants

## Build

```bash
# Java 17 required
./gradlew build
```

Drop `build/libs/LivingVillages-Cluster-*.jar` into `.minecraft/mods/` alongside Fabric API and RoadWeaver.

## Repository structure

```
.
├── DESIGN.md          # Complete technical design document
├── TASKS.md           # 10-module task breakdown with status/dependencies/acceptance
├── README.md          # This file
├── build.gradle       # fabric-loom 1.6 build config
├── settings.gradle
├── gradle.properties
├── libs/              # (gitignored) RoadWeaver jar for compile-time API
└── src/main/
    ├── java/com/livingvillages/regions/
    │   ├── RegionsMod.java           # ModInitializer (server)
    │   ├── RegionsClientMod.java     # ClientModInitializer (client)
    │   ├── biome/
    │   │   ├── RegionType.java       # 10-value enum
    │   │   └── BiomeRegionResolver.java
    │   ├── naming/
    │   │   ├── RegionNamePool.java   # 10 region word pools
    │   │   └── RegionNameGenerator.java
    │   ├── data/
    │   │   └── RegionStateStore.java # SavedData
    │   ├── network/
    │   │   ├── RegionNetworking.java # packet id constant
    │   │   ├── SeedSender.java        # server→client seed packet
    │   │   ├── SeedReceiver.java      # client seed receiver
    │   │   └── RoadWeaverIntegrator.java # village discovery + RoadWeaver API
    │   └── client/
    │       ├── RegionTitleDisplay.java # action-bar float-text
    │       ├── RegionHudRenderer.java  # top-right HUD
    │       └── mixin/
    │           └── RoadMapScreenMixin.java
    ├── resources/
    │   ├── fabric.mod.json
    │   ├── livingvillages.mixins.json
    │   └── data/livingvillages/worldgen/structure_set/extra_village.json
    └── test/java/com/livingvillages/regions/
        ├── biome/BiomeRegionResolverTest.java       # 13 tests
        ├── naming/RegionNamePoolTest.java            # 10 tests
        ├── naming/RegionNameGeneratorTest.java       # 12 tests
        └── data/RegionStateStoreTest.java            # 13 tests
```

## About TASKS.md

[`TASKS.md`](./TASKS.md) is the **module task tracker**. Each of the 10 modules (M0–M9) has:
- **Status**: `[ ]` pending / `[x]` done (with date + commit SHA)
- **Dependencies**: which modules must complete first
- **Sub-agent task description**: what the implementing sub-agent was told to do
- **Acceptance criteria**: what the main agent verified (compile + tests + DESIGN.md compliance + code quality)
- **Unit test requirements**: which modules must have tests

The workflow: main agent launches a sub-agent per module serially (M1 → M2 → ... → M9, with M0 inserted when the seed-sync issue was discovered). Sub-agent implements + runs tests. Main agent reviews (compile + test pass + design compliance + code quality) and marks the module `[x]` done in TASKS.md. All 10 modules are now complete.

## Future directions

### Immediate (in-game testing needed)
- Verify M0 seed packet arrives (check client log for `Received world seed`)
- Verify M6 float-text fires on biome change
- Verify M8 actually discovers villages (check server log for `Registered village at ...`)
- Verify M9 Mixin applies (open H-key map, see region name at top)

### Short-term improvements
- **Village-name overlay**: M6 currently only shows region name. Add village-name resolution (requires client-side `StructureManager` query for nearby village `StructureStart`)
- **M8 cross-tick connections**: currently only connects villages discovered in the same tick. Maintain a "recently registered" list and connect across ticks if RoadWeaver's `autoConnect` isn't sufficient
- **Config**: expose spacing/salt/scan-radius as config values (currently hardcoded)

### Medium-term
- **Multi-house clusters**: hand-place multiple village pieces around a center for mega-village effect (the original "5–7 villages bunched up" vision)
- **Region-aware village spawning**: bias the extra_village StructureSet toward generating in the dominant biome of each super-cell
- **Region borders on H-key map**: draw super-cell boundaries on RoadWeaver's map for visual region separation

### Long-term (shelved from original vision)
- **Economy & caravans**: minecart caravans transporting goods between regions, prices fluctuating with supply/demand
- **Region reputation**: player actions in a region affect villager disposition
- **Custom mega-village templates**: design original large village NBT templates (license-clean, not copied from Better Villages/CTOV)

## License

MIT
