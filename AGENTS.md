# Croety — 项目说明与 AI 协作指南

> 本文件会被 DSH 在**每个会话开始时自动加载**（工作区约定文件名是 `AGENTS.md`，不是 `AGENT.md`）。
> 所以它只放"必须一开始就知道"的东西；深入资料在 `docs/ai/`，按第 5 节索引按需读取。

## 0. 三条铁律

1. **不要凭记忆写 MC/Forge/Create/Goety 的 API。** 本项目所有依赖 jar 都已经反混淆并缓存在本地，
   用 `tools/find-api.ps1` 一条命令就能看到真实签名。写错方法名会浪费一整轮编译。
2. **改完必须验证，并给出证据。** 至少 `gradlew build`；涉及运行时行为还要 `runServer`/`runClient`，
   成功标志见第 9 节。不要用"应该可以"结案。
3. **动 Create / Goety 相关代码前，先读第 5 节里对应那份文档。** 那是专门为你省掉翻 jar 的时间写的。

## 1. 项目是什么

Minecraft 1.20.1 + Forge 的 **Create × Goety 联动 mod**。

- modid：`croety`，主包：`com.croety`，入口类 `src/main/java/com/croety/Croety.java`
- 不是整合包，是一个 Forge MDK 结构的**开发工作区**：可以直接 `runClient`/`runServer` 起游戏调试
- 工作区名字 croety = Create + Goety

## 2. 环境速查

```powershell
# JDK 17 已装好且 JAVA_HOME 已持久化（用户级），新终端直接可用，不需要手动 export
cd D:\Develop\croety
.\gradlew.bat build        # 编译 + 打包 -> build/libs/croety-1.0.0.jar
.\gradlew.bat runClient    # 起客户端（Create/Goety 直接从 classpath 加载）
.\gradlew.bat runServer    # 起服务端（首次需 run/eula.txt 里写 eula=true，已存在）
.\gradlew.bat runData      # 数据生成 -> src/generated/resources
```

- **Forge 1.20.1 必须用 Java 17。** 本机 `java` 命令可能指向 Oracle Java 25（系统 PATH 优先级更高），
  Gradle wrapper 应通过 `JAVA_HOME` 使用 JDK 17。仓库不写死 `org.gradle.java.home` 或用户目录；
  换机后先设置 `JAVA_HOME`，`tools/find-api.ps1` 也从该路径寻找工具。
- 首次构建/首次 runClient 会下载大量东西（Gradle 发行版、Minecraft 反编译、资源文件），
  之后就是几十秒。别以为卡死了。
- Gradle 构建约 20 秒（增量）/ 6 分钟（首次反编译）。

## 3. 工程结构

```
build.gradle                              依赖、仓库、runs（含 mixin refmap 关键配置）
gradle.properties                         版本号、映射、构建参数 —— 改版本只改这里
settings.gradle                           pluginManagement 仓库（Parchment 插件必须在这声明）
src/main/java/com/croety/
    Croety.java                           @Mod 入口：实际内容注册与联动初始化
    integration/CreateIntegration.java    调用 Create API（也是编译期探针）
    integration/GoetyIntegration.java     调用 Goety API
src/main/resources/META-INF/mods.toml     mod 元数据 + 依赖声明（create/goety/curios/patchouli）
src/main/resources/assets/croety/         模型、方块状态、贴图
libs/maven/                               Goety 的本地 Maven 仓库（它没有公共 Maven）
libs/sources/                             Create/Ponder/Flywheel 官方 sources jar
libs/sources/create-1.20.1-6.0.8-291/     ★ Create 完整源码（1997 个 .java，可直接 grep）
libs/sources/.decompiled/                 ForgeFlower 反编译缓存（Goety 等没有源码的依赖）
tools/find-api.ps1                        ★ 查真实 API 签名 / 真实源码
tools/fetch-deps.ps1                      重新拉取 libs/maven 和 libs/sources
docs/ai/                                  ★ 给 AI 读的深度资料（见第 5 节）
```

## 4. 依赖与前置链

| 组件 | 版本 | 来源 |
|---|---|---|
| Minecraft | 1.20.1 | Forge userdev |
| Forge | 1.20.1-47.4.23 | maven.minecraftforge.net |
| 映射 | Parchment 2023.09.03（叠在 Mojang official 上） | maven.parchmentmc.org |
| **Create** | 6.0.8-291（`:slim` 开发包） | maven.createmod.net |
| ↳ Flywheel | 1.0.5-264（API jar 编译 / 完整 jar 运行） | maven.createmod.net |
| ↳ Ponder | 1.0.91（**内含 Catnip**） | maven.createmod.net |
| ↳ Registrate | MC1.20-1.3.3 | maven.tterrag.com |
| ↳ MixinExtras | 0.4.1 | Maven Central |
| **Goety** | 2.5.57.3 | 本地 `libs/maven`（无公共 Maven） |
| ↳ Curios | 5.14.1+1.20.1 | maven.theillusivec4.top |
| ↳ Patchouli | 1.20.1-85-FORGE | maven.blamejared.com |
| JEI（可选） | 15.59.0.210 | maven.blamejared.com |

Flywheel/Ponder/Registrate/MixinExtras 的版本**刻意等于 Create 6.0.8 自己 jar-in-jar 的版本**，
改动前先确认 Create 换了什么，否则开发环境和玩家实际运行会不一致。

## 5. 资料索引（写代码前先读这里）

| 你要做的事 | 读这份 |
|---|---|
| 写 Create 机器/方块/物品/配方/goggle 提示 | `docs/ai/create-6.0.8.md` |
| 写 Goety 物品/法术/仆从/方块，或用它的 API | `docs/ai/goety-2.5.57.3.md` |
| 写客户端渲染动画 / Ponder 教程场景 | `docs/ai/flywheel-ponder.md` |
| 写普通 Forge 方块、物品、方块实体、GUI、配置、datagen | `docs/ai/forge-1.20.1.md` |
| 忘了怎么查 API | 第 7 节 |

这几份文档里的签名都是从本机实际 jar 用 javap 导出的（部分还用 `find-api.ps1 -Source` 对照了真实源码），
并附了自查命令。体量都不小，**按章节读，不要整篇塞进上下文**：

- `create-6.0.8.md` ~2800 行 / 229 个类引用
- `goety-2.5.57.3.md` ~2900 行 / 208 个类引用
- `flywheel-ponder.md` ~2000 行 / 205 个类引用
- `forge-1.20.1.md` ~415 行（本项目自己写的，短，建议先读）

文档里凡是标「已实测编译」的示例都用 `gradlew compileJava` 真编译过，可以照抄。
如果你发现文档和 jar 对不上，**以 jar 为准，并把文档改对**。

## 6. 代码骨架（照抄用）

### 6.1 通用 Forge 骨架

实际注册代码在 `src/main/java/com/croety/content/motor/MotorContent.java`（方块、物品、方块实体）和
`content/DemoTab.java`（创造模式标签页），由 `Croety.java` 接入。MDK 的 example 内容和示例 `Config.java` 已移除。
两个 `integration` 类提供跨 mod 调用。最常用的三段：

```java
// 注册（DeferredRegister 实测签名）
public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, Croety.MODID);

// 构造函数里必须挂到 mod 事件总线，否则不注册
BLOCKS.register(context.getModEventBus());

// 方块实体（注意 build(null) 会立刻取 MY_BLOCK.get()，用 lambda 延迟）
public static final RegistryObject<BlockEntityType<MyBE>> MY_BE =
        BLOCK_ENTITIES.register("my_be",
                () -> BlockEntityType.Builder.of(MyBE::new, MY_BLOCK.get()).build(null));
```

### 6.2 ★ 机械动力（Create）机械元件标准写法

> **模板全部从 Create 6.0.8 官方源码提取，并且已用 `gradlew compileJava` 真实编译通过**
> （方块 + 方块实体 + Registrate 注册链 + 应力注册，整体 EXITCODE=0）。可以直接照抄。
>
> 源码已解包在 `libs/sources/create-1.20.1-6.0.8-291/`，**可以直接 grep**（比如
> `grep "extends GeneratingKineticBlockEntity"` 就能列出所有产能元件）。
> 深度签名见 `docs/ai/create-6.0.8.md` 第 4 节。

> ### ⚠️ 术语先说清楚（别写错）
>
> **Create 本体是纯「旋转机械 + 应力（Stress）」系统，没有任何电力内容。**
> 查过官方 `assets/create/lang/zh_cn.json`（20 万字符）：`发电机 / 电力 / 电能 / 电压 / 供电`
> **出现 0 次**。所以：
>
> | 不要写 | 正确术语 | 依据 |
> |---|---|---|
> | 发电机 | **动力源** | 官方 ponder 分类名 `create.ponder.tag.kinetic_sources` = 「动力源」 |
> | 发电机 | **应力发生器** | 官方护目镜文案 `create.gui.goggles.generator_stats` = 「应力发生器状态：」 |
> | 电功率 / 耗电 | **应力影响（Stress Impact）** | 官方文案「高_应力影响_的元件」 |
> | 发电量 | **应力容量（Stress Capacity）** | 官方 `应力容量` |
> | 电量单位 | **SU（Stress Unit）**，转速单位 **RPM** | 官方「应力（SU）」「转速（RPM）」 |
>
> 「产能 / 耗能」在本节里指的是**应力**的产出与消耗，**不是电功率**。
> 电力玩法只存在于一系列**附属 mod**（第三方电力向附属）里 —— Create 本体没有，本项目也没装。
> 其他官方中文对照：创造马达（不是"电机"）、动力轴承、水车 / 大型水车、蒸汽引擎、手摇曲柄。

#### (1) 先选基类 —— 这一步决定 80% 的写法

| 元件类型 | 方块基类 | 方块实体基类 | Create 源码里的现成例子 |
|---|---|---|---|
| 纯旋转零件（轴） | `AbstractSimpleShaftBlock` | `SimpleKineticBlockEntity` | `ShaftBlock` |
| 纯旋转零件（齿轮） | `RotatedPillarKineticBlock` + `ICogWheel` | `SimpleKineticBlockEntity` | `CogWheelBlock` |
| 有朝向的耗能机器 | `KineticBlock` + `IBE<T>` | `KineticBlockEntity` | `MillstoneBlock` |
| 轴随朝向的机器 | `DirectionalKineticBlock` | `KineticBlockEntity` | `EncasedFanBlock` |
| 沿轴放置的机器 | `RotatedPillarKineticBlock` | `KineticBlockEntity` | — |
| 水平朝向机器 | `HorizontalKineticBlock` | `KineticBlockEntity` | — |
| **动力源 / 应力发生器（产能）** | 同机器，通常 `DirectionalKineticBlock` | **`GeneratingKineticBlockEntity`** | `CreativeMotorBlock`、`WaterWheelBlock`、`HandCrankBlock` |
| 成对的半边轴 | — | `DirectionalShaftHalvesBlockEntity` | — |

#### (2) `IRotate` 只有两个抽象方法

```java
public interface IRotate extends IWrenchable {
    boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face); // 哪面能接轴
    Axis getRotationAxis(BlockState state);                                                      // 绕哪个轴转
    default SpeedLevel getMinimumRequiredSpeedLevel() { return SpeedLevel.NONE; }
    default boolean hideStressImpact() { return false; }
    default boolean showCapacityWithAnnotation() { return false; }
}
```

**`KineticBlock` 并没有实现 `getRotationAxis`** —— 任何 `KineticBlock` 子类都必须自己写这两个方法
（`MillstoneBlock` 就是自己写的，别以为基类给了默认值）。

#### (3) 模板 A：耗能机器（吃转速 → 干活）

```java
public class MyMachineBlock extends KineticBlock implements IBE<MyMachineBlockEntity>, ICogWheel {
    public MyMachineBlock(Properties properties) { super(properties); }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;              // MillstoneBlock 的真实写法：只从下面接轴
    }

    @Override
    public Axis getRotationAxis(BlockState state) { return Axis.Y; }

    // IBE 负责把方块实体挂上去；newBlockEntity / getTicker 由接口默认实现提供，不用自己写
    @Override public Class<MyMachineBlockEntity> getBlockEntityClass() { return MyMachineBlockEntity.class; }
    @Override public BlockEntityType<? extends MyMachineBlockEntity> getBlockEntityType() { return MY_BE.get(); }
}

public class MyMachineBlockEntity extends KineticBlockEntity {
    public MyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Override
    public void tick() {
        super.tick();
        if (getSpeed() == 0) return;                // 没转就不干活（Create 所有机器都这么起手）
        timer -= getProcessingSpeed();
        if (timer <= 0) process();
    }

    /** 转速越快干得越快。注意：这不是基类方法，是每台机器自己定义的（Millstone 也是这样） */
    public int getProcessingSpeed() {
        return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
    }
}
```

#### (4) 模板 B：动力源（产出转速 / 应力容量）

```java
public class MyGeneratorBlock extends DirectionalKineticBlock implements IBE<MyGeneratorBlockEntity> {
    public MyGeneratorBlock(Properties properties) { super(properties); }

    @Override public boolean hasShaftTowards(LevelReader w, BlockPos pos, BlockState s, Direction face) {
        return face == s.getValue(FACING);          // 只朝 FACING 那面出轴
    }
    @Override public Axis getRotationAxis(BlockState s) { return s.getValue(FACING).getAxis(); }

    // 三个可选钩子（IRotate 的 default 方法）
    @Override public SpeedLevel getMinimumRequiredSpeedLevel() { return SpeedLevel.MEDIUM; }
    @Override public boolean showCapacityWithAnnotation() { return true; }
    @Override public boolean hideStressImpact() { return false; }
}

public class MyGeneratorBlockEntity extends GeneratingKineticBlockEntity {
    public MyGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** ★ 产能元件唯一必须覆写的方法：本 tick 产生的转速（正负 = 方向） */
    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(getMyRpm(), getBlockState().getValue(MyGeneratorBlock.FACING));
    }

    /** ★ 动力源的固定仪式，少一行就"放下去不转" */
    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    /** 转速发生变化后必须手动通知，否则动力网络不会刷新 */
    public void setMyRpm(int rpm) { this.rpm = rpm; updateGeneratedRotation(); }
}
```

`convertToDirection(float, Direction)` 是 `KineticBlockEntity` 的 **static** 方法。

#### (5) 应力（stress）怎么给 —— 有个必须避开的坑

**addon 绝对不能用 `CStress`。** `com.simibubi.create.infrastructure.config.CStress` 源码里有：

```java
if (!builder.getOwner().getModid().equals(Create.ID)) {
    throw new IllegalStateException("Non-Create blocks cannot be added to Create's config.");
}
```

所以 `CStress.setNoImpact()` / `setImpact()` / `setCapacity()` 用在 addon 方块上会**直接抛异常崩游戏**。
addon 用官方 API：

```java
// 耗能：注册"1 RPM 时的应力影响"
BlockStressValues.IMPACTS.register(MY_MACHINE.get(), () -> 4.0);
// 产能：注册"1 RPM 时的应力容量"
BlockStressValues.CAPACITIES.register(MY_GENERATOR.get(), () -> 32.0);

// 动力源的 RPM，写在注册链上，只影响护目镜 tooltip，不影响实际转速
.onRegister(BlockStressValues.setGeneratorSpeed(64))         // 恒定转速
.onRegister(BlockStressValues.setGeneratorSpeed(256, true))  // mayGenerateLess
```

`KineticBlockEntity.calculateStressApplied()` / `calculateAddedStressCapacity()` **默认就是查这张表**，
所以通常**不用覆写**。同一个 Block 重复注册会抛 `IllegalArgumentException`。

#### (6) 方块实体注册 + 客户端视觉（一条链写完）

```java
public static final BlockEntityEntry<MyMachineBlockEntity> MY_BE = REGISTRATE
        .blockEntity("my_machine", MyMachineBlockEntity::new)
        .visual(() -> SingleAxisRotatingVisual::shaft, false)     // Flywheel 旋转动画；false = 覆盖原版渲染
        .validBlocks(MY_MACHINE)
        .renderer(() -> ShaftRenderer::new)
        .register();
```

现成的视觉工厂（`AllBlockEntityTypes.java` 里到处在用）：
`SingleAxisRotatingVisual::shaft`、`SingleAxisRotatingVisual.of(AllPartialModels.X)`、
`OrientedRotatingVisual.of(AllPartialModels.X)`、`OrientedRotatingVisual::gantryShaft`。
只有做自定义动画时才需要自己写 `KineticBlockEntityVisual` 子类。

#### (7) 转速 / 应力 语义速查

| 方法 | 含义 |
|---|---|
| `getSpeed()` | 当前**实际**转速（过载会被压低） |
| `getTheoreticalSpeed()` | 网络给出的转速 |
| `getGeneratedSpeed()` | 自己产出的转速 —— **只有动力源覆写** |
| `hasSource()` | 是否已接上动力网络 |
| `isOverStressed()` | 是否过载 |
| `setSpeed(float)` / `updateGeneratedRotation()` | 手动改转速 / 重新推给网络 |
| `SpeedLevel` | `NONE/SLOW/MEDIUM/FAST`，阈值来自服务端配置，配合 `getMinimumRequiredSpeedLevel()` |

#### (8) 机械元件的坑（都来自真实源码/编译）

1. **`CStress` 不能给 addon 用**，会抛 `IllegalStateException`（见上）。
2. **`KineticBlock` 不实现 `getRotationAxis`**，忘了写编译不过。
3. **动力源忘了在 `initialize()` 里 `updateGeneratedRotation()`** → 放下不转。
4. **转速变了不通知网络** → 改了数值但机器没反应。
5. **`addBehaviours` 是在父类构造函数里调用的**（Create 源码实证），此时子类字段初始化器还没跑，
   所以里面赋值的字段，声明时**不要写 `= null` 之类的初始化器**，否则会被覆盖回去。
6. **用 `IBE<T>` 挂方块实体后不要再自己覆写 `newBlockEntity`/`getTicker`**，接口默认实现已经有了。

## 7. 查 API 的标准动作

**不要猜，也不要上网找过时教程 —— 直接查本地 jar。**

```powershell
.\tools\find-api.ps1 -Class com.simibubi.create.AllBlocks          # 精确类签名
.\tools\find-api.ps1 -Class net.minecraft.world.item.Item -Private  # 含私有成员
.\tools\find-api.ps1 -Search goggle                                 # 不知道全名时模糊搜
.\tools\find-api.ps1 -Search IServant -Jar goety                    # 只在某个 jar 里搜
.\tools\find-api.ps1 -ListJars                                      # 列出会搜索的 jar

# ★ 直接看真实源码（比 javap 更有用，能看到实现、注释、mod 自己怎么用这个 API）
.\tools\find-api.ps1 -Source com.simibubi.create.content.kinetics.base.KineticBlockEntity
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.magic.ISpell
```

`-Source` 的实现方式：
1. `libs/sources/` 里放了 **Create / Ponder / Flywheel 的官方 sources jar**，对应类**直接打印原始源码**（带注释）。
2. 没有 sources 的（如 Goety、Curios、Patchouli）**自动用 ForgeFlower 反编译**，结果缓存在
   `libs/sources/.decompiled/`，第二次查同一个类是秒出。
3. 反编译出来的代码可读但会丢失注释和部分泛型，属于正常现象。

**写 Create 代码前强烈建议先 `-Source` 看一眼 Create 自己是怎么写的**，比任何文档都准。

另外，**Create 的完整源码已经解包**在 `libs/sources/create-1.20.1-6.0.8-291/`（1997 个 .java），
可以直接用 grep 工具搜，这是找"Create 是怎么实现 X 的"最快的方式：

```powershell
# 例：列出所有动力源（产能元件）
grep "extends GeneratingKineticBlockEntity" libs/sources/create-1.20.1-6.0.8-291
# 例：看谁用了某个 API
grep "BlockStressValues\\." libs/sources/create-1.20.1-6.0.8-291
```

脚本搜索的就是 Gradle 放在 **compileClasspath** 上的那批 jar（已映射成项目用的 Parchment 命名），
所以**看到什么 javac 就接受什么**。找不到时会给出相近名字的建议。

## 8. 已知坑（踩过的，别再踩）

### 8.1 Create 的 mixin refmap —— 本项目最贵的一个坑
现象：`runClient`/`runServer` 启动即崩：
```
Mixin apply failed create.mixins.json:accessor.SystemReportAccessor -> net.minecraft.SystemReport
InvalidAccessorException: No candidates were found matching f_143509_:Ljava/lang/String;
```
原因：Create 发布包里 mixin 的 refmap 是 SRG 名，而开发环境是 named 名；ForgeGradle 6 自己**不做** refmap 重映射。
解决：`build.gradle` 的 runs 块里两行（**新加 run 配置时别漏**）：
```groovy
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
```
正常启动时日志里每个带 mixin 的 mod 会有一行 `Remapping refMap <mod>.refmap.json`。

### 8.2 Parchment 插件不在 Gradle 插件门户
`org.parchmentmc.librarian.forgegradle` 只在 `maven.parchmentmc.org`。
必须写在 `settings.gradle` 的 `pluginManagement.repositories` 里，写在 `build.gradle` 的
`repositories` 里没用。

### 8.3 Goety 没有公共 Maven
CurseForge 对本机返回 403，且没有官方 Maven。它的发布 jar 被放在 `libs/maven` 当作本地 Maven 模块
（`com.polarice3:goety:2.5.57.3`），再由 `fg.deobf` 反混淆。`libs/maven` 在 `.gitignore` 里，
换了机器跑一次 `pwsh -File tools/fetch-deps.ps1` 重建。
以后要给别的"只有 jar"的 mod 加依赖，照这个套路做。

### 8.4 本机网络坑
出口屏蔽了 CRL/OCSP：`curl.exe` 完全不可用，PowerShell/.NET 默认请求会报
`CRYPT_E_REVOCATION_OFFLINE`。下载前要：
```powershell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[Net.ServicePointManager]::CheckCertificateRevocationList = $false
```
Gradle/JVM 不受影响（不校验证书吊销）。写脚本下载时记得带上这两行。

### 8.5 Goety 包名大小写
根包是 `com.Polarice3.Goety`（大写 P、大写 G），类名里还有 `ModItems`/`ModBlocks`/`ModEntityType` 之分。
大小写写错 javac 只会说"找不到符号"，很容易浪费时间。不确定就 `find-api.ps1 -Search`。

### 8.6 版本号带 build number
Create 的 Maven 版本是 `6.0.8-291`（release 号 + CI build 号），不是 `6.0.8`。
`gradle.properties` 里已经写好，改版本时注意保持这个格式。

## 9. 完成前的验证清单

| 检查 | 命令 | 成功标志 |
|---|---|---|
| 编译 | `gradlew build` | `BUILD SUCCESSFUL`，`build/libs/croety-1.0.0.jar` 存在 |
| 服务端 | `gradlew runServer` | 日志出现 `Done (N.NNNs)! For help, type "help"`，且无 `Mixin apply failed` |
| 客户端 | `gradlew runClient` | `Sound engine started`、Flywheel 的 `Loaded N shader sources`、Create 的 `Loaded 56 train hat configurations` |
| 依赖加载 | 上面两者的日志 | 出现 `Found valid mod file ... {create} ... 6.0.8-291` 和 `{goety} ... 2.5.57.3` |

- `runServer`/`runClient` 会**真的弹窗口/起服务**，跑完要杀掉残留 java 进程：
  `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*croety*' }`
- `runServer` 需要 `run/eula.txt` 里 `eula=true`（已经建好）。
- 客户端里 `Failed to request yggdrasil public key` 是离线开发客户端的正常报错，不是问题。

### 验证一段文档里的示例代码是否真的能编译

文档里的示例不保证永远和 jar 一致（尤其升级 Forge/Create 之后）。要验证：

```powershell
# 1. 把示例贴成一个临时类，放进 src/main/java/com/croety/ 下
# 2. 编译（约 20 秒）
.\gradlew.bat compileJava
# 3. 编译通过就说明签名是真的；然后删掉这个临时文件
Remove-Item src/main/java/com/croety/你的临时类.java
```

`docs/ai/forge-1.20.1.md`、本文件第 6 节，以及 `docs/ai/` 里标了「已实测编译」的示例，
都是用这个方法**实际编译验证过**的。改了依赖版本之后，值得把关键示例再验一遍。

### javac 报错是乱码时

本机 `javac` 默认用 GBK 输出，中文报错在 UTF-8 终端里是乱码，看不出缺哪个符号。加这个环境变量拿英文报错：

```powershell
$env:JAVA_TOOL_OPTIONS = '-Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8'
.\gradlew.bat compileJava
```

典型的 `cannot find symbol` + `location: class X` 能直接指出是**哪个类的**方法不存在，
比对着乱码猜快得多。（这个技巧就是用 `find-api.ps1` 排查 Create 示例时总结出来的。）

## 10. 给 AI 的其它约定

- 注释和文档写**中文**，代码标识符写英文。
- 改 `gradle.properties` 里的版本号时，同步检查 `mods.toml` 里的 `versionRange`。
- 新增 run 配置或新加一个带 mixin 的依赖后，回头看第 8.1 节。
- 人类的说明文档是 `README.md`（构建/运行/加依赖），本文件是给 AI 的协作指南，两者不重复。
