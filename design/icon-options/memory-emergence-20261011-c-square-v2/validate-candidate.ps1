[CmdletBinding()]
param([string]$SdkRoot = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }))
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($SdkRoot)) { throw 'Set ANDROID_HOME or ANDROID_SDK_ROOT, or pass -SdkRoot.' }
$taskAapt = Join-Path $SdkRoot 'build-tools\35.0.0\aapt2.exe'
$taskFramework = Join-Path $SdkRoot 'platforms\android-35\android.jar'
$taskCandidate = Join-Path $PSScriptRoot 'android-candidate\res'
$taskValidation = Join-Path $PSScriptRoot 'validation'
if (-not (Test-Path -LiteralPath $taskAapt)) { throw 'AAPT2 not found' }
if (-not (Test-Path -LiteralPath $taskFramework)) { throw 'Android framework not found' }
New-Item -ItemType Directory -Path $taskValidation -Force | Out-Null
$taskCompileZip = Join-Path $taskValidation 'compiled-resources.zip'
$taskManifest = Join-Path $PSScriptRoot 'validation-manifest.xml'
$taskApk = Join-Path $taskValidation 'resource-validation-only.apk'
& $taskAapt compile --dir $taskCandidate -o $taskCompileZip
if ($LASTEXITCODE -ne 0) { throw 'Candidate resource compile failed' }
& $taskAapt link -I $taskFramework --manifest $taskManifest --min-sdk-version 26 --target-sdk-version 35 -o $taskApk $taskCompileZip
if ($LASTEXITCODE -ne 0) { throw 'Candidate resource link failed' }
& $taskAapt dump resources $taskApk
if ($LASTEXITCODE -ne 0) { throw 'Candidate resource dump failed' }
'PASS: candidate resources compiled and linked. This is an unsigned resource-only validation artifact; no Activity and no installation.'
