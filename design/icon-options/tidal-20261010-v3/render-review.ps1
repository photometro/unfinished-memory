param([string]$OutputPath = (Join-Path $PSScriptRoot 'review.png'))

# Presentation-only: candidate pixels are not edited or recoloured.
Add-Type -AssemblyName System.Drawing
$board = [System.Drawing.Bitmap]::new(1280, 1000)
$canvas = [System.Drawing.Graphics]::FromImage($board)
$canvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$canvas.Clear([System.Drawing.ColorTranslator]::FromHtml('#F5F4EF'))
$ink = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#24454D'))
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#687C80'))
$rule = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#DDE4E0'), 1)
$titleFont = [System.Drawing.Font]::new('Microsoft YaHei', 28, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$headingFont = [System.Drawing.Font]::new('Microsoft YaHei', 23, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$bodyFont = [System.Drawing.Font]::new('Microsoft YaHei', 16, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$smallFont = [System.Drawing.Font]::new('Microsoft YaHei', 13, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$images = [System.Collections.Generic.List[System.Drawing.Image]]::new()

function Draw-Label([string]$Text, [float]$X, [float]$Y, $Font = $bodyFont, $Brush = $muted) {
    $canvas.DrawString($Text, $Font, $Brush, $X, $Y)
}

function Draw-Icon($Image, [float]$X, [float]$Y, [float]$Size, [bool]$Circle = $false) {
    $state = $canvas.Save()
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
        $canvas.SetClip($clip)
        $canvas.DrawImage($Image, [System.Drawing.RectangleF]::new($X, $Y, $Size, $Size))
    } finally {
        $canvas.Restore($state)
        $clip.Dispose()
    }
}

try {
    $iconA = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'a-water-light.png'))
    $iconB = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'b-quiet-shore.png'))
    $images.Add($iconA)
    $images.Add($iconB)
    Draw-Label '未完成 · 新图标候选' 52 34 $titleFont $ink
    Draw-Label '减少附加符号，让水面与潮岸构成图标本身。仅供评审，未替换已安装图标。' 52 82
    $canvas.DrawLine($rule, 52, 121, 1228, 121)

    $entries = @(
        @{Image=$iconA; X=74; Title='A · 水光'; Description='以水面光线和流动层次衔接海面开屏。'},
        @{Image=$iconB; X=712; Title='B · 潮岸'; Description='以海水与沙色之间的曲线表达潮汐。'}
    )
    foreach ($entry in $entries) {
        Draw-Label $entry.Title $entry.X 148 $headingFont $ink
        Draw-Label $entry.Description $entry.X 186
        Draw-Icon $entry.Image ($entry.X + 112) 228 300
        Draw-Icon $entry.Image ($entry.X + 58) 585 48
        Draw-Label '48 px' ($entry.X + 58) 649 $smallFont
        Draw-Icon $entry.Image ($entry.X + 156) 569 64
        Draw-Label '64 px' ($entry.X + 156) 649 $smallFont
        Draw-Icon $entry.Image ($entry.X + 266) 537 96 $true
        Draw-Label '圆形 96 px' ($entry.X + 266) 649 $smallFont
    }

    $canvas.DrawLine($rule, 52, 696, 1228, 696)
    Draw-Label '现有开屏与首页参考' 52 722 $headingFont $ink
    Draw-Label '背景素材保持不变；这里仅检查色彩与气质的联系，不模拟启动动画。' 52 764
    $resourceRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../android/app/src/main/res/drawable-nodpi'))
    $sea = [System.Drawing.Image]::FromFile((Join-Path $resourceRoot 'sea_daylight.png'))
    $shore = [System.Drawing.Image]::FromFile((Join-Path $resourceRoot 'shore_daylight.png'))
    $images.Add($sea)
    $images.Add($shore)
    $canvas.DrawImage($sea, [System.Drawing.RectangleF]::new(886, 728, 88, 176))
    $canvas.DrawImage($shore, [System.Drawing.RectangleF]::new(1076, 728, 88, 176))
    Draw-Label '→' 1008 796 $headingFont
    Draw-Label '开屏' 906 916 $smallFont
    Draw-Label '首页' 1096 916 $smallFont
    Draw-Label '等比缩放与遮罩预览，不修改原图。候选为位图概念稿，并非正式 Android adaptive icon 图层。' 52 961 $smallFont
    $board.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    foreach ($image in $images) { $image.Dispose() }
    foreach ($resource in @($titleFont, $headingFont, $bodyFont, $smallFont, $ink, $muted, $rule, $canvas, $board)) { $resource.Dispose() }
}
