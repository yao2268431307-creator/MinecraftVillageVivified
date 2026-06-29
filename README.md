# Living Villages — 让 Minecraft 村庄活起来 设计文档 v4

> **愿景**：让 Minecraft 的村庄和村民不再是空洞的资源采集点，而是一个有名字、有羁绊、有经济生命、有人间烟火气的文明网络。
>
> **灵感来源**：
> - [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) — 自动连接村庄的道路网络 + KNN/Delaunay/RNG路网规划 + A\*/双向A\*/流体模拟寻路
> - [Project Sid (arXiv:2411.00114)](https://arxiv.org/abs/2411.00114) — 大规模多智能体文明模拟。10~1000+ AI agent 在 MC 中自发形成分工、规则、文化传播
> - 《天际线》的 NPC 活力 + 《我的世界》的建筑自由度
>
> **核心原则**：
> - 不新增物品/方块，赋予原版被忽视的元素更多意义
> - 不推翻原版系统，在原版之上"长"出新玩法层
> - 用经典 CS 算法驱动，不靠堆新内容
>
> **目标 MC 版本**：26.1+ (Java Edition)
>
> **Loader**：Architectury → Fabric + Forge + NeoForge
>
> **版本选择理由**：26.1+ 引入了 data-driven villager trades（datapack JSON 定义交易），"独特特产"系统可完全通过 datapack 实现，无需 Mixin。
>
> **Loader 选择理由**：本 mod ~85% 代码是 loader 无关的纯逻辑（聚类算法、经济模型、命名、商队系统、Datapack 资源）。Mixin 注入点仅涉及约 8 个平台差异（事件注册、Mixin 目标类名、配置路径等）。Architectury + @ExpectPlatform 抽象可覆盖这些差异，一次编写编译三个平台。参考：MCA Reborn 已通过此方案同时支持 Fabric + Forge + NeoForge + Quilt。

---

## 一、问题诊断

### 1.1 原版村庄的"空洞"问题

| 原版现状 | 玩家感受 |
|----------|----------|
| 村庄之间毫无关联，各自孤立 | "一个村子就是一个资源采集点" |
| 村民交易列表固定且无趣 | "搞点绿宝石换附魔书，完事" |
| 村民行为极其简单：if 天黑→睡觉 | "像机器人，不像人" |
| 村民一辈子不出村 | "这个世界是死的" |
| 村庄 = 撸木头 + 撸石头 + 翻铁匠铺 | "来过一次就不想再来了" |
| 煤矿车、箱子矿车几乎没人用 | "这些东西存在的意义是什么？" |
| 村庄间距 800-1000+ 格 | "走了半天一个人影都没有" |

### 1.2 我们解决的问题

```
原版:  均质散布，间距遥远，孤立无关联
  · · · ──────── · · ────────── · ──────── · · ·

目标:  K 个大聚落(城市)，每个内部密集，之间主干道连接
  
  聚落群 "晴原" (R=200格, 5-8个村)          聚落群 "落日川"
  ┌────────────────────────┐    主干道    ┌────────────────────┐
  │  ·晴原镇(中心)·   ·   │  ═══════════ │      · 落日川镇    │
  │    ·     ·   ·        │  (A* 规划)   │  ·        ·     ·  │
  │   ·     ·    ·        │             │      ·   ·     ·   │
  └────────────────────────┘             └────────────────────┘
  聚落内部: 自然密集，步行可及              聚落内部: 自然密集
  不需要人工道路                          不需要人工道路

  ─── 主干道连接聚落中心（类似高速公路）
```

---

## 二、核心架构：两层图模型

这是本 mod 最重要的设计决策。村庄网络不是扁平的全联通图，而是**层次化的二层结构**。

### 2.1 两层图定义

```
Layer 1 (聚落内): 不做人工干预
  - 村庄在 R_cluster ≈ 200 格内自然散布
  - 玩家步行即可遍历整个聚落
  - 不需要我们生成道路或铁轨
  - 聚落内村民可以自由流动（短距寻路）

Layer 2 (聚落间): 主干道/铁路
  - 节点 = K 个聚落中心镇
  - 边   = A\* 规划的路径（道路或铁轨）
  - 每个中心镇承担: 车站、贸易站、命名锚点
  - 煤矿车商队仅在聚落中心之间运行
```

### 2.2 与 RoadWeaver 的关键差异

| | RoadWeaver | Living Villages |
|------|------|------|
| 图的节点 | 每个村庄 | K 个聚落中心镇 |
| 边 | 村→村 | 聚落→聚落 |
| 聚落内连接 | ✅ 所有村庄互联 | ❌ 不需要（聚落内自然密集） |
| 主干道/跨区域 | 无概念 | ✅ 聚落间的"高速公路" |
| 经济节点 | 每个村独立 | 中心镇汇总卫星村 |
| 寻路算法 | A\* | A\*（同算法，不同粒度假图） |

我们向 RoadWeaver 致敬但不依赖它：A\* 寻路、贝塞尔平滑、隧道桥梁判定——这些核心算法原样保留，但图的节点从 N 个村庄降维到 K 个聚落中心。

### 2.3 图的规模降维

```
例子: 世界中有 40 个村庄

RoadWeaver 模式:  N=40 节点 → O(N²) = 780 对潜在边
LivingVillages:  K=5 节点  → O(K²) = 10 对潜在边（降维 78 倍）

经济矩阵: N×N = 1600  →  K×K = 25（降维 64 倍）
```

---

## 三、Layer 0 — 村庄生成重写（K-Center Poisson Cluster Process）

### 3.1 目标

用算法驱动村庄生成，形成 K 个密集聚落群（城市感），替代原版的均质随机散布。

```
原版 random_spread:
  ┌─ · · · ───── · · ──────── · ────── · · · ─┐
  全部稀疏均匀，间距 800-1000 格

我们的 K-Center Poisson Cluster:
  ┌─ ⬤ ──── ⬤ ──── ⬤ ──── ⬤ ─┐   ← K 个区域中心均匀覆盖世界
     │       │       │       │
   ···     ···     ···     ···    ← 每个中心周围 Poisson 采样
   ··      ··      ··      ··      5-8 个村庄，密集散布
  "晴原城" "落日川" "望雪山" "凌霄城"
```

### 3.2 算法

```python
def k_center_poisson_cluster(world_seed, K, R_cluster,
                               N_per_cluster, min_separation):
    """
    K-Center Poisson Cluster Process for village placement.

    参数:
      world_seed      — 世界种子
      K               — 聚落数量（默认 N_villages_total / 6，可配置）
      R_cluster       — 聚落半径（默认 200 格）
      N_per_cluster   — 每聚落村庄数（默认 5-8，带随机波动）
      min_separation  — 村庄最小间距（默认 32 格）

    返回:
      {cluster_id: [village_positions]}
    """

    # ── 阶段 1: 生成 K 个聚落中心 ──
    centers = []
    spacing = WORLD_SIZE / sqrt(K)  # 均匀覆盖

    for i in range(K):
        center = produce_random_spread_position(
            seed = worldSeed XOR hash(i),
            spacing = spacing
        )
        # 验证地形
        if not valid_terrain(center):
            center = jitter_and_retry(center, max_attempts=16)

        centers.append(center)

    # ── 阶段 2: 每中心周围 Poisson Disk 采样生成子村庄 ──
    clusters = {}
    for i, center in enumerate(centers):
        # 波动: 5-8 个村庄
        n = N_per_cluster + random_int(-1, 2)

        poisson = PoissonDiskSampler(
            center = center,
            radius = R_cluster,
            min_dist = min_separation,
            seed = worldSeed XOR hash(i) XOR 0xF00D
        )

        villages = []
        for j in range(n):
            pos = poisson.next_sample()
            if pos is None:
                break
            if valid_terrain(pos) and valid_biome(pos):
                villages.append(pos)

        clusters[i] = Villages(center=center, satellites=villages)

    return clusters
```

### 3.3 关键参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| K | `ceil(N_total / 6)` | 每 6 个村庄形成 1 个聚落 |
| R_cluster | 200 格 | 聚落内最大半径 |
| N_per_cluster | 5-8 | 每个聚落内的村庄数量（随机波动 ±1） |
| min_separation | 32 格 | Poisson Disk 最小间距，避免村庄重叠 |

### 3.4 实现路径

**步骤 A — 注册自定义 Placement Type（少量 Java 代码）**

```java
// common/src/main/java/.../worldgen/ClusteredPlacement.java
public class ClusteredPlacement extends StructurePlacement {
    public static final Codec<ClusteredPlacement> CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("clusters").forGetter(p -> p.K),
            Codec.INT.fieldOf("radius").forGetter(p -> p.R),
            Codec.INT.fieldOf("per_cluster").forGetter(p -> p.N)
        ).apply(instance, ClusteredPlacement::new));

    @Override
    protected boolean isPlacementChunk(WorldGenLevel level,
                                        ChunkPos chunkPos) {
        long seed = level.getSeed();
        return isWithinAnyCluster(chunkPos, seed, this.K, this.R);
    }

    private boolean isWithinAnyCluster(ChunkPos pos, long seed,
                                        int K, int R) {
        for (int i = 0; i < K; i++) {
            ChunkPos center = computeCenter(seed, i);
            if (pos.getChessboardDistance(center) <= R) {
                return true;
            }
        }
        return false;
    }

    private ChunkPos computeCenter(long seed, int kIndex) {
        // 使用 kIndex 和 seed 确定性计算第 k 个中心
        // 复用原版 random_spread 的分布逻辑
    }
}
```

**步骤 B — Datapack 覆盖 village structure_set**

```json
// data/livingvillages/worldgen/structure_set/villages_clustered.json
{
  "structures": [
    { "structure": "minecraft:village_plains",   "weight": 1 },
    { "structure": "minecraft:village_desert",   "weight": 1 },
    { "structure": "minecraft:village_savanna",  "weight": 1 },
    { "structure": "minecraft:village_snowy",    "weight": 1 },
    { "structure": "minecraft:village_taiga",    "weight": 1 }
  ],
  "placement": {
    "type": "livingvillages:clustered",
    "clusters": 5,
    "radius": 200,
    "per_cluster": 7,
    "salt": 10387312
  }
}
```

**步骤 C — 禁用原版均匀散布**

```json
// data/livingvillages/worldgen/structure_set/minecraft_villages_override.json
{
  "structures": [],
  "placement": {
    "type": "minecraft:random_spread",
    "spacing": 10000,
    "separation": 9999,
    "salt": 10387312
  }
}
```

### 3.5 命名：Voronoi + 群系主导

```
K 个聚落中心 → Voronoi 剖分 → K 个命名区域

每个区域:
  1. 查找区域内所有村庄所在群系
  2. 取主导群系 (mode)
  3. 从该群系的前缀词库中选词
  4. 生成: {群系前缀}{地貌中缀}{等级后缀}

示例:
  Voronoi 区域 #1 (主导群系: PLAINS)
    → 前缀词库: 晴原、麦浪、向阳、丰谷、金穗、和风、长云
    → 哈希: hash(聚落中心坐标) → 确定性选 "晴原"
    → 中心镇: "晴原镇"
    → 卫星村: "麦浪村"、"向阳集"、"金穗里"、"和风庄"
```

---

## 四、Layer 1 — 聚落间基础设施

### 4.1 主干道路网络

```java
class RegionalGraph {
    List<ClusterCenter> centers;  // K 个节点

    void planNetwork(NetworkStrategy strategy) {
        // strategy = KNN(k=2) / MST / Delaunay
        for (Edge edge : strategy.edges(this)) {
            Path route = ConstrainedAStar.plan(
                edge.from.position, edge.to.position,
                constraints = RailConstraints  // 或 RoadConstraints
            );
            buildRoute(route, edge.type);
        }
    }
}
```

KNN(k=2) 是默认推荐策略：每个聚落连最近的两个邻居，保证连通但不冗余。

### 4.2 煤矿车商队（聚落间物流）

仅在聚落中心之间运行。卫星村没有车站，村民步行到中心镇上车。

```
商队构成:
  🚂 [煤矿车]  ← 由 CaravanManager 每 tick 维护 Fuel/PushX/PushZ
  📦 [箱子矿车] ← 最多 4 辆（煤矿车物理约束）
  📦 [箱子矿车]
  📦 [箱子矿车]
  📦 [箱子矿车]

运行规则:
  - 仅沿 K 条聚落间主干道运行
  - 到站 → 卸货 → 更新中心镇仓库 → 装载当地特产 → 发车返回
  - 铁轨中断 → 停车 + 粒子警报 → 等待玩家修复
  - 90° 弯 → 废掉商队（Wiki 验证），铁轨设计必须走直轨 + 缓弯
```

### 4.3 聚落中心的特殊角色

```java
class ClusterCenter extends Village {
    RailwayStation station;        // 车站（铁轨起点/终点）
    TradingPost tradingPost;       // 贸易站（经济接口）
    SignpostNetwork signposts;     // 路标网络

    // 聚落内汇总
    float aggregateSupply[goods];  // = Σ satellite.supply
    float aggregateDemand[goods];  // = Σ satellite.demand
    float aggregateProduction[];   // = Σ satellite.production

    // 聚落间商路
    List<TradeRoute> outgoingRoutes;
}
```

**中心镇是经济接口**：所有卫星村的供需汇总到此、所有跨聚落贸易经由此节点。

---

## 五、Layer 2 — 村庄身份与交易

### 5.1 命名

```
两层命名体系:

聚落名 (Voronoi 区域命名):
  由 Voronoi 剖分 + 区域主导群系决定
  例: "晴原"、"望雪山"、"落日川"

子村名 (中心镇 + 卫星村):
  中心镇: {聚落名} + {等级后缀}
    例: 晴原镇、望雪山镇、落日川镇
  卫星村: {从聚落前缀词库中选不同词} + {等级后缀}
    例: 麦浪村、向阳集、金穗里
```

前缀/中缀/后缀词库见附录 A。

### 5.2 Datapack 交易模板（先留接口）

不写死内容，仅提供 JSON 模板和可配置接口：

```json
// data/livingvillages/villager_trade/_template.json
{
  "wants": { "id": "minecraft:emerald", "count": 64 },
  "gives": { "id": "minecraft:diamond", "count": 1 },
  "max_uses": 1,
  "xp": 30,
  "merchant_predicate": {
    "condition": "minecraft:entity_properties",
    "entity": "this",
    "predicate": {
      "type": "minecraft:villager",
      "nbt": "{VillagerData:{profession:\"CHANGE_ME\",level:5}}"
    }
  }
}
```

后期由配置 TOML 动态生成 JSON，或让 modpack 作者直接编辑。

---

## 六、Layer 3 — 层次化经济

### 6.1 聚落内清算

```
每 Economic Tick:
  1. 中心镇汇总所有卫星村:
     aggregateSupply[good]  = Σ satellite.supply
     aggregateDemand[good]  = Σ satellite.demand
     aggregateProduction    = Σ satellite.production

  2. 聚落内部价格 = 中心镇统一定价
     （R_cluster ≤ 200 格 → 信息/物流即时）

  3. 更新中心镇的 villagePriceMultiplier
     → 影响该聚落内所有村民的交易价格
```

### 6.2 聚落间价格扩散

```java
// 仅沿 K 条聚落间主干道执行热传导
// 矩阵维度从 N×N 降到 K×K

void diffusePricesClusterLevel(RegionalGraph graph, String good) {
    for (int iter = 0; iter < 10; iter++) {
        for (ClusterCenter node : graph.centers) {
            double localPrice = computeLocalPrice(node, good);
            double neighborSum = 0, totalWeight = 0;

            for (Edge edge : graph.edgesFrom(node)) {
                double weight = 1.0 / edge.distance;
                if (edge.hasActiveCaravan) weight *= 3.0; // 商队加速
                neighborSum += weight * edge.target.price(good);
                totalWeight += weight;
            }

            double alpha = 0.3;
            node.prices[good] = totalWeight > 0
                ? (1 - alpha) * localPrice + alpha * neighborSum / totalWeight
                : localPrice;
        }
    }
}
```

### 6.3 村庄每日消耗

```java
dailyConsumptionPerVillager = {
    FOOD: 1 unit,    // 面包×1 或等价
    WOOD:  0.2 unit, // 建筑维护/取暖
}

biomeExtraNeeds = {
    SNOWY:    { LEATHER: 0.1 },
    DESERT:   { WOOD: 0.8 },
    MOUNTAIN: { COAL: 0.3 },
    OCEAN:    { WOOD: 0.8 },
}

// 缺粮 → 连续7天 → 人口减少 + 全交易涨价 ×1.5
// 盈粮 → 1.5×需求 → prosperity++ → 累计20 → 新生村民
```

### 6.4 跑商（套利搜索）

```java
// 聚落间套利（Floyd-Warshall 一次性算清全源最短路）
TradeJournal.calculateRoutes(playerPosition, graph):
    kmatrix = FloydWarshall(graph)  // K×K 全源最短路
    for (source, target, good):
        buyPrice  = source.price(good)
        sellPrice = target.price(good)
        distance  = kmatrix[source.id][target.id]
        transportCost = distance * costPerBlock
        profit = sellPrice - buyPrice - transportCost
        if profit > 0: routes.add(...)
    return routes.sortedBy(profit)
```

### 6.5 工业玩家与平衡

```
生电玩家产出 10000 面包:
  → 一个 Lv2 聚落中心每天汇总吸收 ~120 面包（6 个卫星村 × 20）
  → 单聚落吸收有限 → 必须分散运到多个聚落
  → 需要铁路网 → 需要煤矿车商队 → 需要煤供应
  → 工厂不是"打破经济"，而是"供给端发动机"
```

---

## 七、Layer 4 — 村民 AI 扩展（后期）

在原版 Brain 系统上叠加，不替换实体。

```java
// 自定义 MemoryModuleType
public static final MemoryModuleType<VillageShortage> RESOURCE_SHORTAGE = ...;

// 自定义 Sensor → 检测邻村 + 资源短缺 → 写入 Memory
class InterVillageSensor extends Sensor<Villager> { ... }

// 在 WORK/GATHER 时段注入自定义 Activity
// 聚落内短距旅行: 标准 Pathfinding, 扩大目的地范围
// 聚落间长距旅行: 抽象化, 后台旅行 + 传送
```

村民不可行的动作降级为替代方案：

| 原设计 | 降级方案 |
|--------|----------|
| 村民坐长椅 | 在休息区慢速闲逛 + 放松粒子 |
| 村民采集资源 | 拾取掉落物（仅限可拾取物品） |
| 村民驾驶矿车 | 商队做成独立逻辑系统 |

---

## 八、技术约束（Wiki 验证）

> 数据来源：[minecraft.wiki](https://minecraft.wiki)，MediaWiki API (2026-06-27)

### 8.1 村民能力

| 能力 | 状态 |
|------|:---:|
| 拾取物品 | ✅ 仅限 9 种食物/种子/骨粉 |
| 开门(木/铜) | ✅ |
| 开门(铁/栅栏/活板门) | ❌ |
| 爬梯子 | ⚠️ 不主动使用 |
| 工作站认领范围 | 48 格球体 |
| 出村 >32 格 | 6 秒遗忘原村庄 |
| 拾取条件 | `mobGriefing = true` |

### 8.2 交易

| 特性 | 值 |
|------|-----|
| 最大交易数 | 10 |
| 每等级解锁 | 最多 2 个 |
| 26.1+ 数据驱动 | Datapack JSON, 支持 merchant_predicate |
| 需求追踪 | 按物品，不按村民 |

### 8.3 煤矿车

| 特性 | 值 |
|------|-----|
| 仅 JE | Bedrock 无；Jeb 曾考虑删除 |
| 燃料 | 仅煤炭/木炭 |
| 速度 | 4 m/s (比走路慢) |
| 拖车 | 最多 4 辆，连接脆弱 |
| 90° 转弯 | 拖车向后脱出 |
| NBT 控制 | `Fuel(Short)`, `PushX(Double)`, `PushZ(Double)` |

---

## 九、算法支撑总表

| 模块 | 算法 | 来源 | 说明 |
|------|------|------|------|
| **K-Center 聚落生成** | K-Center + Poisson Disk Sampling | 计算几何 | K 个区域中心均匀分布，每中心周围 Poisson 采样生成子村庄 |
| **Voronoi 命名区域** | Voronoi Diagram | 计算几何, 1908 | K 个种子点 → K 个泰森多边形 → K 个命名域 |
| **村庄命名** | Hash-based Weighted Random | 确定性哈希 | `hash(坐标) → Seed → 带权随机选词` |
| **聚落检测** | DBSCAN | 密度聚类, 1996 | eps=250, 自动发现聚落群，不需预设 K |
| **聚落间路网** | KNN(k=2) / MST / Delaunay | 图论 | 仅连接 K 个中心，边数 O(K²) 而非 O(N²) |
| **铁轨/道路路径** | 3D Constrained A\* + Bezier Smooth | 启发式搜索 + 样条 | 坡度 ≤1、禁 90° 弯、最小桥梁/隧道代价 |
| **层次化经济** | CES Production + Graph Heat Eq | 微观经济学 + PDE | 聚落内统一清算，聚落间沿边热传导 |
| **套利搜索** | Floyd-Warshall | 全源最短路, 1962 | K×K 矩阵一次性算出所有聚落间套利路线 |
| **商队调度** | Discrete Event Simulation | DES 理论 | 煤矿车 = 有状态自驱实体，沿预定路线移动 |
| **交易绑定** | Template Substitution (Datapack) | 代码生成 | 运行时参数填充 JSON 模板 |
| **数据持久化** | NBT Serialization (SavedData) | Minecraft 原生 | `CompoundTag` 读写 |

---

## 十、MCA Reborn 对比

| | MCA Reborn | Living Villages |
|------|------|------|
| **方案** | 完全替换村民实体 | 在原版 Villager 上叠加 |
| **实体** | 自定义 VillagerMCA | 原版 Villager (不替换) |
| **AI** | 全部自己写 | 在原版 Brain 系统上扩展 |
| **生成** | 替换 village placement | 自定义 Placement Type + Datapack |
| **交易** | 自定义 GUI | 原版 GUI + Datapack 交易 + priceMultiplier |
| **兼容性** | 低 | 高（只在关键点 Mixin） |

---

## 十一、技术栈与项目结构

### 11.1 技术栈

```
语言:         Java 21 (26.1+ 要求)
构建:         Gradle 8.x + Architectury Loom
IDE:          IntelliJ IDEA (+ Architectury 插件)
Loader 框架:  Architectury 3.x
映射:         Mojang Official Mappings
测试:         JUnit 5 + GameTest Framework
CI/CD:        GitHub Actions
```

### 11.2 项目结构

```
LivingVillages/
├── build.gradle
├── settings.gradle
│
├── common/                          # 85% 代码
│   └── src/main/java/.../
│       ├── LivingVillages.java           # @ExpectPlatform 声明
│       ├── cluster/
│       │   ├── KCenterGenerator.java      # K-Center Poisson Cluster
│       │   ├── ClusterDetector.java       # DBSCAN 检测
│       │   └── VoronoiNamer.java          # Voronoi + 词库命名
│       ├── naming/
│       │   ├── NameGenerator.java
│       │   └── NamePool.java              # 词库
│       ├── economy/
│       │   ├── ClusterCenter.java         # 中心镇（经济汇总）
│       │   ├── MarketSimulator.java
│       │   ├── PriceDiffusion.java        # K×K 热传导
│       │   └── TradeRouteFinder.java      # Floyd-Warshall
│       ├── caravan/
│       │   ├── CaravanManager.java
│       │   └── RailPlanner.java           # A*
│       ├── graph/
│       │   ├── RegionalGraph.java
│       │   └── NetworkPlanner.java        # KNN/MST/Delaunay
│       └── data/
│           ├── VillageSavedData.java
│           └── TradeDatapackGen.java
│
├── fabric/    # Fabric @ExpectPlatform + Mixin
├── forge/     # Forge @ExpectPlatform + Mixin
└── neoforge/  # NeoForge @ExpectPlatform + Mixin
```

### 11.3 开发流程

```
单个功能开发循环:
  1. common/ 写纯逻辑 + 单元测试
  2. common/ 声明 @ExpectPlatform（如需要）
  3. fabric/ 实现 FabricPlatform
  4. forge/ 实现 ForgePlatform
  5. neoforge/ 实现 NeoForgePlatform
  6. fabric/ 运行 GameTest
  7. forge/ 运行验证
  8. 提交 (GitHub Actions → 三平台 jar)
```

---

## 十二、开发优先级

| 优先级 | 模块 | 理由 |
|:---:|------|------|
| **1** | 工程脚手架 (Architectury) | 一切的基础 |
| **2** | K-Center 聚落生成 (Placement Type + Datapack) | 核心：改写村庄生成分布 |
| **3** | DBSCAN 检测 + Voronoi 命名 | 在生成后检测并命名 |
| **4** | Datapack 交易模板 | 留接口，不写死内容 |
| **5** | K 节点路网 + A\* 主干道 + 煤矿车商队 | 聚落间连接和物流 |
| **6** | 层次化经济（汇总+扩散+套利） | K×K 矩阵热传导 |
| **7** | 贸易手册 UI + 跑商玩法 | 玩家可操作 |
| **8** | AI 扩展（Memory/Sensor/旅行） | 最需 Mixin，最后做 |

---

## 十三、设计决策记录

| 决策 | 选择 | 理由 |
|------|------|------|
| MC 版本 | **26.1+** | Datapack 交易系统 |
| Loader | **Architectury (Fabric+Forge+NeoForge)** | 85% 代码 loader 无关 |
| 村庄生成 | **K-Center Poisson Cluster** | 改写原版均质散布，形成城市群 |
| 图模型 | **两层图（聚落内自然 / 聚落间主干道）** | O(N²)→O(K²)，语义清晰 |
| 聚落间路网 | **KNN(k=2)** | 稀疏但保证连通 |
| 命名 | **Voronoi + 群系主导 + Hash 选词** | 确定性、上下文感知、中式古风 |
| 村民 AI | **叠加 Brain，不替换实体** | 与 MCA 相反；兼容性优先 |
| 交易修改 | **Datapack 模板（后期可配置）** | 官方支持，merchant_predicate 精准绑定 |
| 铁路动力 | **煤矿车** | 冷门原版物品，4 m/s 恒定，冒烟有"人味" |
| 经济模型 | **聚落内统一 / 聚落间热传导** | 层次化降低计算复杂度 |

---

## 十四、与原版兼容策略

| 原版系统 | 我们的处理 |
|----------|-----------|
| 村庄生成 (StructureFeature) | 自定义 Placement Type + Datapack 覆盖；不删除原版结构 |
| 结构模板 (Structure Template) | Datapack 注册填充建筑 NBT |
| Villager 类 | 不替换实体；自定义 Memory/Sensor/Activity 通过 Mixin 注入 |
| 村民交易 | Datapack (`villager_trade/`)；Mixin 叠加 `priceMultiplier` |
| Village / POI | 读取原版 Village 数据构建 VillageNode；不写入 |
| 铁轨/矿车 | 原版 Rail / MinecartFurnace / MinecartChest |
| Gossip/Demand | 保留原版机制；叠加村庄级经济系数 |
| 村民繁殖 | 软控制：gossip 条件 + 繁荣度触发 |

---

## 十五、数据流总览

```
┌─────────────────────────────────────────────────────┐
│                每个MC日 Tick                          │
│                                                     │
│  1. 聚落内清算                                       │
│     中心镇汇总所有卫星村的 supply/demand/production    │
│                                                     │
│  2. 聚落内定价                                       │
│     localPrice = base × (aggregateDemand/supply)^ε  │
│     更新 villagePriceMultiplier                      │
│                                                     │
│  3. 聚落间价格扩散 (K×K 热传导)                        │
│     仅沿 K 条主干道执行                                │
│     活跃商队 → 扩散速度 ×3                             │
│                                                     │
│  4. 煤矿车商队更新                                     │
│     CaravanManager.tick()                            │
│     Fuel/PushX/PushZ → 到站 → 卸货/装货               │
│                                                     │
│  5. 村庄升级检查                                      │
│     累计消费达标 → 升级 level                         │
│     Lv4 中心镇 → 注入 Datapack 特产交易                │
│                                                     │
│  6. 事件随机触发                                      │
│     丰收/矿难/商队/病害...                             │
└─────────────────────────────────────────────────────┘
```

---

## 附录 A：命名词库

### A.1 前缀·群系意境

| 群系 | 词库 |
|------|------|
| 雪原/冰刺 | 望雪、至冬、凌霜、素雪、寒山、凝冰、朔风 |
| 针叶林 | 松涛、寒松、云杉、听风、深木、冷杉 |
| 平原/向日葵 | 晴原、麦浪、向阳、丰谷、金穗、和风、长云 |
| 森林/黑森林 | 翠林、密叶、深根、苍木、幽林、青萝 |
| 丛林 | 雨林、碧藤、青蔓、繁花、巨木 |
| 沼泽 | 水泽、雾沼、青苇、烟水 |
| 沙漠/恶地 | 落日、沙海、孤烟、远尘、赤岩、热风 |
| 山地/丘陵 | 云顶、凌霄、望岳、临渊、叠石、高岭 |
| 海洋/沙滩 | 望海、听涛、临潮、碧波、白沙 |
| 河流 | 望川、临流、渡口、水畔 |

### A.2 中缀·地貌特征

| 地形特征 | 词库 |
|----------|------|
| 近河流 | 川、河、溪、渡、浦 |
| 近山脉 | 山、岭、峰、岩、崖 |
| 近海洋 | 海、滨、湾、浦、潮 |
| 近森林 | 林、森、木、荫 |
| 近湖泊 | 湖、泽、潭、池 |
| 平原 | 原、野、田、甸 |
| 丘陵 | 丘、陵、岗 |
| 两河交汇 | 津、渡、汇 |
| 关隘 | 关、塞 |

### A.3 后缀·聚落等级

| 规模 | 词库 |
|------|------|
| 极小 (1-2床) | 村、庄、屯 |
| 小 (3-5床) | 镇、集、里 |
| 中 (6-10床) | 邑、城、堡 |
| 大 (11+床) | 都、府、关 |
| 贸易枢纽 | 驿、埠、港 |

### A.4 聚落群统一命名规则

```
Voronoi 区域 → 主导群系 → 选聚落群名

聚落群 "晴原" (主导群系: PLAINS)
  ├── 晴原镇  (中心镇, Lv2)
  ├── 麦浪村  (卫星, Lv1, 同词库不同词)
  ├── 向阳集  (卫星, Lv1)
  ├── 金穗里  (卫星, Lv1)
  └── 和风庄  (卫星, Lv1)
```

---

> *RoadWeaver gave us roads between villages. We give them clustered cities, named regions, and a living economy flowing along the railways between them.*
