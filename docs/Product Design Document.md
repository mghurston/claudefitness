# Product Design Document — ASCENDANT

The master spec **as originally designed (June 2026)**. Cross-references: `Leveling System.md`, `Achievement System.md`, `Style Guide.md`, `User Flows.md`, `Wireframes.md`, `Technical Architecture.md`, `Video Integration.md`.

> **⚠ Reward economy superseded (v0.2.0–v0.2.2, 2026-07-05).** The shipped XP model is flat
> calorie-based with zero multipliers — see `XP Simplification Spec.md` (authoritative).
> In particular, wherever this doc mentions: XP multipliers / comeback bonuses / "multipliers
> applied at day-close" (removed), quest XP or the +100 All-Clear (quests are badges only),
> or **mood / energy / RPE tags** (removed in v0.2.2 — the journal is notes-only).
> The core loop, quest structure, streaks-as-display, and screen inventory remain accurate.

---

## 1. Vision Recap

A single-user, offline-first Android RPG that reskins the user's six-metric workout spreadsheet as character progression. Full rationale in `Product Vision.md`. The design over-indexes its reward economy on **strength consistency** and **streaks**, because the 30-day data shows walking is already a solved habit (30/30 days) while strength collapses (longest streak: 5 days; 9 total-skip days; Wed/Fri/Sat cliffs).

---

## 2. The Core Loop

```
        ┌──────────────────────────────────────────────┐
        │  1. OPEN  → animated dashboard, streak flame   │
        │  2. BRIEFING → today's quests + daily mission  │
        │  3. TRAIN → (optional) tap exercise → video    │
        │  4. LOG → one-tap reps/sets/miles, ring fills  │
        │  5. EARN XP → numbers fly, bar fills (instant)  │
        │  6. LEVEL / PR / ACHIEVEMENT → flare animation  │
        │  7. STREAK++ → flame grows, loss-aversion hook  │
        │  8. STATS RISE → character sheet reflects you   │
        │        ↘ reminder (day-aware) brings you back ↙ │
        └──────────────────────────────────────────────┘
```

**Micro-loop (per exercise, seconds):** tap exercise → quick-add reps (+10/+25/custom or "set done") → ring + XP update instantly with sound/haptics.
**Macro-loop (per day):** clear daily quests → close day → multipliers applied → streak preserved/extended.
**Meta-loop (weeks/months):** levels, ranks, stat growth, monthly review, achievement ladder.

Every loop stage has an **immediate** payoff (goal-gradient + variable reward) and a **persistent** payoff (stats/streak that you fear losing). See `Product Vision.md` §3 for the psychology mapping.

---

## 3. Exercise Tracking

### 3.1 Supported Exercises (v1)

| Exercise | Category | Primary unit | Secondary fields | Daily target |
|---|---|---|---|---|
| Push-ups | Strength | reps | sets, notes | 100 |
| Squats | Strength | reps | sets, notes | 100 |
| Leg Lifts | Strength | reps | sets, notes | 100 |
| Calf Raises | Strength | reps | sets, notes | 100 |
| Curls | Strength | reps | sets, weight, notes | 100 |
| Walking | Cardio | miles | duration, notes | 5 |
| **+ Custom** | Strength/Cardio/Custom | reps/miles/minutes/count | configurable | user-set |

### 3.2 Logging Model — every field the user asked for

Each `ExerciseEntry` supports, as applicable to its unit:

- **Reps** — total count (quick-add buttons +5/+10/+25, or keypad).
- **Sets** — optional set-by-set entry (e.g. `4 × 25`); the app sums to total reps. Stored as `sets_json`.
- **Time / duration** — seconds/minutes (for planks, timed walks, future exercises).
- **Distance** — miles (walking, running).
- **Weight / resistance** — optional, for curls or future weighted work.
- **Notes** — free text (form cues, soreness).
- **Mood / energy / RPE** — optional day-level tags (anti-burnout signal).

> **Backward compatibility:** the imported sheet only has *total reps*; sets/time/notes are simply empty for historical rows. New rows can use the richer model without breaking the completion formula (which only reads total reps / miles). See `Spreadsheet-to-App Mapping.md`.

### 3.3 Logging UX Principles

- **One-tap minimum.** A "Quick Log Today's Target" button fills every exercise to 100% for power-days.
- **Partial credit always visible.** Logging 30 reps fills 30% of that exercise's bar and earns XP — breaking the all-or-nothing pattern.
- **Reversible.** Any entry is editable; XP/stats recompute deterministically.
- **Frictionless.** No required fields beyond the one number; everything else optional.

### 3.4 Custom Exercises (extensibility)

User defines: name, category, icon, unit, daily target, and weight mode (equal-weight vs pinned-core; see Mapping §4). New exercise immediately joins quests, XP, stats (mapped to a chosen attribute), and the completion formula.

---

## 4. Character Progression (summary — full spec in `Leveling System.md`)

- **XP** from every rep/mile (strength weighted higher than already-automatic walking).
- **Levels** via `xpToNext(n)=round(100·n^1.5)` — fast early, asymptotic late. Import lands the user ~Level 10–12.
- **Five attributes** — STR, END, AGI, DISCIPLINE, CONSISTENCY — with sqrt-scaled, lifetime-volume formulas. DISCIPLINE & CONSISTENCY are behavior stats that reward showing up.
- **Ranks** E→D→C→B→A→S→SS→National-Level (Solo-Leveling flavor).
- **Milestone unlocks** are cosmetic only (palettes, frames, effects) — no pay-walls, no gameplay locks.

**Exercise → stat conversion (at a glance):**

```
Push-ups, Squats, Curls   → STRENGTH
Walking, total volume       → ENDURANCE
Leg Lifts, Calf Raises      → AGILITY
Days ≥80% complete          → DISCIPLINE
Streak length               → CONSISTENCY
```

---

## 5. Daily Quest System

Three nested cadences keep short-term and long-term motivation alive. Quests are **generated, rotating, and partly day-aware** (variable reward + implementation intentions).

### 5.1 Daily Quests (3–4 per day)

| Type | Example | Reward |
|---|---|---|
| **Core mission** | "Reach 100% completion today" | XP + streak |
| **Targeted** | "Complete 100 push-ups" (the metric you skip most) | XP |
| **Bare-minimum safety** | "Do at least 20 reps of anything" (protects streak) | small XP, streak shield |
| **Bonus (variable)** | "Walk 4 miles" / "Hit one Overdrive" | bonus XP |

On **Wednesday / Friday / Saturday**, the daily set shifts to a lighter, more achievable "**Boss Rush**" framing with a **+20% weak-day XP bonus**, directly targeting the data's collapse days.

### 5.2 Weekly Quests (reset Monday)

- "Train strength 5 of 7 days." (beats current ~4.2/week active rate)
- "Walk 25 miles this week." (achievable: avg is ~27/wk)
- "Beat last week's total reps."
- "Hold a 5-day strength streak." → graduates to 6, 7… as records fall.

### 5.3 Monthly Quests / Challenges

- "Beat your imported baseline month (8,160 reps)."
- "Raise your longest streak record."
- "Average ≥70% on Wed/Fri/Sat this month." (the Pattern-Breaker arc)
- "Complete all four weekly quest sets."

### 5.4 Quest Design Rules

- Always at least one quest the user can **definitely** clear (anti-shame, keeps momentum).
- Difficulty auto-scales off the user's trailing 14-day average (personal, fair).
- Clearing **all** daily quests grants a flat +100 XP "All Clear" bonus + an animated stamp.

---

## 6. Streak System

The single highest-leverage motivator, given the data. Full integrity rules in `Technical Architecture.md`.

### 6.1 Streak Types

| Streak | Definition |
|---|---|
| **Daily activity streak** | Consecutive days with *any* logged activity |
| **Strength streak** | Consecutive days with ≥1 strength exercise (the headline streak — record to beat: 5) |
| **Perfect streak** | Consecutive 100% days |
| **Per-exercise streaks** | e.g. "12-day calf-raise streak" micro-motivators |
| **Walking streak** | Already at 30+ — surfaced as a flex, not a focus |

### 6.2 Recovery & Grace (anti-burnout core)

- **Streak Freeze / Shield:** the user earns 1 freeze per 7 perfect days (max 2 banked). A missed day auto-consumes a freeze instead of breaking the streak. Loss aversion stays real (they're scarce) without being brutal.
- **Grace window:** a day can be logged until ~3 AM local (late-night workouts count for the prior day).
- **Planned rest day:** explicitly marking a rest day pauses (does not break) the strength streak — distinguishing strategy from failure.
- **Comeback quest:** breaking a streak spawns a "Rise Again" quest worth bonus XP and a ×1.25 comeback multiplier — turning failure into a re-entry hook, never a red zero.

### 6.3 Anti-Burnout Mechanics

- **Overtraining nudge:** 10+ day streak with rising volume → gentle "Recovery is part of training" suggestion + a guilt-free rest option.
- **No punishment UI:** broken streaks animate as "embers, ready to reignite," not failure.
- **Deload weeks:** optional monthly lighter-target week that still counts.

---

## 7. Progress Visualization (screens)

Detailed layouts in `Wireframes.md`; aesthetic in `Style Guide.md`. Seven core surfaces:

1. **Daily Dashboard** — completion ring, streak flame, today's quests, level/rank, XP bar, quick-log.
2. **XP / Level bar** — persistent on dashboard; animated fill + level-up takeover.
3. **Character Sheet** — portrait, rank, five stat bars (radar + bars), titles, equipped palette.
4. **Achievement Screen** — trophy grid by rarity/category, progress bars, locked "???" mysteries.
5. **Workout Log** — chronological history; per-day breakdown; edit; mirrors the old sheet rows.
6. **Historical Analytics** — calendar heatmap (GitHub-style), per-exercise trend lines, day-of-week chart (surfaces the Wed/Fri/Sat pattern), moving averages, PR markers.
7. **Monthly Review** — auto-generated "season recap": totals, best day, streak record, new achievements, stat deltas, next-month challenge.

---

## 8. Video Integration (summary — full research in `Video Integration.md`)

Per-exercise, the app links **multiple YouTube creators** with **rotation/randomization** and **favorites/bookmarks**. v1 uses lightweight curated **deep-links / embeds** (no heavy API dependency), with optional YouTube Data API for richer metadata later. Licensing, API, and offline limits are documented in `Video Integration.md`.

---

## 9. Notifications & Reminders

- **Day-aware:** stronger nudges on Wed/Fri/Sat (the cliffs).
- **Streak-protective:** "Your 8-day streak ends in 3 hours — 20 reps saves it."
- **Celebration:** level-ups, PRs, achievements can post a notification.
- **Quiet hours & full opt-out** in Settings (personal tool — user controls everything).
- Local notifications only (no server; offline-first).

---

## 10. Settings & Personalization

Targets/formula mode, theme palette (unlocked via levels), notification rules, units (mi/km), streak-freeze policy, data backup/export/import, custom exercises, and a "reset/prestige" option. Everything is local and user-owned.

---

## 11. Out of Scope (v1)

Social, multi-user, monetization, iOS/web, live wearable sync (see `Future Enhancements.md`). Anti-goals reaffirmed from `Product Vision.md` §6.

---

## 12. Open Decisions for Approval

| # | Decision | Recommended default |
|---|---|---|
| D1 | App name | **ASCENDANT** (changeable) |
| D2 | Completion weighting when adding custom exercises | **Pinned-core** (originals stay 1/6) |
| D3 | Streak freezes per week | 1 earned / 7 perfect days, max 2 banked |
| D4 | Walking emphasis | De-emphasized (soft XP cap) — reward strength |
| D5 | Video integration depth in v1 | **Deep-link/embed, no API key** |

These are flagged for sign-off before implementation per the project's "approve before coding" rule.
