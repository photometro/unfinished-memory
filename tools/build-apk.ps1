[CmdletBinding()]
param(
    [ValidateSet('debug', 'release')]
    [string]$Mode = 'debug',
    [switch]$AllowNetwork,
    [switch]$Install
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $projectRoot 'android'
$debugRoot = Join-Path $projectRoot 'debug'
$lockPath = Join-Path $debugRoot '.apk-build.lock'
$startedAt = Get-Date
$stamp = $startedAt.ToString('yyyyMMdd-HHmmss')
$logPath = Join-Path $debugRoot "apk-build-$stamp.log"
$summaryPath = Join-Path $debugRoot 'apk-build-summary.json'
$exitCode = 1
$apkPath = $null
$errorText = $null
$deviceStatus = if ($Install) { 'pending' } else { 'not-run' }
$deviceSerial = $null
$deviceError = $null

New-Item -ItemType Directory -Force -Path $debugRoot | Out-Null

try {
    if (Test-Path -LiteralPath $lockPath) {
        throw "Build lock already exists: $lockPath"
    }
    New-Item -ItemType File -Path $lockPath -ErrorAction Stop | Out-Null

    if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $env:JAVA_HOME = Join-Path $env:USERPROFILE 'Android\jdk-21'
    }
    if ([string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        $env:ANDROID_HOME = Join-Path $env:USERPROFILE 'Android'
    }
    if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
        $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
    }
    if ([string]::IsNullOrWhiteSpace($env:ANDROID_USER_HOME)) {
        $env:ANDROID_USER_HOME = Join-Path $env:USERPROFILE '.android'
    }
    $env:JAVA_TOOL_OPTIONS = "-Duser.home=$env:USERPROFILE"
    if (-not (Test-Path -LiteralPath $env:JAVA_HOME)) {
        throw "JAVA_HOME does not exist: $env:JAVA_HOME. Set JAVA_HOME to a JDK 21 installation."
    }
    if (-not (Test-Path -LiteralPath $env:ANDROID_HOME)) {
        throw "ANDROID_HOME does not exist: $env:ANDROID_HOME. Set ANDROID_HOME to the Android SDK installation."
    }

    "Project: $projectRoot" | Tee-Object -FilePath $logPath
    "Mode: $Mode" | Tee-Object -FilePath $logPath -Append
    "JAVA_HOME: $env:JAVA_HOME" | Tee-Object -FilePath $logPath -Append
    "ANDROID_HOME: $env:ANDROID_HOME" | Tee-Object -FilePath $logPath -Append
    "GRADLE_USER_HOME: $env:GRADLE_USER_HOME" | Tee-Object -FilePath $logPath -Append
    "Node: $((Get-Command node -ErrorAction SilentlyContinue).Source)" | Tee-Object -FilePath $logPath -Append

    $gradlew = Join-Path $androidRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradlew)) {
        throw "Gradle Wrapper not found: $gradlew"
    }

    $task = if ($Mode -eq 'release') { ':app:assembleRelease' } else { ':app:assembleDebug' }
    $gradleArgs = @('--build-cache')
    if (-not $AllowNetwork) {
        $gradleArgs += '--offline'
    }
    Push-Location $androidRoot
    try {
        & $gradlew @gradleArgs $task 2>&1 | Tee-Object -FilePath $logPath -Append
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }

    if ($exitCode -ne 0) {
        throw "Gradle task failed with exit code $exitCode"
    }

    $variant = if ($Mode -eq 'release') { 'release' } else { 'debug' }
    $apkCandidates = @(
        (Join-Path $androidRoot "app\build\outputs\apk\$variant\app-$variant.apk"),
        (Join-Path $androidRoot "app\build\outputs\apk\$variant\app-$variant-unsigned.apk")
    )
    $apkPath = $apkCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $apkPath) {
        throw "Expected APK not found in $($apkCandidates -join ', ')"
    }

    if ($Install) {
        $adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
        if (-not (Test-Path -LiteralPath $adb)) {
            throw "ADB not found: $adb"
        }

        $apksigner = Get-ChildItem -LiteralPath (Join-Path $env:ANDROID_HOME 'build-tools') -Filter apksigner.bat -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
        if (-not $apksigner) {
            throw "apksigner not found under $env:ANDROID_HOME\build-tools"
        }

        $certText = (& $apksigner verify --print-certs $apkPath 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0 -or $certText -notmatch '(?im)certificate SHA-256 digest:\s*([0-9A-F:]+)') {
            throw "Unable to read APK signing certificate: $apkPath"
        }
        $builtCert = $Matches[1].ToUpperInvariant()
        "APK certificate SHA-256: $builtCert" | Tee-Object -FilePath $logPath -Append

        & $adb start-server 2>&1 | Tee-Object -FilePath $logPath -Append | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "ADB server failed to start"
        }
        $deviceLines = @(& $adb devices)
        $online = @($deviceLines | Where-Object { $_ -match '^\S+\s+device(\s|$)' })
        $unauthorized = @($deviceLines | Where-Object { $_ -match '^\S+\s+unauthorized(\s|$)' })
        if ($online.Count -eq 0) {
            if ($unauthorized.Count -gt 0) {
                throw "ADB device is unauthorized. Unlock the phone and accept the USB debugging RSA prompt."
            }
            throw "No online Android device found. Connect a device or start an emulator, then enable USB debugging."
        }
        $deviceSerial = (($online[0] -split '\s+')[0]).Trim()
        "Device serial: $deviceSerial" | Tee-Object -FilePath $logPath -Append

        # If an existing package is present, compare certificates before allowing -r.
        $remotePackage = $null
        $pmPathText = (& $adb -s $deviceSerial shell pm path com.unfinished.app 2>&1 | Out-String)
        if ($pmPathText -match '(?m)^package:(\S+)') {
            $remotePackage = $Matches[1]
        }
        if ($remotePackage) {
            $stagingDir = Join-Path $debugRoot ".device-apk-$stamp"
            $installedApk = Join-Path $stagingDir 'installed.apk'
            New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
            & $adb -s $deviceSerial pull $remotePackage $installedApk 2>&1 | Tee-Object -FilePath $logPath -Append | Out-Null
            if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $installedApk)) {
                throw "Unable to read the installed APK for signature verification: $remotePackage"
            }
            $installedCertText = (& $apksigner verify --print-certs $installedApk 2>&1 | Out-String)
            if ($LASTEXITCODE -ne 0 -or $installedCertText -notmatch '(?im)certificate SHA-256 digest:\s*([0-9A-F:]+)') {
                throw "Unable to read the installed app signing certificate"
            }
            $installedCert = $Matches[1].ToUpperInvariant()
            "Installed certificate SHA-256: $installedCert" | Tee-Object -FilePath $logPath -Append
            if ($builtCert -ne $installedCert) {
                throw "Signing certificate mismatch for com.unfinished.app; refusing to overwrite the installed app."
            }
            Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
        }

        $installOutput = (& $adb -s $deviceSerial install -r $apkPath 2>&1 | Tee-Object -FilePath $logPath -Append | Out-String)
        if ($LASTEXITCODE -ne 0 -or $installOutput -notmatch '(?im)Success') {
            if ($installOutput -match 'INSTALL_FAILED_USER_RESTRICTED') {
                throw "Device rejected ADB installation (INSTALL_FAILED_USER_RESTRICTED). On the phone, enable Developer options > USB installation/通过 USB 安装 for 未完成, keep the device unlocked, then retry."
            }
            throw "ADB install failed for $apkPath`n$installOutput"
        }
        $installedPath = (& $adb -s $deviceSerial shell pm path com.unfinished.app 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0 -or $installedPath -notmatch '(?m)^package:') {
            throw "Package com.unfinished.app was not found after installation"
        }
        $launchOutput = (& $adb -s $deviceSerial shell monkey -p com.unfinished.app -c android.intent.category.LAUNCHER 1 2>&1 | Tee-Object -FilePath $logPath -Append | Out-String)
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to launch com.unfinished.app`n$launchOutput"
        }
        $deviceStatus = 'installed-and-launched'
    }
}
catch {
    $errorText = $_.Exception.Message
    $exitCode = 1
    if ($Install -and $deviceStatus -eq 'pending') {
        $deviceStatus = 'failed'
        $deviceError = $errorText
    }
    throw
}
finally {
    $finishedAt = Get-Date
    $summary = [ordered]@{
        project = $projectRoot
        mode = $Mode
        startedAt = $startedAt.ToString('o')
        finishedAt = $finishedAt.ToString('o')
        exitCode = $exitCode
        apkPath = $apkPath
        apkSha256 = if ($apkPath -and (Test-Path -LiteralPath $apkPath)) { (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash } else { $null }
        logPath = $logPath
        error = $errorText
        browser = 'not-run'
        device = $deviceStatus
        deviceSerial = $deviceSerial
        deviceError = $deviceError
        knowledgeBase = 'not-run'
    }
    $summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
    if (Test-Path -LiteralPath $lockPath) {
        Remove-Item -LiteralPath $lockPath -Force
    }
}
