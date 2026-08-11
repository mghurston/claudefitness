# Changelog

Behavior changes by version, newest first. Reconstructed from the commit history, so the
dates and version numbers are the ones actually shipped. Design rationale lives in the docs
this points at; `Scoring Model.md` and `XP Simplification Spec.md` are the two authoritative
specs for how a day is scored.

Rule for this file: **anything that changes a number the user sees gets an entry**, with what
it was before. That is the whole point of keeping it.

---

## v0.6.2 — 2026-08-11
**Cardio is now worth 25% of the day, each lift 15%.** The six goals were an equal sixth
(16.7%) apiece, which ignored that they are wildly unequal work: filling cardio (walking 5
miles) burns ~544 kcal at 90 kg while filling one 100-rep lift burns ~30, an 18:1 gap that
holds at any body weight. So a day of every lift and no cardio read **83%** and now reads
**75%**; a 5-mile walk with no lifts read **17%** and now reads **25%**. The weights still
sum to 1.0, so a full day is still exactly 100%, and every goal is still uncapped.

Not weighted strictly by burn (that would be 78% cardio / 4.3% per lift), because XP already
pays the full 18:1 — XP is pure calories. Completion is the balance meter, not a second XP
bar. The five lifts stay equal to each other since the burn model charges every rep the same
rate regardless of exercise.

**This re-scores history.** Completion is replayed from the log, never stored, so past days
move too: Log calendar colors shift, and a perfect-day streak now needs cardio to carry a
quarter of the day rather than a sixth. Nothing about XP, levels, stats, or the calorie model
changed. Details in `Scoring Model.md` §1.

## v0.6.1 — 2026-08-11
**Log calendar colors now match the Discipline bar.** The cyan "good day" tile was awarded at
**60%**; it now needs **80%**, the same completion that earns a Discipline point, so the color
means something. The dark-purple "partial" tile started at anything above **0%**, which meant
every single day (one step is enough); it now starts at **20%**, and days below that show the
empty gray tile. Legend labels updated: "none" → "<20%", "60%+" → "80%+". Display only — no
scoring, XP, or stat math changed.

## v0.6.0 — 2026-08-06
**Whole toolchain to latest stable.** Gradle 8.14.3 → 9.7.0, AGP 8.13.2 → **9.3.1** (a major
version), Kotlin 2.3.21 → 2.4.10, KSP 2.3.9 → 2.3.11, core-ktx 1.18.0 → 1.19.0, lifecycle
2.10.0 → 2.11.0, and `compileSdk` 36 → 37 (platform android-37.1), which is what the two
AndroidX bumps require.

AGP 9 has **Kotlin support built in**: applying `org.jetbrains.kotlin.android` now fails the
build outright, so that plugin is gone from both build files. The Compose compiler plugin
stays and is what pins the Kotlin version AGP compiles with.

`targetSdk` deliberately stays at **36**. Raising it opts the app into new OS behaviors and
wants testing on an API 37 device; that is an app-behavior decision, not a library update.

Everything else was already current stable — Room 2.8.4, Compose BOM 2026.06.01, activity
1.13.0, navigation 2.9.8, datastore 1.2.1, browser 1.10.0, work 2.11.2, health-connect 1.1.0.
Several of those have newer **pre-release** lines that a "latest version" query will report;
they are skipped on purpose.

Lint cleanup rode along: warnings 24 → 3. The `modifier` parameter now sits first-optional on
all four screens, bitmap drawables moved to `drawable-nodpi`, and the two test dependencies
moved into the version catalog. The three left are a deliberate `targetSdk` gap, a launcher
icon that fills its square (needs new artwork), and a `mipmap-anydpi-v26` folder that lint
says to rename — **do not**: dropping the `-v26` qualifier makes AAPT fail to resolve
`mipmap/ic_launcher` and breaks the build.

## v0.5.2 — 2026-08-06
Docs sweep for the cardio rework plus a QA pass: new `Scoring Model.md` and this changelog,
superseded banners on the design docs, first-ever tests for backup **restore** (org.json on
the test classpath), and a lint error fixed in the Calendar month header.

## v0.5.1 — 2026-08-06
**ENDURANCE counts every kind of cardio, not just walking.**
END was lifetime walked miles, so an hour on a bike built none of it. It now reads lifetime
cardio as walk-equivalent miles, the same currency the Cardio goal uses. Formula and scale
unchanged (`sqrt(miles × 0.75)`), and a walking-only log feeds it the identical input, so no
existing character moved (verified: 269 walked miles, 269 walk-equivalent, END 14 either way).
Walking keeps its own identity: lifetime walked miles, walk streaks, and walking achievements
still read walking only. Hero screen now shows both totals.

## v0.5.0 — 2026-08-06
**Cardio is a calorie goal, not a mile goal.** See `Scoring Model.md` §2.
Five walked miles is ~544 kcal at 90.7 kg; five cycled is ~204. The old `walkMiles / 5` paid
them the same, and bike/swim minutes moved the goal by zero. The sixth of completion is now
non-strength calories over what walking 5 miles burns (6 kcal per kg), so every kind of cardio
counts for what it burns and walking 5 miles still fills it exactly at any body weight.
Verified against the real 67-day log: zero days changed.
A Cardio custom now carries how it is measured (distance at walk/run/bike pace, or minutes at
light/moderate/hard), fixing a v0.4.2 bug where its miles burned at the walking rate whatever
the exercise was. `Progression.completion` now takes a Profile. Room v12, backup schema 5.

## v0.4.2 — 2026-08-06
**Custom exercises pick a training category.** The goal picker was five per-slot options; it
is now the four categories the Train tab shows plus extra-only. Upper and Lower cover two
100-rep slots each, so an assigned exercise's reps split across the pair. Old assignments fold
in (PUSH/CURLS → Upper Body, SQUATS/CALF_RAISES → Lower Body). Room v11, backup schema 4.

## v0.4.1 — 2026-08-05
Goal controls drawn as buttons rather than text links.

## v0.4.0 — 2026-08-05
**Custom exercises can fill a daily goal**, instead of only earning calories. Credit is
resolved at read time, so re-pointing one re-scores its whole history. Backup schema 3.

## v0.3.9 — 2026-07-28
Attribute bars share one scale, topped by the best stat.

## v0.3.8 — 2026-07-27
**ENDURANCE rebalanced: `sqrt(miles × 0.75)`, was `× 4`.**

## v0.3.7 — 2026-07-23
**DISCIPLINE scores 1 point per 80% day, was 1.5.**

## v0.3.6 — 2026-07-21
Emulator and target moved to Android 16 (API 36). QA pass.

## v0.3.4-0.3.5 — 2026-07-17
**Walking counts step-tracked miles everywhere** — achievements, END, lifetime miles,
quick-log — not just manually logged ones. An all-steps day used to earn no walking
achievement. CSV export gained `steps,totalmiles`.

## v0.3.3 — 2026-07-12
Attributes info modal; lifetime reps count all five exercises.

## v0.3.1-0.3.2 — 2026-07-06 → 07-12
Toolchain upgrade (SDK 36, current stable stack); Train tab layout fixes.

## v0.2.4 — 2026-07-06
Train tab: section headers own the group names; per-exercise form videos.

## v0.2.2 — 2026-07-05
**Daily mood tracking removed** — logged with nothing ever reading it back.

## v0.2.1 — 2026-07-05
Intake carry-forward restored: the last entered calorie figure stays in effect until changed.

## v0.2.0 — 2026-07-05
**XP simplification.** `dayXp = burn − target-shortfall + uncapped diet`. Every multiplier
deleted; quests and achievements pay no XP and are badges only. See
`XP Simplification Spec.md`. Docs swept to match in the same session.

## v0.1.20-0.1.21 — 2026-07-04
Per-day scoring, fasting day support (caloriesConsumed sentinel -1 vs 0), completion tier
colors (gold = 100%, cyan = 60%+).

## v0.1.14 — 2026-06-29
Flexible one-offs, Sunday-first weeks, rank tiers, unified typography.

## v0.1.9 — 2026-06-22
**All activity calories unified on one gross, body-weight-scaled MET model.** Gross rather
than net was a deliberate call, to match what Google Fit and a treadmill report.

## v0.1.3-0.1.7 — 2026-06-18
Passive activity tracking via Health Connect; walking redesign.

## v0.1.0-0.1.2 — 2026-06
Initial build: journal, custom exercises, achievements, splash and branding, calorie-based XP,
one-off and cardio activities, collapsible Train tab, burn goals. Version policy established
(bump versionCode +1 and versionName on every delivered build).
