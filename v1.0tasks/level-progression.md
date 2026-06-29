# LevelProgression — Minimal Workable Plan

> 前置: data-layer  
> 产出: `core/level/LevelProgression.java`  
> 写入字段: `villageLevels[]`, `specialities[]`, `accumulatedConsumption[]`  
> 读: `villages[]`, `warehouses[]`

---

## 任务清单

### L-1 阈值常量

- [ ] **L-1.1** 定义 4 级阈值:
  - LV2: food ≥ 300
  - LV3: food ≥ 3000 && luxury ≥ 100
  - LV4: food ≥ 15000 && luxury ≥ 500

### L-2 等级判定

- [ ] **L-2.1** 遍历所有 village, 读取 `accumulatedConsumption[villageId]`
- [ ] **L-2.2** 提取 foodConsumed (key="food") 和 luxuryConsumed (key="luxury")
- [ ] **L-2.3** 按阈值阶梯判定 level 1-4

### L-3 特产标记

- [ ] **L-3.1** level == 4 且 specialities[villageId] == null → 从 warehouses 中选库存最大商品 → 标记为特产

### L-4 公开 API

- [ ] **L-4.1** `updateLevels(VillageStateStore store)` — 主入口 (cfg 预留, 当前阈值硬编码)

### L-5 单元测试

- [ ] **L-5.1** food=0 → Lv1
- [ ] **L-5.2** food=300 → Lv2
- [ ] **L-5.3** food=3000, luxury=100 → Lv3
- [ ] **L-5.4** food=15000, luxury=500 → Lv4
- [ ] **L-5.5** Lv4 → specialities 注册
- [ ] **L-5.6** 幂等: 重复调用不改变已注册 specialities

---

## 完成标准

- [ ] 4 级阈值判定正确
- [ ] Lv4 特产标记仅首次触发
- [ ] 所有 L-5 测试通过
