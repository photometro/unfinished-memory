param([string]$OutputPath = (Join-Path $PSScriptRoot 'review-b-shore-v2.png'))

# Only layout, equal-size resize and preview clipping; source pixels remain untouched.
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
    Label 'B · 潮岸 / 岸线微调' 48 30 $titleFont $ink
    Label '保留海水、沙色与留白；比较岸线转弯和浅水过渡。尚未替换 App 图标。' 48 78
    $entries = @(
        @{File='b-quiet-shore.png'; X=68; Title='原稿'; Caption='急弯较规整，浅水带接近等宽。'},
        @{File='b-quiet-shore-v2.png'; X=640; Title='调整稿'; Caption='拉开中段转弯，让浅水过渡宽窄变化。'}
    )
    foreach ($entry in $entries) {
        $image = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot $entry.File))
        $images.Add($image)
        Label $entry.Title $entry.X 129 $headingFont $ink
        Label $entry.Caption $entry.X 168
        Icon $image ($entry.X + 68) 216 330
        Icon $image ($entry.X + 42) 612 48
        Label '48 px' ($entry.X + 42) 674 $smallFont
        Icon $image ($entry.X + 140) 596 64
        Label '64 px' ($entry.X + 140) 674 $smallFont
        Icon $image ($entry.X + 252) 564 96 $true
        Label '圆形 96 px' ($entry.X + 252) 674 $smallFont
    }
    Label '局部编辑概念稿。预览仅等比缩放、遮罩裁切；不代表真机或正式 Android 自适应图层。' 48 744 $smallFont
    $board.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    foreach ($image in $images) { $image.Dispose() }
    foreach ($resource in @($titleFont, $headingFont, $bodyFont, $smallFont, $ink, $muted, $graphics, $board)) { $resource.Dispose() }
}
