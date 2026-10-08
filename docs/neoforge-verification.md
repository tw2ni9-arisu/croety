# Croety 1.21.1 NeoForge 验证记录

日期：2026-10-08。分支：`croety-1.21.1-neoforge`，旧版基线 `e305d91`。

## 已完成的构建与自动测试

- 环境：Minecraft 1.21.1、NeoForge 21.1.234、Java 21.0.12.1、Gradle 8.14.3、ModDevGradle 2.0.107。
- 依赖：Create 6.0.10-281、Goety 3.2.0、Curios 9.5.1+1.21.1、Patchouli 1.21.1-93-NEOFORGE。源码提交、下载 URL 与哈希见 `reference/SOURCES.md`。
- `gradlew --offline build runGameTestServer`：exit 0，`BUILD SUCCESSFUL in 33s`，`All 44 required tests passed`；完整日志 `build/migration-verified.log`。
- 原有42项测试未删除或放宽行为断言；新增2项覆盖耗尽图腾完整组件保留，以及图腾缺保存上限时按物品容量注液、模拟不写入。
- 测试实际运行了六方向和8192SU轴网络、永久/召唤上限与跨维度/磁盘重载、Goety原生法杖与锻造仪式、四条搅拌、桶与分液容量竞态、图腾/方舟受液、真实泵和开放管口、灵魂球正负值/合并/拾取/水浮/伤害/日照/寿命。

首轮44项运行有2项真实失败，日志 `build/migration-gametest-first.log`：耗尽图腾时丢名称组件、SavedData从磁盘立即读取失败。前者通过复制component patch并只移除灵魂字段修复；后者根据真实NeoForge源码确认保存为异步，在测试中等待IO完成后重读，生产保存逻辑保留平台异步实现。第二轮全部通过。

## 产物核验

- JAR：`build/libs/croety-1.0.0.jar`，核验时135658字节。
- SHA-256：`d54bd2644117d3015ecad0df2883c2386c78667dd9c38fd29e7370505167dd3d`。后续重建会改变Manifest时间戳，交付时重新记录最终hash。
- 36个Croety class，class-file major全部为65（Java21）。
- 含 `META-INF/neoforge.mods.toml`，不含旧 `META-INF/mods.toml`；Croety元数据无未展开属性。
- 5条配方位于新版 `data/croety/recipe`，测试结构与物品标签使用单数路径；没有旧路径资源文件。当前源目录尚有一个旧 `structures` 空目录，最终整理时移除。
- 16项正式贴图、模型与方块状态：JAR内数据与旧项目逐文件SHA-256一致；无example内容或旧Config类。

## 专用服务端

`gradlew --offline runServer` 已实际运行：`build/migration-server.log` 出现 `Done (1.085s)! For help, type "help"`，无Mixin apply failure。运行只绑定127.0.0.1。

验证后通过控制台 `stop` 正常保存和退出，日志记录全部维度chunks保存，Gradle exit0；已核对没有该目标目录的残留Java进程。

## 独立审查

- GPT-6-sol（high）Task1：构建、注册基础、资源codec和工具通过；3项Minor已由原Luna实现者修正并限定复核关闭。
- GPT-6-sol（high）Task2/3：核心马达/法术/存档及流体/组件接口Spec与Quality通过，没有阻塞项。
- GPT-6-sol（high）Task4：实体、renderer、客户端注册、VALUE首发同步和现有Mixin接口通过，没有阻塞项。
- GPT-6-sol（high）整分支终审：未发现Critical/Important问题，可进入客户端用户验收；三处准备阶段的旧文档状态已同步修正。
- 不能以服务端GameTest替代客户端画面、轴动画、流体纹理/粒子验收；这一阶段仍待用户确认。

## 第一轮用户实机验收

客户端实际启动，日志 `build/migration-client.log` 记录 Sound engine started、Flywheel加载79个shader、Create加载58个train hat configurations。随后按用户要求暂停等待结果，未使用Computer Use。

用户于2026-10-08反馈：
- Croety创造栏中液态灵魂桶和灵魂马达为黑紫，涌动聚晶正常。
- 动力、法术、流体交互正常。
- 灵魂球生成、合并、拾取、白天燃烧正常，但画面不可见。
- 测试无明显卡顿。

模型错误在日志中有具体解析异常：Forge元素数据字段与流体桶loader不被NeoForge接受。修复只改两个JSON中的3处加载元数据，保留PNG与所有几何/UV/transforms。灵魂球对照目标Minecraft ExperienceOrbRenderer，删除旧版额外180°Y转向，保持Cull类型、顶点、配色和UV。

这些是用户实际验证后的必要平台格式修正；原JAR资产字节比对只证明素材原样复制，不能证明新平台能加载旧元数据。修复后的客户端视觉仍需第二轮用户确认，确认后才能最终整理和完成goal。
