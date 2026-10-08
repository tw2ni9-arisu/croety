# Croety 演示说明（Minecraft 1.21.1 / NeoForge）

本页描述当前 1.21.1 NeoForge 工作区。依赖版本、Java 21 的当前终端设置方法和基础命令也见 [README.md](../README.md)。旧的 1.20.1 Forge 资料仅供背景参考；旧版 `-Pdemo` 参数和 `run/saves/croety-demo` 等开发存档路径不属于当前构建配置。

## 安装与启动

目标环境为 Minecraft 1.21.1、NeoForge 21.1.234、Java 21、Create 6.0.10-281、Goety 3.2.0、Curios 9.5.1+1.21.1 和 Patchouli 1.21.1-93-NEOFORGE。完整开发依赖坐标见 [`reference/SOURCES.md`](../reference/SOURCES.md)。

先按 README 在当前 PowerShell 终端选择 Java 21，并设置工作区临时目录；fresh checkout 还需先执行 `tools/fetch-deps.ps1` 恢复 Goety 本地 Maven 制品。完成后从项目目录使用 Gradle Wrapper：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于 `build/libs/croety-1.0.0.jar`。资源按 Minecraft 1.21.1 的单数目录放置：配方在 `src/main/resources/data/croety/recipe/`，物品标签在 `src/main/resources/data/croety/tags/item/`，结构模板在 `src/main/resources/data/croety/structure/`。

## 新内容

| ID | 内容 |
|---|---|
| `croety:soul_motor` | 灵魂马达 |
| `croety:waving_focus` | 涌动聚晶 / Waving Focus |
| `croety:fluid_soul` | 液态灵魂 |
| `croety:fluid_soul_bucket` | 液态灵魂桶 |
| `croety:soul_energy_orb` | 灵魂能量球 |

聚晶的稳定注册 ID 是 `croety:waving_focus`。按住 Shift 查看 Goety 聚晶详情时，显示“召唤一个临时的灵魂马达”。

## 马达与聚晶

灵魂马达输出 128 RPM、64 SU/RPM，即 128 RPM 时提供 8192 SU；发出亮度 4 的光，可旋转、回收或破坏，均不掉落物品。直接从创造模式或指令放置的马达永久存在。

佩戴工程师护目镜看向马达，可在应力信息下查看剩余寿命（分:秒）；永久马达显示“永久”。召唤时出现灵魂火焰和灵魂粒子，到期或被新马达替换时散出灵魂粒子。

将涌动聚晶装入 Goety 法杖，瞄准方块施法可放置临时马达。原始消耗 1024 灵魂，瞬发，冷却 60 秒；Goety 自身的法术消耗修正仍生效。每名玩家最多三台临时马达，第四台立即替换最早的一台。临时马达经过 12000 游戏刻（正常 20 TPS 时 10 分钟）消失。期限和所属玩家保存到世界数据；到期或被淘汰的未加载马达在重新加载时清除且不再供能。

锻造仪式配方：

- 中心激活物品：大型水车。
- 四个周边材料：缠魂水槽、潮汐聚晶、风车轴承、风帆/框架/任意羊毛。
- 每秒消耗 256 灵魂，持续 16 秒；使用 Goety 原有锻造仪式的环境条件。

Create 的 `create:windmill_sails` 是方块标签，包含 16 色风帆、框架和全部羊毛。染色风帆对应白帆物品。Goety 仪式使用物品 Ingredient，因此配方引用对应的 `croety:waving_sails` 物品标签。

## 液态灵魂

1 mB 等于 1 灵魂能量。液态灵魂可装进 Create 流体储罐、工作盆和分液池，经过动力泵、流体管道运输，但不能放成世界流体方块。工作盆中有液态灵魂时，液面间歇冒出原版灵魂粒子；这一装饰效果只在客户端生成。

| 加工 | 输入 | 输出 | 加热 |
|---|---|---|---|
| 动力搅拌器 | 1 灵质 | 5 mB 液态灵魂 | 燃烧 |
| 动力搅拌器 | 1 灵魂沙 | 25 mB 液态灵魂 | 燃烧 |
| 动力搅拌器 | 25 mB 液态灵魂、4 绿宝石 | 4 灵魂绿宝石 | 无 |
| 动力搅拌器 | 25 mB 液态灵魂、2 金锭、2 铁锭 | 4 诅咒金属锭 | 燃烧 |

桶的交互：

- 液态灵魂桶提供流体桶能力；兼容的流体处理器可装入或排出 1000 mB。通过 Create 分液池排出后返还空桶；空间不足或流体不兼容时不执行。
- 当前没有单独实现玩家手持空桶右键流体容器取液的交互。
- 蹲下右键饮用，可为 Goety 当前使用的灵魂账户补充 1000 灵魂并留下空桶；需要有完整 1000 灵魂的剩余容量，以免浪费液体。
- 右键地面或发射器不能将其放成液体方块。

图腾通过普通分液池时只抽出池子能接收的灵魂：例如 10000 灵魂图腾经过剩余 300 mB 容量的池子，留下 9700 灵魂。根之图腾耗尽后消失；灵魂图腾耗尽后变成耗尽的灵魂图腾。原有其他物品数据保留。

将管道接到装有根之图腾或灵魂图腾的诅咒之笼，可为图腾等价充值。接到已绑定、玩家在线且仍连接该方舟的灵魂方舟，可为其账户等价充值。满容量、不兼容或未连接的目标不吞液；不支持向离线玩家写入账户。

## 灵魂球

动力泵将液态灵魂送出开放管口时，消耗的液量转为等价值灵魂球。停止动力或供液后停止产生。灵魂球使用经验球式运动、追踪与 11 档外观，呈蓝绿到浅蓝渐变。相邻同符号的灵魂球会合并为一颗总价值更高、外观更大的球；不要求原本价值相同，也不使用原版的随机分组限制。

玩家一次拾取整颗球的全部可接收能量；容量不足时，剩余能量留在原球中，不重新拆成小球。正值和负值球不互相抵消。旧版本存档中的 `Value × Count` 会合并为新格式的总价值。拾取补充 Goety 灵魂，不增加原版经验或进入背包。

球有 5 点生命值，可受环境伤害，阳光下燃烧，不能被玩家近战或弹射物直接命中。在已加载区块内经过 6000 游戏刻消失；卸载暂停实体寿命，合并保留更长剩余寿命。

```mcfunction
/give @s croety:waving_focus
/give @s croety:fluid_soul_bucket
/give @s croety:soul_motor
/summon croety:soul_energy_orb ~ ~1 ~ {Value:100}
/summon croety:soul_energy_orb ~ ~1 ~ {Value:-10}
```

负值球会扣除灵魂（最低到 0）；余额不足时，未扣除的负价值仍留在球中。没有灵魂容器或容量已满时，正值球保留；容量只能接受部分价值时，剩余价值继续作为球存在。

## 验证状态

本次迁移记录 [`build/migration-verified.log`](../build/migration-verified.log) 显示 `BUILD SUCCESSFUL`，44 项 GameTest 全部通过。专用服务端已启动至 `Done`，正在正常停止和保存。客户端仍待用户在本机实机验收；本记录不代表客户端验收已完成。

## 本版范围

未实现初版标记为占位的萃魂分液池 `croety:soul_drain`、动力诅咒注入器 `croety:mechanical_cursed_infuser`，以及依赖萃魂分液池的萃取/抽取玩法。
