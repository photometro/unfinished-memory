param([string]$OutputPath = (Join-Path $PSScriptRoot 'review-b-shore-v5.png'))

# Presentation only: resize and mask previews, never edit source artwork pixels.
Add-Type -AssemblyName System.Drawing
$board = [System.Drawing.Bitmap]::new(1160, 790)
$graphics = [System.Drawing.Graphics]::FromImage($board)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#F5F4EF'))
$ink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#24454D'))
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#687C80'))
$titleFont = [System.Drawing.Font]::new('Microsoft YaHei', 27, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$headingFont = [System.Drawing.Font]::new('Microsoft YaHei', 22, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$bodyFont = [System.Drawing.Font]::new('Microsoft YaHei', 16, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$smallFont = [System.Drawing.Font]::new('Microsoft YaHei', 13, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$images = [System.Collections.Generic.List[System.Drawing.Image]]::new()

function Label([string]$Text, [float]$X, [float]$Y, $Font = $bodyFont, $Brush = $muted) {
    $graphics.DrawString($Text, $Font, $Brush, $X, $Y)
}

function Icon($Image, [float]$X, [float]$Y, [float]$Size, [bool]$Circle = $false) {
    $state = $graphics.Save()
    $clip = [System.Drawing.Drawing2D.GraphicsPath]::new()
    try {
        if ($Circle) {
            $clip.AddEllipse($X, $Y, $Size, $Size)
        } else {
            $diameter = $Size * 0.44
            $clip.AddArc($X, $Y, $diameter, $diameter, 180, 90)
            $clip.AddArc(($X + $Size - $diameter), $Y, $diameter, $diameter, 270, 90)
            $clip.AddArc(($X + $Size - $diameter), ($Y + $Size - $diameter), $diameter, $diameter, 0, 90)
            $clip.AddArc($X, ($Y + $Size - $diameter), $diameter, $diameter, 90, 90)
            $clip.CloseFigure()
        }
        $graphics.SetClip($clip)
        $graphics.DrawImage($Image, [System.Drawing.RectangleF]::new($X, $Y, $Size, $Size))
    } finally {
        $graphics.Restore($state)
        $clip.Dispose()
    }
}

try {
    Label 'B · 潮岸 / 回到沙滩与海浪' 48 30 $titleFont $ink
    Label '取消潮沟与符号化分叉；两版主要比较色彩，不替换 App。' 48 78
    $entries = @(
        @{File='b-quiet-shore-v5-bright.png'; X=68; Title='清亮色彩'; Caption='保留清蓝海水与奶油沙色。'},
        @{File='b-quiet-shore-v5-muted.png'; X=640; Title='质朴色彩'; Caption='低饱和海蓝与暖灰沙色，保持明亮。'}
    )
    foreach ($entry in $entries) {
        $artwork = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot $entry.File))
        $images.Add($artwork)
        Label $entry.Title $entry.X 129 $headingFont $ink
        Label $entry.Caption $entry.X 168
        Icon $artwork ($entry.X + 68) 216 330
        Icon $artwork ($entry.X + 42) 612 48
        Label '48 px' ($entry.X + 42) 674 $smallFont
        Icon $artwork ($entry.X + 140) 596 64
        Label '64 px' ($entry.X + 140) 674 $smallFont
        Icon $artwork ($entry.X + 252) 564 96 $true
        Label '圆形 96 px' ($entry.X + 252) 674 $smallFont
    }
    Label '候选对照。仅等比缩放、遮罩裁切；不是正式 Android 自适应图层，也不是设备实测。' 48 744 $smallFont
    $board.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    foreach ($artwork in $images) { $artwork.Dispose() }
    foreach ($resource in @($titleFont, $headingFont, $bodyFont, $smallFont, $ink, $muted, $graphics, $board)) { $resource.Dispose() }
}
