# Product Vision — ASCENDANT

> *"A workout tracker tells you what you did. ASCENDANT makes you the protagonist of a story where doing it matters."*

**Working title:** **ASCENDANT** *(codename; rename freely — alternatives: STAND PROUD, STAT QUEST, ARC, GAINS//BIZARRE)*
**Type:** Single-user, Android-only, offline-first personal fitness RPG
**Owner / sole user:** mhurston@gmail.com
**Status:** Design phase — no code until documentation is approved

---

## 1. The Problem

The existing Google Sheet is an honest ledger and a poor coach. The 30-day data proves it:

- **Walking is automatic** (30/30 days) — but **strength training is fragile**: only 21/30 days active, **9 total-collapse days**, and a **longest streak of just 5 days**.
- Completion craters on **Wednesday (36%), Friday (35%), and Saturday (39%)**.
- The sheet computes a single number and then **forgets**. It never celebrates the 1,410th push-up, never warns that a 5-day streak is about to break, never makes tomorrow feel like it's worth showing up for.

**The problem is not tracking. It is motivation and consistency.** The tool records effort but generates none.

---

## 2. The Vision

ASCENDANT reframes daily exercise as **character progression in a JoJo-meets-Solo-Leveling RPG**. Every push-up is XP. Every consistent week raises your Strength stat. Every streak is a power-up you don't want to lose. The same six numbers from the spreadsheet flow in — but they come back out as **levels, stats, quests, achievements, and animated payoffs** tuned specifically to the consistency gaps the data reveals.

The user should feel, on a bad Wednesday, the same pull a gamer feels to keep a daily login streak alive — and on a good day, the dopamine of a level-up screen flaring across the phone.

---

## 3. Motivation Strategy

The design is grounded in established behavior-change mechanics, each mapped to a concrete feature.

| Psychological principle | How ASCENDANT uses it | Targets which data gap |
|---|---|---|
| **Loss aversion** | Visible streak counters + a streak that can *break* (with a grace "shield") | The missing streak feedback loop |
| **Immediate reward** | XP, level-up, and PR animations fire the instant you log | Sheet's zero celebration |
| **Goal gradient effect** | Live completion ring & per-exercise bars that fill toward 100% | All-or-nothing collapse |
| **Variable reward** | Rotating daily quests, random "bonus objective" days, loot-style badge drops | Boredom / routine fatigue |
| **Identity & narrative** | A leveling character ("you are becoming strong") not a chart | Lack of meaning in numbers |
| **Implementation intentions** | Day-aware reminders that hit Wed/Fri/Sat hardest | The mid-week & weekend cliffs |
| **Minimum viable action** | "Bare Minimum" mini-quest (e.g. 20 reps) that still protects the streak | The 9 total-skip days |
| **Mastery feedback** | Stats, PRs, and monthly review show long-term upward trend | No trend visibility today |

**Core strategic bet:** Don't try to motivate walking (already solved). Pour the reward economy into **strength consistency** and **streak preservation**, because that is precisely where the data shows the user fails.

---

## 4. User Psychology & Anti-Patterns

A personal motivation tool can backfire. The design explicitly guards against it:

- **Anti-burnout, not anti-rest.** A *planned* rest day is honored (no streak loss, no guilt), distinct from a *failed* day. Over-training prompts a "recover" suggestion rather than a punishment.
- **No shame spiral.** A broken streak triggers a comeback quest ("Rise again"), not a red zero. Loss aversion motivates; humiliation makes people quit.
- **Earned, not bought.** Because there is no monetization and no other users, all rewards are intrinsic and honest — there is no leaderboard to game, no pay-to-win, no social comparison. The only opponent is yesterday's self.
- **Forgiving partial credit.** Doing *something* always beats nothing; the completion ring and XP reward 30% days too, breaking the all-or-nothing trap.

---

## 5. What Success Looks Like

ASCENDANT succeeds when, six months in, the data shows what the sheet never could: the **longest strength streak climbs past 5 → 15 → 30+**, the **Wed/Fri/Sat completion cliffs flatten**, and the user opens the app *because they want to see their character rise*, not because they have to fill a row.

Concretely, tied to the project's success criteria:

1. The sheet's six metrics + completion formula are fully reproduced and importable. *(Criterion 1)*
2. Logging takes one tap and pays off with motion and sound the sheet never had. *(Criterion 2)*
3. Streaks, levels, and quests create a daily reason to return. *(Criterion 3)*
4. The aesthetic reads unmistakably as an anime RPG, not a fitness utility. *(Criterion 4)*
5. This documentation set lets implementation begin immediately. *(Criterion 5)*
6. Every mechanic here is justified against the actual 30-day data. *(Criterion 6)*
7. Architecture stays lean: one user, one phone, offline-first. *(Criterion 7)*

---

## 6. Non-Goals (Explicitly Out of Scope)

- ❌ Multi-user, accounts, social, or leaderboards
- ❌ Monetization, ads, IAP, subscriptions
- ❌ iOS / web / cross-platform (Android-only)
- ❌ Wearable/health-API integrations in v1 (noted in Future Enhancements)
- ❌ Hosting copyrighted anime artwork (original, inspired-by art only)

---

## 7. The Pitch in One Sentence

**ASCENDANT turns a forgotten spreadsheet into a daily anime RPG where the user is the hero, exercise is XP, and the longest streak they've ever held — 5 days — becomes the first boss they set out to beat.**
