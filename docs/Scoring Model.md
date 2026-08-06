# Scoring Model (CURRENT)

**Authoritative for daily completion, the Cardio goal, custom-exercise categories, and the
five attributes, as shipped in v0.5.1 (2026-08-06).** Where a design doc in this folder
disagrees, this file wins. XP itself is specified in `XP Simplification Spec.md`, which this
does not change.

Every number here is resolved when the log is replayed, never stored per day. Re-pointing an
exercise or correcting a weigh-in re-scores the whole history on the spot.

---

## 1. Daily completion: six equal goals

```
completion = ( pushups/100 + squats/100 + legLifts/100
             + calfRaises/100 + curls/100 + cardioFraction ) / 6
```

Uncapped: a 200-rep day scores 2.0 for that slot and the day can exceed 100%.

The five rep slots read the totals *after* category credit (see §3). The sixth used to be
`walkMiles / 5`. Since v0.5.0 it is calories:

```
cardioFraction = cardioKcal / cardioTarget
```

`Progression.completion(day, profile)` takes the profile because two of the calorie sources
below are absolute numbers rather than body-weight-scaled ones.

## 2. The Cardio goal is calories

A mile is not a unit of effort. At 90.7 kg (200 lb), five walked miles is ~544 kcal and five
cycled is ~204. Scoring both as "5 mi" paid the second one the same for less than half the
work, while bike and swim minutes earned XP and moved the goal by nothing at all.

| | |
|---|---|
| **Target** | `walkKcal(weight, 5 mi)` = `1.2 × weight × 5` = **6.0 kcal per kg** (544 at 90.7 kg) |
| **Counts** | Everything the day burned that was not strength reps: walking, tracked steps, bike/swim minutes, Cardio-assigned customs, one-off activities |
| **Excluded** | Reps of any kind. They already have five goals; counting them here would pay them twice |

Because the target is what the 5-mile walking goal burns, **walking 5 miles fills the ring
exactly, at any body weight, forever**. A walking-only log scores precisely what it did under
the old miles formula; the parity tests in `ProgressionTest` (`completionParity_*`) are
unchanged and are the standing proof of that.

Implementation: `Calories.cardioKcal` / `cardioTarget` / `cardioFraction`.

## 3. Custom exercises fill a training category

A pinned custom exercise points at one of the four categories the Train tab shows, or at
nothing (`ExerciseGoal.NONE` = calories only, the original behavior).

| Category | Slots it covers | What an assigned exercise contributes |
|---|---|---|
| Upper Body | Push-ups + Curls (200 reps) | reps split evenly across the pair, odd rep to the first |
| Core | Leg Lifts (100 reps) | the full rep count |
| Lower Body | Squats + Calf Raises (200 reps) | reps split evenly across the pair |
| Cardio | the calorie goal above | its distance or time, converted to calories (§4) |

100 reps is therefore 1/6 of the day in any rep category: a two-slot category is worth 200
reps, so 100 fills half of each of its two slots.

Credited reps move out of `DayData.customReps` and into the rep columns, so their calories are
counted exactly once (the per-rep burn is the same either way). Reps left on an exercise that
has since been re-pointed at Cardio stay burn-only rather than vanishing.

Older per-slot goals still resolve: `PUSH`/`CURLS` read as Upper Body, `SQUATS`/`CALF_RAISES`
as Lower Body, in both stored definitions and schema-3 backups.

## 4. A Cardio custom carries how it is measured

Set on the exercise, in the goal picker. Rowing is not cycling is not walking, so the exercise
says what a unit of it is worth:

| Mode | Logged in | Setting | Rate |
|---|---|---|---|
| DISTANCE | miles | Walking pace | 1.2 kcal/kg/mi |
| | | Running pace | 1.6 kcal/kg/mi |
| | | Cycling pace | 0.45 kcal/kg/mi |
| MINUTES | minutes | Light / Moderate / Hard | MET 4 / 6 / 9, burned as `MET × 3.5 × kg / 200` per minute |

Its work is converted to **walk-equivalent miles** — the miles of walking that burn the same —
and lands in `DayData.cardioEquivMiles`. Body weight cancels out of both conversions, so the
ratio is fixed and history never depends on today's weigh-in.

`cardioEquivMiles` is deliberately **not** part of `walkMiles`. A cycled mile is not a walked
one: it must never touch the walking achievements, walk streaks, lifetime walked miles, or the
Hero "Lifetime miles walked" row.

## 5. Attributes as shipped

Supersedes the table in `Leveling System.md` §5.

| Stat | Built from | Formula |
|---|---|---|
| STRENGTH | Push-ups + Squats + Curls, lifetime | `floor(sqrt(reps / 50))` |
| ENDURANCE | **All cardio**, lifetime, as walk-equivalent miles | `floor(sqrt(miles × 0.75))` |
| AGILITY | Leg Lifts + Calf Raises, lifetime | `floor(sqrt(reps / 60))` |
| DISCIPLINE | Days at or above 80% completion | `days` (1 point per day) |
| CONSISTENCY | Strength streaks | `longest + current / 2` |

ENDURANCE changed in v0.5.1. It was lifetime walked miles, so an hour on a bike built no
endurance at all. It now reads `CharacterState.enduranceMiles`, the same walk-equivalent
currency the Cardio goal uses, which for a walking-only log is identical to walked miles — no
existing character moved. The Hero screen shows both totals so the input is never a mystery.

The v0.3.7 (DISCIPLINE) and v0.3.8 (ENDURANCE ×0.75, was ×4) rebalances are also reflected
here; the older multipliers in `Leveling System.md` are design history.

## 6. Storage

Resolved at read time from the day row plus the exercise definitions, never denormalized.

| Where | What |
|---|---|
| `workout_day.customReps` | `"id:reps"` — rep customs (any category, or none) |
| `workout_day.customDistance` | `"id:hundredthsOfAMile"` — DISTANCE-mode cardio customs |
| `workout_day.customMinutes` | `"id:minutes"` — MINUTES-mode cardio customs |
| `workout_day.cardioMinutes` | `"id:minutes"` — the built-in Bike (MET 8) and Swim (MET 7) |
| DataStore `custom_exercises` | `id<\|>name<\|>archived<\|>GOAL<\|>MODE<\|>RATE<\|>INTENSITY` |

Room schema **v12**. v11 added `customDistance`, v12 added `customMinutes`; both are plain
`ADD COLUMN ... DEFAULT ''` migrations, so no logged day is ever rewritten.

Backup schema **5**. Days carry `customDistance` and `customMinutes`; custom exercises carry
`goal`, `cardioMode`, `cardioRate`, `cardioIntensity`. Every field is optional on restore and
falls back to its default, so schema 1-4 backups restore with their meaning intact.

CSV export appends `cardiokcal,cardiotarget` after `totalmiles`, since the cardio sixth of
completion is no longer readable from the miles column.

## 7. Invariants to keep

- Walking 5 miles = exactly 1.0 cardio, at every body weight. If a change breaks
  `completionParity_*`, it broke this.
- `cardioEquivMiles` never enters `walkMiles`, lifetime walked miles, or walking achievements.
- Reps never count toward Cardio; cardio never counts toward STR or AGI.
- Assigning a custom to a category changes completion and stats but **never** the day's
  calories or XP — the same work burns the same either way.
- No multipliers or bonus layers anywhere. See `XP Simplification Spec.md`; every prior "small
  bonus" was deliberately removed.
