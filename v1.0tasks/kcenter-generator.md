# KCenterGenerator — Minimal Workable Plan

> 前置: data-layer, config  
> 产出: `core/villagegen/KCenterGenerator.java`  
> 写入字段: `villages[]`

---

## 任务清单

### K-1 确定性 PRNG

- [ ] **K-1.1** 封装 `seed` → `Random` 的确定性初始化 — `new Random(seed)` 或 `SplittableRandom`

### K-2 K-Center 初始化

- [ ] **K-2.1** 实现贪心 K-Center: 第一点随机，后续从 200 点候选池中选 `max(minDist)` 者
- [ ] **K-2.2** 验证: K 个中心间距应接近 `(2*range)/√K`
- [ ] **K-2.3** 输出 `List<Vec3i> centers`

### K-3 Poisson Disk Sampling

- [ ] **K-3.1** 实现 Bridson 算法: 以 center 为中心、rCluster 为半径、minSeparation 为最小间距
- [ ] **K-3.2** 每个候选最多尝试 30 次；上限 `ceil(π·R² / minSep²)` 个点
- [ ] **K-3.3** 对每个采样点构造 `VillageRecord`:
  - id = `UUID.nameUUIDFromBytes(("livingvillage:" + seed + ":" + globalIndex).getBytes())`
  - position = `Vec3i(x, 0, z)` (Y 占位)
  - biomeCategory = `"unresolved"`
  - bedCount = 0, placed = false

### K-4 公开 API

- [ ] **K-4.1** `generateKCenters(long seed, int rangeX, int rangeZ, ModConfig cfg)` → `List<VillageRecord>`
- [ ] **K-4.2** `expandKCenters(long seed, List<VillageRecord> existing, int newMinX, int newMaxX, int newMinZ, int newMaxZ, ModConfig cfg)` → `List<VillageRecord>`
  - 过滤与 existing 距离 < minSeparation 的点

### K-5 单元测试

- [ ] **K-5.1** 确定性: `seed=42` 两次调用 → 输出逐位相等
- [ ] **K-5.2** 所有 VillageRecord.id 唯一
- [ ] **K-5.3** 聚落内任意两村水平间距 ≥ minSeparation
- [ ] **K-5.4** 所有村庄距其最近聚落中心 ≤ rCluster
- [ ] **K-5.5** expandKCenters: 新点不与 existing 重叠

---

## 完成标准

- [ ] generateKCenters 返回非空列表，确定性可复现
- [ ] Poisson 采样满足间距约束
- [ ] expandKCenters 正确避让已有村庄
- [ ] 所有 K-5 测试通过
