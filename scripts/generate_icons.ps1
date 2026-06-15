Add-Type -AssemblyName System.Drawing

$src = "D:\github\iossleep\iosSleep\iosSleep\Resources\Assets.xcassets\AppIcon.appiconset\AppIcon.png"
$resRoot = "D:\github\androidsleep\androidsleep\app\src\main\res"

$srcImg = [System.Drawing.Image]::FromFile($src)

# Compute average color for adaptive icon background
$bmpSmall = New-Object System.Drawing.Bitmap 16,16
$gSmall = [System.Drawing.Graphics]::FromImage($bmpSmall)
$gSmall.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gSmall.DrawImage($srcImg, 0, 0, 16, 16)
$gSmall.Dispose()

$rSum = 0L; $gSum = 0L; $bSum = 0L; $count = 0
for ($x = 0; $x -lt 16; $x++) {
    for ($y = 0; $y -lt 16; $y++) {
        $p = $bmpSmall.GetPixel($x, $y)
        $rSum += $p.R; $gSum += $p.G; $bSum += $p.B
        $count++
    }
}
$avgR = [int]($rSum / $count)
$avgG = [int]($gSum / $count)
$avgB = [int]($bSum / $count)
$bgHex = ('#{0:X2}{1:X2}{2:X2}' -f $avgR, $avgG, $avgB)
Write-Host "Average background color: $bgHex"
$bmpSmall.Dispose()

function Resize-Square {
    param($image, $size, $outPath)
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($image, 0, 0, $size, $size)
    $g.Dispose()
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Resize-Round {
    param($image, $size, $outPath)
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $size, $size)
    $g.SetClip($path)
    $g.DrawImage($image, 0, 0, $size, $size)
    $g.Dispose()
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $path.Dispose()
}

function Resize-Foreground {
    # Place the source image scaled to ~66% centered on a transparent canvas (adaptive icon safe zone)
    param($image, $size, $outPath)
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $inner = [int]($size * 0.66)
    $offset = [int](($size - $inner) / 2)
    $g.DrawImage($image, $offset, $offset, $inner, $inner)
    $g.Dispose()
    $bmp.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# density => (legacy icon size, foreground size)
$densities = @{
    "mdpi"    = @(48, 108)
    "hdpi"    = @(72, 162)
    "xhdpi"   = @(96, 216)
    "xxhdpi"  = @(144, 324)
    "xxxhdpi" = @(192, 432)
}

foreach ($d in $densities.Keys) {
    $sizes = $densities[$d]
    $legacySize = $sizes[0]
    $fgSize = $sizes[1]
    $dir = Join-Path $resRoot "mipmap-$d"
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

    Resize-Square $srcImg $legacySize (Join-Path $dir "ic_launcher.png")
    Resize-Round $srcImg $legacySize (Join-Path $dir "ic_launcher_round.png")
    Resize-Foreground $srcImg $fgSize (Join-Path $dir "ic_launcher_foreground.png")
}

$srcImg.Dispose()

# Write background color to colors.xml
$valuesDir = Join-Path $resRoot "values"
$colorsPath = Join-Path $valuesDir "colors.xml"
$colorsXml = @"
<resources>
    <color name="ic_launcher_background">$bgHex</color>
</resources>
"@
Set-Content -Path $colorsPath -Value $colorsXml -Encoding utf8

# Write adaptive icon XML
$anydpiDir = Join-Path $resRoot "mipmap-anydpi-v26"
if (-not (Test-Path $anydpiDir)) { New-Item -ItemType Directory -Force -Path $anydpiDir | Out-Null }

$adaptiveXml = @"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"@
Set-Content -Path (Join-Path $anydpiDir "ic_launcher.xml") -Value $adaptiveXml -Encoding utf8
Set-Content -Path (Join-Path $anydpiDir "ic_launcher_round.xml") -Value $adaptiveXml -Encoding utf8

Write-Host "Done generating icons."
