$ErrorActionPreference = "Stop"

. "$PSScriptRoot\ensure-java.ps1"
$env:GRADLE_USER_HOME = Join-Path (Get-Location) ".gradle"

$sdkDir = Join-Path (Get-Location) ".android-sdk"
$adb = Join-Path $sdkDir "platform-tools\adb.exe"
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $adb)) {
    Write-Host "adb was not found at $adb"
    Write-Host "Run this first:"
    Write-Host ".\scripts\setup-android-sdk.ps1"
    exit 1
}

if (-not (Test-Path $apkPath)) {
    Write-Host "Debug APK not found. Building it now..."
    .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host "Checking connected Android devices..."
& $adb devices

Write-Host ""
Write-Host "If your device says unauthorized, unlock your phone and accept the USB debugging popup."
Write-Host "Installing $apkPath..."
& $adb install -r $apkPath

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Installed. Open Paisa from your phone's app drawer."

