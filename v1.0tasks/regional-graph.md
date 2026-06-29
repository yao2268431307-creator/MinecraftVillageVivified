# RegionalGraph — Minimal Workable Plan

> 前置: data-layer, config, cluster-detector  
> 产出: `core/graph/` 包 (RegionalGraph, NetworkPlanner, ConstrainedAStar, BezierSmoother)  
> 写入字段: `interClusterEdges[]`  
> 读: `clusters[]`, `clusterNames[]`

---

## 任务清单

### RG-1 NetworkPlanner (KNN 图连接)

- [ ] **RG-1.1** 实现 pairwise 水平距离矩阵 (O(K²) 可接受)
- [ ] **RG-1.2** 对每个 cluster 选距离最近的 k = `cfg.graph.knnK` 个邻居
- [ ] **RG-1.3** 去重: A→B 和 B→A 只保留一条
- [ ] **RG-1.4** 连通性检查: BFS，若不连通 → 桥接最近的两个连通分量
- [ ] **RG-1.5** 输出 `candidateEdges[{fromClusterId, toClusterId}]`

### RG-2 ConstrainedAStar (3D 约束寻路)

- [ ] **RG-2.1** A* 核心: 26-neighbor 3D 搜索, `h = 3D Euclidean`
- [ ] **RG-2.2** 代价函数: `cost = dist + slopePenalty + crossingPenalty`
  - slopePenalty: `Δy/horizDist > maxSlope` → cost × 10
  - crossingPenalty: 路径穿水域/岩浆 → cost × 5
- [ ] **RG-2.3** 禁止 90° 转弯: 方向变化 ≤ 45°
- [ ] **RG-2.4** 隧道/桥梁判定 (标记, 不放方块):
  - 上方 ≥ tunnelThreshold blocks 固体 → 隧道段
  - 下方悬空 > 3 blocks → 桥梁段
- [ ] **RG-2.5** 找不到路径时降低 maxSlope 约束重试 1 次; 仍失败 → skip + log warn

### RG-3 BezierSmoother (平滑)

- [ ] **RG-3.1** 提取关键路点: 方向变化 > 15° 的点
- [ ] **RG-3.2** Cubic Bezier 插值, 采样步长 2 blocks
- [ ] **RG-3.3** 验证平滑后无锐角: 相邻段夹角 ≥ 120°

### RG-4 公开 API

- [ ] **RG-4.1** `buildRegionalGraph(VillageStateStore store, ModConfig cfg)` — 主入口
- [ ] **RG-4.2** 仅处理 `edgesBuilt==false` 的 cluster; < 2 cluster → 直接返回
- [ ] **RG-4.3** 分帧支持: 记录进度, 每 tick 最多探索 1000 A* 节点
- [ ] **RG-4.4** 完成后标记 edgesBuilt=true

### RG-5 单元测试

- [ ] **RG-5.1** K 个 cluster → 总边数 ≈ K (KNN k=2)
- [ ] **RG-5.2** 每个 cluster 至少 1 条边
- [ ] **RG-5.3** BFS 连通性
- [ ] **RG-5.4** A* 路径 `Δy/horizDist ≤ maxSlope` (平坦假地形)
- [ ] **RG-5.5** Bezier 平滑后无 < 120° 转角
- [ ] **RG-5.6** 1 个 cluster → 空边列表, 不抛异常

---

## 完成标准

- [ ] KNN 图连通
- [ ] A* 路径满足坡度约束
- [ ] Bezier 平滑消除锐角
- [ ] 可分帧执行不阻塞 tick
- [ ] 所有 RG-5 测试通过
