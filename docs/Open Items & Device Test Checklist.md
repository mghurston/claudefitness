# Open Items & Device Test Checklist

Living status doc (created 2026-06-14). Tracks what's verified, what still needs testing **on a real phone**, and remaining optional work. Tick boxes as you confirm them on-device.

---

## ✅ Verified on emulator (Android 14 / API 34)

- Build green, **10 unit tests pass**, lint clean.
- Launch, one-tap logging, and **persistence across a force-stop** (data survives process death).
- Database migrations **v1 → v2 → v3** on a real in-place upgrade (no data loss).
- Custom-exercise bonus XP (e.g. 75 reps → +38 XP) with core completion unaffected.
- Mood + notes journal; title screen → tap → app flow; native cold-start splash.

## ☐ Needs testing on the phone (not exercised on emulator)

These are expected to work but were never run end-to-end on real hardware:

- [ ] Installs and launches on the phone (Android **8.0+** required).
- [ ] **Reminders** — enable on the Energy tab; confirm a notification actually fires at the scheduled time (it's day-aware). Needs notification permission granted.
- [ ] **Form videos** — tap "▶ form" on an exercise → opens YouTube in the in-app tab; favorite a video; add your own URL.
- [ ] **Export** — CSV and JSON export via the file picker (Hero tab) actually save a file; then **restore** from a JSON backup.
- [ ] **Energy/calories** — set height/weight, switch units (imperial⇄metric), check burned-vs-consumed and weight-goal progress.
- [ ] Custom exercise add / log / remove; bonus XP displays.
- [ ] Mood + notes persist and reappear after reopening.
- [ ] Overall UI fits your phone's screen size / aspect (emulator was 1080×2400).

## ☐ Open / not yet built (optional polish, not blocking)

- [ ] Per-rank portrait **art** variants (currently an aura-frame treatment that intensifies with rank, not separate images).
- [ ] Custom-exercise-specific achievements (PR/notes/mood achievements for the core 5 are done; customs don't feed achievements yet).
- [ ] Orbitron on section titles (currently only big headers/HUD elements).
- [ ] True Android-12 `SplashScreen` API via `core-splashscreen` — **blocked**: the library isn't in the offline Gradle cache and the network is restricted here. Current approach = native window-theme splash + an in-app tap-to-enter title screen (works offline on all API levels).

## Known constraints (by design)

- **Fresh install starts empty at Level 1 / Rank E** — seeding is disabled; each phone is its own independent profile.
- **Debug-signed APK** — sideloading shows a one-time "unknown developer" warning; normal.
- Offline-only, no backend, no Play Store; data lives on-device (back up via JSON export).

---

_How to use: when something fails on the phone, note it here (or tell me) and it becomes the next fix list._
