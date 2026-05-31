#!/usr/bin/env pwsh
<#
.SYNOPSIS
  Downloads the lightweight Vosk Indian English/Hindi model.
#>

$ErrorActionPreference = "Stop"

$outDir = Join-Path $PSScriptRoot "..\app\src\main\assets"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# URL for vosk-model-small-en-in-0.4 (Verified working)
$modelUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
$zipPath = Join-Path $outDir "vosk-model.zip"

Write-Host "[DOWNLOAD] Downloading Vosk Indian model..."
Invoke-WebRequest -Uri $modelUrl -OutFile $zipPath -UseBasicParsing

Write-Host "[EXTRACT] This script won't unzip it. Vosk on Android can handle the ZIP or we can unzip it to 'assets/vosk-model'."
Write-Host "For simplicity, the app expects a folder 'assets/vosk-model'."

# Unzipping for the app to use
Write-Host "[UNZIP] Unpacking model..."
Expand-Archive -Path $zipPath -DestinationPath "$outDir\temp_vosk" -Force
$innerDir = Get-ChildItem -Path "$outDir\temp_vosk" | Select-Object -First 1
Move-Item -Path $innerDir.FullName -Destination "$outDir\vosk-model" -Force
Remove-Item -Path "$outDir\temp_vosk" -Recurse -Force
Remove-Item -Path $zipPath -Force

Write-Host ""
Write-Host "Vosk model is ready in assets/vosk-model."
Write-Host "Now rebuild and install the app."
