# Achievement System

**120 achievements** across 9 categories, plus tiers, rarity, hidden unlocks, and reward rules. Designed so several are **already earned on import** (from the real 30-day data) and the rest form a long ladder targeted at the user's specific weak spots: strength consistency, streaks, and the Wed/Fri/Sat cliffs.

> **Shipped build: 86 achievements** (the `Achievements.ALL` list in `domain/Achievements.kt`).
> This 120 figure is the original design ceiling; 86 were implemented for v1. The achievements
> screen reads the live count from code, so it always matches what's built (e.g. "8 / 86 unlocked").
> Each is evaluated retroactively as a pure function of the day log + character state.

---

## 1. Mechanics

| Property | Detail |
|---|---|
| **Trigger** | Evaluated at each log + at day-close; pure functions over the entry log |
| **Rarity** | Common → Uncommon → Rare → Epic → Legendary → **Mythic** (drives badge styling, see Style Guide) |
| **Reward** | Flat XP bonus + a collectible badge; some grant a title or palette |
| **Hidden** | Some are 🔒 secret until unlocked (shown as "???") for discovery delight |
| **Retroactive** | All milestone/volume achievements evaluate against imported history on first launch |
| **Progress** | Multi-step achievements show a progress bar (e.g. "740 / 1,000 squats") |

**XP rewards by rarity:** Common 100 · Uncommon 250 · Rare 500 · Epic 1,000 · Legendary 2,500 · Mythic 5,000.

---

## 2. First Steps (Onboarding) — Common

1. **First Blood** — Log your very first workout.
2. **The Awakening** — Complete your first full 100% day.
3. **Name Your Stand** — Customize your character name/title.
4. **Day One** — Open the app on its first day.
5. **Hello, World** — Log all six exercises in a single day.
6. **Import Complete** — Migrate your 30 days of spreadsheet history. *(auto-earned on import)*
7. **Set the Target** — Set or confirm your daily goals.
8. **First Quest Cleared** — Complete any daily quest.

## 3. Push-up Mastery (STR) — tiered

9. **Push Start** — 100 lifetime push-ups. *(earned: 1,410 ✓)*
10. **Press Forward** — 500 lifetime. *(✓)*
11. **Thousand Fists** — 1,000 lifetime. *(✓)*
12. **Iron Arms** — 5,000 lifetime.
13. **Ten-Thousand Reps** — 10,000 lifetime.
14. **The Hundred** — 100 push-ups in a single day. *(✓)*
15. **Over Capacity** — 110+ in a day. *(✓ — 2025-06-19)*
16. **Century Streak** — 100+ push-ups on 7 consecutive days.

## 4. Squat Mastery (STR) — tiered

17. **Knees Bent** — 100 lifetime squats. *(✓)*
18. **Leg Day** — 1,000 lifetime. *(✓)*
19. **Pillars** — 5,000 lifetime.
20. **Quad God** — 10,000 lifetime.
21. **Squat Century** — 100 in one day. *(✓)*
22. **Deep Resolve** — 110+ in one day. *(✓)*

## 5. Curls / Leg Lifts / Calf Raises (STR/AGI) — tiered

23. **Curl Up** — 1,000 lifetime curls. *(✓ — 1,670)*
24. **Peak Contraction** — 5,000 lifetime curls.
25. **Guns Loaded** — 120 curls in a day. *(✓ — 2025-06-12)*
26. **Core Ignition** — 1,000 lifetime leg lifts. *(✓ — 1,800)*
27. **Hanging Tough** — 5,000 lifetime leg lifts.
28. **Calf Awakening** — 1,000 lifetime calf raises. *(✓ — 1,820)*
29. **Mountain Stance** — 5,000 lifetime calf raises.
30. **Tiptoe Titan** — 120 calf raises in a day. *(✓ — 2025-06-12)*

## 6. Walking / Endurance (END) — tiered

31. **First Mile** — Walk 1 mile. *(✓)*
32. **Marathoner** — 26.2 lifetime miles. *(✓ — 116)*
33. **Century Walker** — 100 lifetime miles. *(✓)*
34. **500 Club** — 500 lifetime miles.
35. **Cross-Country** — 1,000 lifetime miles.
36. **The Long Road** — Walk 5+ miles in one day. *(✓ — 2025-06-07: 6 mi)*
37. **Six-Mile Soul** — Walk 6+ miles in one day. *(✓)*
38. **Never Skip Cardio** — Walk every day for 30 days straight. *(✓ — 30/30!)*
39. **Pilgrimage** — 50-day walking streak.
40. **Endless Hamon** — 100-day walking streak.

## 7. Streaks & Consistency (CON/DIS) — the core ladder

41. **Spark** — 2-day strength streak.
42. **Momentum** — 3-day strength streak.
43. **The Five** — 5-day strength streak. *(✓ — your current record; tied)*
44. **Break the Record** — 6-day strength streak. *(the first true challenge)*
45. **Lucky Seven** — 7-day streak.
46. **Fortnight Fighter** — 14-day streak.
47. **Three Weeks Strong** — 21-day streak (habit threshold).
48. **The Unbroken** — 30-day streak.
49. **Fifty Resolve** — 50-day streak.
50. **Hundred-Day Hunter** — 100-day streak.
51. **Year of Iron** — 365-day streak.
52. **Perfect Week** — 100% completion 7 days in a row.
53. **Perfect Fortnight** — 100% for 14 straight days.
54. **Flawless Month** — 100% every day in a calendar month.
55. **Comeback Kid** — Return and log within 1 day of breaking a streak.
56. **Phoenix** — Rebuild a 7-day streak after losing a 7+ day streak.
57. **No Zero Days** — 30 days with *some* activity logged (even minimal).
58. **Shield Bearer** — Use a streak-freeze grace day, then keep going.

## 8. Boss Days — targeting Wed/Fri/Sat (data-driven, partly hidden)

59. **Hump-Day Hero** — 100% on a Wednesday. *(your 36% day)*
60. **Wednesday Warlord** — 100% on 4 Wednesdays.
61. **Friday Night Fever** — 100% on a Friday. *(your 35% day)*
62. **TGIF Titan** — 100% on 4 Fridays.
63. **Saturday Slayer** — 100% on a Saturday. *(your 39% day)*
64. **Weekend Won't Win** — 100% on 4 Saturdays.
65. **Cliff Climber** 🔒 — 100% on a Wed, Fri, AND Sat in the same week.
66. **Pattern Breaker** 🔒 — Average ≥70% across all Wed/Fri/Sat in a month.

## 9. Volume & Milestone Totals — tiered

67. **Apprentice (Lv 5)** — Reach Level 5.
68. **Stand User (Lv 10)** — Reach Level 10. *(near on import)*
69. **Rank Up: C** — Reach Rank C.
70. **Rank Up: B** — Reach Rank B (Lv 20).
71. **Rank Up: A** — Reach Rank A (Lv 35).
72. **Rank Up: S** — Reach Rank S (Lv 50).
73. **Double-S** — Reach Rank SS (Lv 75).
74. **ASCENDANT** — Reach Level 100.
75. **10K Reps** — 10,000 total strength reps. *(✓ — 8,160 + climbing)*
76. **25K Reps** — 25,000 total strength reps.
77. **50K Reps** — 50,000 total strength reps.
78. **100K Club** — 100,000 total strength reps.
79. **XP Magnate** — Earn 100,000 lifetime XP.
80. **Millionaire** — Earn 1,000,000 lifetime XP.

## 10. Stat Achievements (attribute thresholds)

81. **Mighty (STR 10)** — Reach STR 10.
82. **Herculean (STR 25)** — Reach STR 25.
83. **Marathon Lungs (END 25)** — Reach END 25.
84. **Unbreathing (END 50)** — Reach END 50.
85. **Nimble (AGI 15)** — Reach AGI 15.
86. **Disciplined (DIS 25)** — Reach DIS 25.
87. **Iron Will (DIS 50)** — Reach DIS 50.
88. **Resolute (CON 15)** — Reach CON 15.
89. **Unwavering (CON 30)** — Reach CON 30.
90. **Balanced Soul** — All five stats ≥ 15 (no weak link).
91. **Well-Rounded Hunter** — All five stats ≥ 25.

## 11. Behavior & Discipline — partly hidden

92. **Dawn Patrol** 🔒 — Log a workout before 7:00 AM.
93. **Night Owl** 🔒 — Log a workout after 10:00 PM.
94. **The Grind** — Log workouts 5 days in a row before noon.
95. **Overdrive** — Exceed every daily target in one day. *(110%+ days ✓)*
96. **Double Overdrive** — Exceed every target on back-to-back days.
97. **Bare Minimum, Still Counts** — Keep a streak alive with a sub-30% day.
98. **All Quests Cleared** — Complete every daily quest in a day.
99. **Weekly Sweep** — Complete every weekly quest.
100. **Monthly Conqueror** — Complete a monthly quest.
101. **Note to Self** — Add notes to 10 workouts.
102. **Self-Aware** — Log mood/energy 7 days running.
103. **Rest is Strategy** — Take a *planned* rest day without breaking your streak.
104. **No Excuses** — Log on a day you marked "low energy."

## 12. Personal Records & Mastery

105. **New Record!** — Set any personal best.
106. **Record Breaker x5** — Beat 5 different PRs.
107. **PR Hunter** — Beat a PR that's older than 30 days.
108. **Total Mastery** — Hold the all-time PR in every exercise simultaneously.
109. **Above the Line** — Beat your previous best monthly average completion.
110. **Bigger Than June** — Exceed your imported baseline month's total reps.

## 13. Legendary / Mythic — long-haul capstones

111. **The Unbroken Hundred** (Legendary) — 100-day strength streak.
112. **Bizarre Adventure** (Legendary) — 365 total active days.
113. **One With the Stand** (Legendary) — Reach Level 50 with all stats ≥ 25.
114. **World Over Heaven** (Mythic) — Reach Level 100.
115. **The Eternal** (Mythic) — 365-day strength streak.
116. **Beyond the Sheet** (Mythic) — 1,000 total logged days (the spreadsheet could never).
117. **Million-Rep March** (Mythic) — 1,000,000 lifetime reps.

## 14. Meta / Easter Eggs — hidden 🔒

118. **Za Warudo** 🔒 — Log a workout at exactly a paused-second timestamp (fun nod).
119. **Sunday Driver** 🔒 — 100% completion on 4 different weekdays in one week.
120. **The Collector** 🔒 — Unlock 100 achievements.

---

## 15. Achievement → Data Coverage Map

| Weak spot in data | Achievements that attack it |
|---|---|
| Longest streak only 5 | #41–58, #111, #115 (entire streak ladder, escalating just past 5) |
| Wed/Fri/Sat cliffs | #59–66 (Boss Days) + weak-day XP bonus |
| All-or-nothing collapse | #57, #97, "Bare Minimum" mechanics |
| No celebration of PRs | #105–110 (the 120-rep days finally get rewarded) |
| Strength < cardio imbalance | #90–91 "Balanced Soul", stat gates #81–89 |

## 16. Already-Unlocked-on-Import (motivational welcome)

From the real data, the user opens the app with **~18 achievements already earned** (marked ✓ above — e.g. Thousand Fists, Century Walker, Never Skip Cardio, Over Capacity, The Five). This instant trophy shelf is the first proof that ASCENDANT respects the work already done — something a blank spreadsheet row never offered.
