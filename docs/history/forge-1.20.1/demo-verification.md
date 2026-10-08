# Croety demo 验证记录

## 基础版

2026-09-15：`build/demo-verified.log` 记录36项 GameTest 全部通过与 `BUILD SUCCESSFUL`。
自动测试覆盖：

- 马达6朝向、128RPM、8192SU实际轴网络、光照、扳手旋转与拆除无掉落。
- 第四台召唤马达淘汰第一台、永久马达保留、过期重新初始化不供能、跨维度淘汰、SavedData写盘并重新读取以及未加载位置的淘汰记录保存。
- 原生法杖装入聚晶、瞬发、灵魂扣除和冷却。
- 原生锻造仪式实际完成：16秒消耗4096灵魂、消耗四项材料并产出 `croety:waving_focus`。
- Create风帆方块标签对应物品覆盖，包括白帆、框架、全部羊毛。
- 四条搅拌配方实际由马达、齿轮、动力搅拌器、工作盆加工，验证加热限制、输入消耗及产物数量。
- 整桶往返、禁止世界放置、饮用充值与空桶返还、容量不足不吞桶。
- 根图腾部分分液和耗尽消失、灵魂图腾耗尽形态、NBT保留、异种流体/满容量时不消耗。
- 诅咒之笼/已连接灵魂方舟只接受剩余容量，模拟操作无副作用。
- 实际泵和管网有动力排出灵魂球、断动力停止；开放管口模拟/执行能量守恒。
- 灵魂球负值、存档、合并寿命、原版经验不增加、水浮、环境伤害、日晒燃烧与6000刻到期。

专用服务端 `build/demo-server-final.log` 出现 `Done (1.888s)!`；测试完成后通过 `stop` 保存退出。
客户端日志记录 `Sound engine started`、Flywheel `Loaded 77 shader sources`、Create `Loaded 56 train hat configurations`。
初次客户端发现液态灵魂贴图未加入方块纹理图集，已移至 `textures/block` 并验证桶显示恢复正常。

用户随后代为实机确认：

- 马达模型贴图和旋转轴正常，能连接轴/齿轮，128RPM和8192SU正确。
- 聚晶可装入魔杖、可召唤马达、仪式合成正常、名称正确。
- 液态灵魂储罐贴图正常、管口出现蓝绿球、动力泵可向灵魂方舟充值。
- 灵魂球白天燃烧、夜晚不燃烧；拾取增加灵魂且不增加原版经验。
- 全程无崩溃，各项功能符合预期。

这份用户实测反馈作为基础版客户端验收证据，取代重复自动操作界面。

## 实测反馈后的三项改进

新增范围：聚合大球并整颗拾取、护目镜倒计时、马达与工作盆灵魂粒子。

`build/demo-polish-red.log` 记录修改前的合并/整颗拾取需求失败。
`build/demo-polish-tests.log` 记录修改后39项 GameTest全部通过、构建成功。
随后终审补充负值球余量和降低容量配置两项边界用例，先在 `build/demo-polish-boundary-red.log` 重现失败，修复后 `build/demo-polish-verified.log` 记录41项全部通过与 `BUILD SUCCESSFUL`。最新JAR以这轮构建为准。
新增和更新的断言证明：

- 7与17的球合并成价值24、外观档位3的一颗球；仅能接受10时保留价值14的同一颗球。
- 相同位置连续排出200份1灵魂时只保留一颗价值200的球。
- 旧格式 `Value:7, Count:2` 迁移为价值14的球，保存重载不丢失，单次拾取获得14。
- 正负球不互相抵消，合并继承较长剩余寿命。
- 永久马达的寿命文本、2500刻显示2:05、最后1刻显示0:01。
- 负值球只扣到余额0时保留剩余负价值；后续有灵魂再精确扣除余量。
- 余额高于调低后的配置上限时，拾取正球不扣余额、不增大球；负向交易只扣请求量。

马达召唤数据通过 `notifyUpdate()` 同步，客户端按游戏时间计算显示；召唤/消散使用原版粒子包。
工作盆粒子使用客户端专用Mixin，不在服务端加载或扫描世界。
用户已在后续实机验收中确认：粒子效果正常、马达寿命显示无误、灵魂球合并无误。涌动聚晶最终贴图也由用户定稿并接入。
新增功能独立审查所报的两个能量边界已修复，并通过限定范围复审；没有剩余代码阻塞项。

## 修复的实质性问题

- 自定义桶子类未继承Forge默认流体能力：显式提供 `FluidBucketWrapper`。
- 满容量边界饮用浪费灵魂：要求完整接收1000才消耗桶。
- 日晒每刻重设燃烧计时导致伤害过快：已经燃烧时不重复点燃。
- 分液池提交前玩家恰好灌满可吞桶：提交当刻再次要求完整容量。
- Goety诅咒金属字段名与注册ID不同：产物使用真实 `goety:cursed_ingot`，实际加工测试通过。

## 构建环境备注

后续内容清理：移除MDK的 `example_item`、`example_block`、`example_tab` 注册、资源和示例配置；创造栏名称统一为“Croety”；补充 Goety 原生 Shift 详情键 `item.croety.waving_focus.info`。
`build/content-cleanup-red.log` 已重现示例内容仍注册的问题，修正后 `build/content-cleanup-verified.log` 记录42项 GameTest全部通过与构建成功。已核验最终JAR无示例资源/旧配置类，且打包后的中英文语言文件包含正确页签名称和详情文本。
`build/content-cleanup-server.log` 记录普通服务端启动成功：`Done (2.739s)!`，随后通过 `stop` 正常保存退出。旧测试存档报告的缺失映射仅为本次明确移除的 `croety:example_block` 和 `croety:example_item`。

JDK本地通信管道使用工作区 `build/javatmp`，避免系统短路径临时目录触发Windows AF_UNIX错误。
外网预检不稳定时使用已缓存依赖离线运行；`-Dnet.minecraftforge.gradle.check.certs=false` 仅跳过离线构建无须执行的远程预检，不更改依赖下载的TLS配置。
首次验收时存在的MDK示例方块/物品及其缺失贴图已在后续清理中移除；同时移除了示例标签页、配置和日志脚手架。

## 仓库整理验证（2026-10-08）

- 正式源码、资源、Gradle wrapper、依赖恢复脚本、玩法与API资料保留在仓库中；原始需求移到 `docs/design.md`。
- 素材草稿、内部计划、重复MDK说明和独立内层仓库完整保存在忽略的 `.local/archive/2026-10-08/`；内层仓库仍为原提交 `bdbd277`，工作树干净。
- 用户选择采用内层已有MIT许可证；根目录 `LICENSE` 与原文件一致，JAR的模组元数据声明MIT。MDK原始许可证和致谢原文保留于 `docs/third-party/forge-mdk/`。
- `build/repository-layout-build.log`：主工作区 `build` 成功。
- 从Git暂存区导出独立工作副本，不复制旧项目的build、运行目录和依赖目录。`build/repository-dependency-restore.log` 记录重新下载Goety并通过固定SHA1校验；`build/repository-clean-copy-build.log` 记录独立副本全部构建任务执行成功。该验证共用机器的Gradle用户缓存，并不证明首次下载所有Maven依赖的网络可用性。
- GitHub Actions的YAML已解析，构建步骤在本地核对；云端流程未执行。流水线只构建并保存运行产物，不包含Release发布动作。
- 已检查Git索引不包含 `.local`、build、run、依赖JAR、历史素材或嵌套仓库；Gradle wrapper具有执行权限，行尾规则区分文本、批处理和二进制资源。

此轮只整理文件、许可证、文档与构建辅助脚本，未变更玩法。42项游戏测试的既有验证记录见上文。
