# Leveling System

> ## ⚠ XP MODEL SUPERSEDED — see `XP Simplification Spec.md`
> The shipped XP model (v0.2.0+, 2026-07-05) is **calorie-based with zero multipliers**:
>
> `dayXp = burn − max(0, dailyBurnTarget − burn) + diet`
>
> - **burn**: gross activity kcal (body-weight-scaled MET), 1 kcal = 1 XP — there are NO
>   per-rep XP rates and NO walking soft cap (§2 below is design history).
> - **shortfall**: past days that burn under the personal daily target (~25% of BMR) lose the
>   gap; fully skipped days lose the whole target (permanent decay).
> - **diet**: (BMR + burn) − calories eaten, symmetric 1:1, uncapped; the last entered intake
>   carries forward until changed.
> - **§3 multipliers are ALL removed.** Quests and achievements pay no XP (badges only).
>
> Still accurate below: the level curve (§4, `100 × n^1.5`), attributes (§5), ranks & titles
> (§6 — as amended in code), and the deterministic-replay principle (§8). Prestige (§7 note)
> was never built.

Defines how the six tracked metrics convert into XP, levels, attributes, and progression. All numbers are tuned against the **actual 30-day data** so the user starts the app already feeling powerful, and so realistic daily effort produces a satisfying-but-not-trivial climb.

---

## 1. Design Goals

1. **A great day ≈ one level early on.** Hitting ~100% completion should feel like meaningful progress.
2. **Reward consistency over heroics.** Multipliers favor streaks and showing up, not single huge days.
3. **Reward strength more than walking.** Walking is already automatic (30/30 days); XP rates push the user toward the strength work the data shows they skip.
4. **Generous early curve, asymptotic later.** Fast levels at first (hook), slower prestige climb later (long-term).
5. **No dead actions.** Even a 20-rep "bare minimum" day earns visible XP.

---

## 2. XP From Exercise (Base Rates)

XP is earned **per unit** of work, so partial effort always counts.

| Exercise | Base XP | Rationale |
|---|---|---|
| Push-ups | **1.0 XP / rep** | Compound, hardest to keep consistent → highest reward |
| Squats | 1.0 XP / rep | Compound |
| Curls | 0.8 XP / rep | Isolation |
| Leg Lifts | 0.7 XP / rep | Easier, higher historical completion |
| Calf Raises | 0.6 XP / rep | Easiest, highest historical completion |
| Walking | **20 XP / mile** | Capped contribution; already a solved habit |

**Daily walking soft cap:** XP from walking counts fully up to the 5-mile target (100 XP), then at **25%** beyond, so the user can't farm levels by only walking — they must train to climb efficiently.

### 2.1 Sanity Check Against Real Days

| Day (sample) | Work | Base XP |
|---|---|---|
| 2025-06-02 (all 100s, 3 mi) | 500 reps-equiv + 60 mi-XP | ≈ **440 XP** |
| 2025-06-30 (all 100s, 5 mi) | 500 strength + 100 walk | ≈ **480 XP** |
| 2025-06-06 (skip, 3 mi only) | 60 walk XP | **60 XP** |
| 2025-06-18 (10s + 30 curls, 4 mi) | ~70 strength + 80 walk | ≈ **150 XP** |

A full day lands near ~450–480 XP before multipliers; a collapse day still earns ~60. This spread is intentional.

---

## 3. Bonus Multipliers (Consistency Economy)

Applied to the day's base XP at day-close:

| Multiplier | Condition | Value |
|---|---|---|
| **Completion bonus** | Day ≥100% completion | ×1.5 |
| **Overdrive bonus** | Any metric exceeded target (e.g. 110 reps, 6 mi) | +10% flat |
| **Streak multiplier** | Active strength streak | +5% per day, **cap +50%** (day 10+) |
| **Comeback bonus** | First day back after a break | ×1.25 (anti-shame) |
| **Weak-day bonus** | Workout logged on Wed / Fri / Sat | +20% (data-targeted) |
| **All-quests-clear** | Completed every daily quest | +100 XP flat |

> Multipliers stack multiplicatively for percentages, then flat bonuses add. A 100% Wednesday on a 12-day streak: `450 × 1.5 × 1.2 × 1.5 = 1,215 XP` — a deliberately huge payoff for doing the exact thing the data says is hardest.

---

## 4. Level Curve

XP required to go **from level _n_ to _n+1_**:

```
xpToNext(n) = round( 100 × n^1.5 )
```

| Level | XP to next | Cumulative XP | Realistic days to reach* |
|---|---|---|---|
| 1→2 | 100 | 100 | <1 day |
| 5→6 | ~1,118 | ~2,000 | ~5 days |
| 10→11 | ~3,162 | ~9,000 | ~3 weeks |
| 20→21 | ~8,944 | ~38,000 | ~2 months |
| 50→51 | ~35,355 | ~240,000 | ~10 months |
| 99→100 | ~98,500 | ~1.3M | long-haul |

*Assuming ~450 XP/active day with average multipliers.

### 4.1 Retroactive Starting Level (from the 30-day import)

Estimated base XP over the 30 days (before multipliers):

- Strength reps: 1,410+1,460 (×1.0) + 1,670 (×0.8) + 1,800 (×0.7) + 1,820 (×0.6) ≈ **6,608 XP**
- Walking: 116 mi × 20, soft-capped ≈ **~1,800 XP**
- With modest average multipliers (~×1.2): **≈ 10,000 XP total**

→ The user **imports in around Level 10–12** with momentum, not Level 1. (See `Spreadsheet-to-App Mapping.md` §6.)

---

## 5. Attributes (Stats)

Five RPG attributes, each fed by specific exercises. Stats are **lifetime-volume based** (they only go up, like a JRPG), giving a permanent sense of growth even through rough weeks.

| Attribute | Anime flavor | Fed by | Formula |
|---|---|---|---|
| **STRENGTH (STR)** | Raw power | Push-ups, Squats, Curls | `floor(sqrt(total_reps / 50))` |
| **ENDURANCE (END)** | Stamina / "Hamon breathing" | Walking miles, total session volume | `floor(sqrt(total_miles × 4))` |
| **AGILITY (AGI)** | Speed / reflex | Leg Lifts, Calf Raises | `floor(sqrt(total_reps / 60))` |
| **DISCIPLINE (DIS)** | Willpower | # of days ≥80% completion | `floor(days_80pct × 1.5)` |
| **CONSISTENCY (CON)** | Resolve / "Star Power" | Current + longest streak | `longest_streak + floor(current_streak/2)` |

> **Why these formulas:** square-root scaling means early reps move a stat fast (motivating) while later gains require real volume (meaningful). DISCIPLINE and CONSISTENCY are **behavior stats** — they reward the *act of showing up*, which is the project's whole point, and they're the stats the spreadsheet user currently has no way to see grow.

### 5.1 Imported Stat Snapshot (from real data)

| Stat | Source value | Result |
|---|---|---|
| STR | (1,410+1,460+1,670)=4,540 reps → sqrt(4540/50) | **STR ≈ 9** |
| END | 116 mi → sqrt(464) | **END ≈ 21** |
| AGI | (1,800+1,820)=3,620 → sqrt(3620/60) | **AGI ≈ 7** |
| DIS | 12 days ≥80% × 1.5 | **DIS = 18** |
| CON | longest 5 + current/2 | **CON ≈ 5** |

The lopsided sheet (high END, low STR/CON) becomes *visible* — the character literally looks like a walker who needs to train strength, mirroring reality and motivating balance.

---

## 6. Level Milestones & Rewards

Reaching milestone levels unlocks cosmetic/aesthetic rewards (no gameplay paywalls — everything is intrinsic).

| Level | Title (anime-flavored) | Unlock |
|---|---|---|
| 5 | **Awakened** | New character portrait frame; first "aura" color |
| 10 | **Stand User** | Unlock a second theme palette |
| 15 | **Hamon Adept** | Animated level-up effect upgrade |
| 20 | **Ascendant I** | New background art for character sheet |
| 25 | **Iron Will** | Custom title editor unlocked |
| 30 | **Crimson Resolve** | Gold/holographic badge styling |
| 40 | **Beyond Limits** | "Overdrive" UI theme |
| 50 | **The Pinnacle** | Prestige border + particle effects |
| 75 | **Transcendent** | Secret palette |
| 100 | **ASCENDANT** | Endgame title + full holographic UI |

After 100, **Prestige** (optional reset of level for a permanent ★ marker and a small XP bonus) is available for the very long term — purely opt-in.

---

## 7. Rank Tiers (Solo-Leveling Flavor)

Overlaid on levels, a letter rank gives an at-a-glance identity shown on the dashboard:

| Rank | Level range |
|---|---|
| E | 1–4 |
| D | 5–9 |
| C | 10–19 |
| B | 20–34 |
| A | 35–49 |
| S | 50–74 |
| SS | 75–99 |
| **National-Level Hunter** | 100+ |

The import drops the user in around **Rank C**, immediately signaling "you've already come far — keep climbing."

---

## 8. Anti-Exploit & Integrity Notes

- This is a **personal honesty tool**; there's no incentive to cheat, but caps (walking soft cap, streak multiplier cap) keep the curve honest so progress stays meaningful to *you*.
- Editing a past day recomputes XP/level/stats deterministically (idempotent recompute), so corrections never desync the character state.
- All progression math is **pure and replayable** from the entry log — the character can always be perfectly rebuilt from raw entries (critical for the import and for backups; see Technical Architecture).
