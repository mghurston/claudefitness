# Deployment Plan

A complete guide to building ASCENDANT and getting it onto **your own phone** with the least friction. Because this is a **personal, single-user, non-distributed** app, the goal is "build → on my phone," not Play Store publishing.

---

## 0. How this project is ACTUALLY built and run (authoritative)

> ⚠️ Sections 1–8 below are the original design-time plan and assume **Android Studio**.
> The real project does **not** use Android Studio. Everything is a **portable, project-local
> toolchain inside `G:\claudefitness`** — nothing installed to the system, PATH, or registry.
> When building or running, follow THIS section; treat the rest as background.

**Toolchain (all under `G:\claudefitness`):** portable Temurin JDK 17 (`jdk\`), Android SDK
(`android-sdk\`: cmdline-tools, platform-tools, platforms;android-35, build-tools;35.0.0),
Gradle wrapper 8.9, project-local Gradle cache (`.gradle-home\`). `local.properties` points
`sdk.dir` at the local SDK.

**Activate (every new shell — env does NOT persist between calls):**
```powershell
. .\setenv.ps1     # dot-sourced; sets JAVA_HOME/ANDROID_HOME/GRADLE_USER_HOME/PATH for this session only
```
A command run without this has a blank `$env:ANDROID_HOME` and gives **false negatives** — never
trust a negative result from an un-sourced shell.

**Build:**
```powershell
.\gradlew.bat assembleDebug      # → app\build\outputs\apk\debug\app-debug.apk  (BUILD SUCCESSFUL)
```

**Emulator — there IS one; never claim "no emulator/device available":**
- AVD: **`ascendant_test`**, stored at the **custom path `G:\claudefitness\.avd`** (NOT `~/.android/avd`).
- `setenv.ps1` does not set `ANDROID_AVD_HOME`, so `emulator -list-avds` looks empty unless you set it.
```powershell
. .\setenv.ps1; $env:ANDROID_AVD_HOME="G:\claudefitness\.avd"
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd ascendant_test -no-snapshot-load -no-boot-anim   # background
# wait for boot, then:
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb wait-for-device; do { Start-Sleep 2; $b=(& $adb shell getprop sys.boot_completed).Trim() } until ($b -eq "1")
& $adb install -r -d "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell monkey -p com.mhurston.ascendant -c android.intent.category.LAUNCHER 1
```
**Screenshots:** `& $adb shell screencap -p /sdcard/s.png; & $adb pull /sdcard/s.png shot.png`
— do NOT use `adb exec-out screencap -p > file.png` (PowerShell `>` corrupts the PNG with UTF-16/BOM).
**Leave the emulator running** after checks — the user watches it himself and cannot see my screenshots.

(See also the always-loaded `CLAUDE.md` at repo root, which mirrors these run rules.)

---

## 1. Development Environment

### Required Tools

| Tool | Purpose | Notes |
|---|---|---|
| **Android Studio** (latest stable, e.g. Ladybug+) | IDE, SDK manager, emulator, build, deploy | Single install bundles most of below |
| **JDK 17+** | Kotlin/Gradle build | Bundled with Android Studio |
| **Android SDK** (API 26–latest) + Platform Tools (`adb`) | Compile + device deploy | Install via Android Studio SDK Manager |
| **Kotlin** + **Gradle** | Language + build system | Bundled / Gradle wrapper |
| **Git** (optional) | Version control / backup of source | Recommended even solo |
| **A physical Android phone** | The deployment target | USB cable or same Wi-Fi for wireless |

### One-Time Setup
1. Install Android Studio → run the setup wizard (installs SDK, platform-tools, an emulator image).
2. SDK Manager → ensure **Android SDK Platform (target API)** + **Android SDK Build-Tools** + **Platform-Tools** are installed.
3. On your phone: **Settings → About → tap Build Number ×7** to enable Developer Options, then enable **USB debugging** (and **Wireless debugging** if desired).

---

## 2. Build Process (step-by-step)

### A. Create / open the project
1. Android Studio → **New Project → Empty Activity (Compose)** → name `Ascendant`, package e.g. `com.mhurston.ascendant`, language **Kotlin**, min SDK **26**.
2. Add dependencies (Room, DataStore, Hilt, WorkManager, Navigation-Compose, Lottie, kotlinx-serialization/datetime, coil) to `build.gradle.kts`.
3. Drop in assets: `achievements.json`, `videos.json`, `seed_history.csv`, Lottie files.

### B. Build a debug build (fastest iteration)
```bash
# from project root (Gradle wrapper)
./gradlew assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```

### C. Build a signed release APK (for a stable install you keep)
1. **Build → Generate Signed Bundle / APK → APK.**
2. Create a keystore once (keep it safe — you'll reuse it for updates):
   ```bash
   keytool -genkey -v -keystore ascendant.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ascendant
   ```
3. Select **release**, finish → produces `app-release.apk`.
   - Or via CLI: `./gradlew assembleRelease` (after configuring `signingConfigs` in Gradle).

> For purely personal use, the **debug APK is fine**; a signed release APK is only worth it if you want a clean, stable, reinstall-safe build.

---

## 3. Phone Installation — Methods Compared

| Method | How | Effort | Best for |
|---|---|---|---|
| **★ Android Studio Run (recommended)** | Plug in phone (or pair wirelessly) → select device → click ▶ Run | Lowest | Day-to-day dev + just getting it on your phone |
| **Direct APK sideload** | Build APK → transfer to phone → tap to install (enable "Install unknown apps") | Low | Installing without a cable / sharing the file to yourself |
| **adb install** | `adb install -r app-release.apk` | Low | Scripted reinstalls |
| **GitHub Actions (CI)** | Push to repo → workflow builds APK → download artifact → sideload | Medium | Automated builds, building without a local toolchain, version history |
| **Play Store Internal Testing** | Upload AAB to Play Console internal track | High (needs $25 dev account, store listing) | Overkill for one user — **not recommended** |

### ✅ Recommended Personal-Use Strategy

**Android Studio "Run" to your phone** is the simplest end-to-end path:
1. Connect phone via USB **or** pair via **Wireless debugging** (Studio → Device Manager → Pair using Wi-Fi / QR).
2. Select your phone in the device dropdown.
3. Click **▶ Run**. Studio builds, installs, and launches the app in ~seconds.
4. To keep a permanent copy independent of the cable: also `assembleRelease` once and sideload the signed APK so it survives even if you stop opening Studio.

This needs **no Play Store, no developer account, no server, no cost.**

---

## 4. Optional: GitHub Actions Build (zero-local-build option)

Useful if you want APKs built in the cloud and version-archived. Example workflow:

```yaml
# .github/workflows/build.yml
name: Build APK
on: { push: { branches: [main] }, workflow_dispatch: {} }
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - name: Build debug APK
        run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: ascendant-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```
Then: Actions tab → run → download the artifact → sideload to phone. (For signed release, add the keystore as encrypted repo secrets.)

> Note: this is a **native Android** build — it does **not** deploy to Vercel. The Vercel tooling available in this environment is irrelevant to an Android APK and is intentionally unused.

---

## 5. Updating the App

1. Make changes → bump `versionCode`/`versionName` in `build.gradle.kts`.
2. Re-run via Android Studio (auto-reinstalls), or `adb install -r app-release.apk` (the `-r` reinstalls keeping data — **as long as it's signed with the same keystore**).
3. Room migrations handle any schema changes so your logged history survives updates.

---

## 6. Data Safety During Deploy/Update

- **Before reinstalling with a different signing key** (which wipes app data), use **Settings → Export** to back up CSV/JSON first.
- Keep the **keystore file** and an **export** in your own cloud (Drive) — these are the only two things you can't regenerate.
- Android Auto Backup (`allowBackup=true`) provides an additional automatic safety net to your Google account.

---

## 7. Deployment Checklist

- [ ] Android Studio + SDK (API 26–latest) + platform-tools installed
- [ ] Phone Developer Options + USB/Wireless debugging enabled
- [ ] Project builds: `./gradlew assembleDebug` succeeds
- [ ] Seed data imports and verifies (30 days, completion parity) on first run
- [ ] Signed release keystore created and **backed up**
- [ ] App installs & launches on phone via ▶ Run
- [ ] Export/backup tested (CSV round-trips to original column layout)
- [ ] (Optional) GitHub Actions builds artifact successfully

---

## 8. Summary

For a one-person Android app, **build in Android Studio and Run straight to your phone** — no store, no account, no backend, no cost. Keep a signed release APK + a data export in your own cloud as the durable safety copy. Everything else (CI, internal testing) is optional polish, not required to meet the goal.
