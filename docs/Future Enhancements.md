# Future Enhancements

Post-v1 ideas, ranked by value-to-effort for a single personal user. None are required to replace the spreadsheet; all preserve the offline-first, single-user, no-backend principles unless explicitly noted.

---

## 1. Near-Term (high value, low effort)

| Idea | Why | Notes |
|---|---|---|
| **Home-screen widget** | One-glance streak + completion ring; one-tap quick-log | Compose Glance; biggest daily-engagement multiplier |
| **In-app YouTube player** | More integrated than deep-link | `android-youtube-player` lib (Video Integration §2-B) |
| **"Add video from URL"** | Keep creator list fresh forever, zero maintenance | Already specced in Video Integration §3 |
| **Reduce-motion & theme polish** | Accessibility + unlock more palettes | Theme dictionary already supports it |
| **Smart reminder tuning** | Auto-learn best nudge times from log timestamps | Targets Wed/Fri/Sat cliffs harder |
| **Per-exercise streaks surfaced** | Strong micro-motivators (e.g. calf-raise streak) | Data already tracked |

## 2. Mid-Term (richer RPG depth)

| Idea | Why |
|---|---|
| **Story / campaign mode** | Chapters that unlock as you level — narrative arc ("defeat the Wednesday Boss") turns the data weak-spots into literal bosses |
| **Skill tree / perks** | Spend level-up points on passive buffs (e.g. "+10% weekend XP") — more agency |
| **Seasonal events** | Monthly themed challenges with limited-time badges (variable reward) |
| **Boss battles** | Multi-day challenge "raids" (e.g. 1,000 reps in a week) with an HP bar |
| **Equipment / cosmetics** | Earn gear that restyles the portrait (intrinsic reward economy) |
| **Deeper analytics** | Volume-vs-consistency correlation, predicted streak-break risk, "what-if" projections |
| **Custom exercise library expansion** | Templates (planks/timed, running/distance, weighted) with proper unit handling |

## 3. Integrations (optional, may require permissions/network)

| Idea | Trade-off |
|---|---|
| **Health Connect / Google Fit sync** | Auto-import steps/walking distance → less manual logging; adds a permission + dependency |
| **Wearable companion (Wear OS)** | Log reps from the watch; bigger build scope |
| **Step counter (on-device sensor)** | Auto-fill walking miles from the pedometer; battery considerations |
| **Voice / Google Assistant logging** | "Log 50 push-ups" hands-free |
| **Calendar export** | Workouts as calendar events for review |

## 4. Personalization & Intelligence

| Idea | Why |
|---|---|
| **Adaptive targets** | Auto-suggest target tweaks from trailing performance (e.g. bump squats to 110 after consistent overachievement) |
| **Coaching insights** | Plain-language weekly tips grounded in *your* data ("You skip strength after rest days — try a 20-rep bridge") |
| **On-device LLM summaries** | Monthly review written as a flavorful "season recap" narration |
| **Mood/energy correlation** | Surface what conditions precede your best vs worst days |

## 5. Visual / Audio Polish

| Idea | Why |
|---|---|
| **Generated character portraits** | Use AI image tools (Style Guide §8 prompts) to render personalized evolving hero art |
| **Original sound design** | Level-up chimes, log "pings," ambient menu music (OFL/CC0/owned) |
| **Animated Lottie level-up cinematics** | Higher-production "juice" moments |
| **Dynamic portrait evolution** | Portrait visibly powers up across milestone levels |

## 6. Data & Backup

| Idea | Why |
|---|---|
| **Automatic Drive backup scheduling** | Hands-off durability (currently manual/SAF) |
| **Multi-device personal sync** | If you get a second phone/tablet — would need a lightweight sync (file-based via Drive keeps it backend-free) |
| **Richer export formats** | PDF "season recap" sharable with yourself; image cards of achievements |

## 7. Explicitly Deferred (kept out of v1 on purpose)

- Multi-user / social / leaderboards — contradicts the single-user vision.
- Monetization / ads / IAP — never.
- iOS / web ports — Android-only by design (would reopen the Flutter/KMP decision).
- Hosted backend / cloud database — offline-first, file-based backup is sufficient and free.
- Downloading YouTube videos — prohibited by ToS (Video Integration §5).

---

## 8. Suggested Roadmap Ordering

1. **v1.0** — Core loop, six exercises, XP/levels/stats, streaks, quests, 86 achievements, analytics, import/export, deep-link videos. *(replaces the sheet — success criteria met)*
2. **v1.1** — Home-screen widget, in-app video player, add-video-from-URL, reduce-motion/theme polish.
3. **v1.2** — Story/boss mode targeting the Wed/Fri/Sat cliffs, seasonal events, per-exercise streaks surfaced.
4. **v1.3** — Health Connect/step sync, adaptive targets, coaching insights.
5. **v2.0** — Generated evolving portraits, full audio design, Lottie cinematics, Wear OS companion.

Each step is independently shippable and preserves the personal, offline-first architecture.
