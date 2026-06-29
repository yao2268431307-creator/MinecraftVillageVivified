# Data Layer — Minimal Workable Plan

> 前置: none  
> 产出: `core/data/` 包下所有纯数据类型  
> 依赖本模块的模块: 全部

---

## 任务清单

### D-1 Vec3i (基础几何)

- [ ] **D-1.1** 创建 `Vec3i` record — fields: `int x, y, z`
- [ ] **D-1.2** 实现 `distanceSq(Vec3i other)` → `double`
- [ ] **D-1.3** 实现 `horizontalDistance(Vec3i other)` → `double`

### D-2 VillageRecord (村庄)

- [ ] **D-2.1** 创建 `VillageRecord` record — fields: `UUID id, Vec3i position, String biomeCategory, int bedCount, long firstSeenTick, boolean placed`
- [ ] **D-2.2** 添加 compact constructor: biomeCategory default `"unresolved"`, placed default `false`

### D-3 ClusterRecord (聚落)

- [ ] **D-3.1** 创建 `ClusterRecord` record — fields: `String id, List<UUID> memberVillageIds, UUID centerVillageId, boolean isNamed, boolean edgesBuilt`
- [ ] **D-3.2** 添加 compact constructor: isNamed/edgesBuilt default `false`, memberVillageIds 防御性拷贝

### D-4 ClusterName (命名)

- [ ] **D-4.1** 创建 `ClusterName` record — fields: `String clusterName, String centerTownName, Map<UUID,String> satelliteNames`

### D-5 EdgeRecord (路网)

- [ ] **D-5.1** 创建 `EdgeType` enum — values: `ROAD, RAIL`
- [ ] **D-5.2** 创建 `EdgeRecord` record — fields: `String fromClusterId, String toClusterId, List<Vec3i> path, EdgeType type`

### D-6 CaravanState (商队)

- [ ] **D-6.1** 创建 `CaravanPhase` enum — values: `IDLE, LOADING, MOVING, STUCK, UNLOADING, RETURNING`
- [ ] **D-6.2** 创建 `CaravanState` record — fields: `UUID caravanId, String fromClusterId, String toClusterId, int currentPathIndex, int fuelTicks, Map<String,Integer> cargo, CaravanPhase phase`

### D-7 VillageStateStore (数据接口)

- [ ] **D-7.1** 创建 `VillageStateStore` interface — 11 对 get/set + `void markDirty()`
- [ ] **D-7.2** 创建 `InMemoryVillageStateStore` (测试用) — 所有数据存 `HashMap`, `markDirty` no-op

### D-8 单元测试

- [ ] **D-8.1** `Vec3iTest` — 验证 distanceSq, horizontalDistance
- [ ] **D-8.2** `InMemoryStoreTest` — 验证 get/set 往返一致性

---

## 完成标准

- [ ] 所有 record 编译通过
- [ ] VillageStateStore 接口包含 11 字段完整 get/set
- [ ] InMemoryVillageStateStore 可用于后续模块测试
- [ ] D-8 测试通过
