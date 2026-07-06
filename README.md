# ASCENDANT — Personal Anime-Themed Fitness RPG

> **Status: BUILT & RUNNING** (updated 2026-07-05, v0.2.2). The full app is implemented in Kotlin + Jetpack Compose and verified on an Android emulator (API 34). Docs below are the original design package; this header tracks live status. **The XP model was simplified in v0.2.0 — see `docs/XP Simplification Spec.md` for the authoritative rules.**

A single-user, Android-only, offline-first app that replaces a Google Sheets workout tracker with an anime-RPG progression system — turning daily exercise into XP, levels, stats, streaks, quests, and achievements. Every mechanic is tuned against the real 30-day data from `Anime_Workout_Tracker_30_Days.xlsx`.

---

## Status & feature summary (high level)

**App:** `com.mhurston.ascendant` · Kotlin + Jetpack Compose · minSdk 26 / target 35 · offline, no backend, no Play Store (personal sideload). Five tabs: **Hero · Train · Trophies · Log · Energy**, behind a tap-to-enter title screen + native cold-start splash.

**What it does:**
- **Logging** — 5 core exercises (push-ups, squats, leg lifts, calf raises, curls) + walking; one-tap quick-log, per-day editing via a month calendar, **Overdrive** bonus past 100.
- **Progression** — calorie-based XP (`dayXp = burn − target-shortfall + diet`, 1 kcal = 1 XP, no multipliers — see `docs/XP Simplification Spec.md`) → levels (1→100), **8 ranks** (E→National-Class), **5 attributes** (STR/END/AGI/DIS/CON), evolving titles. Replayed deterministically from the day log.
- **Streaks & decay** — strength/activity/perfect streaks (display/badges only, no XP effect), plus permanent inactivity decay: a past day that burns under the personal daily target (~25% of BMR) loses the gap; a skipped day loses the full target.
- **Quests** — day-aware daily + weekly objectives (badges only, no XP).
- **Achievements** — **84**, rarity-tiered, retroactive, badges only (no XP payouts).
- **Custom exercises & one-offs** — user-defined side work; burns calories (= XP) without touching the tuned core completion %/stats.
- **Journal** — per-day free-text notes (mood tracking removed in v0.2.2).
- **Energy** — height/weight, calories burned vs consumed, weight goal + progress.
- **Form videos** — per-exercise YouTube deep-links opened in an in-app tab, favorites, add-your-own.
- **Identity** — 4 hero portraits with a rank-evolving aura frame; Orbitron HUD type.
- **Quality-of-life** — local reminders, imperial/metric units (imperial default), CSV + JSON export and JSON restore.

**Data & QA:** Room (workout log, schema v3 with migrations) + DataStore (profile/settings); unit tests + lint pass; verified live on emulator (launch, logging, persistence across process death, migrations).

**Open items & on-device test checklist:** see [Open Items & Device Test Checklist.md](docs/Open%20Items%20&%20Device%20Test%20Checklist.md) — tracks what's verified, what still needs testing on a real phone (reminders firing, video links, export/restore), and remaining optional polish.

---

## Deliverables (all in `docs/`)

| # | Document | Phase | What it covers |
|---|----------|-------|----------------|
| 1 | [Product Vision.md](docs/Product%20Vision.md) | 2 | Problem, motivation strategy, user psychology, success criteria |
| 2 | [Current State Analysis.md](docs/Current%20State%20Analysis.md) | 1 | Every metric, categories, the reverse-engineered completion formula, 30-day trends, gaps |
| 3 | [Spreadsheet-to-App Mapping.md](docs/Spreadsheet-to-App%20Mapping.md) | 1 | Field-by-field migration, relational schema, history import, round-trip export |
| 4 | [Product Design Document.md](docs/Product%20Design%20Document.md) | 2 | Core loop, exercise tracking, quests, streaks, progress viz — the master spec |
| 5 | [User Flows.md](docs/User%20Flows.md) | 4 | Six required journeys + edge cases |
| 6 | [Wireframes.md](docs/Wireframes.md) | 4 | Low-fi layouts for all six screens + overlays |
| 7 | [Style Guide.md](docs/Style%20Guide.md) | 5 | Color, type, icons, components, animation, art direction, AI image prompts |
| 8 | [Achievement System.md](docs/Achievement%20System.md) | 2 | Achievements (design ceiling 120; **84 shipped**), rarity, retroactive unlocks |
| 9 | [Leveling System.md](docs/Leveling%20System.md) | 2 | Level curve, 5 attributes, ranks (XP model superseded — see XP Simplification Spec.md) |
| 10 | [Technical Architecture.md](docs/Technical%20Architecture.md) | 6 | Framework decision (Kotlin + Compose), schema, offline-first, backup |
| 11 | [Deployment Plan.md](docs/Deployment%20Plan.md) | 7 | Toolchain, build steps, phone install, recommended personal-use path |
| 12 | [Future Enhancements.md](docs/Future%20Enhancements.md) | — | Post-v1 roadmap |
| + | [XP Simplification Spec.md](docs/XP%20Simplification%20Spec.md) | — | **Authoritative XP model (v0.2.0+)**: burn − target-shortfall + uncapped diet; no multipliers, badges-only quests/achievements |
| + | [Video Integration.md](docs/Video%20Integration.md) | 3 | YouTube linking, creators, licensing, API, offline limits |
| + | [assets/seed_history.csv](docs/assets/seed_history.csv) | 1 | Your 30 days, ready to import on first launch |

---

## Key findings from your data (the design is built on these)

- **Completion formula reverse-engineered:** `(P/100 + S/100 + L/100 + C/100 + Cu/100 + Miles/5) / 6` → daily targets are **100 reps each of 5 exercises + 5 miles**.
- **Walking is automatic** (30/30 days) — **strength is fragile**: 21/30 active days, **9 total-skip days**, **longest streak only 5 days**.
- **Collapse days:** Wednesday 36%, Friday 35%, Saturday 39% (vs Mon 78% / Tue 74% / Thu 75%).
- The whole reward economy targets these gaps: streaks just past 5, "Boss Days" on Wed/Fri/Sat, partial-credit to break all-or-nothing, and de-emphasized walking XP.

## Headline design decisions (flagged for your approval)

| # | Decision | Default |
|---|----------|---------|
| D1 | App name | **ASCENDANT** (rename freely) |
| D2 | Stack | **Kotlin + Jetpack Compose** (Android-only ⇒ native beats Flutter/KMP) |
| D3 | Completion mode when adding custom exercises | **Pinned-core** (originals stay 1/6) |
| D4 | Walking emphasis | De-emphasized (soft XP cap) — reward strength |
| D5 | Video integration (v1) | **YouTube deep-link, no API key** |
| D6 | Deployment | **Android Studio ▶ Run to phone** (no store, no account, no backend) |
| D7 | Import | Retroactively lands you ~**Level 10, Rank C, ~18 achievements** on day one (under the v0.2.0 flat calorie model) |

---

## Success criteria — coverage

1. Sheet fully replaceable → formula preserved + `seed_history.csv` import + CSV/XLSX export (no lock-in). ✔
2. More engaging than the sheet → one-tap logging with XP/animation payoff. ✔
3. Long-term consistency → streaks, quests, levels tuned to the real weak spots. ✔
4. Feels like an anime RPG → Style Guide + progression systems. ✔
5. Coding can begin immediately → schema, formulas, flows, screens all specified. ✔
6. Decisions justified → every mechanic mapped to the 30-day data. ✔
7. Single-user Android optimized → offline-first, no backend, file-based backup. ✔

> **Current state:** decisions D1–D7 were confirmed and the app is fully built against them, then the reward economy was rebuilt twice at the user's direction: June 2026 moved XP to calories (1 kcal = 1 XP), and **v0.2.0 (July 2026) stripped every multiplier/bonus** — the whole model is now `dayXp = burn − target-shortfall + diet` (see `docs/XP Simplification Spec.md`). Seeding is disabled so fresh installs start at Level 1. Build/run: `. .\setenv.ps1` then `gradlew assembleDebug`; see [Deployment Plan.md](docs/Deployment%20Plan.md).
