param(
    [Parameter(Mandatory = $true)] [string] $SourceImage
)

# 将图像生成原稿按最近邻采样到游戏尺寸，并输出来自同一贴图的放大预览。
Add-Type -AssemblyName System.Drawing
$source = [Drawing.Bitmap]::FromFile((Resolve-Path -LiteralPath $SourceImage).Path)
$texture = [Drawing.Bitmap]::new(16, 16, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($texture)
$graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
$graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.DrawImage($source, [Drawing.Rectangle]::new(0, 0, 16, 16), 0, 0, $source.Width, $source.Height, [Drawing.GraphicsUnit]::Pixel)
$graphics.Dispose()

# 使用生成提示中的有限色板，消除放大原稿内的细微颜色噪点和边缘低透明像素。
$palette = @('#10183A', '#183F75', '#2563EB', '#1B6BA5', '#24C0EB', '#54C2ED', '#24EBB9', '#C6FFF5') |
    ForEach-Object { [Drawing.ColorTranslator]::FromHtml($_) }
for ($row = 0; $row -lt 16; $row++) {
    for ($column = 0; $column -lt 16; $column++) {
        $pixel = $texture.GetPixel($column, $row)
        if ($pixel.A -lt 128) { $texture.SetPixel($column, $row, [Drawing.Color]::Transparent); continue }
        $best = $palette[0]
        $bestDistance = [double]::PositiveInfinity
        foreach ($color in $palette) {
            $distance = [math]::Pow($pixel.R - $color.R, 2) + [math]::Pow($pixel.G - $color.G, 2) + [math]::Pow($pixel.B - $color.B, 2)
            if ($distance -lt $bestDistance) { $bestDistance = $distance; $best = $color }
        }
        $texture.SetPixel($column, $row, $best)
    }
}
$texture.Save((Join-Path $PSScriptRoot 'waving_focus.png'), [Drawing.Imaging.ImageFormat]::Png)
$source.Dispose()

$preview = [Drawing.Bitmap]::new(512, 512, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [Drawing.Graphics]::FromImage($preview)
$graphics.Clear([Drawing.Color]::FromArgb(35, 42, 54))
$checker = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(48, 56, 69))
for ($row = 0; $row -lt 16; $row++) {
    for ($column = 0; $column -lt 16; $column++) {
        if (($row + $column) % 2 -eq 0) { $graphics.FillRectangle($checker, $column * 32, $row * 32, 32, 32) }
    }
}
$graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
$graphics.DrawImage($texture, [Drawing.Rectangle]::new(0, 0, 512, 512), 0, 0, 16, 16, [Drawing.GraphicsUnit]::Pixel)
$preview.Save((Join-Path $PSScriptRoot 'preview.png'), [Drawing.Imaging.ImageFormat]::Png)
$graphics.Dispose()
$checker.Dispose()
$preview.Dispose()

$colors = [Collections.Generic.HashSet[int]]::new()
$transparent = 0
$partial = 0
for ($row = 0; $row -lt 16; $row++) {
    for ($column = 0; $column -lt 16; $column++) {
        $pixel = $texture.GetPixel($column, $row)
        if ($pixel.A -eq 0) { $transparent++ }
        elseif ($pixel.A -lt 255) { $partial++ }
        if ($pixel.A -gt 0) { [void] $colors.Add($pixel.ToArgb()) }
    }
}
$texture.Dispose()
Write-Output "16x16 RGBA; visible colors: $($colors.Count); transparent pixels: $transparent; partial-alpha pixels: $partial"
