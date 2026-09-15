# Croety — Minecraft 1.20.1 Forge mod workspace

A ready-to-use Forge 1.20.1 development environment (standard Forge MDK layout) that
compiles against, and runs with, **Create 6.0.8** and **Goety 2.5.57.3** plus every
prerequisite they need.

## Documentation map

| File | For whom | Content |
|---|---|---|
| `README.md` | humans | this file — setup, build, run, adding dependencies |
| `AGENTS.md` | AI agents | project brief: environment, ground rules, pitfalls, verification checklist |
| `docs/ai/forge-1.20.1.md` | AI agents | Forge/Minecraft 1.20.1 patterns (registration, blocks, items, block entities, menus, config, datagen) |
| `docs/ai/create-6.0.8.md` | AI agents | Create 6.0.8 API reference (its official `api` packages, Registrate, kinetics, recipes, contraptions) |
| `docs/ai/goety-2.5.57.3.md` | AI agents | Goety 2.5.57.3 API reference (its `api` packages, items, spells, servants) |
| `docs/ai/flywheel-ponder.md` | AI agents | Flywheel client visuals and Ponder tutorial scenes |
| `tools/find-api.ps1` | both | print the real signature **or the real Java source** of any class on the compile classpath |
| `libs/sources/create-1.20.1-6.0.8-291/` | both | Create's complete Java source, extracted and greppable |

The `docs/ai/` references are generated from the actual jars with `javap`, so they describe the
API this project really compiles against rather than a remembered one.

## Verified versions

| Component | Version | Where it comes from |
|---|---|---|
| Minecraft | 1.20.1 | Forge userdev |
| Forge | 1.20.1-47.4.23 | `maven.minecraftforge.net` |
| ForgeGradle | 6.0.x | Gradle plugin portal |
| Gradle | 8.8 | wrapper |
| JDK | Temurin 17.0.20.1+1 | `%USERPROFILE%\.jdks\temurin-17` |
| Mappings | Parchment 2023.09.03-1.20.1 (on top of Mojang official) | `maven.parchmentmc.org` |
| Create | 6.0.8 (maven build 291, `:slim`) | `maven.createmod.net` |
| Flywheel | 1.0.5-264 | `maven.createmod.net` |
| Ponder | 1.0.91 (also bundles Catnip) | `maven.createmod.net` |
| Registrate | MC1.20-1.3.3 | `maven.tterrag.com` |
| MixinExtras | 0.4.1 | Maven Central |
| Goety | 2.5.57.3 | staged in `libs/maven` (no public Maven) |
| Curios API | 5.14.1+1.20.1 | `maven.theillusivec4.top` |
| Patchouli | 1.20.1-85-FORGE | `maven.blamejared.com` |
| JEI | 15.59.0.210 (optional) | `maven.blamejared.com` |

The Flywheel / Ponder / Registrate / MixinExtras versions are exactly the ones Create
6.0.8 bundles as jar-in-jar, so the dev classpath matches what players actually run.

## Java setup

Forge 1.20.1 needs **Java 17**. Set `JAVA_HOME` to a Java 17 JDK before invoking Gradle.
The repository deliberately leaves the path configurable so it works after cloning on another machine.

For one PowerShell session:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Build

```powershell
.\gradlew.bat build
```

The jar lands in `build/libs/croety-1.0.0.jar`.

## Run

```powershell
.\gradlew.bat runClient     # client with Create + Goety loaded from the classpath
.\gradlew.bat runServer     # dedicated server, prints "Done" when the world is up
.\gradlew.bat runData       # datagen
```

Mods declared as `implementation fg.deobf(...)` live on the run classpath directly, so
there is nothing to copy into `run/mods`.

## Layout

```
build.gradle                              ForgeGradle setup, repositories and mod dependencies
gradle.properties                         versions, mappings and the pinned JDK
src/main/java/com/croety/
    Croety.java                           @Mod entry point
    Config.java                           example ForgeConfigSpec
    integration/CreateIntegration.java    calls the Create 6.0.8 API
    integration/GoetyIntegration.java     calls the Goety 2.5.57.3 API
src/main/resources/META-INF/mods.toml     declares the create/goety/curios/patchouli deps
libs/maven/                               local Maven repo holding Goety (no upstream Maven)
tools/fetch-deps.ps1                      re-stages the locally hosted mod jars
tools/find-api.ps1                        prints the real signature of any class on the classpath
AGENTS.md                                 project brief auto-loaded by AI coding agents
docs/ai/                                  per-dependency API references written for AI agents
```

The two `integration` classes exist so a broken or missing dependency fails loudly at
startup instead of silently at first use: `Croety#commonSetup` calls both and logs what
it found.

## Adding another mod

Gradle-resolvable mods go in the `dependencies` block of `build.gradle`:

```groovy
implementation fg.deobf("<group>:<artifact>:<version>")
```

For mods that only exist as a jar (CurseForge/Modrinth releases), stage them under
`libs/maven/<group path>/<artifact>/<version>/` next to a minimal `.pom` and add the
coordinates the same way — `tools/fetch-deps.ps1` is a worked example.

## Known gotchas

**Create's mixin refmap must be remapped for the dev runtime.** Create ships its mixins
with an SRG refmap, but a ForgeGradle dev run uses named mappings, so Mixin fails during
startup with:

```
Mixin apply failed create.mixins.json:accessor.SystemReportAccessor -> net.minecraft.SystemReport
InvalidAccessorException: No candidates were found matching f_143509_:Ljava/lang/String;
```

`build.gradle` therefore sets these two properties on every run configuration (the
documented fix from the Create wiki):

```groovy
property 'mixin.env.remapRefMap', 'true'
property 'mixin.env.refMapRemappingFile', "${projectDir}/build/createSrgToMcp/output.srg"
```

A healthy startup logs one `Remapping refMap <mod>.refmap.json` line per mod that ships
mixins. If you add another mod with mixins and it crashes the same way, this is why.

**The maven build number is part of the version.** Create is published per-CI-build, so
`create_version` is `6.0.8-291`, not `6.0.8`. A new release jar corresponds to a
specific build number on the Maven.

## Network notes

This machine's egress blocks CRL/OCSP endpoints, which makes `curl.exe` and default
.NET requests fail with `CRYPT_E_REVOCATION_OFFLINE` / "The underlying connection was
closed". `tools/fetch-deps.ps1` disables revocation checking before downloading; Gradle
itself is unaffected because the JVM does not check revocation by default.

## Demo scope and release

The implemented demo covers the non-placeholder content from `croety.md`: the Soul Motor,
Waving Focus, Liquid Soul, Liquid Soul Bucket, Soul Energy Orb, Create processing recipes,
the Forge ritual, Goety soul storage, and the Create fluid network integration. The two
blocks marked `#占位` and their dependent extraction gameplay remain outside this first demo.

Detailed usage and verification records are in [docs/demo.md](docs/demo.md) and
[docs/demo-verification.md](docs/demo-verification.md). The final Waving Focus texture is
the approved 16×16 asset at `src/main/resources/assets/croety/textures/item/waving_focus.png`.

To build the release jar:

```powershell
.\gradlew.bat build
```

The artifact is `build/libs/croety-1.0.0.jar`.
