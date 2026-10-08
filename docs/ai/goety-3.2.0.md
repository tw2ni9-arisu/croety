# Goety 3.2.0 NeoForge：Croety 所用核心接口

## 证据与使用边界

目标为 Minecraft 1.21.1 NeoForge 的官方 `goety-3.2.0.jar`，Manifest 的 `Implementation-Version` 为 `3.2.0`。用户原写法为 3.2.00，已确认使用对应官方发行和授权维护源码。

发行 JAR 在 `reference/artifacts/goety-3.2.0.jar`；SHA-1 为 `82a92e697bfb7d8c1fa359c1e6d01a900b33c9e4`，已与 Modrinth 发行元数据核对。当前源码依据是用户提供的 [Vivideru/Goety-3](https://github.com/Vivideru/Goety-3)，位于 `reference/goety/`。Goety-2 的 `1.20` 分支为历史版本，不能当作本版本源码。签名记录在 `reference/goety-3.2.0-api.txt`。源码 commit、发行 URL 和依赖锁定见 `reference/SOURCES.md`。

本文件的签名已用 Java 21 `javap` 核对，实现依据授权维护仓库源码，不再进行反编译。迁移代码已通过构建、44项GameTest和专用服务端启动，客户端用户验收另行记录，见 `docs/neoforge-verification.md`。

## 1. 图腾与 ItemStack 数据

根包仍是 `com.Polarice3.Goety`，大小写不可改。

```java
// com.Polarice3.Goety.api.items.magic.ITotem
int getMaxSouls();
static int currentSouls(ItemStack stack);
static int maximumSouls(ItemStack stack);
static void setSoulsamount(ItemStack stack, int souls);
static void setMaxSoulAmount(ItemStack stack, int souls);
static CompoundTag tag(ItemStack stack);
static void updateTag(ItemStack stack, Consumer<CompoundTag> updater);
```

- 写入方法真实名称是 **`setSoulsamount`**，其中 `amount` 的 `a` 小写；旧版 `setSoulsAmount` 不存在于此 ITotem。
- `ITotem` 在本版不继承 `ISoulContainer`，不能把两者的静态写入方法互换。`ISoulContainer.setSoulsAmount` 本身存在，但有 `instanceof ISoulContainer` 守卫。
- `ITotem.tag(stack)` 返回 `DataComponents.CUSTOM_DATA` 的 **副本**。读取可使用它，写入必须用 `ITotem.updateTag` / `setSoulsamount` 或 `CustomData.update`，修改副本不会写回物品。
- 键名仍是 `"Souls"` 和 `"Max Souls"`。没有最大容量键时使用实际图腾的 `getMaxSouls()`，不能把缺键读取出的 0 当作默认容量。
- `setTagTick` 会初始化缺键并限制余额，但模拟容量检查不能调用会修改物品的方法。
- `ModItems.TOTEM_OF_ROOTS`、`TOTEM_OF_SOULS`、`SPENT_TOTEM` 仍存在，类型改为 NeoForge `DeferredHolder`。
- 分液后变为耗尽图腾时，需要保留用户其他物品数据。1.21.1 数据不只在 CUSTOM_DATA 中，名字、附魔等也是组件，必须核对组件复制方式并以测试证明保留，不能只替换旧 `getTag/setTag`。

## 2. 玩家账户与方舟

```java
// com.Polarice3.Goety.utils.SEHelper
static ISoulEnergy getCapability(Player player);
static boolean getSEActive(Player player);
static int getSESouls(Player player);
static void setSESouls(Player player, int souls);
static BlockPos getArcaBlock(Player player);
static ResourceKey<Level> getArcaDimension(Player player);
static void sendSEUpdatePacket(Player player);

// com.Polarice3.Goety.utils.TotemFinder
static ItemStack FindTotem(Player player);
```

`SEHelper.getCapability` 的内部实现已经是 NeoForge 数据 attachment：`player.getData(SEProvider.CAPABILITY)`。Croety 继续走 SEHelper，不手动复刻 Goety 的数据注册和同步。

`FindTotem` 返回当前物品的引用；其本版实现会查询 Curios、副手和物品栏。不要自行改写选择顺序。物质转换依旧使用 Croety 的实际接受量计算，避免套用 Goety 的施法折扣。

`ArcaBlockEntity` 自身没有声明 `getPlayer`，但其父类 `OwnedBlockEntity` **仍提供 `public Player getPlayer()`**，不能只查子类就判定接口缺失。诅咒之笼仍提供 `getItem()`、`setItem(ItemStack)` 与 `markUpdated()`。

真实方块实体注册入口是 `com.Polarice3.Goety.common.blocks.entities.ModBlockEntities`，字段为 `ARCA` 和 `CURSED_CAGE`。NeoForge 的流体接收器应以这些实际类型通过 `RegisterCapabilitiesEvent` 注册；不要沿用旧的 `AttachCapabilitiesEvent` / `LazyOptional`。

## 3. 聚晶与法术

```java
// com.Polarice3.Goety.common.items.magic.MagicFocus
public MagicFocus(ISpell spell);

// com.Polarice3.Goety.common.magic.Spell
public void SpellResult(ServerLevel level, LivingEntity caster,
                        ItemStack staff, SpellStat stats);
public List<ResourceKey<Enchantment>> acceptedEnchantments();
```

法术仍通过 `MagicFocus` 持有 `ISpell` 对象，不需要新增法术注册表。`defaultSoulCost()`、`defaultCastDuration()`、`defaultSpellCooldown()` 保持 `int` 返回值。

`acceptedEnchantments` 的元素从旧版 `Enchantment` 改为 `ResourceKey<Enchantment>`，Croety 的空列表也必须改为匹配签名。授权源码的 `SpellType.java` 仍声明 `NONE("none")`，原法术类型可保留。

`IWand.getFocus(ItemStack)` 本版通过 `SoulUsingItemHandler.get(stack).getSlot()` 读取安装聚晶。现有 GameTest 已使用 `SoulUsingItemHandler.get(staff).insertItem(focus)`，该调用在目标源码仍存在；handler 本版通过 `DataComponents.CONTAINER` 写回。保留该测试路径，并核实 Goety 的物品能力已注册，继续测试原生法杖施法。

## 4. 仪式配方

目标 JAR 的 `common/crafting/RitualRecipe.java` 对应序列化器使用 `MapCodec` 与 `StreamCodec`。JSON 产物采用 `ItemStack.STRICT_CODEC`，要按 1.21.1 ItemStack 格式使用 `id` 等字段，不能保留旧 `result.item` 格式；维护仓库的该类用于核对当前实现。

本项目用到的真实字段仍为 `craftType`、`ritual_type`、`result`、`activation_item`、`ingredients`、`duration`、`soulCost`；新序列化器默认 `duration=30`、`soulCost=0`，因此 Croety 的 16 秒/256 灵魂必须继续显式写入。数据目录改为 `data/croety/recipe/`，物品标签改为 `tags/item/`。

读取配方列表和查找配方的 GameTest 要适配 `RecipeHolder` 和新 recipe input 接口；不能通过只验证 JSON 文本来替代实际仪式执行。

## 5. 发行包元数据

官方 JAR 明确要求 Curios `[9.5.1,)`。Modrinth 发行列 Curios 和 Patchouli 都为 required；本次参考版本采用 Curios `9.5.1+1.21.1` 与 Patchouli `1.21.1-93-NEOFORGE`。

Goety 的 `neoforge.mods.toml` 中 loader、NeoForge 与 Minecraft 三个 range 含未展开的属性占位符。已对照 FancyModLoader `1.21.1` 的读取路径，并用其 Maven Artifact 3.8.5 解析器实测：这些裸占位符解析为无约束的 recommended-only range，字段本身不造成解析拒载，但没有提供有效兼容范围校验。保留原始发行 JAR，实际是否能加载以 Croety 的新 `runServer/runClient` 为准；不从这些属性推导具体的兼容下限。
