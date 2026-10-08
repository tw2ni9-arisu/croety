<#
.SYNOPSIS
    将 Goety 3.2.0 官方发行 JAR 校验后暂存到本地 Maven。
.DESCRIPTION
    先复用 reference/artifacts 中已核验的官方发行 JAR；缺失时从固定 Modrinth
    发行 URL 下载。SHA-256 必须匹配 SOURCES.md 中的记录，原始 JAR 不改包。
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[Net.ServicePointManager]::CheckCertificateRevocationList = $false

$version = '3.2.0'
$expectedSha256 = 'EDEEB623F626BC85BD26DF6458DBB440FD4B044F34F3DE78EE715CBC01E692D8'
$url = 'https://cdn.modrinth.com/data/4ZVIxU8x/versions/8jB68vz3/goety-3.2.0.jar'
$root = Split-Path -Parent $PSScriptRoot
$referenceJar = Join-Path $root 'reference\artifacts\goety-3.2.0.jar'
$artifactDir = Join-Path $root "libs\maven\com\polarice3\goety\$version"
$targetJar = Join-Path $artifactDir "goety-$version.jar"
$targetPom = Join-Path $artifactDir "goety-$version.pom"
$downloadsDir = Join-Path $root '.local\downloads'
$downloadJar = Join-Path $downloadsDir "goety-$version.jar"

function Get-Sha256([string] $Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToUpperInvariant()
}

$sourceJar = $null
if ((Test-Path -LiteralPath $targetJar) -and (Get-Sha256 $targetJar) -eq $expectedSha256) {
    $sourceJar = $targetJar
}

if (Test-Path -LiteralPath $referenceJar) {
    if (-not $sourceJar -and (Get-Sha256 $referenceJar) -eq $expectedSha256) {
        $sourceJar = $referenceJar
        Write-Host "Using verified release artifact: $referenceJar"
    } elseif (-not $sourceJar) {
        Write-Warning "Reference artifact hash mismatch; downloading the pinned official release."
    }
}

if (-not $sourceJar) {
    New-Item -ItemType Directory -Force -Path $downloadsDir | Out-Null
    Write-Host "Downloading Goety $version from its pinned Modrinth release URL ..."
    (New-Object System.Net.WebClient).DownloadFile($url, $downloadJar)
    $downloadHash = Get-Sha256 $downloadJar
    if ($downloadHash -ne $expectedSha256) {
        throw "SHA-256 mismatch for downloaded Goety ${version}: expected $expectedSha256, got $downloadHash"
    }
    $sourceJar = $downloadJar
}

$sourceHash = Get-Sha256 $sourceJar
if ($sourceHash -ne $expectedSha256) {
    throw "SHA-256 mismatch for Goety ${version}: expected $expectedSha256, got $sourceHash"
}

New-Item -ItemType Directory -Force -Path $artifactDir | Out-Null
if ((Test-Path -LiteralPath $targetJar) -and (Get-Sha256 $targetJar) -eq $expectedSha256) {
    Write-Host "Verified local Maven artifact already present: $targetJar"
} else {
    Copy-Item -LiteralPath $sourceJar -Destination $targetJar -Force
}
$stagedHash = Get-Sha256 $targetJar
if ($stagedHash -ne $expectedSha256) {
    throw "SHA-256 mismatch for staged Goety ${version}: expected $expectedSha256, got $stagedHash"
}

$pom = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.polarice3</groupId>
  <artifactId>goety</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
  <name>Goety</name>
  <description>Goety $version for Minecraft 1.21.1 NeoForge</description>
</project>
"@
Set-Content -LiteralPath $targetPom -Value $pom -Encoding UTF8
Write-Host "Goety $version staged and verified (SHA-256 $stagedHash)."
