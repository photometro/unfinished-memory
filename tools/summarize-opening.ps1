[CmdletBinding()]
param([Parameter(Mandatory)][string[]]$Labels)
$ErrorActionPreference = 'Stop'
$root = Join-Path (Split-Path -Parent $PSScriptRoot) 'debug/NATIVE-20261012-11-PERF-01'
function P($values, $fraction) {
    $sorted = @($values | Sort-Object)
    if (!$sorted.Count) { return $null }
    [double]$sorted[[Math]::Ceiling($fraction * $sorted.Count)-1]
}
function Maximum($values) { ($values | Measure-Object -Maximum).Maximum }
$results = foreach ($label in $Labels) {
    $directory = Join-Path $root $label
    $runs = @(Import-Csv (Join-Path $directory 'runs.csv'))
    foreach ($device in $runs | Group-Object Serial) {
        $frames = @(Get-ChildItem (Join-Path $directory $device.Name) -Filter '*.frames.csv' |
            ForEach-Object { Import-Csv $_.FullName })
        $animation = @($frames | Where-Object phase -eq animation)
        $times = @(Get-ChildItem (Join-Path $directory $device.Name) -Filter '*-launch.txt' |
            ForEach-Object {
                $launch = Get-Content $_.FullName -Raw
                if ($launch -notmatch 'LaunchState: COLD') { throw "Not cold: $($_.Name)" }
                if ($launch -match 'TotalTime:\s+(\d+)') { [double]$Matches[1] }
            })
        $intervals = @()
        foreach ($frameFile in Get-ChildItem (Join-Path $directory $device.Name) -Filter '*.frames.csv') {
            $moving = @(Import-Csv $frameFile.FullName | Where-Object phase -eq animation)
            for ($i=1; $i -lt $moving.Count; $i++) {
                $delta = ([double]$moving[$i].ts - [double]$moving[$i-1].ts)/1e6
                if ($delta -gt 1 -and $delta -lt 50) { $intervals += $delta }
            }
        }
        [pscustomobject][ordered]@{
            Label = $label; Serial = $device.Name; Runs = $device.Count
            InitCpuP50 = P @($device.Group.InitAllCpuMax | ForEach-Object {[double]$_}) .5
            InitCpuP95 = P @($device.Group.InitAllCpuMax | ForEach-Object {[double]$_}) .95
            InitCpuMax = Maximum @($device.Group.InitAllCpuMax | ForEach-Object {[double]$_})
            FirstLaunchCpuMax = [double]($device.Group | Sort-Object Trace | Select-Object -First 1).InitAllCpuMax
            LayoutP95 = P @($device.Group.LayoutMax | ForEach-Object {[double]$_}) .95
            TotalTimeP50 = P $times .5; TotalTimeP95 = P $times .95
            AnimationFrames = $animation.Count
            AnimationDeadlinePercent = if ($animation.Count) { 100 * @($animation | Where-Object {[double]$_.overrun_ms -le 0}).Count/$animation.Count } else {$null}
            AnimationWorstRunPercent = if ($animation.Count) { ($device.Group.AnimationDeadlinePercent | ForEach-Object {[double]$_} | Measure-Object -Minimum).Minimum } else {$null}
            AnimationOverrunP99 = P @($animation | ForEach-Object {[double]$_.overrun_ms}) .99
            AnimationCpuMax = Maximum @($animation | ForEach-Object {[double]$_.cpu_ms})
            AnimationWallMax = Maximum @($animation | ForEach-Object {[double]$_.wall_ms})
            AnimationUiIntervalMedian = P $intervals .5
            SceneReadyDeadlineMean = ($device.Group.SceneReadyDeadlinePercent | ForEach-Object {[double]$_} | Measure-Object -Average).Average
            SceneReadyWallMax = Maximum @($device.Group.SceneReadyWallMax | ForEach-Object {[double]$_})
        }
    }
}
$results | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $root 'comparison.json')
$results | Format-Table Label, Serial, Runs, InitCpuP95, InitCpuMax, TotalTimeP50, AnimationDeadlinePercent, AnimationWorstRunPercent, AnimationOverrunP99, AnimationCpuMax -AutoSize
