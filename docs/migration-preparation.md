# 1.21.1 NeoForge 迁移准备记录

日期：2026-10-08。目标目录：`D:\Develop\croety-1.21.1-neoforge`。

## 基线与工作区

- 基线为旧项目 `D:\Develop\croety` 的提交 `e305d91fd96fc22e9928e6eb2ce81ee69552e305`。
- 新目录是独立 Git 检出，分支为 `croety-1.21.1-neoforge`，保留原提交历史；远程地址为 `https://github.com/tw2ni9-arisu/croety.git`。未推送。
- 旧项目源文件未修改。旧分支当前名称为 `1.20.1`。
- 重新执行旧项目 Java 17 基线 `gradlew.bat --offline build`，结果为 `BUILD SUCCESSFUL in 20s`；日志为目标目录的 `build/migration-baseline-build.log`。
- 此构建未执行游戏测试。旧源码实际有 42 个 GameTest，迁移时保留其行为断言；旧版测试结果不能作为新版验收证据。
- 已在目标 `.local/jdk21/jdk-21.0.12.1+1` 准备 Eclipse Temurin Java 21；`java -version` 与 `javac -version` 均确认 `21.0.12.1`。Windows x64 JDK ZIP 的 SHA-256 为 `f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e`，与 Adoptium 官方 API 及校验文件一致。没有修改全局 Java 环境。

## 当前实现范围

| 模块 | 必须保留的行为 | 主要文件 |
|---|---|---|
| 灵魂马达 | 6 朝向、128 RPM、64 SU/RPM、8192 SU、亮度 4、无掉落、Create 扳手旋转/拆除 | `content/motor/` |
| 召唤与存档 | 临时马达 12000 刻、每玩家跨维度最多 3 台、淘汰最早一台、永久马达不受限、未加载到期马达重载前不能供能 | `SoulMotorData`、`SoulMotorBlockEntity` |
| 涌动聚晶 | 保留 `croety:waving_focus`；1024 基础消耗、瞬发、1200 刻冷却、32 格施法放置与权限检查 | `content/spell/WavingSpell.java` |
| 锻造仪式 | 大型水车为中心，缠魂水槽/潮汐聚晶/风车轴承/风帆或羊毛为周边，256 灵魂每秒、16 秒 | 仪式配方与 `waving_sails` 标签 |
| 液态灵魂 | 1 mB = 1 灵魂；源流体/流动流体/桶，无世界流体方块；Create 储罐、工作盆、泵与管网 | `content/fluid/SoulFluidContent.java` |
| 桶与分液 | 整桶 1000 mB，不能世界放置，蹲下补充 1000 并返空桶；不足容量不吞桶；提交当刻再次核对分液池容量 | `SoulBucketItem`、`ItemDrainMixin` |
| 图腾转换 | 根之图腾和灵魂图腾部分分液，耗尽形态正确，保留其它物品数据；模拟操作无副作用 | `SoulTransfers`、`GenericItemEmptyingMixin` |
| Goety 受液 | 诅咒之笼图腾和在线且仍绑定的玩家方舟，只接受实际剩余容量，不吞不兼容流体 | `SoulReceiver`、`PlayerSouls` |
| 管口排放 | 实际扣液与灵魂球价值守恒，模拟不生球，停泵停排放 | `OpenPipeEffectHandler` 注册 |
| 灵魂球 | 11 档原版经验球纹理、蓝绿渐变、5 生命、浮水、6000 刻寿命、日晒/环境伤害、同号合并、整颗或部分拾取、负值余量、旧 Value×Count 读取 | `content/orb/`、`client/SoulOrbRenderer.java` |
| 搅拌配方 | 加热灵质→5 mB、加热灵魂沙→25 mB、25 mB+4 绿宝石→4 灵魂绿宝石、加热 25 mB+2 金+2 铁→4 诅咒金属锭 | 4 条 Create mixing JSON |
| 客户端与内容 | 马达旋转轴/Flywheel、护目镜寿命、召唤/消散/工作盆粒子、中文英文名称与 Shift 详情、Croety 创造栏 | `SoulMotorClient`、客户端 Mixin、语言资源 |

初版 Obsidian 文档只作为需求资料。当前正式 ID 与修订行为来自源码和 `docs/demo.md`，例如聚晶采用 `waving_focus`。未实现的萃魂池、动力诅咒注入器及其抽取/萃取玩法不属于本次新增范围。

## 资源与测试核对

- 现有 33 个 Java 文件、28 个资源文件；GameTest 在 11 个类中。
- 正式模型、PNG 与动画元数据来自 `src/main/resources/assets/croety/`，直接复用；初版提及的 `TEXTURES_MODELS` 根目录当前不存在。
- 当前没有 Ponder 场景或 DataGenerator 实现，不因依赖包含 Ponder 而新增教程功能。
- 配方、标签与测试结构要调整为 1.21.1 的目录和序列化格式；纹理与模型内容不重制。
- 专用服务端、42 项 GameTest、客户端成功启动和用户实机结果分别提供运行证据，不能相互替代。

## 已确认的 API 变化

- ForgeGradle 更换为官方支持的 ModDevGradle，使用 Java 21 工具链。
- NeoForge 注册和事件类型替代 Forge；方块能力通过 `RegisterCapabilitiesEvent` 注册，查询通过 `Capabilities.FluidHandler.BLOCK`。
- `SoulBucketItem` 是桶子类，需显式注册 `Capabilities.FluidHandler.ITEM`。
- `ResourceLocation` 使用静态工厂；实体同步数据使用 `SynchedEntityData.Builder`。
- `SavedData` 工厂、方块实体及 Create 的 `read/write` 增加 `HolderLookup.Provider`。
- 物品数据改为 Data Components，不能机械替换旧 `ItemStack.getTag()/setTag()`；图腾和法杖数据以目标 Goety 的真实接口为准。
- Create 6.0.10 源码仍包含三个已有 Mixin 的目标方法，继续适配原逻辑；注入是否成功仍需新运行环境验证。

精确依赖版本和源码对应关系见 `reference/SOURCES.md`，NeoForge 标准接口说明见 `docs/ai/neoforge-1.21.1.md`。迁移后的构建与运行结果会另行记录，以上均不表示新版已可运行。
