# Living Villages

> 让 Minecraft 的村庄不再是空洞的资源采集点，而是一个有名字、有聚落、有人间烟火气的文明网络。

---

## 当前状态

| 层 | 状态 | 说明 |
|----|:---:|------|
| **Core** (纯 Java 算法) | ✅ 完成 | 116 个 JUnit 测试全部通过 |
| **Adapter** (MC 胶水) | ✅ 编译通过 | Fabric 1.21.4 + Fabric API |
| **世界生成** | ✅ 逻辑就绪 | 待实机验证 |
| **道路铺设** | ✅ 逻辑就绪 | 待实机验证 |
| **区域名飘字** | ✅ 逻辑就绪 | 待实机验证 |
| **经济 / 商队** | ⏸️ 本轮搁置 | Core 层算法已写完，未接入 Daily Tick |

---

## 快速开始

```bash
git clone https://github.com/yao2268431307-creator/MinecraftVillageVivified.git
cd MinecraftVillageVivified

# 跑 Core 层测试（纯 Java，秒级）
./gradlew :common:test          # 116/116 PASS

# 编译 Fabric 模块（首次需下载 MC ~200MB）
./gradlew :fabric:build          # BUILD SUCCESSFUL

# 启动 Minecraft 测试
./gradlew :fabric:runClient
```

**需要**: Java 21+, Gradle 8.11+ (wrapper 自带)

---

## 核心玩法

### 村庄聚落化
原版的村庄均匀散布在世界中（间距 ~544 格）。Living Villages 把它们聚拢：

```
原版:  · · · ──── · · ──── · · ·   （稀疏均匀）
我们:  ⬤········⬤········⬤        （K 个聚落，内部密集）
      晴原镇      落日川      望雪山
```

- 原版村庄数量不变，只是位置被"拉"到聚落中心附近
- K 自动计算：`K = 原版村庄数 / 7`。世界越大村庄越多，K 越大
- 聚落半径可配置（默认 320 格）

### 中文命名
每个聚落和村庄都有基于 biome 的确定性中文名。`hash(坐标)` 保证同一世界永远同名。

```
聚落群 "晴原" (Plains)
  ├── 晴原镇  (中心镇)
  ├── 麦浪村  (卫星村)
  ├── 向阳集  (卫星村)
  └── 和风庄  (卫星村)
```

### 道路连接
聚落中心之间自动铺设道路（A* 寻路 + Bezier 平滑）。宽度和材料可配置。

### 区域飘字
玩家走进一个聚落时，屏幕上方显示 `晴原——麦浪村`。

---

## 架构

```
┌──────────────────────────────────────┐
│            fabric/ (MC Adapter)       │
│  RailPlacer · RoadPlacer              │
│  CaravanEntityManager                 │
│  NbtVillageStateStore (extends SavedData) │
│  BiomeResolverImpl · ConfigLoader     │
│  LivingVillagesFabric (Fabric entry)  │
│  RegionTitleDisplay (client)          │
└──────────────┬───────────────────────┘
               │ depends on
┌──────────────▼───────────────────────┐
│          common/ (Core Layer)         │
│  零 Minecraft 依赖，纯 Java 21        │
│                                      │
│  KCenterGenerator · ClusterDetector   │
│  NameGenerator · RegionalGraph        │
│  MarketSimulator · CaravanSimulator   │
│  LevelProgression · TickOrchestrator  │
│  VillageStateStore · ModConfig        │
└──────────────────────────────────────┘
```

### 核心算法

| 模块 | 算法 |
|------|------|
| 聚落生成 | 原版 village position → Greedy Max-Min K-Center → Pull-to-Center |
| 聚落检测 | DBSCAN (eps=250, Spatial Grid Index) |
| 路径规划 | KNN(k=2) → 3D Constrained A* → Cubic Bezier Smooth |
| 村庄命名 | Voronoi + Biome Tag → Hash-based Pool Selection |
| 经济定价 | CES Production Function + Graph Laplacian Heat Diffusion |
| 商队调度 | 6-state FSM (IDLE→LOADING→MOVING→STUCK→UNLOADING→RETURNING) |

---

## 配置

`config/livingvillages.toml`：

```toml
[world]
cluster_count = 0              # 0 = 自动 (原版村庄数 ÷ villages_per_cluster)
villages_per_cluster = 7       # 每聚落村庄数

[cluster]
cluster_radius = 320           # 聚落半径 (blocks)
min_separation = 96            # 村庄最小间距

[graph]
knn_degree = 2                 # 路网连接度

[road]
road_width = 3                 # 道路宽度
road_material = "minecraft:stone_bricks"

[economy]
daily_tick_interval = 24000    # MC 日间隔
max_caravans = 20              # 最大商队数
```

---

## 技术栈

| 组件 | 版本 |
|------|------|
| Minecraft | 1.21.4 (≈ 26.x) |
| Java | 21 |
| Gradle | 8.11.1 (wrapper) |
| Fabric Loader | 0.16.10 |
| Fabric API | 0.110.0+1.21.4 |
| Fabric Loom | 1.9-SNAPSHOT |
| JUnit | 5.10.3 |
| Mappings | Mojang Official |

---

## 开发

```bash
# 分支命名
feature/<模块名>   fix/<问题>   refactor/<内容>

# PR 流程
1. 从 main 切分支
2. 开发 + ./gradlew :common:test :fabric:build
3. 提交 PR → review → squash merge
```

---

## 路线图

| 优先级 | 功能 | 状态 |
|:---:|------|:---:|
| 1 | 世界生成实机验证 | 🔜 |
| 2 | 道路铺设实机验证 | 🔜 |
| 3 | 区域名飘字实机验证 | 🔜 |
| 4 | NBT 持久化测试 | 🔜 |
| 5 | Daily Tick 接入经济/商队 | 📋 |
| 6 | Forge 模块 | 📋 |
