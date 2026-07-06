# Passive Activity Tracking via Health Connect

Status: **BUILT** (v0.1.3, versionCode 4). Shipped in package `com.mhurston.ascendant.health`
(`HealthConnect.kt` + `PassiveSync.kt`), DB migrated v7→v8, dependency pinned to
`connect-client:1.1.0-alpha10` (newer 1.1.0 stable needs compileSdk 36 / AGP 8.9.1 — see §8).
Goal: steps/activity the phone already collects (like Pokémon GO Adventure Sync, Orna, MH Now)
feed ASCENDANT automatically, awarding **full XP** with no manual logging.

---

## 1. Decisions locked in

- **Source:** Health Connect (not the raw step-counter sensor). It aggregates steps,
  distance, *and active calories* from the phone **and** any watch / other fitness app, which
  is "more" per the ask. Raw sensor stays as a possible fallback (§7) but isn't the plan.
- **XP rate:** passive activity earns **full XP**, same 1 kcal = 1 XP as logged workouts.
  No reduced rate.
- **Distribution:** personal sideload → **no privacy policy / Play data-safety form needed.**
  (Those are Play Store requirements only.)
- **Passive distance is XP-only** — it does **not** build the Endurance stat or count toward
  the 5-mile goal. Deliberate logged `miles` stays the sole driver of those. (was §6 #1)
- **Passive movement keeps your streak alive** — a real-movement day (`passiveSteps ≥ ~1000`)
  counts as activity, so it sustains the activity streak and resets the idle-decay anchor.
  Strength streak stays strength-only. (was §6 #2)

## 2. How it works (the short version)

The OS already tallies steps/distance/calories in a low-power hardware path, even with every
app closed. We don't run GPS or a foreground service — we just **read** Health Connect's
aggregated totals periodically and on app open, compute the delta since we last banked, and
fold the calories into the existing burn → XP engine. Battery cost ≈ zero.

## 3. Where it plugs into existing code

The XP engine is already 100% calorie-driven, so passive activity needs **one new burn
channel** and nothing else conceptual changes.

- `Calories.activityBurn()` (`domain/Calories.kt:51`) currently returns
  `walk + strength + cardio + oneOffKcal`. Add a **`passiveKcal`** term.
- `Progression.baseXp()` is `activityBurn × 1`, so passive kcal automatically earns full XP
  exactly like everything else. **No change to Progression needed.** *(v0.2.0 note: the
  completion/streak/deficit multipliers this doc originally mentioned were removed — flat
  1 kcal = 1 XP now, see `XP Simplification Spec.md`. Passive kcal still counts toward the
  daily burn target that drives the shortfall penalty.)*
- The Energy screen's "activity" line reads the same `activityBurn`, so it stays consistent
  for free.

### Data model additions
`DayData` (`domain/Models.kt:11`) and `WorkoutDayEntity` (`data/WorkoutDayEntity.kt:14`):

```kotlin
// DayData + WorkoutDayEntity
val passiveSteps: Int = 0      // steps banked from Health Connect for this day
val passiveKcal: Int = 0       // active calories banked (preferred kcal source; see below)
```

Then:
```kotlin
// Calories.activityBurn(): take the GREATER of the device's measured active-calories and our
// own step-based estimate, both via the shared gross walking model (~1.2 kcal/kg/mile). Phone
// pedometers report a low net number, so max() keeps plain walking honest without discarding
// real workout data (max, not sum — both describe the same steps).
val stepEstimate = walkKcal(p.weightKg, day.passiveSteps / STEPS_PER_MILE)
val passive = maxOf(day.passiveKcal.toDouble(), stepEstimate)
return walk + strength + cardio + passive + day.oneOffKcal
```
`STEPS_PER_MILE ≈ 2000` (could later refine from height/stride).

Room is denormalized (one row per day), so this is two nullable-defaulted columns + a Room
**migration** (bump DB version; add columns with default 0). Day rebuild stays a pure replay.

## 4. Health Connect integration

- **Dependency:** `androidx.health.connect:connect-client` (check latest stable at build time).
- **Manifest:**
  - `<uses-permission android:name="android.permission.health.READ_STEPS" />`
  - `<uses-permission android:name="android.permission.health.READ_ACTIVE_CALORIES_BURNED" />`
  - `<uses-permission android:name="android.permission.health.READ_DISTANCE" />` (optional, §6)
  - Health Connect permission rationale `<activity-alias>` / intent-filter
    (`androidx.health.connect.action.SHOW_PERMISSIONS_RATIONALE`) so the system permission
    screen can deep-link back.
- **Availability:** `HealthConnectClient.getSdkStatus()`. On Android 14+ Health Connect is
  built into the OS; older devices may need the Play Store app → handle "unavailable" by just
  hiding the feature (logged workouts still work).
- **Permission flow:** request the read permissions via the Health Connect permission
  contract from a settings toggle ("Sync steps & activity"). Must be explicitly enabled by the
  user — no silent access.

### Reading & banking
- Use `aggregate()` over a time range for `StepsRecord.COUNT_TOTAL` and
  `ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL`, bucketed by local day.
- Store a small cursor (last-synced timestamp). On each sync, re-read **today** (and a short
  back-window, e.g. last 2–3 days, since wearables backfill late) and overwrite that day's
  `passiveSteps`/`passiveKcal` with the authoritative aggregate. Overwrite-not-add avoids
  double counting.
- **Triggers:**
  1. On app foreground (immediate, cheap).
  2. A **WorkManager** periodic job (~every 1–2 h) so days close out even if the app isn't
     opened — this is what makes it feel "passive."

## 5. UI surface

- **Hero / Energy screen:** a "Steps today" readout + ring, alongside the existing burn rings.
- One-time onboarding card: "Sync steps & activity from Health Connect" → triggers permission.
- Settings toggle to enable/disable; show last-sync time.

## 6. Settled decisions (implementation detail)

These are decided — listed here so the build follows them exactly:

1. **Passive distance = XP-only.** Add `passiveSteps`/`passiveKcal` as their own burn channel;
   do **not** add them to `miles`, the END stat (`Progression.kt:161`), or the 5-mile goal.
2. **Passive movement sustains the activity streak.** Extend `hasActivity` (`Models.kt:38`) so
   `passiveSteps >= PASSIVE_ACTIVITY_THRESHOLD` (≈ 1000) counts as activity — this also resets
   the idle-decay "last active" anchor. Strength streak stays strength-only.
3. **kcal source priority:** trust measured `passiveKcal`; fall back to a step estimate only
   when no active-calorie record exists. (Reflected in §3.)
4. **Back-window:** re-read the last **2–3 days** each sync to absorb late wearable backfill.

## 7. Fallback (only if Health Connect is unavailable on a device)

Raw `Sensor.TYPE_STEP_COUNTER` (cumulative since boot) + `ACTIVITY_RECOGNITION` permission,
banked via the same WorkManager job. Self-contained, no Google dependency, but steps-only (no
watch/active-calorie data). Not the primary plan; documented so we don't re-derive it.

## 8. Build checklist (DONE)

- [x] Add `connect-client` dependency. **Pinned `1.1.0-alpha10`** — the 1.1.0 stable/rc line
      requires compileSdk 36 + AGP 8.9.1; alpha10 is the newest that compiles against SDK 35 /
      AGP 8.7.3 and still has every aggregate + permission API we use.
- [x] Manifest: `READ_STEPS` + `READ_ACTIVE_CALORIES_BURNED`, `<queries>` for the provider
      package, `SHOW_PERMISSIONS_RATIONALE` intent-filter (≤A13) + `ViewPermissionUsageActivity`
      alias (A14+). (Skipped `READ_DISTANCE` — passive distance is never used.)
- [x] `HealthConnect.kt` wrapper: `getSdkStatus` availability, permission contract,
      `aggregateGroupByPeriod` daily read of steps + active calories.
- [x] `passiveSteps` / `passiveKcal` on `DayData` + `WorkoutDayEntity`; Room migration `MIGRATION_7_8`.
- [x] `Calories.activityBurn()` passive term + `STEPS_PER_MILE = 2000` (XP-only; not in `miles`/END).
- [x] `hasActivity` now true when `passiveSteps >= PASSIVE_ACTIVITY_THRESHOLD` (1000), sustaining
      the activity streak + resetting idle-decay; strength streak stays strength-only.
- [x] `PassiveSync`: WorkManager periodic job (2 h) + foreground sync in `MainActivity.onResume`.
      Banking overwrites (re-reads a 3-day back-window) so re-sync never double counts.
- [x] Hero "Steps today" ring (appears once steps > 0) + Energy "Sync steps & activity" card
      (toggle, permission flow, last-sync time, steps today).
- [x] Tests: `ProgressionTest` — passive kcal earns full XP & is XP-only; step estimate fallback;
      passive movement sustains activity streak (not strength); below-threshold ignored; re-sync
      idempotency. All green.
- [x] Version bump → versionCode 4 / versionName 0.1.3.
</content>
</invoke>
