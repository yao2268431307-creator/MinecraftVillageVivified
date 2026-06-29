# ClusterDetector — Minimal Workable Plan

> 前置: data-layer, config  
> 产出: `core/cluster/ClusterDetector.java`  
> 写入字段: `clusters[]`  
> 读: `villages[]`

---

## 任务清单

### CD-1 空间索引

- [ ] **CD-1.1** 构建网格索引: cellSize = eps, 将 villages 按 `(x/cellSize, z/cellSize)` 分桶
- [ ] **CD-1.2** `regionQuery(village, eps)` — 查 v 所在桶及相邻 8 桶, 筛选 `distance < eps`

### CD-2 DBSCAN 核心

- [ ] **CD-2.1** 遍历 villages (filter `placed==true`, not yet clustered)
- [ ] **CD-2.2** 若 `|neighbors| >= minPts` → 创建新 cluster, `expandCluster(v, neighbors)`
- [ ] **CD-2.3** `expandCluster(v, neighbors)` — 递归合并 density-reachable 村庄
- [ ] **CD-2.4** 孤立村庄 (noise) 当 `minPts=1` 时也形成单成员 cluster

### CD-3 ClusterRecord 构造

- [ ] **CD-3.1** id = `"cluster_" + hash(centerVillage.position)` — 取 member 中 bedCount 最大者
- [ ] **CD-3.2** centerVillageId = bedCount 最大者的 id
- [ ] **CD-3.3** isNamed = false, edgesBuilt = false

### CD-4 公开 API

- [ ] **CD-4.1** `detectClusters(VillageStateStore store, ModConfig cfg)` — 主入口
- [ ] **CD-4.2** 仅处理尚未归属的村庄; 新 cluster 追加到已有 `clusters[]`
- [ ] **CD-4.3** 幂等: 重复调用不产生重复 cluster

### CD-5 单元测试

- [ ] **CD-5.1** 间距 < eps 的两村 → 同一 cluster
- [ ] **CD-5.2** 间距 > eps 的两村 → 不同 cluster
- [ ] **CD-5.3** centerVillageId == max bedCount
- [ ] **CD-5.4** 所有 village 最终归属于恰好一个 cluster
- [ ] **CD-5.5** 重复调用 → clusters 数量不变

---

## 完成标准

- [ ] DBSCAN 正确聚类 (eps 参数控制)
- [ ] 空间索引使 regionQuery 不暴力 O(n²)
- [ ] 幂等性保证
- [ ] 所有 CD-5 测试通过
