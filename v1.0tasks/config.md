# Config — Minimal Workable Plan

> 前置: data-layer  
> 产出: `core/config/ModConfig.java` + `@Range` 注解  
> 依赖本模块的模块: 全部

---

## 任务清单

### C-1 @Range 注解

- [ ] **C-1.1** 创建 `@Range` 注解 — `double min(); double max();`，RetentionPolicy.RUNTIME

### C-2 ModConfig Record

- [ ] **C-2.1** 创建 `ModConfig` record — 13 个字段，全部带 `@Range`（minPts 除外）
  - kcenter: `k(8)`, `rCluster(3200)`, `minSeparation(96)`
  - cluster: `eps(250)`, `minPts(1)`
  - graph: `knnK(2)`, `maxSlope(1.0)`, `tunnelThreshold(6)`
  - economy: `cesElasticity(0.7)`, `diffusionRate(0.15)`, `dailyTickInterval(24000)`
  - caravan: `fuelTicks(1200)`, `maxActiveCaravans(50)`
- [ ] **C-2.2** 实现 `defaults()` 静态工厂 — 返回默认值实例
- [ ] **C-2.3** compact constructor 中验证 range: 任一项超界 → `throw IllegalArgumentException`

### C-3 TOML Schema 文档

- [ ] **C-3.1** 在 Adapter 层创建 `config/livingvillages.toml` 模板文件（含注释）

### C-4 单元测试

- [ ] **C-4.1** `ModConfigTest` — `defaults()` 构造不抛异常
- [ ] **C-4.2** `ModConfigTest` — 非法值抛 `IllegalArgumentException`

---

## 完成标准

- [ ] ModConfig 编译通过，所有 @Range 合法
- [ ] defaults() 返回的实例可通过所有 range 验证
- [ ] 非法值构造立即失败
- [ ] TOML 模板文件包含所有参数及中文注释
