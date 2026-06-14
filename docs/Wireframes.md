# Wireframes (Low-Fidelity)

ASCII low-fi wireframes for the six required screens, plus key overlays. These define **layout, hierarchy, and interaction zones** — not final visuals (see `Style Guide.md`). Target: phone portrait, ~360–412dp wide, bottom nav, single-hand reach. `[ ]` button · `( )` icon · `▓/░` filled/empty bar · `◴` ring.

---

## 1. Home / Daily Dashboard

```
┌─────────────────────────────────────┐
│ (≡)        ASCENDANT          (⚙)   │  top bar
│                                       │
│   ╭───────╮   LV 11  ·  RANK C        │
│   │  ◴82% │   "Crimson Apprentice"    │  portrait + completion ring
│   │ portrait│  XP ▓▓▓▓▓▓▓░░░ 6,540/9k │
│   ╰───────╯   🔥 5-day streak (record!)│  streak flame = loss aversion
│                                       │
│  ── TODAY'S MISSION ──────────────    │
│  ◴ Completion  ▓▓▓▓▓▓▓▓░░  82%        │
│                                       │
│  Push-ups   ▓▓▓▓▓░░░░░  50/100  (▶)  │  tap (▶)=video, tap bar=log
│  Squats     ▓▓▓▓▓▓▓░░░  70/100  (▶)  │
│  Leg Lifts  ▓▓▓▓▓▓▓▓▓▓ 100/100 ✓     │
│  Calf Raise ▓▓▓▓▓▓▓▓▓▓ 100/100 ✓     │
│  Curls      ▓▓▓▓▓▓▓░░░  70/100  (▶)  │
│  Walking    ▓▓▓▓▓▓░░░░  3.0/5 mi (▶) │
│                                       │
│  ── DAILY QUESTS ──────────────────  │
│  ☐ Reach 100% today          +200xp  │
│  ☑ Walk 3 miles              +60xp ✓ │
│  ☐ Hump-Day Boss: 100 push-ups +20%  │  (Wed/Fri/Sat highlighted)
│                                       │
│            (  +  QUICK LOG  )         │  FAB
├───────────────────────────────────────┤
│ (🏠)Home (👤)Char (📊)Stats (🏆)Ach    │  bottom nav
└─────────────────────────────────────┘
```

---

## 2. Workout / Exercise Detail Screen

```
┌─────────────────────────────────────┐
│ (←)   PUSH-UPS            STR ⚔       │
│                                       │
│   ◴  50 / 100 reps   ▓▓▓▓▓░░░░░       │  big progress ring/bar
│   Today: 50   ·   Best: 110   ·  PR★  │
│                                       │
│  ── LOG ──────────────────────────   │
│   [ +5 ] [ +10 ] [ +25 ] [ keypad ]  │  quick-add chips
│   Sets: [4 × 25] (+ add set)          │
│   Notes: ____________________         │
│                  [  SAVE  +XX XP  ]   │
│                                       │
│  ── FORM VIDEO ───────────────────   │
│  ┌───────────────┐  Creator: ▼ rotate │  multiple creators, rotation
│  │  ▶ thumbnail   │  "Perfect Push-up" │
│  └───────────────┘  ( ★ favorite )    │
│  ‹ prev   • • ○ •   next ›   (shuffle)│  bookmarked + randomize
│                                       │
│  ── 30-DAY TREND ─────────────────   │
│   ╱╲    ╱╲___╱                        │  sparkline
└─────────────────────────────────────┘
```

---

## 3. Character Sheet

```
┌─────────────────────────────────────┐
│ (←)        CHARACTER                  │
│        ╭───────────────╮              │
│        │   PORTRAIT     │  aura frame │  level-gated frame/palette
│        │   (full body)  │             │
│        ╰───────────────╯              │
│   ⟪ Crimson Apprentice ⟫   LV 11 · C  │  editable title
│   XP ▓▓▓▓▓▓▓░░░  6,540 / 9,000        │
│                                       │
│  ── STATS ──────────  ╭──radar──╮     │
│  STR ▓▓▓▓░░░░░░  9     │   ╱◆╲    │   │  bars + radar pentagon
│  END ▓▓▓▓▓▓▓▓░░ 21     │  ◆   ◆   │   │
│  AGI ▓▓▓░░░░░░░  7     │   ╲◆╱    │   │
│  DIS ▓▓▓▓▓▓▓░░░ 18     ╰─────────╯    │
│  CON ▓▓░░░░░░░░  5  ← "beat your record"│  weak stat called out
│                                       │
│  ── TITLES / BADGES (equipped) ───── │
│  [Never Skip Cardio] [The Five] [+]   │
│  ── UNLOCKS ──  Palette ▸  Frame ▸    │
└─────────────────────────────────────┘
```

---

## 4. Analytics Screen

```
┌─────────────────────────────────────┐
│ (←)   ANALYTICS      [Week|Month|All] │
│                                       │
│  ── CONSISTENCY HEATMAP ───────────  │
│   M T W T F S S                       │
│   ▓ ▓ ░ ▓ ░ ░ ▓   wk1   (GitHub-style)│
│   ▓ ▓ ░ ▓ ░ ░ ▓   wk2                 │
│   ▓ ▓ ▓ ▓ ░ ░ ▓   wk3   ░=skip ▓=done │
│   ▓ ▓ ░ ▓ ▓ ░ ▓   wk4                 │
│                                       │
│  ── DAY-OF-WEEK AVG ───────────────  │
│   Mon ▓▓▓▓▓▓▓▓ 78%                    │
│   Wed ▓▓▓░░░░░ 36%  ⚠ weak            │  red highlight on cliffs
│   Fri ▓▓▓░░░░░ 35%  ⚠                 │
│   Sat ▓▓▓▓░░░░ 39%  ⚠                 │
│                                       │
│  ── EXERCISE TREND ────────────────  │
│   [Push] [Squat] [Walk] …  (toggles)  │
│    ╱╲    ╱╲                           │
│   ╱  ╲__╱  ╲___╱╲   + PR★ markers     │
│                                       │
│  ── TOTALS ──  Reps 8,160 · Mi 116    │
│            [ Monthly Review ▸ ]        │
└─────────────────────────────────────┘
```

---

## 5. Achievement Screen

```
┌─────────────────────────────────────┐
│ (←)   ACHIEVEMENTS    37 / 120        │
│  [All][Streak][Strength][Boss][Hidden]│  category filter
│                                       │
│  ▓▓▓▓▓▓░░░░  progress to "Collector"  │
│                                       │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐          │
│  │ 🏅 │ │ 🏅 │ │ 🔒 │ │ 🏅 │   grid   │  lit vs locked
│  │1000│ │Never│ │ ?? │ │The │          │
│  │Fist│ │Skip │ │ ?? │ │Five│          │
│  └────┘ └────┘ └────┘ └────┘          │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐          │
│  │ ▓▓ │ │ 🔒 │ │ 🔒 │ │ 🟣 │          │  rarity glow (🟣=mythic)
│  │740/│ │Break│ │ ?? │ │Wrld│          │
│  │1000│ │Recrd│ │ ?? │ │Heav│          │
│  └────┘ └────┘ └────┘ └────┘          │
│   ↑ in-progress bar on locked tiles    │
│                                       │
│  (tap tile → detail: art, flavor,     │
│   date earned / progress, next tier)  │
└─────────────────────────────────────┘
```

---

## 6. Settings Screen

```
┌─────────────────────────────────────┐
│ (←)        SETTINGS                   │
│                                       │
│  PROFILE                              │
│   Hero name ............ [Edit]       │
│   Portrait / palette ... [Choose]     │
│                                       │
│  GOALS & FORMULA                      │
│   Daily targets ........ [100/…/5]    │
│   Custom exercises ..... [Manage +]   │
│   Completion mode ...... (Pinned▾)    │
│   Units ................ (Miles▾)     │
│                                       │
│  MOTIVATION                           │
│   Reminders ............ [On]         │
│   Weak-day nudges (W/F/S) [On]        │
│   Quiet hours .......... 22:00–07:00  │
│   Streak freeze policy . (1/wk, max2) │
│                                       │
│  DATA                                 │
│   Export → CSV/XLSX .... [Export]     │
│   Import history ....... [Import]     │
│   Cloud backup ......... [Drive ▾]    │
│   Reset / Prestige ..... [⚠]          │
│                                       │
│  ABOUT  ·  v1.0  ·  single-user       │
└─────────────────────────────────────┘
```

---

## 7. Key Overlays

### Level-Up Takeover
```
┌─────────────────────────────────────┐
│        ✦  ✦   ★ LEVEL UP ★   ✦  ✦    │
│              ╭─────────╮              │
│              │  11 → 12 │  aura burst  │
│              ╰─────────╯              │
│        STR +1   CON +1                │
│   ── RANK UP: C → B ──  (if milestone)│
│        [ Equip new palette? ]          │
│              [ CONTINUE ]              │
└─────────────────────────────────────┘
```

### Achievement Unlock Banner
```
┌─────────────────────────────────────┐
│  🏅  ACHIEVEMENT UNLOCKED   +500 XP   │
│     "Break the Record" — Rare         │
│     6-day strength streak!  (tap ▸)   │
└─────────────────────────────────────┘
```

### Quick-Log Bottom Sheet
```
┌─────────────────────────────────────┐
│  QUICK LOG                       (✕) │
│  [ ⚡ Quick Log Target (all 100%) ]   │
│  Push ▸  Squat ▸  Legs ▸  Calf ▸     │
│  Curls ▸  Walk ▸  (+ custom)          │
│  selected: Push-ups  [+10][+25][⌨]   │
│                       [ SAVE +XX XP ] │
└─────────────────────────────────────┘
```

---

## Layout Notes for Implementation

- **Bottom nav** (Home/Character/Analytics/Achievements) + Settings via gear; 4 primary destinations keeps thumb reach easy.
- **Dashboard is the home base** — everything important is one tap away; the streak flame and completion ring are the two emotional anchors, placed top.
- **Weak-day cues** (Wed/Fri/Sat) appear in red/amber on Dashboard quests and Analytics — the UI itself encodes the data insight.
- **Every log action** produces motion + haptic + sound (see `Style Guide.md` §Animation).
- Animations are **interruptible/skippable** (respect "reduce motion" accessibility setting).
