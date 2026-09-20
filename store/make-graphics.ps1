Add-Type -AssemblyName System.Drawing

function New-Clock($g, [float]$cx, [float]$cy, [float]$scale) {
    $black = [System.Drawing.Color]::FromArgb(26,26,26)
    $ring = New-Object System.Drawing.Pen($black, (9 * $scale))
    $g.DrawEllipse($ring, $cx - 25*$scale, $cy - 25*$scale, 50*$scale, 50*$scale)
    $tick = New-Object System.Drawing.Pen($black, (5 * $scale))
    $g.DrawLine($tick, $cx, $cy - 21*$scale, $cx, $cy - 16*$scale)
    $g.DrawLine($tick, $cx, $cy + 16*$scale, $cx, $cy + 21*$scale)
    $g.DrawLine($tick, $cx - 21*$scale, $cy, $cx - 16*$scale, $cy)
    $g.DrawLine($tick, $cx + 16*$scale, $cy, $cx + 21*$scale, $cy)
    $hand = New-Object System.Drawing.Pen($black, (8 * $scale))
    $hand.StartCap = 'Round'; $hand.EndCap = 'Round'
    $g.DrawLine($hand, $cx, $cy, $cx, $cy - 14*$scale)
    $g.DrawLine($hand, $cx, $cy, $cx + 10*$scale, $cy + 7*$scale)
}

function New-Canvas($w, $h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.Clear([System.Drawing.Color]::FromArgb(255,196,0))
    return @($bmp, $g)
}

$dir = $PSScriptRoot

# 512x512 store icon
$bmp, $g = New-Canvas 512 512
New-Clock $g 256 256 (512/108)
$bmp.Save((Join-Path $dir 'icon-512.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()

# 1024x500 feature graphic
$bmp, $g = New-Canvas 1024 500
New-Clock $g 250 250 3.6
$black = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(26,26,26))
$title = New-Object System.Drawing.Font('Arial Black', 72, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$sub = New-Object System.Drawing.Font('Arial', 34, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString('TradeHours', $title, $black, 440, 150)
$g.DrawString('Log your week. Copy. Send.', $sub, $black, 446, 262)
$bmp.Save((Join-Path $dir 'feature-graphic-1024x500.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Output 'done'
