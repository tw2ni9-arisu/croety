$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$targetRoot = Split-Path $PSScriptRoot -Parent
$sourceRoot = Join-Path (Split-Path (Split-Path $targetRoot -Parent) -Parent) 'needed\create_creative_motor\assets\create'
$assetRoot = Join-Path $targetRoot 'assets\croety'
$textureDir = Join-Path $assetRoot 'textures\block\soul_motor'
New-Item -ItemType Directory -Path $textureDir -Force | Out-Null
$names = @('creative_casing', 'creative_motor', 'flap_display_front', 'axis', 'axis_top')
$generated = [System.Drawing.Bitmap]::FromFile((Join-Path $PSScriptRoot 'generated-texture-strip.png'))
$report = @()
for ($i = 0; $i -lt $names.Count; $i++) {
    $source = [System.Drawing.Bitmap]::FromFile((Join-Path $sourceRoot "textures\block\$($names[$i]).png"))
    $output = New-Object System.Drawing.Bitmap 16,16
    $visible = 0
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            # 生成图的有效纹理带位于 y=144..578；按逻辑像素中心取样，避免插值模糊。
            $sx = [int][Math]::Floor(($i * 16 + $x + 0.5) * $generated.Width / 80)
            $sy = [int][Math]::Floor(144 + ($y + 0.5) * 435 / 16)
            $color = $generated.GetPixel($sx, $sy)
            # 用原贴图的透明区域作为精确遮罩，生成图里的背景棋盘不会进入最终资产。
            $alpha = if ($source.GetPixel($x, $y).A -eq 0) { 0 } else { 89 }
            if ($alpha -gt 0) { $visible++ }
            $output.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($alpha, $color.R, $color.G, $color.B))
        }
    }
    $output.Save((Join-Path $textureDir "$($names[$i]).png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $report += [ordered]@{ texture=$names[$i]; width=16; height=16; visiblePixels=$visible; visibleAlpha=89; opacityPercent=(89/255*100) }
    $source.Dispose()
    $output.Dispose()
}
$generated.Dispose()

# 保留所有元素、UV、旋转和显示参数，只改资源引用与半透明渲染类型。
$encoding = New-Object System.Text.UTF8Encoding $false
foreach ($name in @('block', 'block_vertical', 'item')) {
    $text = [IO.File]::ReadAllText((Join-Path $sourceRoot "models\block\creative_motor\$name.json"))
    foreach ($texture in $names) { $text = $text.Replace("create:block/$texture", "croety:block/soul_motor/$texture") }
    $text = $text.Replace('"parent": "block/block",', '"parent": "block/block",' + "`n`t" + '"render_type": "minecraft:translucent",')
    $path = Join-Path $assetRoot "models\block\soul_motor\$name.json"
    New-Item -ItemType Directory -Path (Split-Path $path -Parent) -Force | Out-Null
    [IO.File]::WriteAllText($path, $text, $encoding)
}
foreach ($relative in @('models\item\creative_motor.json', 'blockstates\creative_motor.json')) {
    $text = [IO.File]::ReadAllText((Join-Path $sourceRoot $relative)).Replace('create:block/creative_motor/', 'croety:block/soul_motor/')
    $path = Join-Path $assetRoot ($relative.Replace('creative_motor.json', 'soul_motor.json'))
    New-Item -ItemType Directory -Path (Split-Path $path -Parent) -Force | Out-Null
    [IO.File]::WriteAllText($path, $text, $encoding)
}
$report | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'texture-report.json') -Encoding UTF8
$report | Format-Table
