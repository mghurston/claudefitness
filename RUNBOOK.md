# ASCENDANT — Repo Runbook

Operational guide for anyone cloning or maintaining this repository. For the product
design package see [`README.md`](README.md) and [`docs/`](docs/); this file is about
**getting the code to build, run, and ship from a fresh machine**.

---

## 1. Tech stack

| Layer | Choice | Notes |
|-------|--------|-------|
| Language | **Kotlin** 2.0.21 | |
| UI | **Jetpack Compose** (Material 3, BOM 2024.10.01) | No XML layouts; everything is `@Composable`. |
| Build | **Gradle 8.x** (Kotlin DSL) + **AGP 8.7.3** | Version catalog in `gradle/libs.versions.toml`. |
| Annotation processing | **KSP** 2.0.21-1.0.28 | Room compiler only. |
| Local DB | **Room** 2.6.1 | The workout day log (schema with migrations). |
| Key-value store | **DataStore (Preferences)** 1.1.1 | Profile, units, settings. |
| Navigation | **Navigation-Compose** 2.8.4 | |
| Background work | **WorkManager** 2.9.1 | Periodic passive Health Connect sync. |
| Health data | **Health Connect** 1.1.0-alpha10 | Read-only steps + active calories. Pinned — see catalog comment. |
| In-app browser | **AndroidX Browser** (Custom Tabs) 1.8.0 | Form-video + About links. |
| Min / target SDK | **26 / 35**, `compileSdk 35` | Java/JVM target **17**. |
| Tests | JUnit 4 | Host-JVM unit tests on the progression engine. |

**Architecture in one line:** offline-first, no backend, no Play Store. The whole
character (XP/level/rank/stats/streaks) is a *pure function* of the immutable day log,
replayed on every read by `domain/Progression.kt`. Nothing about the character is stored.

Package: `com.mhurston.ascendant` (both `namespace` and `applicationId`).

---

## 2. Repo layout

```
claudefitness/
├─ app/                      # the only Gradle module
│  └─ src/
│     ├─ main/java/com/mhurston/ascendant/
│     │   ├─ MainActivity.kt
│     │   ├─ data/           # Room entities/DAO, DataStore, Repository, Exporter, seed
│     │   ├─ domain/         # pure logic: Progression, Calories, Quests, Models, Units…
│     │   ├─ health/         # Health Connect client + passive sync worker
│     │   ├─ notify/         # local reminders
│     │   └─ ui/             # Compose screens + components + theme
│     ├─ main/AndroidManifest.xml
│     └─ test/               # JUnit (ProgressionTest)
├─ docs/                     # design-time documents (see README table)
├─ gradle/libs.versions.toml # version catalog (single source of dependency versions)
├─ build.gradle.kts          # root: plugins declared `apply false`
├─ settings.gradle.kts       # repos + `include(":app")`
├─ gradle.properties         # JVM args, caching, AndroidX flags
├─ setenv.ps1 / setenv.bat   # activate the project-local toolchain (see §3)
├─ CLAUDE.md                 # always-loaded operational backstop for the AI agent
├─ README.md                 # status + design package index
└─ RUNBOOK.md                # this file
```

### Project-local, self-contained toolchain (important)
This repo is built to keep **everything project-local** — nothing is installed on the
system PATH or registry. These directories live inside the repo but are **git-ignored**
(see `.gitignore`), so a fresh clone will **not** contain them:

- `jdk/` — JDK 17
- `android-sdk/` — Android SDK (platform-tools, build-tools, platforms;android-35, etc.)
- `gradle-dist/` — Gradle distribution
- `.gradle/`, `.gradle-home/`, `.kotlin/`, `build/`, `app/build/` — caches/outputs
- `local.properties` — machine-specific `sdk.dir`
- `.avd/` — local emulator AVD
- `qa-*.png` — QA screenshots

> The original author's machine stores all of this under `G:\claudefitness`. A new
> machine must supply its own JDK + SDK (see §4).

---

## 3. The toolchain activation scripts

`setenv.ps1` (PowerShell) / `setenv.bat` (cmd) set, **for the current shell session only**:

- `JAVA_HOME`  → `<repo>/jdk`
- `ANDROID_HOME` / `ANDROID_SDK_ROOT` → `<repo>/android-sdk`
- `GRADLE_USER_HOME` → `<repo>/.gradle-home` (keeps Gradle caches in-repo)
- prepends `jdk/bin`, `android-sdk/platform-tools`, `android-sdk/cmdline-tools/latest/bin` to `PATH`

They do **not** touch system/user environment, registry, or other projects.

```powershell
. .\setenv.ps1     # PowerShell — note the leading dot (dot-source) so vars persist in the shell
```

The env does **not** persist across separate tool invocations — re-source it at the top
of every fresh shell, or the SDK paths will be blank and builds will mis-report.

---

## 4. First-time setup on a fresh clone

A clone has **no** `jdk/`, `android-sdk/`, or `local.properties`. Pick one of:

### Option A — System-installed Android Studio / SDK (most common for new devs)
1. Install **JDK 17** and the **Android SDK** (Android Studio bundles both).
2. Create `local.properties` in the repo root pointing at your SDK:
   ```properties
   sdk.dir=/absolute/path/to/Android/Sdk        # macOS/Linux
   # sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk   # Windows (escaped)
   ```
3. Make sure SDK Platform **API 35** and matching build-tools are installed.
4. Build with the wrapper: `./gradlew :app:assembleDebug` (you can ignore `setenv.*`).

### Option B — Reproduce the portable layout (what the author uses)
1. Drop a JDK 17 into `jdk/` and an Android SDK into `android-sdk/` (with
   `platform-tools`, `cmdline-tools/latest`, `platforms/android-35`, build-tools).
2. Create `local.properties` with `sdk.dir` → the in-repo `android-sdk` (escaped path).
3. `. .\setenv.ps1` then `.\gradlew.bat :app:assembleDebug`.

### Accept SDK licenses if Gradle complains
```bash
yes | "$ANDROID_HOME"/cmdline-tools/latest/bin/sdkmanager --licenses
```

---

## 5. Build · test · run

```powershell
. .\setenv.ps1                         # (Option B only) activate toolchain
.\gradlew.bat :app:testDebugUnitTest   # run unit tests (progression engine)
.\gradlew.bat :app:assembleDebug       # build debug APK
```
- Success marker: `BUILD SUCCESSFUL`.
- APK output: `app/build/outputs/apk/debug/app-debug.apk`.

### Emulator (Option B layout)
`setenv.ps1` deliberately does **not** set `ANDROID_AVD_HOME`, so `emulator -list-avds`
looks empty until you point it at the in-repo AVD:
```powershell
. .\setenv.ps1; $env:ANDROID_AVD_HOME="<repo>\.avd"
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd ascendant_test -no-snapshot-load -no-boot-anim
```
Then install + launch:
```powershell
$adb="$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb install -r -d "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell monkey -p com.mhurston.ascendant -c android.intent.category.LAUNCHER 1
```
(A new machine can instead create any AVD in Android Studio and skip `ANDROID_AVD_HOME`.)

---

## 6. What a new user/maintainer would customize

| If you want to… | Change |
|------------------|--------|
| **Build on your own machine** | Create `local.properties` (`sdk.dir`). It is git-ignored and never shipped — required, not optional. |
| **Use a system SDK instead of the portable one** | Ignore/edit `setenv.ps1`/`setenv.bat`; just use `./gradlew` with your `local.properties`. |
| **Make it your own app** (separate install from the author's) | Change `namespace` **and** `applicationId` in `app/build.gradle.kts`, and the package folders under `app/src/main/java/...`. Update `app_name` in `res/values/strings.xml`. |
| **Rebrand identity** | App name (`strings.xml`), launcher icon (`res/mipmap-*`), hero portraits (`res/drawable/hero_portrait*`), theme/colors in `ui/theme/`. |
| **Personal links** | The **About** card on the Hero screen (`ui/CharacterScreen.kt`) — website + Linktree URLs. |
| **Ship a new build** | Bump **both** `versionCode` (+1) and `versionName` in `app/build.gradle.kts` on **every** delivered build — sideloads reject an equal/lower `versionCode`. |
| **Release (signed) build** | No release keystore is configured; `buildTypes.release` has `isMinifyEnabled = false` and no signing. Add a keystore + `signingConfigs` before distributing a release APK. |
| **Bump a dependency / Gradle / AGP / SDK** | Edit `gradle/libs.versions.toml` (single source of truth). Note Health Connect is pinned to `1.1.0-alpha10` because newer needs `compileSdk 36` / AGP 8.9.1. |
| **Change permissions** | `app/src/main/AndroidManifest.xml` declares `POST_NOTIFICATIONS` + Health Connect read perms and the HC rationale deep-links. |
| **Seed/import history** | `data/` seeding is disabled so fresh installs start at Level 1; `docs/assets/seed_history.csv` + the in-app JSON/CSV restore are the import paths. |

---

## 7. Gotchas

- **Re-source `setenv` per shell.** A command run in an un-activated shell has a blank
  `ANDROID_HOME`; don't trust a negative result (e.g. a `Test-Path` False) from one.
- **Screenshots:** use `adb shell screencap -p /sdcard/s.png` then `adb pull`. Do **not**
  pipe `adb exec-out screencap -p > file.png` from PowerShell — `>` writes UTF-16/BOM and
  corrupts the PNG.
- **Config cache + caching are on** (`gradle.properties`); if you hit a stale-cache oddity,
  `--no-configuration-cache` is the escape hatch.
- **`CLAUDE.md`** is the always-loaded operational backstop for the AI coding agent and
  mirrors the build/emulator/screenshot commands above — keep the two in sync if you change
  the toolchain.
