# Croety 1.21.1 NeoForge 依赖源码与制品

核验日期：2026-10-08。此目录只保留依赖源码、发行制品和来源记录；没有改动产品代码或构建文件。

## 源码仓库与锁定版本

| 依赖 | 仓库与引用 | SHA / 版本 | 与目标发行是否对应 |
|---|---|---|---|
| Goety | [Vivideru/Goety-3](https://github.com/Vivideru/Goety-3)，用户确认 Vivideru 是 1.21.1 NeoForge 版的授权维护者；克隆默认分支 main | HEAD 2f42435123d2e781107f41a14dadd0d59cbd64ea；gradle.properties 声明 mod_version=3.2.0 | 版本号、MC、NeoForge 版本和 Windy Update 发行说明对应；仓库没有版本 tag，且制品没有 Git 提交号，见下文的来源限制 |
| Curios API | [TheIllusiveC4/Curios](https://github.com/TheIllusiveC4/Curios)，分支 1.21.1 | 6b122c8a7e2b514fd94197459ade11f19a0bfb09；version=9.5.1+1.21.1 | 源码的版本属性与 1.21.1 NeoForge 制品版本相同 |
| Patchouli | [VazkiiMods/Patchouli](https://github.com/VazkiiMods/Patchouli)，tag release-1.21.1-93 | tag commit 0c2fdffc7e611a828007cecae7bfc3d7b312040a；mc_version=1.21.1，build_number=93 | tag 和 NeoForge 制品 1.21.1-93-NEOFORGE 对应 |
| Create | [Creators-of-Create/Create](https://github.com/Creators-of-Create/Create)，tag mc1.21.1-6.0.10 | ac0c444d9828da3453ae8cc65338e8de063286fb；mod_version=6.0.10，Minecraft=1.21.1 | 源码 tag 对应 6.0.10 发布线；Maven 制品使用 build number 后缀 6.0.10-281 |

Goety-3 仓库目前只有 main 分支，没有 Git tag，也没有 GitHub release。该分支的提交信息为 Windy Update，提交时间 2026-10-03 11:01:16 +02:00。仓库 gradle.properties 写明 Minecraft 1.21.1、NeoForge 21.1.234、Java 21、Curios 9.5.1、Patchouli 93-NEOFORGE。仓库版本字段是 3.2.0，并非 3.2.00；它与发行 JAR 的 Implementation-Version=3.2.0 一致，无需用尾零归一化解释。

Goety 3.2.0 的 CurseForge 文件页和 Modrinth 版本页都将源码链接保留为 Polarice3/Goety-2；发行 JAR 的 displayURL 也仍是该旧仓库。它们是过时的源码链接：Polarice3/Goety-2 只有 1.19 和 1.20 分支、没有 tags，其 1.20 分支是 Goety 2.5.58.4 / Minecraft 1.20.1 Forge。Polarice3 名下的 8 个仓库是 Goety-2、Goety_Cataclysm、ClangingHowl、Goety-and-Spillage、PolarsMadTweaks、TallyMaster、Goety 和 FireNBlood；列表里没有 Goety-3 或 NeoForge 专仓。按用户明确提供的授权维护来源，当前 Goety 1.21.1 API 以 Vivideru/Goety-3 为准，不以 1.20 源码代替。

来源与制品的版本和发行主题相符：Goety-3 当前提交写着 3.2.0 / Windy Update，Modrinth 版本号为 3.2.0，CurseForge 文件也叫 Goety - 3.2.0，JAR Implementation-Version 同样为 3.2.0。当前 main 提交时间晚于 JAR 中的 Implementation-Timestamp（2026-10-03 09:15:38 +02:00）；公开仓库没有 tag，JAR manifest 也没有 Git hash。因此公开证据能确认版本线相符，但不能证明此 JAR 精确由 commit 2f424351 构建。

## Goety 3.2.0 发行记录与制品

- [CurseForge Goety 3.2.0](https://www.curseforge.com/minecraft/mc-mods/goety/files/9044971)：file ID 9044971，Minecraft 1.21.1，NeoForge，上传者 Vivideru，2026-10-03。
- [Modrinth Goety 3.2.0](https://modrinth.com/mod/goety/version/8jB68vz3)：version ID 8jB68vz3，发行文件 goety-3.2.0.jar，版本元数据列出的加载器为 NeoForge，游戏版本为 1.21.1。发行说明名为 Windy Update，主要包含结构地形适配、风祠与 Hurricane 首领、Breeze 仆从及风系内容更新。
- 二进制已保存到 [artifacts/goety-3.2.0.jar](artifacts/goety-3.2.0.jar)，下载 URL 为 https://cdn.modrinth.com/data/4ZVIxU8x/versions/8jB68vz3/goety-3.2.0.jar。
- 文件大小 80,416,092 字节。Modrinth API 提供的 SHA-1 为 82a92e697bfb7d8c1fa359c1e6d01a900b33c9e4、SHA-512 为 bc2277f9dd22114d1aa8900a1ad145bd5cff4b935977a50d5bd8e6a38dc2ea9b2572f5ce535a1ee7e48240389c181743ecde4186d6dbd555ae12b34da8f0ce7f；本地 SHA-256 为 EDEEB623F626BC85BD26DF6458DBB440FD4B044F34F3DE78EE715CBC01E692D8。下载后和后续核验后哈希一致。
- JAR 的 NeoForge 元数据要求 Curios [9.5.1,)；没有 Patchouli 依赖条目。Modrinth 发行记录仍将 Curios 和 Patchouli 都列为 required，但没有固定具体 dependency version ID。Goety-3 的构建属性给出了对应版本：Curios 9.5.1、Patchouli 93-NEOFORGE；源码 build.gradle 将两者列为 runtimeOnly，并用 api classifier 做编译依赖。
- Goety-3 的构建设置为 Minecraft 版本范围 [1.21.1,1.22)、NeoForge 版本范围 [21.1,)，并用 Java 21 编译。发行 JAR 中对应的 loader、NeoForge 和 Minecraft 范围文本仍是未展开的属性占位符；不要从发行 JAR 推导更精确的最低版本。构建文件没有替换这些占位符。另一项解析器核验显示它们按 recommended-only 无约束范围处理，不会单独拒载，但也没有提供有效范围校验。组合目标采用 Goety 构建版本 NeoForge 21.1.234；Create 6.0.10 的构建版本为 21.1.219。
- JAR 中的 3,256 个 class 文件全部是 class-file major 65，即 Java 21 字节码。

## Maven 制品坐标

| 组件 | 坐标 | 核验结果 |
|---|---|---|
| Goety 公开发行 | curse.maven:goety-586095:9044971 | 对应 CurseForge file ID 9044971。源码 build.gradle 设置 group=com.Polarice3.Goety、version=3.2.0、archivesName=goety，并将 Maven publication 写到项目内 mcmodsrepo；没有声明公共 Maven 下载坐标 |
| Curios NeoForge | top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1 | 官方仓库版本属性匹配；runtime JAR 与 api classifier JAR 均从官方 Maven HEAD 核验为 200 |
| Curios API classifier | top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api | 官方 Maven 文件存在 |
| Patchouli NeoForge | vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE | tag、仓库 build_number 和 Maven 制品版本匹配；runtime JAR 与 api classifier JAR 均从官方 Maven HEAD 核验为 200 |
| Patchouli API classifier | vazkii.patchouli:Patchouli:1.21.1-93-NEOFORGE:api | 官方 Maven 文件存在 |
| Create 完整制品 | com.simibubi.create:create-1.21.1:6.0.10-281 | 官方 maven.createmod.net 元数据中没有无 build-number 的 6.0.10；6.0.10-281 的完整 JAR、POM 和 sources JAR 均存在 |
| Create slim 分类器 | com.simibubi.create:create-1.21.1:6.0.10-281:slim | 同版本 slim JAR 存在；因此 NeoForge 1.21.1 的 6.0.10 线仍发布 slim |

Create Maven 元数据最新版本在核验日为 6.0.11-319；6.0.10 线最高的 build number 是 281。Create tag 的 build.gradle 声明 NeoForge 21.1.219，并将 Registrate、Flywheel 和 Ponder 纳入 jarJar。Create 6.0.10-281 POM 声明的关键运行时库为：

- Registrate：com.tterrag.registrate:Registrate:MC1.21-1.3.0+67。
- Ponder：net.createmod.ponder:ponder-neoforge:[1.0.82+mc1.21.1,)。
- Flywheel：dev.engine-room.flywheel:flywheel-neoforge-1.21.1:[1.0.6,2.0)。
- Vanillin：dev.engine-room.vanillin:vanillin-neoforge-1.21.1:1.1.3-41。

同一 POM 还列 Curios 9.2.2+1.21.1；Goety 3.2.0 的构建和制品要求 Curios 至少 9.5.1，因此 Croety 组合依赖应使用 9.5.1+1.21.1。

## 其他仓库锁定记录

- Curios: https://github.com/TheIllusiveC4/Curios.git，分支 1.21.1，commit 6b122c8a7e2b514fd94197459ade11f19a0bfb09。gradle.properties：Java 21、MC 1.21.1、NeoForge 21.1.60、版本 9.5.1+1.21.1。
- Patchouli: https://github.com/VazkiiMods/Patchouli.git，tag release-1.21.1-93（annotated tag object 4420a5987765f910404bbf5e38f749ae73938ee6，peel 后 commit 0c2fdffc7e611a828007cecae7bfc3d7b312040a）。构建版本 1.21.1-93-NEOFORGE，Gradle 使用 Java 21 和 NeoForge 21.1.143。
- Create: https://github.com/Creators-of-Create/Create.git，tag mc1.21.1-6.0.10，commit ac0c444d9828da3453ae8cc65338e8de063286fb。gradle.properties：Minecraft 1.21.1，NeoForge 21.1.219，Flywheel 1.0.6，Ponder 1.0.82，Registrate MC1.21-1.3.0+67，Vanillin 1.1.3-41。

