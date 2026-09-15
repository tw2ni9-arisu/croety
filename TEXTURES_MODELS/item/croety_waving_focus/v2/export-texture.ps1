param([Parameter(Mandatory = $true)] [string] $SourceImage)

# 图像工具负责生成木质齿轮；导出阶段锁定用户参考图的主体像素和14px圆形边界。
Add-Type -AssemblyName System.Drawing
$source = [Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $SourceImage).Path)
$reference = [Drawing.Bitmap]::FromFile((Join-Path $PSScriptRoot 'user-reference.png'))
if ($reference.Width -ne 16 -or $reference.Height -ne 16) { throw 'Expected a 16x16 user reference' }
$sample = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$draw = [Drawing.Graphics]::FromImage($sample)
$draw.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
$draw.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$draw.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$draw.DrawImage($source, [Drawing.Rectangle]::new(0, 0, 16, 16), 0, 0, $source.Width, $source.Height, [Drawing.GraphicsUnit]::Pixel)
$draw.Dispose()
$source.Dispose()

# 逐行轮廓取自用户参考圆框：上下各4px，最宽处14px，画布四边各留1px透明。
$rowStarts = @(16, 6, 4, 3, 2, 2, 1, 1, 1, 1, 2, 2, 3, 4, 6, 16)
function Inside-Circle([int] $X, [int] $Y) {
    if ($X -lt 0 -or $X -ge 16 -or $Y -lt 0 -or $Y -ge 16) { return $false }
    return $X -ge $rowStarts[$Y] -and $X -le 15 - $rowStarts[$Y]
}
$woodPalette = @('#302116', '#4A3323', '#715038', '#997043', '#BC9159', '#DEBA7C', '#F1D8A7', '#AA8250') |
    ForEach-Object { [Drawing.ColorTranslator]::FromHtml($_) }
$rim = [Drawing.ColorTranslator]::FromHtml('#302116')
$texture = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$foreground = 0
$background = 0
$border = 0
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        if (!(Inside-Circle $x $y)) { continue }
        if (!(Inside-Circle ($x - 1) $y) -or !(Inside-Circle ($x + 1) $y) -or
            !(Inside-Circle $x ($y - 1)) -or !(Inside-Circle $x ($y + 1))) {
            $texture.SetPixel($x, $y, $rim)
            $border++
            continue
        }
        $original = $reference.GetPixel($x, $y)
        if ($original.A -gt 0) {
            $texture.SetPixel($x, $y, $original)
            $foreground++
            continue
        }
        $pixel = $sample.GetPixel($x, $y)
        $best = $woodPalette[3]
        if ($pixel.A -ge 128) {
            $bestDistance = [double]::PositiveInfinity
            foreach ($color in $woodPalette) {
                $distance = [math]::Pow($pixel.R - $color.R, 2) + [math]::Pow($pixel.G - $color.G, 2) + [math]::Pow($pixel.B - $color.B, 2)
                if ($distance -lt $bestDistance) { $bestDistance = $distance; $best = $color }
            }
        }
        $texture.SetPixel($x, $y, $best)
        $background++
    }
}
$texture.Save((Join-Path $PSScriptRoot 'waving_focus.png'), [Drawing.Imaging.ImageFormat]::Png)
$sample.Dispose()
$reference.Dispose()

$preview = [Drawing.Bitmap]::new(512, 512)
$draw = [Drawing.Graphics]::FromImage($preview)
$draw.Clear([Drawing.Color]::FromArgb(225, 225, 225))
$checker = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(205, 205, 205))
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    if (($x + $y) % 2 -eq 0) { $draw.FillRectangle($checker, $x * 32, $y * 32, 32, 32) }
} }
$draw.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$draw.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$draw.DrawImage($texture, [Drawing.Rectangle]::new(0, 0, 512, 512), 0, 0, 16, 16, [Drawing.GraphicsUnit]::Pixel)
$preview.Save((Join-Path $PSScriptRoot 'preview.png'), [Drawing.Imaging.ImageFormat]::Png)
$draw.Dispose()
$checker.Dispose()
$preview.Dispose()

$outside = 0
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 16; $x++) {
    if (!(Inside-Circle $x $y) -and $texture.GetPixel($x, $y).A -ne 0) { $outside++ }
} }
$texture.Dispose()
if ($outside -ne 0) { throw 'Pixels escaped the 14px circle' }
Write-Output "16x16 RGBA; circle14px; preserved foreground=$foreground; wood background=$background; rim=$border; outside pixels=$outside"
