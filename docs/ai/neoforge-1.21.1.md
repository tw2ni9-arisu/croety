# Croety：NeoForge 1.21.1 API 与迁移指南

本指南面向 `D:\Develop\croety-1.21.1-neoforge`。它汇总 NeoForge 1.21.1 官方文档、官方 MDK 和本仓库实际代码/对应依赖源码中的接口；它不是“移植已完成”的证明。当前目标源码仍含 Forge 1.20.1 的包名和方法，写代码时须逐步按这里的版本边界迁移。

## 目标版本、来源和核验边界

| 项目 | 当前目标或本地来源 | 说明 |
|---|---|---|
| Minecraft / Java | 1.21.1 / Java 21 | NeoForge 官方 1.21.1 入门文档要求 JDK 21。 |
| NeoForge / ModDevGradle / Parchment | 21.1.234 / 2.0.107 / 2024.11.17 | NeoForge 采用 Goety 3.2.0 源码使用的 21.1.234（Create 源码使用 21.1.219）；ModDevGradle/Parchment 与 Create 6.0.10 源码一致。目标工程当前仍是 ForgeGradle，尚未按目标配置验证。 |
| Create | 6.0.10 | 本地官方源码 `reference/create/`，标签 `mc1.21.1-6.0.10`；可用开发制品坐标为 `com.simibubi.create:create-1.21.1:6.0.10-281:slim`。该源码给出的配套版本为 Flywheel 1.0.6、Ponder 1.0.82（artifact 后缀 `+mc1.21.1`）、Registrate `MC1.21-1.3.0+67`。 |
| Goety | 用户目标 3.2.00 对应发布版本 3.2.0 | 本地制品名为 `goety-3.2.0.jar`，SHA-256：`EDEEB623F626BC85BD26DF6458DBB440FD4B044F34F3DE78EE715CBC01E692D8`。源码为授权维护仓库 `Vivideru/Goety-3`，版本属性为 3.2.0；结合该制品 javap 核验，不改写发行 JAR。 |
| Curios / Patchouli | Curios 9.5.1+1.21.1 / Patchouli 1.21.1-93-NEOFORGE | Goety 3.2.0 的元数据要求 Curios `[9.5.1,)`；Modrinth 发行把 Curios、Patchouli 都列为 required。本地参考源为 `reference/curios/` 分支 `1.21.1`、`reference/patchouli/` 标签 `release-1.21.1-93`。Croety 的最终依赖声明仍须和 Goety 发布文件、实际仓库坐标一致。 |

Goety 3.2.0 制品中的 `loaderVersion="${loader_version_range}"`、NeoForge `versionRange="${neo_version_range}"` 和 Minecraft `versionRange="${minecraft_version_range}"` 仍是未展开占位符。[FancyModLoader 1.21.1 的 `ModFileInfo`](https://github.com/neoforged/FancyModLoader/blob/1.21.1/loader/src/main/java/net/neoforged/fml/loading/moddiscovery/ModFileInfo.java) / [`ModInfo`](https://github.com/neoforged/FancyModLoader/blob/1.21.1/loader/src/main/java/net/neoforged/fml/loading/moddiscovery/ModInfo.java) 将它们直接传给 [`MavenVersionAdapter`](https://github.com/neoforged/FancyModLoader/blob/1.21.1/loader/src/main/java/net/neoforged/neoforgespi/language/MavenVersionAdapter.java)，不会在这些 range 上做模板替换；NeoForge 的 [1.21.1 构建属性](https://github.com/neoforged/NeoForge/blob/1.21.1/gradle.properties)使用 Maven Artifact 3.8.5。该 parser 将这些裸占位符解析为无限制 `(,)` range（recommended version 仍保留占位符）。因此这些字段本身预计不会因解析失败而阻止加载，但也不会执行声明者原本想要的兼容性限制。这里是根据官方 loader 源和 Maven 3.8.5 parser 的检查结论，**不是一次运行验证**；在客户端/服务端运行中仍需确认 Goety 的实际加载结果。制品 hash 已记下，未被本次文档修改触碰。

准确接口和实现见 [`docs/ai/goety-3.2.0.md`](goety-3.2.0.md) 与用户提供的授权维护仓库 [Vivideru/Goety-3](https://github.com/Vivideru/Goety-3)，源码位于 `reference/goety/src/main/java/`。旧 Goety-2 只作历史参考。父类也要一起查询：例如某方法没在 `ArcaBlockEntity` 本类声明，不代表它没从父类继承。

## API 核验顺序

1. NeoForge 自身接口：看[1.21.1 官方文档](https://docs.neoforged.net/docs/1.21.1/)和[官方 1.21.1 MDK（ModDevGradle）](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)。
2. Create API：查 `reference/create/` 的 6.0.10 源码，而不是旧 `docs/ai/create-6.0.8.md`。
3. Goety API：先看 [`docs/ai/goety-3.2.0.md`](goety-3.2.0.md)，再查精确制品 `reference/artifacts/goety-3.2.0.jar` 与用户授权源码 `reference/goety/src/main/java/`；若只有签名，先写明业务行为待核验。
4. `tools/find-api.ps1` 仍只扫描旧 `libs/sources` / ForgeGradle 缓存，不覆盖 NeoForge 1.21.1 依赖；使用前先确认它实际查询的 jar。不要把旧分支的工具输出当作本分支证据。

## MDK、Gradle、runs 与 Mixin

新工作区应以 NeoForge 官方的 `MDK-1.21.1-ModDevGradle` 为脚手架。NeoForge 官方文档说明 1.21.1 可以选 ModDevGradle 或 NeoGradle；本项目选择 ModDevGradle，和 Create 6.0.10 官方源码采用的 `net.neoforged.moddev` 2.0.107 一致。旧工程的 `net.minecraftforge.gradle`、`fg.deobf(...)`、`minecraft { runs { ... } }` 不是这套构建脚手架的配置方式。

官方 MDK 的形状是 Java 21 toolchain 和 `neoForge { version = ...; runs { ... }; mods { ... } }`；run 块提供 `client()`、`server()`、`data()` 与 `gameTestServer { type = "gameTestServer" }`。GameTest 名称空间通过 system property `neoforge.enabledGameTestNamespaces` 设置；数据生成 run 仍通过 `--mod <modid> --all --output ... --existing ...` 指定数据范围。

NeoForge mod 元数据应位于 `src/main/resources/META-INF/neoforge.mods.toml`。Mixins 在元数据中声明，而不是把 Forge 的 `--mixin` 参数和 SRG refmap system properties 搬进 NeoForge runs：

```toml
[[mixins]]
config="croety.mixins.json"
```

官方 MDK 模板保留了 `[[mixins]]` 示例；Create 1.21.1 官方源码也在其 `neoforge.mods.toml` 中声明 Mixin config。依赖版 Create 的三个现有 Mixin 注入点仍要按 `reference/create/` 当前源码逐项比对，再在 `runClient` 中验证。不能把 Forge 的 `mixin.env.remapRefMap` / `createSrgToMcp/output.srg` 配置当成 NeoForge 必需项。

运行验证目标为 `build`、`runClient`、`runServer`、`runGameTestServer` 和 `runData`；本次准备文档没有运行这些任务，不能据此宣称构建或游戏启动成功。

## Mod 入口、事件总线和客户端事件

新主类由 FML 注入参数。常见形式为：

```java
@Mod(Croety.MODID)
public final class Croety {
    public Croety(IEventBus modEventBus, ModContainer modContainer) {
        MotorContent.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }
}
```

`IEventBus` 与 `ModContainer` 包为 `net.neoforged.bus.api.IEventBus`、`net.neoforged.fml.ModContainer`。这里无需再从 `FMLJavaModLoadingContext` 取得 mod bus。NeoForge 官方 [Mod Files](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/)说明 `@Mod` 构造器可以注入 `IEventBus`、`ModContainer` 等实例。

注册表与启动生命周期事件挂到每个 mod 的 mod bus；游戏运行时事件挂到 NeoForge game bus。将事件注册到哪条总线要按官方事件页核对，不能把两种 bus 混用。

客户端注册保持客户端隔离，例如使用 `net.neoforged.fml.common.EventBusSubscriber`（`@EventBusSubscriber(modid = ..., bus = Bus.MOD, value = Dist.CLIENT)`），或通过客户端专用代码把 listener 加到 mod bus。事件总线与 `@SubscribeEvent` 的接口在 `net.neoforged.bus.api`；生命周期事件在 `net.neoforged.fml.event.lifecycle`。实体和方块实体 renderer 在 mod bus 的 `EntityRenderersEvent.RegisterRenderers` 注册；客户端专用类不得被专用服务端加载。客户端 setup 内需要触及需主线程的游戏注册时使用事件提供的 `enqueueWork`。Create 6.0.10 的 Flywheel visual 注册仍由 Create 自己延迟到客户端 setup；直接调用 `SimpleBlockEntityVisualizer` 时，按 `reference/create/src/main/java/com/simibubi/create/foundation/data/CreateBlockEntityBuilder.java` 与当前项目实际渲染类型检查调用方式。

## 注册、Holder 与 ResourceLocation

NeoForge 的 `DeferredRegister` 包为 `net.neoforged.neoforge.registries.DeferredRegister`；vanilla 注册表使用 `Registries`，NeoForge 添加的注册表使用 `NeoForgeRegistries.Keys`。`DeferredHolder<R,T>` 是 supplier/holder；`RegistryObject<T>` 属于 Forge 旧 API。

本项目有方块、方块物品、物品、方块实体、创造标签页、实体、流体、流体类型等注册：

```java
public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
public static final DeferredBlock<SoulMotorBlock> SOUL_MOTOR =
        BLOCKS.register("soul_motor", () -> new SoulMotorBlock(...));

public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
public static final DeferredItem<Item> WAVING_FOCUS = ITEMS.register("waving_focus",
        () -> new MagicFocus(new WavingSpell()));

private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
```

`DeferredRegister.Blocks` 的 `register` 返回 `DeferredBlock<T>`，`DeferredRegister.Items` 提供 `DeferredItem<T>` 与 block-item helper；两者均是 holder/supplier。其它注册表用一般 `DeferredRegister.create(registry, MODID)`，注册完成要 `register(modEventBus)`。查注册表应在注册结束后通过 `BuiltInRegistries`、相应 registry / holder 获取；不要在注册仍进行时查询。

方块实体类型仍用 `BlockEntityType.Builder.of(...).build(null)`；NeoForge 官方 1.21.1 方块实体文档明确这个版本仍传 `null`。`new ResourceLocation(namespace, path)` 构造函数在 MC 1.21 已私有：改为 `ResourceLocation.fromNamespaceAndPath(namespace, path)`；完整 ID 字符串使用 `ResourceLocation.parse(id)`，默认 minecraft namespace 使用 `withDefaultNamespace(path)`。

## ItemStack：DataComponents 与 CustomData

1.21 将 ItemStack 上的通用附加字段迁移到 data component map。优先使用对应 vanilla component；若确实需要保留一段任意旧 NBT，用 `DataComponents.CUSTOM_DATA` / `CustomData` 包装这段数据；结构稳定且需要类型安全的数据应注册自己的 `DataComponentType<T>`，为存档编码设置 `Codec`、为网络同步设置 `StreamCodec`。

修改 component 值后必须通过 `ItemStack#set` / `update` 等方法写回，不能修改读取到的可变视图后就假设自动保存。不要再照抄 `getOrCreateTag()`、`getTag()`、`setTag()` 等旧 ItemStack 方法。

自定义 component 可用 `DeferredRegister.DataComponents`，持久化与网络同步分别配置编解码器：

```java
public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

public static final DeferredHolder<DataComponentType<?>, DataComponentType<MyData>> MY_DATA =
        DATA_COMPONENTS.registerComponentType("my_data",
                builder -> builder.persistent(MY_DATA_CODEC).networkSynchronized(MY_DATA_STREAM_CODEC));
```

component 值按不可变对象处理；字段变化后构造/更新值并通过 `ItemStack#set` / `update` 写回。DeferredHolder 只用于定位注册的 component 类型。

本项目 stack NBT 主要通过 Goety `ITotem` API 读写；`SoulTransfers` 和流体 GameTest 还读写/保留额外 NBT 标记。Goety 3.2.00 的 `ITotem` 与旧版不同，写灵魂的方法精确大小写为 `setSoulsamount`；`ITotem` 不再继承 `ISoulContainer`，并提供 `tag(ItemStack)` / `updateTag(ItemStack, Consumer<CompoundTag>)`。这些签名由本地 Goety 3.2.0 JAR 的 javap 核对，业务上如何写入或保留其它字段仍须读取 `updateTag` 实现并跑原有分液 GameTest。不要将旧版 `ITotem.setSoulsAmount` 或直接 NBT 调用机械改名。

## SavedData 与 BlockEntity 持久化

### SavedData

1.21.1 `SavedData.save` 和加载函数都需要 `HolderLookup.Provider`；`DimensionDataStorage#computeIfAbsent` 接收 `SavedData.Factory`：

```java
public static final SavedData.Factory<SoulMotorData> FACTORY =
        new SavedData.Factory<>(SoulMotorData::new, SoulMotorData::load);

public static SoulMotorData get(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "croety_soul_motors");
}

public static SoulMotorData load(CompoundTag tag, HolderLookup.Provider registries) {
    SoulMotorData data = new SoulMotorData();
    // 读取 tag
    return data;
}

@Override
public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    // 写入 tag
    return tag;
}
```

改变保存数据后仍须 `setDirty()`。跨维度、全存档共用的 Soul Motor index 当前挂在 Overworld，适合这一存储位置。

### 方块实体

普通 NeoForge/vanilla BlockEntity 使用 `saveAdditional(CompoundTag, HolderLookup.Provider)` 与 `loadAdditional(CompoundTag, HolderLookup.Provider)`，两者都调用 `super`；保存状态变化时调用 `setChanged()`。这是本项目其它自有 BlockEntity 可用的 vanilla 形式。

Create 6.0.10 的 `SmartBlockEntity` / `KineticBlockEntity` 自有序列化钩子则是：

```java
protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket)
protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket)
```

以 `reference/create/src/main/java/com/simibubi/create/content/kinetics/base/KineticBlockEntity.java` 和 `foundation/blockEntity/SmartBlockEntity.java` 为准，并在自定义 override 中把 provider 与 `clientPacket` 传给 `super`。当前 `SoulMotorBlockEntity` 的 Forge 版两参数 `write/read` 必须迁移；不能把 vanilla 的 `saveAdditional/loadAdditional` 直接覆盖到 Create 的内部钩子上。

## 实体同步与 spawn packet

1.21.1 的 `defineSynchedData` 改为接受 `SynchedEntityData.Builder`。SoulOrb 的数据声明保留 `defineId`，默认值在 builder 上定义：

```java
private static final EntityDataAccessor<Long> VALUE =
        SynchedEntityData.defineId(SoulOrb.class, EntityDataSerializers.LONG);

@Override
protected void defineSynchedData(SynchedEntityData.Builder builder) {
    builder.define(VALUE, 1L);
}
```

SoulOrb 直接继承 Entity，此处不调用抽象的 `super.defineSynchedData`；若是继承有具体实现的实体子类，则需调用其父类实现。授权源码的 `vehicle/SeatEntity.java` 展示了直接继承 Entity 的写法。客户端数据仍通过实体的 `entityData.get/set` 访问；实体自己的 accessor 必须用它自身 class 建立，不能混入其他实体类型。

MC 1.21 的官方迁移 primer 将 `Entity#getAddEntityPacket()` 改为 `getAddEntityPacket(ServerEntity)`。当前 SoulOrb 使用的零参数 Forge `NetworkHooks.getEntitySpawningPacket` 不能直接移植。SoulOrb 的自定义 `VALUE` 使用 SynchedEntityData；只有额外 spawn payload 必要时才添加自定义初始化协议，并按 NeoForge 1.21.1 networking docs 实现。具体 packet 行为要在 `runServer` + 客户端跟踪实体时验证。

## 自定义流体与 NeoForge Capabilities

### Fluid

Forge `ForgeFlowingFluid` 在 NeoForge 1.21.1 的类名是 `net.neoforged.neoforge.fluids.BaseFlowingFluid`，有 `BaseFlowingFluid.Source`、`.Flowing` 与 `.Properties`。官方 NeoForge 源码的 Properties 构造签名为 `Properties(Supplier<FluidType>, Supplier<Fluid> still, Supplier<Fluid> flowing)`，之后可设 `.bucket(...)`、可选 `.block(...)`；Create 6.0.10 的 `AllFluids` 也直接使用 `BaseFlowingFluid`。

本项目的 fluid type 注册 key 是 `NeoForgeRegistries.Keys.FLUID_TYPES`。`FluidType.initializeClient` 在 NeoForge 1.21 已标记 deprecated；贴图/渲染扩展挂在 mod bus 的 `RegisterClientExtensionsEvent`，用 `registerFluidType(IClientFluidTypeExtensions, FluidType...)` 注册。不要原样复制 Forge `IClientFluidTypeExtensions` 匿名类放在 `initializeClient` 里的旧写法。

### Block / Entity / Item capability

NeoForge 1.21.1 使用 `Capabilities`、`RegisterCapabilitiesEvent` 和 capability query，而非 Forge 的 `ForgeCapabilities`、`LazyOptional`。BlockEntity handler 可按其 type 注册：

```java
event.registerBlockEntity(
        Capabilities.FluidHandler.BLOCK,
        SOME_BLOCK_ENTITY_TYPE,
        (blockEntity, side) -> /* IFluidHandler 或 null */
);
```

访问其它方块的 fluid handler 时用 `level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)`；无 handler 返回 `null`。自有 SoulReceiver 现在附着到 Goety 的 Arca/Cursed Cage；迁移时改为按这两个 Goety BlockEntityType 注册 provider，而非照搬 `AttachCapabilitiesEvent`、`ICapabilityProvider` 和 `LazyOptional`。Goety 精确类型/父类 API 以授权维护源码和该 3.2.0 制品的 javap 为准。

ItemStack 流体 capability 是 `Capabilities.FluidHandler.ITEM`，其 handler 类型是 `IFluidHandlerItem`。NeoForge 只为 `item.getClass() == BucketItem.class` 注册默认 bucket handler，不包括 `BucketItem` 子类。`SoulBucketItem extends BucketItem` 因而必须显式在 `RegisterCapabilitiesEvent` 中对该 item 注册 `FluidBucketWrapper` 或本项目需要的 handler；不要把旧 `initCapabilities` override 当成已自动支持的入口。NeoForge block capability provider 失效、出现或变更时按文档调用 `level.invalidateCapabilities(pos)` 使缓存失效。

NeoForge 流体相关包一般位于 `net.neoforged.neoforge.fluids`、`net.neoforged.neoforge.fluids.capability`；Create 6.0.10 的官方源码展示其当前实际使用的 fluid API。具体 `FluidStack`、`FluidAction` 和跨模组 handler 方法以 1.21.1 对应 jar/source 为准。

## GameTest 与数据包路径

GameTest 方法仍使用 Minecraft `@GameTest` / `GameTestHelper`；NeoForge holder/prefix 标注来自 `net.neoforged.neoforge.gametest`。本项目的测试依赖 `@GameTestHolder("croety")`、`@PrefixGameTestTemplate(false)` 和 `template="empty"`。在 ModDevGradle 的 `gameTestServer` run 中设置 `neoforge.enabledGameTestNamespaces=croety`，用 `runGameTestServer` 执行测试。

MC 1.21 将多种 datapack 目录改成单数。仓库现有文件迁移时至少处理：

| 旧路径 | 1.21.1 路径 |
|---|---|
| `data/croety/recipes/` | `data/croety/recipe/` |
| `data/croety/tags/items/` | `data/croety/tags/item/` |
| `data/croety/structures/` | `data/croety/structure/` |
| `data/croety/advancements/` | `data/croety/advancement/` |
| `data/croety/loot_tables/` | `data/croety/loot_table/` |

现有 `waving_focus.json`、`waving_sails.json` 和 GameTest 的 `empty.nbt` 应分别落在对应的新目录。NeoForge 官方[资源文档](https://docs.neoforged.net/docs/1.21.1/resources/server/)与 1.20.6→1.21 官方迁移 primer 给出具体 datapack 规范；迁移后检查所有运行时引用和 datagen output。

## Create 6.0.10 与 Goety 3.2.00 的项目接口

### Create

项目 Soul Motor 使用 `GeneratingKineticBlockEntity`、`BlockStressValues`、Kinetic BER 和 Flywheel visual。当前 Create 6.0.10 源码确认：

- `KineticBlockEntity.calculateStressApplied()` / `calculateAddedStressCapacity()` 从 `BlockStressValues` registry 取值；Addon 应继续在 `BlockStressValues.IMPACTS` / `CAPACITIES` 注册 supplier，不要使用 Create 自己的 config builder 去注册 addon block。
- Create 的 `KineticBlockEntity` 序列化方法增加 `HolderLookup.Provider`，详见上文。
- Create 6.0.10 还在通过 `CreateBlockEntityBuilder.visual(...).apply()` 于 client setup 注册 visual；也可看 Create 的 `AllBlockEntityTypes` 使用到的 `SimpleBlockEntityVisualizer` / `OrientedRotatingVisual` 实际组合。
- Create 源码仓库的 `gradle.properties` 指明该版本配套 Flywheel 1.0.6、Ponder 1.0.82、Registrate `MC1.21-1.3.0+67`；依赖坐标仍以 Create 的发布配置和 Goety 实际依赖共同核实。

这里已按 `reference/create/` 的实际 6.0.10 源码校对 API 名；没有在 Croety 上编译或启动验证这些接入代码。

### Goety

完整的 Goety 3.2.0 依赖、图腾、玩家 SE、法术、Arca/Cursed Cage 和 ritual 数据签名见 [`docs/ai/goety-3.2.0.md`](goety-3.2.0.md)。

此分支调用的 Goety 项目接口包括 `ITotem`、SE helpers、Cursed Cage、Arca、spell accepted enchantments 和 ritual datapack。精确 Goety JAR 的 javap 已确认：

- `ITotem` 不再继承 `ISoulContainer`。
- 设置灵魂数量的方法是大小写敏感的 `setSoulsamount`（`amount` 的 `a` 小写）；旧 `setSoulsAmount` 找不到。
- 读取 NBT 的辅助 API 包括 `tag(ItemStack)`；修改应使用 `updateTag(ItemStack, Consumer<CompoundTag>)`，不要直接照搬旧 `ItemStack#hasTag/getTag/setTag` 流程。
- spell `acceptedEnchantments` 的元素是 `ResourceKey<Enchantment>`，不是直接的 `Enchantment` 对象。
- `ArcaBlockEntity#getPlayer` 由父类 `OwnedBlockEntity` 提供；核对类 API 时必须连父类一起查。`CursedCageBlockEntity` 仍提供 `getItem` / `setItem` / `markUpdated`。

这些是基于精确 3.2.0 JAR 的签名结果；`SEHelper` 的副作用、Arca owner 与维度检查、totem 数据合并以及 ritual schema 按授权维护源码和既有 GameTests 核验。Goety 发布制品要求 Curios `[9.5.1,)`；参考版本为 Curios 9.5.1+1.21.1 / Patchouli 1.21.1-93-NEOFORGE。Maven 坐标需写入目标 Gradle 后检查实际解析的制品。不要用旧 `docs/ai/goety-2.5.57.3.md` 补齐未知部分。

## 官方与本地一手资料

- [NeoForge 1.21.1 官方文档首页](https://docs.neoforged.net/docs/1.21.1/)
- [官方 Registries](https://docs.neoforged.net/docs/1.21.1/concepts/registries/)、[Items](https://docs.neoforged.net/docs/1.21.1/items/)、[Blocks](https://docs.neoforged.net/docs/1.21.1/blocks/)、[Block Entities](https://docs.neoforged.net/docs/1.21.1/blockentities/)
- [官方 Data Components](https://docs.neoforged.net/docs/1.21.1/items/datacomponents/)、[Saved Data](https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata/)、[Capabilities](https://docs.neoforged.net/docs/1.21.1/inventories/capabilities/)、[GameTests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/)
- [官方 1.20.4→1.20.5 primer：ItemStack DataComponents / CustomData 迁移](https://docs.neoforged.net/primer/docs/1.20.5/)
- [官方 MDK 仓库](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle)；[NeoForge 1.21.1→FancyModLoader 代码版本](https://github.com/neoforged/FancyModLoader/tree/1.21.1)
- [NeoForge 1.20.6→1.21 官方迁移 primer](https://docs.neoforged.net/primer/docs/1.21/)，用于 `ResourceLocation`、datapack 路径和 entity packet 等 MC 变化。
- 本地 Create 官方源码：`reference/create/`（tag `mc1.21.1-6.0.10`）；Curios：`reference/curios/`（branch `1.21.1`）；Patchouli：`reference/patchouli/`（tag `release-1.21.1-93`）。
- 本地 Goety 制品：`reference/artifacts/goety-3.2.0.jar`；hash 见目标版本表。授权维护源码路径为 `reference/goety/src/main/java/`，锁定提交见 `reference/SOURCES.md`。
