# Technical Architecture

Recommends the stack and structure for a **single-user, Android-only, offline-first** RPG fitness app, with a justified framework decision and a schema/architecture that makes the RPG state fully rebuildable from raw logs.

---

## 1. Requirements That Drive the Architecture

| Requirement | Architectural consequence |
|---|---|
| Android-only | No cross-platform abstraction needed; native is on the table |
| Single user | No accounts, no auth server, no multi-tenant data, no sync conflicts |
| Offline-first | All logic + storage on-device; network optional (videos, backup) |
| Local storage | Embedded SQLite database |
| Cloud backup (optional) | File-based export to user's own cloud (Drive), not a hosted backend |
| Heavy animation / "juice" | Strong UI animation framework matters |
| Future extensibility | Clean modular architecture; data model already generalized |
| Personal, must ship easily | Simplest possible build & sideload path (see Deployment Plan) |

---

## 2. Framework Decision

### Comparison

| Criterion | **Kotlin + Jetpack Compose** | Flutter | Kotlin Multiplatform / KMP |
|---|---|---|---|
| Android-native fit | ★★★★★ first-party | ★★★★ very good | ★★★★ |
| Cross-platform need | not required | its main selling point (wasted here) | its main selling point (wasted here) |
| Animation / juice | ★★★★★ Compose animation, particles, haptics | ★★★★ good | ★★★★ (still uses Compose on Android) |
| Local DB | Room (first-party, excellent) | Drift/sqflite (good) | SQLDelight (good) |
| Tooling / deploy simplicity | ★★★★★ Android Studio → one-click to phone | ★★★★ extra toolchain | ★★★ more setup |
| Learning curve | moderate (Kotlin) | moderate (Dart) | steeper |
| Longevity / Google support | first-party, default Android UI | strong but third-party | first-party but newer |
| Background work / notifications | WorkManager/AlarmManager native | plugins | native |
| Asset/animation ecosystem | Lottie, Compose, native | Lottie, Rive, Flutter | shared logic, Compose UI |

### ✅ Recommendation: **Kotlin + Jetpack Compose**

**Justification:**
1. **The only platform target is Android.** Flutter and KMP exist primarily to share code across platforms — a benefit that is *explicitly out of scope* here (Product Vision §6). Choosing them means paying an abstraction tax for a feature you'll never use.
2. **First-party everything.** Compose (UI), Room (DB), WorkManager (reminders), DataStore (prefs), and Material3 are all Google-maintained and designed to work together — minimal glue, maximal longevity, best docs.
3. **Animation is core to the product.** The whole value prop is "juice" — level-up takeovers, particle bursts, ring fills, haptics. Compose's declarative animation APIs (`animate*AsState`, `AnimatedVisibility`, `Transition`, `Canvas`) plus Lottie make this first-class and native-fast.
4. **Simplest deploy for personal use.** Android Studio → connect/pair phone → Run. One toolchain, no Dart/Flutter SDK layer, no extra build bridge. (See Deployment Plan.)
5. **Lean & native performance** on one device, with no runtime engine shipped alongside the app.

**When the other two would win (and why they don't here):** Flutter if you wanted iOS too (you don't). KMP if you wanted to share business logic across native iOS/Android/desktop (you don't). Since both advantages are nullified by the Android-only constraint, Compose is the unambiguous pick.

---

## 3. High-Level Architecture (Clean / MVVM)

```
┌──────────────────────── UI (Jetpack Compose) ────────────────────────┐
│ Screens: Dashboard · Exercise · Character · Analytics · Achievements   │
│ Settings · Overlays (LevelUp, AchievementUnlock, QuickLog)             │
│ State via ViewModels (StateFlow) · Navigation-Compose                  │
└───────────────▲───────────────────────────────────────────────────────┘
                │ UI state / events
┌───────────────┴──────────────── Domain ───────────────────────────────┐
│ Use cases: LogEntry, CloseDay, ComputeCompletion, AwardXp,             │
│   EvaluateStreaks, EvaluateQuests, EvaluateAchievements, RebuildState   │
│ Pure Kotlin · deterministic · unit-testable · NO Android deps          │
└───────────────▲───────────────────────────────────────────────────────┘
                │ repositories
┌───────────────┴──────────────── Data ─────────────────────────────────┐
│ Room DB (SQLite)  ·  DataStore (settings)  ·  Assets (video JSON,      │
│   achievement defs, seed_history.csv)  ·  Backup/Export (CSV/XLSX/JSON)│
└────────────────────────────────────────────────────────────────────────┘
            │ optional, network-gated
   ┌────────┴─────────┐         ┌──────────────────────┐
   │ YouTube deep-link │        │ User cloud (Drive/SAF) │  ← backup files only
   └──────────────────┘         └──────────────────────┘
```

**Key principle — derived state is a pure function of the log.** XP, level, stats, streaks, and achievements are *computed* from the immutable `exercise_entry` log, never stored as the source of truth (only cached). This makes import, edits, undo, and backup-restore trivially correct: `RebuildState(entries) → CharacterState`.

---

## 4. Data Layer (Room)

Schema mirrors `Spreadsheet-to-App Mapping.md` §2:

- `exercise` (catalog: id, name, category, unit, daily_target, weight, icon, active, sort)
- `workout_day` (date PK, completion cache, xp_awarded cache, notes, is_rest_day)
- `exercise_entry` (id, day_date FK, exercise_id FK, total_reps?, sets_json?, distance?, duration_sec?, weight?, notes?, logged_at)
- `achievement_state` (achievement_id PK, unlocked_at, progress)
- `streak_state` (type PK, current, longest, last_active_date, freezes_banked)
- `video_link` (id, exercise_id, yt_id, title, creator, is_favorite, tags) — editable, seeded from assets
- `kv_meta` (schema version, lifetime totals cache)

Settings (targets mode, theme, units, notification prefs) live in **DataStore**, not the DB.

**Migrations:** Room schema versioning; the generalized model means new exercises/metrics are *rows*, not schema changes.

---

## 5. Core Computations (Domain, pure)

| Function | Input → Output | Notes |
|---|---|---|
| `computeCompletion(day)` | entries → 0..n | Generalized weighted formula (Mapping §3); reproduces sheet exactly |
| `awardXp(day)` | entries+context → XP | Base rates × multipliers (Leveling §2–3) |
| `levelForXp(totalXp)` | XP → level/rank | Inverse of `xpToNext` cumulative |
| `computeStats(allEntries)` | → STR/END/AGI/DIS/CON | sqrt-scaled lifetime formulas (Leveling §5) |
| `evaluateStreaks(days)` | → streak states | grace/freeze/rest logic (PDD §6) |
| `evaluateQuests(context)` | → quest progress | day-aware generation (PDD §5) |
| `evaluateAchievements(state)` | → unlocks | pure predicates over state (Achievement doc) |
| `rebuildState(allEntries)` | → full CharacterState | replay; used by import, edit, restore |

All are pure, deterministic, and unit-tested against the real 30-day dataset (e.g. assert imported completion matches sheet to ±0.001, assert ~Level 10–12 on import).

---

## 6. Offline-First & Network Use

- **100% of the fitness loop works offline** — logging, XP, stats, quests, achievements, analytics, export.
- Network is used **only** for: (a) YouTube deep-links (optional enhancement, degrades gracefully — Video Integration §7), and (b) optional cloud backup upload.
- No telemetry, no analytics SDKs, no ads — it's a private tool.

---

## 7. Backup, Export & Restore

| Mechanism | Detail |
|---|---|
| **Local auto-backup** | Periodic (WorkManager) JSON snapshot of DB to app storage; keep last N |
| **Export to sheet format** | CSV/XLSX with the *exact original columns* (`Date, Day, Push-ups…Completion Rate`) — guarantees no lock-in (Mapping §8) |
| **Full export** | JSON of all tables for complete portability |
| **Cloud backup (optional)** | Write backup file to user's Google Drive via Storage Access Framework (SAF) document picker — **no custom backend, no OAuth server**; user owns the file |
| **Android Auto Backup** | Enable framework-level `android:allowBackup` to user's Google account as a safety net |
| **Restore** | Import JSON/CSV → `rebuildState` reconstructs the entire character deterministically |

> Cloud backup deliberately uses **file-based, user-owned storage (Drive/SAF)** rather than a hosted database — true to the single-user, no-backend principle, and zero running cost.

---

## 8. Project Structure (suggested)

```
app/
 ├─ ui/        (compose screens, components, theme, animations)
 ├─ domain/    (pure use-cases, models, formulas)   ← no android imports
 ├─ data/      (room entities/daos, datastore, repositories, backup)
 ├─ assets/    (achievements.json, videos.json, seed_history.csv, lottie/)
 └─ di/        (Hilt modules)
```

Libraries: Compose + Material3, Room, DataStore, Hilt (DI), WorkManager, Navigation-Compose, Lottie-Compose, kotlinx-serialization, kotlinx-datetime, Apache POI or a light CSV lib for XLSX/CSV I/O, coil (image loading), `android-youtube-player` (optional in-app video).

---

## 9. Testing Strategy

- **Unit tests** on all domain functions using the real 30-day data as fixtures (completion parity, XP, stats, streak reconstruction, achievement retro-unlock count).
- **Migration tests** for Room.
- **Snapshot/UI tests** (Compose) for key screens.
- A single golden test: `import(seed_history.csv) → assert {30 days, completion parity, Level≈11, ≥18 achievements}`.

---

## 10. Non-Functional Notes

- **Min SDK:** Android 8.0 (API 26) for broad reach; **Target:** latest stable.
- **Performance budget:** instant log feedback (<16ms frame for animations); DB ops off main thread (coroutines).
- **Security/Privacy:** all data local; no PII leaves device except user-initiated backup; no network permission required for core function (request only for optional features).
- **Size:** keep APK lean; bundle Lottie/vector art, not large video.
