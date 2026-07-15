# LivingVillages Cluster

A Fabric mod for Minecraft 1.20.1 that turns RoadWeaver's road network into a **medieval-traveler world**: most villages stay vanilla-sized, but roughly one in every five or six is a mega **city**. Every settlement gets a deterministic fantasy-medieval Chinese name that appears when you step inside it, and cities become worth traveling to with extra villagers and a permanent wandering-trader trade hub.

[RoadWeaver](https://github.com/shiroha-233/RoadWeaver) connects every village with custom roads; this mod makes the *destinations* worth the journey.

## The gameplay this mod expresses

Vanilla villages are anonymous and barely worth a stop. This mod reshapes that into a journey rhythm: you pass several ordinary villages over mountains, rivers, and plains, then crest a hill to a huge **city** — a civilization hub with crowds of villagers and a permanent trader. Size is the lever: no custom-built content, just tiered scaling of the vanilla jigsaw village generator.

### 1. Settlement tiers (village / town / city)

Each village placement is assigned a tier deterministically from the world seed, drawn at **city : town : village = 1 : 2 : 5** (1/8, 2/8, 5/8). It's "purely luck": you may hit a city first, or pass 10+ villages before one. A soft min-gap guard demotes a city to a town if a neighbouring cell also drew city, so two cities never spawn side by side.

| Tier | Size (vs vanilla) | Name suffix | Villager target | Trader |
|---|---|---|---|---|
| Village | ~1× | `村` | vanilla (~10) | none |
| Town | ~3× | `镇` | ~20 | none |
| City | ~8× (mega) | `城` | ~40 | 1 permanent resident |

Sizing is **per-placement**, not per-config: a Mixin redirects the `JigsawPlacement.addPieces` call inside `JigsawStructure.findGenerationPoint` and multiplies `maxDepth` and `maxDistanceFromCenter` by the tier's factor for *that* village only — so different villages of the same biome type can be different sizes. Village JSONs are reset to vanilla values, so a village-tier settlement is exactly vanilla-sized.

### 2. Per-settlement names, shown on zone entry

Every settlement gets a deterministic name of the form `biome prefix + [terrain modifier] + tier suffix`:
- `风沙城` — desert city
- `霜白岭城` — snowy mountain city
- `暖阳滨镇` — plains waterside town
- `麦浪村` — plains village

- **Biome prefix** — from the settlement's local biome (10 types: desert/snowy/plains/taiga/savanna/swamp/jungle/mountain/forest/other), reusing the existing word pools.
- **Terrain modifier** (optional) — `岭/崖/峰` for elevated (mountain) settlements, `水/滨/渚` for waterside ones.
- **Tier suffix** — `村` / `镇` / `城`.

The name appears on the **action bar only while you're inside the settlement's footprint** (its generation radius — small for villages, large for cities), and fades when you leave. No region grid, no persistent HUD, no map overlay — the settlement is the only named, on-screen thing.

Names are **attributes of the settlement**, not your moving view: biome and terrain are sampled once at the settlement's centre and cached, so a name never flickers or changes as chunks stream in after a teleport.

### 3. City & town populations (Phase 3)

A server-side spawner tops up villagers in loaded, near-player towns and cities and spawns a **permanent wandering-trader resident** at each city centre — a trade hub you can always return to. Villages stay at vanilla population; vanilla roaming wandering traders are untouched.

The cost scales with *active* villagers (those within the simulation distance of an online player), not the world total — so a city only consumes CPU while someone is there. Fly away and its entities freeze. Bounded by target counts, a per-pass spawn cap, and a throttle.

### 4. RoadWeaver road network (unchanged)

The server-side `RoadWeaverIntegrator` (already part of this codebase) discovers villages as players explore, registers each with RoadWeaver, connects consecutive discoveries, and pushes their chunk coordinates to clients (for naming). RoadWeaver handles the actual road generation — terrain-aware A*, bridges, tunnels, decoration.

### 5. Teleport commands

Cities are rare by design, so two commands jump you to the nearest **discovered** settlement of a tier:
- `/nearestcity` — teleport to the nearest discovered city.
- `/nearesttown` — teleport to the nearest discovered town.

Both force-load a chunk ring at the destination so you land on solid ground, and report the settlement's name. They only know about settlements already discovered by `RoadWeaverIntegrator` — explore more to reveal more cities to jump to.

## Architecture

The mod is a set of focused modules under `com.livingvillages.regions`:

| Area | Class | Responsibility |
|---|---|---|
| Tier | `tier/SettlementTier` | deterministic 1:2:5 draw + min-gap guard; drives size + name suffix |
| Sizing | `client/mixin/JigsawTierMixin` | per-placement `@Redirect` on `JigsawPlacement.addPieces` |
| Naming | `naming/SettlementName`, `naming/RegionNamePool`, `naming/SeedHash` | `prefix + [terrain] + tier suffix`; shared hash |
| Terrain | `terrain/TerrainModifier`, `terrain/TerrainResolver` | pure modifier selector + MC-dependent high/water detection |
| Biome | `biome/RegionType`, `biome/BiomeRegionResolver` | 10-type classification (excludes cave/water/beach) |
| Display | `client/RegionTitleDisplay` | footprint-entry action-bar name; cached per settlement |
| Network | `network/SeedSender`/`SeedReceiver`, `network/SettlementsSender`/`SettlementsReceiver`, `network/RegionNetworking` | seed packet + settlements packet |
| Discovery | `network/RoadWeaverIntegrator` | server village scan + RoadWeaver registration |
| Persistence | `data/RegionStateStore` | `SavedData` of processed chunk keys |
| Spawns | `spawn/CitySpawner` | top up town/city villagers + permanent city trader |
| Commands | `command/NearestSettlementCommand` | `/nearestcity`, `/nearesttown` |
| Entry | `RegionsMod`, `RegionsClientMod` | server + client initializers |

**71 unit tests** cover the pure-logic modules (`SettlementTier`, `SettlementName`, `TerrainModifier`, `BiomeRegionResolver`, `RegionNameGenerator`, `RegionNamePool`, `RegionStateStore`), including the no-two-adjacent-cities invariant and tier/naming determinism. GUI/network/Mixin/spawn modules are verified in-game by design.

## How it works (technical)

### Per-placement sizing (JigsawTierMixin)

`JigsawStructure.findGenerationPoint` reads `maxDepth` and `maxDistanceFromCenter` and passes them to the static `JigsawPlacement.addPieces(...)`. The Mixin `@Redirect`s that single call, computes the tier from `ctx.seed()` + `ctx.chunkPos()`, and multiplies the two size args by the tier's factors before forwarding. Because the values flow as call args (not the shared config fields), each placement scales independently. Registered server-side (worldgen).

### Determinism without tier sync

The tier is a pure function `SettlementTier.tierFor(worldSeed, chunkX, chunkZ)`. The server sizing Mixin uses it at worldgen; the client display uses the same function with the seed (delivered by the world-seed packet) and the settlement chunk coords (delivered by the settlements packet). So server size and client name agree with **no server→client tier/name sync** — the server only ships chunk coordinates.

### The settlements packet (why not a client scan)

1.20.1 clients do not reliably expose the structure dynamic registry or complete `StructureStart` maps for loaded chunks, so a client-side village scan cannot match village ids. Instead the server's `RoadWeaverIntegrator` (which discovers villages authoritatively) pushes settlement chunk coordinates to the client: the full known set on join, plus each new discovery incrementally. The client resolves tier + name from the seed. This also makes multiplayer reliable.

### Stable, settlement-defined names

Biome and terrain are sampled at the settlement's centre (not the player's feet) and cached per settlement, so the prefix belongs to the village and won't drift as you walk around. Terrain is resolved only once the centre + water-scan ring are loaded (with a short timeout fallback), so after a teleport you get a ~1–2s blank then the final name — never a transient name that flips.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.16+
- Fabric API 0.92.9+1.20.1
- [RoadWeaver 2.2.2+1.20.1](https://modrinth.com/mod/roadweaver) (road generation)

**Optional** (improves village visuals, no configuration needed):
- [Better Villages](https://modrinth.com/mod/better-village) — larger, more detailed village templates
- [ChoiceTheorem's Overhauled Village](https://modrinth.com/mod/ct-overhaul-village) — 23 village variants

## Build

```bash
# Java 17 required
./gradlew build
```

Drop `build/libs/LivingVillages-Cluster-*.jar` into `.minecraft/mods/` alongside Fabric API and RoadWeaver.

## Tunables

The following are constants in `CitySpawner` / `SettlementTier` and easy to adjust:
- Villager targets: town 20, city 40
- Tier ratio: city:town:village = 1:2:5; min-gap = 1 grid cell
- City trader: permanent (huge despawn delay + persistent)
- Spawn throttle: 20s; spawns-per-pass: 5; near-player radius: 128 blocks

## Future directions

- **Algorithmic city walls** — generate walls around mega cities procedurally (like RoadWeaver's roads), for immersion.
- **`/nearestcity any`** — search all chunk-grid cells (not just discovered) to find a city in unexplored terrain.
- **Config file** — expose the tunables above as config values.
- **Economy & caravans** — minecart caravans transporting goods between settlements, prices fluctuating with supply/demand.
- **Region reputation** — player actions in a settlement affect villager disposition.

## License

MIT
