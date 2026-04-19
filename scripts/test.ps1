$ErrorActionPreference = "Stop"

if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME is not set. Android Studio usually provides a bundled JDK, or install JDK 17+."
}

.\gradlew.bat :app:testDebugUnitTest

