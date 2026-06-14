# ASCENDANT - launch the app in the Android emulator for hands-on testing.
# Everything is project-local; nothing touches your system or other projects.
# Run from this folder:   .\test-app.ps1
# (To rebuild the APK first, pass -Rebuild:   .\test-app.ps1 -Rebuild)

param([switch]$Rebuild)

$ErrorActionPreference = 'Stop'
$proj = $PSScriptRoot

# --- activate the project-local toolchain (this session only) ---
. (Join-Path $proj 'setenv.ps1')
$env:ANDROID_AVD_HOME = Join-Path $proj '.avd'

$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$emu = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$apk = Join-Path $proj 'app\build\outputs\apk\debug\app-debug.apk'

if ($Rebuild -or -not (Test-Path $apk)) {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    & (Join-Path $proj 'gradlew.bat') assembleDebug
}

# --- start the emulator WITH a window (unless one is already running) ---
$running = (& $adb devices) -match 'emulator-\d+\s+device'
if (-not $running) {
    Write-Host "Starting emulator (a window will open)..." -ForegroundColor Cyan
    Start-Process -FilePath $emu -ArgumentList @(
        '-avd','ascendant_test','-no-snapshot','-gpu','auto'
    )
} else {
    Write-Host "Emulator already running." -ForegroundColor Green
}

Write-Host "Waiting for the emulator to finish booting..." -ForegroundColor Cyan
& $adb wait-for-device
do {
    Start-Sleep -Seconds 3
    $booted = (& $adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
} while ($booted -ne '1')

Write-Host "Installing ASCENDANT..." -ForegroundColor Cyan
& $adb install -r $apk | Out-Null

Write-Host "Launching ASCENDANT..." -ForegroundColor Cyan
& $adb shell am start -n com.mhurston.ascendant/.MainActivity | Out-Null

Write-Host ""
Write-Host "ASCENDANT is running in the emulator window. Click around to test it!" -ForegroundColor Green
Write-Host "When done, just close the emulator window (or run:  adb emu kill)." -ForegroundColor DarkGray
