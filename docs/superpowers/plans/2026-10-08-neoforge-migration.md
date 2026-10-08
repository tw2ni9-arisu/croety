# Croety 1.21.1 NeoForge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 用户已指定混合执行：主 agent 编写核心接口和逻辑，GPT-6-luna（max）编写基础部分，GPT-6-sol（high）审查；此分工优先于技能的默认模型路由。

**Goal:** 完整迁移现有 Croety 到 Minecraft 1.21.1 NeoForge / Java 21，产出通过自动与用户实机验收的 JAR。

**Architecture:** 保留现有业务类及注册 ID，适配加载器、能力、持久化、组件和渲染接口。以锁定的上游源码和实际发行签名为准；不增加跨加载器抽象或占位玩法。

**Tech Stack:** Java 21、Gradle 8.14.3、NeoForge 21.1.234、ModDevGradle 2.0.107、Parchment 1.21.1/2024.11.17、Create 6.0.10-281、Goety 3.2.0、Curios 9.5.1+1.21.1、Patchouli 1.21.1-93-NEOFORGE。

**Spec:** `docs/superpowers/specs/2026-10-08-neoforge-migration-design.md`

## Global Constraints

- 全部产品和文档改动位于 `D:\Develop\croety-1.21.1-neoforge`，分支 `croety-1.21.1-neoforge`；旧项目只作参考，不推送或发布。
- 保留 `croety:soul_motor`、`croety:waving_focus`、`croety:fluid_soul`、`croety:flowing_fluid_soul`、`croety:fluid_soul_bucket`、`croety:soul_energy_orb` 和现有数值行为。
- Goety 源码为用户提供的授权维护仓库 `Vivideru/Goety-3`，不再反编译；所有参考源码 commit 和制品 hash 见 `reference/SOURCES.md`。
- Create 前置与发行源码一致：Flywheel 1.0.6、Ponder 1.0.82+mc1.21.1、Registrate MC1.21-1.3.0+67；开发运行库 Vanillin 固定为 1.1.3-41。
- 正式 PNG、模型和方块状态内容保持基线；哈希快照在 `build/migration-baseline-assets.json`，也可直接与旧项目相同路径比较。
- 保留原 42 项 GameTest 的行为断言，不减少测试或放宽断言。新增测试只覆盖迁移带来的真实风险。
- 注释和文档用中文。不新增初版占位机器、Ponder 场景或无需求的数据生成器。
- 客户端验收需真实启动游戏，交给用户测试并暂停工作等待结果；不使用 Computer Use。

## Review Focus

1. 自定义桶子类缺流体能力：整桶往返必须通过 NeoForge capability 与 Create 实际分液池。
2. 图腾耗尽时丢失组件：CUSTOM_DATA 标记与 CUSTOM_NAME 在部分/耗尽转换后保留，只移除灵魂计数字段。
3. 持久化接口适配不完整：方块实体、SavedData 磁盘读取和跨维度淘汰必须在注册 lookup 下重载，不能只测内存对象。
4. 客户端类或 Mixin 错误加载：专用服务端能启动，客户端桶贴图/旋转轴/球生成及同步正常。
5. 配方目录或 codec 改动丢内容：新版运行的原生仪式和四条搅拌必须实际完成，不仅查到文件。

---

### Task 1: 构建、注册基础与资源格式

**负责人：** GPT-6-luna（max），主 agent 协调依赖解析与验证。

**Files:**
- Modify: `build.gradle`、`settings.gradle`、`gradle.properties`、`gradle/wrapper/gradle-wrapper.properties`，必要时官方 Gradle wrapper 文件。
- Modify: `tools/fetch-deps.ps1`、`tools/find-api.ps1`、`.github/workflows/build.yml`。
- Modify: `src/main/java/com/croety/Croety.java`、`content/DemoTab.java`、`content/orb/OrbContent.java`、`integration/CreateIntegration.java`、`integration/GoetyIntegration.java`。
- Replace: `src/main/resources/META-INF/mods.toml` → `META-INF/neoforge.mods.toml`。
- Modify: `src/main/resources/pack.mcmeta`、`croety.mixins.json`。
- Move/adapt: `data/croety/recipes/` → `recipe/`、`tags/items/` → `tags/item/`、`structures/empty.nbt` → `structure/empty.nbt`。
- Test: 由主 agent 运行 Gradle 依赖解析和后续全套 GameTest；本任务不擅自修改马达、流体及测试业务逻辑。

**Interfaces:**
- Consumes: `reference/{create,goety,curios,patchouli}` 和 `reference/SOURCES.md` 的精确版本。
- Produces: Java 21 `build`、`runClient`、`runServer`、`runGameTestServer`；`Croety(IEventBus, ModContainer)`；现有内容静态注册方法继续接收 NeoForge `IEventBus`，注册字段仍可 `.get()`。
- Produces: `writeApiClasspath` Gradle task，将实际 `sourceSets.main.compileClasspath` 写到 `build/api-classpath.txt` 并强制解析，用于 `tools/find-api.ps1`，避免递归猜缓存 JAR。

- [ ] 先换用官方 ModDevGradle 结构和 Java 21，Gradle 采用 Create 源码的 8.14.3，固定全部版本；Goety 获取脚本下载精确官方发行 URL 并校验 hash，在本地 Maven 中提供可重建制品，不依赖 `reference` 中的临时 JAR 运行。
- [ ] 在业务源码尚未改动时运行新环境 `compileJava`，记录旧 Forge/1.20.1 API 的失败，作为平台迁移前证据；依赖下载错误单独处理，不能当作代码不兼容证明。
- [ ] 移植本任务拥有的入口、注册和客户端隔离基础；Mixin compatibility 改为 JAVA_21，在 NeoForge 元数据声明配置，去掉旧 Forge SRG refmap 设置。
- [ ] 迁移数据目录及目标 codec：ItemStack 输出使用 `id`；Create 流体 ingredient/output 格式直接按 6.0.10 serializers 与其生成配方核对。数量、加热和仪式成本不变。
- [ ] 执行 `gradlew tasks --all` 与 `gradlew writeApiClasspath`，确认四个要求的 run tasks 存在、所有 classpath 制品成功解析且版本正确；从本地 Maven 核验 Goety hash。
- [ ] 对比基线资产哈希，PNG/模型/方块状态无内容变化。更新 CI 到 Java 21 与新分支构建，保留仅构建/保存产物的行为。
- [ ] 主 agent 检查 diff，GPT-6-sol（high）审查构建/接入；修复后提交本任务文件。此时剩余业务类仍未迁移，不宣称整体 build 通过。

### Task 2: 马达、召唤存档与 Goety 法术

**负责人：** 主 agent。

**Files:**
- Modify: `content/motor/MotorContent.java`、`SoulMotorBlock.java`、`SoulMotorBlockEntity.java`、`SoulMotorData.java`、`content/spell/WavingSpell.java`。
- Modify tests: `gametest/SoulMotorTests.java`、`MotorPersistenceTests.java`、`WavingSpellTests.java`、`WavingRitualTests.java`。

**Interfaces:**
- Consumes: Task 1 的 NeoForge 总线与真实依赖 classpath。
- Produces: `SoulMotorData.FACTORY`；`load(CompoundTag, HolderLookup.Provider)`；`save(CompoundTag, HolderLookup.Provider)`；Create `read/write(CompoundTag, HolderLookup.Provider, boolean)`。
- Preserves: `SoulMotorData.get(MinecraftServer)`、`add(UUID, ServerLevel, BlockPos)`、`contains(long)`、`forget(long)`、`tick(MinecraftServer)`、`LIFETIME=12000`，以及已有召唤/护目镜 public 接口。
- Produces: `WavingSpell.acceptedEnchantments(): List<ResourceKey<Enchantment>>`；真实 NeoForge 放置事件和 snapshot 回滚。

- [ ] 先适配本任务 GameTest 的平台调用，保留六方向 128 RPM/64 SU、实际轴 8192 SU、第四台/跨维度淘汰、永久马达、过期初始化、扳手无掉落、法杖冷却及仪式断言。
- [ ] 将 `MotorPersistenceTests` 的保存/加载和磁盘 `DimensionDataStorage` 重建改为当前真实 API，assert 保留所属 UUID、recordId、expiresAt、未加载下界 removed 标记和 activeCount=3。
- [ ] 在尚未改本任务实现时运行适用的编译/测试，确认失败源于旧 API 或原行为未实现，保存日志。
- [ ] 最小适配注册、`ServerTickEvent.Post`、Create 持久化 provider、SavedData 工厂及 ResourceLocation 工厂；核心生命周期和应力逻辑沿用。
- [ ] 查授权 Goety 源适配法术签名和原生法杖/仪式测试调用；保持 1024 消耗、瞬发、1200 刻冷却和 32 格放置权限检查。
- [ ] Task 3/4 完成使整工程可运行后，执行本任务对应测试；全部通过且磁盘证据存在，再经 GPT-6-sol（high）审查并提交。

### Task 3: 流体能力、物品组件与能量守恒

**负责人：** 主 agent。

**Files:**
- Modify: `content/PlayerSouls.java`、`content/fluid/SoulFluidContent.java`、`SoulBucketItem.java`、`SoulReceiver.java`、`SoulTransfers.java`。
- Modify: `mixin/ItemDrainMixin.java`、`mixin/GenericItemEmptyingMixin.java`。
- Modify tests: `gametest/SoulFluidTests.java`、`SoulIntegrationTests.java`、`SoulMixingTests.java`。

**Interfaces:**
- Consumes: Task 1 的注册和资源，Goety `ModBlockEntities.ARCA/CURSED_CAGE` 与 ITotem 真实 API。
- Preserves: `PlayerSouls.change(Player, int, boolean): int`、`SoulTransfers.fillTotem(ItemStack, int, boolean): int`、`emptyTotem(ItemStack, IFluidHandler, boolean): Pair<FluidStack, ItemStack>`。
- Produces: `SoulReceiver.registerCapabilities(RegisterCapabilitiesEvent)` 按真实 Goety BE types 提供 `IFluidHandler`，并为 `SoulBucketItem` 注册 `Capabilities.FluidHandler.ITEM`；流体采用 `BaseFlowingFluid`。
- Preserves: `SoulFluidContent.TYPE/SOUL/FLOWING/BUCKET` 字段 `.get()`；无 LiquidBlock 与等价 OpenPipeEffectHandler。

- [ ] 先移植原能力查询、图腾数据和 mixing GameTest，保留模拟零副作用、实际剩余容量、满池/异流体保护、桶 1000 往返、停泵与排放守恒断言。
- [ ] 新增 `spentTotemPreservesCustomComponents`：200 灵魂图腾携带 CUSTOM_DATA marker=`preserve` 与 CUSTOM_NAME=`Migration marker`；排入足容量池后，产 200 mB、变 SPENT_TOTEM、两个标记保留，只有 Souls/Max Souls 移除；原输入模拟时保持全部组件不变。
- [ ] 新增 `totemWithoutStoredMaximumUsesItemCapacity`：未写 Max Souls 的有效图腾可按 `getMaxSouls()` 接受注液，SIMULATE 不创建/修改组件，EXECUTE 仅增加接受量；沿用现有配置调低/负余额边界测试。
- [ ] 用 compile/GameTest 先验证新平台能力缺失或新边界失败，再适配上述本任务实现。图腾读用 `ITotem.tag`，写用 `setSoulsamount/updateTag`，耗尽转换复制实际 component patch 并去掉灵魂字段。
- [ ] 按 NeoForge 事件注册能力，移除本次使其无用的 LazyOptional/旧 provider imports。受液行为仍以目标是否在线绑定、剩余容量和模拟/执行状态判断；不复刻 Goety attachment 系统。
- [ ] 保留分液池提交当刻二次检查、图腾部分转移和无世界流体。通过真实 pipeline、bucket、cage/arca 与四条 mixing 测试验证。
- [ ] GPT-6-sol（high）审查能量账、组件和 Mixin 注入目标；修复、重跑受影响测试后提交本任务文件。

### Task 4: 实体、客户端渲染和其余回归测试

**负责人：** GPT-6-luna（max），主 agent 核查实体核心逻辑并调试。

**Files:**
- Modify: `content/orb/SoulOrb.java`、`client/CroetyClient.java`、`client/SoulOrbRenderer.java`、`content/motor/SoulMotorClient.java`、`mixin/BasinParticlesMixin.java`。
- Modify tests: `gametest/SoulOrbTests.java`、`SoulOrbBehaviorTests.java`、`DemoPolishTests.java`、`ContentCleanupTests.java`。

**Interfaces:**
- Consumes: Task 1 的 DeferredHolder 实体/BE/流体类型注册，Task 2/3 的现有字段和 public 方法。
- Produces: `SoulOrb.defineSynchedData(SynchedEntityData.Builder)`；通过 vanilla/NeoForge `getAddEntityPacket(ServerEntity)` 跟踪生成；RegisterClientExtensionsEvent 的 fluid texture 扩展；原 BER/Flywheel visual 和蓝绿 orb renderer。
- Preserves: `SoulOrb.award(ServerLevel, Vec3, int)`、`getValue()`、`getAge()`、`getIcon()`、运动/合并/拾取/伤害/6000 刻逻辑及旧 Value×Count 读取。

- [ ] 先适配原 orb/polish/cleanup 测试平台接口，保留负值余量、配置调低、同号合并、相反符号、6000刻、水浮、阳光、环境伤害、11档、保存价值和护目镜取整断言。
- [ ] 记录尚未迁移实现的编译或运行失败，再最小适配 Entity builder/spawn packet 与 renderer 顶点链；SoulOrb 直接继承 Entity，不调用抽象 `super.defineSynchedData`。
- [ ] NeoForge 客户端专用事件隔离 renderer/visual/fluid extensions；服务端不加载客户端类。新流体纹理继续引用已有 `block/fluid_soul_still`。
- [ ] 保持三个 Mixin 的目标方法与客户端专用 Basin 配置；按真实 Create 源修正参数或渲染调用，不增加服务端世界扫描。
- [ ] 整工程 `compileJava` 和原 42+新增边界 GameTest 全部通过后，GPT-6-sol（high）审查本任务；修复并提交。

### Task 5: 完整构建、运行验收、审查和整理

**负责人：** 主 agent；GPT-6-sol（high）做全分支审查；用户做实际客户端验收。

**Files:**
- Modify: `AGENTS.md`、`README.md`、`docs/demo.md`、`docs/migration-preparation.md`。
- Create: `docs/neoforge-verification.md`，保存实际命令、测试数量、构建/JAR hash、依赖版本、运行成功标志、审查闭环和用户结果。
- Local evidence: `build/` 日志、`run/` 独立新世界、`.local/` 临时资料归档，不提交依赖仓库/JDK/存档。

- [ ] 执行 `gradlew build`，必须 `BUILD SUCCESSFUL`；检查 JAR 的 NeoForge 元数据、Java class major=65、新单数数据目录、资产哈希一致及无示例内容。
- [ ] 执行 `gradlew runGameTestServer`，确认原 42 项没有缺失、新边界测试执行并全部通过，保留完整报告和退出码。
- [ ] 执行 `gradlew runServer`，确认 Create/Goety/Curios/Patchouli 正确加载、`Done (...)!` 且无 Mixin apply failure；输入 stop 正常保存退出，核对无本任务残留服务端进程。
- [ ] GPT-6-sol（high）审查完整 diff 和 spec/plan 覆盖；主 agent 修复所有阻塞问题并按受影响范围重验，不能用基线测试结果替代新版证据。
- [ ] 真正启动 `runClient`，核对活跃进程与音频/渲染初始化证据；提供用户逐项测试清单和回复格式，按用户指令暂停工作，不使用 Computer Use。
- [ ] 收到用户反馈后处理失败项并复验；全部确认后写验收记录。保留测试世界和原始诊断证据。
- [ ] 最后检查每个目标交付项，更新 AGENTS 的“迁移中”状态、准确版本和查询工具用法；归档过时 Forge 资料、临时 probes 与重复准备产物，只整理本次产生的文件。
- [ ] 核验 Git 分支/索引/工作区和构建产物，提交已验证改动，报告产物路径及验证结果；只有全部完成才标记整体 goal complete。
