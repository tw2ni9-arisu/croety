# Goety 2.5.57.3 开发者 API 参考（写给 AI 编码代理）

> **本文档的定位**：这是 `croety` 工作区里**唯一**需要读的 Goety 资料。里面的每一个类名、方法名、
> 字段名、参数表、常量值、NBT 键都来自本机真实 jar 的 `javap` 输出**或真实反编译源码**，
> 不是从记忆或网上教程抄的。写 Goety 集成代码前读完本文，就**不需要再去翻 jar**。
>
> - Goety jar（deobf + Parchment）：
>   `C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\com\polarice3\goety\2.5.57.3_mapped_parchment_2023.09.03-1.20.1\goety-2.5.57.3_mapped_parchment_2023.09.03-1.20.1.jar`
> - Forge / Minecraft jar：
>   `C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1\forge-1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1.jar`
> - Curios jar：
>   `C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\top\theillusivec4\curios\curios-forge\5.14.1+1.20.1_mapped_parchment_2023.09.03-1.20.1\curios-forge-5.14.1+1.20.1_mapped_parchment_2023.09.03-1.20.1.jar`
>
> **两种查证手段**（都在本工作区里，复现命令见 [第 10 节](#10-验证记录)）：
>
> ```powershell
> .\tools\find-api.ps1 -Class  com.Polarice3.Goety.api.magic.ISpell    # javap 签名（权威）
> .\tools\find-api.ps1 -Source com.Polarice3.Goety.api.magic.ISpell    # 反编译真实源码（更适合看行为）
> ```
>
> 本文档中标 `未验证` 的地方就是"两种手段都看不出结论"，请自己确认后再依赖。
> 其余内容都能在第 10 节的命令输出里逐字对上。

---

## 目录

1. [总览 —— com.Polarice3.Goety.Goety](#1-总览--compolarice3goetygoety)
2. [注册表入口 init.* / ModItems / ModBlocks / ModEntityType / ModTags](#2-注册表入口)
3. [Goety 官方 API 包 com.Polarice3.Goety.api.*](#3-goety-官方-api-包-compolarice3goetyapi)
4. [物品写法](#4-物品写法)
5. [法术写法](#5-法术写法)
6. [方块与方块实体写法](#6-方块与方块实体写法)
7. [仆从 / 召唤物](#7-仆从--召唤物)
8. [与 Create / 原版互操作](#8-与-create--原版互操作)
9. [常见坑](#9-常见坑)
10. [验证记录](#10-验证记录)

---

## 1. 总览 —— com.Polarice3.Goety.Goety

### 1.1 真实签名

```text
public class com.Polarice3.Goety.Goety {
  public static final java.lang.String MOD_ID;
  public static final org.slf4j.Logger LOGGER;
  public static com.Polarice3.Goety.init.ModProxy PROXY;
  public static com.Polarice3.Goety.init.SidedInit SIDED_INIT;
  public static net.minecraft.resources.ResourceLocation location(java.lang.String);
  public com.Polarice3.Goety.Goety();
  public static java.nio.file.Path getOrCreateDirectory(java.nio.file.Path, java.lang.String);
  public void onServerStarting(net.minecraftforge.event.server.ServerStartingEvent);
  public void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent);
  static {};
}
```

反编译出来的字段初值（`find-api.ps1 -Source com.Polarice3.Goety.Goety`）：

```java
public class Goety {
   public static final String MOD_ID = "goety";
   public static final Logger LOGGER = LogUtils.getLogger();
   // ...
}
```

就这些 —— 这个类**没有别的 public 成员**。所有注册工作都发生在构造函数和静态初始化块里（见 1.3）。

### 1.2 包名大小写（最容易翻车的地方）

根包是 **`com.Polarice3.Goety`**：

- `Polarice3` 的 **P 大写**；
- `Goety` 的 **G 大写**；
- 中间**没有**小写 `polarice3` 或 `goety` 的变体。

写错不会有"你是不是想写 X"的提示，javac 只会报 `找不到符号`。同一份代码里同时出现
`com.Polarice3.Goety.common.items.ModItems`（大写 P/G）和 `com.croety`（本项目，全小写）是正常的。

### 1.3 Goety 构造函数实际做了什么

反编译 + 字节码双重确认，`Goety()` 里依次执行：

```text
ModBlockEntities.BLOCK_ENTITY.register(bus)
ModEntityType.ENTITY_TYPE.register(bus)
ModFeatures.FEATURES.register(bus)
ModTrunkPlacerTypes.TRUNK_PLACER_TYPES.register(bus)
ModParticleTypes.PARTICLE_TYPES.register(bus)
ModContainerType.CONTAINER_TYPE.register(bus)
ModEnchantments.ENCHANTMENTS.register(bus)
ModLootModifier.GLOBAL_LOOT_MODIFIER.register(bus)
ModRituals.RITUALS.register(bus)
ModStructureTypes.STRUCTURE_TYPE.register(bus)
ModPlacementType.STRUCTURE_PLACEMENT_TYPE.register(bus)
ModProcessors.STRUCTURE_PROCESSOR.register(bus)
ModCreativeTab.CREATIVE_MODE_TABS.register(bus)
MainConfig / AttributesConfig / SpellConfig / BrewConfig / MobsConfig / ItemConfig .loadConfig(spec, path)
ModItems.init(); ModAttributes.init(); ModBlocks.init(); ModFluids.init();
ModRecipeSerializer.init(); ModSpawnEggs.init(); ServantSpawnEggs.init();
GoetyEffects.init(); ModPotions.init(); ModPaintings.init(); ModPotPatterns.init();
ModBanners.init(); ModSounds.init(); ModCriteriaTriggers.init();
SidedInit.init();
```

另外 `ModNetwork.init()` 和 `com.Polarice3.Goety.compat.OtherModCompat.setup(FMLCommonSetupEvent)`
在另一个方法里被调用。

**对插件作者的意义**：Goety 的内容注册**不依赖你先调用任何 init**。你只要在**自己的**
`DeferredRegister` 上调用 `register(YourMod.getInstance().getModEventBus())` 就行，
两边互不干扰。唯一要注意的是：**在 `get()` 之前必须确保对应注册阶段已完成**（见第 9.15 节）。

### 1.4 代码片段

```java
package com.croety.integration;

import com.Polarice3.Goety.Goety;
import net.minecraft.resources.ResourceLocation;

public final class GoetyIds
{
    /** 用 Goety 的命名空间构造 ResourceLocation："goety:soul_bolt_focus"。 */
    public static ResourceLocation goety(String path)
    {
        return Goety.location(path);          // (String) -> ResourceLocation
    }

    public static void sayHello()
    {
        Goety.LOGGER.info("croety 已接入 Goety，modid={}", Goety.MOD_ID);   // "goety"
    }
}
```

`getOrCreateDirectory` 的签名是 `(java.nio.file.Path, java.lang.String) -> java.nio.file.Path`，
是一个"在父目录下建子目录并返回"的工具方法：`Goety.getOrCreateDirectory(root, "croety")`。

---

## 2. 注册表入口

### 2.1 `com.Polarice3.Goety.init.*` 全清单

用 `javap` 与 jar 全类清单确认存在的类：

```text
ClientInitEvents    ClientSideInit      InitEvents          ModAttributes
ModBanners          ModCauldronInteraction                  ModCreativeTab
ModDispenserRegister                    ModKeybindings      ModLootInject
ModLootModifier     ModMobType          ModPaintings        ModPotPatterns
ModProxy            ModShaders          ModSoundTypes       ModSounds
ModTags             ModTrimMaterials    RaidAdditions       SidedInit
```

> **`ModParticles` 不存在。** 粒子注册表在别的包：
> `com.Polarice3.Goety.client.particles.ModParticleTypes`（见 2.7）。

### 2.2 ModCreativeTab —— 反编译源码全文要点

```text
public class com.Polarice3.Goety.init.ModCreativeTab {
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS;
  public static final RegistryObject<CreativeModeTab> TAB;
  public static final RegistryObject<CreativeModeTab> BLOCK_TAB;
  public static final RegistryObject<CreativeModeTab> FOCUS_TAB;
  public static final RegistryObject<CreativeModeTab> SERVANT_TAB;
  public com.Polarice3.Goety.init.ModCreativeTab();
  static {};
}
```

反编译出的真实构造逻辑（已简化掉 import 与 painting 排序器）：

```java
public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "goety");

public static final RegistryObject<CreativeModeTab> TAB =
    CREATIVE_MODE_TABS.register("goety", () -> CreativeModeTab.builder()
        .icon(() -> ((TotemOfSouls) ModItems.TOTEM_OF_SOULS.get()).getDefaultInstance())
        .title(Component.translatable("itemGroup.goety"))
        .withSearchBar()
        .displayItems((parameters, output) -> {
            // Patchouli 手册（仅当 Patchouli 已加载）
            output.accept(((TotemOfSouls) ModItems.TOTEM_OF_SOULS.get()).getEmptyTotem());
            output.accept(((TotemOfSouls) ModItems.TOTEM_OF_SOULS.get()).getFilledTotem());
            output.accept(((FullSpentTotem) ModItems.TOTEM_OF_ROOTS.get()).getEmptyTotem());
            output.accept(((FullSpentTotem) ModItems.TOTEM_OF_ROOTS.get()).getFilledTotem());
            ModItems.ITEMS.getEntries().forEach(i -> {
                if (i.isPresent()
                        && !ModItems.shouldSkipCreativeModTab(i.get())
                        && !(i.get() instanceof BlockItem)     // 方块物品去 BLOCK_TAB
                        && !ModItems.isFocus(i.get())) {       // focus 去 FOCUS_TAB
                    output.accept(i.get());
                }
            });
            // + 把 ModTags.Paintings.MODDED_PAINTINGS 的画都加进来
            ModSpawnEggs.ITEMS.getEntries().forEach(i -> { if (i.isPresent()) output.accept(i.get()); });
        }).build());

public static final RegistryObject<CreativeModeTab> BLOCK_TAB =
    CREATIVE_MODE_TABS.register("goety_block", () -> CreativeModeTab.builder()
        .icon(() -> ModBlocks.SHADE_STONE_CHISELED_BLOCK.get().asItem().getDefaultInstance())
        .title(Component.translatable("itemGroup.goety.block"))
        .withSearchBar()
        .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(i -> {
            if (i.isPresent() && i.get() instanceof BlockItem) output.accept(i.get());
        })).build());

public static final RegistryObject<CreativeModeTab> FOCUS_TAB =
    CREATIVE_MODE_TABS.register("goety_focus", () -> CreativeModeTab.builder()
        .icon(() -> ModItems.FOCUS_BAG.get().getDefaultInstance())
        .title(Component.translatable("itemGroup.goety.focus"))
        .displayItems((parameters, output) -> ModItems.ITEMS.getEntries().forEach(i -> {
            if (i.isPresent() && ModItems.isFocus(i.get())) output.accept(i.get());
        })).build());

public static final RegistryObject<CreativeModeTab> SERVANT_TAB =
    CREATIVE_MODE_TABS.register("goety_servants", () -> CreativeModeTab.builder()
        .icon(() -> ModItems.SOUL_JAR.get().getDefaultInstance())
        .title(Component.translatable("itemGroup.goety.servant"))
        .withSearchBar()
        .displayItems((parameters, output) -> ServantSpawnEggs.ITEMS.getEntries().forEach(i -> {
            if (i.isPresent()) output.accept(i.get());
        })).build());
```

**页签注册名与本地化键**（javap `ldc` + 源码双重确认）：

| 字段 | 注册名 | ResourceKey | 本地化键 | 图标 | 搜索栏 |
|---|---|---|---|---|---|
| `ModCreativeTab.TAB` | `goety` | `goety:goety` | `itemGroup.goety` | `totem_of_souls` | 有 |
| `ModCreativeTab.BLOCK_TAB` | `goety_block` | `goety:goety_block` | `itemGroup.goety.block` | `shade_stone_chiseled_block` | 有 |
| `ModCreativeTab.FOCUS_TAB` | `goety_focus` | `goety:goety_focus` | `itemGroup.goety.focus` | `focus_bag` | 无 |
| `ModCreativeTab.SERVANT_TAB` | `goety_servants` | `goety:goety_servants` | `itemGroup.goety.servant` | `soul_jar` | 有 |

> ⚠️ **关键结论**：四个 generator 全都只遍历 **Goety 自己的** `DeferredRegister`
> （`ModItems.ITEMS` / `ModSpawnEggs.ITEMS` / `ServantSpawnEggs.ITEMS`）。
> **插件注册物品不会自动出现在 Goety 的页签里**，必须走 Forge 的
> `BuildCreativeModeTabContentsEvent`（见 8.1）。

### 2.3 ModAttributes

```text
public class com.Polarice3.Goety.init.ModAttributes {
  public static final RegistryObject<Attribute> SPELL_POTENCY;
  public static final RegistryObject<Attribute> SPELL_DURATION;
  public static final RegistryObject<Attribute> SPELL_RANGE;
  public static final RegistryObject<Attribute> SPELL_RADIUS;
  public static final RegistryObject<Attribute> SPELL_BURNING;
  public static final RegistryObject<Attribute> SPELL_VELOCITY;
  public static final RegistryObject<Attribute> CASTING_SPEED;
  public static final RegistryObject<Attribute> COOLDOWN_DISCOUNT;
  public static final RegistryObject<Attribute> SOUL_DISCOUNT;
  public static final RegistryObject<Attribute> ABYSS_POTENCY;       FROST_POTENCY;
  public static final RegistryObject<Attribute> GEOMANCY_POTENCY;    NECROMANCY_POTENCY;
  public static final RegistryObject<Attribute> NETHER_POTENCY;      STORM_POTENCY;
  public static final RegistryObject<Attribute> VOID_POTENCY;        WILD_POTENCY;
  public static final RegistryObject<Attribute> WIND_POTENCY;
  public static final RegistryObject<Attribute> ABYSS_DISCOUNT;      FROST_DISCOUNT;
  public static final RegistryObject<Attribute> GEOMANCY_DISCOUNT;   NECROMANCY_DISCOUNT;
  public static final RegistryObject<Attribute> NETHER_DISCOUNT;     STORM_DISCOUNT;
  public static final RegistryObject<Attribute> VOID_DISCOUNT;       WILD_DISCOUNT;
  public static final RegistryObject<Attribute> WIND_DISCOUNT;
  public com.Polarice3.Goety.init.ModAttributes();
  public static void init();
  public static int getPotency(net.minecraft.world.entity.LivingEntity);
  public static int getDuration(net.minecraft.world.entity.LivingEntity);
  public static int getRange(net.minecraft.world.entity.LivingEntity);
  public static double getRadius(net.minecraft.world.entity.LivingEntity);
  public static int getBurning(net.minecraft.world.entity.LivingEntity);
  public static float getVelocity(net.minecraft.world.entity.LivingEntity);
  public static int getPotency(net.minecraft.world.entity.LivingEntity, com.Polarice3.Goety.api.magic.ISpell);
  public static double getCastingSpeed(net.minecraft.world.entity.LivingEntity);
  public static double getCooldownDiscount(net.minecraft.world.entity.LivingEntity);
  public static double getSoulDiscount(net.minecraft.world.entity.LivingEntity, com.Polarice3.Goety.api.magic.ISpell);
  public static void modifyEntityAttributes(net.minecraftforge.event.entity.EntityAttributeModificationEvent);
  static {};
}
```

共 **27** 个 `Attribute`（7 个通用 + 10 个 `*_POTENCY` + 10 个 `*_DISCOUNT`）。
`modifyEntityAttributes(EntityAttributeModificationEvent)` 是 Goety 往玩家身上挂这些属性的入口 ——
**插件想让自定义实体也拥有这些属性，就在自己的 `EntityAttributeModificationEvent` 里处理**，
不要调用 Goety 的这个方法。

### 2.4 其余 init 类（逐个实测签名）

```text
// com.Polarice3.Goety.init.ModBanners
public static final DeferredRegister<BannerPattern> BANNER_PATTERNS;
public static final RegistryObject<BannerPattern> CROSS, GALE, MOON;
public static void init();

// com.Polarice3.Goety.init.ModCauldronInteraction  —— 注意这是个 interface
public static final Map<Item, CauldronInteraction> VOID;
public static final Map<Item, CauldronInteraction> MUD;
public static Object2ObjectOpenHashMap<Item, CauldronInteraction> newInteractionMap();
public static void init();

// com.Polarice3.Goety.init.ModDispenserRegister
public static void registerAlternativeDispenseBehavior(ModDispenserRegister$AlternativeDispenseBehavior);
public static List<ModDispenserRegister$AlternativeDispenseBehavior> getSortedAlternativeDispenseBehaviors();
public static BlockPos offsetPos(net.minecraft.core.BlockSource);

// com.Polarice3.Goety.init.ModKeybindings  —— 全是客户端类
public static net.minecraft.client.KeyMapping[] keyBindings;
public static void init();
public static KeyMapping wandSlot();    public static KeyMapping wandCircle();
public static KeyMapping brewCircle();  public static KeyMapping useCurios();

// com.Polarice3.Goety.init.ModSounds
public ModSounds();
public static void init();
static RegistryObject<SoundEvent> create(String);   // 包私有

// com.Polarice3.Goety.init.InitEvents
public static void onRegisterCommandEvent(RegisterCommandsEvent);
public static void registerCapabilities(RegisterCapabilitiesEvent);
public static void attachCapabilities(AttachCapabilitiesEvent<Entity>);
public static void registerListeners(AddReloadListenerEvent);
```

`ModSounds` 里有**几百个** `public static final RegistryObject<SoundEvent>` 常量，
命名形如 `CAST_SPELL`、`SOUL_BOLT_CAST`、`PREPARE_SUMMON`、`SUMMON_SPELL`、`LICH_AMBIENT`……
要用哪个直接 `ModSounds.XXX.get()`。

### 2.5 ModItems —— `com.Polarice3.Goety.common.items.ModItems`

```text
public class com.Polarice3.Goety.common.items.ModItems {
  public static DeferredRegister<Item> ITEMS;      // 注意：非 final
  public static final RegistryObject<...> ...;     // 共 384 个 RegistryObject 字段
  public static void init();
  public static net.minecraft.world.item.Item$Properties baseProperties();
  public static boolean isFocus(net.minecraft.world.item.Item);
  public static boolean shouldSkipCreativeModTab(net.minecraft.world.item.Item);
  static {};
}
```

**反编译出的真实实现**（这三段是最值得照抄的模板）：

```java
public static DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "goety");

// 每个物品都是 ITEMS.register("<id>", () -> new ...)，id 就是物品注册名
public static final RegistryObject<Item> SOUL_ENERGY = ITEMS.register("soul_energy", SoulItem::new);
public static final RegistryObject<DummyItem> JEI_DUMMY_NONE =
        ITEMS.register("jei_dummy/none", () -> new DummyItem(new Item.Properties()));
public static final RegistryObject<Item> SOUL_BOLT_FOCUS =
        ITEMS.register("soul_bolt_focus", () -> new MagicFocus(new SoulBoltSpell()));

public static void init() {
   ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
}

public static Item.Properties baseProperties() {
   return new Item.Properties();
}

public static boolean isFocus(Item item) {
   return item instanceof MagicFocus;
}

public static boolean shouldSkipCreativeModTab(Item item) {
   return item == JEI_DUMMY_NONE.get()
       || item == JEI_DUMMY_REQUIRE_SACRIFICE.get()
       || item == PEDESTAL_DUMMY.get()
       || item == BONE_SHARD.get()
       || item == COOKING_LADLE.get()
       || item instanceof TotemOfSouls
       || item instanceof BrewItem
       || item instanceof SoulItem;
}
```

> `ITEMS` 声明时**没有 `final`**（这和 `ModSpawnEggs.ITEMS` 等不同），照抄时注意。

**已有的、可参考的具体物品**（名字全部来自 javap 字段清单，未杜撰）：

| 字段 | 字段声明类型 |
|---|---|
| `ModItems.TOTEM_OF_ROOTS` | `RegistryObject<com.Polarice3.Goety.common.items.magic.FullSpentTotem>` |
| `ModItems.TOTEM_OF_SOULS` | `RegistryObject<com.Polarice3.Goety.common.items.magic.TotemOfSouls>` |
| `ModItems.JEI_DUMMY_NONE` / `JEI_DUMMY_REQUIRE_SACRIFICE` | `RegistryObject<DummyItem>` |
| `ModItems.RING_OF_WANT` / `RING_OF_THIRST` / `RING_OF_FORCE` / `RING_OF_WRECKING` / `RING_OF_THE_FORGE` / `RING_OF_THE_DRAGON` | `RegistryObject<com.Polarice3.Goety.common.items.curios.SingleStackItem>` |
| `ModItems.DARK_HAT`、`FROST_CROWN`、`WIND_CROWN`、`STORM_CROWN`、`WILD_CROWN`、`ABYSS_CROWN`、`VOID_CROWN`、`NETHER_CROWN`、`NECRO_CROWN`、`NAMELESS_CROWN`、`WITCH_HAT`、`WITCH_HAT_HEDGE`、`CRONE_HAT`、`UNHOLY_HAT`、`UNHOLY_HAT_HALO` | `RegistryObject<SingleStackItem>` |
| `ModItems.DARK_ROBE`、`GRAND_ROBE`、`NECRO_CAPE`、`NAMELESS_CAPE`、`ILLUSION_ROBE`、`GEO_ROBE`、`FROST_ROBE`、`WIND_ROBE`、`STORM_ROBE`、`WILD_ROBE`、`ABYSS_ROBE`、`VOID_ROBE`、`WITCH_ROBE`、`WARLOCK_ROBE`、`NETHER_ROBE`、`UNHOLY_ROBE`、`SEA_AMULET`、`FELINE_AMULET`、`STAR_AMULET`、`WAYFARERS_BELT`、`SPITEFUL_BELT`、`GRAVE_GLOVE`、`THRASH_GLOVE`、`AMETHYST_NECKLACE` | `RegistryObject<SingleStackItem>` |
| 材料 `CURSED_METAL_INGOT`、`PALE_STEEL_INGOT`、`DARK_ALLOY_INGOT`、`ECTOPLASM`、`SHADOW_ESSENCE`、`DARK_FABRIC`、`UNHOLY_FABRIC`、`JADE`、`SOUL_RUBY`、`MAGIC_EMERALD`、`SOUL_EMERALD`、`UNHOLY_BLOOD`、`SOUL_TRANSFER`、`EMPTY_SOUL_JAR`、`SOUL_JAR`、`HOWLING_SOUL`、`SPENT_TOTEM`、`SOUL_ENERGY`、`SOUL_POTTERY_SHERD`、`DARK_SCROLL`、`BONE_SHARD`、`COOKING_LADLE` | `RegistryObject<Item>` |
| 巫术书 `GRIMOIRE_OF_GRUDGES`、`GRIMOIRE_OF_GOODWILL`、`GRIMOIRE_OF_GROUNDING` | `RegistryObject<Item>` |
| 容器 `FOCUS_BAG`、`FOCUS_PACK`、`BREW_BAG` | `RegistryObject<Item>` |
| 药水 `BREW`、`SPLASH_BREW`、`LINGERING_BREW`、`GAS_BREW` | `RegistryObject<Item>` |
| 寻路石 `WAYSTONE` | `RegistryObject<Item>` |
| 法器 `DARK_WAND`、`OMINOUS_STAFF`、`NECRO_STAFF`、`GEO_STAFF`、`WIND_STAFF`、`STORM_STAFF`、`FROST_STAFF`、`WILD_STAFF`、`ABYSS_STAFF`、`VOID_STAFF`、`NETHER_STAFF`、`NAMELESS_STAFF` | `RegistryObject<Item>` |
| 装备 `DARK_HELMET`/`CHESTPLATE`/`LEGGINGS`/`BOOTS`、`CURSED_KNIGHT_*`、`CURSED_PALADIN_*` | `RegistryObject<Item>` |
| 工具武器 `DARK_SCYTHE`、`DARK_SWORD`、`DARK_SHOVEL`、`DARK_PICKAXE`、`DARK_AXE`、`DARK_HOE` | `RegistryObject<Item>` |

Focus 物品（法术载体）的例子：`VEXING_FOCUS`、`BITING_FOCUS`、`FEAST_FOCUS`、`TEETH_FOCUS`、
`SHREDDING_FOCUS`、`ILLUSION_FOCUS`、`IGNITE_FOCUS`、`SOUL_BOLT_FOCUS`、`MAGIC_BOLT_FOCUS`、
`SWORD_FOCUS`、`SOUL_LIGHT_FOCUS`、`GLOW_LIGHT_FOCUS`、`ILLUMINATE_FOCUS`、`CRAFTING_FOCUS`、
`IRON_HIDE_FOCUS`、`BULWARK_FOCUS`、`SOUL_HEAL_FOCUS`、`SHOCKWAVE_FOCUS`、`WEAKENING_FOCUS`、
`WHISPERING_FOCUS`、`SNARING_FOCUS` …
（`goety:focuses` 标签里有 **133** 个 focus 的完整名单，见 8.2 节。）

> **注意类型**：`ModItems` 里绝大多数字段声明成 `RegistryObject<Item>`，即使实际注册的是
> `SingleStackItem` / `DarkWand` 之类的子类。要调用子类特有方法必须自己强转，
> 或者直接用类型是具体类的字段（`TOTEM_OF_SOULS`、`TOTEM_OF_ROOTS`、所有 `SingleStackItem`）。

### 2.6 ModBlocks —— `com.Polarice3.Goety.common.blocks.ModBlocks`

```text
public class com.Polarice3.Goety.common.blocks.ModBlocks {
  public static DeferredRegister<Block> BLOCKS;               // 非 final
  public static final Map<ResourceLocation, ModBlocks$BlockLootSetting> BLOCK_LOOT;
  public static final RegistryObject<Block> ...;              // 共 797 个 RegistryObject 字段
  public static void init();
  public static boolean always(BlockState, BlockGetter, BlockPos);
  public static boolean always(BlockState, BlockGetter, BlockPos, EntityType<?>);
  public static BlockBehaviour$Properties ShadeStoneProperties();
  public static BlockBehaviour$Properties CryptStoneProperties();
  public static BlockBehaviour$Properties JadeStoneProperties();
  public static BlockBehaviour$Properties MarbleProperties();
  public static BlockBehaviour$Properties SlateMarbleProperties();
  public static BlockBehaviour$Properties CragProperties();
  public static BlockBehaviour$Properties HighrockProperties();
  public static BlockBehaviour$Properties SiltstoneProperties();
  public static BlockBehaviour$Properties IndentedGoldProperties();
  public static BlockBehaviour$Properties OminousStoneProperties();
  public static BlockBehaviour$Properties SnowBrickProperties();
  public static BlockBehaviour$Properties EndStoneProperties();
  public static BlockBehaviour$Properties glassProperties();
  public static BlockBehaviour$Properties tintedGlassProperties();
  public static ToIntFunction<BlockState> litBlockEmission(int);
  static {};
}
```

**可用的具体方块**（javap 字段名，全部真实存在）：

- 祭坛/多方块：`ARCA_BLOCK`、`DARK_ALTAR`（+ `DARK_ALTAR_STONE`/`_DEEPSLATE`/`_OMINOUS_STONE`/`_NETHER_BRICK`/`_BLACKSTONE`/`_END_STONE`/`_HIGHROCK`/`_MARBLE`/`_PRISMARINE`/`_CRYPT_STONE`）、`PEDESTAL`（同样一整套材质变体）、`CURSED_INFUSER`、`GRIM_INFUSER`、`CURSED_CAGE_BLOCK`、`SOUL_ABSORBER`、`SOUL_MENDER`、`NIGHT_BEACON`
- 灵魂/风：`SOUL_CANDLESTICK`、`WIND_BLOWER`、`MARBLE_WIND_BLOWER`、`RESONANCE_CRYSTAL`、`ICE_BOUQUET_TRAP`
- 产怪/训练：`BLACK_CRYSTAL`、`ANIMATOR`、`SPIDER_NEST`、`SPIDER_MOTHER_DEN`、`BLAZING_CAGE`、`OMINOUS_PYRE`、`OMINOUS_IDOL`、`VOID_SPAWNER`
- 储物：`RAIDING_CHEST`、`TRAPPED_RAIDING_CHEST`、`CRYPT_CHEST`（`RegistryObject<CryptChestBlock>`）、`LOFTY_CHEST`（`RegistryObject<LoftyChestBlock>`）、`STASH_URN`、`CRYPT_URN`、`PITHOS`
- 宝座（可坐）：`SHADE_THRONE`、`STONE_THRONE`、`DEEPSLATE_THRONE`、`OMINOUS_THRONE`、`BLACKSTONE_THRONE`、`HIGHROCK_THRONE`、`MARBLE_THRONE`、`ROYAL_THRONE`、`FROSTED_THRONE`
- 装饰/杂项：`DARK_ANVIL`（+ `CHIPPED_DARK_ANVIL`/`DAMAGED_DARK_ANVIL`）、`WITCH_POLE`、`BREWING_CAULDRON`、`CRYSTAL_BALL`、`HAUNTED_MIRROR`、`HAUNTED_JUG`、`MAGIC_THORN`、`HOOK_BELL`、`SHRIEKING_OBELISK`、`CREEPER_TOTEM`、`DIAMOND_MOLD_BLOCK`、`REINFORCED_REDSTONE_BLOCK`、`SHADE_STONE_CHISELED_BLOCK`
- 建材：`HAUNTED_GLASS`、`TINTED_HAUNTED_GLASS`、`GRAVE_SOIL`、`JADE_ORE`、`SHADE_BRAZIER`（+ 各材质 `*_BRAZIER`/`*_SOUL_BRAZIER`）、`FREEZING_LAMP`、`JADE_CRYSTAL_LAMP`、`PINE_LANTERN`
- 虚空：`VOID_BLOCK`、`VOID_FLUID`（`RegistryObject<LiquidBlock>`）、`VOID_BARREL`、`VOID_CAULDRON`、`VOID_VAULT`、`VOID_FRAME`、`VOID_SHRINE`、`VOID_FLAME`

### 2.7 ModEntityType / ModBlockEntities / ModParticleTypes

```text
// com.Polarice3.Goety.common.entities.ModEntityType
public static final DeferredRegister<EntityType<?>> ENTITY_TYPE;   // 296 个 RegistryObject 字段
public ModEntityType();      // 没有 init()！注册在 Goety 构造函数里

// com.Polarice3.Goety.common.blocks.entities.ModBlockEntities
public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY;   // 非 final
public ModBlockEntities();   // 同样没有 init()

// com.Polarice3.Goety.client.particles.ModParticleTypes
public static DeferredRegister<ParticleType<?>> PARTICLE_TYPES;    // 非 final
public static final RegistryObject<SimpleParticleType> ...
public ModParticleTypes();
static {};
```

`ModEntityType` 的字段类型是**具体泛型**，例如
`RegistryObject<EntityType<com.Polarice3.Goety.common.entities.projectiles.NetherMeteor>> NETHER_METEOR`、
`RegistryObject<EntityType<...util.SummonCircleVariant>> SUMMON_FIERY`、
`RegistryObject<EntityType<...util.SummonApostle>> SUMMON_APOSTLE`、
`RegistryObject<EntityType<...vehicle.SeatEntity>> SEAT`、
`RegistryObject<EntityType<...ally.illager.raider.AllyVex>> VEX_SERVANT`（`VexSpell` 源码里用到的）。
实体完整名单请 `javap com.Polarice3.Goety.common.entities.ModEntityType`（303 行）。

### 2.8 ModTags —— 标签常量全表（**不是空的**）

前面的 recon 类清单漏掉了嵌套类，`ModTags` 其实有 **10 个 public 嵌套类**，每个都持有
`public static final TagKey<...>` 常量。真实结构（反编译 + javap 双重确认）：

```java
public class ModTags {
   public static void init() {
      Blocks.init(); Items.init(); Effects.init(); Paintings.init(); BannerPatterns.init();
      EntityTypes.init(); Biomes.init(); GameEvents.init(); Structures.init(); DamageTypes.init();
   }
}
// 每个嵌套类的私有 tag() 都是： ItemTags.create(Goety.location(name))
```

`ModTags.Items`（27 个，`TagKey<Item>`）：

```text
WANDS  STAFFS  TOTEMS  ROBES  CAPES  CROWNS  FOCUSES  GRIMOIRES  PLUSHIE
PILLAGER_WEAPONS  VINDICATOR_WEAPONS  MOUNTAINEER_WEAPONS
CHEF_CAN_COOK  CHEF_CANNOT_COOK
CRUSHER_CAN_SMELT  CRUSHER_CANNOT_SMELT  CRUSHER_CANNOT_CRAFT
BREWABLE_FOOD  GRAVE_GLOVE_BOOST  THRASH_GLOVE_BOOST
WRECKABLE  UNWRECKABLE  MAGIC_SWORD_SHOOTABLE
LICH_WITHER_ITEMS  RESPAWN_BOSS  WITCH_CURRENCY  WITCH_BETTER_CURRENCY
```

`ModTags.DamageTypes`（10 个，`TagKey<DamageType>`）：

```text
PHYSICAL  FIRE_ATTACKS  FROST_ATTACKS  SHOCK_ATTACKS  WATER_ATTACKS
MAGIC_FIRE  HELLFIRE  NO_KNOCKBACK  WANTING_DAMAGE  LICH_IMMUNE
```

`ModTags.Blocks`（34 个，`TagKey<Block>`）：

```text
HAUNTED_LOGS  SHADE_STONE  MARBLE_BLOCKS  INDENTED_GOLD_BLOCKS  JADE_BLOCKS
RED_MOSS_BLOCKS  OMINOUS_BLOCKS  END_STONE  CHORUS_GRASS_BLOCKS  END_SOIL_BLOCKS
ANCIENT_PLANTS  PLUSHIE  PHILOSOPHERS_MACE_HARD  RECALL_BLOCKS  RAIDING_CHESTS
DARK_ANVILS  VOID_BLOCKS  TUNNEL_BLACKLIST  NETHER_SPREAD  NETHER_SPREAD_REPLACEABLE
RED_MOSS_PLANTABLES  CHORUS_GROW  END_PLANTABLES  END_GROWTH_BLOCKS
CHORUS_SAPLING_GROW  CHORUS_BLOSSOM_GROW  REDSTONE_CUBE_DETECT  REDSTONE_CUBE_EXEMPT
PRISONER_MINEABLE  PRISONER_UNMINEABLE  PRISONER_RARE_ORES  CHEF_WORK_TABLES
MONSTROSITY_BREAKS  HEATING
```

`ModTags.EntityTypes`（61 个，`TagKey<EntityType<?>>`）：`CREEPERS`、`ENDERMEN`、`VILLAGERS`、
`SERVANTS`、`VILLAGE_GUARDS`、`ZOMBIE_SERVANTS`、`SKELETON_SERVANTS`、`NO_HEAL_SERVANTS`、
`POWERFUL_SERVANTS`、`TESSERACT_SMALL`/`_MEDIUM`/`_LARGE`、`NO_SEATING`、`HOLE_IMMUNE`、
`VOID_TOUCHED_IMMUNE`、`WANTING_ENTITIES`、`SUMMON_KILL`、`IGNORE_SERVANTS`、`SOULLESS`、
`UNSTUNNABLE`、`UNTANGLEABLE`、`UNBLOWABLE_ENTITIES`、`UNSHACKLEABLE`、`UNWRECKABLE`、
`BANISH_IMMUNE`、`TELEKINESIS_IMMUNE`、`SKELETON_WOLF_BUFF`、`SERVANT_RIDEABLE`、
`FRAYED_CONVERT`、`RATTLED_CONVERT`、`REGULAR_CONVERT`、`CAIRN_CONVERT`、`MOSSY_CONVERT`、
`DROWNED_CONVERT`、`WITHER_CONVERT`、`MINI_BOSSES`、`GLOBAL_MUSIC_BOSS`、`RAID_BOSS`、
`BIC_SHIELDED_MOBS`、`APOSTLE_OTHER_ALLIES`、`WITCH_SET_NEUTRAL`、`ABYSS_SET_NEUTRAL`、
`GEO_SET_NEUTRAL`、`FROST_SET_NEUTRAL`、`WIND_SET_NEUTRAL`、`STORM_SET_NEUTRAL`、
`WILD_SET_NEUTRAL`、`NETHER_SET_NEUTRAL`、`VOID_SET_NEUTRAL`、`NECRO_SET_NEUTRAL`、
`LICH_NEUTRAL`、`ABYSS_HEAL`、`FROST_HEAL`、`WIND_HEAL`、`STORM_HEAL`、`WILD_HEAL`、
`GEO_HEAL`、`NETHER_HEAL`、`VOID_HEAL`、`NECRO_HEAL`、`NECRO_NO_DEBUFF`

`ModTags.Biomes`（47 个）：`COMMON_BLACKLIST`、各种 `*_SPAWN`/`*_EXCLUDE_SPAWN`、
9 个 `*_DISCOUNT`、9 个 `*_MARKUP`、`ILLAGER_ASSAULT_BLACKLIST`、`NO_SUNLIGHT`。

`ModTags.Structures`（25 个）：`WITHER_NECROMANCER_SPAWNS`、`VIZIER_SPAWNS`、`CRONE_SPAWNS`、
`SKULL_LORD_SPAWNS`、`CRYPT`、`NECROMANCER_POWER`、`CAN_SUMMON_BRUTES`、
`CAN_SUMMON_WITHER_SKELETONS`、`CAN_SUMMON_BORDER_WRAITHS`、`NECROMANCER_SPAWN`、`CRYPT_EXPLORER`、
`GRAVEYARD`、`SPIDER_DEN`、`BLIGHTED_SHACK`、`RUINED_MONASTERY`、`DARK_MANOR`、`WIND_SHRINE`、
`OMINOUS_BLACKSMITH`、`SORCEROUS_KEEP`、`FINAL_TERMINAL`、`NECRO_HOSTILE`、`VOID_HOSTILE` …

其余：`ModTags.Effects`（`LICH_IMMUNE`、`UNPURIFIABLE`）、`ModTags.GameEvents`（`BLOCK_EVENTS`）、
`ModTags.BannerPatterns`（`PATTERN_ITEM_CROSS`/`_GALE`/`_MOON`）、`ModTags.Paintings`（`MODDED_PAINTINGS`）。

**用法**：

```java
import com.Polarice3.Goety.init.ModTags;

// 判断物品是不是 focus
if (stack.is(ModTags.Items.FOCUSES)) { /* ... */ }

// 把自己注册的仆从实体加进"算作仆从"的判定
// （通过数据包 JSON 加进去，代码里不要改这个 TagKey）
```

### 2.9 声明风格总结

| 注册表 | 持有者 | 字段 | 注册时机 |
|---|---|---|---|
| Item | `ModItems` | `public static DeferredRegister<Item> ITEMS`（非 final） | `ModItems.init()` → `ITEMS.register(modBus)` |
| Item（刷怪蛋） | `ModSpawnEggs` / `ServantSpawnEggs` | `public static final DeferredRegister<Item> ITEMS` | 各自的 `init()`（Goety 构造调用） |
| Block | `ModBlocks` | `public static DeferredRegister<Block> BLOCKS`（非 final） | `ModBlocks.init()` |
| BlockEntity | `ModBlockEntities` | `public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY` | Goety 构造函数直接 register |
| EntityType | `ModEntityType` | `public static final DeferredRegister<EntityType<?>> ENTITY_TYPE` | Goety 构造函数直接 register |
| ParticleType | `ModParticleTypes` | `public static DeferredRegister<ParticleType<?>> PARTICLE_TYPES` | Goety 构造函数直接 register |
| Attribute | `ModAttributes` | 内部 `DeferredRegister`（没有公开字段） | `ModAttributes.init()` |
| CreativeModeTab | `ModCreativeTab` | `public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS` | Goety 构造函数直接 register |

**没有"直接 new 一个 item 就完事"的写法** —— Goety 全线用 `RegistryObject`。
它也没有给自己的物品写 public 的 `get()` 包装方法。

---

## 3. Goety 官方 API 包 `com.Polarice3.Goety.api.*`

这个包是 Goety **明确对外**的扩展点。用 jar 的全部 `.class` 条目枚举确认，它**恰好 50 个类**，
而且**没有任何嵌套类**（`api.*` 下 `$` 后缀条目数为 0）：

```text
api.blocks.IEnchanteableBlock           api.entities.IAutoRideable
api.blocks.IEnchantedBlock              api.entities.IBreathing
api.blocks.ISeat                        api.entities.ICharger
api.blocks.entities.IBarrack            api.entities.IChunkLoader
api.blocks.entities.IOwnedBlock         api.entities.ICustomAttributes
api.blocks.entities.ISoulCandle         api.entities.IGolem
api.blocks.entities.ITrainingBlock      api.entities.IHeretic
api.blocks.entities.IWaystoneBlock      api.entities.IHiding
api.blocks.entities.IWindPowered        api.entities.IHungry
api.client.SpellArmPose                 api.entities.IMobCrafter
api.entities.ally.IAquaServant          api.entities.IOwned
api.entities.ally.IServant              api.entities.IRM
api.entities.ally.illager.IllagerType   api.entities.ISpellEntity
api.entities.ally.illager.ILooter       api.entities.ITrainable
api.entities.ally.illager.ITrainIllager api.items.armor.ISoulDiscount
api.items.IPersist                      api.items.curios.IActivatable
api.items.IPersistDecorator             api.items.magic.IFocus
api.items.ISoulRepair                   api.items.magic.ISoulContainer
api.magic.GolemType                     api.items.magic.ITotem
api.magic.IBlockSpell                   api.items.magic.IWand
api.magic.IBreathingSpell               api.magic.IChargingSpell
api.magic.IMold                         api.magic.ISpell
api.magic.ISummonSpell                  api.magic.ITouchSpell
api.magic.SpellPoses                    api.magic.SpellType
api.ritual.IRitualType                  api.ritual.RitualType
```

> ⚠️ **两个名字是错的，别写**：
> - `com.Polarice3.Goety.api.entities.IServant` —— **不存在**，正确的是
>   `com.Polarice3.Goety.api.entities.**ally**.IServant`。
> - `com.Polarice3.Goety.api.entities.IChargeable` —— **不存在**，正确的是 `ICharger`。

### 3.1 `api.items.magic`

```text
public interface IFocus {
  public abstract com.Polarice3.Goety.api.magic.ISpell getSpell();
}
```
把物品变成"法术载体"。Goety 自己的 `MagicFocus` 就实现了它。

```text
public interface ISoulContainer {
  public static final java.lang.String SOULS_AMOUNT = "Souls";
  public static boolean isEmpty(ItemStack);
  public static int currentSouls(ItemStack);
  public static void setSoulsAmount(ItemStack, int);
  public static void decreaseSouls(ItemStack, int);
}
```

反编译出的真实实现（**注意那两个 `instanceof ISoulContainer` 守卫**）：

```java
public interface ISoulContainer {
   String SOULS_AMOUNT = "Souls";

   static boolean isEmpty(ItemStack itemStack) {
      if (itemStack.getTag() == null) return true;
      return itemStack.getTag().getInt("Souls") == 0;
   }

   static int currentSouls(ItemStack itemStack) {
      return itemStack.getTag() != null ? itemStack.getTag().getInt("Souls") : 0;
   }

   static void setSoulsAmount(ItemStack itemStack, int souls) {
      if (itemStack.getItem() instanceof ISoulContainer) {        // ← 关键守卫
         itemStack.getOrCreateTag().putInt("Souls", souls);
      }
   }

   static void decreaseSouls(ItemStack itemStack, int souls) {
      if (itemStack.getItem() instanceof ISoulContainer && itemStack.getTag() != null) {
         int soulCount = itemStack.getTag().getInt("Souls");
         if (!isEmpty(itemStack)) {
            itemStack.getOrCreateTag().putInt("Souls", Math.max(soulCount - souls, 0));
         }
      }
   }
}
```

> ⚠️ **想用这套静态方法，你的物品类必须 `implements ISoulContainer`**，否则
> `setSoulsAmount` / `decreaseSouls` 会**静默什么都不做**（读取方法不受影响）。

```text
public interface ITotem extends ISoulContainer {
  public static final java.lang.String MAX_SOUL_AMOUNT = "Max Souls";   // 注意：带空格
  public static final int MAX_SOULS;
  public abstract int getMaxSouls();
  public default void setTagTick(ItemStack);
  public static boolean isFull(ItemStack);
  public static boolean isEmpty(ItemStack);
  public static boolean UndyingEffect(net.minecraft.world.entity.player.Player);
  public static int currentSouls(ItemStack);
  public static int maximumSouls(ItemStack);
  public static void setSoulsAmount(ItemStack, int);
  public static void setSoulsamount(ItemStack, int);
  public static void setMaxSoulAmount(ItemStack, int);
  public static void increaseSouls(ItemStack, int);
  public static void decreaseSouls(ItemStack, int);
  static {};
}
```

反编译出的关键事实：

```java
String MAX_SOUL_AMOUNT = "Max Souls";
int MAX_SOULS = MainConfig.MaxSouls.get();     // ← 运行期配置值，不是编译期常量

int getMaxSouls();                              // 唯一抽象方法

/** @deprecated */
@Deprecated(forRemoval = true)
static void setSoulsamount(ItemStack itemStack, int souls) { setSoulsAmount(itemStack, souls); }
```

> `MAX_SOULS` 来自 `com.Polarice3.Goety.config.MainConfig.MaxSouls`，**不要当编译期常量用**
> （不能出现在 `switch` 的 `case` 里，也不能做常量折叠）。
> `setSoulsamount`（全小写 a）已经标记 `forRemoval`，**不要用**。

```text
public interface IWand extends net.minecraftforge.common.extensions.IForgeItem {
  public static final java.lang.String SOULUSE  = "Soul Use";
  public static final java.lang.String CASTTIME = "Cast Time";
  public static final java.lang.String SOULCOST = "Soul Cost";
  public static final java.lang.String DURATION = "Duration";
  public static final java.lang.String COOLDOWN = "Cooldown";
  public static final java.lang.String COOL     = "Cool";
  public static final java.lang.String SECONDS  = "Seconds";
  public static final java.lang.String SHOTS    = "Shots";
  public abstract com.Polarice3.Goety.api.magic.SpellType getSpellType();
  public default java.util.List<com.Polarice3.Goety.api.magic.SpellType> getSpellTypes();
  public static net.minecraft.world.item.ItemStack getFocus(ItemStack);
  public static com.Polarice3.Goety.api.items.magic.IFocus getMagicFocus(ItemStack);
  public default com.Polarice3.Goety.api.magic.ISpell getSpell(ItemStack);
  public default int SoulUse(LivingEntity, ItemStack);
  public default boolean cannotCast(LivingEntity, ItemStack);
  public default boolean cannotCast(LivingEntity, ItemStack, ISpell);
  public default boolean isOnCooldown(LivingEntity, ItemStack);
  public default boolean isNotInstant(ISpell);
  public default boolean isNotInstant(ISpell, LivingEntity, ItemStack);
  public default boolean notTouch(ISpell);
  public default void useParticles(Level, LivingEntity, ItemStack, ISpell);
  public default float getWandVisualHeight(Level, LivingEntity, ItemStack);
  public default int currentCastTime(LivingEntity, ItemStack);
  public default int ShotsFired(ItemStack);
  public default void failParticles(Level, LivingEntity);
  public static net.minecraftforge.items.IItemHandler getItemHandler(ItemStack);
  public default net.minecraft.nbt.CompoundTag getShareTag(ItemStack);
  public default void readShareTag(ItemStack, CompoundTag);
  public default net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack, CompoundTag);
  public default boolean shouldCauseReequipAnimation(ItemStack, ItemStack, boolean);
}
```

反编译出的关键实现（决定"focus 是怎么塞进法杖的"）：

```java
SpellType getSpellType();                        // 唯一抽象方法

default List<SpellType> getSpellTypes() { List<SpellType> list = new ArrayList(); list.add(this.getSpellType()); return list; }

static ItemStack getFocus(ItemStack itemstack) {
   SoulUsingItemHandler handler = SoulUsingItemHandler.get(itemstack);   // ← 走 item capability
   return handler.getSlot();
}

static IFocus getMagicFocus(ItemStack itemStack) {
   if (getFocus(itemStack) != null && !getFocus(itemStack).isEmpty()) {
      if (getFocus(itemStack).getItem() instanceof IFocus focus) return focus;
   }
   return null;
}

default ISpell getSpell(ItemStack stack) {
   IFocus focus = getMagicFocus(stack);
   return focus != null && focus.getSpell() != null ? focus.getSpell() : null;
}

default boolean isOnCooldown(LivingEntity livingEntity, ItemStack stack) {
   if (livingEntity instanceof Player player && getFocus(stack) != null) {
      return SEHelper.isOnCooldown(player, getFocus(stack));
   }
   return false;
}
```

> `getFocus` **不是**读 NBT，而是通过 `com.Polarice3.Goety.common.items.handler.SoulUsingItemHandler`
> 这个 `IItemHandler` capability 取第 0 格。该 handler 的 `isItemValid` 只接受
> `stack.getItem() instanceof IFocus`，`getSlotLimit` 为 1。
> `SoulUsingItemHandler.get(stack)` 在 capability 缺失时会抛
> `IllegalArgumentException("ItemStack is missing item capability")`。
> 所以**自己做法杖时要照 `DarkWand` 那样在 `initCapabilities` 里提供这个 capability**。

所有 `IWand` 的 NBT 键**都带空格**（`"Soul Cost"` 等），这是历史存档兼容约定，不要改成下划线。

### 3.2 `api.items` 及其子包

```text
public interface IPersist {                       // 物品损坏/未损坏的持久化（如法器用尽）
  public default boolean isNotBroken(ItemStack);
  public default boolean isBroken(ItemStack);
}

public class IPersistDecorator implements net.minecraftforge.client.IItemDecorator {
  public static final net.minecraft.resources.ResourceLocation BROKEN_OVERLAY;
  public IPersistDecorator();
  public boolean render(GuiGraphics, Font, ItemStack, int, int);
  static {};
}

public interface ISoulRepair {
  public default void repairTick(ItemStack, net.minecraft.world.entity.Entity, boolean);
}

public interface com.Polarice3.Goety.api.items.armor.ISoulDiscount {
  public default int getSoulDiscount(net.minecraft.world.entity.EquipmentSlot);
  public default int getSoulDiscount(net.minecraft.world.entity.EquipmentSlot, ItemStack);
  public default net.minecraft.network.chat.Component soulDiscountTooltip(ItemStack);
}

public interface com.Polarice3.Goety.api.items.curios.IActivatable {
  public default void activate(Level, net.minecraft.world.entity.player.Player, ItemStack);
}
```

这五个**都没有抽象方法**，是纯粹的"标记 + 默认实现"接口。
`ISoulDiscount` 让护甲声明它给灵魂消耗打几折（配合 `ModAttributes.SOUL_DISCOUNT` 属性）；
`IActivatable` 由 Goety 的"使用饰品"按键（`ModKeybindings.useCurios()`）触发。

### 3.3 `api.magic`

```text
public interface ISpell {
  public default com.Polarice3.Goety.common.magic.SpellStat defaultStats();
  public abstract int defaultSoulCost();                          // ← 必须实现
  public default int soulCost(LivingEntity, ItemStack);
  public default int SoulCalculation(LivingEntity);
  public abstract int defaultCastDuration();                      // ← 必须实现
  public default int castDuration(LivingEntity, ItemStack);
  public default net.minecraft.sounds.SoundEvent CastingSound(LivingEntity);
  public default net.minecraft.sounds.SoundEvent CastingSound();
  public default float castingVolume();
  public default float castingPitch();
  public abstract int defaultSpellCooldown();                     // ← 必须实现
  public default int spellCooldown();
  public default int spellCooldown(LivingEntity);
  public default boolean hasCustomCooldown(LivingEntity, ItemStack, ItemStack, int);
  public default net.minecraft.client.model.HumanoidModel$ArmPose getPose(LivingEntity, ItemStack, SpellStat);
  public default void startSpell(ServerLevel, LivingEntity, ItemStack, SpellStat);
  public default void useSpell(ServerLevel, LivingEntity, ItemStack, int, SpellStat);
  public default void stopSpell(ServerLevel, LivingEntity, ItemStack, ItemStack, int, SpellStat);
  public default void SpellResult(ServerLevel, LivingEntity, ItemStack, SpellStat);
  public abstract com.Polarice3.Goety.api.magic.SpellType getSpellType();   // ← 必须实现
  public default java.util.List<com.Polarice3.Goety.api.magic.SpellType> getSpellTypes();
  public default boolean conditionsMet(ServerLevel, LivingEntity);
  public default boolean conditionsMet(ServerLevel, LivingEntity, SpellStat);
  public abstract java.util.List<net.minecraft.world.item.enchantment.Enchantment> acceptedEnchantments(); // ← 必须实现
  public default net.minecraft.sounds.SoundEvent loopSound(LivingEntity);
  public default com.Polarice3.Goety.utils.ColorUtil particleColors(LivingEntity);
  public default net.minecraft.core.particles.ParticleOptions getParticle(LivingEntity);
  public default void useParticle(Level, LivingEntity, ItemStack);
  public default LivingEntity getTarget(LivingEntity);
  public default net.minecraft.world.phys.HitResult rayTrace(Level, LivingEntity, int, double);
  public default net.minecraft.world.phys.BlockHitResult blockResult(Level, LivingEntity, double);
  public default net.minecraft.world.phys.EntityHitResult entityResult(Level, LivingEntity, int, double);
  public default boolean ReduceCastTime(LivingEntity);
  public default net.minecraft.world.effect.MobEffectInstance summonDownEffect(LivingEntity);
  public default int SoulCostUp(LivingEntity);
  public default boolean typeStaff(ItemStack, SpellType);
  public default boolean SoulDiscount(LivingEntity);
  public default boolean FrostSoulDiscount(LivingEntity);
  public default boolean WindSoulDiscount(LivingEntity);
  public default boolean GeoSoulDiscount(LivingEntity);
  public default boolean StormSoulDiscount(LivingEntity);
  public default boolean WildSoulDiscount(LivingEntity);
  public default boolean NetherSoulDiscount(LivingEntity);
  public default boolean AbyssSoulDiscount(LivingEntity);
  public default boolean NecroSoulDiscount(LivingEntity);
  public default boolean VoidSoulDiscount(LivingEntity);
}
```

**`ISpell` 的抽象方法只有 5 个**：`defaultSoulCost()`、`defaultCastDuration()`、
`defaultSpellCooldown()`、`getSpellType()`、`acceptedEnchantments()`。

反编译出的若干默认值（写代码时可以直接依赖）：

```java
default SpellStat defaultStats() { return new SpellStat(0, 0, 16, 0.0D, 0, 0.0F); }
default SoundEvent CastingSound() { return SoundEvents.EVOKER_CAST_SPELL; }
default float castingVolume() { return 0.5F; }
default float castingPitch() { return 1.0F; }
default ColorUtil particleColors(LivingEntity caster) { return new ColorUtil(0.2F, 0.2F, 0.2F); }
default ParticleOptions getParticle(LivingEntity caster) { return ParticleTypes.ENTITY_EFFECT; }

@Deprecated default int spellCooldown() { return this.defaultSpellCooldown(); }   // 已废弃，别用

@OnlyIn(Dist.CLIENT)
default HumanoidModel.ArmPose getPose(LivingEntity caster, ItemStack staff, SpellStat spellStat) {
   return SpellPoses.SPELL;
}
```

`SoulCalculation(LivingEntity)` 是**灵魂消耗的全部算法所在**：它读
`SpellConfig.EnvironmentalCost`、`ItemConfig.*RobeDiscount`、生物群系标签
（`ModTags.Biomes.*_DISCOUNT` / `*_MARKUP`）、月光亮度、结构标签
（`ModTags.Structures.NECROMANCER_POWER`）以及 `ModAttributes.getSoulDiscount(caster, this)`。
自己写法术**不需要**重算这些，覆写 `defaultSoulCost()` 就够了。

```text
public interface IChargingSpell extends ISpell {
  public abstract int Cooldown();                                 // ← 新增的唯一抽象方法
  public default int Cooldown(LivingEntity, ItemStack, int);
  public default boolean everCharge();
  public default int defaultCastDuration();
  public default int defaultCastUp();
  public default int castUp(LivingEntity, ItemStack);
  public default int defaultSpellCooldown();
  public default int shotsNumber();
  public default int shotsNumber(LivingEntity, ItemStack);
}

public interface ISummonSpell extends ISpell {
  public static final ColorUtil DEFAULT_SUMMON  = new ColorUtil(9430751);
  public static final ColorUtil VOID_SUMMON     = new ColorUtil(13369594);
  public static final ColorUtil WILD_SUMMON     = new ColorUtil(4209428);
  public static final ColorUtil NETHER_SUMMON   = new ColorUtil(16753408);
  public static final ColorUtil GEO_SUMMON      = new ColorUtil(16763392);
  public static final ColorUtil NORMAL_SUMMON   = ColorUtil.WHITE;
  public static final ColorUtil NAMELESS_SUMMON = new ColorUtil(11009086);
  public abstract int SummonDownDuration();                       // ← 新增抽象方法 1
  public abstract void commonResult(ServerLevel, LivingEntity);   // ← 新增抽象方法 2
  public default boolean hasSummonDown(LivingEntity);
  public default void SummonSap(LivingEntity, LivingEntity);
  public default void SummonDown(LivingEntity);
  public default void setTarget(LivingEntity, net.minecraft.world.entity.Mob);
  public default void uponSummon(ServerLevel, LivingEntity, ItemStack, LivingEntity);
  public default void summonParticles(ServerLevel, LivingEntity, ItemStack, LivingEntity);
  static {};
}

public interface IBlockSpell extends ISpell {          // 无新增抽象方法
  public default int defaultCastDuration();
  public default boolean rightBlock(ServerLevel, LivingEntity, BlockPos);
  public default boolean rightBlock(ServerLevel, LivingEntity, BlockPos, Direction);
  public default boolean rightBlock(ServerLevel, LivingEntity, BlockPos, SpellStat);
  public default boolean rightBlock(ServerLevel, LivingEntity, BlockPos, Direction, SpellStat);
  public default void blockResult(ServerLevel, LivingEntity, ItemStack, BlockPos, Direction);
  public default void blockResult(ServerLevel, LivingEntity, BlockPos, Direction, SpellStat);
  public default void blockResult(ServerLevel, LivingEntity, BlockPos, SpellStat);
  public default void blockResult(ServerLevel, LivingEntity, ItemStack, BlockPos, SpellStat);
  public default void blockResult(ServerLevel, LivingEntity, ItemStack, BlockPos, Direction, SpellStat);
}

public interface IBreathingSpell extends IChargingSpell {   // 无新增抽象方法
  public default int Cooldown();
  public default boolean everCharge();
  public default void useParticle(Level, LivingEntity, ItemStack);
  public default java.util.List<net.minecraft.world.entity.Entity> getBreathTarget(LivingEntity, double);
  public default java.util.List<net.minecraft.world.entity.Entity> getBreathTarget(LivingEntity, double, java.util.function.Predicate<? super net.minecraft.world.entity.Entity>);
  public default void showWandBreath(LivingEntity);
  public default void showWandBreath(LivingEntity, SpellStat);
  public default void showWandBreath(LivingEntity, ItemStack, SpellStat);
  public default void breathAttack(ParticleOptions, LivingEntity, double, double);
  public default void breathAttack(ParticleOptions, LivingEntity, boolean, double, double);
  public default void breathAttack(ParticleOptions, LivingEntity, boolean, int, double, double);
  public default void dragonBreathAttack(ParticleOptions, LivingEntity, double);
  public default void dragonBreathAttack(ParticleOptions, LivingEntity, int, double);
  public default void dragonBreathAttack(ParticleOptions, LivingEntity, int, double, double);
}

public interface ITouchSpell extends ISpell {           // 无新增抽象方法
  public default int defaultCastDuration();
  public default boolean targetConditions(ServerLevel, LivingEntity, LivingEntity, ItemStack, SpellStat);
  public default void touchResult(ServerLevel, LivingEntity, LivingEntity, ItemStack, SpellStat);
}

public interface IMold {
  public default boolean spawnServant(net.minecraft.world.entity.player.Player, ItemStack, Level, BlockPos);
}
```

**枚举 / 可扩展枚举：**

```text
public final class SpellType extends java.lang.Enum<SpellType> implements net.minecraftforge.common.IExtensibleEnum {
  NONE, NECROMANCY, NETHER, ILL, FROST, GEOMANCY, WIND, STORM, ABYSS, WILD, VOID
  public static SpellType create(java.lang.String, java.lang.String);   // (name, baseName)
  public java.lang.String getBaseName();
  public net.minecraft.network.chat.Component getName();
  static {};
}

public final class GolemType extends java.lang.Enum<GolemType> implements net.minecraftforge.common.IExtensibleEnum {
  NONE, WHISPERER, LEAPLEAF, ICE_GOLEM, SQUALL_GOLEM, REDSTONE_GOLEM,
  GRAVE_GOLEM, REDSTONE_MONSTROSITY
  public static GolemType create(java.lang.String, java.util.function.Supplier<BlockState>, IMold);
  public BlockState getBlockState();
  public IMold getMold();
  public static Map<BlockState, IMold> getGolemList();
  static {};
}

public class SpellPoses {
  public static final net.minecraft.client.model.HumanoidModel$ArmPose SPELL;
  public static final net.minecraft.client.model.HumanoidModel$ArmPose FLIGHT_POSE;
  public static final net.minecraft.client.model.HumanoidModel$ArmPose HOLD_STAFF;
}
```

`GolemType.create(...)` 是插件加"新傀儡"的**唯一路口**：给它一个方块状态 + 一个 `IMold`，
之后那个方块状态就能被 Goety 的傀儡系统识别。

### 3.4 `api.client.SpellArmPose` —— 一个真正的空枚举

反编译源码全文（除了 import 就这些）：

```java
package com.Polarice3.Goety.api.client;

import net.minecraftforge.common.IExtensibleEnum;

public enum SpellArmPose implements IExtensibleEnum {
   // $FF: synthetic method
   private static SpellArmPose[] $values() {
      return new SpellArmPose[0];
   }
}
```

**它没有任何枚举常量，也没有 `create` 方法**，`values()` 返回空数组。
这个类目前是个占位符，**不要用它**。要自定义施法姿势请：

- 用 `api.magic.SpellPoses` 的 `SPELL` / `FLIGHT_POSE` / `HOLD_STAFF`，或
- 覆写 `ISpell.getPose(LivingEntity, ItemStack, SpellStat)` 返回 `HumanoidModel.ArmPose`。
  该方法带 `@OnlyIn(Dist.CLIENT)`，只在客户端调用。

### 3.5 `api.entities` —— IOwned 是核心

```text
public interface IOwned {
  public static final java.util.UUID SPEED_MODIFIER_UUID;                 // "9c47949c-b896-4802-8e8a-f08c50791a8a"
  public static final net.minecraft.world.entity.ai.attributes.AttributeModifier SPEED_MODIFIER;
                                                                          // ("Staying speed penalty", -1.0, ADDITION)

  // —— 必须实现的 5 个 ——
  public abstract LivingEntity getTrueOwner();
  public abstract java.util.UUID getOwnerId();
  public abstract void setOwnerId(java.util.UUID);
  public abstract void setHostile(boolean);
  public abstract boolean isHostile();

  // —— 其余全部有默认实现 ——
  public default void setOwnerClientId(int);
  public default void setTrueOwner(LivingEntity);
  public default void copyTrueOwner(IOwned);
  public default void copyTrueOwner(com.Polarice3.Goety.api.blocks.entities.IOwnedBlock);
  public default void removeTrueOwner();
  public default int getOwnerClientId();
  public default LivingEntity getMasterOwner();
  public default EntityType<?> getVariant(Player, Level, BlockPos);
  public default EntityType<?> getVariant(Level, BlockPos);
  public default void convertNewEquipment(Entity);
  public default void setLimitedLife(int);   public default void setHasLifespan(boolean);
  public default boolean hasLifespan();      public default void setLifespan(int);
  public default int getLifespan();          public default boolean isLimitedLife();
  public default void setNatural(boolean);   public default boolean isNatural();
  public default boolean isChargingCrossbow();
  public default void setFamiliar();         public default boolean isFamiliar();
  public default boolean canBeFamiliar();
  public default void onCeaseFire(net.minecraft.server.level.ServerPlayer);
  public default void onStopAttack();        public default void checkHostility();
  public default void ownedTick();           public default void ownerCheck();
  public default void targetingTick();       public default void mobSense();
  public default void lifeSpanDamage();      public default void dismiss();
  public default void teleportTowards(Entity);      public default void teleportTowards(Entity, double);
  public default boolean ownedTeleport(Vec3);       public default boolean ownedTeleport(double, double, double);
  public default void teleportHits();
  public default boolean preventsSleep(Player);
  public default java.util.function.Predicate<Entity> summonPredicate();
  public default int getSummonLimit(LivingEntity);
  public default void uncreditedKill(LivingEntity);
  public default boolean canRevive(DamageSource);
  public default int getRevivingTime();      public default void setRevivingTime(int);
  public default boolean isReviving();       public default void startRevival();
  public default void reviveOwned();         public default void reviveTick();
  public default BlockPos getRevivePos();    public default Vec3 vec3RevivePos();
  public default void setRevivePos(BlockPos);
  public default ResourceKey<Level> getReviveLevel();
  public default String getReviveDim();
  public default void setReviveDim(ResourceKey<Level>);   public default void setReviveDim(String);
  public default void pacifySurroundingMobs(double);
  public default boolean hasSummons();       public default int getHasSummonCheck();
  public default void setHasSummonCheck(int);
  public default boolean isGrudgedTowardsType(EntityType<?>);   public default boolean isGrudgedTowards(LivingEntity);
  public default boolean isAllyWithType(EntityType<?>);         public default boolean isAllyWith(LivingEntity);
  public default void readOwnedData(CompoundTag);               public default void saveOwnedData(CompoundTag);
  static {};
}
```

**`IOwned` 的 5 个抽象方法就是"归属系统"的全部硬性要求**。反编译出的关键默认实现：

```java
default void setTrueOwner(@Nullable LivingEntity livingEntity) {
   if (livingEntity != null) {                 // （源码里 set 分支的具体写法）
      this.setOwnerId(livingEntity.getUUID());
      this.setOwnerClientId(livingEntity.getId());
   }
   // ...
}

default void removeTrueOwner() {
   this.setOwnerId((UUID) null);
   this.setOwnerClientId(-1);
}

default void copyTrueOwner(IOwned owned) {
   if (owned.getOwnerId() != null) {
      this.setOwnerId(owned.getOwnerId());
      this.setOwnerClientId(owned.getOwnerClientId());
   }
}
```

也就是说 **"设主人"= 把 UUID 和实体 id 写进同步数据**，另外两步是配套的；不要只调
`setOwnerId` 而不调 `setOwnerClientId`（客户端会认不出主人）。

其余接口（都**没有**抽象方法，除非注明）：

```text
public interface IGolem   { canAnimateMove(); getAttackReachSqr(LivingEntity); targetClose(LivingEntity, double); }
public interface IHiding  { isHiding(); }
public interface ISpellEntity { }                                   // 空接口
public interface IBreathing { isBreathing(); setBreathing(boolean); doBreathing(Entity); }   // 全是抽象
public interface ICharger  { isCharging(); setCharging(boolean); }                            // 全是抽象
public interface IAutoRideable { setAutonomous(boolean); isAutonomous(); }                    // 全是抽象
public interface ICustomAttributes { void setConfigurableAttributes(); }                      // 抽象
public interface IHungry   { ModFoodData getFoodData();             // 抽象
                             checkMovementStatistics(double,double,double);
                             causeFoodExhaustion(float); }
```

```text
public interface IHeretic {
  public default boolean isChanting();
  public default int getChantTimes();          public default void setChantTimes(int);
  public default void setCasting(boolean);     public default boolean isCasting();
  public default void setMonolith(com.Polarice3.Goety.common.entities.neutral.AbstractObsidianMonolith);
  public default com.Polarice3.Goety.common.entities.neutral.AbstractObsidianMonolith getMonolith();
  public default float getCast(float);
  public default java.util.List<net.minecraft.world.phys.Vec3> getConvokePos();
}

public interface IRM {
  public abstract int getDeathTime();      public abstract float getBigGlow();
  public abstract float getMinorGlow();    public abstract boolean isActivating();
  public abstract boolean isSummoning();   public abstract boolean isBelching();
  public abstract boolean canAnimateMove();public abstract boolean isHostile();
}

public interface ITrainable {          // 全部 default，无抽象方法
  getTotalTrainTime(); getTrainTime(); setTrainTime(int); getTrainCheck(); setTrainCheck(int);
  canTrain(Level, BlockPos, EntityType<? extends Mob>); isTraining();
  Optional<BlockPos> getTrainPos(); Optional<BlockPos> getStoredTrainPos(); Vec3 vec3TrainPos();
  setTrainPos(BlockPos); setStoredTrainPos(BlockPos);
  getCurrentTrain(); setCurrentTrain(String); trainTick();
  trainSpeed(EntityType<? extends Mob>); train(EntityType<? extends Mob>); isTrained();
  completeTraining(EntityType<? extends Mob>); readTrainableData(CompoundTag); saveTrainableData(CompoundTag);
}

public interface IMobCrafter {         // 全部 default
  getFurnacePos/setFurnacePos/getCraftTablePos/setCraftTablePos/setUsingFurnace/isUsingFurnace/
  isFurnace(BlockState)/setCrafting/isCrafting/isCraftTable(BlockState)/isFurnaceActuallyCooking/
  setFurnaceLit/getNearbyCraftTableUsers(Level, AABB, BlockPos)/readCrafterData/saveCrafterData
}

public interface IChunkLoader {        // 全部 default
  selfLoadRadius(); getTicketTime(); setTicketTime(long); decreaseTicketTime(); shouldChunkLoad();
  chunkLoad(); forceChunkLoadSelf(); chunkLoadTarget(BlockPos); saveDataCheck(); saveDataChecked();
  chunkLoadBlock();
}
```

### 3.6 `api.entities.ally.*`

```text
public interface IServant extends IOwned, IChunkLoader {
  public static final int GUARDING_RANGE;
  // —— 必须实现的 7 个 ——
  public abstract boolean isWandering();      public abstract void setWandering(boolean);
  public abstract boolean isStaying();        public abstract void setStaying(boolean);
  public abstract boolean canUpdateMove();
  public abstract boolean isCommanded();
  public abstract void setCommandPosEntity(LivingEntity);
  public abstract void tryKill(net.minecraft.world.entity.player.Player);
  // 其余 60+ 个全是 default，包括：
  public default boolean canWander();  public default boolean canStay();
  public default boolean isGuardingArea(); public default boolean canGuardArea();
  public default BlockPos getBoundPos();   public default Vec3 vec3BoundPos();
  public default void setBoundPos(BlockPos);
  public default ResourceKey<Level> getBoundLevel();  public default String getBoundDim();
  public default void setBoundDim(ResourceKey<Level>); public default void setBoundDim(String);
  public default boolean isWithinGuard(BlockPos);
  public default LivingEntity getPriorityTarget();  public default void setPriorityTarget(LivingEntity);
  public default boolean isPrioritizing(); public default int getPriorityTime(); public default void setPriorityTime(int);
  public default BlockPos getPriorityPos(); public default void setPriorityPos(BlockPos);
  public default void overrideSetTarget(LivingEntity);
  public default void setWandering(); public default void setStaying(); public default void setGuarding();
  public default void setFollowing(); public default boolean isFollowing(); public default boolean canFollow();
  public default void copyStance(IServant); public default void spawnUpgraded();
  public default void updateMoveMode(Player); public default boolean canBeCommanded();
  public default boolean canCommandToBlock(Level, BlockPos);
  public default void setCommandPos(BlockPos); public default BlockPos getCommandPos();
  public default void setCommandPos(BlockPos, boolean);
  public default void setCommandPosEntityOrder(LivingEntity);
  public default LivingEntity getCommandPosEntity();
  public default int getCommandTick(); public default void setCommandTick(int);
  public default int getNoHealTime();  public default void setNoHealTime(int);
  public default int getKillChance();  public default void setKillChance(int);
  public default boolean isUpgraded(); public default void setUpgraded(boolean);
  public default boolean servantSunBurn(); public default boolean burnSunTick();
  public default boolean isAbleToRide(LivingEntity); public default boolean canRide(LivingEntity);
  public default boolean canBeRidden(LivingEntity);
  public default void servantTick(); public default void ownedByServantTick();
  public default void healServant(); public default void burnServant(LivingEntity);
  public default double getCommandSpeed(); public default void commandMode();
  public default void stayingMode(); public default void stayingPosition();
  public default boolean canWearArmor(); public default boolean canWearArmorSlot(EquipmentSlot);
  public default boolean canHaveWeapon(); public default boolean canHaveEquipment();
  public default void readServantData(CompoundTag); public default void saveServantData(CompoundTag);
  static {};
}
```

反编译确认：`int GUARDING_RANGE = MobsConfig.ServantGuardingRange.get();` —— 同样是**运行期配置值**，
不能当编译期常量。它的用途是"召唤物活动的最大半径"（源码里与 `getBoundPos().distSqr(...)` 比较）。

```text
public interface IAquaServant extends IServant {   // 无新增抽象方法
  public default boolean wantsToSwim();
  public default boolean closeToNextPos();
  public default boolean isSearchingForLand();
  public default void setSearchingForLand(boolean);
}
```

### 3.7 `api.entities.ally.illager.*`

```text
public final class IllagerType extends Enum<IllagerType> implements IExtensibleEnum {
  public static final IllagerType NONE;
  public static final IllagerType NORMAL;
  public static IllagerType create(java.lang.String, ITrainIllager);
  public ITrainIllager getIllager();
  public static ITrainIllager getIllagerFromType(Level, BlockPos, int, EntityType<? extends Mob>);
  public static java.util.List<IllagerType> getIllagerList();
  static {};
}

public interface ILooter extends net.minecraft.world.entity.npc.InventoryCarrier {
  public default java.util.List<ItemStack> itemsInInv(java.util.function.Predicate<ItemStack>);
  public default int getItemAmount(java.util.function.Predicate<ItemStack>);
  public default boolean hasItem(java.util.function.Predicate<ItemStack>);
  public default BlockPos getChestPos();  public default void setChestPos(BlockPos);
  public default ResourceKey<Level> getChestLevel(); public default String getChestDim();
  public default void setChestDim(String); public default void setChestDim(ResourceKey<Level>);
  public default BlockPos getDumpChestPos(); public default void setDumpChestPos(BlockPos);
  public default ResourceKey<Level> getDumpChestLevel(); public default String getDumpChestDim();
  public default void setDumpChestDim(String); public default void setDumpChestDim(ResourceKey<Level>);
  public default void readLooterData(CompoundTag); public default void saveLooterData(CompoundTag);
}

public interface ITrainIllager {
  public default boolean canSpawn(Level, BlockPos, int);
  public default boolean mobCanTrainTo(Mob, Level, BlockPos, int);
  public abstract EntityType<? extends Mob> getIllager(Level, BlockPos, int);   // ← 唯一抽象方法
}
```

`IllagerType.create(String, ITrainIllager)` 是插件注册"可被兵营训练出来的新灾厄村民"的入口。
`ILooter` 的唯一抽象方法来自父接口 `InventoryCarrier`（`getContainer()`），
`ILooter` 自己**没有**新增抽象方法。

### 3.8 `api.blocks` 与 `api.blocks.entities`

```text
public interface IEnchanteableBlock extends EntityBlock, net.minecraftforge.common.extensions.IForgeBlock {
  public default void setEnchantments(ItemStack, BlockEntity);
  public default ItemStack getCloneItemStack(BlockState, net.minecraft.world.phys.HitResult,
                                             BlockGetter, BlockPos, Player);
}
// 抽象方法来自 EntityBlock#newBlockEntity —— 实现这个接口的方块必须是 BlockEntity 方块

public interface IEnchantedBlock {           // 给方块实体用
  public abstract it.unimi.dsi.fastutil.objects.Object2IntMap<Enchantment> getEnchantments();  // ← 抽象
  public default void loadEnchants(CompoundTag);
  public default void saveEnchants(CompoundTag, net.minecraft.world.item.Item);
}

public interface ISeat {
  public default InteractionResult sitDown(BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult);
  public static boolean isSeatOccupied(Level, BlockPos);
  public static java.util.Optional<Entity> getLeashed(Level, Player);
  public static boolean canBePickedUp(Entity);
  public default Vec3 seatOffset(BlockPos);
  public default boolean hasLookAngle();
  public default float seatLookAngle(Level, BlockPos);
  public default boolean isThrone(Level, BlockPos);
  public default LivingEntity getOwner(Level, BlockPos);
  public default void placeSeat(Level, BlockPos, Entity);
}

public interface IOwnedBlock {
  public default boolean screenView();
  public default java.util.UUID getOwnerUUID();   public default void setOwnerUUID(java.util.UUID);
  public default int getOwnerId();                public default void setOwnerId(int);
  public default LivingEntity getTrueOwner();
  public abstract Player getPlayer();             // ← 唯一抽象方法
  public default CompoundTag getAttitudeLists();
  public default boolean isGrudgedTowards(LivingEntity);
  public default boolean isAllyWith(LivingEntity);
}

public interface IBarrack {
  public abstract EntityType<? extends Mob> getTrainedMob(Level, BlockPos);   // ← 抽象
  public default String getCurrentMob();     public default int getRange();
  public default int trainLimit();
  public default java.util.function.Predicate<Mob> trainingRequirements(Level, BlockPos);
  public default java.util.List<Mob> getMobsInRange(Level, BlockPos);
  public default boolean checkEligibility(Mob, Level, BlockPos);
  public default java.util.List<Mob> getTrainableList(Level, BlockPos);
  public default void addTrainable(Mob, Level, BlockPos);  public default void addTrainable(Mob);
  public default int getCurrentAmount();     public default int amountTraining(Level, BlockPos);
  public default boolean capacityAvailable(Level, BlockPos);
  public default void trainMobs(Level, BlockPos); public default void trainMob(Mob, Level, BlockPos);
}

public interface ISoulCandle {               // 全 default，无抽象方法
  public default void drainSouls(BlockPos);  public default void drainSouls(int, BlockPos);
  public default int getSouls();             public default int soulDrainAmount();
  public default boolean checkCage();
}

public interface ITrainingBlock extends IOwnedBlock {
  public abstract int getTrainingTime();     public abstract int getMaxTrainTime();
  public abstract int amountTrainLeft();     public abstract int maxTrainAmount();
  public abstract EntityType<?> getTrainMob();
  public default boolean isSensorSensitive(); public default boolean isGuarding();
  public default boolean isGrounding();       public default boolean reachedLimit();
}

public interface IWaystoneBlock {            // 全 default
  public default Direction getDirection();   public default net.minecraft.core.GlobalPos getPosition();
  public default int getSoulCost();          public default boolean isShowBlock();
  public default void setShowBlock(boolean);
}

public interface IWindPowered {
  public abstract int activeTicks();                     // ← 抽象
  public abstract void activate(int);                    // ← 抽象
  public default int windPower();   public default void setWindPower(int);
}
```

### 3.9 `api.ritual`

```text
public interface IRitualType {
  public abstract java.lang.String getName();          // ← 唯一抽象方法
  public default ItemStack getJeiIcon();
  public default boolean getRequirement(com.Polarice3.Goety.common.blocks.entities.RitualBlockEntity,
                                        BlockPos, Level);
  public default boolean getRequirement(RitualBlockEntity, Player, BlockPos, Level);
  public default void onPerformRitual(Level, BlockPos, DarkAltarBlockEntity, Player, ItemStack);
  public default void sendFinishRay(Level, BlockPos, DarkAltarBlockEntity, Player, ItemStack);
  public default void onFinishRitual(Level, BlockPos, DarkAltarBlockEntity, Player, ItemStack);
}

public class RitualType {
  public static Map<String, IRitualType> RITUAL_TYPE_LIST;
  public static IRitualType ANIMATION, NECROTURGY, FORGE, MAGIC, ADEPT_NETHER, EXPERT_NETHER,
                             SABBATH, END, SKY, STORM, GEOTURGY, FROST, DEEP, OVERGROWN, DIVINATION;
  public static void addRitualType(String, IRitualType);       // ← 插件注册仪式用这个
  public static Map<String, IRitualType> getRitualTypeList();
  public static java.util.List<IRitualType> getAllRitualType();
  public static IRitualType getRitualType(String);
  static {};
}
```

### 3.10 API 代码片段（实现 `IBarrack` + `IOwnedBlock` 的最小骨架）

```java
package com.croety.block.entity;

import com.Polarice3.Goety.api.blocks.entities.IBarrack;
import com.Polarice3.Goety.common.blocks.entities.BarracksBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 复用 Goety 的 BarracksBlockEntity（它已经实现了 IOwnedBlock 的全部方法）。 */
public class CroetyBarracks extends BarracksBlockEntity implements IBarrack
{
    public CroetyBarracks(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.CROETY_BARRACKS.get(), pos, state);   // (BlockEntityType<?>, BlockPos, BlockState)
    }

    /** IBarrack 唯一必须实现的方法。 */
    @Override
    public EntityType<? extends Mob> getTrainedMob(Level level, BlockPos pos)
    {
        return EntityType.VINDICATOR;
    }
}
```

---

## 4. 物品写法

### 4.1 Goety 自己用的物品基类

```text
public class com.Polarice3.Goety.common.items.ItemBase extends net.minecraft.world.item.Item {
  public ItemBase();
  public void appendHoverText(ItemStack, Level, java.util.List<Component>, TooltipFlag);
}
```
就是 `Item` + 一个 tooltip 钩子。**没有**别的魔法。绝大多数 Goety 物品直接继承 `net.minecraft.world.item.Item`。

```text
public class com.Polarice3.Goety.common.items.magic.MagicFocus
        extends net.minecraft.world.item.Item implements com.Polarice3.Goety.api.items.magic.IFocus {
  public com.Polarice3.Goety.api.magic.ISpell spell;      // ← public 字段
  public int soulCost;                                    // ← public 字段
  public MagicFocus(com.Polarice3.Goety.api.magic.ISpell);   // ← 唯一构造函数
  public boolean isEnchantable(ItemStack);
  public int getEnchantmentValue(ItemStack);
  public boolean canApplyAtEnchantingTable(ItemStack, Enchantment);
  public ISpell getSpell();
  public void appendHoverText(ItemStack, Level, java.util.List<Component>, TooltipFlag);
  public void addInformationAfterShift(java.util.List<Component>);
}
```

反编译出的完整构造与附魔逻辑：

```java
public MagicFocus(ISpell spell) {
   super(new Item.Properties().rarity(Rarity.UNCOMMON).setNoRepair().stacksTo(1));
   this.spell = spell;
   this.soulCost = spell.defaultSoulCost();      // ← 构造时就缓存了消耗
}

public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
   if (stack.getItem() instanceof MagicFocus magicFocus) {
      if (magicFocus.getSpell() != null && !magicFocus.getSpell().acceptedEnchantments().isEmpty()) {
         return magicFocus.getSpell().acceptedEnchantments().contains(enchantment);
      }
   }
   return false;
}
```

> **两个直接结论**：
> 1. **构造 focus 时只能给 `ISpell`**，`Item.Properties` 是写死的
>    （`UNCOMMON` 稀有度、不可修复、单堆叠）。想换属性只能自己写一个 `IFocus` 实现。
> 2. `acceptedEnchantments()` 返回空 List 时，**focus 在附魔台上完全不可附魔**。
>    想支持附魔就照 `VexSpell` 那样返回 `ModEnchantments.POTENCY` 等。

```text
public class com.Polarice3.Goety.common.items.magic.DarkWand
        extends net.minecraft.world.item.Item implements com.Polarice3.Goety.api.items.magic.IWand {
  public com.Polarice3.Goety.api.magic.SpellType spellType;
  public java.util.List<com.Polarice3.Goety.api.magic.SpellType> list;
  public DarkWand(net.minecraft.world.item.Item$Properties, SpellType...);
  public DarkWand(net.minecraft.world.item.Item$Properties, SpellType);
  public DarkWand(SpellType);
  public DarkWand();
  public static Item$Properties wandProperties();
  public int SoulCost(ItemStack);   public int CastDuration(ItemStack);   public int Cooldown(ItemStack);
  public int ShotsFired(ItemStack); public void increaseShots(ItemStack); public void setShots(ItemStack, int);
  public void setSpellConditions(ISpell, ItemStack, LivingEntity);
  public void MagicResults(ItemStack, Level, LivingEntity);
  public void MagicResults(ItemStack, Level, LivingEntity, ISpell);
  public boolean canCastTouch(ItemStack, Level, LivingEntity, LivingEntity);
  public SoundEvent CastingSound(ItemStack, LivingEntity);
  public float castingVolume(ItemStack); public float castingPitch(ItemStack);
  public void initializeClient(java.util.function.Consumer<IClientItemExtensions>);
  // + 完整的 Item 覆写：use / onUseTick / releaseUsing / getUseDuration / getUseAnimation /
  //   finishUsingItem / inventoryTick / onCraftedBy / onLeftClickEntity / interactLivingEntity / useOn ...
}

public class com.Polarice3.Goety.common.items.magic.DarkStaff extends DarkWand {
  public DarkStaff(net.minecraft.world.item.Item$Properties, double, double, SpellType);
  public DarkStaff(net.minecraft.world.item.Item$Properties, double, SpellType);
  public DarkStaff(double, double, SpellType);
  public DarkStaff(double, SpellType);
}
```
`DarkStaff` 比 `DarkWand` 多的两个 `double` 参数的具体含义 **未验证**（需要读 `DarkStaff` 的 body）。
**做自定义法杖最省事的做法是继承 `DarkWand`/`DarkStaff`，而不是从头实现 `IWand`** ——
因为 `IWand.getFocus` 依赖 `SoulUsingItemHandler` capability，那个 capability 是 `DarkWand` 提供的。

### 4.2 灵魂在物品里怎么存

- **物品容器**（灵魂罐、灵魂图腾、SoulItem）：用 `api.items.magic.ISoulContainer` 的 4 个静态方法，
  NBT 键是字面量 `"Souls"`；**你的物品必须 implements ISoulContainer**（见 3.1 的守卫）：

```java
import com.Polarice3.Goety.api.items.magic.ISoulContainer;
import net.minecraft.world.item.ItemStack;

public final class SoulUtil
{
    public static int  read(ItemStack stack)              { return ISoulContainer.currentSouls(stack); }
    public static boolean empty(ItemStack stack)          { return ISoulContainer.isEmpty(stack); }
    public static void write(ItemStack stack, int amount) { ISoulContainer.setSoulsAmount(stack, amount); }
    public static void spend(ItemStack stack, int amount) { ISoulContainer.decreaseSouls(stack, amount); }
}
```

- **图腾**（`ITotem`）额外用键 `"Max Souls"`，并且只有 `getMaxSouls()` 一个抽象方法。
  Goety 自带的实现是 `com.Polarice3.Goety.common.items.magic.TotemOfSouls`：

```text
public class TotemOfSouls extends Item implements ITotem, ICurioItem {
  public int maxSouls;
  public TotemOfSouls(int);                 // ← 构造参数就是容量
  public int getMaxSouls();
  public ItemStack getEmptyTotem();         // 空/满两个变体，供创造页签展示
  public ItemStack getFilledTotem();
  public int getBarColor(ItemStack);
  public double amountColor(ItemStack);
  public boolean hasCraftingRemainingItem(ItemStack);
  public ItemStack getCraftingRemainingItem(ItemStack);
  public void onCraftedBy(ItemStack, Level, Player);
  public void inventoryTick(ItemStack, Level, Entity, int, boolean);
  public static boolean isActivated(ItemStack);
  public boolean isBarVisible(ItemStack);
  public int getBarWidth(ItemStack);
  public InteractionResult useOn(UseOnContext);
  public boolean canEquipFromUse(SlotContext, ItemStack);
  public void appendHoverText(ItemStack, Level, List<Component>, TooltipFlag);
}
```

```java
import com.Polarice3.Goety.common.items.magic.TotemOfSouls;

public class CroetyTotem extends TotemOfSouls
{
    public CroetyTotem() { super(1024); }     // maxSouls = 1024
}
```

- **玩家灵魂能量**：走 capability，接口是
  `com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy`：

```text
public interface ISoulEnergy {
  public abstract BlockPos getArcaBlock();       public abstract void setArcaBlock(BlockPos);
  public abstract ResourceKey<Level> getArcaBlockDimension();
  public abstract void setArcaBlockDimension(ResourceKey<Level>);
  public abstract boolean getSEActive();         public abstract void setSEActive(boolean);
  public abstract int getSoulEnergy();           public abstract void setSoulEnergy(int);
  public abstract boolean increaseSE(int);       public abstract boolean decreaseSE(int);
  public abstract int getRecoil();               public abstract void setRecoil(int);
  public abstract boolean apostleWarned();       public abstract void setApostleWarned(boolean);
  public abstract int getRestPeriod();           public abstract void setRestPeriod(int);
  public abstract boolean increaseRestPeriod(int); public abstract boolean decreaseRestPeriod(int);
  public abstract java.util.Set<java.util.UUID> grudgeList();
  public abstract void addGrudge(java.util.UUID);  public abstract void removeGrudge(java.util.UUID);
  public abstract java.util.List<EntityType<?>> grudgeTypeList();
  public abstract void addGrudgeType(EntityType<?>); public abstract void removeGrudgeType(EntityType<?>);
  public abstract java.util.Set<java.util.UUID> allyList();
  public abstract void addAlly(java.util.UUID);    public abstract void removeAlly(java.util.UUID);
  public abstract java.util.List<EntityType<?>> allyTypeList();
  public abstract void addAllyType(EntityType<?>); public abstract void removeAllyType(EntityType<?>);
  public abstract java.util.Set<java.util.UUID> groundedList();
  public abstract void addGrounded(java.util.UUID);public abstract void removeGrounded(java.util.UUID);
  public abstract java.util.List<EntityType<?>> groundedTypeList();
  public abstract void addGroundedType(EntityType<?>); public abstract void removeGroundedType(EntityType<?>);
  public abstract java.util.List<com.Polarice3.Goety.common.research.Research> getResearch();
  public abstract void addResearch(com.Polarice3.Goety.common.research.Research);
  public abstract void removeResearch(com.Polarice3.Goety.common.research.Research);
}
```

**不要直接抓 capability**，用工具类 `com.Polarice3.Goety.utils.SEHelper`：

```text
public static ISoulEnergy getCapability(Player);
public static boolean getSEActive(Player);       public static void setSEActive(Player, boolean);
public static int  getSESouls(Player);           public static void setSESouls(Player, int);
public static void setSoulsAmount(Player, int);
public static boolean decreaseSESouls(Player, int);
public static boolean increaseSESouls(Player, int);
public static boolean getSoulsContainer(Player);
public static boolean getSoulsAmount(Player, int);
public static int  getSoulAmountInt(Player);
public static int  getSoulGiven(LivingEntity);
public static void increaseSouls(Player, int);   public static void decreaseSouls(Player, int);
public static float soulDiscount(LivingEntity);
public static int  SoulMultiply(LivingEntity, DamageSource);
public static int  getRecoil(Player);            public static void setRecoil(Player, int);
public static int  getRestPeriod(Player);        public static void setRestPeriod(Player, int);
public static boolean isGrudged(LivingEntity, LivingEntity);
public static boolean isGrudged(Player, LivingEntity);
public static boolean addGrudgeEntity(Player, LivingEntity);
public static boolean addAllyEntity(Player, LivingEntity);
public static boolean getSoulsContainer(Player);
```

`ISpell.isOnCooldown` 走的就是 `SEHelper.isOnCooldown(player, focusStack)`（内部委托给
`FocusCooldown`，见 5.5）。

capability 的注册入口是 `com.Polarice3.Goety.init.InitEvents` 的
`registerCapabilities(RegisterCapabilitiesEvent)` 与
`attachCapabilities(AttachCapabilitiesEvent<Entity>)`（**不要**自己再注册一遍）。

```java
import com.Polarice3.Goety.utils.SEHelper;
import net.minecraft.world.entity.player.Player;

public static void reward(Player player, int amount)
{
    SEHelper.increaseSESouls(player, amount);   // 返回 boolean，表示是否成功
}
```

### 4.3 饰品（Curios）怎么写

Goety 的饰品**直接在物品类上实现 `ICurioItem`**，不使用 `CuriosApi.registerCurio`
（虽然那个方法存在）。例子：

```text
public class com.Polarice3.Goety.common.items.curios.SingleStackItem
        extends net.minecraft.world.item.Item
        implements top.theillusivec4.curios.api.type.capability.ICurioItem {
  public SingleStackItem();
  public SingleStackItem(net.minecraft.world.item.Item$Properties);
  public boolean hasCraftingRemainingItem(ItemStack);
  public ItemStack getCraftingRemainingItem(ItemStack);
  public boolean isEnchantable(ItemStack);
  public int getEnchantmentValue();
  public boolean canApplyAtEnchantingTable(ItemStack, Enchantment);
  public boolean canEquipFromUse(top.theillusivec4.curios.api.SlotContext, ItemStack);
  public void appendHoverText(ItemStack, Level, java.util.List<Component>, TooltipFlag);
}

public class TotemOfSouls extends Item implements ITotem, ICurioItem { ... }
public class FocusBag    extends Item implements ICurioItem { ... }
```

**收藏栏位靠 JSON 标签决定，不是在代码里**：Goety 在 `data/curios/tags/items/` 下写了 8 个文件
（`charm` / `ring` / `rings` / `hands` / `head` / `body` / `back` / `belt` / `necklace`），
把物品 id 列进去。例如 `data/curios/tags/items/ring.json`：

```json
{
  "replace": false,
  "values": [
    "goety:ring_of_want",
    "goety:ring_of_thirst",
    "goety:ring_of_force",
    "goety:ring_of_wrecking",
    "goety:ring_of_the_forge",
    "goety:ring_of_the_dragon"
  ]
}
```

Goety 还给自己的 `haunted_armor_stand` 注册了 curios 实体栏
（`data/curios/entities/haunted_armor_stand.json`）：

```json
{
  "entities": ["goety:haunted_armor_stand"],
  "slots": ["curios"]
}
```

插件想加自定义饰品，就是三件事：

1. 写一个 `class MyCharm extends Item implements ICurioItem`；
2. 用 `DeferredRegister<Item>` 注册；
3. 在**自己的** mod 里加 `data/<yourmod>/curios/tags/items/charm.json`，
   内容 `"values": ["yourmod:my_charm"]`。

`ICurioItem` 本身的方法**全是 `default`**（`curioTick`、`onEquip`、`onUnequip`、
`canEquip`、`canUnequip`、`getAttributeModifiers(SlotContext, UUID, ItemStack)` …），
所以**不实现任何方法也能编译**。要"用一下"饰品，配合 `api.items.curios.IActivatable#activate`。

### 4.4 找饰品 / 找法杖 / 找 focus 的工具

```text
com.Polarice3.Goety.utils.CuriosFinder:
  public static ItemStack findCurio(LivingEntity, java.util.function.Predicate<ItemStack>);
  public static boolean   hasCurio(LivingEntity, java.util.function.Predicate<ItemStack>);
  public static boolean   hasCurio(LivingEntity, net.minecraft.world.item.Item);
  public static ItemStack findCurio(LivingEntity, net.minecraft.world.item.Item);
  public static ItemStack findCurioInAll(Player, net.minecraft.world.item.Item);
  // 还有一堆套装判定：hasWanting / hasCastTimeReduce / hasMagicHat / hasDarkRobe /
  // hasWildRobe / hasWildCrown / hasWildSet / hasGeoRobe / hasVoidRobe / hasVoidCrown /
  // hasFrostRobes / hasFrostCrown / hasWindRobes / hasWindCrown / hasStormRobes / hasStormCrown /
  // hasAbyssRobes / hasAbyssCrown / hasNetherRobe / hasNetherCrown / hasUndeadCrown /
  // hasUndeadCape / hasAmethystNecklace ...

com.Polarice3.Goety.utils.WandUtil:
  public static ItemStack findWandOnHand(LivingEntity, InteractionHand);
  public static ItemStack findWand(LivingEntity);
  public static ItemStack findFocusOnHand(LivingEntity, InteractionHand);
  public static ItemStack findFocus(LivingEntity);
  public static ItemStack findFocusInInv(Player);
  public static ISpell     getSpellOnHand(LivingEntity, InteractionHand);
  public static ISpell     getSpell(LivingEntity);
  public static int        getShots(LivingEntity);
  public static boolean    hasFocusInInv(Player);
  public static boolean    enchantedFocus(LivingEntity);
  public static int getLevels(Enchantment, LivingEntity);
  public static int getPotencyLevel(LivingEntity); public static int getRangeLevel(LivingEntity);
  public static SpellStat getStats(LivingEntity, ISpell);
  // + 大量特效工具：chainLightning / spawnFangs / spawnSpikes / spawnIceBouquet /
  //   summonMonolith / summonTurret / summon*Trap ...

com.Polarice3.Goety.utils.TotemFinder:
  public static ItemStack findBag(Player);
  public static ItemStack findFocusInBag(Player);
  public static int  getFocusBagTotal(Player);
  public static boolean hasEmptyBagSpace(Player);
  public static boolean hasFocusInBag(Player);
  public static boolean canOpenWandCircle(Player);
  public static ItemStack FindTotem(Player);      // 注意首字母大写
```

### 4.5 物品代码片段：一个自定义 focus

```java
package com.croety.item;

import com.Polarice3.Goety.common.items.magic.MagicFocus;
import com.croety.spell.CroetyBoltSpell;

/** MagicFocus 只有一个构造函数 (ISpell)，所以这样写。 */
public class CroetyBoltFocus extends MagicFocus
{
    public CroetyBoltFocus()
    {
        super(new CroetyBoltSpell());
    }
}
```

注册（放在你自己的 `ModItems` 里，形状照抄 Goety 的 `ModItems`）：

```java
public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "croety");

public static final RegistryObject<Item> CROETY_BOLT_FOCUS =
        ITEMS.register("croety_bolt_focus", () -> new CroetyBoltFocus());

// 在 @Mod 构造函数里：
ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
```

---

## 5. 法术写法

### 5.1 法术是怎么"注册"的 —— 没有法术注册表

用 jar 全部 `.class` 条目 + 全包 javap 确认过：

- `com.Polarice3.Goety.common.magic` 下**只有**：`BlockSpell`、`BreathingSpell`、
  `ChargingSpell`、`EverChargeSpell`、`Spell`、`SpellStat`、`SummonSpell`、`TouchSpell`、`Tremor`。
- 整个 Goety jar（**3125 个 class 条目**）里**没有任何** `SpellRegistry` / `ModSpells` 之类的类
  （对全类清单 grep `SpellRegistry|ModSpells|SpellInit` 命中 0）。
- **法术对象就是 focus 物品的一个字段**：`MagicFocus` 持有 `public ISpell spell`。

反编译 `ModItems` 可以直接看到真实的注册写法：

```java
public static final RegistryObject<Item> SOUL_BOLT_FOCUS =
        ITEMS.register("soul_bolt_focus", () -> new MagicFocus(new SoulBoltSpell()));
public static final RegistryObject<Item> VEXING_FOCUS =
        ITEMS.register("vexing_focus", () -> new MagicFocus(new VexSpell()));
public static final RegistryObject<Item> TEETH_FOCUS =
        ITEMS.register("teeth_focus", () -> new MagicFocus(new TeethSpell()));
```

而"是不是 focus"的判定是 `ModItems.isFocus(Item)`，实现即 `item instanceof MagicFocus`。

> **结论：注册一个法术 = 注册一个 `MagicFocus` 物品。没有别的注册表要碰。**
> 注意 `isFocus` 用的是 `instanceof MagicFocus` 而不是 `instanceof IFocus`，
> 所以**直接实现 `IFocus` 而不继承 `MagicFocus` 的物品，不会进 Goety 的 focus 页签**
> （能不能被法杖用则取决于 `IWand.getFocus` 那条 capability 路径，见 3.1）。

法术的冷却不进注册表，走玩家 capability `FocusCooldown`（见 5.5）。

Goety 法术类的实际位置（`com.Polarice3.Goety.common.magic.spells` 及其子包，共 125 个类）：

| 子包 | 数量 | 例子 |
|---|---|---|
| `spells`（顶层） | 20 | `SoulBoltSpell`、`TeethSpell`、`VexSpell`、`IgniteSpell`、`SpikeSpell`、`SwordSpell`、`CorruptedBeamSpell`、`SonicBoomSpell`、`ArrowRainSpell`、`IronHideSpell`、`FeastSpell`、`FangSpell`、`TelekinesisSpell` |
| `spells.abyss` | 10 | — |
| `spells.frost` | 9 | `FrostBreathSpell` 系列 |
| `spells.geomancy` | 12 | — |
| `spells.necromancy` | 11 | — |
| `spells.nether` | 12 | — |
| `spells.storm` | 11 | `ChargeSpell`、`DischargeSpell` |
| `spells.utility` | 5 | `CommandSpell`、`CraftingSpell`、`GlowLightSpell`、`IlluminateSpell`、`SoulLightSpell` |
| `spells.void_spells` | 14 | `SnarelingSpell`、`BlastlingSpell`、`WatchlingSpell` |
| `spells.wild` | 12 | — |
| `spells.wind` | 9 | — |

> 注意包名是 **`void_spells`（带下划线）**，不是 `void`。

### 5.2 继承基类后"必须实现哪些方法"

```text
public abstract class com.Polarice3.Goety.common.magic.Spell implements ISpell {
  public abstract int defaultSoulCost();
  public abstract int defaultCastDuration();
  public abstract int defaultSpellCooldown();
  public void mobSpellResult(LivingEntity, ItemStack);
  public void mobSpellResult(LivingEntity, ItemStack, SpellStat);
  public void serverCheckSpellResult(Level, LivingEntity, ItemStack, SpellStat);
  public SpellType getSpellType();                 // 已实现（返回 SpellType.NONE）
  public boolean GeoPower(LivingEntity);
  public boolean isShifting(LivingEntity);
  public boolean conditionsMet(Level, LivingEntity);
  public LivingEntity getTarget(LivingEntity);
  public LivingEntity getTarget(LivingEntity, int);
  public boolean rightStaff(ItemStack);
  public boolean typeStaff(ItemStack, SpellType);
  public void useParticle(Level, LivingEntity, ItemStack);
  public java.util.List<Enchantment> acceptedEnchantments();
  protected HitResult rayTraceCollide(Level, LivingEntity, int, double);
  protected EntityHitResult entityCollideResult(Level, LivingEntity, int, double);
  public SoundSource getSoundSource();
  public float projPitch(RandomSource);
  public void playSound(ServerLevel, Entity, SoundEvent);
  public void playSound(ServerLevel, Entity, SoundEvent, float, float);
  public void playSound(ServerLevel, LivingEntity, float, float);
}
```

反编译出的几条关键链：

```java
public void mobSpellResult(LivingEntity caster, ItemStack staff) {
   this.mobSpellResult(caster, staff, WandUtil.getStats(caster, this));
}

public void mobSpellResult(LivingEntity caster, ItemStack staff, SpellStat spellStat) {
   this.serverCheckSpellResult(caster.level, caster, staff, spellStat);
}

public void serverCheckSpellResult(Level level, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
   if (level instanceof ServerLevel serverLevel) {
      this.SpellResult(serverLevel, caster, staff, spellStat);     // ← 你的实现在这里被调用
   }
}

public SpellType getSpellType() { return SpellType.NONE; }         // 默认 NONE，建议覆写
```

**继承 `Spell` 时必须实现的只有 3 个**：`defaultSoulCost()`、`defaultCastDuration()`、
`defaultSpellCooldown()`（`getSpellType()` 与 `acceptedEnchantments()` 已给出实现）。

继承链上的增量：

| 基类 | 关系 | 新增的抽象方法 |
|---|---|---|
| `Spell` | `implements ISpell`（abstract） | `defaultSoulCost()`、`defaultCastDuration()`、`defaultSpellCooldown()` |
| `ChargingSpell` | `extends Spell implements IChargingSpell`（abstract） | `Cooldown()`（另两个已实现） |
| `EverChargeSpell` | `extends ChargingSpell`（abstract） | 无（`Cooldown()`、`everCharge()` 已实现） |
| `BreathingSpell` | `extends EverChargeSpell implements IBreathingSpell`（abstract） | 无 |
| `SummonSpell` | `extends Spell implements ISummonSpell`（abstract） | `SummonDownDuration()`（`commonResult` 已实现） |
| `BlockSpell` | `extends Spell implements IBlockSpell`（abstract） | 无（`defaultCastDuration()` 已实现） |
| `TouchSpell` | `extends Spell implements ITouchSpell`（abstract） | 无（`defaultCastDuration()` 已实现） |

`SummonSpell` 反编译出的额外行为：

```java
public SpellStat defaultStats() { return super.defaultStats().setDuration(1).setBurning(0); }
public int summonLimit()        { return 64; }
public Predicate<LivingEntity> summonPredicate() { return e -> e instanceof IOwned; }
public boolean NecroPower(LivingEntity e) { return CuriosFinder.hasUndeadCape(e); }
public boolean FrostPower(LivingEntity e) { return CuriosFinder.hasFrostRobes(e); }
// WildPower / NetherPower / GeoPower 同理，都读 CuriosFinder 的套装判定
```

**直接从接口实现**（不继承 `Spell`）时要实现：

- `implements ISpell` → `defaultSoulCost()`、`defaultCastDuration()`、`defaultSpellCooldown()`、
  `getSpellType()`、`acceptedEnchantments()`（5 个）
- `implements IChargingSpell` → 上面 5 个 + `Cooldown()`
- `implements ISummonSpell` → 上面 5 个 + `SummonDownDuration()`、`commonResult(ServerLevel, LivingEntity)`
- `implements IBlockSpell` / `ITouchSpell` / `IBreathingSpell` → 这三个接口自身**不新增抽象方法**

### 5.3 `SpellStat` —— 施法数值载体

```text
public class com.Polarice3.Goety.common.magic.SpellStat {
  public int potency;
  public int duration;
  public int range;
  public double radius;
  public int burning;
  public float velocity;
  public SpellStat(int, int, int, double, int, float);   // ← 顺序：potency,duration,range,radius,burning,velocity
  public SpellStat setPotency(int);   public SpellStat setDuration(int);
  public SpellStat setRange(int);     public SpellStat setRadius(double);
  public SpellStat setBurning(int);   public SpellStat setVelocity(float);
  public SpellStat increasePotency(int);  public SpellStat increaseDuration(int);
  public SpellStat increaseRange(int);    public SpellStat increaseRadius(double);
  public SpellStat increaseBurning(int);  public SpellStat increaseVelocity(float);
  public int getPotency();  public int getDuration();  public int getRange();
  public double getRadius(); public int getBurning();  public float getVelocity();
}
```

`setXxx` 与 `increaseXxx` 都返回 `this`（可链式调用，`SummonSpell` 就是
`super.defaultStats().setDuration(1).setBurning(0)`）。数值来源是 `ModAttributes` 的
`SPELL_POTENCY` / `SPELL_DURATION` / `SPELL_RANGE` / `SPELL_RADIUS` / `SPELL_BURNING` /
`SPELL_VELOCITY`，由 `WandUtil.getStats(LivingEntity, ISpell)` 汇总。

### 5.4 一个完整召唤法术的实现（Goety `VexSpell` 的真实形状）

反编译 `VexSpell` 得到的**召唤流程**（这是本文档最有价值的一段，照抄即可）：

```java
public class VexSpell extends SummonSpell {
   public int defaultSoulCost()      { return SpellConfig.VexCost.get(); }
   public int defaultCastDuration()  { return SpellConfig.VexDuration.get(); }
   public int SummonDownDuration()   { return SpellConfig.VexSummonDown.get(); }
   public int defaultSpellCooldown() { return SpellConfig.VexCoolDown.get(); }
   public SoundEvent CastingSound()  { return SoundEvents.EVOKER_PREPARE_SUMMON; }
   public SpellType  getSpellType()  { return SpellType.ILL; }
   public int summonLimit()          { return SpellConfig.VexLimit.get(); }
   public ColorUtil particleColors(LivingEntity caster) { return new ColorUtil(0.7F, 0.7F, 0.8F); }
   public Predicate<LivingEntity> summonPredicate() { return e -> e instanceof AllyVex; }

   public List<Enchantment> acceptedEnchantments() {
      List<Enchantment> list = new ArrayList();
      list.add(ModEnchantments.POTENCY.get());
      list.add(ModEnchantments.DURATION.get());
      return list;
   }

   public void SpellResult(ServerLevel worldIn, LivingEntity caster, ItemStack staff, SpellStat spellStat) {
      this.commonResult(worldIn, caster);                      // 1. 公共前置（潜行时收兵等）

      int potency  = spellStat.getPotency();
      int duration = spellStat.getDuration();
      if (WandUtil.enchantedFocus(caster)) {                   // 2. focus 被附魔时加成
         potency  += WandUtil.getPotencyLevel(caster);
         duration += WandUtil.getLevels(ModEnchantments.DURATION.get(), caster) + 1;
      }

      if (!this.isShifting(caster)) {
         int i = 3;
         if (this.rightStaff(staff)) i = 3 + worldIn.random.nextInt(3);   // 3. 法杖加成数量

         for (int i1 = 0; i1 < i; ++i1) {
            BlockPos blockpos = caster.blockPosition().offset(-2 + caster.getRandom().nextInt(5), 1,
                                                             -2 + caster.getRandom().nextInt(5));
            AllyVex vexentity = new AllyVex(ModEntityType.VEX_SERVANT.get(), worldIn);
            vexentity.setTrueOwner(caster);                                    // ★ 设主人
            vexentity.moveTo(blockpos, 0.0F, 0.0F);
            vexentity.finalizeSpawn(worldIn, caster.level.getCurrentDifficultyAt(blockpos),
                                    MobSpawnType.MOB_SUMMONED, null, null);
            vexentity.setBoundOrigin(blockpos);
            vexentity.setLimitedLife(MobUtil.getSummonLifespan(worldIn) * duration);   // ★ 寿命
            if (potency > 0) { /* 给主手武器挂 Sharpness = potency */ }

            this.SummonSap(caster, vexentity);          // 4. 主人处于召唤虚弱时削弱新兵
            this.setTarget(caster, vexentity);          // 5. 继承主人的目标
            worldIn.addFreshEntity(vexentity);          // 6. 入世界
            this.summonAdvancement(caster, vexentity);
         }

         this.playSound(worldIn, caster, SoundEvents.EVOKER_CAST_SPELL);
         this.SummonDown(caster);                       // 7. 给自己挂召唤虚弱
      }
   }
}
```

对应的最小自建召唤法术：

```java
package com.croety.spell;

import com.Polarice3.Goety.api.magic.ISummonSpell;
import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.common.magic.SummonSpell;
import com.Polarice3.Goety.utils.ColorUtil;
import com.Polarice3.Goety.utils.WandUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class CroetySummonSpell extends SummonSpell
{
    @Override public int defaultSoulCost()      { return 100; }
    @Override public int defaultCastDuration()  { return 40; }
    @Override public int defaultSpellCooldown() { return 200; }

    /** ISummonSpell 的抽象方法：召唤后的"虚弱"时长（tick）。 */
    @Override public int SummonDownDuration()   { return 1200; }

    @Override public SpellType getSpellType()   { return SpellType.NECROMANCY; }
    @Override public int summonLimit()          { return 4; }
    @Override public ColorUtil particleColors(LivingEntity caster) { return ISummonSpell.DEFAULT_SUMMON; }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat spellStat)
    {
        this.commonResult(level, caster);                 // SummonSpell 已实现
        int duration = spellStat.getDuration();
        if (WandUtil.enchantedFocus(caster)) {
            duration += WandUtil.getLevels(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, caster) + 1;
        }

        // 在这里 new 你的仆从、setTrueOwner(caster)、addFreshEntity(...)
        this.SummonDown(caster);
    }
}
```

普通（非召唤）法术的最小实现，形状照 `SoulBoltSpell`（它覆写了 `defaultSoulCost`、
`defaultCastDuration`、`defaultSpellCooldown`、`CastingSound`、`acceptedEnchantments`、
`SpellResult`）：

```java
package com.croety.spell;

import com.Polarice3.Goety.api.magic.SpellType;
import com.Polarice3.Goety.common.magic.Spell;
import com.Polarice3.Goety.common.magic.SpellStat;
import com.Polarice3.Goety.common.enchantments.ModEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class CroetyBoltSpell extends Spell
{
    @Override public int defaultSoulCost()       { return 10; }
    @Override public int defaultCastDuration()   { return 20; }
    @Override public int defaultSpellCooldown()  { return 30; }

    @Override public SpellType getSpellType()    { return SpellType.NECROMANCY; }
    @Override public SoundEvent CastingSound()   { return SoundEvents.EVOKER_CAST_SPELL; }

    /** 空 List = 这个 focus 在附魔台上完全不可附魔（见 4.1）。 */
    @Override
    public List<Enchantment> acceptedEnchantments()
    {
        List<Enchantment> list = new ArrayList<>();
        list.add(ModEnchantments.POTENCY.get());
        return list;
    }

    @Override
    public void SpellResult(ServerLevel level, LivingEntity caster, ItemStack staff, SpellStat spellStat)
    {
        // spellStat.getPotency() / getDuration() / getRange() / getRadius() / getBurning() / getVelocity()
    }
}
```

> `ISummonSpell` 的 7 个颜色常量的真实数值：
> `DEFAULT_SUMMON = new ColorUtil(9430751)`、`VOID_SUMMON = 13369594`、`WILD_SUMMON = 4209428`、
> `NETHER_SUMMON = 16753408`、`GEO_SUMMON = 16763392`、`NORMAL_SUMMON = ColorUtil.WHITE`、
> `NAMELESS_SUMMON = 11009086`。`ColorUtil` 另有 `(float,float,float,float)`、`(float,float,float)`、
> `(int,int,int,float)`、`(int)`、`(MapColor)`、`(ChatFormatting)` 六个构造函数和
> `WHITE`/`BLACK`/`DARK_RED`/`GOLD`/`DARK_PURPLE`/`LIGHT_PURPLE`/`AQUA` 常量。

### 5.5 法术冷却

冷却不在法术对象里，而在玩家的 soul-energy capability 上：

```text
public class com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown {
  public final Map<Item, FocusCooldown$CooldownInstance> cooldowns;
  public final Map<String, FocusCooldown$CooldownInstance> cooldownsSpecific;
  public static String keyOf(ItemStack);
  public boolean isOnSpecificCooldown(ItemStack);
  public boolean isOnCooldown(Item);
  public float getCooldownPercent(Item);
  public float getSpecificCooldownPercent(ItemStack);
  public void tick(Player, Level);
  public void addCooldown(Player, Level, Item, int);
  public void addSpecificCooldown(Player, Level, ItemStack, int);
  public void removeCooldown(Player, Level, Item);
  public void removeSpecificCooldown(Player, Level, ItemStack);
  public void removeSpecificCooldown(Player, Level, String);
  ...
}
```

想让某个法术有独立冷却（而不是按物品算），覆写
`ISpell.hasCustomCooldown(LivingEntity caster, ItemStack staff, ItemStack focus, int initialCooldown)`。
`IWand.isOnCooldown(...)` 内部就是 `SEHelper.isOnCooldown(player, IWand.getFocus(stack))`。

---

## 6. 方块与方块实体写法

### 6.1 方块实体基类阶梯

```text
ChunkLoadBlockEntity (abstract, extends BlockEntity, implements IChunkLoader)
   ├── ModBlockEntity (abstract)          ← 需要网络同步的普通 BE
   └── OwnedBlockEntity (abstract, implements IOwnedBlock)   ← "有主人"的 BE
          ├── BarracksBlockEntity (abstract, implements IBarrack, GameEventListener)
          ├── TrainingBlockEntity (abstract, implements ITrainingBlock, WorldlyContainer, GameEventListener)
          ├── ThroneBlockEntity (implements IEnchantedBlock)
          ├── BlackCrystalBlockEntity (implements IEnchantedBlock)
          ├── SculkDevourerBlockEntity (implements IEnchantedBlock, GameEventListener)
          └── ...
SaveBlockEntity (abstract, extends BlockEntity)   ← 另一条独立支线
```

```text
public abstract class com.Polarice3.Goety.common.blocks.entities.ChunkLoadBlockEntity
        extends net.minecraft.world.level.block.entity.BlockEntity
        implements com.Polarice3.Goety.api.entities.IChunkLoader {
  public long ticketTime;
  public boolean saveDataCheck;
  public ChunkLoadBlockEntity(BlockEntityType<?>, BlockPos, BlockState);
  public long getTicketTime();  public void setTicketTime(long);
  public boolean saveDataCheck();  public void saveDataChecked();
}

public abstract class com.Polarice3.Goety.common.blocks.entities.ModBlockEntity
        extends ChunkLoadBlockEntity {
  public ModBlockEntity(BlockEntityType<?>, BlockPos, BlockState);
  public abstract void readNetwork(CompoundTag);            // ← 必须实现
  public abstract CompoundTag writeNetwork(CompoundTag);    // ← 必须实现
  public void onDataPacket(Connection, ClientboundBlockEntityDataPacket);
  public void handleUpdateTag(CompoundTag);
  public CompoundTag getUpdateTag();
  public void load(CompoundTag);
  public void saveAdditional(CompoundTag);
  public ClientboundBlockEntityDataPacket getUpdatePacket();
  public void markUpdated();
  public Packet getUpdatePacket();
}

public abstract class com.Polarice3.Goety.common.blocks.entities.OwnedBlockEntity
        extends ChunkLoadBlockEntity implements com.Polarice3.Goety.api.blocks.entities.IOwnedBlock {
  public OwnedBlockEntity(BlockEntityType<?>, BlockPos, BlockState);
  public CompoundTag getUpdateTag();   public void load(CompoundTag);
  public void saveAdditional(CompoundTag);
  public void readNetwork(CompoundTag);  public CompoundTag writeNetwork(CompoundTag);
  public ClientboundBlockEntityDataPacket getUpdatePacket();
  public UUID getOwnerUUID();          public void setOwnerUUID(UUID);
  public int getOwnerId();             public void setOwnerId(int);
  public void setOwner(LivingEntity);
  public LivingEntity getTrueOwner();
  public Player getPlayer();                        // IOwnedBlock 的唯一抽象方法，这里已实现
  public void onDataPacket(Connection, ClientboundBlockEntityDataPacket);
  public boolean screenView();
  public void handleUpdateTag(CompoundTag);
  public Packet getUpdatePacket();
}
```

**`OwnedBlockEntity` 已经把 `IOwnedBlock` 需要的东西全实现了**，子类只管写自己的逻辑。
典型的"有主方块"实现（javap 实测）：

| 方块实体 | 实现的接口 |
|---|---|
| `BarracksBlockEntity` | `GameEventListener`, `IBarrack` |
| `TrainingBlockEntity` | `ITrainingBlock`, `WorldlyContainer`, `GameEventListener` |
| `ThroneBlockEntity` | `IEnchantedBlock` |
| `BlackCrystalBlockEntity` | `IEnchantedBlock` |
| `SculkDevourerBlockEntity` | `IEnchantedBlock`, `GameEventListener` |
| `SoulCandlestickBlockEntity` | `ISoulCandle`（注意：它**直接继承 `BlockEntity`**，不是 OwnedBlockEntity） |
| `AnimatorBlockEntity` | `IWaystoneBlock`, `Clearable` |
| `ResonanceCrystalBlockEntity` | `IWindPowered` |

### 6.2 方块层面

```text
public abstract class com.Polarice3.Goety.common.blocks.EnchanteableBlock
        extends BaseEntityBlock implements IEnchanteableBlock {
  protected EnchanteableBlock(BlockBehaviour$Properties);      // 构造是 protected
  public void playerDestroy(Level, Player, BlockPos, BlockState, BlockEntity, ItemStack);
  public void setPlacedBy(Level, BlockPos, BlockState, LivingEntity, ItemStack);
}

public class com.Polarice3.Goety.common.blocks.ThroneBlock
        extends HorizontalDirectionalBlock
        implements SimpleWaterloggedBlock, ISeat, IEnchanteableBlock { ... }

public class com.Polarice3.Goety.common.blocks.RoyalThroneBlock extends ThroneBlock {
  public RoyalThroneBlock(BlockBehaviour$Properties);
  public Vec3 seatOffset(BlockPos);                     // 覆写座位偏移
  public VoxelShape getShape(BlockState, BlockGetter, BlockPos, CollisionContext);
}

public abstract class com.Polarice3.Goety.common.blocks.BarracksBlock extends BaseEntityBlock {
  public static final BooleanProperty POWERED;
  public BarracksBlock(BlockBehaviour$Properties);
  public void setPlacedBy(...); public InteractionResult use(...);
  public boolean isSignalSource(BlockState); public int getSignal(BlockState, BlockGetter, BlockPos, Direction);
  public <T extends BlockEntity> GameEventListener getListener(ServerLevel, T);
}

public abstract class com.Polarice3.Goety.common.blocks.TrainingBlock extends BaseEntityBlock {
  public static final BooleanProperty POWERED;
  // 与 BarracksBlock 同形
}

public class com.Polarice3.Goety.common.blocks.WindBlowerBlock
        extends DirectionalBlock implements EntityBlock {
  public static final IntegerProperty POWER;
  public static final BooleanProperty POWERED;
  public WindBlowerBlock(BlockBehaviour$Properties);
  public RenderShape getRenderShape(BlockState);
  public BlockState getStateForPlacement(BlockPlaceContext);
  public void onPlace(BlockState, Level, BlockPos, BlockState, boolean);
  public void neighborChanged(BlockState, Level, BlockPos, Block, BlockPos, boolean);
  public BlockEntity newBlockEntity(BlockPos, BlockState);
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level, BlockState, BlockEntityType<T>);
}

public class com.Polarice3.Goety.common.blocks.SoulCandlestickBlock extends BaseEntityBlock {
  public static final VoxelShape SHAPE;
  public static final BooleanProperty LIT;
  public SoulCandlestickBlock();
  public BlockEntity newBlockEntity(BlockPos, BlockState);
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level, BlockState, BlockEntityType<T>);
  // + getRenderShape / getShape / createBlockStateDefinition / isPathfindable / animateTick
}
```

### 6.3 灵魂蜡烛（soul candle）系统

Goety 的"灵魂蜡烛"由三部分组成，全部实测：

1. 方块 `ModBlocks.SOUL_CANDLESTICK`（字段类型 `RegistryObject<Block>`），类 `SoulCandlestickBlock`，
   有 `LIT` 布尔状态、`SHAPE` 碰撞箱、ticker。
2. 方块实体 `SoulCandlestickBlockEntity extends BlockEntity implements ISoulCandle`，
   构造函数是 `(BlockPos, BlockState)`（**不是** `(BlockEntityType, BlockPos, BlockState)`），
   提供 `tick()`、`drainSouls(int, BlockPos)`、`getSouls()`、`checkCage()`。
3. 接口 `ISoulCandle` 的方法**全是 default**：
   `drainSouls(BlockPos)`、`drainSouls(int, BlockPos)`、`getSouls()`、`soulDrainAmount()`、`checkCage()`。

"抽灵魂"的联动方还有 `CursedCageBlockEntity` / `SoulAbsorberBlockEntity` / `SoulMenderBlockEntity`
（这几个类存在，但具体协作关系 **未验证**）。

### 6.4 兵营 / 训练方块

- `BarracksBlock`（抽象）+ `BarracksBlockEntity`（抽象，实现 `IBarrack`）。
  `IBarrack` 只有 `getTrainedMob(Level, BlockPos)` 一个抽象方法，其余（`getRange()`、
  `trainLimit()`、`getMobsInRange`、`trainMobs` 等）都有默认实现。
  配套的数据接口是 `api.entities.ITrainable`（给**生物**用）与
  `api.blocks.entities.ITrainingBlock`（给**方块实体**用，扩展 `IOwnedBlock`）。
- `TrainingBlock`（抽象方块）+ `TrainingBlockEntity`（抽象 BE）：
  `ITrainingBlock` 的 5 个抽象方法 `getTrainingTime()`、`getMaxTrainTime()`、
  `amountTrainLeft()`、`maxTrainAmount()`、`getTrainMob()` 由 `TrainingBlockEntity` 实现。
  `TrainingBlockEntity` 还实现了 `WorldlyContainer`（完整物品栏 API）和
  `GameEventListener`（`getListenerSource` / `getDeliveryMode` / `getListenerRadius` / `handleGameEvent`）。
  静态钩子：`clientTick(Level, BlockPos, BlockState, TrainingBlockEntity)` /
  `serverTick(同上)`，字段 `public static int RANGE`。

### 6.5 寻路石（waystone）

寻路石是**物品 + 方块实体**的组合，不是普通方块：

```text
public class com.Polarice3.Goety.common.items.WaystoneItem extends com.Polarice3.Goety.common.items.ItemBase {
  public static final String TAG_OWNER, TAG_OWNER_NAME, TAG_POS, TAG_DIRECTION, TAG_FACING, TAG_DIMENSION;
  public WaystoneItem();
  public boolean isFoil(ItemStack);
  public static boolean hasBlock(ItemStack);
  public static net.minecraft.core.GlobalPos getPosition(ItemStack);
  public static net.minecraft.core.GlobalPos getPosition(CompoundTag);
  public static BlockPos getBlockPos(ItemStack);
  public static BlockEntity getBlockEntity(ItemStack, Level);
  public static Direction getDirection(ItemStack);   public static Direction getDirection(CompoundTag);
  public static Direction getFacing(CompoundTag);
  public static boolean isSameDimension(LivingEntity, ItemStack);
  public static boolean isSameDimension(BlockEntity, ItemStack);
  public static boolean isInRange(Vec3, ItemStack, int);
  public static boolean canAffect(LivingEntity, ItemStack, Vec3, int);
  public InteractionResult interactLivingEntity(ItemStack, Player, LivingEntity, InteractionHand);
  public InteractionResult useOn(UseOnContext);
  public InteractionResultHolder<ItemStack> use(Level, Player, InteractionHand);
  public void appendHoverText(ItemStack, Level, List<Component>, TooltipFlag);
  public static void addWaystoneText(ItemStack, List<Component>);
}
```

物品是 `ModItems.WAYSTONE`。区块侧的接口 `IWaystoneBlock` 由 `AnimatorBlockEntity` 实现，
**全部是 default 方法**：`getDirection()`、`getPosition()`（返回 `GlobalPos`）、`getSoulCost()`、
`isShowBlock()`、`setShowBlock(boolean)`。

### 6.6 风动力（wind powered）

```text
public interface IWindPowered {
  public abstract int activeTicks();      // ← 必须实现
  public abstract void activate(int);     // ← 必须实现
  public default int windPower();         public default void setWindPower(int);
}
```

⚠️ **`WindBlowerBlock` / `WindBlowerBlockEntity` 并不实现 `IWindPowered`** ——
`WindBlowerBlockEntity` 直接继承 `net.minecraft.world.level.block.entity.BlockEntity`
（javap 实测）。真正实现 `IWindPowered` 的是 `ResonanceCrystalBlockEntity`
（`extends ModBlockEntity implements IWindPowered`）。别把这两件事搞混。

`WindBlowerBlockEntity` 的可用方法只有：`WindBlowerBlockEntity(BlockPos, BlockState)`、
`tick()`、`static Vec3 getCenterOf(Vec3i)`、`AABB getAABB()`、`protected void blowEntities()`。

### 6.7 方块 / 方块实体代码片段

```java
package com.croety.block.entity;

import com.Polarice3.Goety.common.blocks.entities.OwnedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 有主人的方块实体。OwnedBlockEntity 已经实现了 IOwnedBlock 的全部方法，
 * 这里只需要补自己需要的 readNetwork / writeNetwork（父类已给出实现，按需覆写）。
 */
public class CroetyAltarBlockEntity extends OwnedBlockEntity
{
    public CroetyAltarBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.CROETY_ALTAR.get(), pos, state);
    }

    @Override
    public void readNetwork(CompoundTag tag)
    {
        super.readNetwork(tag);          // 保留父类对 owner 数据的同步
        // 追加你自己的字段
    }

    @Override
    public CompoundTag writeNetwork(CompoundTag tag)
    {
        // 写入你自己的字段，然后交给父类收尾
        return super.writeNetwork(tag);
    }
}
```

```java
// 方块实体注册（照工作区既有骨架；注意 build(null) 要放进 lambda 里延迟取方块）
public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "croety");

public static final RegistryObject<BlockEntityType<CroetyAltarBlockEntity>> CROETY_ALTAR =
        BLOCK_ENTITIES.register("croety_altar",
                () -> BlockEntityType.Builder.of(CroetyAltarBlockEntity::new, MY_BLOCK.get()).build(null));

// 在 @Mod 构造函数里：
BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
```

---

## 7. 仆从 / 召唤物

### 7.1 归属是怎么存的

**同步数据（`EntityDataAccessor`）**，在 `Owned` 里定义（javap 实测的 protected 静态字段）：

```text
protected static final EntityDataAccessor<java.util.Optional<java.util.UUID>> OWNER_UNIQUE_ID;
protected static final EntityDataAccessor<java.lang.Integer>                  OWNER_CLIENT_ID;
protected static final EntityDataAccessor<java.lang.Boolean>                  HOSTILE;
protected static final EntityDataAccessor<java.lang.Boolean>                  NATURAL;
```

**持久化**走 `addAdditionalSaveData` / `readAdditionalSaveData`，配合接口提供的
`saveOwnedData(CompoundTag)` / `readOwnedData(CompoundTag)`。

**设主人的正确写法**（反编译 `IOwned` 得到的语义）：

```java
vexentity.setTrueOwner(caster);      // → setOwnerId(caster.getUUID()) + setOwnerClientId(caster.getId())
// 解除归属：
owned.removeTrueOwner();             // → setOwnerId(null) + setOwnerClientId(-1)
```

> 别只调 `setOwnerId` 而不调 `setOwnerClientId`：客户端靠 `OWNER_CLIENT_ID` 找主人实体，
> 少了它主人相关逻辑在客户端会失效。

```text
public class com.Polarice3.Goety.common.entities.neutral.Owned
        extends net.minecraft.world.entity.PathfinderMob
        implements com.Polarice3.Goety.api.entities.IOwned,
                   net.minecraft.world.entity.OwnableEntity,
                   com.Polarice3.Goety.api.entities.ICustomAttributes {
  public boolean limitedLifespan; public int limitedLifeTicks;
  public int hasSummonCheck;      public int revivingTime;
  protected Owned(EntityType<? extends Owned>, Level);
  public void setConfigurableAttributes();
  public LivingEntity getTrueOwner();   public LivingEntity getMasterOwner();
  public UUID getOwnerUUID();           public LivingEntity getOwner();
  public UUID getOwnerId();             public void setOwnerId(UUID);
  public int  getOwnerClientId();       public void setOwnerClientId(int);
  public void setHostile(boolean);      public boolean isHostile();
  public void setNatural(boolean);      public boolean isNatural();
  public boolean areOwnedByEachOther(LivingEntity);
  public boolean isAlliedTo(Entity);
  public net.minecraft.world.scores.Team getTeam();
  public void setHasLifespan(boolean);  public boolean hasLifespan();
  public void setLifespan(int);         public int getLifespan();
  public void convertNewEquipment(Entity);
  public EntityType<?> getVariant(Level, BlockPos);
  public int getHasSummonCheck();  public void setHasSummonCheck(int);
  public int getRevivingTime();    public void setRevivingTime(int);
  public boolean doHurtTarget(Entity);  public boolean doHurtTarget(float, Entity);
  public static boolean checkHostileSpawnRules(EntityType<? extends Owned>, ServerLevelAccessor,
                                               MobSpawnType, BlockPos, RandomSource);
  // ...
}
```

### 7.2 仆从基类

```text
public class com.Polarice3.Goety.common.entities.ally.Summoned
        extends com.Polarice3.Goety.common.entities.neutral.Owned
        implements com.Polarice3.Goety.api.entities.ally.IServant {
  protected static final EntityDataAccessor<Byte> SUMMONED_FLAGS;
  protected static final EntityDataAccessor<Byte> UPGRADE_FLAGS;
  public LivingEntity commandPosEntity;   public BlockPos commandPos;
  public BlockPos priorityPos;            public BlockPos boundPos;
  public String boundDim;   public int priorityTime; public int commandTick;
  public int killChance;    public int noHealTime;   public long ticketTime;
  public Summoned(EntityType<? extends Owned>, Level);
  public boolean isWandering();  public void setWandering(boolean);
  public boolean isStaying();    public void setStaying(boolean);
  public boolean canUpdateMove(); public boolean isCommanded();
  public void setCommandPos(BlockPos, boolean);   public BlockPos getCommandPos();
  public void setCommandPosEntity(LivingEntity);  public LivingEntity getCommandPosEntity();
  public int getCommandTick();   public void setCommandTick(int);
  public BlockPos getBoundPos(); public void setBoundPos(BlockPos);
  public String getBoundDim();   public void setBoundDim(String);
  public boolean isUpgraded();   public void setUpgraded(boolean);
  public void upgrade();         public void downgrade();
  public void warnKill(Player);  public void tryKill(Player);
  public DamageSource getServantAttack();
  public void summonParticles(ServerLevel, MobSpawnType);
  public long getTicketTime();  public void setTicketTime(long);  public long decreaseTicketTime();
  // ...
}
```

`Summoned` 是**具体类（不是 abstract）**，它把 `IServant` 的抽象方法全实现了。
所以 **`extends Summoned` 时没有任何抽象方法必须写**。

如果你要**从零**实现 `IServant`（不继承 `Owned` / `Summoned`），必须实现：

1. 来自 `IOwned`：`getTrueOwner()`、`getOwnerId()`、`setOwnerId(UUID)`、
   `setHostile(boolean)`、`isHostile()`
2. 来自 `IServant`：`isWandering()`、`setWandering(boolean)`、`isStaying()`、
   `setStaying(boolean)`、`canUpdateMove()`、`isCommanded()`、
   `setCommandPosEntity(LivingEntity)`、`tryKill(Player)`
3. 来自 `IChunkLoader`（`IServant` 的第二个父接口）：**没有**抽象方法，全是 default

`IServant.GUARDING_RANGE` 的真实定义是 `MobsConfig.ServantGuardingRange.get()` ——
**运行期配置值**，不是编译期常量。

### 7.3 Goety 已有的仆从基类

```text
com.Polarice3.Goety.common.entities.ally.golem.AbstractGolemServant (abstract)
        extends Summoned implements IGolem
com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant (abstract)
        extends com.Polarice3.Goety.common.entities.ally.illager.raider.RaiderServant
        implements ITrainable, ILooter
com.Polarice3.Goety.common.entities.ally.spider.AbstractSpiderServant (abstract)
```

`AbstractGolemServant` 的构造实参类型是
`EntityType<? extends com.Polarice3.Goety.common.entities.neutral.Owned>`。
`AbstractIllagerServant` 构造参数同型，还实现了 `InventoryCarrier` 的
`getInventory()` / `getSlot(int)`（配合 `ILooter`）。

具体仆从（可作模板，名字全部来自类清单）：
`ally` 包有 `BlackBeast`、`BlackWolf`、`BearServant`、`GhastServant`、`HoglinServant`、
`ElderGuardianServant`、`GuardianServant`、`EndermiteServant`、`SilverfishServant`、
`SlimeServant`、`MagmaCubeServant`、`CryptSlimeServant`、`TropicalSlimeServant`、
`Doppelganger`、`Leapleaf`、`Snapper`、`Gnasher`、`Hellhound`、`Stormhound`、
`WinterWolf`、`TwilightGoat`、`Whisperer`、`Wavewhisperer`、`MiniGhast`、`SpriteMob`、`AnimalSummon`；
`ally.golem` 有 `IceGolem`、`SquallGolem`、`RedstoneGolem`、`RedstoneCube`、
`RedstoneMinistrosity`、`RedstoneMonstrosity`、`StoneMinistrosity`、`RaiderGolemServant`；
`ally.undead` 有 `ReaperServant`、`WraithServant`、`MuckWraithServant`、`BorderWraithServant`、
`PhantomServant`、`Haunt`、`HauntedSkull`、`HauntedArmorServant`、`GraveGolem`；
`ally.spider` 有 `SpiderServant`、`CaveSpiderServant`、`BoneSpiderServant`、`IcySpiderServant`、
`WebSpiderServant`、`BroodMotherServant`；
`ally.ender` 有 `BlastlingServant`、`SnarelingServant`、`WatchlingServant`；
`ally.illager` 有 `VindicatorServant`、`PillagerServant`、`EvokerServant`、`GeomancerServant`、
`CryologerServant`、`IceologerServant`、`WindCallerServant`、`StormCasterServant`、
`MountaineerServant`、`CrusherServant`、`PikerServant`、`SignalerServant`、
`VindicatorChefServant`、`Neollager`、`SpellcasterIllagerServant`。
另有 `ally.illager.raider.AllyVex`、`AllyIrk`（`VexSpell` 召唤的正是这两个）。

### 7.4 相关工具

```text
com.Polarice3.Goety.utils.ServantUtil:
  public static void convertZombies(Entity, LivingEntity, boolean);
  public static void convertSkeletons(Entity, LivingEntity, boolean, boolean);
  public static boolean convertUndead(Mob, LivingEntity, boolean, boolean);
  public static void infect(LivingEntity, LivingEntity, boolean, boolean);
  public static void infect(Mob, LivingEntity, boolean, boolean);
  public static boolean isFrostHeal(LivingEntity);   // 各系仆从治疗判定
  public static boolean isWindHeal / isStormHeal / isWildHeal / isGeoHeal /
                        isNetherHeal / isNecroHeal / isAbyssHeal / isVoidHeal (LivingEntity);
  public static boolean isValidServantHeal(LivingEntity);
  public static boolean notServantButOwned(LivingEntity);
  public static void healServant(LivingEntity, LivingEntity);
  public static void healServant(Player, LivingEntity);
  public static Entity peekReviveTarget(IOwned);
  public static Entity teleportToRevive(IOwned);
  public static InteractionResult equipServantArmor(LivingEntity, Summoned, ItemStack, InteractionResult);
  public static EquipmentSlot getClickedSlot(Mob, Vec3);
  public static boolean nullifyTarget(IOwned, LivingEntity);

com.Polarice3.Goety.utils.MobUtil:
  public static final java.util.function.Predicate<LivingEntity> NO_CREATIVE_OR_SPECTATOR;
  public static final java.util.function.Predicate<Entity> LIVING_OR_PART;
  public static boolean areAllies(Entity, Entity);
  public static boolean illagerAllies(Entity, Entity);
  public static boolean sameDimension(Entity, Entity);
  public static boolean isShifting(Entity);
  public static boolean validEntity(Entity);
  // + knockBack / pull / push / twister / drag / WebMovement / ClimbAnyWall ...
  // + getSummonLifespan(Level)（VexSpell 用它算仆从寿命）
```

`MobUtil.areAllies(Entity, Entity)` 是判断"这两个生物算不算一伙"的**正确入口**
（`ISummonSpell.setTarget` 内部就用它），不要自己去比 UUID。

### 7.5 仆从代码片段

```java
package com.croety.entity;

import com.Polarice3.Goety.common.entities.ally.Summoned;
import com.Polarice3.Goety.common.entities.neutral.Owned;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Summoned 已经是具体类，且实现了 IServant 的全部抽象方法，
 * 所以这里不需要再写任何"必须实现"的方法。
 * 注意构造参数类型是 EntityType<? extends Owned>。
 */
public class CroetyServant extends Summoned
{
    public CroetyServant(EntityType<? extends Owned> type, Level level)
    {
        super(type, level);
    }
}
```

### 7.6 生成一个仆从（照抄 `VexSpell` 的 7 步）

```java
// 摘自 VexSpell.SpellResult 的反编译结果（只保留流程）
BlockPos pos = caster.blockPosition().offset(-2 + caster.getRandom().nextInt(5), 1,
                                            -2 + caster.getRandom().nextInt(5));
CroetyServant servant = new CroetyServant(ModEntityTypes.CROETY_SERVANT.get(), worldIn);
servant.setTrueOwner(caster);                                     // 1. 设主人（= ownerId + ownerClientId）
servant.moveTo(pos, 0.0F, 0.0F);                                  // 2. 摆位置
servant.finalizeSpawn(worldIn, caster.level.getCurrentDifficultyAt(pos),
                      MobSpawnType.MOB_SUMMONED, null, null);      // 3. 走一遍生成流程
servant.setBoundOrigin(pos);                                      // 4. 绑定"家"的位置
servant.setLimitedLife(MobUtil.getSummonLifespan(worldIn) * duration);   // 5. 限时寿命
this.SummonSap(caster, servant);                                  // 6. 主人有召唤虚弱时削弱它
this.setTarget(caster, servant);                                  // 7. 继承主人的目标
worldIn.addFreshEntity(servant);
this.summonAdvancement(caster, servant);
// ... 最后
this.SummonDown(caster);                                          // 给自己挂召唤虚弱
```

`SummonSap` 的效果（反编译 `ISummonSpell`）：主人身上有 `GoetyEffects.SUMMON_DOWN` 时，
新召唤物会被加 `WEAKNESS` + `GoetyEffects.SAPPED`，装备被随机磨损，生命值减半。
`SummonDown` 会给自己叠 `SUMMON_DOWN` 效果（等级 clamp 到 0..4），
而且**如果 focus 被附魔了，虚弱时长乘 1.5**。

---

## 8. 与 Create / 原版互操作

### 8.1 往 Goety 创造模式页签加物品（**插件必须走这条路**）

Goety 的 4 个页签（注册名 → ResourceKey，来自 2.2 的源码）：

| ResourceKey | 常量 |
|---|---|
| `goety:goety` | `ModCreativeTab.TAB` |
| `goety:goety_block` | `ModCreativeTab.BLOCK_TAB` |
| `goety:goety_focus` | `ModCreativeTab.FOCUS_TAB` |
| `goety:goety_servants` | `ModCreativeTab.SERVANT_TAB` |

**Goety 的 4 个 generator 只遍历 Goety 自己的注册表**，所以你的物品不会自动出现。
用 Forge 事件（从 forge jar 实测）：

```text
public final class net.minecraftforge.event.BuildCreativeModeTabContentsEvent
        extends net.minecraftforge.eventbus.api.Event
        implements net.minecraftforge.fml.event.IModBusEvent,
                   net.minecraft.world.item.CreativeModeTab$Output {
  public CreativeModeTab getTab();
  public ResourceKey<CreativeModeTab> getTabKey();
  public FeatureFlagSet getFlags();
  public CreativeModeTab$ItemDisplayParameters getParameters();
  public boolean hasPermissions();
  public MutableHashedLinkedMap<ItemStack, CreativeModeTab$TabVisibility> getEntries();
  public void accept(ItemStack, CreativeModeTab$TabVisibility);
  public void accept(java.util.function.Supplier<? extends ItemLike>, CreativeModeTab$TabVisibility);
  public void accept(java.util.function.Supplier<? extends ItemLike>);
}
```

`RegistryObject` 实现 `Supplier`，而且 `RegistryObject#getKey()` 返回 `ResourceKey<T>`
（在 forge jar 里实测：`public net.minecraft.resources.ResourceKey<T> getKey();`），
所以两种比较方式都能编译：

```java
package com.croety.integration;

import com.Polarice3.Goety.Goety;
import com.Polarice3.Goety.init.ModCreativeTab;
import com.croety.Croety;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** 注意：这是 MOD 事件总线上的事件，必须注册到 mod bus（不是 forge bus）。 */
public final class GoetyTabIntegration
{
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event)
    {
        ResourceKey<CreativeModeTab> key = event.getTabKey();

        if (key.equals(ModCreativeTab.TAB.getKey()))              // goety:goety
        {
            event.accept(Croety.TEST_ITEM);                       // Supplier<? extends ItemLike>
        }
        else if (key.location().equals(Goety.location("goety_focus")))
        {
            event.accept(Croety.CROETY_BOLT_FOCUS);               // goety:goety_focus
        }
    }
}
```

### 8.2 标签：Goety 的标签常量 + 数据包文件

**代码里用常量**（见 2.8 的完整清单）：

```java
import com.Polarice3.Goety.init.ModTags;
import net.minecraft.world.item.ItemStack;

public static boolean isWandOrStaff(ItemStack stack) { return stack.is(ModTags.Items.WANDS); }
public static boolean isFocus(ItemStack stack)       { return stack.is(ModTags.Items.FOCUSES); }
```

**数据包侧**（`data/goety/tags/`，直接从 jar 列目录确认）。最重要的几个：

| 标签 | 内容 |
|---|---|
| `goety:focuses` | **所有**法术载体，共 133 项（`soul_bolt_focus`、`teeth_focus`、`vexing_focus`、`snaring_focus` …） |
| `goety:wands` | `goety:dark_wand` + `#goety:staffs` |
| `goety:staffs` | 11 个法杖：`necro_staff`、`nameless_staff`、`ominous_staff`、`frost_staff`、`wild_staff`、`wind_staff`、`storm_staff`、`geo_staff`、`abyss_staff`、`void_staff`、`nether_staff` |
| `goety:totems` | `totem_of_roots`、`totem_of_souls` |
| `goety:robes` / `crowns` / `capes` / `witches_hat` | 套装判定 |
| `goety:grimoires` | `grimoire_of_grudges`、`grimoire_of_goodwill`、`grimoire_of_grounding` |
| `goety:meat`、`hoglin_meat`、`brewable_food` | 食物 |
| `goety:skulls`、`chests`、`trapped_chests`、`bookshelves`、`leaves` | 通用 |
| `goety:witch_currency` / `witch_better_currency` | 女巫交易货币 |
| `goety:sabbath_ritual`、`respawn_boss`、`lich_wither_items` | 机制 |
| `goety:servants`、`bosses`、`mini_bosses`、`golems`、`golem_servants`、`ignore_servants` … | 实体类型（见 2.8） |
| `goety:hellfire`、`magic_fire`、`physical`、`shock_attacks`、`frost_attacks` … | 伤害类型 |

**Goety 也往原版 Forge 标签里写东西**（`data/forge/tags/`）：`forge:tools/scythes`、
`forge:tools/hammers`、`forge:tools/daggers`、`forge:ores/jade`、`forge:gems/jade`、
`forge:storage_blocks/jade`、`forge:bookshelves`、`forge:chests/wooden`、
`forge:glass/colorless`、`forge:entity_types/bosses`、`forge:entity_types/spirit`、
`forge:damage_type/is_magic`、`forge:fluids/void`。
**插件要跟别人互通，就往这些 `forge:` 标签里加**（用 datagen 或手写 JSON）。

### 8.3 Curios 栏位

Goety 占用的 8 个饰品栏标签（`data/curios/tags/items/`）：
`charm`、`ring`（+ 兼容用 `rings`）、`hands`、`head`、`body`、`back`、`belt`、`necklace`。
实测内容举例：

- `charm`：`totem_of_roots`、`totem_of_souls`、`alarming_charm`、`ominous_charm`、`targeting_monocle`
- `belt`：`focus_bag`、`focus_pack`、`brew_bag`、`warlock_sash`、`wayfarers_belt`、`spiteful_belt`
- `body`：23 件长袍（`dark_robe` … `eternal_cauldron`）
- `necklace`：`star_amulet`、`pendant_of_hunger`、`sea_amulet`、`amethyst_necklace`、`feline_amulet`
- `head`：`#goety:plushie` + 各种帽子/王冠（含 `targeting_monocle`）

Curios API 侧（从 curios jar 实测）：

```text
public interface top.theillusivec4.curios.api.type.capability.ICurioItem {
  public static final ICurio defaultInstance;
  public default boolean hasCurioCapability(ItemStack);
  public default void curioTick(SlotContext, ItemStack);
  public default void onEquip(SlotContext, ItemStack, ItemStack);
  public default void onUnequip(SlotContext, ItemStack, ItemStack);
  public default boolean canEquip(SlotContext, ItemStack);
  public default boolean canUnequip(SlotContext, ItemStack);
  public default Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext, java.util.UUID, ItemStack);
  public default void onEquipFromUse(SlotContext, ItemStack);
  public default boolean canEquipFromUse(SlotContext, ItemStack);
  public default ICurio$DropRule getDropRule(SlotContext, DamageSource, int, boolean, ItemStack);
  // ... 全部 default，没有抽象方法
}

public final class top.theillusivec4.curios.api.SlotContext extends java.lang.Record {
  public SlotContext(String, LivingEntity, int, boolean, boolean);
  public String getIdentifier();  public int getIndex();  public LivingEntity getWearer();
  public String identifier();     public LivingEntity entity();
  public int index();             public boolean cosmetic();  public boolean visible();
}

public final class top.theillusivec4.curios.api.CuriosApi {
  public static final String MODID;
  public static void registerCurio(Item, ICurioItem);
  public static java.util.Optional<ISlotType> getSlot(String, Level);
  public static java.util.Map<String, ISlotType> getSlots(Level);
  public static java.util.Map<String, ISlotType> getPlayerSlots(Player);
  public static java.util.Map<String, ISlotType> getItemStackSlots(ItemStack, LivingEntity);
  public static net.minecraftforge.common.util.LazyOptional<ICuriosItemHandler> getCuriosInventory(LivingEntity);
  public static net.minecraftforge.common.util.LazyOptional<ICurio> getCurio(ItemStack);
  public static boolean isStackValid(SlotContext, ItemStack);
  public static java.util.UUID getSlotUuid(SlotContext);
  // ...
}
```

**Goety 自己的查法**（推荐直接用，别自己遍历）：

```java
import com.Polarice3.Goety.utils.CuriosFinder;
import net.minecraft.world.entity.LivingEntity;

public static boolean hasMyCharm(LivingEntity entity)
{
    return CuriosFinder.hasCurio(entity, MyItems.MY_CHARM.get());   // hasCurio(LivingEntity, Item)
}
```

### 8.4 伤害类型

`ModDamageSource` 里定义了 **40** 个 `ResourceKey<DamageType>` 常量（javap 实测）：

```text
SUMMON, SHOCK, DIRECT_SHOCK, INDIRECT_SHOCK, LIGHTNING, DIRECT_FREEZE, INDIRECT_FREEZE,
ICE_SPIKE, DRENCH, DIRECT_DRENCH, INDIRECT_DRENCH, SWORD, WIND_BLAST, ICE_BOUQUET,
HELLFIRE, INDIRECT_HELLFIRE, MAGIC_FIRE, MAGIC_FIREBALL, NO_OWNER_MAGIC_FIREBALL,
LOOT_EXPLODE, LOOT_EXPLODE_OWNED, FIRE_BREATH, FROST_BREATH, BUBBLE_STREAM, MAGIC_BOLT,
SOUL_LEECH, LIFE_LEECH, ROT, ACID, VENOM, SPIKE, BOILING, PHOBIA, CHOKE, SWARM,
VOIDED, RAGE, DISMISSED, DOOM, DEATH
```

对应 JSON 在 `data/goety/damage_type/<name>.json`，例如 `hellfire.json`：

```json
{
  "effects": "burning",
  "exhaustion": 0.0,
  "message_id": "goety.hellfire",
  "scaling": "when_caused_by_living_non_player"
}
```

常用工厂方法（全部实测）：

```text
ModDamageSource.getDamageSource(Level, ResourceKey<DamageType>);
ModDamageSource.getDamageSource(Level, ResourceKey<DamageType>, EntityType<?>...);
ModDamageSource.entityDamageSource(Level, ResourceKey<DamageType>, Entity, EntityType<?>...);
ModDamageSource.getEntityDamageSource(Level, ResourceKey<DamageType>, Entity, EntityType<?>...);
ModDamageSource.indirectEntityDamageSource(Level, ResourceKey<DamageType>, Entity, Entity);
ModDamageSource.getIndirectEntityDamageSource(Level, ResourceKey<DamageType>, Entity, Entity, EntityType<?>...);
ModDamageSource.source(Level, ResourceKey<DamageType>, Entity, Entity);
ModDamageSource.noKnockbackDamageSource(Level, ResourceKey<DamageType>, Entity, Entity, EntityType<?>...);
ModDamageSource.ownedDamageSource(Level, ResourceKey<DamageType>, Entity, LivingEntity);
ModDamageSource.summonAttack(LivingEntity /*owner*/, LivingEntity /*attacker*/);
ModDamageSource.directShock(LivingEntity);   ModDamageSource.indirectShock(Entity, Entity);
ModDamageSource.lightning(Entity, Entity);   ModDamageSource.directFreeze(LivingEntity);
ModDamageSource.indirectFreeze(Entity, Entity); ModDamageSource.iceSpike(Entity, Entity);
ModDamageSource.directDrench(LivingEntity);  ModDamageSource.indirectDrench(Entity, Entity);
ModDamageSource.modFireball(Entity, Level);  ModDamageSource.magicFireball(Fireball, Entity, Level);
ModDamageSource.lootExplosion(Entity, Entity, Level); ModDamageSource.sword(Entity, Entity);
ModDamageSource.iceBouquet(Entity, Entity);  ModDamageSource.hellfire(Entity, Entity);
ModDamageSource.fireBreath(Entity, Entity);  ModDamageSource.magicFireBreath(Entity, Entity);
ModDamageSource.frostBreath(Entity, Entity); ModDamageSource.bubbleStream(Entity, Entity);
ModDamageSource.magicBolt(Entity, Entity);   ModDamageSource.acid(Entity, Entity);
ModDamageSource.spike(Entity, Entity);       ModDamageSource.windBlast(Entity, Entity);
ModDamageSource.deathCurse(Entity);
ModDamageSource.soulLeech(Entity, Entity);   ModDamageSource.lifeLeech(Entity, Entity);
ModDamageSource.choke(Entity, Entity);       ModDamageSource.swarm(Entity, Entity);
// 判定：
ModDamageSource.hellfireAttacks(DamageSource);  ModDamageSource.isMagicFire(DamageSource);
ModDamageSource.shockAttacks(DamageSource);     ModDamageSource.freezeAttacks(DamageSource);
ModDamageSource.waterAttacks(DamageSource);     ModDamageSource.physicalAttacks(DamageSource);
ModDamageSource.toolAttack(DamageSource, java.util.function.Predicate<Item>);
ModDamageSource.wantingAttacks(DamageSource);
// 数据生成：
ModDamageSource.bootstrap(net.minecraft.data.worldgen.BootstapContext<DamageType>);
ModDamageSource.source(String);   // static String source(String)
```

**你的仆从打人要用 `ModDamageSource.summonAttack(owner, attacker)`**，
不然不计入"召唤物击杀"，灵魂/成就都不认。

### 8.5 属性

见 2.3 的 `ModAttributes` 全表。要点：

- 通用：`SPELL_POTENCY`、`SPELL_DURATION`、`SPELL_RANGE`、`SPELL_RADIUS`、
  `SPELL_BURNING`、`SPELL_VELOCITY`、`CASTING_SPEED`、`COOLDOWN_DISCOUNT`、`SOUL_DISCOUNT`
- 10 学派各一对 `*_POTENCY` / `*_DISCOUNT`：`ABYSS`、`FROST`、`GEOMANCY`、
  `NECROMANCY`、`NETHER`、`STORM`、`VOID`、`WILD`、`WIND`（`ILL` 学派**没有**对应属性）
- 读数值用 getter，不要直接 `getAttribute(...)`：
  `ModAttributes.getPotency(LivingEntity)` / `getPotency(LivingEntity, ISpell)` /
  `getDuration` / `getRange` / `getRadius` / `getBurning` / `getVelocity` /
  `getCastingSpeed` / `getCooldownDiscount` / `getSoulDiscount(LivingEntity, ISpell)`

### 8.6 和 Create 的关系

**Goety 2.5.57.3 里没有任何 Create 相关类。**
对 Goety 全类清单 grep `simibubi|create` 的结果是 **0**。

所以 Create × Goety 的联动只能：

1. 在 **croety** 侧写代码，同时 import 两边的类（这正是本工作区的做法）；
2. 用**标签 / 数据包**做数据层面的桥；
3. 用 Create 的 API（`BlockSpoutingBehaviour`、`BoilerHeater` 等）注册 —— 那属于
   `docs/ai/create-6.0.8.md` 的范畴。

Goety 侧唯一对"别的 mod"开放的官方机制是：

- `com.Polarice3.Goety.init.ModDispenserRegister.registerAlternativeDispenseBehavior(...)`
- `com.Polarice3.Goety.api.ritual.RitualType.addRitualType(String, IRitualType)`
- `com.Polarice3.Goety.api.magic.SpellType.create(...)` / `GolemType.create(...)` /
  `IllagerType.create(...)`
- Goety 自己在 `FMLCommonSetupEvent` 里调用了
  `com.Polarice3.Goety.compat.OtherModCompat.setup(FMLCommonSetupEvent)` ——
  这是 Goety 实现 mod 兼容的地方（内容 **未验证**，值得反编译看一眼）。

---

## 9. 常见坑

按"踩到会浪费多少时间"排序。除标注外都由 jar / 源码直接证实。

### 9.1 包名大小写（最容易翻车）

- 根包是 `com.Polarice3.Goety`：`Polarice3` 的 P 大写，`Goety` 的 G 大写。
- 拼错只会得到 `找不到符号`。
- 想不起全名就 `.	oolsind-api.ps1 -Search GoetyXXX -Jar goety`。

### 9.2 `IServant` 不在你以为的包里

```text
✗ com.Polarice3.Goety.api.entities.IServant          —— javap 报"找不到类"
✓ com.Polarice3.Goety.api.entities.ally.IServant
```

### 9.3 `IChargeable` 不存在

只有 `com.Polarice3.Goety.api.entities.ICharger`（两个方法：`isCharging()` / `setCharging(boolean)`）。

### 9.4 `ModParticles` 不存在

`com.Polarice3.Goety.init` 下**没有** `ModParticles`。粒子是
`com.Polarice3.Goety.client.particles.ModParticleTypes`（**客户端包**）。
同理，客户端专用的东西都在 `com.Polarice3.Goety.client.*`，
服务端代码里 import 它会在专用服务器上炸。

### 9.5 ⚠️ 别用临时类清单文件判断"某类不存在"

本项目早期生成过一份 `goety-classes.txt`（2121 行），但它**漏掉了嵌套类**：

```text
jar 里的 .class 条目数            : 3125
那份清单的行数                    : 2121
差额主要就是 ModTags$Items 这类嵌套类
```

我就因此一度误判"`ModTags` 里没有任何标签常量"，实际它有 **10 个嵌套类、近百个 TagKey 常量**
（见 2.8）。**结论：判断某类/某成员是否存在，用 `.	oolsind-api.ps1 -Class/-Search`，
不要用那份清单。**

### 9.6 法术没有注册表

`new MySpell()` 不会注册到任何地方。法术对象只存在于 `MagicFocus.spell` 字段里
（见 5.1 的反编译证据）。想"注册"法术 = 注册一个 `MagicFocus` 物品。

另外 `ModItems.isFocus(Item)` 的实现是 `item instanceof MagicFocus`（**不是** `instanceof IFocus`），
所以只实现 `IFocus` 的自定义类不会进 Goety 的 focus 页签。

### 9.7 Goety 的页签不会自动收录你的物品

4 个 generator 都只遍历 `ModItems.ITEMS` / `ModSpawnEggs.ITEMS` / `ServantSpawnEggs.ITEMS`
（源码见 2.2）。必须用 `BuildCreativeModeTabContentsEvent`（见 8.1）。

### 9.8 `ModBlocks` / `ModItems` 的字段类型被擦成了父类

`ModBlocks.SOUL_CANDLESTICK` 的声明类型是 `RegistryObject<Block>`，
不是 `RegistryObject<SoulCandlestickBlock>`。要用子类方法必须：

```java
Block b = ModBlocks.SOUL_CANDLESTICK.get();
if (b instanceof SoulCandlestickBlock candlestick) { /* ... */ }
```

只有少数几个字段保留了具体类型（`CRYPT_CHEST` → `CryptChestBlock`、
`LOFTY_CHEST` → `LoftyChestBlock`、`VOID_FLUID`/`END_MUD_FLUID` → `LiquidBlock`、
`TOTEM_OF_SOULS` → `TotemOfSouls`、`TOTEM_OF_ROOTS` → `FullSpentTotem`、
`JEI_DUMMY_*` → `DummyItem`、所有饰品 → `SingleStackItem`）。

### 9.9 `ModEntityType` / `ModBlockEntities` 没有 `init()`

`ModItems` 和 `ModBlocks` 有 `public static void init()`；
`ModEntityType`、`ModBlockEntities`、`ModParticleTypes` **没有**。
它们的 `DeferredRegister` 是在 **Goety 构造函数内部**直接 `register(bus)` 的。
自己写代码时不要照抄"没有 init"这个形状 —— 你自己的注册器必须在
`@Mod` 构造函数里显式注册到 mod 事件总线，否则**什么都不会注册**且**不会报错**。

### 9.10 Goety 的 NBT 键里有空格

`ISoulContainer.SOULS_AMOUNT = "Souls"`（无空格），但
`ITotem.MAX_SOUL_AMOUNT = "Max Souls"`（有空格），
`IWand.SOULCOST = "Soul Cost"`、`"Cast Time"`、`"Soul Use"` 等**全部带空格**。
写错键名的后果是"读出来永远是 0"，不报错。

### 9.11 灵魂容器的静态方法有 `instanceof` 守卫

`ISoulContainer.setSoulsAmount` / `decreaseSouls` 内部先判断
`itemStack.getItem() instanceof ISoulContainer`，不满足就**静默返回**。
你的物品类必须 `implements ISoulContainer`（读方法 `currentSouls` / `isEmpty` 没有守卫）。

### 9.12 `ITotem` 的两个陷阱

- 同时存在 `setSoulsAmount` 和 `setSoulsamount`（后者全小写 a，
  已被标记 `@Deprecated(forRemoval = true)`）。用规范的 `setSoulsAmount`。
- `ITotem.MAX_SOULS` 的真实值是 `MainConfig.MaxSouls.get()`，**运行期配置值**，
  不是编译期常量，不能用于 `case` 标签或常量折叠。

### 9.13 法术包名 `void_spells` 带下划线

`com.Polarice3.Goety.common.magic.spells.void_spells`，不是 `void`。

### 9.14 `WindBlowerBlockEntity` 不实现 `IWindPowered`

别看着名字就想当然（详见 6.6）。`IWindPowered` 的实现者是 `ResonanceCrystalBlockEntity`。

### 9.15 `api.client.SpellArmPose` 是个完全空的枚举

反编译全文只有一个空的 `$values()`，**零个枚举常量、零个 `create` 方法**。
不要用它；姿势走 `api.magic.SpellPoses` 或 `ISpell.getPose(...)`。

### 9.16 `ModCauldronInteraction` 是接口不是类

`public interface com.Polarice3.Goety.init.ModCauldronInteraction`。它有静态字段
（`VOID` / `MUD`）和静态方法（`newInteractionMap()` / `init()`），但不能 `new`。

### 9.17 注册顺序 / 类加载

- Goety 的 `DeferredRegister` 全部挂在 **mod 事件总线**上。你自己的也必须挂到 mod bus
  （`FMLJavaModLoadingContext.get().getModEventBus()`），挂到
  `MinecraftForge.EVENT_BUS` 上不会有任何效果。
- `RegistryObject.get()` 在对应注册阶段完成前会抛异常。
  在**静态初始化块**里访问别的 mod 的 `RegistryObject.get()`（比如
  `ModItems.TOTEM_OF_SOULS.get()`）是危险的 —— 用 `Supplier` 延迟到 `FMLCommonSetupEvent`
  之后再取。工作区里的 `GoetyIntegration.logEnvironment()` 就是按这个原则写的
  （"Only call this once Goety has finished registering its content"）。
- Goety 的 `Goety()` 构造函数里，`ModCreativeTab.CREATIVE_MODE_TABS` 排在
  `ModItems.init()` 之后的 `SidedInit.init()` 之前，而页签的 generator 是
  **lazy 的 `Supplier`**（`CREATIVE_MODE_TABS.register(name, () -> CreativeModeTab.builder()...)`），
  所以顺序本身不构成问题。**但别依赖这个顺序去写挂载代码**，用事件。

### 9.18 `DeferredRegister` 字段不全是 final

`ModItems.ITEMS`、`ModBlocks.BLOCKS`、`ModBlockEntities.BLOCK_ENTITY`、
`ModParticleTypes.PARTICLE_TYPES` 都是 `public static`（**非 final**）；
只有 `ModEntityType.ENTITY_TYPE`、`ModCreativeTab.CREATIVE_MODE_TABS`、
`ModSpawnEggs.ITEMS`、`ServantSpawnEggs.ITEMS` 是 `final`。
不要假设它们不可变，也不要在运行时给它们重新赋值。

### 9.19 枚举 `SpellType` / `GolemType` / `IllagerType` 要用 `create`

它们是 `IExtensibleEnum`，**不能** `switch` 穷举，扩展必须走
`SpellType.create(name, baseName)` / `GolemType.create(name, blockStateSupplier, mold)` /
`IllagerType.create(name, trainIllager)`。什么时候调用（静态初始化还是构造期间）
**未验证**，Forge 的 `IExtensibleEnum` 惯例是在类加载时调用 `create`。

---

## 10. 验证记录

下面命令全部在本机 PowerShell 5.1 下实际运行过，本文档的所有签名、常量值与行为描述
都来自它们的输出。

### 10.1 环境变量

```powershell
$jd   = "C:\Users\TW2NTY_NIN9\.jdks\temurin-17\jdk-17.0.20.1+1\bin\javap.exe"
$jar  = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\com\polarice3\goety\2.5.57.3_mapped_parchment_2023.09.03-1.20.1\goety-2.5.57.3_mapped_parchment_2023.09.03-1.20.1.jar"
$mjar = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\minecraft_user_repo\net\minecraftforge\forge\1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1\forge-1.20.1-47.4.23_mapped_parchment_2023.09.03-1.20.1.jar"
$cjar = "C:\Users\TW2NTY_NIN9\.gradle\caches\forge_gradle\deobf_dependencies\top\theillusivec4\curios\curios-forge\5.14.1+1.20.1_mapped_parchment_2023.09.03-1.20.1\curios-forge-5.14.1+1.20.1_mapped_parchment_2023.09.03-1.20.1.jar"
```

### 10.2 主类与注册表入口

```powershell
& $jd -cp $jar com.Polarice3.Goety.Goety
& $jd -cp $jar com.Polarice3.Goety.init.ModCreativeTab
& $jd -cp $jar com.Polarice3.Goety.init.ModAttributes
& $jd -cp $jar com.Polarice3.Goety.init.ModBanners
& $jd -cp $jar com.Polarice3.Goety.init.ModCauldronInteraction
& $jd -cp $jar com.Polarice3.Goety.init.ModDispenserRegister
& $jd -cp $jar com.Polarice3.Goety.init.ModKeybindings
& $jd -cp $jar com.Polarice3.Goety.init.InitEvents
& $jd -cp $jar com.Polarice3.Goety.common.items.ModItems
& $jd -cp $jar com.Polarice3.Goety.common.blocks.ModBlocks
& $jd -cp $jar com.Polarice3.Goety.common.entities.ModEntityType
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.ModBlockEntities
& $jd -cp $jar com.Polarice3.Goety.client.particles.ModParticleTypes

# 反编译出构造函数里到底注册了什么、ModItems.init() 调了什么
& $jd -c -p -cp $jar com.Polarice3.Goety.Goety
& $jd -c -p -cp $jar com.Polarice3.Goety.common.items.ModItems
& $jd -c -p -cp $jar com.Polarice3.Goety.init.ModCreativeTab
# 页签注册名 goety / goety_block / goety_focus / goety_servants
# 就是从最后那条 -c -p 输出的 ldc 指令里读出来的
```

### 10.3 ModTags（**含嵌套类**）

```powershell
# 外围类本身只有 init()
& $jd -cp $jar com.Polarice3.Goety.init.ModTags
# 嵌套类里才是真正的 TagKey 常量（PowerShell 里 $ 要转义或用拼接）
foreach ($n in @('Blocks','Items','EntityTypes','DamageTypes','Biomes','Structures',
                 'Effects','GameEvents','BannerPatterns','Paintings')) {
    $cn = 'com.Polarice3.Goety.init.ModTags' + [char]36 + $n
    "--- $n ---"
    & $jd -cp $jar $cn 2>&1 | Where-Object { $_ -match 'TagKey<' } |
        ForEach-Object { ($_.Trim() -replace '^public static final net.minecraft.tags.TagKey<[^>]*> ','') -replace ';','' }
}

# 或者直接看反编译源码（能看到 init() 依次调用了 10 个子类的 init()）
.\tools\find-api.ps1 -Source 'com.Polarice3.Goety.init.ModTags$Items'
```

### 10.4 `api.*` 全包

```powershell
# 权威做法：直接枚举 jar 里的 class 条目（别用任何预先导出的清单文件）
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($jar)
$cls = $z.Entries | Where-Object { $_.FullName -like '*.class' } |
       ForEach-Object { $_.FullName -replace '\.class$','' -replace '/','.' }
"class 条目总数 : " + $cls.Count                                   # 3125
"api 包类数     : " + ($cls | Where-Object { $_ -like 'com.Polarice3.Goety.api.*' }).Count   # 50

& $jd -cp $jar com.Polarice3.Goety.api.items.magic.IFocus
& $jd -cp $jar com.Polarice3.Goety.api.items.magic.ISoulContainer
& $jd -cp $jar com.Polarice3.Goety.api.items.magic.ITotem
& $jd -cp $jar com.Polarice3.Goety.api.items.magic.IWand
& $jd -cp $jar com.Polarice3.Goety.api.items.IPersist
& $jd -cp $jar com.Polarice3.Goety.api.items.IPersistDecorator
& $jd -cp $jar com.Polarice3.Goety.api.items.ISoulRepair
& $jd -cp $jar com.Polarice3.Goety.api.items.armor.ISoulDiscount
& $jd -cp $jar com.Polarice3.Goety.api.items.curios.IActivatable
& $jd -cp $jar com.Polarice3.Goety.api.magic.ISpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.IChargingSpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.ISummonSpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.IBlockSpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.IBreathingSpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.ITouchSpell
& $jd -cp $jar com.Polarice3.Goety.api.magic.IMold
& $jd -cp $jar com.Polarice3.Goety.api.magic.GolemType
& $jd -cp $jar com.Polarice3.Goety.api.magic.SpellType
& $jd -cp $jar com.Polarice3.Goety.api.magic.SpellPoses
& $jd -cp $jar com.Polarice3.Goety.api.client.SpellArmPose
& $jd -cp $jar com.Polarice3.Goety.api.entities.IOwned
& $jd -cp $jar com.Polarice3.Goety.api.entities.IGolem
& $jd -cp $jar com.Polarice3.Goety.api.entities.IHeretic
& $jd -cp $jar com.Polarice3.Goety.api.entities.ICharger
& $jd -cp $jar com.Polarice3.Goety.api.entities.IRM
& $jd -cp $jar com.Polarice3.Goety.api.entities.IHiding
& $jd -cp $jar com.Polarice3.Goety.api.entities.IHungry
& $jd -cp $jar com.Polarice3.Goety.api.entities.ITrainable
& $jd -cp $jar com.Polarice3.Goety.api.entities.IMobCrafter
& $jd -cp $jar com.Polarice3.Goety.api.entities.IChunkLoader
& $jd -cp $jar com.Polarice3.Goety.api.entities.ICustomAttributes
& $jd -cp $jar com.Polarice3.Goety.api.entities.IAutoRideable
& $jd -cp $jar com.Polarice3.Goety.api.entities.IBreathing
& $jd -cp $jar com.Polarice3.Goety.api.entities.ISpellEntity
& $jd -cp $jar com.Polarice3.Goety.api.entities.ally.IServant
& $jd -cp $jar com.Polarice3.Goety.api.entities.ally.IAquaServant
& $jd -cp $jar com.Polarice3.Goety.api.entities.ally.illager.IllagerType
& $jd -cp $jar com.Polarice3.Goety.api.entities.ally.illager.ILooter
& $jd -cp $jar com.Polarice3.Goety.api.entities.ally.illager.ITrainIllager
& $jd -cp $jar com.Polarice3.Goety.api.blocks.IEnchanteableBlock
& $jd -cp $jar com.Polarice3.Goety.api.blocks.IEnchantedBlock
& $jd -cp $jar com.Polarice3.Goety.api.blocks.ISeat
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.IBarrack
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.IOwnedBlock
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.ISoulCandle
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.ITrainingBlock
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.IWaystoneBlock
& $jd -cp $jar com.Polarice3.Goety.api.blocks.entities.IWindPowered
& $jd -cp $jar com.Polarice3.Goety.api.ritual.IRitualType
& $jd -cp $jar com.Polarice3.Goety.api.ritual.RitualType

# 这两个"不存在"的反证
& $jd -cp $jar com.Polarice3.Goety.api.entities.IServant      # 报：找不到类
& $jd -cp $jar com.Polarice3.Goety.api.entities.IChargeable   # 报：找不到类

# NBT 键的字符串值（javap -constants 会展开 static final String）
& $jd -constants -cp $jar com.Polarice3.Goety.api.items.magic.ISoulContainer   # "Souls"
& $jd -constants -cp $jar com.Polarice3.Goety.api.items.magic.ITotem           # "Max Souls"
& $jd -constants -cp $jar com.Polarice3.Goety.api.items.magic.IWand
```

### 10.5 行为 / 常量值 —— 用 `-Source` 反编译

```powershell
# ISpell 的默认值、SoulCalculation 全算法、SpellArmPose 是空枚举
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.magic.ISpell
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.client.SpellArmPose
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.magic.ISummonSpell
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.items.magic.IWand
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.items.magic.ISoulContainer
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.items.magic.ITotem
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.entities.IOwned
.\tools\find-api.ps1 -Source com.Polarice3.Goety.api.entities.ally.IServant

# 实现类
.\tools\find-api.ps1 -Source com.Polarice3.Goety.Goety
.\tools\find-api.ps1 -Source com.Polarice3.Goety.init.ModCreativeTab
.\tools\find-api.ps1 -Source com.Polarice3.Goety.init.ModTags
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.items.ModItems
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.items.magic.MagicFocus
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.items.handler.SoulUsingItemHandler
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.magic.Spell
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.magic.SummonSpell
.\tools\find-api.ps1 -Source com.Polarice3.Goety.common.magic.spells.VexSpell

# 长输出分页
.\tools\find-api.ps1 -Source com.Polarice3.Goety.Goety | Select-Object -First 80
```

> **注意**：传给 `-Source` 的类名里如果含 `$`（嵌套类），
> 在 PowerShell 里必须写成单引号（`'com.Foo$Bar'`）或用 `[char]36` 拼接，
> 否则 `$Bar` 会被当变量展开成空串。

### 10.6 法术类、物品类、方块类、仆从类

```powershell
& $jd -cp $jar com.Polarice3.Goety.common.magic.ChargingSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.EverChargeSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.BreathingSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.BlockSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.TouchSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.SpellStat
& $jd -cp $jar com.Polarice3.Goety.common.magic.spells.SoulBoltSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.spells.TeethSpell
& $jd -cp $jar com.Polarice3.Goety.common.magic.spells.IgniteSpell

& $jd -cp $jar com.Polarice3.Goety.common.items.ItemBase
& $jd -cp $jar com.Polarice3.Goety.common.items.magic.DarkWand
& $jd -cp $jar com.Polarice3.Goety.common.items.magic.DarkStaff
& $jd -cp $jar com.Polarice3.Goety.common.items.magic.TotemOfSouls
& $jd -cp $jar com.Polarice3.Goety.common.items.magic.SoulItem
& $jd -cp $jar com.Polarice3.Goety.common.items.magic.FocusBag
& $jd -cp $jar com.Polarice3.Goety.common.items.curios.SingleStackItem
& $jd -cp $jar com.Polarice3.Goety.common.items.WaystoneItem
& $jd -cp $jar com.Polarice3.Goety.common.items.ModSpawnEggItem

& $jd -cp $jar com.Polarice3.Goety.utils.WandUtil
& $jd -cp $jar com.Polarice3.Goety.utils.CuriosFinder
& $jd -cp $jar com.Polarice3.Goety.utils.SEHelper
& $jd -cp $jar com.Polarice3.Goety.utils.TotemFinder
& $jd -cp $jar com.Polarice3.Goety.utils.ServantUtil
& $jd -cp $jar com.Polarice3.Goety.utils.MobUtil
& $jd -cp $jar com.Polarice3.Goety.utils.ColorUtil
& $jd -cp $jar com.Polarice3.Goety.utils.ConstantPaths
& $jd -cp $jar com.Polarice3.Goety.utils.ModDamageSource
& $jd -cp $jar com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy
& $jd -p -cp $jar com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown
& $jd -p -cp $jar com.Polarice3.Goety.config.SpellConfig

& $jd -cp $jar com.Polarice3.Goety.common.entities.neutral.Owned
& $jd -cp $jar com.Polarice3.Goety.common.entities.ally.Summoned
& $jd -cp $jar com.Polarice3.Goety.common.entities.ally.golem.AbstractGolemServant
& $jd -cp $jar com.Polarice3.Goety.common.entities.ally.illager.AbstractIllagerServant
& $jd -cp $jar com.Polarice3.Goety.common.entities.ally.spider.AbstractSpiderServant

& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.ChunkLoadBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.ModBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.OwnedBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.BarracksBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.TrainingBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.SaveBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.SoulCandlestickBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.entities.WindBlowerBlockEntity
& $jd -cp $jar com.Polarice3.Goety.common.blocks.EnchanteableBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.BarracksBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.TrainingBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.ThroneBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.RoyalThroneBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.WindBlowerBlock
& $jd -cp $jar com.Polarice3.Goety.common.blocks.SoulCandlestickBlock
```

**"哪个方块实体实现了哪个接口"** 是用这一条循环一次性扫出来的（比逐个猜快得多）：

```powershell
$be = $cls | Where-Object { $_ -like 'com.Polarice3.Goety.common.blocks.entities.*' }
$bl = $cls | Where-Object { $_ -like 'com.Polarice3.Goety.common.blocks.*' -and $_ -notlike '*entities*' }
$res = foreach ($c in ($be + $bl)) {
    & $jd -cp $jar $c 2>$null | Where-Object { $_ -match '^(public|abstract|final).*(class|interface|enum) ' }
}
$res | Where-Object { $_ -match 'IOwnedBlock|IEnchanteableBlock|IEnchantedBlock|ISeat|IBarrack|ISoulCandle|ITrainingBlock|IWaystoneBlock|IWindPowered' } | Sort-Object -Unique
```

### 10.7 Forge / Curios

```powershell
& $jd -cp $mjar net.minecraftforge.event.BuildCreativeModeTabContentsEvent
& $jd -cp $mjar net.minecraft.world.item.CreativeModeTab
& $jd -cp $mjar net.minecraftforge.registries.RegistryObject        # getKey() / getId() 在这里确认
& $jd -cp $mjar net.minecraftforge.registries.DeferredRegister      # createTagKey(...) 在这里确认
& $jd -cp $cjar top.theillusivec4.curios.api.type.capability.ICurioItem
& $jd -cp $cjar top.theillusivec4.curios.api.SlotContext
& $jd -cp $cjar top.theillusivec4.curios.api.CuriosApi
```

### 10.8 数据文件（标签 / 伤害类型 / curios）

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($jar)
$z.Entries | Where-Object { $_.FullName -like 'data/goety/tags/*' } | Select-Object -ExpandProperty FullName | Sort-Object
$z.Entries | Where-Object { $_.FullName -like 'data/curios/*' }     | Select-Object -ExpandProperty FullName | Sort-Object
$z.Entries | Where-Object { $_.FullName -like 'data/forge/*' }      | Select-Object -ExpandProperty FullName | Sort-Object

# 读某个 JSON 的内容（例：focus 标签全名单 / 伤害类型定义）
$e = $z.Entries | Where-Object { $_.FullName -eq 'data/goety/tags/items/focuses.json' }
$sr = New-Object System.IO.StreamReader($e.Open()); $sr.ReadToEnd(); $sr.Close()
```

### 10.9 "Goety 里没有 Create 代码"的证据

```powershell
($cls | Where-Object { $_ -match 'simibubi|create' } | Measure-Object).Count   # → 0
```
