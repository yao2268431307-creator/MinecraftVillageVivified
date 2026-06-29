# CaravanEntityManager — Minimal Workable Plan

> 前置: data-layer, caravan-simulator  
> 产出: `adapter/caravan/CaravanEntityManager.java`  
> 写入: 世界实体 (via ServerLevel)  
> 读: `caravanStates[]`, `interClusterEdges[]`

---

## 任务清单

### CE-1 实体生命周期

- [ ] **CE-1.1** 遍历 caravanStates: phase==MOVING 且无实体 → spawn
- [ ] **CE-1.2** 遍历现有实体: 对应 caravan phase!=MOVING → despawn
- [ ] **CE-1.3** 维护 `Map<UUID, Entity>` 映射 (caravanId → 矿车实体)

### CE-2 实体 Spawn

- [ ] **CE-2.1** spawn `MinecartFurnace` 作为火车头 (fuel 由 caravan.fuelTicks 控制)
- [ ] **CE-2.2** 若 caravan.cargo 非空 → spawn `MinecartChest` 并填充 cargo
- [ ] **CE-2.3** 将实体链接 (FurnaceMinecart 推动 ChestMinecart)

### CE-3 位置同步

- [ ] **CE-3.1** 每 tick 从 caravan.currentPathIndex → 查 EdgeRecord.path → 得 Vec3i → 更新实体 position
- [ ] **CE-3.2** 铁轨中断检测: 检查 path[i] 处方块是否为铁轨 → 若非 → 实体停止

### CE-4 公开 API

- [ ] **CE-4.1** `syncEntities(ServerLevel level, VillageStateStore store)` — 主入口
- [ ] **CE-4.2** `removeAllCaravanEntities(ServerLevel level)` — 清理所有商队实体

### CE-5 测试 (GameTest)

- [ ] **CE-5.1** caravan phase=MOVING → spawn 实体
- [ ] **CE-5.2** caravan phase=UNLOADING → despawn 实体
- [ ] **CE-5.3** 铁轨中断 → 实体停止移动

---

## 完成标准

- [ ] MOVING 商队有对应矿车实体
- [ ] 实体位置与 CaravanState 同步
- [ ] 非 MOVING 状态下实体被清理
- [ ] removeAll 不留孤儿实体
