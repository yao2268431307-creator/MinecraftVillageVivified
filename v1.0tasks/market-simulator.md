# MarketSimulator — Minimal Workable Plan

> 前置: data-layer, config, cluster-detector, regional-graph  
> 产出: `core/economy/` 包 (MarketSimulator, CESPricing, GraphHeatDiffusion)  
> 写入字段: `prices[]`, `warehouses[]`  
> 读: `villages[]`, `clusters[]`, `clusterNames[]`, `interClusterEdges[]`, `warehouses[]`, `caravanStates[]`

---

## 任务清单

### M-1 CESPricing (定价)

- [ ] **M-1.1** 定义商品常量表: basePrice + perCapitaDemand 至少包含 5 种商品 e.g. `["food", "wood", "stone", "iron", "luxury"]`
- [ ] **M-1.2** 对每个 cluster:
  - 人口 = sum(member villages' bedCount)
  - 需求 = 人口 × perCapitaDemand
  - 供给 = sum(member warehouses 存量)
  - CES: `Q = A × (α·L^ρ + (1-α)·K^ρ)^(1/ρ)`, `ρ=(σ-1)/σ`, `σ=cesElasticity`
- [ ] **M-1.3** 均衡价格 = `basePrice × (demand / supply)^(1/σ)`; supply→0 时 cap = basePrice × 10

### M-2 GraphHeatDiffusion (扩散)

- [ ] **M-2.1** 构建图: 节点=clusters, 边权重 `w_ij = 1/(1+distance_ij)`
  - RAIL 边权重 × 2
  - 活跃商队在该边 → 权重 × 3
- [ ] **M-2.2** 归一化拉普拉斯矩阵 L
- [ ] **M-2.3** 迭代扩散: `P_{t+1} = (I - αL) × P_t`, `α=diffusionRate`
  - 收敛条件: `max|ΔP| < 0.01` 或 100 次迭代

### M-3 消费与库存

- [ ] **M-3.1** 按均衡后价格计算每 cluster 实际消费量
- [ ] **M-3.2** 扣减 warehouses 库存
- [ ] **M-3.3** 累加 `accumulatedConsumption[]` (food/luxury 分类)

### M-4 缺粮惩罚

- [ ] **M-4.1** 追踪每个 village 连续缺粮天数 (库存 < 需求的 20%)
- [ ] **M-4.2** 连续 7 天 → bedCount -= 1 (最小 1)

### M-5 公开 API

- [ ] **M-5.1** `simulateMarket(VillageStateStore store, ModConfig cfg)` — 主入口

### M-6 单元测试

- [ ] **M-6.1** supply > demand → price < basePrice
- [ ] **M-6.2** demand > supply → price > basePrice
- [ ] **M-6.3** 有铁路连接 → 价格差 < 无连接
- [ ] **M-6.4** 活跃商队 → 收敛更快
- [ ] **M-6.5** 连续 7 天缺粮 → bedCount -1
- [ ] **M-6.6** 空 warehouses → price = basePrice × 10

---

## 完成标准

- [ ] CES 定价公式正确
- [ ] 热扩散使连通 cluster 价格趋同
- [ ] 缺粮惩罚正确生效
- [ ] 所有 M-6 测试通过
