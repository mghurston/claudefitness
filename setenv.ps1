# ASCENDANT - portable toolchain activation (PowerShell)
# Sets JAVA_HOME / ANDROID_HOME for THIS SESSION ONLY.
# Does NOT modify system/user environment, registry, or other projects.
# Usage:  . .\setenv.ps1     (note the leading dot to dot-source it)

$proj = $PSScriptRoot
$env:JAVA_HOME    = Join-Path $proj 'jdk'
$env:ANDROID_HOME = Join-Path $proj 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
# Keep Gradle's distribution + caches project-local (not in C:\Users\...\.gradle)
$env:GRADLE_USER_HOME = Join-Path $proj '.gradle-home'

$env:PATH = @(
    (Join-Path $env:JAVA_HOME 'bin'),
    (Join-Path $env:ANDROID_HOME 'platform-tools'),
    (Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin'),
    $env:PATH
) -join ';'

Write-Host "ASCENDANT toolchain active (this session only):" -ForegroundColor Green
Write-Host "  JAVA_HOME    = $env:JAVA_HOME"
Write-Host "  ANDROID_HOME = $env:ANDROID_HOME"
