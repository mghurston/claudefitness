# User Flows

Step-by-step flows for the six required journeys. Notation: `→` next step, `[Screen]`, `(decision)`, `★` animation/feedback moment, `⚠` edge case. Screen layouts are in `Wireframes.md`.

---

## Flow 1 — First Launch & Onboarding

Goal: get the user from install to an already-leveled character in under 2 minutes, proving immediate value over the spreadsheet.

```
[Splash ★logo flare]
  → [Welcome] "You are not tracking workouts. You are leveling up."
  → [Set Identity] enter hero name / pick starter portrait + palette
  → [Confirm Targets] pre-filled 100/100/100/100/100 reps + 5 mi (editable)
  → (Import history?) ──yes──> [Import] pick .csv/.xlsx OR use bundled seed_history.csv
  │                              → ★progress: "Replaying 30 days…"
  │                              → [Import Summary] "30 days · 116 mi · 8,160 reps
  │                                 → Level 11 · Rank C · 18 achievements unlocked!"
  │                              → ★trophy shower + level-up takeover
  │                    ──no───> [Fresh Start] Level 1, Rank E
  → [Quick Tutorial] 3 cards: log, quests, streak (skippable)
  → [Dashboard]  ← lands here
```

⚠ No file to import → offer "Start fresh" or "Import later from Settings."
⚠ Import row with bad data → skip row, log it, continue; show count in summary.

---

## Flow 2 — Daily Workout (the primary loop)

```
[Dashboard]
  → reads: completion ring (0%), streak flame, today's quests, XP bar
  → (work out now or just log?)
  ├─ TRAIN: tap an exercise card → [Exercise Detail]
  │     → ★rotating creator video thumbnail → tap → YouTube/embedded player
  │     → return, do the reps
  └─ LOG directly ↓
  → on [Exercise Detail] or quick-log sheet: enter reps (+10/+25/set-done/keypad)
  → ★ ring fills + "+120 XP" flies up + haptic tick + sound
  → repeat per exercise (partial credit always counts)
  → (all targets met?) → ★ Overdrive glow if exceeded
  → quests auto-tick as thresholds cross → ★ "Quest Cleared!" stamp
  → end of day (auto at ~3AM grace OR manual "Close Day")
  → ★ multipliers tally screen → streak++ → flame grows
  → (level up?) → Flow 5   (achievement?) → Flow 6
```

⚠ Logged after midnight but before 3 AM → counts for previous day (grace window).
⚠ User edits a past entry → recompute XP/stats/streak deterministically; toast "Recalculated."

---

## Flow 3 — Logging Activity (micro-flow, all field types)

```
[Quick-Log sheet]  (from dashboard FAB or exercise card)
  → choose exercise (or "Quick Log Target" = fill all to 100%)
  → enter value:
       reps  → quick-add chips or keypad
       sets  → optional "add set" rows (4×25 → auto-sum 100)
       miles → decimal keypad / slider
       time  → mm:ss picker (timed exercises)
  → optional: weight, notes, mood/energy tags
  → [Save] → ★ instant XP + bar update → sheet dismisses
```

⚠ Value exceeds target → accepted (Overdrive), not capped in data; ring caps visually at 100%.
⚠ Zero/empty save → no-op (no phantom entries).

---

## Flow 4 — Viewing Progress

```
[Dashboard] → bottom nav
  ├─ [Character] portrait, rank, 5 stat bars + radar, titles
  │     → tap a stat → "fed by Push-ups, Squats, Curls — +3 this week"
  ├─ [Analytics]
  │     → calendar heatmap (tap a day → that day's breakdown)
  │     → per-exercise trend lines (toggle exercises)
  │     → day-of-week bar chart (Wed/Fri/Sat highlighted red)
  │     → moving averages + PR markers
  ├─ [Log] chronological days (mirrors old sheet) → tap → edit
  └─ [Achievements] grid by category/rarity → tap → detail + progress
  → [Monthly Review] (auto-prompt on the 1st, or from Analytics)
        → season recap: totals, best day, streak record, stat deltas, next challenge
```

---

## Flow 5 — Leveling Up

```
(trigger: XP crosses threshold during log/day-close)
  → ★ screen takeover: aura burst, "LEVEL UP!" + new level number slams in
  → stat increases roll up (+1 STR etc.)
  → (milestone level? e.g. 10/20/30…)
       → ★ unlock card: "Rank up: C → B" / new palette / new frame
       → (optional) "Equip now?" → applies theme
  → "Continue" → returns to prior context (dashboard/log)
```

⚠ Multiple levels in one log (big import day) → queue takeovers, allow "skip all" → land on final level.

---

## Flow 6 — Completing an Achievement

```
(trigger: condition met at log or day-close)
  → ★ toast/banner slides in: badge art + name + rarity glow + "+500 XP"
  → tap banner → [Achievement Detail] (art, flavor text, date earned, next tier)
  → OR dismiss → continues loop
  → badge now lit in [Achievements] grid
  → (Mythic/Legendary?) → ★ full-screen cinematic unlock
  → (hidden 🔒 unlocked?) → "??? revealed:" reveal animation
```

⚠ Several achievements at once (import) → collapse into a single "18 unlocked — view all" summary card to avoid spam.

---

## Cross-Cutting Edge Cases

| Case | Handling |
|---|---|
| Missed yesterday | Auto-consume Streak Freeze if banked; else streak breaks → Comeback quest spawns |
| Planned rest day | User marks rest → streak pauses, no XP penalty, no break |
| Wrong-day entry | Editable date on any entry; full recompute |
| App reopened mid-day | Resume exactly; nothing to "submit," state is live |
| First Wed/Fri/Sat | Boss-Rush quest framing + weak-day bonus surfaced |
| Backup restore | Character fully rebuilt from entry log (pure replay) |
