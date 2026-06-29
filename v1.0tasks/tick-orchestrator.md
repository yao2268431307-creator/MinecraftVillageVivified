# TickOrchestrator — Minimal Workable Plan

> 前置: 所有 Core 模块 (KCenterGenerator, ClusterDetector, NameGenerator, RegionalGraph, MarketSimulator, CaravanSimulator, LevelProgression)  
> 产出: `core/orchestrator/TickOrchestrator.java`  
> 耦合: 唯一允许 import 所有模块的类

---

## 任务清单

### TO-1 世界创建

- [ ] **TO-1.1** `onWorldCreate(long seed, int rangeX, int rangeZ, VillageStateStore store, ModConfig cfg)`:
  1. 调用 `KCenterGenerator.generateKCenters(seed, rangeX, rangeZ, cfg)`
  2. 结果写入 `store.setVillages()`
  3. `store.markDirty()`

### TO-2 每日 Tick

- [ ] **TO-2.1** 前置条件检查 — 三个 helper:
  - `hasUnprocessedVillages(store)` → villages 中有 placed==true 但未在 clusters 中者
  - `hasUnnamedClusters(store)` → clusters 中有 isNamed==false 者
  - `needsEdgeRebuild(store)` → clusters 中有 edgesBuilt==false 且 count ≥ 2
- [ ] **TO-2.2** 串行阶段 1: if 有未处理 → `ClusterDetector.detectClusters(store, cfg)`
- [ ] **TO-2.3** 串行阶段 2: if 有未命名 → `NameGenerator.generateNames(store, biomeResolver, cfg)`
- [ ] **TO-2.4** 串行阶段 3: if 需建边 → `RegionalGraph.buildRegionalGraph(store, cfg)` — 可分帧
- [ ] **TO-2.5** 并行阶段: `CompletableFuture.allOf(MarketSimulator, LevelProgression)`
- [ ] **TO-2.6** 串行阶段 4: `CaravanSimulator.simulateCaravans(store, cfg)`
- [ ] **TO-2.7** `store.markDirty()`

### TO-3 异常处理

- [ ] **TO-3.1** 每个模块调用包裹独立 try-catch
- [ ] **TO-3.2** 异常 → log (级别按 §11.3 分类) + 跳过该模块 + 继续下一个
- [ ] **TO-3.3** KCenterGenerator 异常 → log FATAL + 回退 (抛 Runtime 让 Adapter 感知)

### TO-4 性能监控

- [ ] **TO-4.1** 每个模块调用记录 `System.nanoTime()` 耗时
- [ ] **TO-4.2** 维护过去 10 次滑动窗口
- [ ] **TO-4.3** 连续 3 次超预算 → 触发降级策略 (按 §13.3)

### TO-5 公开 API

- [ ] **TO-5.1** `onWorldCreate(long seed, int rangeX, int rangeZ, VillageStateStore store, ModConfig cfg)`
- [ ] **TO-5.2** `runDailyCycle(VillageStateStore store, ModConfig cfg, BiomeResolver biomeResolver)`

### TO-6 单元测试

- [ ] **TO-6.1** onWorldCreate: villages 写入 store
- [ ] **TO-6.2** runDailyCycle: 顺序正确 (mock 模块, 验证调用顺序)
- [ ] **TO-6.3** 某模块抛异常 → 后续模块仍执行
- [ ] **TO-6.4** MarketSimulator + LevelProgression 在不同线程执行

---

## 完成标准

- [ ] 世界创建流程完整
- [ ] Daily Tick 串行/并行调度正确
- [ ] 异常隔离: 单模块失败不崩溃
- [ ] 所有 TO-6 测试通过
