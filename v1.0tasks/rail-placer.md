# RailPlacer — Minimal Workable Plan

> 前置: data-layer, config, regional-graph  
> 产出: `adapter/rail/RailPlacer.java`  
> 写入: 世界方块 (via ServerLevel)  
> 读: `interClusterEdges[]`

---

## 任务清单

### RP-1 进度追踪

- [ ] **RP-1.1** 维护放置游标: 当前 edge index + 当前 pathIndex — 存储在 private static field 或外部 tracker
- [ ] **RP-1.2** 每次调用从游标恢复，超预算时保存游标

### RP-2 路段判定

- [ ] **RP-2.1** `classifySegment(level, pos)` → TUNNEL / BRIDGE / GROUND
  - TUNNEL: pos 上方 3 格全是固体
  - BRIDGE: pos 下方 3 格全是空气
  - GROUND: 其余

### RP-3 方块放置

- [ ] **RP-3.1** 隧道段: 挖掘 pos 及上方 2 格 → 空气; 两侧 → 石砖; 天花板 → 火把
- [ ] **RP-3.2** 桥梁段: pos.y-1 → 木板, pos.y-2 → 栅栏; 桥高 > 10 时栅栏延伸到地面
- [ ] **RP-3.3** 地面段: 铁轨下方非完整方块 → 填充泥土

### RP-4 铁轨放置

- [ ] **RP-4.1** 计算方向: prev → pos → next 的朝向 (水平 4 方向 + 上/下)
- [ ] **RP-4.2** 上坡 (Δy > 0): 每格 = 动力铁轨 `minecraft:powered_rail` + 红石火把
- [ ] **RP-4.3** 平坦: 每 26 格 = 动力铁轨 + 红石火把, 其余 = 普通铁轨 `minecraft:rail`
- [ ] **RP-4.4** 转弯: Bezier 已保证无锐角, 使用普通铁轨的曲线变体

### RP-5 公开 API

- [ ] **RP-5.1** `placeRails(ServerLevel level, VillageStateStore store, ModConfig cfg, int maxBlockOps)` → `boolean`
- [ ] **RP-5.2** `removeRails(ServerLevel level, EdgeRecord edge)` — 仅移除铁轨方块, 不破坏隧道/桥梁结构

### RP-6 测试 (GameTest)

- [ ] **RP-6.1** 传入 BlockPos 路径 → 铁轨类型正确
- [ ] **RP-6.2** 桥梁段: 木板 + 栅栏
- [ ] **RP-6.3** 隧道段: 上方 2 格空气
- [ ] **RP-6.4** 上坡段: 每格动力铁轨
- [ ] **RP-6.5** 平坦段: 每 26 格动力铁轨
- [ ] **RP-6.6** 分帧: maxBlockOps=10 → 4 次调用完成

---

## 完成标准

- [ ] 隧道/桥梁/地面三段式放置正确
- [ ] 动力铁轨分布符合上坡+每26格规则
- [ ] 分帧执行不超预算
- [ ] removeRails 干净移除
