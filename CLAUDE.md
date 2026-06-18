# ASCENDANT — operational facts (read before building/running)

Anime fitness RPG, Kotlin + Jetpack Compose. Package `com.mhurston.ascendant`.
Toolchain is **fully project-local** inside `G:\claudefitness` — nothing on system PATH/registry.

## Every PowerShell call must start by activating the toolchain
```powershell
. .\setenv.ps1
```
Env does NOT persist between tool calls. A command run without this has a blank
`$env:ANDROID_HOME` and will give false results (e.g. `Test-Path "$env:ANDROID_HOME\..."`
returns False). **Never report a negative from an un-sourced shell as fact.**

## There IS an emulator — never say "no emulator/device available"
- AVD name: **`ascendant_test`**, stored at the **custom path `G:\claudefitness\.avd`**.
- `setenv.ps1` does NOT set `ANDROID_AVD_HOME`, so `emulator -list-avds` returns EMPTY
  unless you set it first. Empty ≠ none — the AVD home is just wrong.

Boot (run_in_background):
```powershell
. .\setenv.ps1; $env:ANDROID_AVD_HOME="G:\claudefitness\.avd"
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd ascendant_test -no-snapshot-load -no-boot-anim
```
Then build + install + launch:
```powershell
. .\setenv.ps1; $adb="$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb wait-for-device; do { Start-Sleep 2; $b=(& $adb shell getprop sys.boot_completed).Trim() } until ($b -eq "1")
.\gradlew.bat :app:assembleDebug
& $adb install -r -d "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell monkey -p com.mhurston.ascendant -c android.intent.category.LAUNCHER 1
```

## Screenshots
Use screencap + pull. Do NOT use `adb exec-out screencap -p > file.png` — PowerShell `>`
writes UTF-16/BOM and corrupts the PNG.
```powershell
& $adb shell screencap -p /sdcard/s.png; & $adb pull /sdcard/s.png "G:\claudefitness\shot.png"
```
Bottom-nav tabs (1080-wide screen, y≈2230): Hero x98 · Train x324 · Trophies x540 · Log x756 · Energy x972.
Navigate with `adb shell input tap/swipe/text`. Delete temp `shot*.png` from repo root when done.

## Testing steps / passive activity — turn Health Connect sync OFF first
The emulator has NO Health Connect step data. If passive sync is ON, every app
foreground re-syncs and banks HC's authoritative **0** over today's `passiveSteps`/
`passiveKcal` — so any injected/observed steps reset to 0 (Steps ring + tracked-walking
vanish). **Before testing anything step-related, disable passive sync** (Energy tab toggle).
This is expected overwrite-not-add behavior, not a bug.

To force a step value for UI testing (debuggable build, has `/system/bin/sqlite3`):
```bash
ADB=/g/claudefitness/android-sdk/platform-tools/adb.exe
"$ADB" shell am force-stop com.mhurston.ascendant            # release the DB lock
"$ADB" shell "run-as com.mhurston.ascendant sqlite3 databases/ascendant.db \
  \"UPDATE workout_day SET passiveSteps=7500,passiveKcal=320 WHERE date='<today ISO>'; PRAGMA wal_checkpoint(TRUNCATE);\""
```
DB: `ascendant.db`, table `workout_day` (PK `date` = ISO yyyy-MM-dd). Revert to 0 when done
so test data doesn't inflate XP/level. (Use the Bash tool, not PowerShell — `/sdcard` &
`(*)` get mangled by PS/MSYS path conversion.)

## Verification rules
- The user CANNOT see my screenshots — he watches the emulator window himself.
  **Leave the emulator running** after checks; don't `adb emu kill`. Use my screenshots
  to catch regressions, not as proof to him.
- Build green check: `BUILD SUCCESSFUL`. APK at `app\build\outputs\apk\debug\app-debug.apk`.

Deeper notes live in `C:\Users\mghur\.claude\projects\G--claudefitness\memory\` (toolchain,
emulator-runbook). This file is the always-loaded backstop so they can't be missed.
