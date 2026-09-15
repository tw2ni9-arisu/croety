$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Split-Path $PSScriptRoot -Parent
$original = Join-Path (Split-Path (Split-Path $root -Parent) -Parent) 'needed\create_creative_motor\assets\create'
$edited = Join-Path $root 'assets\croety'
$canvas = New-Object System.Drawing.Bitmap 1500,830
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.Clear([System.Drawing.Color]::FromArgb(20,32,45))
$font = New-Object System.Drawing.Font 'Microsoft YaHei',22
$small = New-Object System.Drawing.Font 'Microsoft YaHei',12
$brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220,240,250))
$g.DrawString('幽灵马达 / SOUL MOTOR', $font, $brush, 44, 28)
$g.DrawString('原模型结构 · 淡冰蓝贴图 · 34.90% 不透明度', $small, $brush, 47, 72)

function Rotate-Point($p, $rotation, $vector = $false) {
    if (-not $rotation -or $rotation.angle -eq 0) { return ,$p }
    $o = if ($vector) { @(0,0,0) } else { $rotation.origin }
    $v = @(($p[0]-$o[0]), ($p[1]-$o[1]), ($p[2]-$o[2]))
    $a = $rotation.angle * [Math]::PI / 180
    $c = [Math]::Cos($a); $s = [Math]::Sin($a)
    $v = switch ($rotation.axis) {
        'x' { @($v[0], ($v[1]*$c-$v[2]*$s), ($v[1]*$s+$v[2]*$c)) }
        'y' { @(($v[0]*$c+$v[2]*$s), $v[1], (-$v[0]*$s+$v[2]*$c)) }
        'z' { @(($v[0]*$c-$v[1]*$s), ($v[0]*$s+$v[1]*$c), $v[2]) }
    }
    return ,@(($v[0]+$o[0]), ($v[1]+$o[1]), ($v[2]+$o[2]))
}

function Render-Motor($assetRoot, $modelName, $centerX, $yaw, $label) {
    $isOriginal = $assetRoot -eq $original
    $folder = if ($isOriginal) { 'creative_motor' } else { 'soul_motor' }
    $model = Get-Content -Raw -LiteralPath (Join-Path $assetRoot "models\block\$folder\$modelName.json") | ConvertFrom-Json
    $textures = @{}
    foreach ($property in $model.textures.PSObject.Properties) {
        $rel = ($property.Value -split ':')[1]
        $textures[$property.Name] = [System.Drawing.Bitmap]::FromFile((Join-Path $assetRoot "textures\$rel.png"))
    }
    $polygons = New-Object 'System.Collections.Generic.List[object]'
    $angle = $yaw * [Math]::PI / 180
    $sy = [Math]::Sin($angle); $cy = [Math]::Cos($angle)
    $sp = [Math]::Sin(0.48); $cp = [Math]::Cos(0.48)
    foreach ($element in $model.elements) {
        $x0,$y0,$z0 = $element.from
        $x1,$y1,$z1 = $element.to
        $faces = @{
            north = @(@($x1,$y1,$z0), @($x0,$y1,$z0), @($x0,$y0,$z0), @($x1,$y0,$z0), @(0,0,-1))
            south = @(@($x0,$y1,$z1), @($x1,$y1,$z1), @($x1,$y0,$z1), @($x0,$y0,$z1), @(0,0,1))
            east  = @(@($x1,$y1,$z1), @($x1,$y1,$z0), @($x1,$y0,$z0), @($x1,$y0,$z1), @(1,0,0))
            west  = @(@($x0,$y1,$z0), @($x0,$y1,$z1), @($x0,$y0,$z1), @($x0,$y0,$z0), @(-1,0,0))
            up    = @(@($x0,$y1,$z0), @($x1,$y1,$z0), @($x1,$y1,$z1), @($x0,$y1,$z1), @(0,1,0))
            down  = @(@($x0,$y0,$z1), @($x1,$y0,$z1), @($x1,$y0,$z0), @($x0,$y0,$z0), @(0,-1,0))
        }
        foreach ($faceProperty in $element.faces.PSObject.Properties) {
            $face = $faceProperty.Value
            $corners = $faces[$faceProperty.Name]
            $normal = Rotate-Point $corners[4] $element.rotation $true
            if (($normal[0]*$sy*$cp + $normal[1]*$sp + $normal[2]*$cy*$cp) -le 0) { continue }
            $shade = 0.78 + 0.22 * [Math]::Max(0,$normal[1])
            $texture = $textures[$face.texture.TrimStart('#')]
            $u0,$v0,$u1,$v1 = $face.uv
            $nu = [Math]::Max(1,[Math]::Ceiling([Math]::Abs($u1-$u0)*2))
            $nv = [Math]::Max(1,[Math]::Ceiling([Math]::Abs($v1-$v0)*2))
            if ($face.rotation -eq 90 -or $face.rotation -eq 270) { $nu,$nv = $nv,$nu }
            for ($iy=0; $iy -lt $nv; $iy++) {
                for ($ix=0; $ix -lt $nu; $ix++) {
                    $u=($ix+0.5)/$nu; $v=($iy+0.5)/$nv
                    switch ($face.rotation) {
                        90  { $u,$v = $v,(1-$u) }
                        180 { $u,$v = (1-$u),(1-$v) }
                        270 { $u,$v = (1-$v),$u }
                    }
                    $tx = (([int][Math]::Floor($u0+($u1-$u0)*$u)) % 16 + 16) % 16
                    $ty = (([int][Math]::Floor($v0+($v1-$v0)*$v)) % 16 + 16) % 16
                    $pixel = $texture.GetPixel($tx,$ty)
                    if ($pixel.A -eq 0) { continue }
                    $points = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
                    $depth=0
                    foreach ($uv in @(@(($ix/$nu),($iy/$nv)), @((($ix+1)/$nu),($iy/$nv)), @((($ix+1)/$nu),(($iy+1)/$nv)), @(($ix/$nu),(($iy+1)/$nv)))) {
                        $p = @(0.0,0.0,0.0)
                        for ($k=0; $k -lt 3; $k++) { $p[$k]=$corners[0][$k]+($corners[1][$k]-$corners[0][$k])*$uv[0]+($corners[3][$k]-$corners[0][$k])*$uv[1] }
                        $p = Rotate-Point $p $element.rotation
                        $px=$p[0]-8; $py=$p[1]-8; $pz=$p[2]-8
                        $points.Add([System.Drawing.PointF]::new(($centerX+($px*$cy-$pz*$sy)*22), (335-(-$px*$sy*$sp+$py*$cp-$pz*$cy*$sp)*22)))
                        $depth += $px*$sy*$cp+$py*$sp+$pz*$cy*$cp
                    }
                    $polygons.Add(@{ points=$points.ToArray(); depth=$depth; color=[System.Drawing.Color]::FromArgb($pixel.A,[int]($pixel.R*$shade),[int]($pixel.G*$shade),[int]($pixel.B*$shade)) })
                }
            }
        }
    }
    # 逐面、逐像素排序的静态材质预览；实际游戏由 Forge/Flywheel 决定透明面的排序。
    foreach ($polygon in ($polygons | Sort-Object { $_.depth })) {
        $fill = New-Object System.Drawing.SolidBrush $polygon.color
        $g.FillPolygon($fill, $polygon.points)
        $fill.Dispose()
    }
    $g.DrawString($label, $small, $brush, ($centerX-115), 555)
    foreach ($texture in $textures.Values) { $texture.Dispose() }
}

Render-Motor $original 'item' 265 42 '原版创造马达'
Render-Motor $edited 'item' 750 42 '幽灵马达 · 正面 / 轴端'
Render-Motor $edited 'item' 1235 222 '幽灵马达 · 背面'
$g.DrawString('最终贴图（深色底）', $small, $brush, 48, 623)
$names = @('creative_casing', 'creative_motor', 'flap_display_front', 'axis', 'axis_top')
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
for ($i=0; $i -lt $names.Count; $i++) {
    $bmp = [System.Drawing.Bitmap]::FromFile((Join-Path $edited "textures\block\soul_motor\$($names[$i]).png"))
    $g.DrawImage($bmp, (New-Object System.Drawing.Rectangle (290+$i*190),620,128,128),0,0,16,16,[System.Drawing.GraphicsUnit]::Pixel)
    $g.DrawString($names[$i], $small, $brush, (280+$i*190), 754)
    $bmp.Dispose()
}
$g.DrawString('直接读取最终 JSON 与 PNG 渲染；示意预览，非游戏截图。透明表面叠加后局部视觉不透明度会高于单张贴图。', $small, $brush, 47, 798)
$canvas.Save((Join-Path $root 'preview.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $canvas.Dispose(); $font.Dispose(); $small.Dispose(); $brush.Dispose()
Write-Output 'preview.png 已生成'
