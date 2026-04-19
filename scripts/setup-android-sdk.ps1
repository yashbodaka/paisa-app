$ErrorActionPreference = "Stop"

$sdkDir = Join-Path (Get-Location) ".android-sdk"
$zipPath = Join-Path $sdkDir "commandlinetools-win.zip"
$tempDir = Join-Path $sdkDir "_temp"
$cmdlineToolsDir = Join-Path $sdkDir "cmdline-tools"
$latestDir = Join-Path $cmdlineToolsDir "latest"
$sdkManager = Join-Path $latestDir "bin\sdkmanager.bat"
$toolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip"

Write-Host "This installs Android command-line tools into:"
Write-Host $sdkDir
Write-Host ""
Write-Host "You will be asked to accept the Android SDK licenses."
Write-Host "Press Ctrl+C now if you do not want to continue."
Pause

New-Item -ItemType Directory -Force -Path $sdkDir | Out-Null
New-Item -ItemType Directory -Force -Path $cmdlineToolsDir | Out-Null

if (-not (Test-Path $zipPath)) {
    Write-Host "Downloading Android command-line tools..."
    Invoke-WebRequest -Uri $toolsUrl -OutFile $zipPath
}

if (Test-Path $tempDir) {
    Remove-Item -LiteralPath $tempDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

Write-Host "Extracting command-line tools..."
Expand-Archive -Path $zipPath -DestinationPath $tempDir -Force

if (Test-Path $latestDir) {
    Remove-Item -LiteralPath $latestDir -Recurse -Force
}
Move-Item -LiteralPath (Join-Path $tempDir "cmdline-tools") -Destination $latestDir
Remove-Item -LiteralPath $tempDir -Recurse -Force

$sdkDirForProperties = $sdkDir.Replace("\", "/")
Set-Content -Path "local.properties" -Value "sdk.dir=$sdkDirForProperties"

Write-Host "Accept Android SDK licenses when prompted."
& $sdkManager --sdk_root=$sdkDir --licenses

Write-Host "Installing Android SDK packages..."
& $sdkManager --sdk_root=$sdkDir "platform-tools" "platforms;android-35" "build-tools;35.0.0"

Write-Host "Android SDK is ready."
Write-Host "Next:"
Write-Host ".\scripts\test.ps1"
Write-Host ".\scripts\build-debug-apk.ps1"

