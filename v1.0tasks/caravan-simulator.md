# CaravanSimulator — Minimal Workable Plan

> 前置: data-layer, config, market-simulator  
> 产出: `core/caravan/CaravanSimulator.java`  
> 写入字段: `caravanStates[]`, `warehouses[]`  
> 读: `interClusterEdges[]`, `caravanStates[]`, `prices[]`, `warehouses[]`

---

## 任务清单

### CS-1 状态机骨架

- [ ] **CS-1.1** 遍历 `caravanStates` 中 `phase != IDLE` 的商队
- [ ] **CS-1.2** 实现 `switch(c.phase)` 分发到各状态处理

### CS-2 各状态逻辑

- [ ] **CS-2.1** LOADING: 比较 fromCluster/toCluster prices → 选价差最大商品 (≤3 种) → 从 warehouse 扣减 → 加入 cargo → `phase = MOVING`
- [ ] **CS-2.2** MOVING: fuelTicks--; 若 fuel ≤ 0 → `phase = IDLE`; currentPathIndex++; 若 path 铁轨缺失 → `phase = STUCK`; 若 pathIndex ≥ path.length → `phase = UNLOADING`
- [ ] **CS-2.3** STUCK: fuelTicks-- (半速); 若 fuel ≤ 0 → `phase = IDLE`; 路径恢复 → `phase = MOVING`
- [ ] **CS-2.4** UNLOADING: cargo → toCluster warehouses; cargo 清空; fuelTicks 重置; 选择下一目的地 (价差最大邻居) → `phase = LOADING`; 无合适邻居 → `phase = RETURNING`
- [ ] **CS-2.5** RETURNING: 沿 path 反向 MOVING; 到站 → `phase = IDLE`

### CS-3 新商队生成

- [ ] **CS-3.1** 活跃商队数 < maxActiveCaravans → 选价差最大 edge → `new CaravanState(phase=LOADING)`
- [ ] **CS-3.2** 追加到 caravanStates[]

### CS-4 路径完整性检查

- [ ] **CS-4.1** 定义 `PathIntegrityChecker` 接口 (Core 定义, Adapter 注入) — `boolean isRailIntact(EdgeRecord edge, int pathIndex)`
- [ ] **CS-4.2** 默认实现: `return true` (纯数据测试用)

### CS-5 公开 API

- [ ] **CS-5.1** `simulateCaravans(VillageStateStore store, ModConfig cfg)` — 主入口

### CS-6 单元测试

- [ ] **CS-6.1** 构造 edge + 商队 → tick N 次 → phase 按状态机迁移
- [ ] **CS-6.2** fuelTicks → 0 → phase = IDLE
- [ ] **CS-6.3** 到站 → cargo 写入 warehouses
- [ ] **CS-6.4** 活跃数 > max → 不再 spawn
- [ ] **CS-6.5** STUCK → 路径恢复 → MOVING

---

## 完成标准

- [ ] 6 态状态机正确迁移
- [ ] 燃料管理正确 (消耗/补给)
- [ ] 货物装卸正确 (价差驱动)
- [ ] 所有 CS-6 测试通过
