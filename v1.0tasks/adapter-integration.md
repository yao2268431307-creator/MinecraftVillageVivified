# Adapter Integration — Minimal Workable Plan

> 前置: 所有模块  
> 产出: `adapter/` 包下全部 MC 胶水代码  
> 包含: LivingVillagesMod, NbtVillageStateStore, BiomeResolverImpl, ConfigLoader, WorldGen/Chunk/Tick hooks

---

## 任务清单

### A-1 NbtVillageStateStore

- [ ] **A-1.1** 创建 `NbtVillageStateStore extends SavedData implements VillageStateStore`
- [ ] **A-1.2** 实现所有 11 字段的 NBT 序列化 — `save(CompoundTag)`:
  - villages/clusters/caravans/edges → `ListTag<CompoundTag>`
  - prices/warehouses/accumulatedConsumption → `CompoundTag` of `CompoundTag`s
  - levels/specialities → `CompoundTag<UUID→int/String>`
- [ ] **A-1.3** 实现反序列化 `load(CompoundTag)`
- [ ] **A-1.4** `markDirty()` → `setDirty()` (Minecraft SavedData 机制)

### A-2 BiomeResolverImpl

- [ ] **A-2.1** 实现 `BiomeResolver` — `getBiomeCategory(x, y, z)`:
  - 从 ServerLevel 获取 biome → 映射到 7 category + "other"
  - 映射表: `Biomes.PLAINS` → "plains", `Biomes.DESERT` → "desert", etc.

### A-3 ConfigLoader

- [ ] **A-3.1** 检查 `config/livingvillages.toml` 是否存在
- [ ] **A-3.2** 不存在 → 用 `ModConfig.defaults()` 生成 TOML (含注释)
- [ ] **A-3.3** 存在 → 读取 TOML → 构造 `ModConfig` → range 验证
- [ ] **A-3.4** 缓存 ModConfig 单例

### A-4 WorldGen Hook

- [ ] **A-4.1** 在 `WorldGenEvents` / `RegisterStructurePiecesEvent` 中注入自定义 StructurePlacement
- [ ] **A-4.2** 禁用原版 `minecraft:village_*` placement
- [ ] **A-4.3** 世界创建时调用 `TickOrchestrator.onWorldCreate()`

### A-5 Chunk Load Hook

- [ ] **A-5.1** 监听 `ServerChunkEvents.CHUNK_LOAD`
- [ ] **A-5.2** 检查 chunk 内是否有 `placed==false` 的 VillageRecord
- [ ] **A-5.3** 调用 `StructureTemplate.placeInWorld()` 放置结构
- [ ] **A-5.4** 填充 biomeCategory, bedCount, 更新 Y 坐标, `placed=true`

### A-6 Daily Tick Hook

- [ ] **A-6.1** 监听 `ServerLevelEvents.TICK`
- [ ] **A-6.2** `worldTime % dailyTickInterval == 0` → 调用:
  1. `TickOrchestrator.runDailyCycle(store, cfg, biomeResolver)`
  2. `RailPlacer.placeRails(level, store, cfg, 50)`
  3. `CaravanEntityManager.syncEntities(level, store)`

### A-7 Save/Unload Hooks

- [ ] **A-7.1** `ServerLevelEvents.SAVE` → 若 dirty → NBT 自动保存
- [ ] **A-7.2** `ServerLevelEvents.UNLOAD` → `CaravanEntityManager.removeAllCaravanEntities()`

### A-8 LivingVillagesMod 主入口

- [ ] **A-8.1** `@ExpectPlatform` 声明
- [ ] **A-8.2** 初始化顺序: ConfigLoader → NbtVillageStateStore → 注册所有 hooks
- [ ] **A-8.3** 用户禁用 mod → 恢复原版村庄生成

---

## 完成标准

- [ ] NBT 序列化往返正确 (write→read→write 等价)
- [ ] TOML 配置读取/生成正常
- [ ] WorldGen 接管成功: 原版村庄不生成, Living Village 结构正确放置
- [ ] Daily Tick 完整链路跑通
- [ ] 世界退出无残留实体
