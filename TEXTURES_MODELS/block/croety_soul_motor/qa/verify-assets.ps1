$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$original = Join-Path (Split-Path (Split-Path $root -Parent) -Parent) 'needed\create_creative_motor\assets\create'
$assets = Join-Path $root 'assets\croety'
$results = @()
foreach ($file in (Get-ChildItem -LiteralPath (Join-Path $assets 'textures\block\soul_motor') -Filter '*.png')) {
    $bitmap = [System.Drawing.Bitmap]::FromFile($file.FullName)
    $reference = [System.Drawing.Bitmap]::FromFile((Join-Path $original "textures\block\$($file.Name)"))
    if ($bitmap.Width -ne 16 -or $bitmap.Height -ne 16) { throw "分辨率错误：$($file.Name)" }
    $count = 0; $min = 255; $max = 0
    for ($y=0; $y -lt 16; $y++) {
        for ($x=0; $x -lt 16; $x++) {
            $c = $bitmap.GetPixel($x,$y)
            $expectedAlpha = if ($reference.GetPixel($x,$y).A -eq 0) { 0 } else { 89 }
            if ($c.A -ne $expectedAlpha) { throw "透明遮罩或不透明度错误：$($file.Name) ($x,$y)" }
            if ($c.A -gt 0) {
                if ($c.B -lt $c.G -or $c.G -lt $c.R) { throw "存在非蓝色材质像素：$($file.Name) ($x,$y)" }
                $count++; $min=[Math]::Min($min,$c.A); $max=[Math]::Max($max,$c.A)
            }
        }
    }
    $results += "$($file.Name): 16x16 RGBA; visible=$count; alpha=$min..$max; max opacity=$([Math]::Round($max/255*100,2))%; source mask preserved; blue palette PASS"
    $bitmap.Dispose(); $reference.Dispose()
}
foreach ($name in @('block','block_vertical','item')) {
    $source = Get-Content -Raw -LiteralPath (Join-Path $original "models\block\creative_motor\$name.json") | ConvertFrom-Json
    $model = Get-Content -Raw -LiteralPath (Join-Path $assets "models\block\soul_motor\$name.json") | ConvertFrom-Json
    foreach ($property in @('elements','groups','display','parent')) {
        $a = $source.$property | ConvertTo-Json -Depth 100 -Compress
        $b = $model.$property | ConvertTo-Json -Depth 100 -Compress
        if ($a -cne $b) { throw "$name.json 的 $property 与原模型不一致" }
    }
    if ($model.render_type -ne 'minecraft:translucent') { throw "$name.json 缺少半透明渲染类型" }
    foreach ($property in $model.textures.PSObject.Properties) {
        $id = $property.Value
        if (-not $id.StartsWith('croety:block/soul_motor/')) { throw "残留原贴图引用：$id" }
        $path = Join-Path $assets ('textures\' + ($id -split ':')[1] + '.png')
        if (-not (Test-Path -LiteralPath $path)) { throw "缺少贴图：$path" }
    }
    $results += "$name.json: original geometry/UV/display preserved; all textures resolved; translucent PASS"
}
$state = Get-Content -Raw -LiteralPath (Join-Path $assets 'blockstates\soul_motor.json') | ConvertFrom-Json
if (@($state.variants.PSObject.Properties).Count -ne 6) { throw '方块朝向数量错误' }
foreach ($variant in $state.variants.PSObject.Properties) {
    $path = Join-Path $assets ('models\' + ($variant.Value.model -split ':')[1] + '.json')
    if (-not (Test-Path -LiteralPath $path)) { throw "方块状态模型缺失：$path" }
}
$item = Get-Content -Raw -LiteralPath (Join-Path $assets 'models\item\soul_motor.json') | ConvertFrom-Json
if ($item.parent -ne 'croety:block/soul_motor/item') { throw '物品模型引用错误' }
$results += 'All 6 blockstate variants and item parent resolved PASS'
$results | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'validation.txt') -Encoding UTF8
$results
