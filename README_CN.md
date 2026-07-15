# LivingVillages Cluster（生机村庄·聚落）

一个 Minecraft 1.20.1 的 Fabric 模组，在 RoadWeaver 的道路网络之上构建一个**中世纪旅人世界**：大多数村庄保持原版大小，但大约每五到六个村庄中就有一座巨型**城市**。每个聚落都有一个确定性的中文武侠/奇幻名称，在你踏入其边界时显示；城市则因为更多村民和一名常驻流浪商人而值得专程前往。

[RoadWeaver](https://github.com/shiroha-233/RoadWeaver) 用自定义道路把每个村庄连起来；本模组让**目的地**本身值得这段旅程。

## 本模组呈现的玩法

原版村庄千篇一律、几乎不值得停留。本模组将其重塑为一种旅程节奏：你翻山越岭、穿过河流平原，路过好几座普通村庄，然后登上一座山头，眼前出现一座巨大的**城市**——一座有熙攘村民和常驻商人的文明枢纽。这里的杠杆是**尺寸**：不靠手工搭建任何内容，只对原版 jigsaw 村庄生成器做分层缩放。

### 1. 聚落分层（村 / 镇 / 城）

每个村庄放置点都根据世界种子被确定性地赋予一个层级，抽取比例为 **城 : 镇 : 村 = 1 : 2 : 5**（1/8、2/8、5/8）。这是"纯看运气"的：你可能第一座就遇到城市，也可能路过十多个村庄才遇到一座。一个软性的最小间隔保护会把与相邻格同为城市的聚落降级为镇，因此两座城市绝不会相邻。

| 层级 | 尺寸（相对原版） | 名称后缀 | 村民目标 | 商人 |
|---|---|---|---|---|
| 村 | ~1× | `村` | 原版（~10） | 无 |
| 镇 | ~3× | `镇` | ~20 | 无 |
| 城 | ~8×（巨型） | `城` | ~40 | 1 名常驻 |

缩放是**按放置点**进行的，而非按配置：一个 Mixin 重定向了 `JigsawStructure.findGenerationPoint` 内部的 `JigsawPlacement.addPieces` 调用，并针对*那一个*村庄分别把 `maxDepth` 和 `maxDistanceFromCenter` 乘以该层级的系数——于是同一生物群系类型的不同村庄可以是不同大小。村庄 JSON 被重置为原版数值，因此村级聚落与原版完全一致。

### 2. 按聚落命名，进入区域时显示

每个聚落都有一个确定性名称，格式为 `生物群前缀 + [地形修饰] + 层级后缀`：
- `风沙城` —— 沙漠城市
- `霜白岭城` —— 雪原山地城市
- `暖阳滨镇` —— 平原水畔集镇
- `麦浪村` —— 平原村庄

- **生物群前缀** —— 取自聚落所在生物群系（10 类：沙漠/雪原/平原/针叶林/热带草原/沼泽/丛林/山地/森林/其他），复用现有词池。
- **地形修饰**（可选）—— 高处用 `岭/崖/峰`（山地），水畔用 `水/滨/渚`。
- **层级后缀** —— `村` / `镇` / `城`。

该名称**只在你处于聚落边界（footprint，即其生成半径——村庄小、城市大）之内时**显示在动作栏上，离开后淡出。没有区域网格、没有常驻 HUD、没有地图覆盖——聚落是唯一被命名、会显示在屏幕上的东西。

名称是**聚落的属性**，而非随你移动而变的视图：生物群与地形在聚落中心采样一次并缓存，因此名称在传送后随着区块加载而**不会闪烁、不会改变**。

### 3. 城镇与城市人口（Phase 3）

一个服务端生成器会在已加载、靠近玩家的镇与城中补充村民，并在每座城市中心生成一名**常驻流浪商人**——一个你可以随时回去的交易枢纽。村庄保持原版人口；原版随机刷新的流浪商人不受影响。

开销随**活跃**村民（处于某个在线玩家的模拟距离之内）数量增长，而非随世界总量增长——所以一座城市只在你（或某人）身处其中时消耗 CPU。走开后其实体便冻结。受目标数量、单次生成上限和节流限制，开销有界。

### 4. RoadWeaver 道路网络（保持不变）

服务端的 `RoadWeaverIntegrator`（本代码库已有）随玩家探索发现村庄，将每个村庄注册到 RoadWeaver、连接相邻的新发现、并把其区块坐标推送给客户端（供命名使用）。RoadWeaver 负责实际的道路生成——地形感知的 A* 寻路、桥梁、隧道、装饰。

### 5. 传送指令

城市按设计很稀有，因此有两个指令把你送到最近的**已发现**对应层级聚落：
- `/nearestcity` —— 传送到最近的已发现城市。
- `/nearesttown` —— 传送到最近的已发现集镇。

两者都会在目的地预加载一圈区块以防你掉进未加载的区块，并报告聚落名称。它们只知道 `RoadWeaverIntegrator` 已经发现的聚落——多探索才能解锁更多可传送的城市。

## 架构

模组由 `com.livingvillages.regions` 下一组职责单一的模块组成：

| 领域 | 类 | 职责 |
|---|---|---|
| 分层 | `tier/SettlementTier` | 确定性 1:2:5 抽取 + 最小间隔保护；驱动尺寸与名称后缀 |
| 尺寸 | `client/mixin/JigsawTierMixin` | 对 `JigsawPlacement.addPieces` 的按放置点 `@Redirect` |
| 命名 | `naming/SettlementName`、`naming/RegionNamePool`、`naming/SeedHash` | `前缀 + [地形] + 层级后缀`；共享哈希 |
| 地形 | `terrain/TerrainModifier`、`terrain/TerrainResolver` | 纯选择器 + 依赖 MC 的高/水检测 |
| 生物群 | `biome/RegionType`、`biome/BiomeRegionResolver` | 10 类分类（排除洞穴/水域/沙滩） |
| 显示 | `client/RegionTitleDisplay` | 进入边界时的动作栏名称；按聚落缓存 |
| 网络 | `network/SeedSender`/`SeedReceiver`、`network/SettlementsSender`/`SettlementsReceiver`、`network/RegionNetworking` | 种子包 + 聚落包 |
| 发现 | `network/RoadWeaverIntegrator` | 服务端村庄扫描 + RoadWeaver 注册 |
| 持久化 | `data/RegionStateStore` | 已处理区块键的 `SavedData` |
| 生成 | `spawn/CitySpawner` | 补充镇/城村民 + 城市常驻商人 |
| 指令 | `command/NearestSettlementCommand` | `/nearestcity`、`/nearesttown` |
| 入口 | `RegionsMod`、`RegionsClientMod` | 服务端 + 客户端初始化 |

**71 个单元测试**覆盖纯逻辑模块（`SettlementTier`、`SettlementName`、`TerrainModifier`、`BiomeRegionResolver`、`RegionNameGenerator`、`RegionNamePool`、`RegionStateStore`），包括"无相邻城市"不变性与分层/命名确定性。GUI/网络/Mixin/生成模块按设计在游戏内验证。

## 技术实现

### 按放置点缩放（JigsawTierMixin）

`JigsawStructure.findGenerationPoint` 读取 `maxDepth` 和 `maxDistanceFromCenter`，并作为参数传给静态方法 `JigsawPlacement.addPieces(...)`。该 Mixin 用 `@Redirect` 截获这唯一一次调用，根据 `ctx.seed()` + `ctx.chunkPos()` 计算层级，在转发前把两个尺寸参数乘以该层级的系数。因为这些值是作为调用参数流动的（而非共享配置字段），所以每个放置点独立缩放。注册为服务端（世界生成）Mixin。

### 无需同步分层的确定性

层级是纯函数 `SettlementTier.tierFor(worldSeed, chunkX, chunkZ)`。服务端尺寸 Mixin 在世界生成时用它；客户端显示用同一个函数，种子来自种子包、聚落区块坐标来自聚落包。于是服务端尺寸与客户端名称**无需任何服务端→客户端的层级/名称同步**即可一致——服务端只推送区块坐标。

### 聚落包（为何不用客户端扫描）

1.20.1 客户端不能可靠地暴露结构动态注册表或已加载区块的完整 `StructureStart` 映射，因此客户端村庄扫描无法匹配村庄 id。取而代之的是，服务端的 `RoadWeaverIntegrator`（权威发现村庄之处）把聚落区块坐标推送给客户端：加入时推送完整已知集合，之后每次新发现增量推送。客户端再从种子推导层级与名称。这也让多人游戏变得可靠。

### 稳定、聚落定义的名称

生物群与地形在聚落中心采样（而非玩家脚下）并按聚落缓存，所以前缀属于村庄、不会随你走动而漂移。地形只在中心 + 水域扫描环加载完成后才解析（带一个短超时回退），因此传送后你会看到约 1–2 秒空白，然后是最终名称——绝不会出现一个随后翻转的临时名称。

## 运行要求

- Minecraft 1.20.1
- Fabric Loader 0.16+
- Fabric API 0.92.9+1.20.1
- [RoadWeaver 2.2.2+1.20.1](https://modrinth.com/mod/roadweaver)（道路生成）

**可选**（改善村庄视觉效果，无需额外配置）：
- [Better Villages](https://modrinth.com/mod/better-village) —— 更大、更精细的村庄模板
- [ChoiceTheorem's Overhauled Village](https://modrinth.com/mod/ct-overhaul-village) —— 23 种村庄变体

## 构建

```bash
# 需要 Java 17
./gradlew build
```

将 `build/libs/LivingVillages-Cluster-*.jar` 放入 `.minecraft/mods/`，与 Fabric API 和 RoadWeaver 一起使用。

> 注：构建前需把 `roadweaver-fabric-2.2.2-1.20.1.jar` 放入 `libs/`（已 gitignore，不提交），以及 `gradle/wrapper/gradle-wrapper.jar`（同样 gitignore）。

## 可调参数

以下为 `CitySpawner` / `SettlementTier` 中的常量，便于调整：
- 村民目标：镇 20、城 40
- 分层比例：城:镇:村 = 1:2:5；最小间隔 = 1 个网格格
- 城市商人：常驻（极大反刷延迟 + 持久化）
- 生成节流：20 秒；单次生成上限：5；近玩家半径：128 格

## 未来方向

- **城市城墙的算法化生成** —— 像 RoadWeaver 的道路那样，按算法围绕巨型城市生成城墙，增强沉浸感。
- **`/nearestcity any`** —— 搜索全部网格格（而非仅已发现），在未探索地形中找到城市。
- **配置文件** —— 把上述可调参数暴露为配置项。
- **经济与商队** —— 矿车商队在聚落间运输货物，价格随供需波动。
- **区域声望** —— 玩家在某聚落的行为影响村民态度。

## 许可证

MIT
