# XP Simplification Spec (v0.2.0)

**Status: APPROVED by user 2026-07-05. This supersedes every prior XP design.
Do not add any bonus, multiplier, cap, or special case not written here. If a
new rule feels needed, STOP and ask the user first — "stop trying to make more
arbitrary rules" is a direct quote.**

## The whole model (memorize this — it fits in three lines)

```
dayXp = burn − shortfall + diet

burn      = Calories.activityBurn(profile, day)              // gross kcal, 1 kcal = 1 XP, unchanged
shortfall = max(0, dailyBurnTarget − burn)                   // past days only; today is never charged
diet      = (bmr + burn) − caloriesConsumed                  // only when food logged; 1:1, UNCAPPED, signed
```

- Exercise more → gain more. Exercise less than your personal daily burn
  target → lose exactly the gap. Eat under your total burn → gain the deficit
  1:1. Eat over it → lose the surplus 1:1. Nothing else touches XP. Ever.
- `dailyBurnTarget` is the existing `Calories.dailyBurnTarget(profile)`
  (~25% of BMR, rounded to 25s). It is now the ONLY penalty scale — the
  ~730 XP `missedDayPenalty` (reverse-gains formula) is deleted.
- A fully unlogged calendar day scores as `burn = 0, no diet term` →
  loses exactly `dailyBurnTarget`. Same rule, not a separate mechanic.
- Penalties remain **permanent** (earn it back, don't get it back) and only
  apply from `decayAnchor` forward, and never to today-in-progress. That
  machinery stays; only the per-day price changes.

## Deletions (the point of this exercise)

### 1. All multipliers — DELETE
In `domain/Progression.kt`:
- `COMPLETION_BONUS`, `STREAK_BONUS_PER_DAY`, `STREAK_BONUS_CAP_DAYS`,
  `DEFICIT_FULL_BONUS_KCAL`, `DEFICIT_BONUS_MAX`,
  `SURPLUS_FULL_PENALTY_KCAL`, `SURPLUS_PENALTY_MAX_XP` — all gone.
- `dayXp()` becomes the three-line formula above. It no longer takes
  `strengthStreak`.
- Streaks are still TRACKED (stats screen, achievements, CON stat) — they
  just never touch XP again.
- `completion()` stays for stats/achievements/rings display only. It no
  longer gates or scales anything XP-related.

### 2. Rep/mile shortfall + missedDayPenalty — REPLACE
- `missedDayPenalty()` — delete. Both charging sites
  (`Progression.kt:197` logged-day shortfall, `Progression.kt:242` unlogged-day
  decay walk) now charge against `Calories.dailyBurnTarget(profile)` instead.
  - Logged past day: `max(0, target − burn)`.
  - Unlogged past day: full `target`.
- The decay walk no longer needs the day-by-day weight carry for penalty
  sizing (target comes from the profile). Keep weight carry-forward for the
  BURN math (weigh-ins still matter); the penalty is just `target`.
- `interiorPenalty` / `trailingPenalty` / `idleDays` bookkeeping and the
  "permanent, never refunded" rule are unchanged in spirit — only the
  per-day amount changes.

### 3. Quest & achievement XP — DELETE (badges only)
- `Quests.earnedXp()` — delete. Quests stay in the UI as goals/trophies with
  completion states; any "+N XP" labels on quest cards become badge/checkmark
  display only. If `Quests` data classes carry an `xp` field, remove it and
  its UI rendering.
- `Achievements.unlockedXp()` — delete. Achievement rarity payouts become
  cosmetic (keep rarity for display). `AchievementsScreen.kt:55` shows a
  total bonus-XP figure — remove or repurpose as "N unlocked".
- `Progression.withBonuses()` and the **entire fixpoint loop in
  `rebuildFull()`** — delete. `rebuildFull` collapses to: `rebuild()` once,
  then `Achievements.evaluate()` ONCE against that state. No iteration —
  achievements can't move XP anymore, so there is nothing to converge.
- `CharacterState.questBonusXp` / `achievementBonusXp` (`Models.kt:174-175`)
  — delete the fields and every read (`CharacterScreen.kt:130-131` "incl.
  +N quests · +N trophies" caption goes away).
- Level-gated achievements still work: level comes from calorie XP only.

### 4. Intake carry-forward — KEPT (user decision 2026-07-05, reversing the
### first draft of this spec)
- `Progression.carryForward()` carries BOTH weight and `caloriesConsumed`
  forward: "the number you last entered stays in effect until you change it."
  Enter 2000 today → tomorrow uses 2000 for its diet term unless edited; a
  logged fast (0) sticks and carries the same way. Before the first entry
  anywhere, intake is -1 → no diet term.
- Do NOT remove the inheritance again — the user explicitly wants it.

## Accepted consequences — do NOT "fix" these
- **Uncapped diet XP dominates on fasting/deep-deficit days.** A fasted day
  at ~1,900 kcal total burn earns ~1,900 diet XP even with zero exercise.
  User explicitly chose symmetric-uncapped. Leave it.
- **Day XP is frequently negative** (surplus + missed target). Fine —
  effective XP already floors at 0 globally (`coerceAtLeast(0)`), keep that.
- **Levels will shift for existing users** (the log is replayed under new
  rules — that's the whole architecture). No migration/grandfathering.

## Touch points checklist
- `domain/Progression.kt` — dayXp, rebuild penalty sites, delete
  withBonuses/fixpoint, delete missedDayPenalty + all bonus consts.
- `domain/Quests.kt` — delete earnedXp; strip xp from quest defs/UI model.
- `domain/Achievements.kt` — delete unlockedXp; rarity display-only.
- `domain/Models.kt` — drop questBonusXp/achievementBonusXp from
  CharacterState.
- `domain/Calories.kt` — unchanged (gross MET model is settled; `deficit()`
  stays as-is and is now used 1:1).
- UI: `CharacterScreen.kt` (bonus caption, any multiplier text),
  `AchievementsScreen.kt` (bonus XP total), quest cards' XP labels,
  `AppText.kt` / `Components.kt` / `DashboardScreen.kt` /
  `CalendarScreen.kt` / `EnergyScreen.kt` — grep for "bonus", "streak",
  "multiplier", "deficit", "XP" strings and align all copy with the
  three-line model. Anywhere the UI explains XP, it should be explainable in
  one sentence.
- Tests: `ProgressionTest.kt` — rewrite. Lines 283-321 (quest/achievement XP
  assertions) are now asserting deleted behavior. New tests to add:
  1. flat 1:1: dayXp == round(burn) for today (no shortfall), no food logged.
  2. shortfall: past day burning half its target loses the other half.
  3. unlogged past day loses exactly dailyBurnTarget.
  4. diet symmetric: +300 deficit → +300 XP; −300 surplus → −300 XP; unlogged
     food → 0 diet term (no inheritance from yesterday).
  5. fasted day (consumed = 0) earns bmr+burn as diet XP.
  6. rebuildFull == rebuild + one achievements pass; no XP delta between them.
- No DB schema change expected (sentinels unchanged) — if one sneaks in,
  bump DB version with a migration as usual.
- **Version bump**: versionCode +1 AND versionName → 0.2.0 (model change
  warrants a minor bump).

## Verification (device/emulator)
Per CLAUDE.md: source `setenv.ps1`, boot `ascendant_test`, **turn passive
Health Connect sync OFF**, then check: Hero tab total XP matches a hand
computation of the three-line formula over the seeded log; quest cards show
no "+XP"; a day logged with food over burn shows negative day XP in the
calendar. Leave the emulator running.
