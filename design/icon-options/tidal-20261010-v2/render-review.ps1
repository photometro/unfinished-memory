param([string]$OutputPath = (Join-Path $PSScriptRoot 'review.png'))

# Review-only layout: original candidate PNGs are not edited or recoloured.
Add-Type -AssemblyName System.Drawing
$board = New-Object System.Drawing.Bitmap 1280, 1060
$canvas = [System.Drawing.Graphics]::FromImage($board)
$canvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$canvas.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$canvas.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$canvas.Clear([System.Drawing.ColorTranslator]::FromHtml('#F4F4EF'))
$ink = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#24454D'))
$muted = New-Object System.Drawing.SolidBrush ([System.Drawing.ColorTranslator]::FromHtml('#657B80'))
$line = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml('#D8E2DF')), 1
$fontTitle = New-Object System.Drawing.Font 'Microsoft YaHei', 26, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$fontHeading = New-Object System.Drawing.Font 'Microsoft YaHei', 23, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$fontBody = New-Object System.Drawing.Font 'Microsoft YaHei', 17, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$fontSmall = New-Object System.Drawing.Font 'Microsoft YaHei', 13, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$sourceImages = @()

function Draw-Label([string]$Value, [float]$X, [float]$Y, $Font = $fontBody, $Brush = $muted) {
    $canvas.DrawString($Value, $Font, $Brush, $X, $Y)
}

function Draw-ImagePreview($Image, [float]$X, [float]$Y, [float]$Size, [string]$Mask = 'rounded') {
    $saved = $canvas.Save()
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    try {
        if ($Mask -eq 'circle') {
            $path.AddEllipse($X, $Y, $Size, $Size)
        } else {
            $diameter = $Size * 0.44
            $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
            $path.AddArc(($X + $Size - $diameter), $Y, $diameter, $diameter, 270, 90)
            $path.AddArc(($X + $Size - $diameter), ($Y + $Size - $diameter), $diameter, $diameter, 0, 90)
            $path.AddArc($X, ($Y + $Size - $diameter), $diameter, $diameter, 90, 90)
            $path.CloseFigure()
        }
        $canvas.SetClip($path)
        $canvas.DrawImage($Image, [System.Drawing.RectangleF]::new($X, $Y, $Size, $Size))
    } finally {
        $canvas.Restore($saved)
        $path.Dispose()
    }
}

function Draw-Scene($Image, [float]$X, [float]$Y, [float]$Width, [float]$Height) {
    $canvas.DrawImage($Image, [System.Drawing.RectangleF]::new($X, $Y, $Width, $Height))
}

try {
    $iconA = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'a-returning-tide.png'))
    $iconB = [System.Drawing.Image]::FromFile((Join-Path $PSScriptRoot 'b-unfinished-current.png'))
    $sourceImages += $iconA
    $sourceImages += $iconB
    Draw-Label '未完成 · 潮汐品牌图标 V2' 48 28 $fontTitle $ink
    Draw-Label '两种原创符号方向 / 桌面尺寸与现有开屏配色对照 / 未替换 App 图标' 48 76 $fontBody
    $canvas.DrawLine($line, 48, 116, 1232, 116)
    $canvas.DrawLine($line, 640, 148, 640, 626)

    Draw-Label 'A · 归潮' 76 140 $fontHeading $ink
    Draw-Label '纸船与潮线：一段记忆，被时间带走。' 76 180 $fontBody
    Draw-ImagePreview $iconA 174 226 260
    Draw-Label 'B · 潮汐的未' 710 140 $fontHeading $ink
    Draw-Label '名称与洋流：保留「未」的识别，柔化笔画。' 710 180 $fontBody
    Draw-ImagePreview $iconB 812 226 260

    foreach ($entry in @(@{Image=$iconA; X=128}, @{Image=$iconB; X=766})) {
        Draw-ImagePreview $entry.Image $entry.X 554 48
        Draw-Label '48 px' $entry.X 612 $fontSmall
        Draw-ImagePreview $entry.Image ($entry.X + 100) 538 64
        Draw-Label '64 px' ($entry.X + 100) 612 $fontSmall
        Draw-ImagePreview $entry.Image ($entry.X + 220) 506 96 'circle'
        Draw-Label '圆形 96 px' ($entry.X + 220) 612 $fontSmall
    }

    $canvas.DrawLine($line, 48, 650, 1232, 650)
    Draw-Label '与现有 UI 的联系' 48 671 $fontHeading $ink
    Draw-Label '同一海水蓝与浅沙色；不插入新的品牌页，也不重做已经认可的退潮动画。' 48 714 $fontBody
    $resourceRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../android/app/src/main/res/drawable-nodpi'))
    $states = @(
        @{ Key='daylight'; Title='日光' },
        @{ Key='slant'; Title='斜阳' },
        @{ Key='night'; Title='夜色' }
    )
    $sceneIndex = 0
    foreach ($state in $states) {
        $sea = [System.Drawing.Image]::FromFile((Join-Path $resourceRoot ('sea_' + $state.Key + '.png')))
        $shore = [System.Drawing.Image]::FromFile((Join-Path $resourceRoot ('shore_' + $state.Key + '.png')))
        $sourceImages += $sea
        $sourceImages += $shore
        $sceneX = 168 + $sceneIndex * 380
        Draw-Scene $sea $sceneX 770 88 176
        Draw-Scene $shore ($sceneX + 134) 770 88 176
        Draw-Label '→' ($sceneX + 100) 835 $fontHeading
        Draw-Label ($state.Title + '：开屏 → 首页') $sceneX 958 $fontSmall
        $sceneIndex++
    }
    Draw-Label '原图经等比缩放与预览裁切，不修改图标像素；此图不是 Android 自适应图层或真机截图。' 48 1011 $fontSmall
    $board.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    foreach ($sourceImage in $sourceImages) { $sourceImage.Dispose() }
    $fontTitle.Dispose()
    $fontHeading.Dispose()
    $fontBody.Dispose()
    $fontSmall.Dispose()
    $ink.Dispose()
    $muted.Dispose()
    $line.Dispose()
    $canvas.Dispose()
    $board.Dispose()
}
