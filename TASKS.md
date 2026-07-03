# LivingVillages Biome Regions — 模块任务清单

> 配合 `DESIGN.md` 使用。本文件列出所有待完成模块，主 agent 串行启动子 agent 完成，每个模块审核通过后才能标注 done。

## 工作流

1. 主 agent 按模块顺序（M1 → M9）启动子 agent
2. 子 agent 完成模块后，主 agent 审核：
   - 能编译
   - unit tests 通过（纯逻辑模块必须有测试）
   - 符合 DESIGN.md 描述
   - 代码质量审查（命名/结构/无冗余）
3. 审核通过 → 主 agent 在本文件标注 `[x]` + 日期
4. 审核不通过 → 主 agent 反馈问题，子 agent 修复后重审
5. 全部模块 done → 整体完成度 100%

## 完成度

- 已完成：5 / 9 模块
- 进度：56%

---

## 模块清单

### M1: 自定义 StructureSet（额外村庄）

- **状态**：`[x]` done — 2026-07-03 / commit pending
- **依赖**：无
- **子 agent 任务**：
  - 创建 `src/main/resources/data/livingvillages/worldgen/structure_set/extra_village.json`
  - 内容：spacing=13, separation=4, salt=5803128, 5 种 village structure（plains/desert/savanna/snowy/taiga）
  - 验证 JSON 格式符合 1.20.1 vanilla structure_set 规范
- **验收标准**：
  - 文件存在且 JSON 语法正确
  - 字段值与 DESIGN.md 一致（spacing=13, salt=5803128）
  - 5 种 village structure 都列出
- **unit tests**：无（纯数据文件）
- **风险**：低

---

### M2: BiomeRegionResolver（biome → 区位类型）

- **状态**：`[x]` done — 2026-07-03 / commit pending
- **依赖**：无
- **子 agent 任务**：
  - 写 `RegionType` 枚举（DESERT/SNOWY/PLAINS/TAIGA/SAVANNA/SWAMP/JUNGLE/MOUNTAIN/FOREST/OTHER）
  - 写 `BiomeRegionResolver` 类，方法 `resolveRegionType(Holder<Biome>) → RegionType`（返回 null 表示排除）
  - 排除逻辑：cave biomes（手动列表 lush_caves/dripstone_caves/deep_dark）、water（is_ocean/is_river）、beach（is_beach）
  - 归类逻辑：按 DESIGN.md 优先级（mountain → snowy → desert → swamp → jungle → taiga → savanna → forest → plains）
  - 提供 server 版本（接 ServerLevel）和 client 版本（接 Level）
- **验收标准**：
  - 编译通过
  - 10 种区位类型枚举完整
  - 排除逻辑正确（cave/water/beach 返回 null）
  - 归类优先级符合 DESIGN.md
- **unit tests**：**必须有**（测纯逻辑）
  - 测每个区位类型的判定（mock Holder<Biome>）
  - 测排除逻辑（cave/water/beach 返回 null）
- **风险**：低

---

### M3: RegionNamePool（10 种区位命名池）

- **状态**：`[x]` done — 2026-07-03 / commit pending
- **依赖**：M2（RegionType 枚举）
- **子 agent 任务**：
  - 写 `RegionNamePool` 类，含 `RegionWords` record（prefixes/midfix/suffixes）
  - 10 种区位类型的命名池（DESIGN.md 提供的前缀/中缀/后缀）
  - 通用村庄后缀列表（村/镇/集/庄/堡）
- **验收标准**：
  - 编译通过
  - 10 种区位池都定义
  - 每个池有 prefixes（≥8 个）+ midfix + suffixes（≥3 个）
  - 村庄后缀列表完整
- **unit tests**：**必须有**（测纯数据）
  - 测每种区位池非空
  - 测村庄后缀列表非空
- **风险**：低

---

### M4: RegionNameGenerator（确定性命名）

- **状态**：`[x]` done — 2026-07-03 / commit pending
- **依赖**：M2（RegionType）+ M3（RegionNamePool）
- **子 agent 任务**：
  - 写 `RegionNameGenerator` 类
  - 方法 `regionName(worldSeed, superX, superZ, type) → String`
  - 方法 `villageName(worldSeed, chunkX, chunkZ, type) → String`
  - 方法 `fullDisplay(regionName, villageName) → String`（含 "——" 分隔符）
  - seed 哈希公式：`worldSeed ^ (coord * 341873128712L) ^ (coord * 132897987541L)`
- **验收标准**：
  - 编译通过
  - 确定性：同 seed + 同坐标 → 同名（测试验证）
  - 不同 seed 或不同坐标 → 不同名（测试验证）
  - 输出格式符合 DESIGN.md（如 "风沙之地"、"赤焰村"、"风沙之地——赤焰村"）
- **unit tests**：**必须有**（测纯函数）
  - 测确定性（同输入同名）
  - 测不同输入产生不同输出
  - 测 fullDisplay 格式
- **风险**：低

---

### M5: RegionStateStore（SavedData 持久化）

- **状态**：`[x]` done — 2026-07-03 / commit pending
- **依赖**：无
- **子 agent 任务**：
  - 写 `RegionStateStore extends SavedData`
  - 存已处理村庄 chunkKey 集合（Set<Long>）
  - 方法：`load(ServerLevel)`、`save(CompoundTag, Provider)`、`isProcessed(chunkX, chunkZ)`、`markProcessed(chunkX, chunkZ)`
  - DATA_NAME = "livingvillages_regions"
- **验收标准**：
  - 编译通过
  - NBT 序列化/反序列化正确（round-trip 测试）
  - isProcessed/markProcessed 行为正确
- **unit tests**：**必须有**（测序列化）
  - 测 markProcessed 后 isProcessed 返回 true
  - 测 save → load round-trip 数据一致
- **风险**：低

---

### M6: RegionTitleDisplay（客户端飘字）

- **状态**：`[ ]` pending
- **依赖**：M2（BiomeRegionResolver）+ M4（RegionNameGenerator）
- **子 agent 任务**：
  - 写 `RegionTitleDisplay` 实现 `ClientTickEvents.EndClientTick`
  - 节流：每 10 tick 检查一次
  - 用 player 位置算 superX/superZ + 区位类型 + 区位名
  - 调 `player.displayClientMessage(Component.literal(text), true)` 显示在 action bar
  - 只在 region 变化时触发（避免每帧刷新）
  - **先实现只飘区位名**（不查村庄），村庄名留 M6b 或合并到 M6
- **验收标准**：
  - 编译通过
  - 走进不同 biome 飘对应区位名
  - 走进 cave/water/beach 不飘字
  - 节流正确（不每帧调用）
- **unit tests**：无（调 MC API，难测）
- **风险**：中（客户端 API 调用时机）

---

### M7: RegionHudRenderer（屏幕角落 HUD）

- **状态**：`[ ]` pending
- **依赖**：M2 + M4
- **子 agent 任务**：
  - 写 `RegionHudRenderer` 实现 `HudRenderCallback`
  - 屏幕右上角小字显示当前区位名
  - 用 `GuiGraphics.drawString` + `Font`
- **验收标准**：
  - 编译通过
  - 屏幕右上角显示区位名
  - cave/water/beach 不显示
- **unit tests**：无（GUI 渲染，难测）
- **风险**：低

---

### M8: RoadWeaverIntegrator（server 端村庄注册 + 连接）

- **状态**：`[ ]` pending
- **依赖**：M5（RegionStateStore）+ RoadWeaver API
- **子 agent 任务**：
  - 写 `RoadWeaverIntegrator` 类
  - 节流：每秒一次
  - 扫描玩家附近 500 格内的新村庄（StructureManager 查 StructureStart）
  - 对每个未处理村庄：
    1. `RoadNetworkApi.registerStructureEndpoint(level, villagePos, "village", true)`
    2. 找最近的 1-2 个已处理村庄，调 `ensureConnection`
    3. `markProcessed` 存到 RegionStateStore
- **验收标准**：
  - 编译通过
  - 不崩（RoadWeaver API 调用 try-catch）
  - 村庄间生成 RoadWeaver 道路
- **unit tests**：无（依赖 MC + RoadWeaver 运行时）
- **风险**：中（StructureManager 查询 + RoadWeaver API 兼容）

---

### M9: RoadMapScreenMixin（H 键地图 UI 标注）

- **状态**：`[ ]` pending
- **依赖**：M2 + M4 + RoadWeaver RoadMapScreen 类
- **子 agent 任务**：
  - 写 `RoadMapScreenMixin`，`@Mixin(RoadMapScreen.class)`
  - `@Inject(method="render", at=@At("RETURN"))` 注入区位标注
  - 地图上每个可见 super-cell 区域叠加区位名（半透明）
  - 玩家所在 super-cell 高亮（屏幕顶部大字显示当前区位名）
  - 更新 `livingvillages.mixins.json` 加入 RoadMapScreenMixin
  - 更新 `fabric.mod.json` 加 mixins 引用 + depends roadweaver
- **验收标准**：
  - 编译通过
  - Mixin 应用成功（游戏不崩）
  - 按 H 打开地图看到区位名标注
  - 玩家区位高亮显示
- **unit tests**：无（Mixin GUI，难测）
- **风险**：高（Mixin RoadWeaver 类，需访问私有字段 viewport）

---

## 模块依赖图

```
M1 (StructureSet)         独立
M2 (BiomeResolver)        独立
M3 (NamePool)              依赖 M2
M4 (NameGenerator)         依赖 M2, M3
M5 (StateStore)            独立
M6 (TitleDisplay)          依赖 M2, M4
M7 (HudRenderer)           依赖 M2, M4
M8 (RoadWeaverIntegrator)  依赖 M5
M9 (RoadMapScreenMixin)    依赖 M2, M4
```

## 串行执行顺序

按依赖拓扑排序：M1 / M2 / M5（独立）→ M3 → M4 → M6 / M7 / M8 / M9

推荐顺序：M1 → M2 → M3 → M4 → M5 → M6 → M7 → M8 → M9

## 完成记录

（主 agent 审核通过后填写）

- [x] M1 — 2026-07-03 / commit pending
- [x] M2 — 2026-07-03 / commit pending
- [x] M3 — 2026-07-03 / commit pending
- [x] M4 — 2026-07-03 / commit pending
- [x] M5 — 2026-07-03 / commit pending
- [ ] M6 — 日期 / commit SHA
- [ ] M7 — 日期 / commit SHA
- [ ] M8 — 日期 / commit SHA
- [ ] M9 — 日期 / commit SHA
