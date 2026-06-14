# Video Integration Research (Phase 3)

How ASCENDANT links exercise-form videos: requirements, per-exercise creator recommendations, integration approaches, and the licensing/API/offline realities for a **single-user, offline-first** Android app.

---

## 1. Requirements Recap

- YouTube support.
- Multiple creators per exercise.
- Rotation / randomization between them.
- Bookmark favorites.
- Per-exercise: recommended creators, example video categories, integration approach.

---

## 2. Integration Approaches Compared

| Approach | How | Pros | Cons | Verdict |
|---|---|---|---|---|
| **A. Deep-link to YouTube app** | `Intent`/URL `youtube.com/watch?v=…` or a search query | Zero API key, zero quota, ToS-clean, always up-to-date, full creator monetization respected | Leaves the app; no in-app overlay | ✅ **v1 default** |
| **B. `YouTubePlayer` IFrame embed (WebView/AndroidX)** | Official IFrame Player API in a WebView, or the community `android-youtube-player` lib | In-app playback, looks integrated | Must honor branding/ToS; ads still show; webview weight | ✅ optional v1.1 (in-app feel) |
| **C. YouTube Data API v3** | Query channels/playlists for fresh video lists | Dynamic rotation, metadata, thumbnails | Needs API key + OAuth, 10k-unit daily quota, more code, key management on-device | ⚠ later/optional only |
| **D. Download / cache videos** | — | Offline playback | **Prohibited** by YouTube ToS; do not implement | ❌ never |

**Recommendation:** ship **Approach A** (deep-link) for v1 — it satisfies every requirement (multiple creators, rotation, favorites) using a **local curated list** of video/channel IDs, with **no API key, no quota, no ToS risk**. Optionally add **Approach B** for in-app playback once the core app is done. Reserve **Approach C** only if you later want auto-refreshing creator playlists.

---

## 3. Data Model for Videos (local, no backend)

A small bundled/editable JSON the app reads — no server needed:

```json
{
  "pushups": {
    "category": "STRENGTH",
    "videos": [
      { "id": "yt:VIDEO_ID_1", "title": "Perfect Push-up Form", "creator": "…", "tags": ["form","beginner"], "fav": false },
      { "id": "yt:VIDEO_ID_2", "title": "Push-up Variations", "creator": "…", "tags": ["variations"] }
    ],
    "playlists": [ { "id": "yt:PLAYLIST_ID", "creator": "…" } ]
  }
}
```

- **Rotation:** dashboard shows a different entry each visit (round-robin); **shuffle** button = random pick (`videos[random]`).
- **Favorites:** toggling `fav` writes to local DB; a "Favorites" filter surfaces them first.
- **Editable in-app:** a "Manage Videos" settings screen lets the user paste any YouTube URL → app extracts the ID → adds it under an exercise. This keeps curation in the user's hands and avoids stale hard-coded lists.

---

## 4. Per-Exercise Creator & Category Guidance

> Specific channels change/rebrand over time; rather than hard-code possibly-stale handles, the recommended approach is **search-query seeds + user-curated IDs**. Below are the **video categories** to populate per exercise and the **types of creators** to look for. The app ships with a starter set the user verifies/edits on first run.

| Exercise | Example video categories to link | Type of creator to source from |
|---|---|---|
| **Push-ups** | Form fundamentals · Variations (incline/diamond/wide) · Building from knee push-ups · 100-rep challenge follow-alongs | Calisthenics & bodyweight-strength channels; physiotherapy/form channels |
| **Squats** | Bodyweight squat form · Depth & knee tracking · Squat variations (sumo/jump) · Daily squat routines | Calisthenics, mobility, and strength-coach channels |
| **Leg Lifts** | Lying/hanging leg-raise form · Core engagement cues · Ab routines including leg lifts | Core/ab-focused fitness channels; physio channels |
| **Calf Raises** | Standing calf-raise form · Single-leg progression · Calf endurance sets | Strength & physio channels |
| **Curls** | Bicep curl form (dumbbell/band) · Avoiding swinging · Resistance-band curl alternatives | Strength-training & home-workout channels |
| **Walking** | Walking-for-fitness tips · Indoor walking follow-alongs · Brisk-walk/step routines · Walking podcasts/music for pace | Walking-workout channels; "indoor walk" follow-along creators |

**Curation seed strategy:** for each exercise, store 2–4 search-query seeds (e.g. `"perfect push up form"`, `"push up variations"`). The "Discover" button deep-links a YouTube search; the user taps the channel/video they like and "Add to ASCENDANT," which captures the stable video/channel ID. This stays fresh forever with zero maintenance and respects each creator's monetization (views happen on YouTube).

---

## 5. Licensing Considerations

- **Linking / deep-linking is allowed** — sending the user to YouTube respects creators and YouTube's ToS; views count for them.
- **Embedding (IFrame Player API) is allowed** if you don't strip ads/branding and follow YouTube's [API ToS](https://developers.google.com/youtube/terms/api-services-terms-of-service). Use the official player only.
- **Downloading/caching/ripping is prohibited.** No offline video storage of YouTube content. Ever.
- **No re-hosting** of third-party video.
- Because the app is **personal & non-distributed**, there's no redistribution issue — but the above still applies to YouTube's ToS regardless of audience size.
- Fonts/icons/sounds in the app itself must be OFL/CC0/owned (see Style Guide §10).

---

## 6. API Requirements

| If using… | Need |
|---|---|
| Deep-link (A) | **Nothing** — just URLs/Intents |
| IFrame embed (B) | Official player lib or WebView; no key for basic playback; follow branding ToS |
| Data API (C) | Google Cloud project, YouTube Data API v3 key, OAuth for private playlists, 10,000 units/day quota (a `search` call = 100 units → ~100/day, so cache aggressively) |

For a single user, **Approach A needs no Google Cloud setup at all** — a major simplicity win.

---

## 7. Offline Limitations

- **YouTube content cannot be made offline** within ToS. When offline, video buttons show a "needs connection" state.
- **What still works offline:** the app's own curated metadata (titles, creators, your favorites list, thumbnails if you cache *your own* generated placeholders — not YouTube thumbnails beyond ToS-permitted use).
- **Mitigation:** the core app is fully offline-first (logging, XP, stats, quests all work with no network); videos are an *optional enhancement layer* that gracefully degrades. The fitness loop never depends on connectivity.
- Optional future: let the user link to **locally stored personal video files** (their own recordings) for true offline form reference — fully ToS-safe since it's their own media.

---

## 8. Recommendation Summary

1. **v1:** Approach A (deep-link) + local editable JSON of curated video/channel IDs + "Add from URL" + favorites + shuffle/rotation. No API key, no quota, fully ToS-clean, near-zero maintenance.
2. **v1.1 (optional):** Approach B in-app IFrame player for a more integrated feel.
3. **Later (optional):** Approach C Data API only if auto-refreshing creator playlists becomes desirable.
4. **Never:** download/cache YouTube media.
