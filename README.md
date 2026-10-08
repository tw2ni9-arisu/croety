# Croety

Croety 是 Minecraft 1.21.1 / NeoForge 的 Create × Goety 联动模组，modid 为 `croety`。

## 当前内容

- `croety:soul_energy_orb`：液态灵魂从动力管道开口排出时生成；拾取后补充 Goety 灵魂。
- `croety:fluid_soul` 与 `croety:fluid_soul_bucket`：液态灵魂流体、桶、Create 加工配方，以及对 Goety 图腾和灵魂方舟的流体交互。
- `croety:waving_focus`：涌动聚晶；施法召唤 `croety:soul_motor`。
- `croety:soul_motor`：提供 128 RPM、64 SU/RPM 的动力，法术召唤的马达有 10 分钟寿命。

涌动聚晶的稳定注册 ID 是 `croety:waving_focus`。萃魂池 `croety:soul_drain` 和动力诅咒注入器 `croety:mechanical_cursed_infuser` 尚未实现。

## 版本与依赖

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.234 |
| Java | 21 |
| Create | 6.0.10（开发制品 `6.0.10-281`） |
| Goety | 3.2.0 |
| Curios | 9.5.1+1.21.1 |
| Patchouli | 1.21.1-93-NEOFORGE |

开发运行还固定使用 Flywheel 1.0.6、Ponder 1.0.82+mc1.21.1、Vanillin 1.1.3-41 和 Registrate MC1.21-1.3.0+67。依赖来源及坐标见 [`reference/SOURCES.md`](reference/SOURCES.md)。

## 构建与运行

此机准备的 JDK 21 位于 `.local/jdk21/jdk-21.0.12.1+1`；`.local` 被 Git 忽略，fresh checkout 不会包含它。换机器时，把下面的 `$jdkHome` 改为该机器上任意已安装的 JDK 21 根目录。在项目目录的 PowerShell 中设置当前终端环境即可，不会修改系统环境变量：

```powershell
$jdkHome = (Resolve-Path '.local/jdk21/jdk-21.0.12.1+1').Path
$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$env:PATH"
java -version
javac -version
```

此机还需把 Java 临时目录指向工作区，避免 Java 21 默认临时路径导致 Gradle 启动失败。下面设置只作用于当前 PowerShell 进程；每个新终端都要重新执行：

```powershell
New-Item -ItemType Directory -Force .local/javatmp | Out-Null
$taskTemp = (Resolve-Path .local/javatmp).Path.Replace('\', '/')
$env:JAVA_TOOL_OPTIONS = "-Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8 -Djdk.net.unixdomain.tmpdir=$taskTemp"
```

临时目录位于 `.local`，执行 `gradlew clean` 时也会保留。

`libs/maven` 同样由 Git 忽略。fresh checkout 后，首次构建前先运行一次脚本，以恢复并校验 Goety 本地 Maven 制品：

```powershell
.\tools\fetch-deps.ps1
```

确认 `java -version` 和 `javac -version` 显示 Java 21、临时目录变量已设置，并完成首次依赖恢复后运行：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物为 `build/libs/croety-1.0.0.jar`。首次运行专用服务器时，按服务器提示接受 EULA。详细玩法和本次迁移验证状态见 [`docs/demo.md`](docs/demo.md)。

1.21.1 数据资源使用单数目录：配方在 `src/main/resources/data/croety/recipe/`，物品标签在 `src/main/resources/data/croety/tags/item/`，结构模板在 `src/main/resources/data/croety/structure/`。

旧 Forge 1.20.1 资料已归档：[Forge 指南](docs/history/forge-1.20.1/ai/forge-1.20.1.md)、[Create 6.0.8 指南](docs/history/forge-1.20.1/ai/create-6.0.8.md)、[Goety 2.5.57.3 指南](docs/history/forge-1.20.1/ai/goety-2.5.57.3.md)、[Flywheel / Ponder 指南](docs/history/forge-1.20.1/ai/flywheel-ponder.md)。它们只作历史背景参考；当前依赖和开发运行方式以本仓库的 1.21.1 配置及 [`docs/ai/neoforge-1.21.1.md`](docs/ai/neoforge-1.21.1.md) 为准。历史验证记录见 [`docs/history/forge-1.20.1/README.md`](docs/history/forge-1.20.1/README.md)。

## 许可

MIT
