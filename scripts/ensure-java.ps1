$javaExe = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME "bin\java.exe"
} else {
    $null
}

if ($javaExe -and (Test-Path $javaExe)) {
    return
}

$candidateRoots = @(
    "C:\Program Files\Java",
    "C:\Program Files\Eclipse Adoptium",
    "C:\Program Files\Microsoft",
    "$env:LOCALAPPDATA\Programs\Java"
) | Where-Object { $_ -and (Test-Path $_) }

$candidates = $candidateRoots |
    ForEach-Object { Get-ChildItem -Path $_ -Directory -ErrorAction SilentlyContinue } |
    Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") }

$candidate = $candidates |
    Where-Object { $_.Name -match "jdk" } |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $candidate) {
    $candidate = $candidates |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

if ($candidate) {
    $env:JAVA_HOME = $candidate.FullName
    Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
    return
}

Write-Host "A valid JDK was not found."
Write-Host "Install Android Studio or JDK 17+, then set JAVA_HOME to that JDK folder."
Write-Host "Example: `$env:JAVA_HOME='C:\Program Files\Java\jdk-22'"
exit 1
