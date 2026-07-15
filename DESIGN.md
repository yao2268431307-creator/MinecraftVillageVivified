# LivingVillages Biome Regions — Design Document

> **⚠️ SUPERSEDED.** This document describes the *original* super-cell-region design
> (regions, M0–M9 modules), which has been replaced by the settlement-tier system
> (village/town/city tiers, per-settlement naming, city spawns, teleport commands).
> See [`README.md`](./README.md) for the current, authoritative design. This file is
> kept as a historical record of the project's evolution.

## 概述

将 Minecraft 1.20.1 的村庄系统从"均匀散布的资源点"重塑为"以 biome 区位为单位的奇幻中世纪风文明网络"。玩家走到不同 biome 区域，看到不同风格的区位命名（如"风沙之地""霜白之境"），区位内的村庄自动用 RoadWeaver 连接成路网，H 键地图上标注区位信息。

## 核心玩法

### 1. 村庄密度增加（7 倍）

通过自定义 StructureSet `livingvillages:extra_village`（spacing=13，原版 spacing=34），让世界额外多生成约 7 倍村庄。新村庄使用原版 village 模板（用户装 Better Villages 或 CTOV 时会自动升级）。

### 2. Biome 区位划分

世界按 super-cell（3808×3808 格）划分为区位单元。每个 super-cell 统计主流 biome 类型，归类到 10 种区位类型之一：
- `desert`（沙漠）
- `snowy`（雪原）
- `plains`（平原）
- `taiga`（针叶林）
- `savanna`（草原）
- `swamp`（沼泽）
- `jungle`（丛林）
- `mountain`（山地）
- `forest`（森林）
- `other`（其他）

**排除**：cave biomes（`lush_caves`/`dripstone_caves`/`deep_dark`）、water biomes（`is_ocean`/`is_river`）、beach biomes（`is_beach`）——这些区域无村庄，不命名。

### 3. 奇幻中世纪风命名

每个区位有**确定性名字**（同 seed 同坐标永远同名），由 biome 类型对应的命名池生成：
- 区位名格式：`前缀 + 之 + 后缀`，如 `风沙之地`、`霜白之境`、`晴原之领`
- 村庄名格式：`前缀 + 村/镇/集/庄`，如 `赤焰村`、`麦浪镇`

### 4. 飘字系统

玩家走进村庄 100 格内时，屏幕下方（action bar）淡入淡出显示：
```
风沙之地——赤焰村
```
区位名 + 村庄名，提示玩家所在区位与村庄。

### 5. RoadWeaver 路网集成

新村庄生成后，mod 自动：
- 注册村庄到 RoadWeaver 的结构端点
- 连接同区位内相邻村庄
- 连接跨区位的邻近村庄（形成跨区位干道）

RoadWeaver 负责实际铺路（含桥隧、地形融合、装饰）。

### 6. H 键地图 UI 标注（方案 A：Mixin RoadMapScreen）

Mixin RoadWeaver 的 `RoadMapScreen.render` 方法，在地图渲染后叠加区位标注：
- 地图上每个 super-cell 区域显示对应区位名（半透明文字）
- 玩家所在区位高亮（边框 + 区位名放大显示在屏幕顶部）
- 鼠标悬停某区域时 tooltip 显示完整区位名

---

## 技术实现

### 项目结构

```
lv-cluster/  (改名为 lv-regions 或保留)
├── build.gradle
├── settings.gradle
├── gradle.properties
├── libs/
│   └── roadweaver-fabric-2.2.2-1.20.1.jar  (gitignore)
├── src/main/java/com/livingvillages/regions/
│   ├── RegionsMod.java                      (ModInitializer)
│   ├── RegionsClient.java                   (ClientModInitializer)
│   ├── biome/
│   │   ├── BiomeRegionResolver.java         (biome → 区位类型)
│   │   └── RegionType.java                  (枚举)
│   ├── naming/
│   │   ├── RegionNamePool.java              (10 种区位命名池)
│   │   ├── RegionNameGenerator.java         (确定性命名)
│   │   └── VillageNameGenerator.java        (村庄命名)
│   ├── network/
│   │   └── RoadWeaverIntegrator.java        (server 端注册村庄+连接)
│   ├── client/
│   │   ├── RegionHudRenderer.java           (HudRenderCallback 屏幕角落)
│   │   ├── RegionTitleDisplay.java          (ClientTick 飘字)
│   │   └── mixin/
│   │       └── RoadMapScreenMixin.java      (Mixin RoadWeaver H 键地图)
│   └── data/
│       └── RegionStateStore.java           (SavedData 存已处理村庄)
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── livingvillages.mixins.json          (含 RoadMapScreenMixin)
│   └── data/
│       └── livingvillages/worldgen/structure_set/
│           └── extra_village.json          (spacing=13 的额外村庄)
```

### 1. 自定义 StructureSet

**文件**：`src/main/resources/data/livingvillages/worldgen/structure_set/extra_village.json`

```json
{
  "placement": {
    "type": "minecraft:random_spread",
    "salt": 5803128,
    "separation": 4,
    "spacing": 13
  },
  "structures": [
    { "structure": "minecraft:village_plains", "weight": 1 },
    { "structure": "minecraft:village_desert", "weight": 1 },
    { "structure": "minecraft:village_savanna", "weight": 1 },
    { "structure": "minecraft:village_snowy", "weight": 1 },
    { "structure": "minecraft:village_taiga", "weight": 1 }
  ]
}
```

- `spacing=13`：村庄数约为原版 (34/13)² ≈ 6.8 倍
- `salt=5803128`：不同于原版 `10387312`，避免与原版村庄位置重合
- 5 种 village structure 自动按 biome 生成对应类型（沙漠 biome → village_desert）

### 2. Biome 区位解析

**类**：`BiomeRegionResolver`

```java
public enum RegionType {
    DESERT, SNOWY, PLAINS, TAIGA, SAVANNA, SWAMP, JUNGLE, MOUNTAIN, FOREST, OTHER
}

public class BiomeRegionResolver {
    private final ServerLevel level;
    private final Registry<Biome> biomeRegistry;
    
    public RegionType resolveRegionType(int blockX, int blockZ) {
        Holder<Biome> biome = level.getBiome(new BlockPos(blockX, 64, blockZ));
        
        // 1. 排除 cave biomes（vanilla 1.20.1 无 is_cave 标签，手动列表）
        if (isCaveBiome(biome)) return null;
        
        // 2. 排除 water biomes
        if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)) return null;
        
        // 3. 排除 beach biomes
        if (biome.is(BiomeTags.IS_BEACH)) return null;
        
        // 4. 按特征归类
        if (biome.is(BiomeTags.IS_MOUNTAIN)) return RegionType.MOUNTAIN;
        float temp = biome.value().getBaseTemperature();
        if (temp < 0.15) return RegionType.SNOWY;
        if (temp > 1.5) return RegionType.DESERT;
        if (biome.is(BiomeTags.HAS_SWAMP_HUT)) return RegionType.SWAMP;
        if (biome.is(BiomeTags.IS_JUNGLE)) return RegionType.JUNGLE;
        if (biome.is(BiomeTags.IS_TAIGA)) return RegionType.TAIGA;
        if (temp > 1.0) return RegionType.SAVANNA;
        if (biome.is(/* is_forest */)) return RegionType.FOREST;  // 用 biome key 判断
        return RegionType.PLAINS;
    }
    
    private boolean isCaveBiome(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrap().orElse(null);
        if (key == null) return false;
        return key.equals(Biomes.LUSH_CAVES)
            || key.equals(Biomes.DRIPSTONE_CAVES)
            || key.equals(Biomes.DEEP_DARK);
    }
}
```

**Super-cell 主流 biome 统计**：在每个 super-cell 中心采样 5×5 网格的 biome，取众数确定区位类型。

### 3. 命名系统

**类**：`RegionNamePool` + `RegionNameGenerator`

```java
public class RegionNamePool {
    public record RegionWords(List<String> prefixes, String midfix, List<String> suffixes) {}
    
    public static final Map<RegionType, RegionWords> POOLS = Map.of(
        RegionType.DESERT, new RegionWords(
            List.of("风沙", "烈日", "炽阳", "荒漠", "金沙", "烈焰", "旱地", "驼铃", "戈壁", "灼热"),
            "之",
            List.of("地", "领", "境", "域")
        ),
        RegionType.SNOWY, new RegionWords(
            List.of("霜白", "凛冬", "雪原", "冰封", "银白", "寒月", "永冻", "皓雪", "玉霜", "玄冬"),
            "之",
            List.of("地", "领", "境", "域")
        ),
        RegionType.PLAINS, new RegionWords(
            List.of("晴原", "麦浪", "暖阳", "丰饶", "金穗", "和风", "翠野", "广袤", "明光", "暖野"),
            "之",
            List.of("地", "领", "境", "原")
        ),
        RegionType.TAIGA, new RegionWords(
            List.of("松涛", "雪岭", "寒林", "霜枝", "苍针", "北境", "冷杉", "玄松"),
            "之",
            List.of("林", "领", "境", "域")
        ),
        RegionType.SAVANNA, new RegionWords(
            List.of("赤原", "日炎", "旱风", "金草", "烈阳", "稀树", "暖原", "暮野"),
            "之",
            List.of("地", "领", "原", "域")
        ),
        RegionType.SWAMP, new RegionWords(
            List.of("雾沼", "幽影", "深泽", "暗藤", "碧水", "沉渊", "迷雾", "苔泽"),
            "之",
            List.of("地", "泽", "境", "域")
        ),
        RegionType.JUNGLE, new RegionWords(
            List.of("翠林", "密莽", "雨荫", "繁茂", "苍翠", "深丛", "碧藤", "郁林"),
            "之",
            List.of("林", "野", "境", "域")
        ),
        RegionType.MOUNTAIN, new RegionWords(
            List.of("云峰", "霜岭", "峻岳", "苍崖", "孤峰", "雪顶", "玄岩", "高岭"),
            "之",
            List.of("山", "岭", "峰", "岳")
        ),
        RegionType.FOREST, new RegionWords(
            List.of("幽林", "深翠", "暗木", "古树", "苍林", "碧荫", "密枝", "老林"),
            "之",
            List.of("林", "野", "境", "域")
        ),
        RegionType.OTHER, new RegionWords(
            List.of("异境", "未知", "迷地", "荒原"),
            "之",
            List.of("地", "域", "境")
        )
    );
    
    // 村庄后缀（所有区位通用）
    public static final List<String> VILLAGE_SUFFIXES = List.of("村", "镇", "集", "庄", "堡");
    // 村庄前缀（从对应区位池取，2 字）
}
```

**确定性命名**：
```java
public class RegionNameGenerator {
    public static String regionName(long worldSeed, int superX, int superZ, RegionType type) {
        long seed = worldSeed ^ ((long) superX * 341873128712L) ^ ((long) superZ * 132897987541L);
        Random rng = new Random(seed);
        RegionWords pool = RegionNamePool.POOLS.get(type);
        return pick(rng, pool.prefixes()) + pool.midfix() + pick(rng, pool.suffixes());
        // 如 "风沙" + "之" + "地" = "风沙之地"
    }
    
    public static String villageName(long worldSeed, int chunkX, int chunkZ, RegionType type) {
        long seed = worldSeed ^ ((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L);
        Random rng = new Random(seed);
        RegionWords pool = RegionNamePool.POOLS.get(type);
        String prefix = pick(rng, pool.prefixes());
        String suffix = pick(rng, RegionNamePool.VILLAGE_SUFFIXES);
        return prefix + suffix;  // 如 "赤焰村"
    }
    
    public static String fullDisplay(String regionName, String villageName) {
        return regionName + "——" + villageName;  // 如 "风沙之地——赤焰村"
    }
}
```

### 4. 客户端飘字

**类**：`RegionTitleDisplay`（ClientModInitializer 注册）

```java
public class RegionTitleDisplay implements ClientTickEvents.EndClientTick {
    private static String lastRegion = "";
    private static String lastVillage = "";
    
    @Override
    public void onEndClientTick(Minecraft mc) {
        // 节流：每 10 tick (0.5 秒) 检查一次
        if (mc.player == null || mc.level == null) return;
        if (mc.player.tickCount % 10 != 0) return;
        
        long seed = mc.level.getSeed();
        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, SUPER_CELL_SIZE);
        
        // 算当前区位类型 + 区位名（确定性，不需要 server 同步）
        RegionType type = BiomeRegionResolver.resolveClient(mc.level, blockX, blockZ);
        if (type == null) {
            lastRegion = "";
            return;  // 在 cave/water/beach，不飘字
        }
        String regionName = RegionNameGenerator.regionName(seed, superX, superZ, type);
        
        // 查附近最近村庄（用 StructureManager）
        String villageName = findNearestVillageName(mc, type, seed);
        if (villageName == null) {
            // 在区位内但不在村庄附近，只飘区位名
            if (!regionName.equals(lastRegion)) {
                mc.player.displayClientMessage(Component.literal(regionName), true);
                lastRegion = regionName;
                lastVillage = "";
            }
            return;
        }
        
        // 在村庄附近，飘完整名
        String full = RegionNameGenerator.fullDisplay(regionName, villageName);
        if (!full.equals(lastRegion + "——" + lastVillage)) {
            mc.player.displayClientMessage(Component.literal(full), true);
            lastRegion = regionName;
            lastVillage = villageName;
        }
    }
    
    private static String findNearestVillageName(Minecraft mc, RegionType type, long seed) {
        // 用 StructureManager 查半径 100 格内的 StructureStart
        // 对每个找到的村庄，用其 chunkX/chunkZ 算村名
        // 返回最近的
        // ⚠️ 待验证：客户端能否查 StructureStart
        return null;  // 先实现为 null，阶段 2 验证后补
    }
}
```

### 5. HUD 区位显示（屏幕角落小字）

**类**：`RegionHudRenderer`（HudRenderCallback）

```java
public class RegionHudRenderer implements HudRenderCallback {
    @Override
    public void onHudRender(GuiGraphics g, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        long seed = mc.level.getSeed();
        int blockX = mc.player.getBlockX();
        int blockZ = mc.player.getBlockZ();
        int superX = Math.floorDiv(blockX, SUPER_CELL_SIZE);
        int superZ = Math.floorDiv(blockZ, SUPER_CELL_SIZE);
        
        RegionType type = BiomeRegionResolver.resolveClient(mc.level, blockX, blockZ);
        if (type == null) return;
        
        String regionName = RegionNameGenerator.regionName(seed, superX, superZ, type);
        
        // 屏幕右上角小字显示
        Font font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() - font.width(regionName) - 8;
        int y = 8;
        g.drawString(font, regionName, x, y, 0xFFFFFF, true);
    }
}
```

### 6. H 键地图 UI Mixin（方案 A）

**类**：`RoadMapScreenMixin`

```java
@Mixin(RoadMapScreen.class)
public abstract class RoadMapScreenMixin {
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void livingvillages$renderRegionOverlay(
            GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        long seed = mc.level.getSeed();
        
        // 1. 在地图上每个可见 super-cell 区域叠加区位名（半透明）
        //    需要从 RoadMapScreen 获取当前地图 viewport（map rect）
        //    将 super-cell 坐标映射到屏幕坐标
        //    在每个 super-cell 中心绘制区位名
        
        // 2. 玩家所在 super-cell 高亮（边框 + 放大区位名显示在屏幕顶部）
        int playerSuperX = Math.floorDiv(mc.player.getBlockX(), SUPER_CELL_SIZE);
        int playerSuperZ = Math.floorDiv(mc.player.getBlockZ(), SUPER_CELL_SIZE);
        RegionType playerType = BiomeRegionResolver.resolveClient(mc.level, 
            mc.player.getBlockX(), mc.player.getBlockZ());
        if (playerType != null) {
            String playerRegion = RegionNameGenerator.regionName(seed, playerSuperX, playerSuperZ, playerType);
            // 屏幕顶部居中显示当前区位名（大字）
            Font font = mc.font;
            int x = (mc.getWindow().getGuiScaledWidth() - font.width(playerRegion)) / 2;
            g.drawString(font, playerRegion, x, 16, 0xFFFFAA, true);
        }
        
        // 3. 鼠标悬停某 super-cell 时 tooltip 显示区位名
        //    （需要将鼠标坐标转回 super-cell 坐标，查区位名）
    }
}
```

**mixins.json**：
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.livingvillages.regions.client.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": [],
  "client": [
    "RoadMapScreenMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**fabric.mod.json** 需要加 mixins 引用 + depends RoadWeaver。

### 7. Server 端 RoadWeaver 集成

**类**：`RoadWeaverIntegrator`

```java
public class RoadWeaverIntegrator {
    private final ServerLevel level;
    private final RegionStateStore store;  // 存已处理村庄
    
    public void onTick() {
        // 节流：每秒一次
        // 扫描玩家附近 500 格内的新村庄（StructureManager 查 StructureStart）
        // 对每个未处理村庄:
        //   1. 注册到 RoadWeaver: RoadNetworkApi.registerStructureEndpoint(level, villagePos, "village", true)
        //   2. 找最近的 1-2 个已处理村庄，调 ensureConnection
        //   3. 标记为已处理，存到 store
    }
}
```

### 8. 数据持久化

**类**：`RegionStateStore extends SavedData`

```java
public class RegionStateStore extends SavedData {
    private final Set<Long> processedVillages = new HashSet<>();  // chunkKey 集合
    
    public static RegionStateStore load(ServerLevel level) { ... }
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        // 保存 processedVillages
    }
    
    public boolean isProcessed(int chunkX, int chunkZ) { ... }
    public void markProcessed(int chunkX, int chunkZ) { ... }
}
```

### 9. Entrypoint

**Server** (`RegionsMod implements ModInitializer`)：
```java
onInitialize():
  - 注册 ServerWorldEvents.LOAD（加载 RegionStateStore）
  - 注册 ServerTickEvents.END_SERVER_TICK（节流 1 秒）:
      * 调 RoadWeaverIntegrator.onTick()
  - 注册 ServerLifecycleEvents.SERVER_STOPPING（保存数据）
```

**Client** (`RegionsClient implements ClientModInitializer`)：
```java
onInitializeClient():
  - 注册 ClientTickEvents.END_CLIENT_TICK → RegionTitleDisplay
  - 注册 HudRenderCallback → RegionHudRenderer
```

---

## 工作量分解

| 阶段 | 任务 | 工作量 | 风险 |
|---|---|---|---|
| 阶段 1 | 自定义 StructureSet JSON + 移植 NamePool + 写 10 种区位池 | 2h | 低 |
| 阶段 1 | 写 BiomeRegionResolver + RegionNameGenerator | 1.5h | 低 |
| 阶段 1 | 写客户端飘字（先只飘区位名） | 1h | 低 |
| 阶段 1 | 测试：走进不同 biome 飘对应区位名 | 0.5h | - |
| 阶段 2 | 验证客户端查 StructureStart | 1h | 中 |
| 阶段 2 | 加村庄名到飘字 | 1h | 中 |
| 阶段 2 | 测试：走进村庄飘完整名 | 0.5h | - |
| 阶段 3 | 写 RegionStateStore + RoadWeaverIntegrator | 2h | 中 |
| 阶段 3 | 测试：村庄间有 RoadWeaver 道路 | 0.5h | - |
| 阶段 4 | 写 RegionHudRenderer（屏幕角落 HUD） | 1h | 低 |
| 阶段 4 | 写 RoadMapScreenMixin（H 键地图标注） | 2-3h | 中（Mixin RoadWeaver 类） |
| 阶段 4 | 测试：H 地图显示区位名 + 玩家区位高亮 | 0.5h | - |

**总计约 13-15 小时**，分 3-4 次做。

---

## 关键技术风险

### 风险 1：客户端能否查 StructureStart？

客户端 `level.getStructureManager()` 能否查到附近村庄 `StructureStart`？如果不行，飘字只能显示区位名（不显示村庄名）。

**验证方法**：阶段 2 先写最小测试，客户端 tick 打印附近 `StructureStart` 列表。

**降级方案**：如果客户端查不到，飘字简化为只显示区位名（仍然有用，玩家知道自己在哪个区位）。

### 风险 2：RoadMapScreenMixin 的 viewport 获取

Mixin `RoadMapScreen.render` 需要知道当前地图 viewport（map rect）才能将 super-cell 坐标映射到屏幕坐标。需要用 `@Accessor` 或 `@Shadow` 访问 `RoadMapScreen` 的私有字段（如 `mapRect` / `viewport`）。

**验证方法**：阶段 4 先看 `RoadMapScreen` 源码，确认 viewport 字段名和访问方式。

**降级方案**：如果访问不到 viewport，Mixin 只在屏幕顶部显示当前区位名（不做地图上的多区位标注），仍然有用。

### 风险 3：自定义 StructureSet 与原版村庄位置重叠

两个 StructureSet 独立放置，位置可能重叠。重叠时两个村庄贴在一起。

**处理**：不解决。重叠反而像 mega village，是聚落感想要的。

### 风险 4：cave biomes 排除

vanilla 1.20.1 没有 `is_cave` 标签，手动列 `lush_caves` / `dripstone_caves` / `deep_dark`。

**处理**：手动列表可接受。未来 MC 加新 cave biome 需手动更新。

---

## 实施顺序

### 阶段 1（村庄密度 + 区位命名 + 飘字，4-5h）

1. 写自定义 StructureSet JSON
2. 写 BiomeRegionResolver（biome → 区位类型）
3. 写 RegionNamePool（10 种区位命名池）+ RegionNameGenerator
4. 写客户端 RegionTitleDisplay（先只飘区位名）
5. **测试**：走进沙漠飘 `structures 风沙之地`，雪原飘 `structures 霜白之境`

### 阶段 2（村庄飘字，2h）

1. 验证客户端查 StructureStart
2. 加村庄名到飘字
3. **测试**：走进村庄飘 `structures 风沙之地——赤焰村`

### 阶段 3（RoadWeaver 集成，2-3h）

1. 写 RegionStateStore
2. 写 RoadWeaverIntegrator（扫描新村庄 + 注册 + 连接）
3. **测试**：村庄间有 RoadWeaver 道路

### 阶段 4（HUD + H 地图标注，3-4h）

1. 写 RegionHudRenderer（屏幕角落小字）
2. 写 RoadMapScreenMixin（H 键地图叠加区位名 + 玩家区位高亮）
3. **测试**：屏幕角落永远显示区位名；按 H 打开地图看到区位标注

---

## 依赖

- Minecraft 1.20.1
- Fabric Loader 0.16+
- Fabric API 0.92.9+1.20.1
- RoadWeaver 2.2.2+1.20.1（必需，用于道路 + H 键地图）
- 用户可选装 Better Villages 或 CTOV 升级村庄模板

## License

MIT
