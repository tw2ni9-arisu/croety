<#
.SYNOPSIS
    查询实际 Gradle compileClasspath 上的签名与可用源码。
.DESCRIPTION
    先运行 .\gradlew.bat writeApiClasspath。签名与类搜索只读取 Gradle 实际解析的
    build/api-classpath.txt；源码优先使用 reference 源码 checkout、生成源码和对应 sources JAR。
    不会猜缓存 JAR，也不会反编译 Goety。
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

$root = Split-Path -Parent $PSScriptRoot
$classpathFile = Join-Path $root 'build\api-classpath.txt'
if (-not (Test-Path -LiteralPath $classpathFile)) {
    throw "没有 build/api-classpath.txt。先运行 .\gradlew.bat writeApiClasspath。"
}

$classpathEntries = Get-Content -LiteralPath $classpathFile |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$jars = @($classpathEntries | Where-Object { [IO.Path]::GetExtension($_) -ieq '.jar' })
if ($Jar) {
    $jars = @($jars | Where-Object { (Split-Path -Leaf $_) -like "*$Jar*" })
}

if ($ListJars) {
    foreach ($jarPath in $jars) {
        $item = Get-Item -LiteralPath $jarPath
        '{0,8:N1} MB  {1}' -f ($item.Length / 1MB), $item.FullName
    }
    return
}
if ($jars.Count -eq 0) {
    throw '实际 compileClasspath 中没有匹配的 JAR。'
}

$jdkBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { $null }
$javap = if ($jdkBin) { Join-Path $jdkBin 'javap.exe' } else { $null }
if (-not $javap -or -not (Test-Path -LiteralPath $javap)) {
    $javap = (Get-Command javap.exe -ErrorAction SilentlyContinue).Source
}
if (-not $javap) {
    throw '未找到 javap.exe；请在当前命令环境将 JAVA_HOME 指向 Java 21 JDK。'
}

$index = @{}
foreach ($jarPath in $jars) {
    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            foreach ($entry in $zip.Entries) {
                if (-not $entry.FullName.EndsWith('.class')) { continue }
                if ($entry.FullName -match '\$\d') { continue }
                $fqn = $entry.FullName.Substring(0, $entry.FullName.Length - 6) -replace '/', '.'
                if (-not $index.ContainsKey($fqn)) { $index[$fqn] = $jarPath }
            }
        } finally {
            $zip.Dispose()
        }
    } catch {
        Write-Warning "无法读取 compileClasspath JAR: $jarPath ($($_.Exception.Message))"
    }
}

if ($Search) {
    $hits = @($index.Keys | Where-Object { $_ -like "*$Search*" } | Sort-Object)
    if ($hits.Count -eq 0) {
        Write-Host "No class matching '$Search' on the resolved compileClasspath." -ForegroundColor Yellow
        return
    }
    Write-Host "$($hits.Count) match(es) for '$Search':" -ForegroundColor Cyan
    foreach ($hit in $hits) {
        Write-Host "  $hit"
        Write-Host "      in $(Split-Path -Leaf $index[$hit])" -ForegroundColor DarkGray
    }
    return
}

if ($Source) {
    $relativeSource = (($Source -replace '\.', [IO.Path]::DirectorySeparatorChar) + '.java')
    $sourceRoots = @()
    $referenceRoot = Join-Path $root 'reference'
    if (Test-Path -LiteralPath $referenceRoot) {
        foreach ($checkout in (Get-ChildItem -LiteralPath $referenceRoot -Directory -ErrorAction SilentlyContinue)) {
            $sourceRoots += Get-ChildItem -LiteralPath $checkout.FullName -Directory -Recurse -Filter 'java' -ErrorAction SilentlyContinue |
                Where-Object { $_.Parent.Name -eq 'main' -and $_.FullName -notmatch '[\\/]\.git[\\/]' } |
                Select-Object -ExpandProperty FullName
        }
    }
    $generatedRoot = Join-Path $root 'build\generated\sources'
    if (Test-Path -LiteralPath $generatedRoot) {
        $sourceRoots += Get-ChildItem -LiteralPath $generatedRoot -Directory -Recurse -Filter 'java' -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty FullName
    }

    foreach ($sourceRoot in $sourceRoots) {
        $candidate = Join-Path $sourceRoot $relativeSource
        if (Test-Path -LiteralPath $candidate) {
            Write-Host "### $Source" -ForegroundColor Cyan
            Write-Host "### source: $candidate" -ForegroundColor DarkGray
            Get-Content -LiteralPath $candidate -Raw
            return
        }
    }

    $sourceEntry = ($Source -replace '\.', '/') + '.java'
    $searchedVersionDirs = @{}
    foreach ($jarPath in $jars) {
        $hashDir = Split-Path -Parent $jarPath
        $versionDir = Split-Path -Parent $hashDir
        if (-not (Test-Path -LiteralPath $versionDir) -or $searchedVersionDirs.ContainsKey($versionDir)) { continue }
        $searchedVersionDirs[$versionDir] = $true
        foreach ($sourceJar in (Get-ChildItem -LiteralPath $versionDir -Recurse -Filter '*-sources.jar' -ErrorAction SilentlyContinue)) {
            try {
                $zip = [System.IO.Compression.ZipFile]::OpenRead($sourceJar.FullName)
                try {
                    $entry = $zip.GetEntry($sourceEntry)
                    if ($entry) {
                        $reader = [IO.StreamReader]::new($entry.Open())
                        try { $sourceText = $reader.ReadToEnd() } finally { $reader.Dispose() }
                        Write-Host "### $Source" -ForegroundColor Cyan
                        Write-Host "### original source from $($sourceJar.Name)" -ForegroundColor DarkGray
                        Write-Output $sourceText
                        return
                    }
                } finally {
                    $zip.Dispose()
                }
            } catch {
                Write-Warning "无法读取 sources JAR: $($sourceJar.FullName) ($($_.Exception.Message))"
            }
        }
    }

    Write-Host "No source available for '$Source'. No dependency was decompiled." -ForegroundColor Yellow
    return
}

if (-not $Class) {
    throw '请传入 -Class <FQN>、-Source <FQN>、-Search <文本> 或 -ListJars。'
}
if (-not $index.ContainsKey($Class)) {
    Write-Host "Class '$Class' is not on the resolved compileClasspath." -ForegroundColor Yellow
    return
}

Write-Host "### $Class" -ForegroundColor Cyan
Write-Host "### from $(Split-Path -Leaf $index[$Class])" -ForegroundColor DarkGray
$arguments = @()
if ($Private) { $arguments += '-p' }
$arguments += '-classpath'
$arguments += ($jars -join [IO.Path]::PathSeparator)
$arguments += $Class
& $javap @arguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
