# NameGenerator — Minimal Workable Plan

> 前置: data-layer, config, cluster-detector  
> 产出: `core/naming/` 包 (BiomeResolver, NameGenerator, NamePool)  
> 写入字段: `clusterNames[]`, `villageNames[]`  
> 读: `clusters[]`, `villages[]`

---

## 任务清单

### N-1 BiomeResolver 接口

- [ ] **N-1.1** 创建 `BiomeResolver` — `@FunctionalInterface`, method: `String getBiomeCategory(int x, int y, int z)`

### N-2 NamePool 词库

- [ ] **N-2.1** 创建 `NamePool` 类 — 内部 record: `BiomeWordPool { List<String> prefixes, midfixes, suffixes }`
- [ ] **N-2.2** 为 7 个 biome category 各准备 ≥ 20 词:
  - plains: 晴麦风原野田云暖丰金绿广平明和…
  - desert: 金沙炎驼漠泉烈阳荒旱焰赤烁…
  - taiga: 松雪桦冷涛岭霜寒冰杉峻…
  - snowy: 冰霜白晶冬银寒素净莹…
  - savanna: 赤日狮金旱稀热烈荒…
  - swamp: 沼雾藤暗水影幽静深…
  - jungle: 密翠藤绿深莽繁茂湿…

### N-3 确定性命名

- [ ] **N-3.1** `hash(x, z)` → long seed — 确定性映射, e.g. `Objects.hash(x, z)` 或 `Long.hashCode(x ^ (z * 31L))`
- [ ] **N-3.2** 从 seed 初始化 PRNG → 按词库随机选取
- [ ] **N-3.3** 生成规则:
  - `clusterName = prefix[2字] + mid[1字] + suffix[1字]` → 4字
  - `centerTownName = clusterName + "镇"`
  - `satelliteName = prefix[2字] + suffix[1字] + "村"` → 4字
- [ ] **N-3.4** 卫星村去重: 聚落内 satelliteNames 不重复

### N-4 公开 API

- [ ] **N-4.1** `generateNames(VillageStateStore store, BiomeResolver biomeResolver, ModConfig cfg)` — 主入口
- [ ] **N-4.2** 仅处理 `isNamed==false` 的 cluster
- [ ] **N-4.3** 命名完成后标记 `isNamed=true` 并写回 `clusters[]`

### N-5 单元测试

- [ ] **N-5.1** 同名坐标 → 同名 (确定性)
- [ ] **N-5.2** 不同坐标 → 不同名 (≥95% 概率)
- [ ] **N-5.3** clusterName 长度 4, centerTownName 以 "镇" 结尾
- [ ] **N-5.4** satelliteName 以 "村" 结尾
- [ ] **N-5.5** desert biomeResolver → 名称来自沙漠词库
- [ ] **N-5.6** 幂等: 重复调用不产生新名

---

## 完成标准

- [ ] 7 套词库覆盖所有 biome category
- [ ] 命名确定性: 同坐标永远同名
- [ ] 命名质量: 无不良语义组合 (人工抽查 20 个)
- [ ] 所有 N-5 测试通过
