[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Label,
    [Parameter(Mandatory)][string]$TraceProcessor
)
$ErrorActionPreference = 'Stop'
$root = Join-Path (Split-Path -Parent $PSScriptRoot) "debug/NATIVE-20261012-11-PERF-01/$Label"
$frameSql = @'
INCLUDE PERFETTO MODULE android.frames.per_frame_metrics;
SELECT f.frame_id, u.ts ts, f.dur / 1e6 wall_ms,
       (r.ts+r.dur-u.ts) / 1e6 cpu_ms,
       (max(actual.ts+actual.dur,r.ts+r.dur)-e.ts-e.dur) / 1e6 overrun_ms,
       c.do_frame_dur / 1e6 ui_ms, c.draw_frame_dur / 1e6 render_ms,
       c.app_vsync_delay / 1e6 vsync_delay_ms,
       e.dur / 1e6 budget_ms,
       (max(actual.ts+actual.dur,r.ts+r.dur)-e.ts) / 1e6 full_ms,
       CASE WHEN u.ts >= a.ts AND u.ts < a.ts+a.dur THEN 'animation'
            WHEN u.ts >= a.ts+a.dur THEN 'home' ELSE 'initialization' END phase
FROM android_frames f
JOIN android_frame_stats s USING(frame_id)
JOIN android_cpu_time_per_frame c USING(frame_id)
JOIN slice u ON u.id=f.do_frame_id
JOIN slice r ON r.id=f.draw_frame_id
JOIN actual_frame_timeline_slice actual ON actual.id=f.actual_frame_timeline_id
LEFT JOIN expected_frame_timeline_slice e ON e.id = f.expected_frame_timeline_id
LEFT JOIN (SELECT min(ts) ts, dur FROM slice
           WHERE name='Opening.Animation' AND dur>0) a ON 1=1
WHERE f.process_name = 'com.unfinished.app' AND u.dur>0 AND r.dur>0 AND actual.dur>0 AND e.dur>0
ORDER BY u.ts
'@
$slicesSql = @'
SELECT slice.name, slice.ts, slice.dur/1e6 ms, thread.name thread_name
FROM slice JOIN thread_track ON thread_track.id=slice.track_id
JOIN thread USING(utid) JOIN process USING(upid)
WHERE process.name='com.unfinished.app' AND slice.dur>0
  AND (slice.name IN ('AndroidOwner:measureAndLayout','Opening.RevealField.Build',
      'Opening.Font.Preload','decodeBitmap','Creating EGLContext','shader_compile',
      'Compose:recompose','TextLayout:initLayout','flush layers')
      OR slice.name LIKE 'Choreographer#doFrame%')
ORDER BY slice.ts
'@
function Percentile($values, $p) {
    $sorted = @($values | Sort-Object)
    if (!$sorted.Count) { return $null }
    return [double]$sorted[[Math]::Max(0,[Math]::Ceiling($p * $sorted.Count)-1)]
}
$runs = foreach ($trace in Get-ChildItem -LiteralPath $root -Filter '*.perfetto-trace' -Recurse) {
    $raw = & $TraceProcessor query $trace.FullName $frameSql 2> ($trace.FullName + '.parser.txt')
    if ($LASTEXITCODE -ne 0) { throw "Frame query failed: $trace" }
    $raw | Set-Content ($trace.FullName + '.frames.csv')
    $frames = @($raw | ConvertFrom-Csv)
    if ($frames.Count -lt 10) { throw "Insufficient app frames: $trace" }
    $slices = & $TraceProcessor query $trace.FullName $slicesSql 2> ($trace.FullName + '.slices-parser.txt')
    if ($LASTEXITCODE -ne 0) { throw "Slice query failed: $trace" }
    $slices | Set-Content ($trace.FullName + '.slices.csv')
    $parsed = @($slices | ConvertFrom-Csv)
    $first = @($frames | Select-Object -First 2)
    $animation = @($frames | Where-Object { $_.phase -eq 'animation' -and [double]$_.wall_ms -gt 0 })
    $initialization = @($frames | Where-Object {$_.phase -eq 'initialization'})
    $firstHome = @($frames | Where-Object phase -eq 'home' | Select-Object -First 1)
    # Baseline has no animation marker. After the field completes, preserve the
    # original 350ms hold; these frames are a conservative scene-ready subset,
    # NOT an exact animation-only verdict. Final traces use explicit markers.
    $field = @($parsed | Where-Object name -eq 'Opening.RevealField.Build' | Select-Object -First 1)
    $cutoff = if ($field.Count) { [double]$field[0].ts + ([double]$field[0].ms + 350) * 1e6 } else { [double]::PositiveInfinity }
    $steady = @($frames | Where-Object { [double]$_.ts -ge $cutoff -and [double]$_.wall_ms -gt 0 })
    [pscustomobject]@{
        Serial = $trace.Directory.Name; Trace = $trace.Name; Frames = $frames.Count
        FirstTwoCpuMax = ($first | ForEach-Object {[double]$_.cpu_ms} | Measure-Object -Maximum).Maximum
        FirstTwoUiMax = ($first | ForEach-Object {[double]$_.ui_ms} | Measure-Object -Maximum).Maximum
        LayoutMax = ($parsed | Where-Object name -eq 'AndroidOwner:measureAndLayout' | ForEach-Object {[double]$_.ms} | Measure-Object -Maximum).Maximum
        SceneReadyFrames = $steady.Count
        SceneReadyDeadlinePercent = if ($steady.Count) { 100 * @($steady | Where-Object {[double]$_.overrun_ms -le 0}).Count / $steady.Count } else {$null}
        SceneReadyOverrunP99 = Percentile @($steady | ForEach-Object {[double]$_.overrun_ms}) 0.99
        SceneReadyWallMax = ($steady | ForEach-Object {[double]$_.wall_ms} | Measure-Object -Maximum).Maximum
        InitAllCpuMax = ($initialization | ForEach-Object {[double]$_.cpu_ms} | Measure-Object -Maximum).Maximum
        AnimationFrames = $animation.Count
        AnimationDeadlinePercent = if ($animation.Count) { 100 * @($animation | Where-Object {[double]$_.overrun_ms -le 0}).Count / $animation.Count } else {$null}
        AnimationOverrunP99 = Percentile @($animation | ForEach-Object {[double]$_.overrun_ms}) 0.99
        AnimationWallMax = ($animation | ForEach-Object {[double]$_.wall_ms} | Measure-Object -Maximum).Maximum
        AnimationCpuP99 = Percentile @($animation | ForEach-Object {[double]$_.cpu_ms}) 0.99
        FirstHomeFrameAfterFirstFrame = if ($firstHome.Count) { ([double]$firstHome[0].ts - [double]$frames[0].ts) / 1e6 } else {$null}
    }
}
$runs | Export-Csv -NoTypeInformation -Path (Join-Path $root 'runs.csv')
$runs | Group-Object Serial | ForEach-Object {
    [pscustomobject]@{
        Serial=$_.Name; Runs=$_.Count
        InitCpuP95=Percentile @($_.Group.InitAllCpuMax) 0.95
        InitCpuMax=($_.Group.InitAllCpuMax | Measure-Object -Maximum).Maximum
        InitUiP95=Percentile @($_.Group.FirstTwoUiMax) 0.95
        LayoutP95=Percentile @($_.Group.LayoutMax) 0.95
        ReadyDeadlineAverage=($_.Group.SceneReadyDeadlinePercent | Measure-Object -Average).Average
        ReadyWallMax=($_.Group.SceneReadyWallMax | Measure-Object -Maximum).Maximum
    }
} | Format-Table -AutoSize
