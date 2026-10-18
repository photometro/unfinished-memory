param([string]$OutputPath = (Join-Path $PSScriptRoot 'review-b-shore-v6.png'))

# Presentation only: resize and mask copies, do not recolor or edit source artwork.
Add-Type -AssemblyName System.Drawing
$board = [System.Drawing.Bitmap]::new(1320, 740)
$graphics = [System.Drawing.Graphics]::FromImage($board)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#F5F4EF'))
$ink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#24454D'))
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#687C80'))
$titleFont = [System.Drawing.Font]::new('Microsoft YaHei', 27, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$headingFont = [System.Drawing.Font]::new('Microsoft YaHei', 21, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
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
    Label 'B · 潮岸 / 三种色彩对照' 48 28 $titleFont $ink
    Label '中间为新候选。保留沙滩与海浪，仅调整颜色；不使用潮滩实拍作为输入。' 48 76
    $entries = @(
        @{File='b-quiet-shore-v5-bright.png'; X=48; Title='清亮 / 上一版'; Caption='较高彩度蓝色、暖奶油沙色。'},
        @{File='b-quiet-shore-v6-natural.png'; X=472; Title='自然 / 新候选'; Caption='适中彩度蓝绿、浅米沙色。'},
        @{File='b-quiet-shore-v5-muted.png'; X=896; Title='质朴 / 上一版'; Caption='较低彩度蓝灰、暖灰沙色。'}
    )
    foreach ($entry in $entries) {
        $artwork = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot $entry.File))
        $images.Add($artwork)
        Label $entry.Title $entry.X 124 $headingFont $ink
        Label $entry.Caption $entry.X 160
        Icon $artwork ($entry.X + 24) 198 300
        Icon $artwork ($entry.X + 24) 574 48
        Label '48 px' ($entry.X + 24) 636 $smallFont
        Icon $artwork ($entry.X + 124) 558 64
        Label '64 px' ($entry.X + 124) 636 $smallFont
        Icon $artwork ($entry.X + 244) 526 96 $true
        Label '圆形 96 px' ($entry.X + 244) 636 $smallFont
    }
    Label '候选预览，不替换 App。只等比缩放、裁切；不是像素级调色证明或设备实测。' 48 692 $smallFont
    $board.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    foreach ($artwork in $images) { $artwork.Dispose() }
    foreach ($resource in @($titleFont, $headingFont, $bodyFont, $smallFont, $ink, $muted, $graphics, $board)) { $resource.Dispose() }
}
