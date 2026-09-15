# Create 6.0.8 开发 API 参考（croety / 1.20.1 Forge）

> 本文所有类名、方法名、参数列表、字段均通过 **javap 直接读取本机 jar** 得到，未依赖记忆或网络资料。
> 依据的 jar（与 `gradle.properties` 中 croety 的 compileClasspath 一致）：
>
> | 组件 | 版本 | 文件 |
> |---|---|---|
> | Create | `6.0.8-291` | `create-1.20.1-6.0.8-291_mapped_parchment_2023.09.03-1.20.1-slim.jar` |
> | Registrate | `MC1.20-1.3.3` | `Registrate-MC1.20-1.3.3_mapped_parchment_2023.09.03-1.20.1.jar` |
> | Minecraft + Forge | `1.20.1-47.4.23` | `forge-1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1.jar` |
> | Ponder（内含 Catnip） | `1.0.91` | `Ponder-Forge-1.20.1-1.0.91_mapped_parchment_2023.09.03-1.20.1.jar` |
> | Forge EventBus | `6.2.33` | `eventbus-6.2.33.jar` |
>
> 复现命令见第 10 节。**若本文与 jar 不一致，以 jar 为准。**

> ### ⚠️ 术语：Create 本体没有任何电力内容
>
> Create 是纯「旋转机械 + **应力（Stress）**」系统。官方 `assets/create/lang/zh_cn.json`（20 万字符）里
> `发电机 / 电力 / 电能 / 电压 / 供电` 出现 **0 次**。本文与任何 Create 附属代码/文档请用官方术语：
>
> - **动力源** —— 官方 ponder 分类名 `create.ponder.tag.kinetic_sources` = 「动力源」
> - **应力发生器** —— 官方护目镜文案 `create.gui.goggles.generator_stats` = 「应力发生器状态：」
> - 单位是 **SU（应力）** 与 **RPM（转速）**；耗能叫 **应力影响**，产能叫 **应力容量**
> - 官方中文对照：**创造马达**（不是"电机"）、动力轴承、水车 / 大型水车、蒸汽引擎、手摇曲柄
>
> 下文里的「产能 / 耗能」一律指**应力**的产出与消耗，**不是电功率**。
> 电力玩法只存在于第三方**电力向附属 mod**，Create 本体不提供，本项目也没装。

---

## 目录

1. [总览](#1-总览)
2. [Create 官方 API 包 `com.simibubi.create.api.*`](#2-create-官方-api-包-comsimibubicreateapi)
   - [2.1 api.behaviour.display](#21-apibehaviourdisplay)
   - [2.2 api.behaviour.interaction](#22-apibehaviourinteraction)
   - [2.3 api.behaviour.movement](#23-apibehaviourmovement)
   - [2.4 api.behaviour.spouting](#24-apibehavioursputing)
   - [2.5 api.boiler](#25-apiboiler)
   - [2.6 api.connectivity](#26-apiconnectivity)
   - [2.7 api.contraption.*](#27-apicontraption)
   - [2.8 api.data 与 api.data.recipe](#28-apidata-与-apidatarecipe)
   - [2.9 api.effect](#29-apieffect)
   - [2.10 api.equipment.goggles](#210-apiequipmentgoggles)
   - [2.11 api.equipment.potatoCannon](#211-apiequipmentpotatocannon)
   - [2.12 api.event](#212-apievent)
   - [2.13 api.packager](#213-apipackager)
   - [2.14 api.registrate](#214-apiregistrate)
   - [2.15 api.registry](#215-apiregistry)
   - [2.16 api.schematic.*](#216-apischematic)
   - [2.17 api.stress](#217-apistress)
3. [注册方块与物品（CreateRegistrate）](#3-注册方块与物品createregistrate)
4. [动能机械（kinetics）](#4-动能机械kinetics)
5. [方块实体（SmartBlockEntity 与 behaviour）](#5-方块实体smartblockentity-与-behaviour)
6. [护目镜信息](#6-护目镜信息)
7. [配方（processing recipe）](#7-配方processing-recipe)
8. [装置与移动（contraptions）](#8-装置与移动contraptions)
9. [常见坑](#9-常见坑)
10. [验证记录](#10-验证记录)

---

## 1. 总览

### 1.1 com.simibubi.create.Create

`javap -cp <create.jar> com.simibubi.create.Create` 实测结果（节选）：

```java
public class com.simibubi.create.Create {
  public static final java.lang.String ID;          // "create"
  public static final java.lang.String NAME;
  public static final org.slf4j.Logger LOGGER;
  public static final com.google.gson.Gson GSON;
  public static final java.util.Random RANDOM;

  public static net.createmod.catnip.lang.LangBuilder lang();
  public static net.minecraft.resources.ResourceLocation asResource(java.lang.String);
  public static com.simibubi.create.foundation.data.CreateRegistrate registrate();
  public static void onCtor();
  public static void init(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent);
  public static void onRegister(net.minecraftforge.registries.RegisterEvent);
}
```

要点：

- `Create.ID` 是 `"create"`（字节码中 `asResource` 用的常量字符串就是 `"create"`）。
- `Create.LOGGER` 是 `org.slf4j.Logger`（slf4j，不是 log4j）。
- `Create.lang()` 返回 `net.createmod.catnip.lang.LangBuilder`，构造用的 namespace 也是 `"create"`。
- `Create.asResource(String)` 等价于 `new ResourceLocation("create", path)`。

> ⚠️ **`Create.registrate()` 对 addon 不可用。**
> 反编译其字节码可见：它用 `StackWalker.getCallerClass().getPackageName().startsWith("com.simibubi.create")` 判断调用方，
> 不满足时直接 `throw new UnsupportedOperationException("Other mods are not permitted to use create's registrate instance.")`。
> **addon 必须自己 `CreateRegistrate.create("croety")`**（见第 3 节）。

### 1.2 顶层常量类（哪些真实存在）

以下全部在 jar 的 class 清单中确认存在，并用 javap 验证了签名：

| 类 | 内容 |
|---|---|
| `com.simibubi.create.AllBlocks` | `public static final BlockEntry<...>` 若干；`public static void register()` |
| `com.simibubi.create.AllItems` | `public static final ItemEntry<...>` 若干；`public static void register()` |
| `com.simibubi.create.AllBlockEntityTypes` | `public static final BlockEntityEntry<...>` 若干；`public static void register()` |
| `com.simibubi.create.AllFluids` | `public static final FluidEntry<...>` 若干；`public static void register()` / `registerFluidInteractions()` |
| `com.simibubi.create.AllTags` | 见下 |
| `com.simibubi.create.AllRecipeTypes` | `public final class ... extends java.lang.Enum<AllRecipeTypes> implements IRecipeTypeInfo`；`public static void register(IEventBus)` |
| `com.simibubi.create.AllSoundEvents` | `public static final AllSoundEvents$SoundEntry` 若干；`public static void register(RegisterEvent)`、`public static void prepare()` |
| `com.simibubi.create.AllCreativeModeTabs` | `public static final RegistryObject<CreativeModeTab> BASE_CREATIVE_TAB`、`PALETTES_CREATIVE_TAB`；`public static void register(IEventBus)` |

其它同样存在的常量类（未逐一展开签名）：
`AllBogeyStyles`、`AllContraptionTypes`、`AllContraptionMovementSettings`、`AllDamageTypes`、`AllDisplaySources`、
`AllDisplayTargets`、`AllEnchantments`、`AllEntityDataSerializers`、`AllEntityTypes`、`AllInteractionBehaviours`、
`AllKeys`、`AllMenuTypes`、`AllMountedDispenseItemBehaviors`、`AllMountedStorageTypes`、`AllMovementBehaviours`、
`AllOpenPipeEffectHandlers`、`AllPackets`、`AllPartialModels`、`AllParticleTypes`、`AllSchematicStateFilters`、
`AllShapes`、`AllSpecialTextures`、`AllSpriteShifts`、`AllStructureProcessorTypes`、`AllBlockSpoutingBehaviours`。

**注意：`AllTags` 的子类是内部类，名字带 `$`：**

```java
public class com.simibubi.create.AllTags {
  public static <T> net.minecraft.tags.TagKey<T> optionalTag(IForgeRegistry<T>, ResourceLocation);
  public static <T> net.minecraft.tags.TagKey<T> forgeTag(IForgeRegistry<T>, java.lang.String);
  public static TagKey<Block>  forgeBlockTag(java.lang.String);
  public static TagKey<Item>   forgeItemTag(java.lang.String);
  public static TagKey<Fluid>  forgeFluidTag(java.lang.String);
}
```

存在的内部类（jar class 清单确认）：`AllTags$AllBlockTags`、`AllTags$AllItemTags`、`AllTags$AllFluidTags`、
`AllTags$AllEntityTags`、`AllTags$AllContraptionTypeTags`、`AllTags$AllMountedItemStorageTypeTags`、
`AllTags$AllRecipeSerializerTags`、`AllTags$NameSpace`。

每个枚举都有形如 `public final TagKey<Block> tag;` 的字段和 `matches(...)` 方法：

```java
public final class com.simibubi.create.AllTags$AllBlockTags extends java.lang.Enum<AllTags$AllBlockTags> {
  public static final AllTags$AllBlockTags CASING;   // 还有 BRITTLE / COPYCAT_ALLOW / COPYCAT_DENY /
  public final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag;  // NON_MOVABLE / WRENCH_PICKUP / ...
  public boolean matches(net.minecraft.world.level.block.Block);
  public boolean matches(net.minecraft.world.item.ItemStack);
  public boolean matches(net.minecraft.world.level.block.state.BlockState);
}
```

`AllTags$AllItemTags`、`AllTags$AllFluidTags`、`AllTags$AllEntityTags` 结构相同（`matches(Item)`/`matches(ItemStack)`、
`matches(Fluid)`/`matches(FluidState)`、`matches(EntityType<?>)`/`matches(Entity)`）。

### 1.3 addon 如何引用 Create 内容

Create 的所有内容都是 Registrate entry，**不要把 `AllBlocks.COGWHEEL` 直接当 Block 用**，要 `.get()`：

```java
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import net.minecraft.world.level.block.Block;

Block shaft = AllBlocks.SHAFT.get();                                  // BlockEntry<T>.get() -> T
boolean isCasing = AllTags.AllBlockTags.CASING.matches(state);
boolean isWrench = AllTags.AllItemTags.WRENCH.matches(stack);
ResourceLocation id = com.simibubi.create.Create.asResource("crushing");
```

`RegistryEntry` 的取值语义（**已从字节码确认**）：

| 方法 | 行为 |
|---|---|
| `T get()` | 等价于 `Objects.requireNonNull(getUnchecked(), supplier)` → **未注册时抛 NPE** |
| `T getUnchecked()` | 等价于 `delegate.orElse(null)` → **未注册时返回 null** |
| `boolean isPresent()` | 判断是否已注册 |

所以：**类加载期（静态初始化块 / 静态字段初始化）绝不能调用 `.get()`**；要用 lambda / `NonNullSupplier` 延迟。

### 1.4 mods.toml 里的 Create 依赖（取自 Create 自己的 jar）

Create jar 内 `META-INF/mods.toml` 原文（节选）：

```toml
[[mods]]
modId = "create"
version = "6.0.8-291"

[[dependencies.create]]
modId = "forge"
mandatory = true
versionRange = "[47.1.3,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.create]]
modId = "minecraft"
mandatory = true
versionRange = "[1.20.1]"
ordering = "NONE"
side = "BOTH"

[[dependencies.create]]
modId = "flywheel"
mandatory = true
versionRange = "[1.0.0,2.0)"
ordering = "AFTER"
side = "CLIENT"
```

---

## 2. Create 官方 API 包 com.simibubi.create.api.*

`com.simibubi.create.api` 下共 **85** 个顶层类，分布在 30 个子包里（数量由 jar class 清单统计）。下面逐个走。

### 2.1 api.behaviour.display

**用途**：让方块成为「显示屏数据源」或「显示屏目标」（Display Link 系统）。

**DisplaySource**（`com.simibubi.create.api.behaviour.display.DisplaySource`，`public abstract class`）：

```java
public static final SimpleRegistry.Multi<Block, DisplaySource> BY_BLOCK;
public static final SimpleRegistry.Multi<BlockEntityType<?>, DisplaySource> BY_BLOCK_ENTITY;
public static final java.util.List<MutableComponent> EMPTY;
public static final MutableComponent EMPTY_LINE;
public static final MutableComponent WHITESPACE;

public abstract List<MutableComponent> provideText(DisplayLinkContext, DisplayTargetStats);
public void transferData(DisplayLinkContext, DisplayTarget, int);
public void onSignalReset(DisplayLinkContext);
public void populateData(DisplayLinkContext);
public int getPassiveRefreshTicks();
public boolean shouldPassiveReset();
protected final ResourceLocation getId();
protected String getTranslationKey();
public Component getName();
public void loadFlapDisplayLayout(DisplayLinkContext, FlapDisplayBlockEntity, FlapDisplayLayout, int);
public void loadFlapDisplayLayout(DisplayLinkContext, FlapDisplayBlockEntity, FlapDisplayLayout);
public List<List<MutableComponent>> provideFlapDisplayText(DisplayLinkContext, DisplayTargetStats);
public void initConfigurationWidgets(DisplayLinkContext, ModularGuiLineBuilder, boolean);

public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displaySource(RegistryEntry<? extends DisplaySource>);
public static DisplaySource get(ResourceLocation);
public static List<DisplaySource> getAll(LevelAccessor, BlockPos);
```

**DisplayTarget**：

```java
public static final SimpleRegistry<Block, DisplayTarget> BY_BLOCK;
public static final SimpleRegistry<BlockEntityType<?>, DisplayTarget> BY_BLOCK_ENTITY;

public abstract void acceptText(int, List<MutableComponent>, DisplayLinkContext);
public abstract DisplayTargetStats provideStats(DisplayLinkContext);
public AABB getMultiblockBounds(LevelAccessor, BlockPos);
public Component getLineOptionText(int);
public static void reserve(int, BlockEntity, DisplayLinkContext);
public boolean isReserved(int, BlockEntity, DisplayLinkContext);
public boolean requiresComponentSanitization();

public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displayTarget(RegistryEntry<? extends DisplayTarget>);
public static DisplayTarget get(ResourceLocation);
public static DisplayTarget get(LevelAccessor, BlockPos);
```

**注册**：DisplaySource 是 `SimpleRegistry.Multi`（用 `add`），DisplayTarget 是 `SimpleRegistry`（用 `register`）。

源码里 `getPassiveRefreshTicks()` 默认返回 **100**，`shouldPassiveReset()` 默认返回 **true**；
`getId()` 的实现是 `CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(this)` —— **没在 DISPLAY_SOURCE 注册表里注册过的对象调用 `getId()` 会返回 null**。
`getName()` 拼的 lang key 是 `<namespace>.display_source.<path>`。

`DisplaySource.displaySource(RegistryEntry)` / `DisplayTarget.displayTarget(...)` 的实现是
`builder.onRegisterAfter(CreateRegistries.DISPLAY_SOURCE, block -> BY_BLOCK.add(block, source.get()))`，
所以它**保证**在 DisplaySource 本身注册完成之后才把方块登记进去 —— 这是接入数据源的推荐写法。

```java
// 1) 用 CreateRegistrate 注册 DisplaySource/DisplayTarget 本体
public static final RegistryEntry<MyDisplaySource> MY_SOURCE =
        REGISTRATE.displaySource("my_source", MyDisplaySource::new).register();

// 2) 把方块挂上数据源：用 Create 提供的 builder transform
REGISTRATE.block("my_gauge", MyGaugeBlock::new)
        .transform(DisplaySource.displaySource(MY_SOURCE))
        .simpleItem()
        .register();

// 3) 手工登记（必须在方块注册完成之后调用）
DisplaySource.BY_BLOCK.add(MyBlocks.MY_GAUGE.get(), MY_SOURCE.get());
DisplayTarget.BY_BLOCK.register(MyBlocks.MY_BOARD.get(), new MyDisplayTarget());
```

### 2.2 api.behaviour.interaction

**MovingInteractionBehaviour**（`public abstract class`）—— 让方块在装置上被玩家右键时执行逻辑：

```java
public static final SimpleRegistry<Block, MovingInteractionBehaviour> REGISTRY;

public static <B extends Block> NonNullConsumer<? super B> interactionBehaviour(MovingInteractionBehaviour);
protected void setContraptionActorData(AbstractContraptionEntity, int, StructureTemplate.StructureBlockInfo, MovementContext);
protected void setContraptionBlockData(AbstractContraptionEntity, BlockPos, StructureTemplate.StructureBlockInfo);
public boolean handlePlayerInteraction(Player, InteractionHand, BlockPos, AbstractContraptionEntity);
public void handleEntityCollision(Entity, BlockPos, AbstractContraptionEntity);
```

**ConductorBlockInteractionBehavior**（`public abstract class extends MovingInteractionBehaviour`）：

```java
public abstract boolean isValidConductor(BlockState);
protected void onScheduleUpdate(boolean, BlockState, Consumer<BlockState>);
public final boolean handlePlayerInteraction(Player, InteractionHand, BlockPos, AbstractContraptionEntity);
```

内部类 `ConductorBlockInteractionBehavior$BlazeBurner`（`public class ... extends ConductorBlockInteractionBehavior`，
构造 `()`，`public boolean isValidConductor(BlockState)`）。

```java
REGISTRATE.block("my_lever", MyLeverBlock::new)
        .onRegister(MovingInteractionBehaviour.interactionBehaviour(new MyLeverInteraction()))
        .simpleItem()
        .register();
// 或注册后手工：MovingInteractionBehaviour.REGISTRY.register(block, new MyLeverInteraction());
```

### 2.3 api.behaviour.movement

**MovementBehaviour**（`public interface`）—— 装置上「actor」的行为（钻头、锯、犁等）。

全部成员（javap 实测）：

```java
public static final SimpleRegistry<Block, MovementBehaviour> REGISTRY;

public static <B extends Block> NonNullConsumer<? super B> movementBehaviour(MovementBehaviour);

default boolean isActive(MovementContext);
default void tick(MovementContext);
default void startMoving(MovementContext);
default void visitNewPosition(MovementContext, BlockPos);
default Vec3 getActiveAreaOffset(MovementContext);
default ItemStack canBeDisabledVia(MovementContext);
default void onDisabledByControls(MovementContext);
default boolean mustTickWhileDisabled();
default void dropItem(MovementContext, ItemStack);
default void onSpeedChanged(MovementContext, Vec3, Vec3);
default void stopMoving(MovementContext);
default void cancelStall(MovementContext);
default void writeExtraData(MovementContext);
default boolean disableBlockEntityRendering();
default void renderInContraption(MovementContext, VirtualRenderWorld, ContraptionMatrices, MultiBufferSource);
default ActorVisual createVisual(VisualizationContext, VirtualRenderWorld, MovementContext);
```

最常用的是 `tick(MovementContext)` 和 `visitNewPosition(MovementContext, BlockPos)`。

```java
public class MyDrillMovement implements MovementBehaviour {
    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        Level level = context.world;
        if (level.isClientSide) return;
        level.destroyBlock(pos, true);
    }

    @Override
    public void tick(MovementContext context) {
        // 每 tick
    }
}

REGISTRATE.block("my_drill", MyDrillBlock::new)
        .onRegister(MovementBehaviour.movementBehaviour(new MyDrillMovement()))
        .simpleItem()
        .register();
```

### 2.4 api.behaviour.spouting

**BlockSpoutingBehaviour**（`public interface`）—— 让 Spout（灌注口）能对某方块灌注。

```java
public static final SimpleRegistry<Block, BlockSpoutingBehaviour> BY_BLOCK;
public static final SimpleRegistry<BlockEntityType<?>, BlockSpoutingBehaviour> BY_BLOCK_ENTITY;

public static BlockSpoutingBehaviour get(Level, BlockPos);
public abstract int fillBlock(Level, BlockPos, SpoutBlockEntity, FluidStack, boolean simulate);
```

**CauldronSpoutingBehavior**（`public final class ... extends Enum<...> implements BlockSpoutingBehaviour`）：

```java
public static final CauldronSpoutingBehavior INSTANCE;
public static final SimpleRegistry<Fluid, CauldronSpoutingBehavior.CauldronInfo> CAULDRON_INFO;
public int fillBlock(Level, BlockPos, SpoutBlockEntity, FluidStack, boolean);
```

内部类 `CauldronSpoutingBehavior$CauldronInfo` 是 record：
`public CauldronInfo(int amount, Block cauldron)` / `public CauldronInfo(int amount, BlockState cauldron)`，
访问器 `amount()` / `cauldron()`。

**StateChangingBehavior**（`public final class ... extends Record implements BlockSpoutingBehaviour`）：

```java
public StateChangingBehavior(int, Predicate<Fluid>, Predicate<BlockState>, UnaryOperator<BlockState>);
public static BlockSpoutingBehaviour setTo(int amount, Predicate<Fluid>, Block);
public static BlockSpoutingBehaviour setTo(int amount, Predicate<Fluid>, BlockState);
public static BlockSpoutingBehaviour incrementingState(int amount, Predicate<Fluid>, IntegerProperty);
```

```java
// 让"我的方块"被岩浆灌注后变成另一个方块，每次消耗 250mB
BlockSpoutingBehaviour.BY_BLOCK.register(MyBlocks.MY_CRUCIBLE.get(),
        StateChangingBehavior.setTo(250, fluid -> fluid == Fluids.LAVA, MyBlocks.MY_MOLTEN.get()));
```

### 2.5 api.boiler

**BoilerHeater**（`public interface`）—— 声明某方块能给蒸汽锅炉供热。

```java
public static final int PASSIVE_HEAT;
public static final int NO_HEAT;
public static final BoilerHeater PASSIVE;
public static final BoilerHeater BLAZE_BURNER;
public static final SimpleRegistry<Block, BoilerHeater> REGISTRY;

public static float findHeat(Level, BlockPos, BlockState);
public abstract float getHeat(Level, BlockPos, BlockState);
```

```java
BoilerHeater.REGISTRY.register(MyBlocks.MY_BURNER.get(), (level, pos, state) ->
        state.getValue(MyBurnerBlock.LIT) ? 4.0f : BoilerHeater.NO_HEAT);
```

### 2.6 api.connectivity

**ConnectivityHandler**（`public class`）—— 多方块（流体罐、仓库等）的连接/拆解工具。

```java
public static <T extends BlockEntity & IMultiBlockEntityContainer> void formMulti(T);
public static <T extends BlockEntity & IMultiBlockEntityContainer> void splitMulti(T);
public static <T extends BlockEntity & IMultiBlockEntityContainer> T partAt(BlockEntityType<?>, BlockGetter, BlockPos);
public static <T extends BlockEntity & IMultiBlockEntityContainer> boolean isConnected(BlockGetter, BlockPos, BlockPos);
```

（内部类 `ConnectivityHandler$SearchCache` 的公开成员**未验证**。）

### 2.7 api.contraption

#### ContraptionType

```java
public final class com.simibubi.create.api.contraption.ContraptionType {
  public final java.util.function.Supplier<? extends Contraption> factory;
  public final Holder.Reference<ContraptionType> holder;
  public ContraptionType(Supplier<? extends Contraption> factory);
  public boolean is(TagKey<ContraptionType>);
  public static Contraption fromType(java.lang.String);
}
```

注册用 `CreateRegistries.CONTRAPTION_TYPE`（`ResourceKey<Registry<ContraptionType>>`）。

#### ContraptionMovementSetting

```java
public final class ... extends Enum<ContraptionMovementSetting> {
  public static final ContraptionMovementSetting MOVABLE;
  public static final ContraptionMovementSetting NO_PICKUP;
  public static final ContraptionMovementSetting UNMOVABLE;
  public static final SimpleRegistry<Block, Supplier<ContraptionMovementSetting>> REGISTRY;

  public static ContraptionMovementSetting get(BlockState);
  public static ContraptionMovementSetting get(Block);
  public static boolean anyAre(Collection<StructureTemplate.StructureBlockInfo>, ContraptionMovementSetting);
  public static boolean isNoPickup(Collection<StructureTemplate.StructureBlockInfo>);
}
```

内部接口 `ContraptionMovementSetting$MovementSettingProvider extends IForgeBlock`：
`public abstract ContraptionMovementSetting getContraptionMovementSetting();` —— 方块可以直接实现它而不用注册。

#### BlockMovementChecks

```java
public static void registerMovementNecessaryCheck(BlockMovementChecks.MovementNecessaryCheck);
public static void registerMovementAllowedCheck(BlockMovementChecks.MovementAllowedCheck);
public static void registerBrittleCheck(BlockMovementChecks.BrittleCheck);
public static void registerAttachedCheck(BlockMovementChecks.AttachedCheck);
public static void registerNotSupportiveCheck(BlockMovementChecks.NotSupportiveCheck);

public static boolean isMovementNecessary(BlockState, Level, BlockPos);
public static boolean isMovementAllowed(BlockState, Level, BlockPos);
public static boolean isBrittle(BlockState);
public static boolean isBlockAttachedTowards(BlockState, Level, BlockPos, Direction);
public static boolean isNotSupportive(BlockState, Direction);
```

五个回调接口都返回 `BlockMovementChecks$CheckResult`：

```java
public final class BlockMovementChecks$CheckResult extends Enum<CheckResult> {
  public static final CheckResult SUCCESS;
  public static final CheckResult FAIL;
  public static final CheckResult PASS;      // 交给下一个检查器
  public boolean toBoolean();          // PASS 会抛 IllegalStateException("PASS does not have a boolean value")
  public static CheckResult of(boolean);         // true -> SUCCESS, false -> FAIL
  public static CheckResult of(java.lang.Boolean); // null -> PASS
}
```

查询顺序（源码 `BlockMovementChecks` 类注释原文）：
"Each query will iterate all registered checks of that type in **reverse-registration order**. If a check returns
a non-PASS result, that is the result of the query. If no check catches a query, then a best-effort fallback is used."
注册是**线程安全**的（可以在并行 mod init 阶段调用）。

回调接口签名：

```java
interface MovementNecessaryCheck { CheckResult isMovementNecessary(BlockState, Level, BlockPos); }
interface MovementAllowedCheck   { CheckResult isMovementAllowed(BlockState, Level, BlockPos); }
interface BrittleCheck           { CheckResult isBrittle(BlockState); }
interface AttachedCheck          { CheckResult isBlockAttachedTowards(BlockState, Level, BlockPos, Direction); }
interface NotSupportiveCheck     { CheckResult isNotSupportive(BlockState, Direction); }
```

#### contraption.dispenser

```java
public interface MountedDispenseBehavior {
  public static final SimpleRegistry<Item, MountedDispenseBehavior> REGISTRY;
  public abstract ItemStack dispense(ItemStack, MovementContext, BlockPos);
  public static Vec3 getDispenserNormal(MovementContext);
  public static Direction getClosestFacingDirection(Vec3);
  public static void placeItemInInventory(ItemStack, MovementContext, BlockPos);
}

public class DefaultMountedDispenseBehavior implements MountedDispenseBehavior {
  public static final MountedDispenseBehavior INSTANCE;
  public ItemStack dispense(ItemStack, MovementContext, BlockPos);
  protected ItemStack execute(ItemStack, MovementContext, BlockPos, Vec3);
  protected void playSound(LevelAccessor, BlockPos);
  protected void playAnimation(LevelAccessor, BlockPos, Vec3);
  protected void playAnimation(LevelAccessor, BlockPos, Direction);
  public static void spawnItem(Level, ItemStack, int, Vec3, BlockPos, MovementContext);
}

public abstract class MountedProjectileDispenseBehavior extends DefaultMountedDispenseBehavior {
  protected ItemStack execute(ItemStack, MovementContext, BlockPos, Vec3);
  protected abstract Projectile getProjectile(Level, double, double, double, ItemStack);
  protected float getUncertainty();
  protected float getPower();
  public static MountedDispenseBehavior of(AbstractProjectileDispenseBehavior);
}

public class OptionalMountedDispenseBehavior extends DefaultMountedDispenseBehavior {
  protected final ItemStack execute(ItemStack, MovementContext, BlockPos, Vec3);
  protected ItemStack doExecute(ItemStack, MovementContext, BlockPos, Vec3);
}
```

```java
MountedDispenseBehavior.REGISTRY.register(MyItems.MY_WAND.get(), new OptionalMountedDispenseBehavior() {
    @Override
    protected ItemStack doExecute(ItemStack stack, MovementContext ctx, BlockPos pos, Vec3 normal) {
        // 自己决定要不要消耗 / 掉出来
        return stack;
    }
});
```

#### contraption.storage（item / fluid）

```java
public abstract class MountedItemStorage implements IItemHandlerModifiable {
  public static final Codec<MountedItemStorage> CODEC;
  public final MountedItemStorageType<? extends MountedItemStorage> type;
  protected MountedItemStorage(MountedItemStorageType<?>);
  public abstract void unmount(Level, BlockState, BlockPos, BlockEntity);
  public boolean handleInteraction(ServerPlayer, Contraption, StructureTemplate.StructureBlockInfo);
  protected IItemHandlerModifiable getHandlerForMenu(StructureTemplate.StructureBlockInfo, Contraption);
  protected boolean isMenuValid(ServerPlayer, Contraption, Vec3);
  protected Component getMenuName(StructureTemplate.StructureBlockInfo, Contraption);
  protected MenuProvider createMenuProvider(Component, IItemHandlerModifiable, Predicate<Player>, Consumer<Player>);
  protected void playOpeningSound(ServerLevel, Vec3);
  protected void playClosingSound(ServerLevel, Vec3);
}

public abstract class MountedItemStorageType<T extends MountedItemStorage> {
  public static final Codec<MountedItemStorageType<?>> CODEC;
  public static final SimpleRegistry<Block, MountedItemStorageType<?>> REGISTRY;
  public final Codec<? extends T> codec;
  public final Holder.Reference<MountedItemStorageType<?>> holder;
  protected MountedItemStorageType(Codec<? extends T>);
  public final boolean is(TagKey<MountedItemStorageType<?>>);
  public abstract T mount(Level, BlockState, BlockPos, BlockEntity);
  public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mountedItemStorage(RegistryEntry<? extends MountedItemStorageType<?>>);
}
```

`MountedFluidStorage` / `MountedFluidStorageType` 与之完全对称（`implements IFluidHandler`、`mount(...)`、`mountedFluidStorage(...)`）。

便捷实现：

```java
public abstract class WrapperMountedItemStorage<T extends IItemHandlerModifiable> extends MountedItemStorage {
  protected final T wrapped;
  protected WrapperMountedItemStorage(MountedItemStorageType<?>, T);
  // 转发 setStackInSlot / getSlots / getStackInSlot / insertItem / extractItem / getSlotLimit / isItemValid
  public static ItemStackHandler copyToItemStackHandler(IItemHandler);
}

public class SimpleMountedStorage extends WrapperMountedItemStorage<ItemStackHandler> {
  public static final Codec<SimpleMountedStorage> CODEC;
  public SimpleMountedStorage(MountedItemStorageType<?>, IItemHandler);
  public SimpleMountedStorage(IItemHandler);
  public void unmount(Level, BlockState, BlockPos, BlockEntity);
  protected Optional<IItemHandlerModifiable> validate(IItemHandler);
  public static <T extends SimpleMountedStorage> Codec<T> codec(Function<IItemHandler, T>);
}

public abstract class SimpleMountedStorageType<T extends SimpleMountedStorage> extends MountedItemStorageType<SimpleMountedStorage> {
  protected SimpleMountedStorageType(Codec<T>);
  public SimpleMountedStorage mount(Level, BlockState, BlockPos, BlockEntity);
  protected IItemHandler getHandler(BlockEntity);
  protected SimpleMountedStorage createStorage(IItemHandler);
}

public class ChestMountedStorage extends SimpleMountedStorage { /* CODEC + 双箱逻辑 */ }
public class ChestMountedStorageType extends SimpleMountedStorageType<ChestMountedStorage> { /* getHandler / createStorage */ }

public class MountedItemStorageWrapper extends CombinedInvWrapper {
  public final ImmutableMap<BlockPos, MountedItemStorage> storages;
  public MountedItemStorageWrapper(ImmutableMap<BlockPos, MountedItemStorage>);
}
```

菜单辅助：

```java
public class MountedStorageMenus {
  public static final java.util.List<MenuType<?>> GENERIC_CHEST_MENUS;
  public static MenuProvider createGeneric(Component, IItemHandlerModifiable, Predicate<Player>, Consumer<Player>);
  public static MenuProvider createGeneric9x9(Component, IItemHandlerModifiable, Predicate<Player>, Consumer<Player>);
}

public class StorageInteractionWrapper extends RecipeWrapper {
  public StorageInteractionWrapper(IItemHandlerModifiable, Predicate<Player>, Consumer<Player>);
  public boolean stillValid(Player);
  public int getMaxStackSize();
  public void stopOpen(Player);
}
```

同步接口：

```java
public interface SyncedMountedStorage {
  boolean isDirty();
  void markClean();
  void afterSync(Contraption, BlockPos);
}
```

```java
// 让我的容器在装置上能打开
public class MyChestStorageType extends SimpleMountedStorageType<SimpleMountedStorage> {
    public MyChestStorageType() {
        super(SimpleMountedStorage.codec(SimpleMountedStorage::new));
    }

    @Override
    protected IItemHandler getHandler(BlockEntity be) {
        return ((MyChestBlockEntity) be).getInventory();
    }
}
```

#### contraption.train

```java
public interface PortalTrackProvider {
  public static final SimpleRegistry<Block, PortalTrackProvider> REGISTRY;
  public abstract PortalTrackProvider.Exit findExit(ServerLevel, net.createmod.catnip.math.BlockFace);
  public static boolean isSupportedPortal(BlockState);
  public static PortalTrackProvider.Exit getOtherSide(ServerLevel, BlockFace);
  public static PortalTrackProvider.Exit fromTeleporter(ServerLevel, BlockFace, ResourceKey<Level>, ResourceKey<Level>, Function<ServerLevel, ITeleporter>);
  public static PortalTrackProvider.Exit fromProbe(ServerLevel, BlockFace, ResourceKey<Level>, ResourceKey<Level>, BiFunction<ServerLevel, SuperGlueEntity, PortalInfo>);
}

public final class PortalTrackProvider$Exit extends Record {
  public Exit(ServerLevel, BlockFace);
  public ServerLevel level();
  public BlockFace face();
}
```

#### contraption.transformable

```java
public interface TransformableBlock {
  BlockState transform(BlockState, StructureTransform);
}
public interface TransformableBlockEntity {
  void transform(BlockEntity, StructureTransform);
}
public class MovedBlockTransformerRegistries {
  public static final SimpleRegistry<Block, MovedBlockTransformerRegistries.BlockTransformer> BLOCK_TRANSFORMERS;
  public static final SimpleRegistry<BlockEntityType<?>, MovedBlockTransformerRegistries.BlockEntityTransformer> BLOCK_ENTITY_TRANSFORMERS;
}
// BlockTransformer / BlockEntityTransformer 的签名与上面两个接口完全一致
```

**综合示例**：

```java
// 让我的方块在装置上不被推动
ContraptionMovementSetting.REGISTRY.register(MyBlocks.MY_ANCHOR.get(),
        () -> ContraptionMovementSetting.UNMOVABLE);

// 允许我的方块被 contraption 采集
BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
        state.is(MyBlocks.MY_COLLECTABLE.get())
                ? BlockMovementChecks.CheckResult.SUCCESS
                : BlockMovementChecks.CheckResult.PASS);

// 在装置世界里旋转时修正我的 BE
public class MyBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
    @Override
    public void transform(BlockEntity be, StructureTransform transform) {
        // 修正自己缓存的朝向
    }
}
```

### 2.8 api.data 与 api.data.recipe

**api.data.TrainHatInfoProvider**：

```java
public abstract class com.simibubi.create.api.data.TrainHatInfoProvider implements net.minecraft.data.DataProvider {
  protected final java.util.Map<ResourceLocation, TrainHatInfo> trainHatOffsets;
  public TrainHatInfoProvider(PackOutput);
  protected abstract void createOffsets();
  protected void makeInfoFor(EntityType<?>, Vec3);
  protected void makeInfoFor(EntityType<?>, Vec3, java.lang.String);
  protected void makeInfoFor(EntityType<?>, Vec3, float);
  protected void makeInfoFor(EntityType<?>, Vec3, java.lang.String, float);
  protected void makeInfoFor(EntityType<?>, Vec3, java.lang.String, int, float);
  public CompletableFuture<?> run(CachedOutput);
  public java.lang.String getName();
}
```

**api.data.recipe**（19 个类）—— Create 的配方 datagen 基类。

`BaseRecipeProvider`：

```java
public abstract class BaseRecipeProvider extends net.minecraft.data.recipes.RecipeProvider {
  protected final java.lang.String modid;
  protected final java.util.List<BaseRecipeProvider.GeneratedRecipe> all;
  public BaseRecipeProvider(PackOutput, java.lang.String);   // (输出, 你的 modid)
  protected ResourceLocation asResource(java.lang.String);
  protected GeneratedRecipe register(GeneratedRecipe);
  protected void buildRecipes(Consumer<FinishedRecipe>);
}
public interface BaseRecipeProvider$GeneratedRecipe {
  void register(Consumer<FinishedRecipe>);
}
```

`ProcessingRecipeGen`（所有加工配方的共同父类）：

```java
public abstract class ProcessingRecipeGen extends BaseRecipeProvider {
  public ProcessingRecipeGen(PackOutput, java.lang.String);

  protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(
      java.lang.String name, Supplier<ItemLike> ing, UnaryOperator<ProcessingRecipeBuilder<T>> op);
  protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(
      java.lang.String name, UnaryOperator<ProcessingRecipeBuilder<T>> op);
  protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(
      Supplier<ItemLike> ing, UnaryOperator<ProcessingRecipeBuilder<T>> op);
  protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(
      ResourceLocation id, UnaryOperator<ProcessingRecipeBuilder<T>> op);
  protected <T extends ProcessingRecipe<?>> GeneratedRecipe createWithDeferredId(
      Supplier<ResourceLocation>, UnaryOperator<ProcessingRecipeBuilder<T>> op);

  protected abstract IRecipeTypeInfo getRecipeType();
  protected <T extends ProcessingRecipe<?>> ProcessingRecipeSerializer<T> getSerializer();
  protected Supplier<ResourceLocation> idWithSuffix(Supplier<ItemLike>, java.lang.String);
  public java.lang.String getName();
}
```

各具体生成器（都是 `public abstract class ... extends ProcessingRecipeGen`，构造函数都是
`public XxxRecipeGen(PackOutput output, java.lang.String modid)`，且都已实现 `getRecipeType()`）：

> ⚠️ `ProcessingRecipeGen.create(String, Supplier<ItemLike>, ...)` 的 `String` 参数是 **namespace**，
> 不是配方名 —— 配方 id 取的是**物品的注册名**（源码：`new ResourceLocation(namespace, ...getKeyOrThrow(itemLike.asItem()).getPath())`）。
> 想要自定义名字用 `create(String name, UnaryOperator)`。详见 7.4 节表格。

| 类 | 对应 AllRecipeTypes | 额外辅助方法（节选） |
|---|---|---|
| `CrushingRecipeGen` | `CRUSHING` | `stoneOre`、`deepslateOre`、`netherOre`、`ore(...)`、`rawOre(...)`、`rawOreBlock(...)`、`moddedOre(CommonMetal, Supplier)`、`mineralRecycling(...)` |
| `CuttingRecipeGen` | `CUTTING` | `stripAndMakePlanks(Block, Block, Block)`、`stripAndMakePlanks(..., int)`、`cuttingCompat(DatagenMod, String...)`、`cuttingCompatLogOnly(...)` |
| `MillingRecipeGen` | `MILLING` | `metalOre(String, ItemEntry<? extends Item>, int)`、`moddedSandstone(DatagenMod, String)` |
| `MixingRecipeGen` | `MIXING` | `moddedMud(DatagenMod, String)` |
| `CompactingRecipeGen` | `COMPACTING` | — |
| `PressingRecipeGen` | `PRESSING` | `moddedCompacting(...)`、`moddedPaths(...)` |
| `PolishingRecipeGen` | `SANDPAPER_POLISHING` | — |
| `HauntingRecipeGen` | `HAUNTING` | `public GeneratedRecipe convert(ItemLike, ItemLike)`、`convert(Supplier<Ingredient>, Supplier<ItemLike>)` |
| `DeployingRecipeGen` | `DEPLOYING` | `public GeneratedRecipe copperChain(CopperBlockSet)`、`addWax(Supplier, Supplier)`、`oxidizationChain(List<Supplier<ItemLike>>)` |
| `FillingRecipeGen` | `FILLING` | `moddedGrass(DatagenMod, String)` |
| `EmptyingRecipeGen` | `EMPTYING` | — |
| `ItemApplicationRecipeGen` | `ITEM_APPLICATION` | `woodCasing(...)`、`woodCasingTag(...)`、`woodCasingIngredient(...)` |
| `WashingRecipeGen` | `SPLASHING` | `public GeneratedRecipe convert(Block, Block)`、`crushedOre(ItemEntry<Item>, Supplier, Supplier, float)` |
| `SequencedAssemblyRecipeGen` | `SEQUENCED_ASSEMBLY` | `protected GeneratedRecipe create(String, UnaryOperator<SequencedAssemblyRecipeBuilder>)` |
| `MechanicalCraftingRecipeGen` | `MECHANICAL_CRAFTING` | `protected GeneratedRecipeBuilder create(com.google.common.base.Supplier<ItemLike>)` → `returns(int)` / `withSuffix(String)` / `recipe(UnaryOperator<MechanicalCraftingRecipeBuilder>)` |

`MechanicalCraftingRecipeBuilder`（可脱离上面的 Gen 单独使用）：

```java
public MechanicalCraftingRecipeBuilder(ItemLike result, int count);
public static MechanicalCraftingRecipeBuilder shapedRecipe(ItemLike);
public static MechanicalCraftingRecipeBuilder shapedRecipe(ItemLike, int);
public MechanicalCraftingRecipeBuilder key(char, TagKey<Item>);
public MechanicalCraftingRecipeBuilder key(char, ItemLike);
public MechanicalCraftingRecipeBuilder key(char, Ingredient);
public MechanicalCraftingRecipeBuilder patternLine(java.lang.String);
public MechanicalCraftingRecipeBuilder disallowMirrored();
public void build(Consumer<FinishedRecipe>);
public void build(Consumer<FinishedRecipe>, java.lang.String);
public void build(Consumer<FinishedRecipe>, ResourceLocation);
public MechanicalCraftingRecipeBuilder whenModLoaded(java.lang.String);
public MechanicalCraftingRecipeBuilder whenModMissing(java.lang.String);
public MechanicalCraftingRecipeBuilder withCondition(ICondition);
```

`DatagenMod`（配合 `require(DatagenMod, String)` / `output(DatagenMod, String)` 使用）：

```java
public interface DatagenMod {
  default ResourceLocation asResource(java.lang.String);
  default java.lang.String recipeId(java.lang.String);
  java.lang.String getId();
  default ResourceLocation ingotOf(java.lang.String);
  default ResourceLocation nuggetOf(java.lang.String);
  default ResourceLocation oreOf(java.lang.String);
  default ResourceLocation deepslateOreOf(java.lang.String);
  default boolean reversedMetalPrefix();
  default boolean strippedIsSuffix();
  default boolean omitWoodSuffix();
}
```

---

### 2.9 api.effect

**OpenPipeEffectHandler**（`public interface`）—— 开口管道把流体洒到世界上时的效果。

```java
public static final SimpleRegistry<Fluid, OpenPipeEffectHandler> REGISTRY;
public abstract void apply(Level, AABB, FluidStack);
```

```java
OpenPipeEffectHandler.REGISTRY.register(MyFluids.MY_FLUID.get(),
        (level, area, stack) -> { /* 在 area 范围内产生效果 */ });
```

### 2.10 api.equipment.goggles

```java
public interface IHaveCustomOverlayIcon {
  default ItemStack getIcon(boolean isPlayerSneaking);
}

public interface IHaveGoggleInformation extends IHaveCustomOverlayIcon {
  default boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking);
  default boolean containedFluidTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking,
                                        LazyOptional<IFluidHandler> handler);
}

public interface IHaveHoveringInformation extends IHaveCustomOverlayIcon {
  default boolean addToTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking);
}

public interface IProxyHoveringInformation {
  BlockPos getInformationSource(Level, BlockPos, BlockState);
}
```

细节和示例见第 6 节。

### 2.11 api.equipment.potatoCannon

```java
public final class PotatoCannonProjectileType extends Record {
  public static final Codec<PotatoCannonProjectileType> CODEC;
  public PotatoCannonProjectileType(HolderSet<Item>, int reloadTicks, int damage, int split,
      float knockback, float drag, float velocityMultiplier, float gravityMultiplier, float soundPitch,
      boolean sticky, ItemStack dropStack, PotatoProjectileRenderMode,
      Optional<PotatoProjectileEntityHitAction> preEntityHit,
      Optional<PotatoProjectileEntityHitAction> onEntityHit,
      Optional<PotatoProjectileBlockHitAction> onBlockHit);

  public static Optional<Holder.Reference<PotatoCannonProjectileType>> getTypeForItem(RegistryAccess, Item);
  public boolean preEntityHit(ItemStack, EntityHitResult);
  public boolean onEntityHit(ItemStack, EntityHitResult);
  public boolean onBlockHit(LevelAccessor, ItemStack, BlockHitResult);
  public ItemStack dropStack();
  // 访问器：items() reloadTicks() damage() split() knockback() drag() velocityMultiplier()
  //         gravityMultiplier() soundPitch() sticky() renderMode() preEntityHit() onEntityHit() onBlockHit()
}

public class PotatoCannonProjectileType$Builder {
  public Builder reloadTicks(int);   public Builder damage(int);     public Builder splitInto(int);
  public Builder knockback(float);   public Builder drag(float);     public Builder velocity(float);
  public Builder gravity(float);     public Builder soundPitch(float);
  public Builder sticky();           public Builder dropStack(ItemStack);
  public Builder renderMode(PotatoProjectileRenderMode);
  public Builder renderBillboard();  public Builder renderTumbling();
  public Builder renderTowardMotion(int, float);
  public Builder preEntityHit(PotatoProjectileEntityHitAction);
  public Builder onEntityHit(PotatoProjectileEntityHitAction);
  public Builder onBlockHit(PotatoProjectileBlockHitAction);
  public Builder addItems(ItemLike...);
  public PotatoCannonProjectileType build();
}

public interface PotatoProjectileRenderMode {
  public static final Codec<PotatoProjectileRenderMode> CODEC;
  void transform(PoseStack, PotatoProjectileEntity, float);
  Codec<? extends PotatoProjectileRenderMode> codec();
}

public interface PotatoProjectileEntityHitAction {
  public static final Codec<PotatoProjectileEntityHitAction> CODEC;
  boolean execute(ItemStack, EntityHitResult, PotatoProjectileEntityHitAction.Type);
  Codec<? extends PotatoProjectileEntityHitAction> codec();
}

public interface PotatoProjectileBlockHitAction {
  public static final Codec<PotatoProjectileBlockHitAction> CODEC;
  boolean execute(LevelAccessor, ItemStack, BlockHitResult);
  Codec<? extends PotatoProjectileBlockHitAction> codec();
}
```

注册表是数据驱动注册表 `CreateRegistries.POTATO_PROJECTILE_TYPE`
（`ResourceKey<Registry<PotatoCannonProjectileType>>`），即走 datapack JSON，不是 SimpleRegistry。

### 2.12 api.event

```java
public class BlockEntityBehaviourEvent<T extends SmartBlockEntity> extends GenericEvent<T> {
  public BlockEntityBehaviourEvent(T, java.util.Map<BehaviourType<?>, BlockEntityBehaviour>);
  public java.lang.reflect.Type getGenericType();
  public void attach(BlockEntityBehaviour);
  public BlockEntityBehaviour remove(BehaviourType<?>);
  public T getBlockEntity();
  public BlockState getBlockState();
}

public class PipeCollisionEvent extends Event {
  public Level getLevel();
  public BlockPos getPos();
  public BlockState getState();
  public void setState(BlockState);
}
// 子类 PipeCollisionEvent.Flow： getFirstFluid() / getSecondFluid()
// 子类 PipeCollisionEvent.Spill：getWorldFluid() / getPipeFluid()

public class TrackGraphMergeEvent extends Event {
  public TrackGraphMergeEvent(TrackGraph, TrackGraph);
  public TrackGraph getGraphMergedInto();
  public TrackGraph getGraphMergedFrom();
}
```

`BlockEntityBehaviourEvent` 由 `SmartBlockEntity` 在初始化时 **post 到 `MinecraftForge.EVENT_BUS`**
（已用 `javap -c` 在其字节码中确认 `new BlockEntityBehaviourEvent(be, behaviours)` → `IEventBus.post`）。
因为它是 `GenericEvent`，必须用 `addGenericListener` 注册（Forge EventBus 6.2.33 实测签名）：

```java
public <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F>, Consumer<T>);
```

用法见 5.6 节。

### 2.13 api.packager

```java
public interface InventoryIdentifier {
  public static final SimpleRegistry<Block, InventoryIdentifier.Finder> REGISTRY;
  boolean contains(net.createmod.catnip.math.BlockFace);
  public static InventoryIdentifier get(Level, BlockFace);
}
public interface InventoryIdentifier$Finder {
  InventoryIdentifier find(Level, BlockState, BlockFace);
}
public final class InventoryIdentifier$Single   extends Record implements InventoryIdentifier { Single(BlockPos);  BlockPos pos(); }
public final class InventoryIdentifier$Pair     extends Record implements InventoryIdentifier { Pair(BlockPos, BlockPos); BlockPos first(); BlockPos second(); }
public final class InventoryIdentifier$Bounds   extends Record implements InventoryIdentifier { Bounds(BoundingBox); BoundingBox bounds(); }
public final class InventoryIdentifier$MultiFace extends Record implements InventoryIdentifier { MultiFace(BlockPos, java.util.Set<Direction>); BlockPos pos(); java.util.Set<Direction> sides(); }

public interface UnpackingHandler {
  public static final SimpleRegistry<Block, UnpackingHandler> REGISTRY;
  public static final UnpackingHandler DEFAULT;
  boolean unpack(Level, BlockPos, BlockState, Direction, java.util.List<ItemStack>, PackageOrderWithCrafts, boolean);
}
public final class VoidingUnpackingHandler extends Enum<VoidingUnpackingHandler> implements UnpackingHandler {
  public static final VoidingUnpackingHandler INSTANCE;
  public boolean unpack(Level, BlockPos, BlockState, Direction, java.util.List<ItemStack>, PackageOrderWithCrafts, boolean);
}
```

```java
// 把一个双箱登记成一个"库存标识"（同一个库存的两个面）
InventoryIdentifier.REGISTRY.register(MyBlocks.MY_DOUBLE_CHEST.get(),
        (level, state, face) -> new InventoryIdentifier.Pair(face.getPos(), face.getPos().relative(face.getFace())));

// 我的方块不接受包裹：直接销毁内容
UnpackingHandler.REGISTRY.register(MyBlocks.MY_VOID.get(), VoidingUnpackingHandler.INSTANCE);
```

### 2.14 api.registrate

**CreateRegistrateRegistrationCallback**：

```java
public static <R, T extends R> void register(ResourceKey<? extends Registry<R>>, ResourceLocation, NonNullConsumer<? super T>);
public static void provideRegistrate(CreateRegistrate);
```

用途：在 `CreateRegistrate` 实例还没建立之前先把「注册回调」登记下来，等 `provideRegistrate(...)` 时统一执行。

源码确认：**`CreateRegistrate.create(String)` 内部就会调用 `CreateRegistrateRegistrationCallback.provideRegistrate(registrate)`**，
所以 addon 不需要（也不应该）自己再调一次 `provideRegistrate`；你只需要在注册前用 `register(...)` 登记回调。

**registry.registrate.SimpleBuilder**（`public class ... extends AbstractBuilder`）：

```java
public SimpleBuilder(AbstractRegistrate<?>, P parent, java.lang.String name, BuilderCallback,
                     ResourceKey<Registry<R>>, Supplier<T>);
protected T createEntry();
public SimpleBuilder<R, T, P> byBlock(SimpleRegistry<Block, R>);
public SimpleBuilder<R, T, P> byBlock(SimpleRegistry.Multi<Block, R>);
public SimpleBuilder<R, T, P> byBlockEntity(SimpleRegistry<BlockEntityType<?>, R>);
public SimpleBuilder<R, T, P> byBlockEntity(SimpleRegistry.Multi<BlockEntityType<?>, R>);
public SimpleBuilder<R, T, P> byEntity(SimpleRegistry<EntityType<?>, R>);
public SimpleBuilder<R, T, P> byEntity(SimpleRegistry.Multi<EntityType<?>, R>);
public SimpleBuilder<R, T, P> byFluid(SimpleRegistry<Fluid, R>);
public SimpleBuilder<R, T, P> byFluid(SimpleRegistry.Multi<Fluid, R>);
public SimpleBuilder<R, T, P> associate(Block);
public SimpleBuilder<R, T, P> associateBlockTag(TagKey<Block>);
public SimpleBuilder<R, T, P> associate(BlockEntityType<?>);
public SimpleBuilder<R, T, P> associateBeTag(TagKey<BlockEntityType<?>>);
public SimpleBuilder<R, T, P> associate(EntityType<?>);
public SimpleBuilder<R, T, P> associateEntityTag(TagKey<EntityType<?>>);
public SimpleBuilder<R, T, P> associate(Fluid);
public SimpleBuilder<R, T, P> associateFluidTag(TagKey<Fluid>);
```

> 用法要点（源码确认）：`byBlock(...)` / `byBlockEntity(...)` 等是**前置开关**，必须先调用它，
> 之后的 `associate(...)` / `associate*Tag(...)` 才允许；否则 `assertPresent` 会抛
> `IllegalStateException("This type does not support Block associations")`。
> `associate*` 系列内部走的是 `onRegister(...)`，即**等这个 entry 自己注册完成后再执行**。
> `CreateRegistrate.displaySource(...)` / `displayTarget(...)` 已经替你调好了 `byBlock` + `byBlockEntity`。

### 2.15 api.registry

```java
public interface SimpleRegistry<K, V> {
  void register(K, V);
  void registerProvider(SimpleRegistry.Provider<K, V>);
  void invalidate();
  V get(K);
  V get(StateHolder<K, ?>);
  public static <K, V> SimpleRegistry<K, V> create();
}

public interface SimpleRegistry$Multi<K, V> extends SimpleRegistry<K, java.util.List<V>> {
  void add(K, V);
  void addProvider(SimpleRegistry.Provider<K, V>);
  java.util.List<V> get(K);
  java.util.List<V> get(StateHolder<K, ?>);
  public static <K, V> SimpleRegistry.Multi<K, V> create();
}

> 已从实现类 `com.simibubi.create.impl.registry.SimpleRegistryImpl` 的源码确认：
> - `register(K,V)` / `add(K,V)` 对**同一个 key 重复注册会抛 `IllegalArgumentException`**（"Tried to register duplicate values for object ..."）。
> - 单值查表时**先查直接注册的值，再问 provider**；provider 结果会被缓存，直到 `invalidate()`。
> - `Multi` 的 `get` 返回**不可变 List**：直接注册的值在前，provider 提供的值在后。
> - `registerProvider` 把 provider **插到列表开头**，所以 provider 之间是"后注册的先被问"。
> - 注册方法都是 `synchronized` 的，可以在并行 mod 加载阶段调用。

public interface SimpleRegistry$Provider<K, V> {
  V get(K);
  default void onRegister(java.lang.Runnable);
  public static <K, V> Provider<K, V> forTag(TagKey<K>, Function<K, Holder<K>>, V);
  public static <V> Provider<Block, V> forBlockTag(TagKey<Block>, V);
  public static <V> Provider<BlockEntityType<?>, V> forBlockEntityTag(TagKey<BlockEntityType<?>>, V);
  public static <V> Provider<Item, V> forItemTag(TagKey<Item>, V);
  public static <V> Provider<EntityType<?>, V> forEntityTag(TagKey<EntityType<?>>, V);
  public static <V> Provider<Fluid, V> forFluidTag(TagKey<Fluid>, V);
}
```

`CreateRegistries`（`ResourceKey`，用于 `DeferredRegister.create(...)`）：

```java
public static final ResourceKey<Registry<ArmInteractionPointType>> ARM_INTERACTION_POINT_TYPE;
public static final ResourceKey<Registry<FanProcessingType>>        FAN_PROCESSING_TYPE;
public static final ResourceKey<Registry<ItemAttributeType>>        ITEM_ATTRIBUTE_TYPE;
public static final ResourceKey<Registry<DisplaySource>>            DISPLAY_SOURCE;
public static final ResourceKey<Registry<DisplayTarget>>            DISPLAY_TARGET;
public static final ResourceKey<Registry<MountedItemStorageType<?>>>  MOUNTED_ITEM_STORAGE_TYPE;
public static final ResourceKey<Registry<MountedFluidStorageType<?>>> MOUNTED_FLUID_STORAGE_TYPE;
public static final ResourceKey<Registry<ContraptionType>>          CONTRAPTION_TYPE;
public static final ResourceKey<Registry<PotatoCannonProjectileType>> POTATO_PROJECTILE_TYPE;
public static final ResourceKey<Registry<Codec<? extends PotatoProjectileRenderMode>>>      POTATO_PROJECTILE_RENDER_MODE;
public static final ResourceKey<Registry<Codec<? extends PotatoProjectileEntityHitAction>>> POTATO_PROJECTILE_ENTITY_HIT_ACTION;
public static final ResourceKey<Registry<Codec<? extends PotatoProjectileBlockHitAction>>>  POTATO_PROJECTILE_BLOCK_HIT_ACTION;
```

`CreateBuiltInRegistries` 是对应的**已构建注册表实例**（`public static final Registry<...>` 同名字段），
外加 `public static void init();`。

```java
// 典型的 addon 注册写法
public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
        DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, "croety");

// 给全部包了机壳的方块统一登记
DisplaySource.BY_BLOCK.addProvider(SimpleRegistry.Provider.forBlockTag(
        AllTags.AllBlockTags.CASING.tag, MY_SOURCE.get()));
```

### 2.16 api.schematic

```java
// schematic.nbt
public interface PartialSafeNBT { void writeSafe(CompoundTag); }

public class SafeNbtWriterRegistry {
  public static final SimpleRegistry<BlockEntityType<?>, SafeNbtWriterRegistry.SafeNbtWriter> REGISTRY;
}
public interface SafeNbtWriterRegistry$SafeNbtWriter {
  void writeSafe(BlockEntity, CompoundTag);
}

// schematic.requirement
public interface SpecialBlockItemRequirement {
  ItemRequirement getRequiredItems(BlockState, BlockEntity);
}
public interface SpecialBlockEntityItemRequirement {   // SmartBlockEntity 已经实现它
  ItemRequirement getRequiredItems(BlockState);
}
public interface SpecialEntityItemRequirement {
  ItemRequirement getRequiredItems();
}
public class SchematicRequirementRegistries {
  public static final SimpleRegistry<Block, SchematicRequirementRegistries.BlockRequirement> BLOCKS;
  public static final SimpleRegistry<BlockEntityType<?>, SchematicRequirementRegistries.BlockEntityRequirement> BLOCK_ENTITIES;
  public static final SimpleRegistry<EntityType<?>, SchematicRequirementRegistries.EntityRequirement> ENTITIES;
}
interface BlockRequirement       { ItemRequirement getRequiredItems(BlockState, BlockEntity); }
interface BlockEntityRequirement { ItemRequirement getRequiredItems(BlockEntity, BlockState); }
interface EntityRequirement      { ItemRequirement getRequiredItems(Entity); }

// schematic.state
public interface SchematicStateFilter {
  BlockState filterStates(BlockEntity, BlockState);
}
public class SchematicStateFilterRegistry {
  public static final SimpleRegistry<Block, SchematicStateFilterRegistry.StateFilter> REGISTRY;
}
interface StateFilter { BlockState filterStates(BlockEntity, BlockState); }
```

`ItemRequirement` 位于 `com.simibubi.create.content.schematics.requirement.ItemRequirement`（不在 api 包）。

```java
// 蓝图里不要保存"运行中"这个状态
SchematicStateFilterRegistry.REGISTRY.register(MyBlocks.MY_BLOCK.get(),
        (be, state) -> state.setValue(MyBlock.ACTIVE, false));
```

### 2.17 api.stress

**BlockStressValues** —— 动能机器的应力声明入口，第 4 节的核心。

```java
public class com.simibubi.create.api.stress.BlockStressValues {
  public static final SimpleRegistry<Block, java.util.function.DoubleSupplier> IMPACTS;
  public static final SimpleRegistry<Block, java.util.function.DoubleSupplier> CAPACITIES;
  public static final SimpleRegistry<Block, BlockStressValues.GeneratedRpm> RPM;

  public static double getImpact(Block);
  public static double getCapacity(Block);

  public static NonNullConsumer<Block> setGeneratorSpeed(int speed);
  public static NonNullConsumer<Block> setGeneratorSpeed(int speed, boolean mayGenerateLess);
}

public final class BlockStressValues$GeneratedRpm extends Record {
  public GeneratedRpm(int value, boolean mayGenerateLess);
  public int value();
  public boolean mayGenerateLess();
}
```

源码里这三个注册表的原始注释（直接引用）：

- `IMPACTS` —— "Registry for suppliers of stress impacts. **Determine the base impact at 1 RPM.**"
- `CAPACITIES` —— "Registry for suppliers of stress capacities. **Determine the base capacity at 1 RPM.**"
- `RPM` —— "Registry for generator RPM values. **This is only used for tooltips; actual functionality is determined by the block.**"

`getImpact(Block)` / `getCapacity(Block)` 在**没有注册值时返回 0**（不是抛异常）。
`setGeneratorSpeed(int)` 的实现是 `block -> RPM.register(block, new GeneratedRpm(value, false))` —— 也就是**只影响 tooltip**，
真正的转速要靠方块/BE 的 `getGeneratedSpeed()`。

Create 自己是**通过 config** 填这两个表的（`com.simibubi.create.infrastructure.config.AllConfigs`）：
```java
BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
```
对应类 `CStress` 只能给 Create 自己的方块用：它的 `setImpact/setCapacity/setNoImpact` 内部有
`assertFromCreate(builder)`，不是 Create 的方块会抛
`IllegalStateException("Non-Create blocks cannot be added to Create's config.")`。
**addon 不要碰 `CStress`，直接用 `BlockStressValues.IMPACTS.register(...)`。**

---

## 3. 注册方块与物品（CreateRegistrate）

### 3.1 com.simibubi.create.foundation.data.CreateRegistrate

```java
public class CreateRegistrate extends AbstractRegistrate<CreateRegistrate> {
  protected CreateRegistrate(java.lang.String modid);                  // protected！
  public static CreateRegistrate create(java.lang.String modid);       // ← addon 用这个

  public static boolean isInCreativeTab(RegistryEntry<?>, RegistryObject<CreativeModeTab>);
  public CreateRegistrate setTooltipModifierFactory(Function<Item, TooltipModifier>);
  public Function<Item, TooltipModifier> getTooltipModifierFactory();
  public CreateRegistrate setCreativeTab(RegistryObject<CreativeModeTab>);
  public RegistryObject<CreativeModeTab> getCreativeTab();
  public CreateRegistrate registerEventListeners(IEventBus);           // ← 必须挂到 mod 事件总线

  public <T extends BlockEntity> CreateBlockEntityBuilder<T, CreateRegistrate> blockEntity(String, BlockEntityBuilder.BlockEntityFactory<T>);
  public <T extends BlockEntity, P> CreateBlockEntityBuilder<T, P> blockEntity(P, String, BlockEntityBuilder.BlockEntityFactory<T>);
  public <T extends Entity> CreateEntityBuilder<T, CreateRegistrate> entity(String, EntityType.EntityFactory<T>, MobCategory);
  public <T extends Entity, P> CreateEntityBuilder<T, P> entity(P, String, EntityType.EntityFactory<T>, MobCategory);

  public <T extends MountedItemStorageType<?>> SimpleBuilder<MountedItemStorageType<?>, T, CreateRegistrate> mountedItemStorage(String, Supplier<T>);
  public <T extends MountedFluidStorageType<?>> SimpleBuilder<MountedFluidStorageType<?>, T, CreateRegistrate> mountedFluidStorage(String, Supplier<T>);
  public <T extends DisplaySource> SimpleBuilder<DisplaySource, T, CreateRegistrate> displaySource(String, Supplier<T>);
  public <T extends DisplayTarget> SimpleBuilder<DisplayTarget, T, CreateRegistrate> displayTarget(String, Supplier<T>);

  public <T extends Block> BlockBuilder<T, CreateRegistrate> paletteStoneBlock(String, NonNullFunction<BlockBehaviour.Properties, T>, NonNullSupplier<Block>, boolean, boolean);
  public BlockBuilder<Block, CreateRegistrate> paletteStoneBlock(String, NonNullSupplier<Block>, boolean, boolean);

  public <T extends ForgeFlowingFluid> FluidBuilder<T, CreateRegistrate> virtualFluid(String, ResourceLocation, ResourceLocation, FluidBuilder.FluidTypeFactory, NonNullFunction<ForgeFlowingFluid.Properties, T>, NonNullFunction<ForgeFlowingFluid.Properties, T>);
  public FluidBuilder<VirtualFluid, CreateRegistrate> virtualFluid(String);
  public FluidBuilder<ForgeFlowingFluid.Flowing, CreateRegistrate> standardFluid(String);

  public static FluidType defaultFluidType(FluidType.Properties, ResourceLocation, ResourceLocation);
  public static <T extends Block> NonNullConsumer<? super T> casingConnectivity(BiConsumer<T, CasingConnectivity>);
  public static <T extends Block> NonNullConsumer<? super T> blockModel(Supplier<NonNullFunction<BakedModel, ? extends BakedModel>>);
  public static <T extends Item>  NonNullConsumer<? super T> itemModel(Supplier<NonNullFunction<BakedModel, ? extends BakedModel>>);
  public static <T extends Block> NonNullConsumer<? super T> connectedTextures(Supplier<ConnectedTextureBehaviour>);
}
```

> ⚠️ `setCreativeTab(...)` **不会**把物品放进那个标签页。源码：`accept(...)` 只是把
> `entry -> currentTab` 记进 `CreateRegistrate.TAB_LOOKUP`，唯一读者是 `CreateRegistrate.isInCreativeTab(entry, tab)`。
> 而 Create 自己的 `AllCreativeModeTabs` 是用 `Create.registrate().getAll(Registries.BLOCK/ITEM)` **遍历 Create 自己的 registrate**
> 并配合 `isInCreativeTab` 来填充标签页的 —— addon 的 entry 不在那个 registrate 里，所以永远进不了 Create 的标签页。
> 想让 addon 物品出现在 Create 标签页，用 Forge 的 `BuildCreativeModeTabContentsEvent`：
>
> ```java
> @SubscribeEvent
> public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
>     if (event.getTabKey().equals(AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey())) {
>         event.accept(MyItems.MY_ITEM);                 // accept(Supplier<? extends ItemLike>)
>     }
> }
> ```
>
> 自己新建一个标签页则用 Registrate 的 `defaultCreativeTab(ResourceKey<CreativeModeTab>)`（注意它是 `AbstractRegistrate` 上的方法）。

从 `AbstractRegistrate<?>` 继承、写 addon 时必用的方法（`S` = `CreateRegistrate`）：

```java
public <T extends Item>  ItemBuilder<T, S>  item(String name, NonNullFunction<Item.Properties, T> factory);
public <T extends Block> BlockBuilder<T, S> block(String name, NonNullFunction<BlockBehaviour.Properties, T> factory);
public <T extends BlockEntity> BlockEntityBuilder<T, S> blockEntity(String name, BlockEntityBuilder.BlockEntityFactory<T> factory);
public S defaultCreativeTab(ResourceKey<CreativeModeTab>);
public S object(java.lang.String);
public S skipErrors(boolean);
public S addLang(java.lang.String, ResourceLocation, java.lang.String);
public S addRawLang(java.lang.String, java.lang.String);
public net.minecraftforge.eventbus.api.IEventBus getModEventBus();
public java.lang.String getModid();
```

### 3.2 foundation.data 下的 builder 辅助类（真实类名）

| 类 | 作用 |
|---|---|
| `BuilderTransformers` | 一整组 `static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>>`：`encasedShaft`、`casing`、`layeredCasing`、`copycat`、`slidingDoor`、`crate`、`bearing`、`mechanicalPiston`、`tableCloth`、`packager`、`trapdoor(boolean)`、`bell()`、`backtank(Supplier<ItemLike>)`、`scaffold(...)`、`ladder(...)`、`encasedCogwheel`、`encasedLargeCogwheel`、`cuckooClock`、`valveHandle(DyeColor)`、`beltTunnel(String, ResourceLocation)`、`palettesIronBlock`；另有 `packageItem(PackageStyles.PackageStyle)` |
| `BlockStateGen` | 方块状态生成器工厂：`axisBlockProvider(boolean)`、`directionalBlockProvider(boolean)`、`directionalBlockProviderIgnoresWaterlogged(boolean)`、`horizontalBlockProvider(boolean)`、`horizontalAxisBlockProvider(boolean)`、`directionalAxisBlockProvider()`、`simpleCubeAll(String)`、`horizontalWheelProvider(boolean)`、`cartAssembler()`、`blazeHeater()`、`linearChassis()`、`radialChassis()`、`naturalStoneTypeBlock(String)`、`encasedPipe()`、`pipe()`、`whistleExtender()`、`mapToAir(RegistrateBlockstateProvider)` |
| `AssetLookup` | 模型查找：`partialBaseModel(DataGenContext<?,?>, RegistrateBlockstateProvider, String...)`、`standardModel(...)`、`customBlockItemModel(String...)`、`customGenericItemModel(String...)`、`forPowered(...)`、`withIndicator(..., IntegerProperty)`、`existingItemModel()`、`itemModel(String)`、`itemModelWithPartials()` |
| `ModelGen` | `customItemModel()` / `customItemModel(String...)` 返回 `NonNullFunction<ItemBuilder<I, P>, P>`（给 `.transform(...)` 用）；`createOvergrown(...)` |
| `TagGen` | `axeOrPickaxe()`、`axeOnly()`、`pickaxeOnly()`、`tagBlockAndItem(...)` 返回 `NonNullFunction<BlockBuilder<T,P>, BlockBuilder<T,P>>` 或 `...ItemBuilder<BlockItem, BlockBuilder<T,P>>`；`addOptional(TagAppender, Mods, String[, List<String>])` |
| `SharedProperties` | `public static Block wooden() / stone() / softMetal() / copperMetal() / netheriteMetal()` —— 给 `.initialProperties(SharedProperties::copperMetal)` 用 |
| `SpecialBlockStateGen` | 自定义状态生成基类：`protected Property<?>[] getIgnoredProperties()`、`protected int horizontalAngle(Direction)`、`protected abstract int getXRotation(BlockState)`、`getYRotation(BlockState)`、`public abstract ModelFile getModel(...)`、`public final <T extends Block> void generate(...)` |
| `DirectionalAxisBlockStateGen` | 继承 `SpecialBlockStateGen`；已实现 `getXRotation`/`getYRotation`，要求子类实现 `getModelPrefix(...)` |
| `CreateBlockEntityBuilder` | 比 Registrate 的 `BlockEntityBuilder` 多：`validBlocksDeferred(...)`、`displaySource(RegistryEntry<? extends DisplaySource>)`、`displayTarget(...)`、`visual(NonNullSupplier<SimpleBlockEntityVisualizer.Factory<T>>[, boolean \| NonNullPredicate<T>])` |
| `CreateEntityBuilder` | `visual(NonNullSupplier<SimpleEntityVisualizer.Factory<T>>[, boolean \| NonNullPredicate<T>])` |
| `VirtualFluidBuilder` / `RuntimeDataGenerator` / `SimpleDatagenIngredient` / `WindowGen` / `DamageTypeTagGen` / `MetalBarsGen` | 少量专用工具 |
| `foundation.data.recipe.*` | Create 自己的配方生成器（`CreateStandardRecipeGen`、`CreateCrushingRecipeGen` 等）；**addon 不要继承它们**，继承 `api.data.recipe.*` |

### 3.3 注册一个方块 + 物品 + 方块实体（可编译写法）

```java
package com.croety;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

public class CroetyRegistrate {

    // 绝不能写 Create.registrate()，会抛 UnsupportedOperationException
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create("croety");

    public static final BlockEntry<MyMachineBlock> MY_MACHINE = REGISTRATE
            .block("my_machine", MyMachineBlock::new)                  // block(String, NonNullFunction<Properties, T>)
            .initialProperties(SharedProperties::copperMetal)          // 继承 Create 的金属基础属性
            .properties(p -> p.noOcclusion())                          // properties(NonNullUnaryOperator<Properties>)
            .transform(TagGen.pickaxeOnly())                           // 自动打 mineable/pickaxe 标签
            .blockstate(BlockStateGen.directionalBlockProvider(true))  // blockstate(...)
            .item()                                                    // ItemBuilder<BlockItem, BlockBuilder<T,P>>
                .transform(ModelGen.customItemModel())                 // NonNullFunction<ItemBuilder<I,P>, P>
            .register();                                               // -> BlockEntry<MyMachineBlock>

    public static final BlockEntityEntry<MyMachineBlockEntity> MY_MACHINE_BE = REGISTRATE
            .blockEntity("my_machine", MyMachineBlockEntity::new)      // CreateBlockEntityBuilder
            .validBlock(MY_MACHINE)                                    // NonNullSupplier<? extends Block>
            .register();                                               // -> BlockEntityEntry<...>

    /** 在 mod 构造函数里调用 */
    public static void register(net.minecraftforge.eventbus.api.IEventBus modBus) {
        REGISTRATE.registerEventListeners(modBus);
    }
}
```

> **已实测编译**：上面这段用 `gradlew compileJava` 真实编译通过（EXITCODE=0）。
>
> ⚠️ **踩坑记录（实测）**：`.item()` 之后如果调用了 `.transform(...)`，就**不要再接 `.build()`**。
> `ItemBuilder.build()` 返回的是**父 builder**（这里是 `BlockBuilder<T, CreateRegistrate>`），
> 而 `transform(...)` 本身已经回到父 builder 了；再 `.build()` 一次会掉到
> **`CreateRegistrate`** 上，于是 `.register()` 报
> `cannot find symbol: method register() / location: class CreateRegistrate`。
> 正确写法就是上面那段：`.item().transform(...).register()`。
> （Create 源码里常见的 `.item(...).build().register()` 是**没有 transform** 的写法，两者别混。）

配套的方块 / 方块实体（构造函数签名必须匹配 Registrate 的 `BlockEntityFactory`）：

```java
public class MyMachineBlock extends DirectionalKineticBlock {
    public MyMachineBlock(Properties properties) { super(properties); }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();      // FACING 由 DirectionalKineticBlock 提供
    }
}

public class MyMachineBlockEntity extends KineticBlockEntity {
    public MyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
```

常用 builder 方法速查（全部 javap 验证存在）：

| 想要 | 调用 |
|---|---|
| 方块继承某方块的全部属性 | `BlockBuilder.initialProperties(NonNullSupplier<? extends Block>)` |
| 改 `BlockBehaviour.Properties` | `BlockBuilder.properties(NonNullUnaryOperator<Properties>)` |
| 渲染层 | `BlockBuilder.addLayer(Supplier<Supplier<RenderType>>)` |
| 默认方块状态 | `BlockBuilder.defaultBlockstate()` |
| 默认语言 | `BlockBuilder.defaultLang()` / `ItemBuilder.defaultLang()` |
| 默认掉落 | `BlockBuilder.defaultLoot()` |
| 默认物品模型 | `ItemBuilder.defaultModel()` |
| 打标签 | `BlockBuilder.tag(TagKey<Block>...)` / `ItemBuilder.tag(TagKey<Item>...)` |
| 注册后回调 | `Builder.onRegister(NonNullConsumer<? super T>)` |
| 自定义 datagen provider | `Builder.setData(ProviderType<? extends D>, NonNullBiConsumer<DataGenContext<R,T>, D>)` |
| 取延迟 Supplier | `Builder.asSupplier()` |
| 方块带简单物品 | `BlockBuilder.simpleItem()` |
| 方块带自定义 BlockItem | `BlockBuilder.item(NonNullBiFunction<? super T, Item.Properties, ? extends I>)` |

---

## 4. 动能机械（kinetics）

### 4.1 基类清单（com.simibubi.create.content.kinetics.base）

真实存在的顶层类（jar class 清单确认）：`IRotate`、`KineticBlock`、`KineticBlockEntity`、
`GeneratingKineticBlockEntity`、`AbstractEncasedShaftBlock`、`DirectionalAxisKineticBlock`、
`DirectionalKineticBlock`、`DirectionalShaftHalvesBlockEntity`、`HorizontalAxisKineticBlock`、
`HorizontalKineticBlock`、`RotatedPillarKineticBlock`、`BlockBreakingKineticBlockEntity`、
`BlockBreakingMovementBehaviour`、`KineticBlockEntityRenderer`、`KineticBlockEntityVisual`、
`KineticEffectHandler`、`OrientedRotatingVisual`、`RotatingInstance`、`ShaftVisual`、
`SingleAxisRotatingVisual`、`ShaftRenderer`、`RotationIndicatorParticle`、`RotationIndicatorParticleData`。

> ⚠️ **不存在** `RotateBlock`、`GeneratingKineticBlock`、`AbstractKineticBlock`。
> 这三个名字在 6.0.8 的 jar 里查无此类（已用 class 清单逐一确认 ABSENT）。
> **产能元件（动力源 / 应力发生器）**请直接继承 `KineticBlock`（或某个方向基类）**并且**让 BE 继承 `GeneratingKineticBlockEntity`。

### 4.2 IRotate

```java
public interface com.simibubi.create.content.kinetics.base.IRotate
        extends com.simibubi.create.content.equipment.wrench.IWrenchable {

  boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction side);  // 抽象
  Direction.Axis getRotationAxis(BlockState state);                                            // 抽象

  default IRotate.SpeedLevel getMinimumRequiredSpeedLevel();   // 默认 NONE
  default boolean hideStressImpact();
  default boolean showCapacityWithAnnotation();
}
```

`IRotate$SpeedLevel`：枚举 `NONE / SLOW / MEDIUM / FAST`，方法
`ChatFormatting getTextColor()`、`int getColor()`、`int getParticleSpeed()`、`float getSpeedValue()`、
`static SpeedLevel of(float)`、`static LangBuilder getFormattedSpeedText(float, boolean)`。

`IRotate$StressImpact`：枚举 `LOW / MEDIUM / HIGH / OVERSTRESSED`，方法
`ChatFormatting getAbsoluteColor()`、`ChatFormatting getRelativeColor()`、`static StressImpact of(double)`、
`static boolean isEnabled()`、`static LangBuilder getFormattedStressText(double)`。
（主要用于 tooltip 显示；`IRotate` 本身没有返回 `StressImpact` 的方法。）

### 4.3 方块基类

`KineticBlock`（`public abstract class ... extends Block implements IRotate`）：

```java
public KineticBlock(BlockBehaviour.Properties);
public void onPlace(BlockState, Level, BlockPos, BlockState, boolean);
public void onRemove(BlockState, Level, BlockPos, BlockState, boolean);
public boolean hasShaftTowards(LevelReader, BlockPos, BlockState, Direction);   // 已实现
protected boolean areStatesKineticallyEquivalent(BlockState, BlockState);
public void updateIndirectNeighbourShapes(BlockState, LevelAccessor, BlockPos, int, int);
public void setPlacedBy(Level, BlockPos, BlockState, LivingEntity, ItemStack);
public float getParticleTargetRadius();
public float getParticleInitialRadius();
```

方向基类（任选其一；注意 `getRotationAxis` 是否还需要你自己实现）：

| 类 | 提供的属性 | 你还需要实现 |
|---|---|---|
| `DirectionalKineticBlock` | `public static final DirectionProperty FACING;` | `getRotationAxis(BlockState)` |
| `HorizontalKineticBlock` | `public static final Property<Direction> HORIZONTAL_FACING;` | `getRotationAxis(BlockState)` |
| `DirectionalAxisKineticBlock` | `FACING` + `public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE;`（已实现 `getRotationAxis`、`hasShaftTowards`、`transform`） | 无 |
| `HorizontalAxisKineticBlock` | `public static final Property<Direction.Axis> HORIZONTAL_AXIS;`（已实现 `getRotationAxis`、`hasShaftTowards`） | 无 |
| `RotatedPillarKineticBlock` | `public static final EnumProperty<Direction.Axis> AXIS;` | `getRotationAxis(BlockState)` |

### 4.4 KineticBlockEntity：哪些方法可以覆写

`public class KineticBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation`

公开/受保护字段（`protected` 的都能在子类直接读写）：

```java
public    Long network;          public BlockPos source;
public    boolean networkDirty;  public boolean updateSpeed;   public int preventSpeedUpdate;
protected KineticEffectHandler effects;
protected float speed;   protected float capacity;   protected float stress;
protected boolean overStressed;  protected boolean wasMoved;
protected float lastStressApplied;  protected float lastCapacityProvided;
public    SequencedGearshiftBlockEntity.SequenceContext sequenceContext;
public    KineticBlockEntity(BlockEntityType<?>, BlockPos, BlockState);
```

生命周期 / 机械行为：

```java
public void initialize();
public void tick();
public void updateFromNetwork(float speed, float capacity, int networkSize);
protected Block getStressConfigKey();
public float calculateStressApplied();                // 默认: (float) BlockStressValues.getImpact(getStressConfigKey())
public float calculateAddedStressCapacity();          // 默认: (float) BlockStressValues.getCapacity(getStressConfigKey())
public void onSpeedChanged(float previousSpeed);      // 转速（含方向）变化回调
public void remove();
protected void write(CompoundTag, boolean clientPacket);
protected void read(CompoundTag, boolean clientPacket);
public boolean needsSpeedUpdate();
public float getGeneratedSpeed();                     // 自己产生的转速（0 = 不产生）
public boolean isSource();
public float getSpeed();
public float getTheoreticalSpeed();
public void setSpeed(float);
public boolean hasSource();
public void setSource(BlockPos);
public void removeSource();
public void setNetwork(Long);
public KineticNetwork getOrCreateNetwork();
public boolean hasNetwork();
public void attachKinetics();
public void detachKinetics();
public boolean isSpeedRequirementFulfilled();
public static void switchToBlockState(Level, BlockPos, BlockState);
public void addBehaviours(java.util.List<BlockEntityBehaviour>);      // ← 加 tank/filter 等
public boolean addToTooltip(java.util.List<Component>, boolean);
public boolean addToGoggleTooltip(java.util.List<Component>, boolean);
protected void addStressImpactStats(java.util.List<Component>, float);
public void clearKineticInformation();
public void warnOfMovement();
public int getFlickerScore();
public static float convertToDirection(float, Direction);
public static float convertToLinear(float);
public static float convertToAngular(float);
public boolean isOverStressed();
public float propagateRotationTo(KineticBlockEntity, BlockState, BlockState, BlockPos, boolean, boolean);
public java.util.List<BlockPos> addPropagationLocations(IRotate, BlockState, java.util.List<BlockPos>);
public boolean isCustomConnection(KineticBlockEntity, BlockState, BlockState);
protected boolean canPropagateDiagonally(IRotate, BlockState);
public void requestModelDataUpdate();
public void tickAudio();
protected boolean isNoisy();
public int getRotationAngleOffset(Direction.Axis);
protected boolean syncSequenceContext();
```

`GeneratingKineticBlockEntity`（`public abstract class ... extends KineticBlockEntity`）：

```java
public boolean reActivateSource;
public GeneratingKineticBlockEntity(BlockEntityType<?>, BlockPos, BlockState);
protected void notifyStressCapacityChange(float);
public void removeSource();
public void setSource(BlockPos);
public void tick();
public boolean addToGoggleTooltip(java.util.List<Component>, boolean);
public void updateGeneratedRotation();               // ← 转速变化后必须调用
public void applyNewSpeed(float prevSpeed, float speed);
public Long createNetworkId();
```

辅助：`KineticEffectHandler`（`public KineticEffectHandler(KineticBlockEntity)`，方法
`tick()`、`queueRotationIndicators()`、`spawnEffect(ParticleOptions, float, int)`、
`spawnRotationIndicators()`、`triggerOverStressedEffect()`）。

### 4.5 应力（stress）怎么声明：BlockStressValues

**关键事实（源码确认）：通常你根本不需要覆写 `calculateStressApplied()` / `calculateAddedStressCapacity()`。**
`KineticBlockEntity` 的默认实现就是去查 `BlockStressValues`：

```java
protected Block getStressConfigKey() {
    return getBlockState().getBlock();
}

public float calculateStressApplied() {
    float impact = (float) BlockStressValues.getImpact(getStressConfigKey());
    this.lastStressApplied = impact;
    return impact;
}

public float calculateAddedStressCapacity() {
    float capacity = (float) BlockStressValues.getCapacity(getStressConfigKey());
    this.lastCapacityProvided = capacity;
    return capacity;
}
```

所以**只要注册数值**就行（三种方式）：

```java
// (a) 消耗应力的机器：注册 impact（单位 SU，@1 RPM）
BlockStressValues.IMPACTS.register(MyBlocks.MY_MACHINE.get(), () -> 4.0);

// (b) 产能的机器：注册 capacity（单位 SU，@1 RPM）
BlockStressValues.CAPACITIES.register(MyBlocks.MY_GENERATOR.get(), () -> 256.0);

// (c) 动力源转速信息：只影响护目镜 tooltip，不影响实际转速！
REGISTRATE.block("my_generator", MyGeneratorBlock::new)
        .onRegister(BlockStressValues.setGeneratorSpeed(256, true))  // (int) 或 (int, boolean mayGenerateLess)
        .simpleItem()
        .register();
```

读取用 `BlockStressValues.getImpact(Block)` / `BlockStressValues.getCapacity(Block)`（未注册返回 0）。

> 何时才该覆写 `calculateStressApplied()`：只有当你的机器应力**随运行状态动态变化**时。
> 覆写后请自己维护 `lastStressApplied`（工具提示和网络同步都读它）。
>
> 注册时机：必须在**方块已经注册之后**调用 `register(...)`（否则 `MyBlocks.X.get()` 会 NPE）。
> 惯例是在 `FMLCommonSetupEvent` 里注册；`setGeneratorSpeed` 这类返回 `NonNullConsumer<Block>` 的可以用 `onRegister(...)`。
>
> 同一个方块**不能注册两次** impact/capacity：`SimpleRegistryImpl.register` 会抛 `IllegalArgumentException`。
>
> Create 官方 config 注释（`CStress.Comments.impact`）原文：
> "Configure the individual stress impact of mechanical blocks. Note that **this cost is doubled for every speed increase it receives**."
> 也就是说 `IMPACTS` 里填的是 1 RPM 时的基准值；实际消耗还会随转速等级放大。

### 4.6 一个完整的自定义动能机器

```java
// ---- 方块 ----
public class MyPressBlock extends DirectionalKineticBlock {

    public MyPressBlock(Properties properties) { super(properties); }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
        return IRotate.SpeedLevel.MEDIUM;     // 低于此速度不工作
    }
}

// ---- 方块实体 ----
public class MyPressBlockEntity extends KineticBlockEntity {

    public MyPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 不需要覆写 calculateStressApplied()：默认实现会去查 BlockStressValues.getImpact(方块)
    // 只有在应力随运行状态动态变化时才覆写，并且要自己更新 lastStressApplied

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        // 转速变了：重算进度、停掉正在跑的加工
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        if (isOverStressed() || !isSpeedRequirementFulfilled()) return;
        // 正常加工逻辑
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);       // 必须调用 super
        // tag.putInt("Progress", progress);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        // progress = tag.getInt("Progress");
    }
}

// ---- 注册 + 应力 ----
public static final BlockEntry<MyPressBlock> MY_PRESS = REGISTRATE
        .block("my_press", MyPressBlock::new)
        .initialProperties(SharedProperties::copperMetal)
        .transform(TagGen.pickaxeOnly())
        .blockstate(BlockStateGen.directionalBlockProvider(true))
        .item().build()
        .register();

public static final BlockEntityEntry<MyPressBlockEntity> MY_PRESS_BE = REGISTRATE
        .blockEntity("my_press", MyPressBlockEntity::new)
        .validBlock(MY_PRESS)
        .register();

// 在 FMLCommonSetupEvent 里：
BlockStressValues.IMPACTS.register(MY_PRESS.get(), () -> 4.0);
```

**动力源 / 应力发生器**（产能）把上面两处换掉即可：

```java
public class MyGeneratorBlockEntity extends GeneratingKineticBlockEntity {

    public MyGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public float getGeneratedSpeed() {
        return 64.0f;                          // 我产生的转速（0 表示不产生）
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = 256.0f;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // 转速 / 是否运转变化后调用 updateGeneratedRotation();
}
```

---

## 5. 方块实体（SmartBlockEntity 与 behaviour）

### 5.1 继承链

```
BlockEntity (net.minecraft)
  └─ SyncedBlockEntity            (com.simibubi.create.foundation.blockEntity)
       └─ CachedRenderBBBlockEntity
            └─ SmartBlockEntity    ← Create 机器的标准基类
                 └─ KineticBlockEntity
```

### 5.2 SmartBlockEntity

```java
public abstract class SmartBlockEntity extends CachedRenderBBBlockEntity
        implements PartialSafeNBT, IInteractionChecker, SpecialBlockEntityItemRequirement, VirtualBlockEntity {

  protected int lazyTickRate;
  protected int lazyTickCounter;

  public SmartBlockEntity(BlockEntityType<?>, BlockPos, BlockState);

  public abstract void addBehaviours(java.util.List<BlockEntityBehaviour>);   // ← 唯一必须实现的方法
  public void addBehavioursDeferred(java.util.List<BlockEntityBehaviour>);

  public void initialize();
  public void tick();                       // 已处理 lazyTick、behaviour tick、同步
  public void lazyTick();
  protected void write(CompoundTag, boolean clientPacket);
  protected void read(CompoundTag, boolean clientPacket);
  public void writeSafe(CompoundTag);
  public final void load(CompoundTag);
  public final void saveAdditional(CompoundTag);
  public final void readClient(CompoundTag);
  public final CompoundTag writeClient(CompoundTag);

  public void onChunkUnloaded();
  public final void setRemoved();
  public void invalidate();
  public void remove();
  public void destroy();

  public <T extends BlockEntityBehaviour> T getBehaviour(BehaviourType<T>);
  public void forEachBehaviour(java.util.function.Consumer<BlockEntityBehaviour>);
  public java.util.Collection<BlockEntityBehaviour> getAllBehaviours();
  public void attachBehaviourLate(BlockEntityBehaviour);
  public void removeBehaviour(BehaviourType<?>);

  public ItemRequirement getRequiredItems(BlockState);
  public void setLazyTickRate(int);
  public void markVirtual();
  public boolean isVirtual();
  public boolean isChunkUnloaded();
  public boolean canPlayerUse(Player);
  public void sendToMenu(FriendlyByteBuf);
  public void refreshBlockState();
  protected boolean isItemHandlerCap(Capability<?>);
  protected boolean isFluidHandlerCap(Capability<?>);
  public void registerAwardables(java.util.List<BlockEntityBehaviour>, CreateAdvancement...);
  public void award(CreateAdvancement);
  public void awardIfNear(CreateAdvancement, int);
}
```

`SyncedBlockEntity`（`public abstract class ... extends BlockEntity`）负责网络同步：

```java
public CompoundTag getUpdateTag();
public ClientboundBlockEntityDataPacket getUpdatePacket();
public void handleUpdateTag(CompoundTag);
public void onDataPacket(Connection, ClientboundBlockEntityDataPacket);
public void readClient(CompoundTag);
public CompoundTag writeClient(CompoundTag);
public void sendData();
public void notifyUpdate();
public PacketDistributor.PacketTarget packetTarget();
public LevelChunk containedChunk();
public HolderGetter<Block> blockHolderGetter();
```

`CachedRenderBBBlockEntity`：`public AABB getRenderBoundingBox()`、`protected void invalidateRenderBoundingBox()`、
`protected AABB createRenderBoundingBox()`。

`SmartBlockEntityTicker<T extends BlockEntity> implements BlockEntityTicker<T>`：
`public SmartBlockEntityTicker()`、`public void tick(Level, BlockPos, BlockState, T)` —— **在方块里返回它就能自动驱动 BE**。

`SmartBlockEntityRenderer<T extends SmartBlockEntity> extends SafeBlockEntityRenderer<T>`：
`protected void renderSafe(T, float, PoseStack, MultiBufferSource, int, int)`、
`protected void renderNameplateOnHover(T, Component, float, PoseStack, MultiBufferSource, int, int)`。

其它工具：`IMergeableBE`（`void accept(BlockEntity)`）、
`ComparatorUtil.fractionToRedstoneLevel(double)` / `ComparatorUtil.levelOfSmartFluidTank(BlockGetter, BlockPos)`。

`IMultiBlockEntityContainer`（多方块容器）：

```java
BlockPos getController();
<T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE();
boolean isController();
void setController(BlockPos);
void removeController(boolean keepContents);
BlockPos getLastKnownPos();
void preventConnectivityUpdate();
void notifyMultiUpdated();
default void setExtraData(java.lang.Object);
default java.lang.Object getExtraData();
default java.lang.Object modifyExtraData(java.lang.Object);
Direction.Axis getMainConnectionAxis();
default Direction.Axis getMainAxisOf(BlockEntity);
int getMaxLength(Direction.Axis, int);
int getMaxWidth();
int getHeight();  void setHeight(int);
int getWidth();   void setWidth(int);
// 子接口 Inventory: default boolean hasInventory();
// 子接口 Fluid:     default boolean hasTank(); int getTankSize(int); void setTankSize(int,int);
//                   IFluidTank getTank(int); FluidStack getFluid(int);
```

> ⚠️ **`addBehaviours` 是在父类构造函数里被调用的，不是 initialize 时。** 源码原文：
>
> ```java
> public SmartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
>     super(type, pos, state);
>     setLazyTickRate(10);                       // 默认 lazyTickRate = 10
>     ArrayList<BlockEntityBehaviour> list = new ArrayList<>();
>     addBehaviours(list);                       // ← 这里
>     list.forEach(b -> behaviours.put(b.getType(), b));
> }
> ```
>
> 后果：`addBehaviours` 跑的时候，**子类的实例字段初始化器还没执行**（Java 里它们排在 `super(...)` 之后）。
> 所以 `private SmartFluidTankBehaviour tank = null;` 这种写法会把 `addBehaviours` 里赋的值**覆盖回 null**。
> **在 `addBehaviours` 里赋值的字段，声明时不要写初始化器。**
>
> 另外事件 `BlockEntityBehaviourEvent` 在 `initialize()`（以及第一次 `read(...)`）里 post；
> `initialize()` 由第一次 `tick()` 在 `hasLevel()` 为真时自动触发，也可以手动调用。

### 5.3 BlockEntityBehaviour

```java
public abstract class BlockEntityBehaviour {
  public SmartBlockEntity blockEntity;
  public BlockEntityBehaviour(SmartBlockEntity);

  public abstract BehaviourType<?> getType();
  public void initialize();
  public void tick();
  public void read(CompoundTag, boolean clientPacket);
  public void write(CompoundTag, boolean clientPacket);
  public void writeSafe(CompoundTag);
  public boolean isSafeNBT();
  public ItemRequirement getRequiredItems();
  public void onBlockChanged(BlockState);
  public void onNeighborChanged(BlockPos);
  public void unload();
  public void destroy();
  public void setLazyTickRate(int);
  public void lazyTick();
  public BlockPos getPos();
  public Level getWorld();

  public static <T extends BlockEntityBehaviour> T get(BlockGetter, BlockPos, BehaviourType<T>);
  public static <T extends BlockEntityBehaviour> T get(BlockEntity, BehaviourType<T>);
}

public class BehaviourType<T extends BlockEntityBehaviour> {
  public BehaviourType(java.lang.String name);
  public BehaviourType();
  public java.lang.String getName();
  public int hashCode();
}
```

自定义 behaviour 的最小骨架：

```java
public class MyCounterBehaviour extends BlockEntityBehaviour {

    public static final BehaviourType<MyCounterBehaviour> TYPE = new BehaviourType<>("croety_counter");

    public int counter;

    public MyCounterBehaviour(SmartBlockEntity be) { super(be); }

    @Override public BehaviourType<?> getType() { return TYPE; }

    @Override public void tick() {
        if (getWorld().isClientSide) return;
        counter++;
    }

    @Override public void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("Counter", counter);
    }

    @Override public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        counter = tag.getInt("Counter");
    }
}
```

### 5.4 Create 6.0.8 里真实可用的 behaviour 类

包路径前缀都是 `com.simibubi.create.foundation.blockEntity.behaviour`。

**fluid.SmartFluidTankBehaviour** —— 储罐（最常用的一个）：

```java
public static final BehaviourType<SmartFluidTankBehaviour> TYPE;
public static final BehaviourType<SmartFluidTankBehaviour> INPUT;
public static final BehaviourType<SmartFluidTankBehaviour> OUTPUT;

public static SmartFluidTankBehaviour single(SmartBlockEntity be, int capacity);
public SmartFluidTankBehaviour(BehaviourType<SmartFluidTankBehaviour>, SmartBlockEntity, int tanks, int capacity, boolean enforceVariety);

public SmartFluidTankBehaviour whenFluidUpdates(Runnable);
public SmartFluidTankBehaviour allowInsertion();
public SmartFluidTankBehaviour allowExtraction();
public SmartFluidTankBehaviour forbidInsertion();
public SmartFluidTankBehaviour forbidExtraction();

public void initialize();   public void tick();
public void sendDataImmediately();  public void sendDataLazily();
public void unload();
public SmartFluidTank getPrimaryHandler();
public SmartFluidTankBehaviour.TankSegment getPrimaryTank();
public SmartFluidTankBehaviour.TankSegment[] getTanks();
public boolean isEmpty();
public void forEach(java.util.function.Consumer<TankSegment>);
public LazyOptional<? extends IFluidHandler> getCapability();
public void write(CompoundTag, boolean);
public void read(CompoundTag, boolean);
public BehaviourType<?> getType();

// TankSegment: getRenderedFluid() getFluidLevel() getTotalUnits(float) isEmpty(float)
//              onFluidStackChanged() writeNBT() readNBT(CompoundTag, boolean)
```

**filtering.FilteringBehaviour** —— 过滤槽（物品 / 流体 / 配方）：

```java
public static final BehaviourType<FilteringBehaviour> TYPE;
public FilteringBehaviour(SmartBlockEntity, ValueBoxTransform);
public FilteringBehaviour withCallback(java.util.function.Consumer<ItemStack>);
public FilteringBehaviour withPredicate(java.util.function.Predicate<ItemStack>);
public FilteringBehaviour forRecipes();
public FilteringBehaviour forFluids();
public FilteringBehaviour onlyActiveWhen(java.util.function.Supplier<java.lang.Boolean>);
public FilteringBehaviour showCountWhen(java.util.function.Supplier<java.lang.Boolean>);
public FilteringBehaviour showCount();
public boolean setFilter(Direction, ItemStack);
public boolean setFilter(ItemStack);
public ItemStack getFilter(Direction);
public ItemStack getFilter();
public boolean test(ItemStack);
public boolean test(FluidStack);
public int getAmount();
public boolean anyAmount();
public boolean isActive();
public boolean testHit(Vec3);
public void setLabel(MutableComponent);
public ItemRequirement getRequiredItems();
public void destroy();
// 也实现 ValueSettingsBehaviour，可作为可点击数值框
```

`SidedFilteringBehaviour extends FilteringBehaviour`：
`public SidedFilteringBehaviour(SmartBlockEntity, ValueBoxTransform.Sided, java.util.function.BiFunction<Direction, FilteringBehaviour, FilteringBehaviour>, java.util.function.Predicate<Direction>)`，
方法 `get(Direction)`、`setFilter(Direction, ItemStack)`、`getFilter(Direction)`、`test(Direction, ItemStack)`、
`removeFilter(Direction)`、`updateFilterPresence()`、`testHit(LevelAccessor, BlockPos, Direction, Vec3)`。

**scrollValue.ScrollValueBehaviour** —— 滚轮数值：

```java
public static final BehaviourType<ScrollValueBehaviour> TYPE;
public ScrollValueBehaviour(Component label, SmartBlockEntity, ValueBoxTransform);
public ScrollValueBehaviour withClientCallback(java.util.function.Consumer<java.lang.Integer>);
public ScrollValueBehaviour withCallback(java.util.function.Consumer<java.lang.Integer>);
public ScrollValueBehaviour between(int min, int max);
public ScrollValueBehaviour requiresWrench();
public ScrollValueBehaviour withFormatter(java.util.function.Function<java.lang.Integer, java.lang.String>);
public ScrollValueBehaviour onlyActiveWhen(java.util.function.Supplier<java.lang.Boolean>);
public void setValue(int);   public int getValue();
public java.lang.String formatValue();
public void setLabel(Component);
public boolean isActive();
public boolean testHit(Vec3);
// 公开字段： public int value;  public Component label;
```

`ScrollOptionBehaviour<E extends java.lang.Enum<E> & INamedIconOptions> extends ScrollValueBehaviour`：
`public ScrollOptionBehaviour(java.lang.Class<E>, Component, SmartBlockEntity, ValueBoxTransform)`、`public E get()`。
`INamedIconOptions`：`AllIcons getIcon();` + `java.lang.String getTranslationKey();`。

**inventory.*** —— 与相邻容器交互：

```java
public abstract class CapManipulationBehaviourBase<T, S extends CapManipulationBehaviourBase<?, ?>> extends BlockEntityBehaviour {
  public CapManipulationBehaviourBase(SmartBlockEntity, CapManipulationBehaviourBase.InterfaceProvider);
  protected abstract Capability<T> capability();
  public S bypassSidedness();
  public S simulate();
  public S withFilter(com.google.common.base.Predicate<BlockEntity>);
  public boolean hasInventory();
  public T getInventory();
  public BlockFace getTarget();
  public void tick();  public void lazyTick();
  public int getAmountFromFilter();
  public ItemHelper.ExtractionCountMode getModeFromFilter();
  public void findNewCapability();
}

public interface CapManipulationBehaviourBase$InterfaceProvider {
  public static InterfaceProvider towardBlockFacing();
  public static InterfaceProvider oppositeOfBlockFacing();
  BlockFace getTarget(Level, BlockPos, BlockState);
}

public class InvManipulationBehaviour extends CapManipulationBehaviourBase<IItemHandler, InvManipulationBehaviour> {
  public static final BehaviourType<InvManipulationBehaviour> TYPE;
  public static final BehaviourType<InvManipulationBehaviour> EXTRACT;
  public static final BehaviourType<InvManipulationBehaviour> INSERT;
  public static InvManipulationBehaviour forExtraction(SmartBlockEntity, InterfaceProvider);
  public static InvManipulationBehaviour forInsertion(SmartBlockEntity, InterfaceProvider);
  public ItemStack extract();
  public ItemStack extract(ItemHelper.ExtractionCountMode, int);
  public ItemStack extract(ItemHelper.ExtractionCountMode, int, java.util.function.Predicate<ItemStack>);
  public ItemStack insert(ItemStack);
}

public class TankManipulationBehaviour extends CapManipulationBehaviourBase<IFluidHandler, TankManipulationBehaviour> {
  public static final BehaviourType<TankManipulationBehaviour> OBSERVE;
  public FluidStack extractAny();
}

public class VersionedInventoryTrackerBehaviour extends BlockEntityBehaviour {
  public static final BehaviourType<VersionedInventoryTrackerBehaviour> TYPE;
  public boolean stillWaiting(InvManipulationBehaviour);
  public boolean stillWaiting(IItemHandler);
  public void awaitNewVersion(InvManipulationBehaviour);
  public void awaitNewVersion(IItemHandler);
  public void reset();
}
```

**其它**：

| 类 | 作用 / 关键成员 |
|---|---|
| `edgeInteraction.EdgeInteractionBehaviour` | 沿边连接（类似机壳连接）。`public EdgeInteractionBehaviour(SmartBlockEntity, ConnectionCallback)`、`connectivity(ConnectivityPredicate)`、`require(Item)`、`require(Predicate<Item>)` |
| `simple.DeferralBehaviour` | 延迟若干 tick 后执行。`public DeferralBehaviour(SmartBlockEntity, java.util.function.Supplier<java.lang.Boolean>)`、`scheduleUpdate()` |
| `animatedContainer.AnimatedContainerBehaviour<M extends MenuBase<? extends SmartBlockEntity>>` | 容器开合动画。`public AnimatedContainerBehaviour(SmartBlockEntity, java.lang.Class<M>)`、`onOpenChanged(Consumer<Boolean>)`、`startOpen(Player)`、`stopOpen(Player)`、公开字段 `public int openCount;` |
| `ValueSettingsBehaviour`（接口） | 可点击数值框。`testHit(Vec3)`、`isActive()`、`getSlotPositioning()`、`createBoard(Player, BlockHitResult)`、`setValueSettings(Player, ValueSettings, boolean)`、`getValueSettings()`、`onlyVisibleWithWrench()`、`acceptsValueSettings()`、`onShortInteract(...)`、`bypassesInput(ItemStack)`、`netId()` |
| `ValueBoxTransform`（抽象类） | `abstract Vec3 getLocalOffset(LevelAccessor, BlockPos, BlockState)`、`abstract void rotate(LevelAccessor, BlockPos, BlockState, PoseStack)`、`boolean testHit(...)`、`void transform(...)`、`boolean shouldRender(...)`、`int getOverrideColor()`、`protected Vec3 rotateHorizontally(BlockState, Vec3)`、`float getScale()` / `getFontScale()` |
| `CenteredSideValueBoxTransform` | `public CenteredSideValueBoxTransform()` 或 `(BiPredicate<BlockState, Direction>)`；`protected Vec3 getSouthLocation()`、`protected boolean isSideActive(BlockState, Direction)`；父类 `ValueBoxTransform.Sided` 有 `fromSide(Direction)` / `getSide()` |
| `ValueBoxTransform.Dual` | 双槽。`static Pair<ValueBoxTransform, ValueBoxTransform> makeSlots(Function<Boolean, ? extends Dual>)` |
| `ValueSettingsBoard` | record：`ValueSettingsBoard(Component title, int maxValue, int milestoneInterval, List<Component> rows, ValueSettingsFormatter)` |
| `ValueSettingsFormatter` | `public ValueSettingsFormatter(java.util.function.Function<ValueSettings, MutableComponent>)` |
| `ValueSettingsBehaviour.ValueSettings` | record：`ValueSettings(int row, int value)`、`MutableComponent format()` |
| `ValueBox` | `ValueBox(Component, AABB, BlockPos[, BlockState])`、`transform(ValueBoxTransform)`、`withColor(int)`、`passive(boolean)`、`wideOutline()` |

### 5.5 用法示例

```java
public class MyTankBlockEntity extends SmartBlockEntity {

    private SmartFluidTankBehaviour tank;
    private FilteringBehaviour filter;

    public MyTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
    }

    @Override
    public void addBehaviours(java.util.List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 8000);   // 8000 mB
        tank.whenFluidUpdates(() -> sendData());
        behaviours.add(tank);

        filter = new FilteringBehaviour(this, new CenteredSideValueBoxTransform());
        filter.withCallback(stack -> { /* 过滤条件变了 */ });
        behaviours.add(filter);
    }

    public SmartFluidTankBehaviour getTank() { return tank; }

    // 暴露 Forge 流体能力（与 Create 自己的 FluidTankBlockEntity 同样的做法）
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
}
```

`getCapability` / `invalidateCaps` 来自 Forge 的 `CapabilityProvider`
（`BlockEntity extends CapabilityProvider<BlockEntity>`；实测签名
`public <T> LazyOptional<T> getCapability(Capability<T>, Direction)`、`public void invalidateCaps()`）。
Create 自己的 `FluidTankBlockEntity`、`BeltBlockEntity`、`PortableFluidInterfaceBlockEntity` 都是这么覆写的。

### 5.6 BlockEntityBehaviourEvent

签名见 2.12。它在 `SmartBlockEntity` 初始化时被 post 到 `MinecraftForge.EVENT_BUS`，
而且是 `GenericEvent`，所以只能用 `addGenericListener`：

```java
// 在 mod 构造函数 / FMLCommonSetupEvent 里注册（必须早于任何 BE 被创建）
MinecraftForge.EVENT_BUS.addGenericListener(MyTankBlockEntity.class,
        (BlockEntityBehaviourEvent<MyTankBlockEntity> event) -> {
            MyTankBlockEntity be = event.getBlockEntity();
            if (be.wantsExtraBehaviour()) {
                event.attach(new MyCounterBehaviour(be));    // 也可以 event.remove(SomeType.TYPE)
            }
        });
```

适合的场合：你的 BE 已经被别的 mod 继承、不方便覆写 `addBehaviours` 时。

---

## 6. 护目镜信息

三个接口都在 `com.simibubi.create.api.equipment.goggles`：

```java
public interface IHaveCustomOverlayIcon {
  default ItemStack getIcon(boolean isPlayerSneaking);
}

public interface IHaveGoggleInformation extends IHaveCustomOverlayIcon {
  default boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking);
  default boolean containedFluidTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking,
                                        LazyOptional<IFluidHandler> fluidHandler);
}

public interface IHaveHoveringInformation extends IHaveCustomOverlayIcon {
  default boolean addToTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking);
}

public interface IProxyHoveringInformation {
  BlockPos getInformationSource(Level level, BlockPos pos, BlockState state);
}
```

- `IHaveGoggleInformation` = 戴护目镜时显示（详细面板）。
- `IHaveHoveringInformation` = 不戴护目镜、鼠标悬停时显示（简略信息）。
- `KineticBlockEntity` **同时实现了这两个接口**，所以自定义动能机器直接覆写 `addToGoggleTooltip` 即可。
- `IProxyHoveringInformation`：让某个方块把信息「转交」给另一个方块（例如多方块的一部分指向控制器）。

> ⚠️ 源码确认的**密封（sealed）关系**：
>
> ```java
> public sealed interface IHaveCustomOverlayIcon permits IHaveGoggleInformation, IHaveHoveringInformation {
>     default ItemStack getIcon(boolean isPlayerSneaking) { return AllItems.GOGGLES.asStack(); }
> }
> public non-sealed interface IHaveGoggleInformation extends IHaveCustomOverlayIcon { ... }
> public non-sealed interface IHaveHoveringInformation extends IHaveCustomOverlayIcon { ... }
> ```
>
> 因此 **addon 不能直接 `implements IHaveCustomOverlayIcon`**（编译报错：不在 permits 列表里）。
> 必须实现 `IHaveGoggleInformation` 或 `IHaveHoveringInformation`，然后覆写 `getIcon` 换图标；
> 不覆写时默认图标就是 Create 的护目镜。
>
> 「何时显示」属于行为描述（来自接口 javadoc：goggle overlay / hovering overlay）；javap 与源码只能验证签名与默认实现。

**示例**（`LangBuilder` = `net.createmod.catnip.lang.LangBuilder`，实测
`public LangBuilder(java.lang.String namespace)`、`translate(String, Object...)`、
`style(ChatFormatting)`、`text(String)`、`addTo(java.util.List<? super MutableComponent>)`、
`forGoggles(java.util.List<? super MutableComponent>[, int])`）：

```java
public class MyMachineBlockEntity extends KineticBlockEntity
        implements IHaveGoggleInformation, IHaveHoveringInformation {

    public MyMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);        // 保留 Create 的转速 / 应力行

        new LangBuilder("croety")
                .translate("croety.goggles.my_machine.status")
                .style(ChatFormatting.GOLD)
                .forGoggles(tooltip);                               // 自动加缩进

        return true;                                                // true = 有内容显示
    }

    @Override
    public boolean addToTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder("croety")
                .translate("croety.goggles.my_machine.hint")
                .forGoggles(tooltip);
        return true;
    }

    @Override
    public ItemStack getIcon(boolean isPlayerSneaking) {
        return new ItemStack(MyItems.MY_WRENCH.get());              // 面板左上角图标
    }
}
```

显示容器里的流体，直接复用现成的 helper
（`SmartFluidTankBehaviour.getCapability()` 返回 `LazyOptional<? extends IFluidHandler>`，
正好可以传给 `containedFluidTooltip`）：

```java
@Override
public boolean addToGoggleTooltip(java.util.List<Component> tooltip, boolean isPlayerSneaking) {
    containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
    return true;
}
```

---

## 7. 配方（processing recipe）

### 7.1 核心类

`com.simibubi.create.content.processing.recipe`：

```java
public abstract class ProcessingRecipe<T extends Container> implements Recipe<T> {
  protected ResourceLocation id;
  protected NonNullList<Ingredient> ingredients;
  protected NonNullList<ProcessingOutput> results;
  protected NonNullList<FluidIngredient> fluidIngredients;
  protected NonNullList<FluidStack> fluidResults;
  protected int processingDuration;
  protected HeatCondition requiredHeat;

  public ProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeBuilder.ProcessingRecipeParams params);

  protected abstract int getMaxInputCount();
  protected abstract int getMaxOutputCount();
  protected boolean canRequireHeat();          // 默认 false  ！！覆写才会生效
  protected boolean canSpecifyDuration();      // 默认 false  ！！覆写才会生效
  protected int getMaxFluidInputCount();       // 默认 0
  protected int getMaxFluidOutputCount();      // 默认 0
  // ...（其余见 2.8 节附近的签名清单）
}
```

> ⚠️ 源码确认：`canRequireHeat()` 与 `canSpecifyDuration()` 的**默认返回值都是 `false`**。
> 不覆写它们，你就既不能用 `.duration(n)` 也不能用 `.requiresHeat(...)`。
> 构造函数末尾会调用私有的 `validate(typeInfo.getId())`：输入/输出数量超限、超范围地指定了时长或热量时，
> **只是往 `Create.LOGGER` 打 warn**，不会抛异常 —— 所以配方"没生效"时先看日志。

`ProcessingRecipeBuilder<T extends ProcessingRecipe<?>>`：

```java
public ProcessingRecipeBuilder(ProcessingRecipeBuilder.ProcessingRecipeFactory<T> factory, ResourceLocation id);

public ProcessingRecipeBuilder<T> require(ItemLike);
public ProcessingRecipeBuilder<T> require(TagKey<Item>);
public ProcessingRecipeBuilder<T> require(Ingredient);
public ProcessingRecipeBuilder<T> require(ResourceLocation);
public ProcessingRecipeBuilder<T> require(DatagenMod, java.lang.String);
public ProcessingRecipeBuilder<T> require(Fluid, int amount);
public ProcessingRecipeBuilder<T> require(TagKey<Fluid>, int amount);
public ProcessingRecipeBuilder<T> require(FluidIngredient);

public ProcessingRecipeBuilder<T> output(ItemLike);
public ProcessingRecipeBuilder<T> output(float chance, ItemLike);
public ProcessingRecipeBuilder<T> output(ItemLike, int count);
public ProcessingRecipeBuilder<T> output(float chance, ItemLike, int count);
public ProcessingRecipeBuilder<T> output(ItemStack);
public ProcessingRecipeBuilder<T> output(float chance, ItemStack);
public ProcessingRecipeBuilder<T> output(ResourceLocation);
public ProcessingRecipeBuilder<T> output(DatagenMod, java.lang.String);
public ProcessingRecipeBuilder<T> output(float chance, ResourceLocation, int count);
public ProcessingRecipeBuilder<T> output(ProcessingOutput);
public ProcessingRecipeBuilder<T> output(Fluid, int amount);
public ProcessingRecipeBuilder<T> output(FluidStack);

public ProcessingRecipeBuilder<T> withItemIngredients(Ingredient...);
public ProcessingRecipeBuilder<T> withItemIngredients(NonNullList<Ingredient>);
public ProcessingRecipeBuilder<T> withSingleItemOutput(ItemStack);
public ProcessingRecipeBuilder<T> withItemOutputs(ProcessingOutput...);
public ProcessingRecipeBuilder<T> withItemOutputs(NonNullList<ProcessingOutput>);
public ProcessingRecipeBuilder<T> withFluidIngredients(FluidIngredient...);
public ProcessingRecipeBuilder<T> withFluidIngredients(NonNullList<FluidIngredient>);
public ProcessingRecipeBuilder<T> withFluidOutputs(FluidStack...);
public ProcessingRecipeBuilder<T> withFluidOutputs(NonNullList<FluidStack>);

public ProcessingRecipeBuilder<T> duration(int ticks);
public ProcessingRecipeBuilder<T> averageProcessingDuration();
public ProcessingRecipeBuilder<T> requiresHeat(HeatCondition);
public ProcessingRecipeBuilder<T> toolNotConsumed();
public ProcessingRecipeBuilder<T> whenModLoaded(java.lang.String);
public ProcessingRecipeBuilder<T> whenModMissing(java.lang.String);
public ProcessingRecipeBuilder<T> withCondition(ICondition);

public T build();
public void build(java.util.function.Consumer<FinishedRecipe>);
```

```java
public interface ProcessingRecipeBuilder$ProcessingRecipeFactory<T extends ProcessingRecipe<?>> {
  T create(ProcessingRecipeBuilder.ProcessingRecipeParams params);
}
```

`ProcessingRecipeSerializer<T>`：`public ProcessingRecipeSerializer(ProcessingRecipeFactory<T>)`，
公开 `public ProcessingRecipeFactory<T> getFactory();`（datagen 就是靠它拿到 factory 的）。

配套类型：`HeatCondition`（`NONE / HEATED / SUPERHEATED`）、`ProcessingOutput`、`ProcessingInventory`、
`com.simibubi.create.foundation.fluid.FluidIngredient`、`com.simibubi.create.foundation.recipe.IRecipeTypeInfo`。
签名见 2.8 节。

### 7.2 JSON 格式（源码确认，不靠猜）

`ProcessingRecipeSerializer.readFromJson` / `writeToJson` 的真实顶层字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `ingredients` | array | 每项要么是 `FluidIngredient`（判据 `FluidIngredient.isFluidIngredient`），要么是原版 `Ingredient.fromJson` |
| `results` | array | 每项若含 `"fluid"` 键就按流体输出解析，否则走 `ProcessingOutput.deserialize` |
| `processingTime` | int（可选） | 只有 > 0 时才写出/读入 |
| `heatRequirement` | string（可选） | 只有 != `NONE` 时才写出，值是 `HeatCondition.serialize()` |

`ProcessingOutput` 的 JSON 形状（源码确认）：`item`（必填，ResourceLocation 字符串）、
`count`（默认 1）、`nbt`（可选）、`chance`（默认 1，浮点）。
注意 `rollOutput()` 是对**每一个 count 单位**单独掷一次 `chance`，
所以 `count=3, chance=0.5` 的期望产出是 1.5 个。

```json
{
  "type": "croety:my_processing",
  "ingredients": [
    { "item": "croety:raw_ore" },
    { "tag": "forge:ingots/copper" }
  ],
  "results": [
    { "item": "croety:ore_dust", "count": 2 },
    { "item": "minecraft:gravel", "chance": 0.25 }
  ],
  "processingTime": 200,
  "heatRequirement": "heated"
}
```

（`ingredients` 里物品项用原版 Ingredient 格式 `{"item":...}` / `{"tag":...}`；这是原版 `Ingredient.fromJson` 的约定，
不是 Create 私有格式。）

### 7.3 定义并注册一个新配方类型

Create 侧零件是 `ProcessingRecipe` + `ProcessingRecipeSerializer` + `IRecipeTypeInfo`；
配方类型/序列化器走 Forge 注册表（`ForgeRegistries.RECIPE_TYPES` / `RECIPE_SERIALIZERS`，javap 已确认字段存在）。

`IRecipeTypeInfo` 的实现要点（源码里 datagen 会调用 `getRecipeType().getSerializer().getFactory()`，
所以 `getSerializer()` 必须返回你真正的 `ProcessingRecipeSerializer`）：

```java
package com.croety.recipe;

public final class MyRecipeTypes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "croety");
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, "croety");

    public static final RegistryObject<RecipeType<?>> TYPE =
            TYPES.register("my_processing", () -> RecipeType.simple(Create.asResource("my_processing")));

    public static final RegistryObject<ProcessingRecipeSerializer<MyRecipe>> SERIALIZER =
            SERIALIZERS.register("my_processing", () -> new ProcessingRecipeSerializer<>(MyRecipe::new));

    public static final IRecipeTypeInfo MY_PROCESSING = new IRecipeTypeInfo() {
        @Override public ResourceLocation getId() { return Create.asResource("my_processing"); }
        @SuppressWarnings("unchecked")
        @Override public <T extends RecipeSerializer<?>> T getSerializer() { return (T) SERIALIZER.get(); }
        @SuppressWarnings("unchecked")
        @Override public <T extends RecipeType<?>> T getType() { return (T) TYPE.get(); }
    };

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
        TYPES.register(modBus);
    }
}
```

`MyRecipe::new` 必须能匹配 `ProcessingRecipeFactory<MyRecipe>`，即构造函数只能吃一个 `ProcessingRecipeParams`：

```java
public class MyRecipe extends ProcessingRecipe<Container> {

    public MyRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(MyRecipeTypes.MY_PROCESSING, params);      // typeInfo 在这里注入
    }

    @Override protected int getMaxInputCount()  { return 2; }
    @Override protected int getMaxOutputCount() { return 4; }
    @Override protected int getMaxFluidInputCount()  { return 0; }
    @Override protected int getMaxFluidOutputCount() { return 0; }
    @Override protected boolean canRequireHeat() { return true; }      // 默认 false，必须覆写
    @Override protected boolean canSpecifyDuration() { return true; }  // 默认 false，必须覆写

    @Override
    public boolean matches(Container container, Level level) {
        return !getIngredients().isEmpty() && getIngredients().get(0).test(container.getItem(0));
    }

    // getSerializer() / getType() 已在 ProcessingRecipe 里由 typeInfo 实现好，无需重写
    // 需要额外的 JSON 字段时覆写 readAdditional/writeAdditional
}
```

### 7.4 Datagen 生成器（`com.simibubi.create.api.data.recipe.*`）

`ProcessingRecipeGen` 的类注释原文（**addon 的标准入口**）：
"A base class for all processing recipes, containing helper methods for datagenning processing recipes.
**Addons should extend this for custom processing recipe types, and return that recipe type in `getRecipeType()`.**"

`getRecipeType()` 的注释："Create uses an enum, however this is **not in any way required for addons**."

`create(...)` 的真实行为（源码确认）：内部会 `all.add(generatedRecipe)`，**所以只要调用一次就会进入输出**，
赋值给字段只是 Create 的书写习惯，不是必需。

几个 `create` 重载的参数含义容易搞错：

| 重载 | `String` 参数是 | 生成的文件名 |
|---|---|---|
| `create(String namespace, Supplier<ItemLike> ing, UnaryOperator op)` | **namespace** | `<namespace>:<物品注册名>` |
| `create(Supplier<ItemLike> ing, UnaryOperator op)` | — | `<你的 modid>:<物品注册名>` |
| `create(String name, UnaryOperator op)` | 配方路径 | `<你的 modid>:<name>` |
| `create(ResourceLocation id, UnaryOperator op)` | 完整 id | 原样 |
| `createWithDeferredId(Supplier<ResourceLocation>, UnaryOperator op)` | — | 延迟决定 |

真实用法（摘自 Create 自己的 `CreateMixingRecipeGen`，原样保留结构）：

```java
public final class CreateMixingRecipeGen extends MixingRecipeGen {

    GeneratedRecipe
        TEA = create("tea", b -> b.require(Fluids.WATER, 250)
            .require(Tags.Fluids.MILK, 250)
            .require(ItemTags.LEAVES)
            .output(AllFluids.TEA.get(), 500)
            .requiresHeat(HeatCondition.HEATED)),

        BRASS_INGOT = create("brass_ingot", b -> b.require(CreateRecipeProvider.I.copper())
            .require(CreateRecipeProvider.I.zinc())
            .output(AllItems.BRASS_INGOT.get(), 2)
            .requiresHeat(HeatCondition.HEATED)),

        MUD = create("mud_by_mixing", b -> b.require(BlockTagIngredient.create(BlockTags.CONVERTABLE_TO_MUD))
            .require(Fluids.WATER, 250)
            .output(Blocks.MUD, 1));

    public CreateMixingRecipeGen(PackOutput output) {
        super(output, Create.ID);
    }
}
```

addon 版本（继承 API 包里的基类，返回自己的配方类型）：

```java
public final class CroetyCrushingRecipeGen extends CrushingRecipeGen {   // getRecipeType() 已由基类实现

    public CroetyCrushingRecipeGen(PackOutput output) { super(output, "croety"); }

    GeneratedRecipe MY_ORE = create("my_ore_from_crushing", b -> b
            .duration(200)
            .output(MyItems.ORE_DUST.get(), 2)          // output(ItemLike, int count)
            .output(0.25f, Items.GRAVEL));              // output(float chance, ItemLike)
}

// 自定义配方类型：自己实现 getRecipeType()
public final class CroetyMyProcessingRecipeGen extends ProcessingRecipeGen {

    public CroetyMyProcessingRecipeGen(PackOutput output) { super(output, "croety"); }

    @Override
    protected IRecipeTypeInfo getRecipeType() { return MyRecipeTypes.MY_PROCESSING; }

    GeneratedRecipe STONE_TO_DUST = create("stone_to_dust", b -> b
            .duration(120)
            .require(Items.STONE)
            .output(MyItems.STONE_DUST.get(), 2));
}
```

机械合成（`MechanicalCraftingRecipeGen`，注意 `create` 吃的是 **`com.google.common.base.Supplier`**）：

```java
public final class CroetyMechanicalCraftingGen extends MechanicalCraftingRecipeGen {

    public CroetyMechanicalCraftingGen(PackOutput output) { super(output, "croety"); }

    GeneratedRecipeBuilder MY_ITEM = create(() -> MyItems.MY_ITEM.get())   // com.google.common.base.Supplier<ItemLike>
            .returns(1)                                                    // returns(int)
            .withSuffix("_from_parts")                                     // withSuffix(String)
            .recipe(b -> b.key('P', MyItems.MY_PLATE.get())
                    .key('C', com.simibubi.create.AllItems.COPPER_SHEET.get())
                    .patternLine("PPP")
                    .patternLine("PCP")
                    .patternLine("PPP"));
}
```

挂到 `GatherDataEvent`（`addProvider` 在 **`DataGenerator`** 上，`GatherDataEvent` 没有这个方法）：

```java
public static void onGatherData(GatherDataEvent event) {
    DataGenerator generator = event.getGenerator();
    generator.addProvider(event.includeServer(),
            (DataProvider.Factory<CroetyCrushingRecipeGen>) CroetyCrushingRecipeGen::new);
    generator.addProvider(event.includeServer(),
            (DataProvider.Factory<CroetyMechanicalCraftingGen>) CroetyMechanicalCraftingGen::new);
}
```

条件配方（所有 builder 都支持）：`.whenModLoaded("createaddition")`、`.whenModMissing("x")`、`.withCondition(ICondition)`。

---

## 8. 装置与移动（contraptions）

### 8.1 关键类

`com.simibubi.create.content.contraptions.Contraption`（`public abstract class`）必须实现：

```java
public abstract boolean assemble(Level, BlockPos) throws AssemblyException;
public abstract boolean canBeStabilized(Direction, BlockPos);
public abstract ContraptionType getType();
```

其它常用公开成员：

```java
public AbstractContraptionEntity entity;
public AABB bounds;           public BlockPos anchor;
public boolean stalled;       public boolean disassembled;

public Contraption();
public ContraptionWorld getContraptionWorld();
public static Contraption fromNBT(Level, CompoundTag, boolean);
public boolean searchMovedStructure(Level, BlockPos, Direction) throws AssemblyException;
public void onEntityCreated(AbstractContraptionEntity);
public void onEntityRemoved(AbstractContraptionEntity);
public void onEntityInitialize(Level, AbstractContraptionEntity);
public void readNBT(Level, CompoundTag, boolean);
public CompoundTag writeNBT(boolean);
public void removeBlocksFromWorld(Level, BlockPos);
public void addBlocksToWorld(Level, StructureTransform);
public void startMoving(Level);
public void stop(Level);
public void forEachActor(Level, java.util.function.BiConsumer<MovementBehaviour, MovementContext>);
public java.util.Map<BlockPos, StructureTemplate.StructureBlockInfo> getBlocks();
public java.util.List<MutablePair<StructureTemplate.StructureBlockInfo, MovementContext>> getActors();
public MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> getActorAt(BlockPos);
public java.util.Map<BlockPos, MovingInteractionBehaviour> getInteractors();
public MountedStorageManager getStorage();
public boolean isActorTypeDisabled(ItemStack);
public void setActorsActive(ItemStack, boolean);
public java.util.List<ItemStack> getDisabledActors();
public void invalidateColliders();
public static double getRadius(Iterable<? extends Vec3i>, Direction.Axis);
```

`MovementContext`（`com.simibubi.create.content.contraptions.behaviour.MovementContext`）：

```java
public Vec3 position;  public Vec3 motion;  public Vec3 relativeMotion;
public java.util.function.UnaryOperator<Vec3> rotation;
public Level world;    public BlockState state;   public BlockPos localPos;
public CompoundTag blockEntityData;
public boolean stall;  public boolean disabled;   public boolean firstMovement;
public CompoundTag data;
public Contraption contraption;
public java.lang.Object temporaryData;

public MovementContext(Level, StructureTemplate.StructureBlockInfo, Contraption);
public float getAnimationSpeed();
public static MovementContext readNBT(Level, StructureTemplate.StructureBlockInfo, CompoundTag, Contraption);
public CompoundTag writeToNBT(CompoundTag);
public FilterItemStack getFilterFromBE();
public MountedItemStorage getItemStorage();
public MountedFluidStorage getFluidStorage();
```

`AbstractContraptionEntity`（`public abstract class ... extends Entity implements IEntityAdditionalSpawnData`）
的抽象方法：

```java
protected abstract void tickContraption();
public abstract Vec3 applyRotation(Vec3, float);
public abstract Vec3 reverseRotation(Vec3, float);
protected abstract StructureTransform makeStructureTransform();
protected abstract float getStalledAngle();
protected abstract void handleStallInformation(double, double, double, float);
public abstract AbstractContraptionEntity.ContraptionRotationState getRotationState();
public abstract void applyLocalTransforms(PoseStack, float);
```

（`OrientedContraptionEntity` / `ControlledContraptionEntity` 是现成实现，通常继承它们而不是直接继承 `AbstractContraptionEntity`。）

### 8.2 注册方式

```java
// 1) 自定义 ContraptionType（数据驱动注册表）
public static final DeferredRegister<ContraptionType> CONTRAPTION_TYPES =
        DeferredRegister.create(CreateRegistries.CONTRAPTION_TYPE, "croety");

public static final RegistryObject<ContraptionType> MY_CONTRAPTION =
        CONTRAPTION_TYPES.register("my_contraption", () -> new ContraptionType(MyContraption::new));

// 2) 我方块的移动行为（actor）—— 直接注册表，或 Registrate 的 onRegister helper
MovementBehaviour.REGISTRY.register(MyBlocks.MY_SAW.get(), new MySawMovement());

REGISTRATE.block("my_saw", MySawBlock::new)
        .onRegister(MovementBehaviour.movementBehaviour(new MySawMovement()))   // 推荐
        .simpleItem()
        .register();

// 3) 装置上被右键时的行为
REGISTRATE.block("my_button", MyButtonBlock::new)
        .onRegister(MovingInteractionBehaviour.interactionBehaviour(new MyButtonInteraction()))
        .simpleItem()
        .register();

// 4) 让我的方块可以被装置推动
BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) ->
        state.is(MyBlocks.MY_MACHINE.get())
                ? BlockMovementChecks.CheckResult.SUCCESS
                : BlockMovementChecks.CheckResult.PASS);

// 5) 声明不可推动
ContraptionMovementSetting.REGISTRY.register(MyBlocks.MY_ANCHOR.get(),
        () -> ContraptionMovementSetting.UNMOVABLE);          // 注意 value 是 Supplier

// 6) 让我的容器在装置上保留物品
REGISTRATE.block("my_crate", MyCrateBlock::new)
        .transform(MountedItemStorageType.mountedItemStorage(MyStorageTypes.MY_CRATE))
        .register();
```

`MovementBehaviour.movementBehaviour(...)` 的源码实现就是
`b -> REGISTRY.register(b, behaviour)`，所以 `onRegister` 与直接注册等价。
`MovementBehaviour` 的默认值（源码确认）：`isActive` 返回 `!context.disabled`、
`getActiveAreaOffset` 返回 `Vec3.ZERO`、`cancelStall` 设 `context.stall = false`、
`dropItem` 默认把物品塞进装置存储、塞不下才掉出来（受 `AllConfigs.server().kinetics.moveItemsToStorage` 影响）。

一个移动行为的最小实现：

```java
public class MySawMovement implements MovementBehaviour {

    @Override
    public void startMoving(MovementContext context) {
        // 装置刚组装好
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        Level level = context.world;
        if (level.isClientSide) return;                     // 只在服务端改世界
        if (!level.getBlockState(pos).is(BlockTags.LOGS)) return;
        level.destroyBlock(pos, true);
    }

    @Override
    public void tick(MovementContext context) {
        // 每 tick（isActive 为 false 时不会被调用）
    }

    @Override
    public void stopMoving(MovementContext context) {
        // 装置停下
    }

    @Override
    public void writeExtraData(MovementContext context) {
        // 需要跨 tick 保存状态时写 context.data
    }
}
```

---

## 9. 常见坑

每一条都给出可验证依据；纯推断标「推测」。

1. **`Create.registrate()` 会直接抛异常。** 字节码确认：它用
   `StackWalker.getCallerClass().getPackageName().startsWith("com.simibubi.create")` 判断调用方，不满足则抛
   `UnsupportedOperationException("Other mods are not permitted to use create's registrate instance.")`。
   → addon 必须 `CreateRegistrate.create("croety")`。

2. **`addBehaviours` 是在 `SmartBlockEntity` 的构造函数里调用的**（源码确认），此时子类字段初始化器还没执行。
   `private SmartFluidTankBehaviour tank = null;` 会把 `addBehaviours` 里赋的值覆盖回 null。
   → 在 `addBehaviours` 中赋值的字段，声明时不要写初始化器。

3. **`RegistryEntry.get()` 未注册时抛 NPE（不是返回 null）。** 字节码确认：`get()` = `Objects.requireNonNull(getUnchecked(), ...)`，
   `getUnchecked()` = `delegate.orElse(null)`。类加载期/静态字段初始化调用 `MyBlocks.X.get()` 必炸。

4. **`CreateRegistrate` 的构造函数是 `protected`。** 而且它的 `registerEventListeners(IEventBus)` 必须挂到
   **mod 事件总线**，否则所有 entry 都不会注册。

5. **`setCreativeTab(...)` 不会把物品放进 Create 的标签页。** 它只是把 `entry -> tab` 记进
   `CreateRegistrate.TAB_LOOKUP`，唯一读者是 `CreateRegistrate.isInCreativeTab`；而 Create 的标签页由
   `Create.registrate().getAll(Registries.BLOCK/ITEM)` 遍历 **Create 自己的 registrate** 来填充。
   → 用 `BuildCreativeModeTabContentsEvent`（`event.getTabKey()` + `event.accept(...)`），或建自己的标签页。

6. **`IHaveCustomOverlayIcon` 是 sealed 接口。** 源码：`public sealed interface IHaveCustomOverlayIcon permits IHaveGoggleInformation, IHaveHoveringInformation`。
   → addon **不能**直接 `implements IHaveCustomOverlayIcon`，只能实现两个 `non-sealed` 子接口之一。

7. **`CheckResult.PASS` 没有布尔值。** 源码：`toBoolean()` 对 `PASS` 抛
   `IllegalStateException("PASS does not have a boolean value")`。查询按**注册逆序**，第一个非 PASS 的结果胜出；
   全部 PASS 才走 fallback。

8. **`SimpleRegistry.register/add` 重复注册同一 key 会抛 `IllegalArgumentException`。**
   所以 `BlockStressValues.IMPACTS.register(block, ...)` 之类的调用**只能做一次**；
   如果同时用 `register` 和 `registerProvider`，直接注册的值优先。

9. **`ProcessingRecipe.canRequireHeat()` / `canSpecifyDuration()` 默认都是 `false`。**
   不覆写就永远用不了 `.requiresHeat(...)` / `.duration(...)`，而且构造函数里的 `validate` 只会打 warn，
   表现为"配方存在但时长/热量无效"，很难查。

10. **`ProcessingRecipeGen.create(String, Supplier<ItemLike>, ...)` 的第一个参数是 namespace，不是配方名。**
    配方 id 由**物品的注册名**决定（`<namespace>:<item path>`）。
    想要自定义名字请用 `create(String name, UnaryOperator)`（走 `asResource(name)`）。

11. **`CStress.setImpact(...)` 只能给 Create 自己的方块用。** 源码里有
    `assertFromCreate`，非 Create 方块抛 `IllegalStateException("Non-Create blocks cannot be added to Create's config.")`。
    → addon 用 `BlockStressValues.IMPACTS/CAPACITIES.register(...)`。

12. **不要覆写 `calculateStressApplied()` 除非真的需要动态应力。** 默认实现已经是查 `BlockStressValues`；
    覆写后要自己维护 `lastStressApplied`（tooltip 与网络同步都读它）。

13. **不存在 `RotateBlock` / `GeneratingKineticBlock` / `AbstractKineticBlock`。**
    jar 里查无此类（class 清单逐条确认 ABSENT）。

14. **`KineticBlock` 没有实现 `getRotationAxis`。** 只有 `DirectionalAxisKineticBlock` 与
    `HorizontalAxisKineticBlock` 已经实现；其余方向基类都要你自己写。

15. **覆写 `write` / `read` 一定要调用 `super`。** `KineticBlockEntity.write` 负责
    `Speed` / `Network` / `Source` / `Sequence`，`KineticBlockEntity.read` 在 `wasMoved` 时**直接 return**（不读动能数据）。
    不调 `super` 会导致存档与同步数据丢失。

16. **`SmartBlockEntity` 没有 `getCapability`。** 它来自 Forge 的 `CapabilityProvider`；
    要暴露 tank/inventory 就在自己的 BE 里覆写 `public <T> LazyOptional<T> getCapability(Capability<T>, Direction)`
    （Create 的 `FluidTankBlockEntity` 就是这么写的，见其源码）。

17. **`DataGenerator.addProvider` 只在 `DataGenerator` 上。** `GatherDataEvent` 只有 `getGenerator()` / `getPackOutput()`
    之类的 getter。

18. **`MechanicalCraftingRecipeGen.create(...)` 的参数类型是 `com.google.common.base.Supplier`**，
    不是 `java.util.function.Supplier`。显式声明变量类型时别写错包。

19. **`BlockBuilder.item()` 返回 `ItemBuilder<BlockItem, BlockBuilder<T,P>>`，链尾必须 `.build()`** 才回到父 builder。
    忘了 `.build()` 直接 `.register()` 是编译期类型错误。

20. **`AllTags` 的子类是嵌套类。** Java 里写 `AllTags.AllBlockTags.CASING`；
    javap 命令里要写 `com.simibubi.create.AllTags$AllBlockTags`，且在 PowerShell 里必须用单引号（`$` 会被解释）。

21. **`DisplaySource.getId()` 依赖注册表**（`CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(this)`）。
    没注册进 `DISPLAY_SOURCE` 的对象调用 `getId()` 会拿到 null，`getName()` 会 NPE。
    用 `CreateRegistrate.displaySource(...)` 或 `CreateRegistries.DISPLAY_SOURCE` 注册。

22. **动能机器的应力注册不要放在静态初始化块里。** `IMPACTS.register(block, ...)` 需要真实的 Block 实例，
    而它在方块注册完成前拿不到。放 `FMLCommonSetupEvent`，或用 Registrate 的 `onRegister(...)`。

23. **`KineticBlockEntity.getStressConfigKey()` 默认返回当前 blockstate 的方块。**
    如果同一个 BE 会出现在多个方块上（例如带朝向变体的机器），默认行为已经正确；
    只有需要"多种方块共享一份应力配置"时才覆写（推测其用法意图，签名与默认实现已验证）。

24. **`BlockMovementChecks` 的注册是线程安全的、顺序敏感的。** 后注册的检查先被问；
    一个检查对所有方块都返回 `SUCCESS`/`FAIL` 会**全局**生效，影响别的 mod。返回 `PASS` 才是"我不管"。

25. **`MovementBehaviour.visitNewPosition` 会在客户端和服务端都被调用。**
    改世界前先判 `context.world.isClientSide`（Create 自己的 actor 都是这么写的）。

---

## 10. 验证记录

以下命令均在本机实际执行（PowerShell 5.1 / JDK 17）。
两类证据：**javap**（字节码签名）与 **`tools/find-api.ps1 -Source`**（Create/Ponder/Flywheel 官方 sources jar 的原始源码）。

变量：

```powershell
$javap  = "C:\Users\TW2NTY_NIN9\.jdks\temurin-17\jdk-17.0.20.1+1\bin\javap.exe"
$create = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\com\simibubi\create\create-1.20.1\6.0.8-291_mapped_parchment_2023.09.03-1.20.1\create-1.20.1-6.0.8-291_mapped_parchment_2023.09.03-1.20.1-slim.jar"
$reg    = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\com\tterrag\registrate\Registrate\MC1.20-1.3.3_mapped_parchment_2023.09.03-1.20.1\Registrate-MC1.20-1.3.3_mapped_parchment_2023.09.03-1.20.1.jar"
$mc     = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1\forge-1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1.jar"
$bus    = "C:\Users\TW2NTY_NIN9\.gradle\caches\modules-2\files-2.1\net.minecraftforge\eventbus\6.2.33\3fae69cfa9c5095bcc25c0a8a3ed9b26c156f922\eventbus-6.2.33.jar"
```

### 10.1 总览 / 常量类

```powershell
& $javap -cp $create com.simibubi.create.Create
& $javap -cp $create com.simibubi.create.AllBlocks
& $javap -cp $create com.simibubi.create.AllItems
& $javap -cp $create com.simibubi.create.AllTags
& $javap -cp $create 'com.simibubi.create.AllTags$AllBlockTags'
& $javap -cp $create 'com.simibubi.create.AllTags$AllItemTags'
& $javap -cp $create 'com.simibubi.create.AllTags$AllFluidTags'
& $javap -cp $create 'com.simibubi.create.AllTags$AllEntityTags'
& $javap -cp $create com.simibubi.create.AllRecipeTypes
& $javap -cp $create com.simibubi.create.AllSoundEvents
& $javap -cp $create 'com.simibubi.create.AllSoundEvents$SoundEntry'
& $javap -cp $create com.simibubi.create.AllCreativeModeTabs
& $javap -cp $create com.simibubi.create.AllBlockEntityTypes
& $javap -cp $create com.simibubi.create.AllFluids
# 存在性核对（class 清单来自 jar 解包，非记忆）
Get-Content create-classes.txt | Where-Object { $_ -like 'com.simibubi.create.All*' } | Sort-Object
# 关键否定结论（全部 ABSENT）：
#   com.simibubi.create.content.kinetics.base.RotateBlock
#   com.simibubi.create.content.kinetics.base.GeneratingKineticBlock
#   com.simibubi.create.content.kinetics.base.AbstractKineticBlock
# Create.registrate() 的调用方校验（字节码）
& $javap -c -p -cp $create com.simibubi.create.Create | Select-String -Pattern "registrate|asResource" -Context 6,10
# mods.toml 原文（直接读 jar entry）
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($create)
($zip.Entries | Where-Object FullName -eq 'META-INF/mods.toml').Open()   # 用 StreamReader 读
$zip.Dispose()
```

### 10.2 api 包（javap 全量 dump + 源码核对）

```powershell
$out = "$env:TEMP\croety-api-recon\javap"
foreach ($c in @(
  'com.simibubi.create.api.behaviour.display.DisplaySource',
  'com.simibubi.create.api.behaviour.display.DisplayTarget',
  'com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour',
  'com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior',
  'com.simibubi.create.api.behaviour.movement.MovementBehaviour',
  'com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour',
  'com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior',
  'com.simibubi.create.api.behaviour.spouting.StateChangingBehavior',
  'com.simibubi.create.api.boiler.BoilerHeater',
  'com.simibubi.create.api.connectivity.ConnectivityHandler',
  'com.simibubi.create.api.contraption.BlockMovementChecks',
  'com.simibubi.create.api.contraption.ContraptionMovementSetting',
  'com.simibubi.create.api.contraption.ContraptionType',
  'com.simibubi.create.api.contraption.dispenser.DefaultMountedDispenseBehavior',
  'com.simibubi.create.api.contraption.dispenser.MountedDispenseBehavior',
  'com.simibubi.create.api.contraption.dispenser.MountedProjectileDispenseBehavior',
  'com.simibubi.create.api.contraption.dispenser.OptionalMountedDispenseBehavior',
  'com.simibubi.create.api.contraption.storage.item.MountedItemStorage',
  'com.simibubi.create.api.contraption.storage.item.MountedItemStorageType',
  'com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper',
  'com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage',
  'com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage',
  'com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorageType',
  'com.simibubi.create.api.contraption.storage.item.chest.ChestMountedStorage',
  'com.simibubi.create.api.contraption.storage.item.chest.ChestMountedStorageType',
  'com.simibubi.create.api.contraption.storage.item.menu.MountedStorageMenus',
  'com.simibubi.create.api.contraption.storage.item.menu.StorageInteractionWrapper',
  'com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage',
  'com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType',
  'com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageWrapper',
  'com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage',
  'com.simibubi.create.api.contraption.storage.SyncedMountedStorage',
  'com.simibubi.create.api.contraption.train.PortalTrackProvider',
  'com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries',
  'com.simibubi.create.api.contraption.transformable.TransformableBlock',
  'com.simibubi.create.api.contraption.transformable.TransformableBlockEntity',
  'com.simibubi.create.api.data.TrainHatInfoProvider',
  'com.simibubi.create.api.effect.OpenPipeEffectHandler',
  'com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation',
  'com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation',
  'com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon',
  'com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation',
  'com.simibubi.create.api.equipment.potatoCannon.PotatoCannonProjectileType',
  'com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileBlockHitAction',
  'com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileEntityHitAction',
  'com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileRenderMode',
  'com.simibubi.create.api.event.BlockEntityBehaviourEvent',
  'com.simibubi.create.api.event.PipeCollisionEvent',
  'com.simibubi.create.api.event.TrackGraphMergeEvent',
  'com.simibubi.create.api.packager.InventoryIdentifier',
  'com.simibubi.create.api.packager.unpacking.UnpackingHandler',
  'com.simibubi.create.api.packager.unpacking.VoidingUnpackingHandler',
  'com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback',
  'com.simibubi.create.api.registry.SimpleRegistry',
  'com.simibubi.create.api.registry.CreateRegistries',
  'com.simibubi.create.api.registry.CreateBuiltInRegistries',
  'com.simibubi.create.api.schematic.nbt.PartialSafeNBT',
  'com.simibubi.create.api.schematic.nbt.SafeNbtWriterRegistry',
  'com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries',
  'com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement',
  'com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement',
  'com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement',
  'com.simibubi.create.api.schematic.state.SchematicStateFilter',
  'com.simibubi.create.api.schematic.state.SchematicStateFilterRegistry',
  'com.simibubi.create.api.stress.BlockStressValues'
)) {
  & $javap -cp $create $c | Out-File -Encoding UTF8 (Join-Path $out ($c + ".txt"))
}
# 内部类清单：从 jar entry 里列，避免猜名字
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($create)
$zip.Entries | ForEach-Object { $_.FullName } |
  Where-Object { $_ -like 'com/simibubi/create/api/*' -and $_ -like '*$*' }
$zip.Dispose()
```

内部类：

```powershell
& $javap -cp $create 'com.simibubi.create.api.registry.SimpleRegistry$Provider'
& $javap -cp $create 'com.simibubi.create.api.registry.SimpleRegistry$Multi'
& $javap -cp $create 'com.simibubi.create.api.stress.BlockStressValues$GeneratedRpm'
& $javap -cp $create 'com.simibubi.create.api.contraption.BlockMovementChecks$CheckResult'
& $javap -cp $create 'com.simibubi.create.api.contraption.BlockMovementChecks$MovementAllowedCheck'
& $javap -cp $create 'com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior$CauldronInfo'
& $javap -cp $create 'com.simibubi.create.api.contraption.train.PortalTrackProvider$Exit'
& $javap -cp $create 'com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries$BlockTransformer'
```

**源码核对**（`-Source` 直接打印官方 sources jar 里的原始代码）：

```powershell
.\tools\find-api.ps1 -Source com.simibubi.create.api.stress.BlockStressValues
.\tools\find-api.ps1 -Source com.simibubi.create.api.behaviour.display.DisplaySource
.\tools\find-api.ps1 -Source com.simibubi.create.api.behaviour.movement.MovementBehaviour
.\tools\find-api.ps1 -Source com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour
.\tools\find-api.ps1 -Source com.simibubi.create.api.contraption.BlockMovementChecks
.\tools\find-api.ps1 -Source com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
.\tools\find-api.ps1 -Source com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation
.\tools\find-api.ps1 -Source com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon
.\tools\find-api.ps1 -Source com.simibubi.create.api.registry.registrate.SimpleBuilder
.\tools\find-api.ps1 -Source com.simibubi.create.impl.registry.SimpleRegistryImpl
.\tools\find-api.ps1 -Source com.simibubi.create.AllMovementBehaviours
.\tools\find-api.ps1 -Source com.simibubi.create.AllCreativeModeTabs
.\tools\find-api.ps1 -Source com.simibubi.create.infrastructure.config.AllConfigs
.\tools\find-api.ps1 -Source com.simibubi.create.infrastructure.config.CStress
.\tools\find-api.ps1 -Source com.simibubi.create.foundation.data.CreateRegistrate
.\tools\find-api.ps1 -Source com.simibubi.create.foundation.data.CreateBlockEntityBuilder
```

### 10.3 Registrate / 注册

```powershell
& $javap -cp $create com.simibubi.create.foundation.data.CreateRegistrate
& $javap -cp $create com.simibubi.create.foundation.data.BuilderTransformers
& $javap -cp $create com.simibubi.create.foundation.data.BlockStateGen
& $javap -cp $create com.simibubi.create.foundation.data.AssetLookup
& $javap -cp $create com.simibubi.create.foundation.data.ModelGen
& $javap -cp $create com.simibubi.create.foundation.data.TagGen
& $javap -cp $create com.simibubi.create.foundation.data.SharedProperties
& $javap -cp $create com.simibubi.create.foundation.data.CreateBlockEntityBuilder
& $javap -cp $create com.simibubi.create.foundation.data.CreateEntityBuilder
& $javap -cp $create com.simibubi.create.foundation.data.SpecialBlockStateGen
& $javap -cp $create com.simibubi.create.foundation.data.DirectionalAxisBlockStateGen

& $javap -cp $reg com.tterrag.registrate.AbstractRegistrate
& $javap -cp $reg com.tterrag.registrate.builders.BlockBuilder
& $javap -cp $reg com.tterrag.registrate.builders.ItemBuilder
& $javap -cp $reg com.tterrag.registrate.builders.BlockEntityBuilder
& $javap -cp $reg com.tterrag.registrate.builders.Builder
& $javap -cp $reg com.tterrag.registrate.util.entry.RegistryEntry
& $javap -cp $reg com.tterrag.registrate.util.entry.BlockEntry
& $javap -cp $reg com.tterrag.registrate.util.entry.ItemEntry
& $javap -cp $reg com.tterrag.registrate.util.entry.BlockEntityEntry
& $javap -cp $reg com.tterrag.registrate.util.entry.ItemProviderEntry
# get() 的 NPE 语义
& $javap -c -p -cp $reg com.tterrag.registrate.util.entry.RegistryEntry | Select-String "public T get\(\)|public T getUnchecked\(\)" -Context 0,18
# 真实用法（Create 自己的 AllBlocks，包含 displaySource / mountedItemStorage / CStress 的用法）
.\tools\find-api.ps1 -Source com.simibubi.create.AllBlocks
```

### 10.4 kinetics

```powershell
& $javap -cp $create com.simibubi.create.content.kinetics.base.IRotate
& $javap -cp $create 'com.simibubi.create.content.kinetics.base.IRotate$SpeedLevel'
& $javap -cp $create 'com.simibubi.create.content.kinetics.base.IRotate$StressImpact'
& $javap -cp $create com.simibubi.create.content.kinetics.base.KineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.KineticBlockEntity
& $javap -cp $create com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
& $javap -cp $create com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
& $javap -cp $create com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity
& $javap -cp $create com.simibubi.create.content.kinetics.base.KineticEffectHandler
& $javap -cp $create com.simibubi.create.api.stress.BlockStressValues
# 源码：calculateStressApplied 的默认实现与 tooltip 里的应力公式
.\tools\find-api.ps1 -Source com.simibubi.create.content.kinetics.base.KineticBlockEntity
```

### 10.5 方块实体 / behaviour

```powershell
& $javap -cp $create com.simibubi.create.foundation.blockEntity.SmartBlockEntity
& $javap -cp $create com.simibubi.create.foundation.blockEntity.SyncedBlockEntity
& $javap -cp $create com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity
& $javap -cp $create com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker
& $javap -cp $create com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer$Fluid'
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer$Inventory'
& $javap -cp $create com.simibubi.create.foundation.blockEntity.IMergeableBE
& $javap -cp $create com.simibubi.create.foundation.blockEntity.ComparatorUtil
& $javap -cp $create com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
& $javap -cp $create com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour$TankSegment'
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform$Sided'
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform$Dual'
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase$InterfaceProvider'
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.animatedContainer.AnimatedContainerBehaviour
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour
& $javap -cp $create 'com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour$ValueSettings'
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard
& $javap -cp $create com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter
# BlockEntityBehaviourEvent 的 post 点（字节码）
& $javap -c -p -cp $create com.simibubi.create.foundation.blockEntity.SmartBlockEntity |
   Select-String -Pattern "BlockEntityBehaviourEvent" -Context 2,2
# 源码：addBehaviours 在构造函数里被调用 / 事件 post 时机 / isItemHandlerCap
.\tools\find-api.ps1 -Source com.simibubi.create.foundation.blockEntity.SmartBlockEntity
# 源码：真实 BE 怎么暴露 capability
.\tools\find-api.ps1 -Source com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
& $javap -cp $mc net.minecraftforge.common.capabilities.CapabilityProvider
```

### 10.6 护目镜 / 配方 / contraption / 其它

```powershell
# goggles
.\tools\find-api.ps1 -Source com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
& $javap -cp $create com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
& $javap -cp $create com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation
& $javap -cp $create com.simibubi.create.api.equipment.goggles.IHaveCustomOverlayIcon
& $javap -cp $create com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation
# LangBuilder（在 Ponder jar 里，内含 Catnip）
$ponder = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\net\createmod\ponder\Ponder-Forge-1.20.1\1.0.91_mapped_parchment_2023.09.03-1.20.1\Ponder-Forge-1.20.1-1.0.91_mapped_parchment_2023.09.03-1.20.1.jar"
& $javap -cp $ponder net.createmod.catnip.lang.LangBuilder

# recipe
& $javap -cp $create com.simibubi.create.content.processing.recipe.ProcessingRecipe
& $javap -cp $create com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
& $javap -cp $create 'com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder$ProcessingRecipeFactory'
& $javap -cp $create 'com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder$ProcessingRecipeParams'
& $javap -cp $create com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer
& $javap -cp $create com.simibubi.create.content.processing.recipe.HeatCondition
& $javap -cp $create com.simibubi.create.content.processing.recipe.ProcessingOutput
& $javap -cp $create com.simibubi.create.content.processing.recipe.ProcessingInventory
& $javap -cp $create com.simibubi.create.foundation.fluid.FluidIngredient
& $javap -cp $create com.simibubi.create.foundation.recipe.IRecipeTypeInfo
& $javap -cp $create com.simibubi.create.foundation.recipe.RecipeApplier
& $javap -cp $create com.simibubi.create.foundation.recipe.RecipeConditions
& $javap -cp $create com.simibubi.create.foundation.recipe.RecipeFinder
& $javap -cp $create com.simibubi.create.foundation.recipe.BlockTagIngredient
& $javap -cp $create com.simibubi.create.api.data.recipe.BaseRecipeProvider
& $javap -cp $create com.simibubi.create.api.data.recipe.ProcessingRecipeGen
& $javap -cp $create com.simibubi.create.api.data.recipe.CrushingRecipeGen
& $javap -cp $create com.simibubi.create.api.data.recipe.MixingRecipeGen
& $javap -cp $create com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder
& $javap -cp $create com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen
& $javap -cp $create com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen
& $javap -cp $create com.simibubi.create.api.data.recipe.DatagenMod
& $javap -cp $create com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder
& $javap -cp $mc net.minecraft.world.item.crafting.RecipeType
& $javap -cp $mc net.minecraft.data.DataGenerator
& $javap -cp $mc 'net.minecraft.data.DataProvider$Factory'
& $javap -cp $mc net.minecraftforge.data.event.GatherDataEvent
# 源码：JSON 字段名 / create(...) 的参数含义 / canSpecifyDuration 默认值
.\tools\find-api.ps1 -Source com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer
.\tools\find-api.ps1 -Source com.simibubi.create.content.processing.recipe.ProcessingRecipe
.\tools\find-api.ps1 -Source com.simibubi.create.content.processing.recipe.ProcessingOutput
.\tools\find-api.ps1 -Source com.simibubi.create.api.data.recipe.ProcessingRecipeGen
.\tools\find-api.ps1 -Source com.simibubi.create.api.data.recipe.BaseRecipeProvider
.\tools\find-api.ps1 -Source com.simibubi.create.foundation.data.recipe.CreateMixingRecipeGen

# contraption
& $javap -cp $create com.simibubi.create.content.contraptions.Contraption
& $javap -cp $create com.simibubi.create.content.contraptions.behaviour.MovementContext
& $javap -cp $create com.simibubi.create.content.contraptions.AbstractContraptionEntity

# 其它依赖
& $javap -cp $bus net.minecraftforge.eventbus.api.IEventBus
& $javap -cp $bus net.minecraftforge.eventbus.api.GenericEvent
& $javap -cp $mc net.minecraftforge.registries.DeferredRegister
& $javap -cp $mc net.minecraftforge.registries.ForgeRegistries       # RECIPE_TYPES / RECIPE_SERIALIZERS
& $javap -cp $mc net.minecraftforge.event.BuildCreativeModeTabContentsEvent
& $javap -cp $mc net.minecraftforge.common.capabilities.ForgeCapabilities
& $javap -cp $mc net.minecraftforge.common.util.LazyOptional
```

### 10.7 明确标记「未验证」的条目

- `ConnectivityHandler$SearchCache` 的公开成员（内部缓存类，未展开）。
- `BlockStateGen.*` 各个 provider 工厂的 **boolean 参数具体含义** —— 签名已验证、语义未验证；
  照抄 Create 自己 `AllBlocks` 里的用法最安全（`.\tools\find-api.ps1 -Source com.simibubi.create.AllBlocks`）。
- `IHaveGoggleInformation` / `IHaveHoveringInformation` 在**运行时的具体触发时机**（由 Create 的客户端逻辑调用），
  本文只验证了签名、默认实现与默认返回值。
- 各 `*MovementBehaviour` 的运行时细节（只验证了接口默认实现与 Create 自己的用法）。
- `kinetics.base` 下与渲染相关的类（`RotatingInstance`、`ShaftVisual`、`KineticBlockEntityVisual`、
  `OrientedRotatingVisual`、`SingleAxisRotatingVisual`）只确认了存在性与类名，**未展开方法签名**；
  这部分请参考 `docs/ai/flywheel-ponder.md`。
- 第 9 节里标了「推测」的条目。





