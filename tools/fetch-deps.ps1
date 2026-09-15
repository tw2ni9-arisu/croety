<#
.SYNOPSIS
    (Re)stages the mod jars that this workspace needs but that are not published
    to any public Maven repository.

.DESCRIPTION
    Everything else (Create, Flywheel, Ponder, Registrate, MixinExtras, Curios,
    Patchouli, JEI) is pulled by Gradle straight from its upstream Maven.

    Goety is only distributed through Modrinth / CurseForge, so its release jar is
    staged into ./libs/maven as a local Maven module that build.gradle resolves as
    com.polarice3:goety:2.5.57.3 and deobfuscates with fg.deobf(...).

    Run this if ./libs/maven is missing or you bump the Goety version.

.NOTES
    On networks that block CRL/OCSP endpoints, .NET needs revocation checking
    disabled or the download fails with "The underlying connection was closed".
#>
[CmdletBinding()]
param(
    [string] $Version = '2.5.57.3',
    [string] $Sha1    = 'f140a0d2bc17f518703088a170d72bf9570efcc6'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[Net.ServicePointManager]::CheckCertificateRevocationList = $false

$root = Split-Path -Parent $PSScriptRoot
$dir  = Join-Path $root "libs\maven\com\polarice3\goety\$Version"
New-Item -ItemType Directory -Force -Path $dir | Out-Null

$jar = Join-Path $dir "goety-$Version.jar"
$url = "https://cdn.modrinth.com/data/4ZVIxU8x/versions/jVtlXH2H/goety-$Version.jar"

if (Test-Path $jar) {
    Write-Host "Already present: $jar"
} else {
    Write-Host "Downloading Goety $Version ..."
    (New-Object System.Net.WebClient).DownloadFile($url, $jar)
}

$actual = (Get-FileHash $jar -Algorithm SHA1).Hash.ToLower()
if ($Sha1 -and $actual -ne $Sha1.ToLower()) {
    throw "SHA1 mismatch for $jar - expected $Sha1 but got $actual"
}
Write-Host "goety-$Version.jar OK ($([math]::Round((Get-Item $jar).Length / 1MB, 1)) MB, sha1 $actual)"

$pom = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.polarice3</groupId>
  <artifactId>goety</artifactId>
  <version>$Version</version>
  <packaging>jar</packaging>
  <name>Goety</name>
  <description>Goety $Version for Minecraft 1.20.1 (Forge) - staged locally, no public Maven</description>
</project>
"@
Set-Content -Path (Join-Path $dir "goety-$Version.pom") -Value $pom -Encoding UTF8

# ---------------------------------------------------------------------------
# Source jars. Create/Ponder/Flywheel publish sources to their Maven, and
# tools/find-api.ps1 -Source prints them verbatim. Classes with no sources jar
# (Goety, Curios, Patchouli) are decompiled on demand with ForgeFlower instead,
# so nothing else needs downloading.
# ---------------------------------------------------------------------------
$srcDir = Join-Path $root 'libs\sources'
New-Item -ItemType Directory -Force -Path $srcDir | Out-Null

$sources = @{
    'create-1.20.1-6.0.8-291-sources.jar'      = 'https://maven.createmod.net/com/simibubi/create/create-1.20.1/6.0.8-291/create-1.20.1-6.0.8-291-sources.jar'
    'Ponder-Forge-1.20.1-1.0.91-sources.jar'   = 'https://maven.createmod.net/net/createmod/ponder/Ponder-Forge-1.20.1/1.0.91/Ponder-Forge-1.20.1-1.0.91-sources.jar'
    'flywheel-forge-1.20.1-1.0.5-264-sources.jar' = 'https://maven.createmod.net/dev/engine-room/flywheel/flywheel-forge-1.20.1/1.0.5-264/flywheel-forge-1.20.1-1.0.5-264-sources.jar'
}
foreach ($name in $sources.Keys) {
    $target = Join-Path $srcDir $name
    if (Test-Path $target) { Write-Host "Already present: $name"; continue }
    Write-Host "Downloading $name ..."
    (New-Object System.Net.WebClient).DownloadFile($sources[$name], $target)
    Write-Host "  -> $([math]::Round((Get-Item $target).Length / 1MB, 2)) MB"
}

Write-Host 'Done.'
