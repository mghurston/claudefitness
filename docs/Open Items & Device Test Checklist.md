# Open Items & Device Test Checklist

Living status doc (created 2026-06-14). Tracks what's verified, what still needs testing **on a real phone**, and remaining optional work. Tick boxes as you confirm them on-device.

---

## ✅ Verified on emulator (Android 14 / API 34)

- Build green, **10 unit tests pass**, lint clean.
- Launch, one-tap logging, and **persistence across a force-stop** (data survives process death).
- Database migrations **v1 → v2 → v3** on a real in-place upgrade (no data loss).
- Custom-exercise bonus XP (e.g. 75 reps → +38 XP) with core completion unaffected.
- Mood + notes journal; title screen → tap → app flow; native cold-start splash.

### UI fixes verified on emulator (2026-06-15)
- **Hero / character select** — avatar picker is now a single row of 4 compact chips (Male 1 / Male 2 / Female 1 / Female 2) instead of two oversized buttons.
- **Train — exercise controls** — simplified to `+10` / `−10` (walking `+0.5` / `−0.5`) plus a **free-text entry** ("Enter reps"/"Miles") + **Add** + **Reset** per exercise (core, walking, custom). Removed the `+25`/`+100`/`+50`/`+1.0` quick buttons as redundant with custom entry; also fixed the earlier overflow that rendered the 4th button as a blank/stacked "−10". Confirmed: typing `12` adds 12; number-only keyboard; field clears after Add; Reset zeroes that one exercise.
- **Trophies — achievements** — logic was already correct (e.g. "First Blood" unlocks at `activeDays ≥ 1`); the gray COMMON star just didn't read as earned. Earned rows now show a green **✓ EARNED** badge + rarity-tinted card/ring; locked rows keep the 🔒 + progress bar.

## ✅ Verified on emulator (2026-07-04, v0.1.18 hardening pass)

- Full review-fix pass: quest + achievement XP now actually granted (`rebuildFull`), day-write
  mutex (user edits vs passive sync can no longer clobber each other), fasting sentinel
  (DB v10: −1 = unlogged, 0 = logged fast — verified live: a saved 0 sticks and shows the
  full-BMR deficit), locale-pinned CSV/UI number formatting, backup schema 2 (custom
  exercises, favorites, videos, unit, avatar restore), midnight day-rollover ticker,
  reset-day preserves Health Connect steps, one-off remembers walk/run/bike, reminder skips
  already-logged days, distinct per-stat bar colors, AppText adoption sweep.
- Borrowed flavor terms renamed for distribution (Aura Wielder, Breath Adept,
  National-Class, Hundred-Day Legend, Well-Rounded Warrior); quest copy made generic.
- v9→v10 DB migration exercised in-place on the emulator (install over old data, no loss).
- Unit suite green (Progression/Achievements/Quests/OneOff encoding + new fasting, quest-XP
  replay, and achievement-fixpoint tests). Play prep: `docs/privacy-policy.html` +
  RUNBOOK §7G (Health Connect declarations).

## ✅ Verified on emulator (2026-07-05, v0.2.0–v0.2.2 XP simplification)

- **XP model rebuilt** (v0.2.0, user-approved spec in `XP Simplification Spec.md`):
  `dayXp = burn − max(0, dailyBurnTarget − burn) + diet`. ALL multipliers removed
  (completion +25%, streak +50%, deficit +50%, flat surplus penalty), rep/mile shortfall +
  `missedDayPenalty` replaced by the daily-burn-target rule, quest + achievement XP removed
  (badges only; `rebuildFull` fixpoint loop deleted — this supersedes the v0.1.18 "quest XP
  granted" item above). Verified live: burn ring kcal == day XP exactly (658/658); shifting
  intake by 600 kcal moved XP by exactly 600 (uncapped, symmetric); decay banner reconciles
  (11 skipped × 450 = 4950).
- **Burn-ring weight fix**: Hero rings now read the carried-forward day (latest weigh-in),
  so displayed kcal and XP always agree — previously ring used stale profile weight.
- **Intake carry-forward restored** (v0.2.1, user decision): the last entered calories stay
  in effect until changed (fast 0 included); pre-fills Energy tab + Log day editor, feeds
  each day's diet term. Verified: unlogged day inherited 1800 and paid its diet XP.
- **Mood tracking removed** (v0.2.2, user verdict "worthless" — write-only data): emoji
  picker gone from Train + day editor, mood achievements retired → **84 trophies**. DB
  column + backup round-trip preserved. Notes journaling stays.
- Unit suite green (25 tests, rewritten for the flat model; seed month → Level 10 / Rank C).

## ☐ Needs testing on the phone (not exercised on emulator)

These are expected to work but were never run end-to-end on real hardware:

- [ ] Installs and launches on the phone (Android **8.0+** required).
- [ ] **Reminders** — enable on the Energy tab; confirm a notification actually fires at the scheduled time (it's day-aware). Needs notification permission granted.
- [ ] **Form videos** — tap "▶ form" on an exercise → opens YouTube in the in-app tab; favorite a video; add your own URL.
- [ ] **Export** — CSV and JSON export via the file picker (Hero tab) actually save a file; then **restore** from a JSON backup.
- [ ] **Energy/calories** — set height/weight, switch units (imperial⇄metric), check burned-vs-consumed and weight-goal progress.
- [ ] Custom exercise add / log / remove; calorie XP displays.
- [ ] Notes persist and reappear after reopening (mood was removed in v0.2.2).
- [ ] Overall UI fits your phone's screen size / aspect (emulator was 1080×2400).

## ☐ Open / not yet built (optional polish, not blocking)

- [ ] Per-rank portrait **art** variants (currently an aura-frame treatment that intensifies with rank, not separate images).
- [ ] Custom-exercise-specific achievements (PR/notes achievements for the core 5 are done; customs don't feed achievements yet).
- [ ] Orbitron on section titles (currently only big headers/HUD elements).
- [ ] True Android-12 `SplashScreen` API via `core-splashscreen` — **blocked**: the library isn't in the offline Gradle cache and the network is restricted here. Current approach = native window-theme splash + an in-app tap-to-enter title screen (works offline on all API levels).

## Known constraints (by design)

- **Fresh install starts empty at Level 1 / Rank E** — seeding is disabled; each phone is its own independent profile.
- **Debug-signed APK** — sideloading shows a one-time "unknown developer" warning; normal.
- Offline-only, no backend, no Play Store; data lives on-device (back up via JSON export).

---

_How to use: when something fails on the phone, note it here (or tell me) and it becomes the next fix list._
