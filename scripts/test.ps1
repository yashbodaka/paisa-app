$ErrorActionPreference = "Stop"

. "$PSScriptRoot\ensure-java.ps1"
$env:GRADLE_USER_HOME = Join-Path (Get-Location) ".gradle"

.\gradlew.bat :app:testDebugUnitTest
exit $LASTEXITCODE
