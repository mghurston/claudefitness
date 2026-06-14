# Current State Analysis

**Project:** ASCENDANT — Personal Anime-Themed Fitness RPG
**Source artifact:** `Anime_Workout_Tracker_30_Days.xlsx` (Sheet1)
**Analysis date:** 2026-06-13
**Scope:** Single-user, Android-only, personal use

---

## 1. Source Overview

The existing tracker is a single-tab Google Sheet (exported as `.xlsx`) covering **30 consecutive days** (2025-06-01 → 2025-06-30). It is a flat, row-per-day log with one derived column. There are no secondary tabs, no charts, no pivot tables, and no historical archive beyond the 30-day window.

| Property | Value |
|---|---|
| Sheets | 1 (`Sheet1`) |
| Tracked days | 30 (June 2025) |
| Columns | 9 |
| Derived columns | 1 (`Completion Rate`) |
| Data types | Date, text, integer reps, float miles, percentage |
| Granularity | One aggregated row per calendar day |

---

## 2. Every Metric Currently Tracked

| # | Column | Type | Unit | Daily Target (derived) | Notes |
|---|--------|------|------|------------------------|-------|
| 1 | `Date` | Date | YYYY-MM-DD | — | Primary key; one row per day |
| 2 | `Day` | Text | weekday name | — | Redundant (derivable from Date); useful for day-of-week patterns |
| 3 | `Push-ups` | Integer | reps | 100 | Total reps for the day (sets not separated) |
| 4 | `Squats` | Integer | reps | 100 | Total reps for the day |
| 5 | `Leg Lifts` | Integer | reps | 100 | Total reps for the day |
| 6 | `Calf Raises` | Integer | reps | 100 | Total reps for the day; max recorded 120 |
| 7 | `Curls` | Integer | reps | 100 | Total reps for the day; max recorded 120 |
| 8 | `Miles Walked` | Float | miles | 5 | Cardio; logged every single day |
| 9 | `Completion Rate` | Float | % (0–110%+) | 100% | **Derived** — see formula below |

### 2.1 Reverse-Engineered Completion Formula

The `Completion Rate` column is a computed average of six normalized components:

```
Completion Rate =
  ( Pushups/100 + Squats/100 + LegLifts/100 + CalfRaises/100 + Curls/100 + Miles/5 ) / 6
```

This was validated against every row (e.g. 2025-06-01: `(0.5+0.5+0.5+0.5+0.3+0.3)/6 = 0.4333` ✓; 2025-06-30: `(1+1+1+1+1+1)/6 = 1.00` ✓). The formula **allows values above 100%** when a target is exceeded (e.g. 110 reps or 6 miles), so the metric doubles as an "overachievement" signal.

**Implicit daily targets:** 100 reps each for the five strength exercises, 5 miles walked.

---

## 3. Exercise Categories

The tracker implicitly groups activity into two categories. The app should make this explicit.

| Category | Exercises | Measurement | Current behavior |
|----------|-----------|-------------|------------------|
| **Strength / Calisthenics** | Push-ups, Squats, Leg Lifts, Calf Raises, Curls | Reps (count) | Inconsistent — frequently skipped together |
| **Cardio / Endurance** | Miles Walked | Distance (miles) | Rock-solid — logged 30/30 days |

There is no concept of **sets**, **time/duration**, **weight/resistance**, **rest days**, **notes**, or **per-exercise targets** in the sheet — these are gaps the app will fill (see §6).

---

## 4. Progress Calculations Present Today

| Calculation | How it's done now | Limitation |
|---|---|---|
| Daily completion % | Weighted average of 6 normalized metrics | Single number; hides which exercise failed |
| Overachievement | Completion >100% | Not surfaced or celebrated |
| (none) Streaks | Not calculated | No streak tracking at all |
| (none) Totals / cumulative | Not calculated | No "1,410 lifetime push-ups" view |
| (none) Trends | Not calculated | No week-over-week or moving average |
| (none) Personal records | Not calculated | No "best day" or PR tracking |

The sheet performs exactly **one** calculation. Everything else a motivated person would want must be eyeballed.

---

## 5. Historical Trends (Computed From the 30-Day Sample)

These insights come from analyzing the actual data and directly inform the gamification design.

### 5.1 Volume Totals (30 days)

| Exercise | Total | Daily Avg (all days) | Days Active | Avg on Active Days |
|----------|-------|----------------------|-------------|--------------------|
| Push-ups | 1,410 | 47.0 | 21/30 | 67.1 |
| Squats | 1,460 | 48.7 | 21/30 | 69.5 |
| Leg Lifts | 1,800 | 60.0 | 21/30 | 85.7 |
| Calf Raises | 1,820 | 60.7 | 21/30 | 86.7 |
| Curls | 1,670 | 55.7 | 19/30 | 87.9 |
| Miles Walked | 116.0 | 3.9 | **30/30** | 3.9 |

### 5.2 Consistency Signals

- **Average completion: 58.2%** (max 110%, min 10%).
- **Days ≥80% complete: 12/30.** Days ≥100%: **5/30.**
- **Full strength-skip days: 9/30** — on these, only walking happened.
- **Longest strength streak: just 5 consecutive days.** This is the single most important number in the dataset.
- **Longest walking streak: 30/30 days** — walking is already an automatic habit.

### 5.3 Day-of-Week Pattern (avg completion)

| Mon | Tue | Wed | Thu | Fri | Sat | Sun |
|-----|-----|-----|-----|-----|-----|-----|
| 78% | 74% | **36%** | 75% | **35%** | **39%** | 64% |

**Insight:** Wednesday, Friday, and Saturday are systematic failure days. The app can intervene specifically on these days (lighter "boss-rush" missions, extra XP incentives, or scheduled reminders).

### 5.4 Behavioral Diagnosis

1. **Cardio is solved; strength is the battle.** The motivation system must over-index on rewarding strength consistency, not walking (which is already automatic).
2. **All-or-nothing collapse.** When the user skips strength, they skip *all five* exercises at once (9 such days). Partial-credit mechanics and "minimum viable workout" quests can break this pattern.
3. **No streak feedback loop exists.** The biggest untapped motivator — there is currently zero streak visibility, so the user never feels momentum or loss aversion.
4. **Mid-week and weekend cliffs.** Targeted, day-specific nudges have a clear data-backed opportunity.

---

## 6. Missing Data Opportunities

Data the app should start capturing that the sheet cannot:

| Opportunity | Why it matters |
|---|---|
| **Sets × reps breakdown** | "100 push-ups" could be 1×100 or 10×10 — load/intensity differs |
| **Time of day / timestamp** | Reveals best workout windows; enables time-based quests |
| **Duration per session** | Enables time-based exercises (planks, walks by minutes) |
| **Per-exercise notes** | Track form cues, soreness, injuries |
| **Rest / recovery days** | Distinguish a *planned* rest from a *failed* day (anti-burnout) |
| **Bodyweight / measurements** | Long-term physical progress beyond reps |
| **Mood / energy / RPE** | Correlate effort with consistency |
| **Per-exercise streaks** | "21-day calf-raise streak" is a strong micro-motivator |
| **Personal records (PRs)** | Celebrate new maxes (the 120-rep calf-raise day went unrewarded) |
| **Custom exercises** | The sheet is hard-coded to 6 metrics; the app must be extensible |
| **Photos / progress media** | Optional visual progress log |

---

## 7. Automation Opportunities

| Manual today | Automated in app |
|---|---|
| Typing the date & weekday each row | Auto-filled from device clock |
| Mentally checking if you "did enough" | Live completion ring + per-exercise progress bars |
| No reminders | Smart notifications, day-of-week aware (hit Wed/Fri/Sat hardest) |
| No streak awareness | Automatic streak counters + grace-period logic |
| No celebration of PRs/100% days | Automatic level-up, achievement, and PR animations |
| Eyeballing trends | Auto-generated weekly/monthly review screens & charts |
| Manual data entry friction | One-tap "+10 / +25 / set complete" quick loggers |
| Re-deriving completion by hand | Real-time XP and stat conversion |
| Spreadsheet open on desktop | Always-on phone app with home-screen widget |

---

## 8. Summary

The spreadsheet is a **competent ledger but a non-existent motivator.** It records six numbers and computes one. It has no memory of streaks, no celebration of wins, no awareness of the user's clear behavioral patterns (cardio-strong, strength-weak, mid-week collapse), and no feedback loop. The ASCENDANT app's entire reason to exist is to convert this passive ledger into an active progression engine that (a) preserves the exact six metrics and the completion formula for continuity, and (b) layers RPG systems — XP, levels, stats, streaks, quests, achievements — specifically tuned to the consistency gaps the data reveals.

See `Spreadsheet-to-App Mapping.md` for the field-by-field migration plan.
