<#
.SYNOPSIS
    Look up real API signatures AND source code for everything on this workspace's compile classpath.

.DESCRIPTION
    Minecraft, Forge, Create, Goety, Flywheel, Ponder, Registrate, Curios and Patchouli are already
    downloaded and deobfuscated into the Gradle caches. There is never a reason to guess an API from
    memory: use this script to print the actual signature, or the actual source.

    -Signature mode (-Class) runs javap against the exact jars Gradle puts on the compile classpath,
    already remapped to the Parchment/1.20.1 named mappings this project builds against. What you see
    is what javac will accept.

    -Source mode prints real Java source:
      1. if a sources jar is present in libs/sources, the original source is printed;
      2. otherwise the class is decompiled with ForgeFlower (cached under libs/sources/.decompiled).
    This is the fastest way to learn how Create or Goety actually implements something.

.PARAMETER Class
    Fully qualified class name, e.g. com.simibubi.create.content.kinetics.base.KineticBlockEntity
    Prints the javap signature. Use -Private to include private members.

.PARAMETER Source
    Fully qualified class name whose Java source you want.

.PARAMETER Search
    Substring (case-insensitive) matched against class names across every jar, e.g. "goggle" or
    "IServant". Prints the matching fully qualified names and which jar they came from.

.PARAMETER Jar
    Optional substring filter on the jar file name, e.g. -Jar goety.

.PARAMETER Private
    Pass -p to javap (show private members too).

.PARAMETER ListJars
    Print every jar that is searched.

.EXAMPLE
    .\tools\find-api.ps1 -Class com.simibubi.create.AllBlocks
    .\tools\find-api.ps1 -Search goggle -Jar create
    .\tools\find-api.ps1 -Source com.simibubi.create.content.kinetics.base.KineticBlockEntity
    .\tools\find-api.ps1 -Source com.Polarice3.Goety.api.magic.ISpell
    .\tools\find-api.ps1 -Class com.Polarice3.Goety.api.items.magic.IFocus -Private
#>
[CmdletBinding(DefaultParameterSetName = 'Class')]
param(
    [Parameter(ParameterSetName = 'Class', Position = 0)]
    [string] $Class,

    [Parameter(ParameterSetName = 'Source', Mandatory = $true)]
    [string] $Source,

    [Parameter(ParameterSetName = 'Search', Mandatory = $true)]
    [string] $Search,

    [Parameter(ParameterSetName = 'Jars', Mandatory = $true)]
    [switch] $ListJars,

    [string] $Jar,
    [switch] $Private
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root     = Split-Path -Parent $PSScriptRoot
$jdkBin   = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { $null }
$javap    = if ($jdkBin) { Join-Path $jdkBin 'javap.exe' } else { $null }
$java     = if ($jdkBin) { Join-Path $jdkBin 'java.exe' } else { $null }
if (-not $javap -or -not (Test-Path -LiteralPath $javap)) { $javap = (Get-Command javap.exe -ErrorAction SilentlyContinue).Source }
if (-not $java -or -not (Test-Path -LiteralPath $java)) { $java = (Get-Command java.exe -ErrorAction SilentlyContinue).Source }
if (-not $javap -or -not $java) { throw '未找到 JDK 工具，请设置 JAVA_HOME 为 Java 17 JDK。' }

$srcDir   = Join-Path $root 'libs\sources'
$decompDir = Join-Path $srcDir '.decompiled'

$fg = Join-Path $env:USERPROFILE '.gradle\caches\forge_gradle'
$m2 = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'

# 1. Minecraft + Forge (dev-mapped, what the project compiles against)
# 2. Every mod dependency ForgeGradle has deobfuscated
# 3. The FML / event bus jars, which live outside the Forge artifact
$jars = @()
$jars += Get-ChildItem (Join-Path $fg 'minecraft_user_repo') -Recurse -Filter '*_mapped_*.jar' -ErrorAction SilentlyContinue
$jars += Get-ChildItem (Join-Path $fg 'deobf_dependencies') -Recurse -Filter '*_mapped_*.jar' -ErrorAction SilentlyContinue
# Layout is files-2.1/<group as dirs>/<artifact>/<version>/<sha1>/<file>.jar, so scan recursively
# rather than assuming a fixed depth (eventbus, for example, sits one level deeper than expected).
foreach ($ga in @('net.minecraftforge\eventbus', 'net.minecraftforge\javafmllanguage', 'net.minecraftforge\fmlcore',
                  'net.minecraftforge\mclanguage', 'net.minecraftforge\fmlloader', 'org.spongepowered\mixin')) {
    $dir = Join-Path $m2 $ga
    if (Test-Path $dir) { $jars += Get-ChildItem $dir -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue }
}
# Newest version first: several Forge/MC builds can sit in the cache at once (e.g. a stale
# 47.1.47 next to the 47.4.23 this project pins), and the first occurrence wins in the index.
$jars = $jars | Where-Object { $_ -and $_.Name -notmatch '(-sources|-javadoc)\.jar$' } |
        Sort-Object FullName -Descending -Unique

if ($Jar) { $jars = $jars | Where-Object { $_.Name -like "*$Jar*" } }

if ($ListJars) {
    $jars | ForEach-Object { "{0,8:N1} MB  {1}" -f ($_.Length / 1MB), $_.FullName }
    return
}
if ($jars.Count -eq 0) { throw "No jars found. Has the project been built at least once? Run .\gradlew.bat build" }

# Collect the class index once (this is the slow part).
$index = @{}
foreach ($j in $jars) {
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($j.FullName)
        foreach ($e in $zip.Entries) {
            $n = $e.FullName
            if (-not $n.EndsWith('.class')) { continue }
            # keep nested classes (Foo\$Bar) but skip anonymous ones (Foo\$1)
            if ($n -match '\$\d') { continue }
            $fqn = $n.Substring(0, $n.Length - 6) -replace '/', '.'
            if (-not $index.ContainsKey($fqn)) { $index[$fqn] = $j.FullName }
        }
        $zip.Dispose()
    } catch { }
}

# Returns a hashtable @{ Text = <java source>; Origin = <description> } or $null.
# It must NOT emit anything else to the pipeline, otherwise the caller cannot tell
# "found" from "not found".
function Get-SourceInfo([string] $fqn) {
    $rel = ($fqn -replace '\.', '/') + '.java'

    # 1. original sources, if a sources jar is present
    if (Test-Path $srcDir) {
        foreach ($sj in (Get-ChildItem $srcDir -Filter '*.jar' -ErrorAction SilentlyContinue)) {
            try {
                $zip = [System.IO.Compression.ZipFile]::OpenRead($sj.FullName)
                $entry = $zip.Entries | Where-Object { $_.FullName -eq $rel } | Select-Object -First 1
                if ($entry) {
                    $sr = New-Object System.IO.StreamReader($entry.Open())
                    $txt = $sr.ReadToEnd()
                    $sr.Close()
                    $zip.Dispose()
                    return @{ Text = $txt; Origin = "original source from $($sj.Name)" }
                }
                $zip.Dispose()
            } catch { }
        }
    }

    if (-not $index.ContainsKey($fqn)) { return $null }
    $owner = $index[$fqn]

    # 2. cached decompilation
    $cacheFile = Join-Path $decompDir ((Split-Path $owner -Leaf) + '.' + ($fqn -replace '\.', '_') + '.java')
    if (Test-Path $cacheFile) {
        return @{ Text = (Get-Content $cacheFile -Raw); Origin = "decompiled (cached) from $(Split-Path $owner -Leaf)" }
    }

    # 3. decompile with ForgeFlower
    $flower = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches\forge_gradle') -Recurse -Filter 'forgeflower-*.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $flower -or -not (Test-Path $java)) { return $null }

    $tmpIn  = Join-Path ([System.IO.Path]::GetTempPath()) ("fa_in_" + [guid]::NewGuid().ToString('N'))
    $tmpOut = Join-Path ([System.IO.Path]::GetTempPath()) ("fa_out_" + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $tmpIn, $tmpOut | Out-Null
    try {
        $inner = ($fqn -replace '\.', '/') + '.class'
        $zip = [System.IO.Compression.ZipFile]::OpenRead($owner)
        foreach ($e in $zip.Entries) {
            if ($e.FullName -eq $inner) {
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($e, (Join-Path $tmpIn (($fqn -replace '\.', '_') + '.class')), $true)
            }
        }
        $zip.Dispose()
        & $java -jar $flower.FullName -dgs=1 $tmpIn $tmpOut *> $null
        $produced = Get-ChildItem $tmpOut -Recurse -Filter '*.java' -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $produced) { return $null }
        New-Item -ItemType Directory -Force -Path $decompDir | Out-Null
        Copy-Item $produced.FullName $cacheFile -Force
        return @{ Text = (Get-Content $cacheFile -Raw); Origin = "decompiled from $(Split-Path $owner -Leaf) with ForgeFlower" }
    } finally {
        Remove-Item $tmpIn, $tmpOut -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if ($Search) {
    $pattern = '*' + $Search + '*'
    $hits = $index.Keys | Where-Object { $_ -like $pattern } | Sort-Object
    if (-not $hits) { Write-Host "No class matching '$Search'." -ForegroundColor Yellow; return }
    Write-Host "$($hits.Count) match(es) for '$Search':" -ForegroundColor Cyan
    foreach ($h in $hits) {
        Write-Host ("  {0}" -f $h)
        Write-Host ("      in {0}" -f (Split-Path $index[$h] -Leaf)) -ForegroundColor DarkGray
    }
    return
}

if ($Source) {
    $info = Get-SourceInfo $Source
    if ($info) {
        Write-Host ("### {0}" -f $Source) -ForegroundColor Cyan
        Write-Host ("### {0}" -f $info.Origin) -ForegroundColor DarkGray
        Write-Host ''
        $info.Text
        return
    }
    Write-Host "No source available for '$Source' (not on the compile classpath, or decompilation failed)." -ForegroundColor Yellow
    $guess = $index.Keys | Where-Object { $_ -like ('*' + ($Source -split '\.')[-1] + '*') } | Sort-Object | Select-Object -First 10
    if ($guess) { Write-Host "Did you mean:"; $guess | ForEach-Object { Write-Host "  $_" } }
    return
}

if (-not $Class) { throw "Pass -Class <fqn>, -Source <fqn>, -Search <substring>, or -ListJars." }

if (-not $index.ContainsKey($Class)) {
    Write-Host "Class '$Class' is not on the compile classpath." -ForegroundColor Yellow
    $guess = $index.Keys | Where-Object { $_ -like ('*' + ($Class -split '\.')[-1] + '*') } | Sort-Object | Select-Object -First 10
    if ($guess) { Write-Host "Did you mean:"; $guess | ForEach-Object { Write-Host "  $_" } }
    return
}

$owner = $index[$Class]
Write-Host ("### {0}" -f $Class) -ForegroundColor Cyan
Write-Host ("### from {0}" -f (Split-Path $owner -Leaf)) -ForegroundColor DarkGray
Write-Host ''
if ($Private) { & $javap -p -cp $owner $Class } else { & $javap -cp $owner $Class }
