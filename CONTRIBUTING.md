# 参与开发

## 本地环境

需要 Java 17 和 PowerShell 7。设置 `JAVA_HOME` 后执行：

```powershell
./tools/fetch-deps.ps1
./gradlew.bat build
```

依赖 JAR 和反编译缓存属于本地工作文件，不提交到仓库。

## 修改约定

- 修改围绕具体问题，避免混入无关重构。
- 注释和文档使用中文，代码标识符使用英文。
- Create、Goety、Minecraft、Forge API 以本地依赖为准；使用 `tools/find-api.ps1` 核实。
- 功能或缺陷修复应有可复现步骤；适合自动验证的行为使用现有 GameTest。
- 至少执行 `./gradlew.bat build`；运行时行为还需相应服务端或客户端验证。

服务端测试：

```powershell
./gradlew.bat runGameTestServer
```

测试生成的存档和日志位于忽略的运行目录。提交前检查 `git diff --check` 和 `git status`。

## 提交问题和改动

问题报告请注明 Minecraft、Forge、Create、Goety 和 Croety 版本，提供复现步骤、预期结果和实际结果。
崩溃问题请附日志或崩溃报告，并移除不相关的个人信息。

改动说明应写清问题、最终行为及已完成的验证。保留第三方来源说明和许可证。
