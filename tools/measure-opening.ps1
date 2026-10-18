[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Serial,
    [Parameter(Mandatory)][string]$Label,
    [ValidateRange(1,100)][int]$Iterations = 5,
    [string]$Adb = 'adb'
)
$ErrorActionPreference = 'Stop'
if ($Serial -notmatch '^[a-zA-Z0-9]+$' -or $Label -notmatch '^[a-zA-Z0-9-]+$') { throw 'Unsafe serial or label' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$runRoot = Join-Path $projectRoot 'debug/NATIVE-20261012-11-PERF-01'
$output = Join-Path $runRoot "$Label/$Serial"
New-Item -ItemType Directory -Force -Path $output | Out-Null
$config = '/data/local/tmp/unfinished-opening-perfetto.pbtxt'
& $Adb -s $Serial push (Join-Path $PSScriptRoot 'opening-perfetto.pbtxt') $config | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Config push failed' }
$identity = & $Adb -s $Serial shell dumpsys package com.unfinished.app
$identity | Select-String 'codePath=|versionCode=|versionName=|pkgFlags=|lastUpdateTime=' | Set-Content (Join-Path $output 'package.txt')
for ($iteration = 0; $iteration -lt $Iterations; $iteration++) {
    $name = 'cold-{0:d3}' -f $iteration
    & $Adb -s $Serial shell am force-stop com.unfinished.app
    & $Adb -s $Serial shell input keyevent KEYCODE_HOME
    Start-Sleep -Milliseconds 500
    $remote = "/data/misc/perfetto-traces/unfinished-opening-$Serial-$name.perfetto-trace"
    $pidText = & $Adb -s $Serial shell "cat $config | perfetto --txt -c - -o $remote --background-wait"
    if ($LASTEXITCODE -ne 0) { throw "Trace start failed: $pidText" }
    $launch = & $Adb -s $Serial shell am start -W -n com.unfinished.app/.MainActivity
    $launch | Set-Content (Join-Path $output "$name-launch.txt")
    if ($LASTEXITCODE -ne 0 -or ($launch -join ' ') -notmatch 'Status: ok') { throw "Launch failed: $launch" }
    Start-Sleep -Seconds 6
    & $Adb -s $Serial pull $remote (Join-Path $output "$name.perfetto-trace") | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Trace pull failed' }
    # Only remove this exact scratch trace, never a directory or app data.
    & $Adb -s $Serial shell rm $remote
    Write-Output "$Label / $Serial / $($iteration + 1) of $Iterations captured"
}
