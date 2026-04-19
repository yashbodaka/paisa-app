$ErrorActionPreference = "Stop"

. "$PSScriptRoot\ensure-java.ps1"
$env:GRADLE_USER_HOME = Join-Path (Get-Location) ".gradle"

if (-not (Test-Path "local.properties")) {
    Write-Host "local.properties was not found."
    Write-Host "Open the project in Android Studio once, or create local.properties with:"
    Write-Host "sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk"
    exit 1
}

.\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "APK created at $apkPath"
}
