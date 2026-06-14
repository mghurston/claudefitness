# Spreadsheet-to-App Mapping

**Purpose:** Define exactly how every element of `Anime_Workout_Tracker_30_Days.xlsx` migrates into the ASCENDANT app's data model, and how the existing 30 days of history are imported. This guarantees **Success Criterion #1: the Google Sheet can be fully replaced** with zero data loss.

---

## 1. Column → Field Mapping

| Sheet Column | App Field | App Type | Storage Location | Transformation |
|---|---|---|---|---|
| `Date` | `WorkoutDay.date` | `LocalDate` (ISO-8601) | `workout_day` table, PK | Direct copy |
| `Day` | *(dropped as stored field)* | — | derived | Recomputed from `date` at display time |
| `Push-ups` | `ExerciseEntry(exerciseId=pushups).totalReps` | `Int` | `exercise_entry` table | Direct copy; one entry row |
| `Squats` | `ExerciseEntry(exerciseId=squats).totalReps` | `Int` | `exercise_entry` table | Direct copy |
| `Leg Lifts` | `ExerciseEntry(exerciseId=leglifts).totalReps` | `Int` | `exercise_entry` table | Direct copy |
| `Calf Raises` | `ExerciseEntry(exerciseId=calfraises).totalReps` | `Int` | `exercise_entry` table | Direct copy |
| `Curls` | `ExerciseEntry(exerciseId=curls).totalReps` | `Int` | `exercise_entry` table | Direct copy |
| `Miles Walked` | `ExerciseEntry(exerciseId=walking).distanceMiles` | `Float` | `exercise_entry` table | Direct copy |
| `Completion Rate` | `WorkoutDay.completionRate` | `Float` | computed, cached | **Recomputed**, not imported (verify match) |

> The `Day` text column and the `Completion Rate` column are both **derived** values in the app, not stored inputs. Importing them is unnecessary; the app recomputes them and uses the imported values only as a validation checksum.

---

## 2. Flat Sheet → Relational Model

The single flat sheet becomes a small normalized schema so the app can support sets, custom exercises, notes, and future metrics that the sheet never could.

```
exercise (catalog)                 workout_day (per day)            exercise_entry (per exercise per day)
─────────────────                  ─────────────────────            ────────────────────────────────────
id            TEXT PK              date        DATE PK              id            INTEGER PK
name          TEXT                 completion  REAL (cached)        day_date      DATE FK -> workout_day
category      TEXT  (STRENGTH|     xp_awarded  INTEGER              exercise_id   TEXT FK -> exercise
              CARDIO|CUSTOM)       notes       TEXT                 total_reps    INTEGER (nullable)
unit          TEXT  (REPS|MILES|   is_rest_day INTEGER (bool)       sets_json     TEXT    (nullable)
              MINUTES|...)         created_at  INTEGER              distance      REAL    (nullable)
daily_target  REAL                                                 duration_sec  INTEGER (nullable)
weight        REAL  (formula wt)                                   notes         TEXT    (nullable)
icon          TEXT                                                 logged_at     INTEGER
is_active     INTEGER (bool)
sort_order    INTEGER
```

### 2.1 Seed Catalog (`exercise` table)

| id | name | category | unit | daily_target | weight |
|----|------|----------|------|--------------|--------|
| `pushups` | Push-ups | STRENGTH | REPS | 100 | 1/6 |
| `squats` | Squats | STRENGTH | REPS | 100 | 1/6 |
| `leglifts` | Leg Lifts | STRENGTH | REPS | 100 | 1/6 |
| `calfraises` | Calf Raises | STRENGTH | REPS | 100 | 1/6 |
| `curls` | Curls | STRENGTH | REPS | 100 | 1/6 |
| `walking` | Walking | CARDIO | MILES | 5 | 1/6 |

The `weight` column generalizes the hard-coded `/6` averaging so the formula survives when the user adds custom exercises (see §4).

---

## 3. Completion Rate: Preserved Exactly, Then Generalized

**Legacy formula (must reproduce identically for imported days):**

```
completion = ( pushups/100 + squats/100 + leglifts/100
             + calfraises/100 + curls/100 + miles/5 ) / 6
```

**Generalized app formula (for current + future exercises):**

```
completion = Σ ( entry.value / exercise.daily_target × exercise.weight )
             over all active exercises
where Σ weight = 1.0
```

With the six seed exercises each weighted `1/6` and targets `{100,100,100,100,100,5}`, the generalized formula is **mathematically identical** to the legacy one. A unit test will replay all 30 imported days and assert the recomputed `completion` matches the sheet's `Completion Rate` to within `±0.001`.

> **Design choice — capping:** The sheet lets completion exceed 100% (e.g. 110%). The app keeps the *raw* uncapped value for XP/overachievement bonuses, but the **dashboard ring caps visually at 100%** and routes the surplus into a separate "Overdrive" bonus indicator. This rewards the 120-rep days the sheet silently ignored.

---

## 4. Handling Extensibility (Future Custom Exercises)

When the user adds a custom exercise, they choose a `unit` (reps / miles / minutes / count) and a `daily_target`. Two weighting modes are offered:

- **Equal-weight mode (default, matches today):** every active exercise gets `weight = 1 / N`. Adding a 7th exercise re-weights all to `1/7`.
- **Pinned-core mode:** the original 6 stay at `1/6` and custom exercises contribute *bonus* completion above 100% (so adding exercises never dilutes the core goal).

The user picks the mode in Settings; default is **Pinned-core** so the historical baseline stays meaningful.

---

## 5. History Import Procedure

The 30 days of existing data are imported once on first launch.

1. **Export** the Google Sheet as `.xlsx` or `.csv` (already have the `.xlsx`).
2. **Bundle** a one-time CSV (`seed_history.csv`) in the app's assets, OR provide an in-app "Import CSV" picker (recommended — see Technical Architecture).
3. For each row:
   - Upsert a `workout_day(date)`.
   - Create six `exercise_entry` rows (5 reps + 1 distance).
   - Recompute `completion`; **assert** it equals the sheet's value (log mismatches).
   - Retroactively award XP, recompute stats, and reconstruct streaks (see §6).
4. **Validation report** shown to user: "Imported 30 days, 116 miles, 8,160 total reps. All completion rates verified ✓."

### 5.1 CSV Contract

```csv
date,pushups,squats,leglifts,calfraises,curls,miles
2025-06-01,50,50,50,50,30,1.5
2025-06-02,100,100,100,100,100,3.0
...
```

A small `import_mapping.json` declares which CSV column maps to which `exercise.id`, so re-importing an updated sheet (or a differently-named export) needs no code change.

---

## 6. Retroactive Gamification of Imported History

Importing isn't just data — the app reconstructs the RPG state *as if* it had been running all along, so the user starts with a meaningful character instead of level 1.

| Derived from import | Result |
|---|---|
| Sum of all reps/miles × XP rates | Starting **total XP** and **level** (see Leveling System) |
| Per-exercise lifetime totals | Stat values (STR from push/squat/curl reps, etc.) |
| Consecutive active days | Reconstructed **streak history** and "longest streak" record |
| Best single-day reps per exercise | Seeded **personal records** |
| 100%+ days, milestone totals | Retroactively **unlocked achievements** (e.g. "1,000 Push-ups" — already earned: 1,410) |

**Motivational payoff:** On day one the user opens the app and is already (per the data) ~Level X with several achievements unlocked and a visible "longest streak: 5 — beat it" challenge. This is far more engaging than a blank slate and immediately demonstrates the app's value over the sheet.

---

## 7. What Is Intentionally NOT Migrated

| Item | Reason |
|---|---|
| `Day` (weekday) column | Recomputed from date; storing it risks drift |
| `Completion Rate` stored values | Recomputed; imported only as a checksum |
| Cell formatting / colors | Replaced by the app's style system |
| The 30-day window limit | App keeps unlimited history |

---

## 8. Round-Trip Safety (Export Back to Sheet)

To honor "fully replace the sheet" without lock-in, the app provides **Export → CSV/XLSX** producing a file with the *exact original column layout* (`Date, Day, Push-ups, Squats, Leg Lifts, Calf Raises, Curls, Miles Walked, Completion Rate`). The user can always regenerate their familiar spreadsheet, guaranteeing the migration is reversible and trustworthy. See `Technical Architecture.md` §Backup.
