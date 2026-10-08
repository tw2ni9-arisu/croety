# Croety 1.21.1 NeoForge — AI 协作指南

> 本文件只保留每次工作都必须知道的项目约定；NeoForge 1.21.1 的接口和迁移细节见 [`docs/ai/neoforge-1.21.1.md`](docs/ai/neoforge-1.21.1.md)。

## 必须遵守

1. **API 不可凭记忆猜。** 先看与目标版本匹配的官方 NeoForge 文档、官方 MDK 和本仓库的对应依赖源/制品。若源码或签名尚未就绪，明确标记待核验，不用旧版本示例补空白。
2. **改代码后必须验证并报告证据。** 至少运行 `gradlew build`；涉及运行时行为时再运行相应客户端、服务端或 GameTest。没有运行就不要写“已编译”或“已验证”。
3. **注释和文档用中文，代码标识符用英文。** 改动只覆盖用户要求的范围。

## 项目与迁移目标

- Croety 是 Create × Goety 联动 mod，modid `croety`，主包 `com.croety`。
- 目标环境：Minecraft 1.21.1、NeoForge 21.1.234、Java 21；Create 6.0.10-281、Goety 3.2.0（用户原写作 3.2.00）。
- `D:\Develop\croety-1.21.1-neoforge` 是迁移工作区。源码和构建已适配新平台；构建、游戏测试、专用服务端与用户客户端验收分别需要记录，不能把编译通过当作全部完成。
- 依赖来源、commit/hash 与下载坐标见 [`reference/SOURCES.md`](reference/SOURCES.md)，现有功能和准备证据见 [`docs/migration-preparation.md`](docs/migration-preparation.md)。
- 本次分工：主 agent 负责规划、核心接口/代码和调试；基础查找、仓库拉取与基础代码由 GPT-6-luna（max）处理；审查使用 GPT-6-sol（high）。
- 不扩充初版占位需求；现有功能、数值、注册 ID 和 42 项 GameTest 都要迁移。模型和贴图直接复用。

## 常用命令

```powershell
cd D:\Develop\croety-1.21.1-neoforge
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runGameTestServer
.\gradlew.bat runData
```

NeoForge 1.21.1 使用 Java 21。不要把机器专属 JDK 路径写进仓库配置。此机仅在新项目的 `.local/jdk21/jdk-21.0.12.1+1` 准备了 JDK 21，尚未更改全局 JAVA_HOME；执行 Gradle 前在当前进程选择 Java 21，避免默认 Java 17/25。运行服务器前按运行目录的 EULA 处理。

实际启动客户端供用户验收时，打开游戏、提供测试清单和回复格式，然后暂停工作等待用户结果；不能使用 Computer Use 代替用户操作或验收。

## 资料与 API 查询

- NeoForge 1.21.1 通用迁移和接口：[`docs/ai/neoforge-1.21.1.md`](docs/ai/neoforge-1.21.1.md)。
- Create 官方源码 checkout：`reference/create/`，标签 `mc1.21.1-6.0.10`。
- Curios 1.21.1 官方源码 checkout：`reference/curios/`。
- Patchouli 官方源码 checkout：`reference/patchouli/`，标签 `release-1.21.1-93`。
- Goety 3.2.0 精确发行制品：`reference/artifacts/goety-3.2.0.jar`；授权维护仓库为 [Vivideru/Goety-3](https://github.com/Vivideru/Goety-3)，源码在 `reference/goety/src/main/java/`，锁定提交 `2f42435123d2e781107f41a14dadd0d59cbd64ea`。深入接口见 [`docs/ai/goety-3.2.0.md`](docs/ai/goety-3.2.0.md)。用户明确要求不再反编译。
- `gradlew writeApiClasspath` 生成真实 `build/api-classpath.txt`（含 NeoForge/Minecraft 生成产物）。`tools/find-api.ps1` 查询此清单；`-Source` 优先读锁定的 reference 源码，再读同版本 sources JAR，不反编译 Goety。清单缺文件时重跑上述任务，不能查旧 Forge 缓存补空白。
- `docs/ai/forge-1.20.1.md`、`docs/ai/create-6.0.8.md`、`docs/ai/goety-2.5.57.3.md` 和 `docs/ai/flywheel-ponder.md` 只可作旧版本背景资料；接口签名不能直接照搬。

## 固定依赖

| 组件 | 版本/开发坐标 |
|---|---|
| Minecraft / NeoForge / Java | 1.21.1 / 21.1.234 / 21 |
| ModDevGradle / Parchment | 2.0.107 / 1.21.1-2024.11.17 |
| Create | `com.simibubi.create:create-1.21.1:6.0.10-281:slim` |
| Flywheel | `dev.engine-room.flywheel:flywheel-neoforge-api-1.21.1:1.0.6` 编译；完整 NeoForge artifact 运行 |
| Vanillin（与 Create 开发运行依赖对齐） | `dev.engine-room.vanillin:vanillin-neoforge-1.21.1:1.1.3-41` 仅运行 |
| Ponder（含 Catnip） | `net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1` |
| Registrate | `com.tterrag.registrate:Registrate:MC1.21-1.3.0+67` |
| Goety | 3.2.0，官方发行 JAR + 本地 Maven，来源和 hash 见 SOURCES |
| Curios | `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1` |
| Patchouli | `vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE` |

使用 Create slim 时显式提供匹配的前置，不将其 POM 中的浮动范围解析成未经核对的新版本。Goety 源码和 JAR 的版本相符，但没有 tag/制品 Git hash 证明精确二进制来源；方法签名同时查发行 JAR。原始依赖 JAR 不静默改包。

## 本项目的新版标准接口

以下已对照官方 1.21.1 文档、目标仓库或真实制品签名，尚不表示 Croety 迁移示例已经编译。详细源码路径见两个新版 API 指南。

1. 入口注入 `net.neoforged.bus.api.IEventBus` / `net.neoforged.fml.ModContainer`。注册和生命周期使用 mod bus，游戏 tick 使用 `NeoForge.EVENT_BUS`；服务端末尾 tick 类型为 `ServerTickEvent.Post`。
2. 用 `net.neoforged.neoforge.registries.DeferredRegister` / `DeferredHolder`，vanilla registry key 来自 `Registries`，流体类型 key 是 `NeoForgeRegistries.Keys.FLUID_TYPES`。`DeferredRegister.createBlocks/createItems` 分别返回特化注册器，条目为 `DeferredBlock/DeferredItem`。
3. ResourceLocation 不再直接 new：`fromNamespaceAndPath(namespace, path)`、`parse(id)`、`withDefaultNamespace(path)`。
4. Create 的持久化钩子为 `read/write(CompoundTag, HolderLookup.Provider, boolean)`。SavedData 使用 `SavedData.Factory`，`load/save` 同样带 registry provider；保留 `setDirty()` 与主世界跨维度索引。NeoForge 的 SavedData 保存为异步写盘，测试重载前需 `IOUtilities.waitUntilIOWorkerComplete()`；不要把测试同步要求写进生产保存逻辑。
5. SoulOrb 直接继承 Entity，`defineSynchedData(SynchedEntityData.Builder)` 内调用 `builder.define`，不调用抽象的父类方法。生成包签名带 `ServerEntity`，自定义 VALUE 继续由实体同步数据同步。
6. 流体用 `BaseFlowingFluid`。方块和物品能力在 `RegisterCapabilitiesEvent` 注册；方块查询用 `level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)`，无能力是 null。不使用旧 LazyOptional/AttachCapabilitiesEvent。
7. `BucketItem` 子类不继承 NeoForge 为精确桶类提供的默认能力，必须给 `SoulBucketItem` 注册 `Capabilities.FluidHandler.ITEM` / `FluidBucketWrapper`。客户端流体扩展用 `RegisterClientExtensionsEvent` 并保持客户端隔离。
8. Goety `ITotem.tag(stack)` 是 CUSTOM_DATA 的副本，写入用 `updateTag` 或精确方法 `setSoulsamount`（a 小写）。ITotem 不再继承 ISoulContainer。图腾转成耗尽物品时要保留其它数据组件，不能只复制旧 NBT。
9. Goety `Spell.acceptedEnchantments()` 返回 `List<ResourceKey<Enchantment>>`；`ArcaBlockEntity.getPlayer()` 从 OwnedBlockEntity 继承。接收能力的真实类型来自 `ModBlockEntities.ARCA/CURSED_CAGE`。
10. 数据包目录为 `recipe`、`tags/item`、`structure`；资源包 format 为 34。ItemStack 配方输出按新 codec 使用 `id`，流体 JSON 按 Create 6.0.10 serializer 核对。
11. 复用模型时仍须迁移加载元数据：元素 `forge_data` → `neoforge_data`，桶 parent/loader 使用 `neoforge:item/bucket` / `neoforge:fluid_container`。PNG、几何、UV与变换保留。实际客户端已证明旧字段会让模型解析失败，不能只用服务端或JSON语法检查替代模型加载验证。
12. SoulOrb沿用本版本ExperienceOrbRenderer的顶点与Cull类型，cameraOrientation后不附加旧版180°Y旋转，否则正面会翻到背面被剔除；颜色与11档UV仍保留。

Create 仍是旋转机械/应力系统，使用 RPM、SU、应力容量、动力源等术语。Addon 应力用 `BlockStressValues.CAPACITIES/IMPACTS`，不能用仅支持 Create 自有方块的 CStress。马达初始化仍要通知动力网络；保留 128 RPM × 64 SU/RPM。

## 执行与验收原则

- 先说明假设和成功条件；不猜依赖 API，存在关键歧义时先核查并询问。
- 只做必要迁移，不重构相邻业务，不写无需求的兼容层。
- 从基线、失败证据到修复后证据：构建必须 `BUILD SUCCESSFUL`，原 42 项及新增必要边界 GameTest 全部执行通过；服务端出现 `Done (...)!` 且无 Mixin 失败；客户端加载证据和用户实际反馈独立记录。
- PNG/模型内容与旧项目逐文件比较；新平台测试结果不能用旧版日志代替。
- 完成后保留依赖参考仓库、正式源码资源、查询/获取工具、构建产物与验证记录；临时文件放忽略的 `.local`，不把 nested repo/JAR/JDK/run 存档提交进 Git。
