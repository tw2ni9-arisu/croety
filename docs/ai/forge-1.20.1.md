# Forge 1.20.1 模组编写参考

> 本文件里的**所有签名都是用 `javap` 从本项目实际使用的 jar 里导出的**，不是凭记忆写的。
> 对应的 jar 路径与查证方法见文末「如何自查」。
> 环境：Minecraft 1.20.1 + Forge 47.4.23 + Parchment 2023.09.03 映射。

## 目录

- [1. 入口与两条事件总线](#1-入口与两条事件总线)
- [2. mods.toml 与 pack.mcmeta](#2-modstoml-与-packmcmeta)
- [3. 注册：DeferredRegister + RegistryObject](#3-注册deferredregister--registryobject)
- [4. 方块](#4-方块)
- [5. 物品](#5-物品)
- [6. 创造模式标签页](#6-创造模式标签页)
- [7. 方块实体与 ticker](#7-方块实体与-ticker)
- [8. 配置 ForgeConfigSpec](#8-配置-forgeconfigspec)
- [9. 数据生成 datagen](#9-数据生成-datagen)
- [10. 菜单 / GUI](#10-菜单--gui)
- [11. Mixin](#11-mixin)
- [12. 1.20.1 常见坑](#12-1201-常见坑)
- [13. 如何自查](#13-如何自查)

---

## 1. 入口与两条事件总线

Forge 有**两条**总线，用错是最常见的低级错误：

| 总线 | 拿到方式 | 放什么 |
|---|---|---|
| Mod 事件总线 | `context.getModEventBus()` | 注册(Registry)、生命周期(CommonSetup/ClientSetup)、`GatherDataEvent`、`BuildCreativeModeTabContentsEvent` |
| Forge 事件总线 | `MinecraftForge.EVENT_BUS` | 游戏内事件：`ServerStartingEvent`、玩家交互、tick 等 |

本项目已验证可编译的入口（见 `src/main/java/com/croety/Croety.java`）：

```java
@Mod(Croety.MODID)
public class Croety
{
    public static final String MODID = "croety";

    public Croety(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);   // FMLCommonSetupEvent
        BLOCKS.register(modEventBus);                 // DeferredRegister 挂到 mod 总线
        ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);      // 实例方法上的 @SubscribeEvent
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) { /* ... */ }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) { /* ... */ }
}
```

静态事件订阅走注解，注意**必须写对 bus**：

```java
@Mod.EventBusSubscriber(modid = Croety.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public static class ClientModEvents
{
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) { /* ... */ }
}
```

已验证的类型关系：

```
FMLCommonSetupEvent extends ParallelDispatchEvent
BuildCreativeModeTabContentsEvent extends Event implements IModBusEvent, CreativeModeTab.Output
GatherDataEvent extends Event implements IModBusEvent
```

> `FMLCommonSetupEvent` 是并行派发的。要在主线程碰注册表/世界，用 `event.enqueueWork(() -> ...)`。
> `Croety.java` 里就是这么调用 Create/Goety 集成类的。

---

## 2. mods.toml 与 pack.mcmeta

`src/main/resources/META-INF/mods.toml` 里的 `${...}` 占位符由 `build.gradle` 的
`processResources` 任务用 `gradle.properties` 的值展开，**不要写成字面量**。

声明对其它 mod 的依赖（本项目已经这么干了）：

```toml
[[dependencies.${mod_id}]]
    modId="create"
    mandatory=true
    versionRange="[6.0.8,6.1.0)"
    ordering="NONE"
    side="BOTH"
```

- `mandatory=false` 时**必须**给 `ordering`（`BEFORE`/`AFTER`）。
- `side` 取 `BOTH` / `CLIENT` / `SERVER`。
- 版本区间是 Maven 风格：`[a,b)` 含 a 不含 b。

已声明的依赖：`create`、`goety`、`curios`（强制），`patchouli`（可选）。

---

## 3. 注册：DeferredRegister + RegistryObject

全部实测签名：

```java
public static <B> DeferredRegister<B> create(IForgeRegistry<B> registry, String modid);          // 最常用
public static <B> DeferredRegister<B> create(ResourceKey<? extends Registry<B>> key, String modid);
public static <B> DeferredRegister<B> create(ResourceLocation registryName, String modid);

public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> sup);
public void register(IEventBus bus);

// RegistryObject
public T get();                       // 注册完成前调用会抛异常
public ResourceLocation getId();
public ResourceKey<T> getKey();
public boolean isPresent();
public <U> Optional<U> map(Function<? super T, ? extends U> mapper);
```

模式：

```java
public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, Croety.MODID);

public static final RegistryObject<Block> MY_BLOCK =
        BLOCKS.register("my_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));

// 构造函数里必须挂上总线，否则永远不会注册
BLOCKS.register(modEventBus);
```

> `RegistryObject.get()` 在注册事件跑完之前**会抛异常**。类里的 `static final` 字段只放 RegistryObject 本身，
> 需要真实对象时再 `get()`（例如在 commonSetup 之后）。

对于**非 Forge 注册表**（原版 `Registry`），用 `ResourceKey` 版本：

```java
public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Croety.MODID);
```

---

## 4. 方块

`BlockBehaviour.Properties` 实测方法（节选，全部返回 `Properties` 可链式）：

```java
public static Properties of();
public static Properties copy(BlockBehaviour block);     // 复制已有方块的属性
public Properties mapColor(MapColor color);
public Properties mapColor(DyeColor color);
public Properties strength(float destroyTime, float explosionResistance);
public Properties strength(float destroyTime);
public Properties instabreak();
public Properties sound(SoundType sound);
public Properties lightLevel(ToIntFunction<BlockState> light);
public Properties noCollission();
public Properties noOcclusion();
public Properties friction(float f);
public Properties speedFactor(float f);
public Properties jumpFactor(float f);
public Properties randomTicks();
public Properties dynamicShape();
public Properties noLootTable();
public Properties dropsLike(Block block);
public Properties lootFrom(Supplier<? extends Block> block);
public Properties ignitedByLava();
public Properties liquid();
public Properties forceSolidOn();
```

自定义方块继承 `Block`，需要状态属性时重写：

```java
@Override
protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, POWERED);
}
```

### 带方块实体的方块

必须实现 `net.minecraft.world.level.block.EntityBlock`（实测签名）：

```java
public interface EntityBlock {
    BlockEntity newBlockEntity(BlockPos pos, BlockState state);                    // 抽象
    default <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type);
    default <T extends BlockEntity> GameEventListener getListener(ServerLevel level, T blockEntity);
}
```

```java
public class MyMachineBlock extends Block implements EntityBlock
{
    public MyMachineBlock() { super(BlockBehaviour.Properties.of().strength(2f).noOcclusion()); }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MyMachineBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> ((MyMachineBlockEntity) be).serverTick();
    }
}
```

`BlockEntityTicker` 实测只有一个方法：

```java
public interface BlockEntityTicker<T extends BlockEntity> {
    void tick(Level level, BlockPos pos, BlockState state, T blockEntity);
}
```

---

## 5. 物品

`Item.Properties` 实测方法：

```java
public Item.Properties food(FoodProperties food);
public Item.Properties stacksTo(int maxStackSize);
public Item.Properties durability(int maxDamage);
public Item.Properties defaultDurability(int maxDamage);
public Item.Properties craftRemainder(Item item);
public Item.Properties rarity(Rarity rarity);
public Item.Properties fireResistant();
public Item.Properties setNoRepair();
```

```java
public static final RegistryObject<Item> MY_ITEM =
        ITEMS.register("my_item", () -> new Item(new Item.Properties().stacksTo(16)));
```

方块物品用 `BlockItem`：

```java
public static final RegistryObject<Item> MY_BLOCK_ITEM =
        ITEMS.register("my_block", () -> new BlockItem(MY_BLOCK.get(), new Item.Properties()));
```

重写行为时用 1.20.1 的方法名（Parchment 映射，可直接 Ctrl+点击核对）：

```java
@Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { ... }
@Override public boolean isFoil(ItemStack stack) { ... }
@Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) { ... }
```

---

## 6. 创造模式标签页

`CreativeModeTab.Builder` 实测方法：

```java
public Builder title(Component title);
public Builder icon(Supplier<ItemStack> icon);
public Builder displayItems(CreativeModeTab.DisplayItemsGenerator generator);
public Builder withTabsBefore(ResourceKey<CreativeModeTab>... tabs);   // 也有 ResourceLocation... 重载
public Builder withTabsAfter(ResourceKey<CreativeModeTab>... tabs);
public Builder hideTitle();
public Builder noScrollBar();
public Builder alignedRight();
public CreativeModeTab build();
```

```java
public static final RegistryObject<CreativeModeTab> MY_TAB =
        CREATIVE_MODE_TABS.register("my_tab", () -> CreativeModeTab.builder()
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .icon(() -> MY_ITEM.get().getDefaultInstance())
                .displayItems((params, output) -> output.accept(MY_ITEM.get()))
                .build());
```

想往**别人的**标签页塞东西时监听事件（实测方法）：

```java
public final class BuildCreativeModeTabContentsEvent extends Event implements IModBusEvent, CreativeModeTab.Output {
    public ResourceKey<CreativeModeTab> getTabKey();
    public void accept(Supplier<? extends ItemLike> item);
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility);
}

@SubscribeEvent
public void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        event.accept(MY_BLOCK_ITEM);
}
```

---

## 7. 方块实体与 ticker

`BlockEntityType.Builder` 实测：

```java
public static <T extends BlockEntity> Builder<T> of(BlockEntityType.BlockEntitySupplier<? extends T> supplier, Block... validBlocks);
public BlockEntityType<T> build(Type<?> dataFixerType);     // 没有 datafixer 时传 null
```

```java
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Croety.MODID);

public static final RegistryObject<BlockEntityType<MyMachineBlockEntity>> MY_BE =
        BLOCK_ENTITIES.register("my_machine",
                () -> BlockEntityType.Builder.of(MyMachineBlockEntity::new, MY_BLOCK.get()).build(null));
```

**注意构造顺序**：`build(null)` 会立刻读 `MY_BLOCK.get()`，所以方块必须已经注册完毕。用 lambda 延迟求值即可。

方块实体自身：

```java
public class MyMachineBlockEntity extends BlockEntity
{
    public MyMachineBlockEntity(BlockPos pos, BlockState state) { super(MY_BE.get(), pos, state); }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); /* 写 NBT */ }
    @Override public void load(CompoundTag tag) { super.load(tag); /* 读 NBT */ }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) { /* 客户端同步 */ }

    public void serverTick() { /* 每 tick */ }
    @Override public void setChanged() { super.setChanged(); /* 标脏以便保存 */ }
}
```

与服务器同步：`level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL)`。

> Create 的方块实体请**不要**直接继承 `BlockEntity`，改用 Create 的 `SmartBlockEntity`，
> 详见 `docs/ai/create-6.0.8.md`。

---

## 8. 配置 ForgeConfigSpec

`ForgeConfigSpec.Builder` 实测方法（节选）：

```java
public <T> ConfigValue<T> define(String path, T defaultValue);
public <V extends Comparable<? super V>> ConfigValue<V> defineInRange(String path, V defaultValue, V min, V max, Class<V> clazz);
public <T> ConfigValue<List<? extends T>> defineList(String path, List<? extends T> def, Predicate<Object> elementValidator);
public <T> ConfigValue<List<? extends T>> defineListAllowEmpty(String path, List<? extends T> def, Predicate<Object> elementValidator);
public Builder comment(String... comment);
public Builder push(String path);
public Builder pop();
```

`src/main/java/com/croety/Config.java` 是可直接照抄的完整范例：定义 → `BUILDER.build()` 得到 SPEC →
在 `@Mod` 构造函数里 `context.registerConfig(ModConfig.Type.COMMON, Config.SPEC)` →
监听 `ModConfigEvent` 把值读进 `static` 字段。

> 配置值**不要**在静态初始化时读，要等 `ModConfigEvent`，否则会拿到默认值。
> 另外`defineInRange` 的 int 版本用起来是 `defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE)`（自动装箱）。

---

## 9. 数据生成 datagen

`GatherDataEvent` 实测方法：

```java
public DataGenerator getGenerator();
public ExistingFileHelper getExistingFileHelper();
public CompletableFuture<HolderLookup.Provider> getLookupProvider();
public boolean includeServer();
public boolean includeClient();
public boolean includeDev();
public boolean includeReports();
public boolean validate();
```

`build.gradle` 里已经有 `runData` 配置，输出目录是 `src/generated/resources`（已被加进主资源集）。

---

## 10. 菜单 / GUI

`MenuType` 实测：

```java
public MenuType(MenuType.MenuSupplier<T> supplier, FeatureFlagSet requiredFeatures);
public T create(int windowId, Inventory inventory);
public T create(int windowId, Inventory inventory, FriendlyByteBuf data);
// 类声明：class MenuType<T extends AbstractContainerMenu> implements FeatureElement, IForgeMenuType<T>
```

Forge 的带额外数据工厂（实测）：

```java
public interface IForgeMenuType<T> {
    static <T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory);
}
```

即注册时写 `IForgeMenuType.create((windowId, inv, buf) -> new MyMenu(windowId, inv, buf))`。
GUI 画面类在客户端注册，用 `MenuScreens.register(MENU_TYPE.get(), MyScreen::new)`（放在
`FMLClientSetupEvent` 或 `@Mod.EventBusSubscriber(..., value = Dist.CLIENT)` 里）。

---

## 11. Mixin

- **MixinExtras 0.4.1 已在 classpath**（`build.gradle` 的 `implementation`），可以直接用 `@WrapOperation`、
  `@ModifyExpressionValue` 等注解。
- **Create / Goety 的 mixin 已经能正常加载**，靠的是 `build.gradle` 里 runs 的
  `mixin.env.remapRefMap` 两行（见 `AGENTS.md` 的「已知坑」）。
- **要加自己的 mixin**，需要 MixinGradle 插件（`org.spongepowered.mixin`）+ `annotationProcessor`
  生成 refmap，并在 jar manifest 里声明 `MixinConfigs`。本项目 MDK 默认**没有**配这一套；
  动手前先读 `AGENTS.md` 的坑列表，或者干脆优先用 Forge 事件/扩展点解决，避免引入 mixin。

> 说明：这一节没有像其他章节那样逐条 javap 验证（Mixin 是编译期注解处理器，不是可 javap 的运行时 API）。
> 如果确实要加 mixin，务必先用一次 `gradlew build` + `runClient` 验证 refmap 生成正确。

---

## 12. 1.20.1 常见坑

1. **`new ResourceLocation(String)` 已废弃**（本项目编译时会打印 deprecation 警告）。
   1.20.1 实测可用的替代：
   ```java
   ResourceLocation.fromNamespaceAndPath("croety", "my_item");
   ResourceLocation.parse("croety:my_item");      // 解析失败抛异常
   ResourceLocation.tryParse("croety:my_item");   // 失败返回 null
   ```
2. **`RegistryObject.get()` 早于注册完成会抛异常** —— 静态字段里只放 RegistryObject。
3. **两条事件总线别搞混**（见第 1 节），mod 总线的事件挂到 `MinecraftForge.EVENT_BUS` 上不会触发，反之亦然。
4. **`FMLCommonSetupEvent` 是并行的**，需要主线程操作时用 `event.enqueueWork(...)`。
5. **方块物品不会自动注册**，必须自己 `ITEMS.register(...)` 一个 `BlockItem`。
6. **模型/贴图缺失只会打警告不会崩**（本项目模板早期就出现过
   `Unable to load model: 'croety:example_block#inventory'`），补上
   `assets/<modid>/blockstates|models|textures` 即可。注意 1.20.1 的方块状态文件格式是
   `{"variants": {"": {"model": "..."}}}`。

---

## 13. 如何自查

**不要凭记忆写 API。** 用项目自带的查询脚本（它会在所有已反混淆的依赖 jar 里找类并调 javap）：

```powershell
# 打印某个类的真实签名
.\tools\find-api.ps1 -Class net.minecraft.world.item.Item

# 看私有成员
.\tools\find-api.ps1 -Class net.minecraft.world.level.block.Block -Private

# 按名字模糊找类（不知道全名时用这个）
.\tools\find-api.ps1 -Search goggle
.\tools\find-api.ps1 -Search IServant -Jar goety

# 列出会被搜索的所有 jar
.\tools\find-api.ps1 -ListJars
```

它搜索的 jar 就是 Gradle 放在 **compileClasspath** 上的那批（已映射到本项目使用的
Parchment/1.20.1 命名），所以看到什么 javac 就接受什么。

如果你要的类不在 classpath 上，脚本会给出相近名字的建议。
