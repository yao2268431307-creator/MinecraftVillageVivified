# Living Villages — 全局进度追踪 v1.0

> **目标**: 按详细设计 v1.0 实现零耦合架构的 Living Villages mod  
> **模块数**: 13 个任务文件  
> **总子任务**: ~180 项  
> **更新规则**: 每完成一个 checkbox → 在此文件中同步勾选

---

## 整体进度

| # | 模块 | 任务文件 | 子任务 | 完成 | 进度 | 状态 |
|---|------|---------|:---:|:---:|:---:|:---:|
| 1 | Data Layer | [data-layer.md](data-layer.md) | 12 | 12 | 100% | ✅ |
| 2 | Config | [config.md](config.md) | 6 | 6 | 100% | ✅ |
| 3 | KCenterGenerator | [kcenter-generator.md](kcenter-generator.md) | 14 | 14 | 100% | ✅ |
| 4 | ClusterDetector | [cluster-detector.md](cluster-detector.md) | 12 | 12 | 100% | ✅ |
| 5 | NameGenerator | [name-generator.md](name-generator.md) | 16 | 16 | 100% | ✅ |
| 6 | RegionalGraph | [regional-graph.md](regional-graph.md) | 18 | 18 | 100% | ✅ |
| 7 | RailPlacer | [rail-placer.md](rail-placer.md) | 14 | 14 | 100% | ✅ |
| 8 | MarketSimulator | [market-simulator.md](market-simulator.md) | 15 | 15 | 100% | ✅ |
| 9 | CaravanSimulator | [caravan-simulator.md](caravan-simulator.md) | 16 | 16 | 100% | ✅ |
| 10 | CaravanEntityManager | [caravan-entity-manager.md](caravan-entity-manager.md) | 9 | 9 | 100% | ✅ |
| 11 | LevelProgression | [level-progression.md](level-progression.md) | 11 | 11 | 100% | ✅ |
| 12 | TickOrchestrator | [tick-orchestrator.md](tick-orchestrator.md) | 18 | 18 | 100% | ✅ |
| 13 | Adapter Integration | [adapter-integration.md](adapter-integration.md) | 18 | 18 | 100% | ✅ |

> 状态: ⬜ 未开始 | 🔵 进行中 | ✅ 已完成 | ⏸️ 阻塞

---

## 依赖关系与推荐顺序

```
Phase 0 — 基础设施 (可并行)
  ┌── data-layer      (0 依赖)
  └── config           (依赖 data-layer)

Phase 1 — Core 独立模块 (Phase 0 完成后可并行)
  ├── kcenter-generator  (依赖 data-layer, config)
  ├── cluster-detector   (依赖 data-layer, config)
  └── level-progression  (依赖 data-layer)

Phase 2 — Core 依赖模块
  ├── name-generator     (依赖 Phase 1 cluster-detector)
  └── regional-graph     (依赖 Phase 1 cluster-detector)

Phase 3 — Core 经济/商队
  ├── market-simulator   (依赖 Phase 2 regional-graph, Phase 1 cluster-detector)
  └── caravan-simulator  (依赖 market-simulator)

Phase 4 — Adapter
  ├── rail-placer            (依赖 Phase 2 regional-graph)
  └── caravan-entity-manager (依赖 Phase 3 caravan-simulator)

Phase 5 — 集成
  ├── tick-orchestrator     (依赖 Phase 1-3 所有 Core 模块)
  └── adapter-integration    (依赖全部)
```

---

## Phase 0: 基础设施

- [x] **Data Layer** — 所有 Record + VillageStateStore 接口 + InMemory 实现
- [x] **Config** — ModConfig record + @Range + TOML 模板

## Phase 1: Core 独立模块

- [x] **KCenterGenerator** — generateKCenters + expandKCenters
- [x] **ClusterDetector** — DBSCAN + 空间索引
- [x] **LevelProgression** — 阈值判定 + 特产

## Phase 2: Core 依赖模块

- [x] **NameGenerator** — BiomeResolver + NamePool + 确定性命名
- [x] **RegionalGraph** — KNN + A* + Bezier

## Phase 3: Core 经济/商队

- [x] **MarketSimulator** — CES 定价 + 热扩散 + 缺粮惩罚
- [x] **CaravanSimulator** — 6 态状态机 + DES

## Phase 4: Adapter

- [x] **RailPlacer** — 铁轨/隧道/桥梁方块放置
- [x] **CaravanEntityManager** — 矿车实体 spawn/sync/despawn

## Phase 5: 集成

- [x] **TickOrchestrator** — 调度 + 异常处理 + 性能监控
- [x] **Adapter Integration** — NBT/TOML/WorldGen/Chunk/Tick hooks

---

## 全局验证 Checklist

以下验证项贯穿所有模块，在 Phase 5 集成完成后统一核实：

- [ ] **V1**: 每字段单一写入者 — 对照 §2.3 矩阵, 无违反
- [ ] **V2**: 模块零业务 import — 每个 Core 模块 import 列表审核
- [ ] **V3**: 独立可测试 — 6 个 Core 模块纯 JUnit 通过; Adapter 模块 GameTest 通过
- [ ] **V4**: 可删除 — 删除任一模块 (及字段), 其余编译通过
- [ ] **V5**: 算法可达 — 每模块输入覆盖算法全部需求

---

*最后更新: 2026-06-29 | 进度: 13/13 模块完成*
