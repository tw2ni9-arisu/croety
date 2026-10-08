# Croety

Minecraft 1.20.1 / Forge 的 **Create × Goety 联动模组**。

目前实现灵魂马达、涌动聚晶（`croety:waving_focus`）、液态灵魂与桶、灵魂能量球，以及加工、仪式和流体管网联动。
完整玩法见 [使用说明](docs/demo.md)，验证记录见 [验证说明](docs/demo-verification.md)。
原始需求保存在 [设计文档](docs/design.md)，标记 `#占位` 的内容不属于当前版本。

## 玩家安装

在 Forge 1.20.1 环境中，把 Croety JAR 与下列依赖放进 `mods/`：

| 组件 | 当前验证版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.23 |
| Create | 6.0.8（开发坐标 6.0.8-291） |
| Goety | 2.5.57.3 |
| Curios | 5.14.1+1.20.1 |
| Patchouli（推荐，用于手册） | 1.20.1-85-FORGE |

Create 和 Goety 所需的前置组件也需按对应版本安装；JEI 为可选依赖。
当前仓库只有本地构建和 `v1.0.0` tag，没有远程发布的 Release。

## 开发环境与首次构建

需要 **Java 17**、**PowerShell 7**。Gradle 8.8 由仓库中的 wrapper 提供。

设置 `JAVA_HOME` 为自己的 Java 17 JDK：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

克隆到本机后，在仓库根目录执行：

```powershell
./tools/fetch-deps.ps1
./gradlew.bat build
```

Goety 没有本项目使用的公共 Maven 坐标；恢复脚本从固定的 Modrinth 地址取得版本2.5.57.3，
校验 SHA1 后放入本地 Maven 仓库。脚本同时取得 Create、Ponder、Flywheel 源码，便于查 API。
JAR 与源码缓存均不提交 Git。版本与依赖配置分别在 `gradle.properties` 和 `build.gradle`。

构建产物：`build/libs/croety-1.0.0.jar`。首次构建可能需要下载和处理依赖，耗时比增量构建长。

## 运行与测试

```powershell
./gradlew.bat runClient          # 开发客户端
./gradlew.bat runServer          # 开发专用服务端
./gradlew.bat runGameTestServer  # 自动游戏测试，完成后退出
./gradlew.bat runData            # 数据生成
```

开发依赖从 Gradle classpath 加载，不需要再复制到 `run/mods`。
服务端首次运行时按提示编辑 `run/eula.txt` 接受 Minecraft EULA，再重启。
测试和运行生成的配置、存档、日志留在本地，不提交仓库。

GitHub 构建检查见 [.github/workflows/build.yml](.github/workflows/build.yml)：
Windows runner 设置 Java 17，恢复依赖后执行构建并保存 JAR 作为运行产物。它不自动发布 Release。
云端流程尚未执行，本地验证结果见验证说明。

## 仓库结构

```text
.github/                 GitHub 构建检查
src/main/java/           模组代码与 GameTest
src/main/resources/      元数据、贴图、模型、语言、配方
src/generated/           数据生成资源
gradle/wrapper/          Gradle wrapper
tools/                   依赖恢复、真实 API 查询
docs/                    玩法、设计、验证与第三方声明
docs/ai/                 本地依赖 API 参考
AGENTS.md                AI 协作约定
CONTRIBUTING.md           开发与提交说明
CHANGELOG.md              项目更新记录
LICENSE / NOTICE.md       MIT 许可证与第三方来源
```

`build/`、`run/`、`.gradle/`、`libs/maven/`、`libs/sources/`、`release-staging/` 和 `.local/`
均为本地工作目录，已在 [.gitignore](.gitignore) 排除。
历史素材、内部计划及重复的内层仓库完整保存在 `.local/archive/2026-10-08/`。

## 常见构建问题

- Java版本不正确：检查 `JAVA_HOME`，不要依赖可能指向Java25的系统 `java` 命令。
- 找不到Goety：先执行 `./tools/fetch-deps.ps1`。
- Windows出现 `Unable to establish loopback connection`：可指定仓库内临时目录后重试：

```powershell
New-Item -ItemType Directory -Force build/javatmp | Out-Null
$taskTemp = (Resolve-Path build/javatmp).Path.Replace('\', '/')
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=$taskTemp"
./gradlew.bat build
```

Create开发运行必须保留 `build.gradle` 中的Mixin refmap重映射配置。
API查询使用 `./tools/find-api.ps1 -Class 完整类名` 或 `-Source 完整类名`，以本地依赖为准。

## 许可证

Croety采用 [MIT许可证](LICENSE)。第三方依赖和衍生素材的归属与声明见 [NOTICE.md](NOTICE.md)。
