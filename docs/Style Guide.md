# Style Guide — ASCENDANT

Complete visual system for an **anime-inspired RPG aesthetic**. All artwork must be **original or AI-generated in an inspired-by style** — no copyrighted characters, logos, or screenshots from any referenced work (see §10 Legal).

---

## 1. Visual Direction

**One-line mood:** *A JoJo-vivid, Solo-Leveling-dark JRPG status screen you live inside.* High-contrast, ink-and-neon, dramatic diagonals, glowing auras, holographic "system window" panels, and decisive type. Reads as a hero's progression UI, never as a clinical fitness app.

### Influence → Extracted Theme (themes only, not assets)

| Influence | What we borrow (abstract) |
|---|---|
| **JoJo's Bizarre Adventure** | Bold diagonal compositions, dramatic "menacing" energy, saturated unconventional color pairings, expressive pose framing, onomatopoeia-as-graphic-accent |
| **Solo Leveling** | Dark UI, glowing blue "system" notification windows, rank letters (E→S), "you have leveled up" pop-ups, purple/cyan magic glow |
| **Persona 5 menus** | Confident asymmetry, jagged red/black, kinetic motion, stylized type as UI |
| **Dragon Quest / retro JRPG** | Clean command-window framing, blue gradient menu boxes, crisp stat lists |
| **Habitica** | Habit-as-quest framing, pixel-friendly badges, streak/daily structure |
| **Idle RPG progression** | Number-go-up juice, particle bursts, tiered rarity glows |

---

## 2. Color Palette

### 2.1 Core (default "Crimson Resolve" theme)

| Token | Hex | Use |
|---|---|---|
| `bg/base` | `#0E0B14` | App background (near-black indigo) |
| `bg/panel` | `#1A1426` | Cards, system windows |
| `bg/panel-alt` | `#241A33` | Raised/selected |
| `ink/primary` | `#F4ECFF` | Primary text |
| `ink/muted` | `#9A8FB0` | Secondary text |
| `accent/crimson` | `#FF2D55` | Primary accent, STR, CTAs |
| `accent/gold` | `#FFC53D` | XP, level-up, rewards |
| `accent/cyan` | `#27E1FF` | "System window" glow, links, info |
| `accent/violet` | `#8B5CFF` | Magic/aura, secondary brand |
| `state/success` | `#3DDC84` | Completed, ✓ |
| `state/warn` | `#FF9F1C` | Weak-day (Sat) cues |
| `state/danger` | `#FF3B30` | Streak-at-risk, Wed/Fri cliffs |

### 2.2 Stat Colors (consistent everywhere)

| Stat | Color |
|---|---|
| STRENGTH | `#FF2D55` crimson |
| ENDURANCE | `#3DDC84` green |
| AGILITY | `#27E1FF` cyan |
| DISCIPLINE | `#8B5CFF` violet |
| CONSISTENCY | `#FFC53D` gold |

### 2.3 Rarity Glows (badges, borders)

| Rarity | Color | Effect |
|---|---|---|
| Common | `#9AA0A6` gray | flat |
| Uncommon | `#3DDC84` green | soft glow |
| Rare | `#27E1FF` cyan | glow + sheen |
| Epic | `#8B5CFF` violet | glow + particles |
| Legendary | `#FFC53D` gold | animated shimmer |
| Mythic | holo gradient `#FF2D55→#8B5CFF→#27E1FF` | holographic + burst |

### 2.4 Unlockable Themes (level-gated)

`Crimson Resolve` (default) · `Hamon Gold` (Lv10) · `Stand Cyan` (Lv15) · `Shadow Monarch` (deep purple/black, Lv30) · `Overdrive` (neon high-contrast, Lv40) · `Holographic` (Lv100). Each is just a swap of the token map above — implement as a theme dictionary.

---

## 3. Typography

| Role | Typeface (suggested, OFL-licensed) | Style |
|---|---|---|
| Display / titles | **Orbitron** or **Teko** | wide, geometric, "system UI" feel |
| Numbers / stats / XP | **Rajdhani** or **Chakra Petch** | tabular, techy, legible at a glance |
| Body / notes | **Inter** or **Noto Sans** | clean, high legibility |
| Accent / onomatopoeia | **Bangers** or a bold display face | sparingly, for "LEVEL UP!" "PERFECT!" |

Rules: titles ALL-CAPS with slight letter-spacing; numbers tabular-lining (no jitter when counting up); never more than 2 families on screen at once (display + body); accent face only for celebration moments.

> Use only **open-source / OFL** fonts to keep the personal build license-clean. All listed are freely licensable.

---

## 4. Iconography

- **Style:** crisp line-icons with a 2px stroke + optional inner glow; filled variants for active state.
- **Set:** one icon per exercise (push-up arms, squat figure, leg-lift, calf, dumbbell curl, walking boot), plus stat sigils (sword=STR, lung/wind=END, wing/bolt=AGI, eye/shield=DIS, flame=CON).
- **Motif accents:** small "✦/★/◆" sparkles, diagonal speed-lines, and bracket frames `⟪ ⟫` for titles (JoJo/JRPG flavor).
- Source: generate as SVG (e.g. via the gemini `generate_svg` tool) or use an OFL icon set (Lucide/Tabler) restyled with the glow treatment.

---

## 5. Components

### Cards / "System Windows"
Dark panel `bg/panel`, 1px cyan or violet border with outer glow, slightly clipped corner (one corner cut diagonally for that Persona/Solo-Leveling "UI window" look), subtle inner gradient.

### Progress Bars
- Track `#2A2140`, fill = stat/accent color with a moving gloss highlight.
- **Completion ring:** circular, gradient stroke crimson→gold, glowing tip, caps visually at 100% with a separate "OVERDRIVE" ember ring for surplus.
- XP bar: gold fill, animated count-up of the number beside it.

### Buttons
- **Primary:** crimson fill, white text, slight skew/diagonal accent, press = scale-down + glow pulse + haptic.
- **Secondary:** ghost (transparent + glowing border).
- **Quick-add chips:** pill, panel-alt bg, tap = brief gold flash.

### Streak Flame
Animated flame icon; size & color escalate with streak length (ember → orange → blue-white at 30+); when at-risk, pulses red.

### Badges
Hexagonal or shield medallion, rarity-glow border (§2.3), embossed icon, holo sheen for Mythic.

---

## 6. Animation Concepts

| Moment | Animation |
|---|---|
| Log reps | number "+120 XP" floats up & fades; bar fills with easing; haptic tick + soft "ping" |
| Bar fills to 100% | gold flash + ✓ stamp + chime |
| Overdrive (exceed target) | ring emits ember particles, brief screen-edge glow |
| Level up | screen-dim → aura burst from portrait → number slams 11→12 with shake → stat roll-up |
| Achievement | badge slides in with rarity glow; Mythic = full-screen holo shatter |
| Streak extend | flame grows + spark trail |
| Streak at risk | flame pulses red; notification |
| Quest cleared | "CLEARED" diagonal stamp (Persona-style) |
| Screen transitions | quick diagonal wipe / slide (kinetic, ~200ms) |

**Principles:** fast (150–400ms), juicy but skippable, never block input for long; **respect a "Reduce Motion" setting** (swap takeovers for simple fades). Implement with Jetpack Compose animation APIs (see Technical Architecture).

---

## 7. Art Direction

### Character Portrait
- **Style:** semi-realistic anime / cel-shaded, dramatic rim lighting, dynamic ¾ hero pose, aura glow whose color = current theme/rank.
- **Progression:** portrait "powers up" at milestone levels — added aura, more dramatic lighting, an accessory or background change (not a different person; same hero evolving). Frames upgrade by level (§Leveling milestones).
- Single hero (the user's avatar). Optionally a few selectable base archetypes.

### Backgrounds
Dark gradient voids with faint geometric/magic-circle motifs, diagonal speed-lines, subtle particle drift. Character sheet gets a more elaborate "throne/aura" backdrop that upgrades with rank.

### Achievement Badges
Medallion/shield silhouettes, iconographic center (fist, flame, boot…), rarity glow border, holo for Mythic. Consistent 1:1 framing for the grid.

### Level-Up Screen
Full-bleed dark, radial aura burst behind the big level number, gold particles, kinetic type. Milestone variant adds rank-letter reveal.

### Stat Panels
JRPG command-window aesthetic: clean rows, stat sigil + bar + number, optional radar pentagon, cyan system-window framing.

---

## 8. Example AI Image-Generation Prompts

> For future asset creation (e.g. via the `generate_image` tool). All prompts are **original, inspired-by** — they name styles/moods, never copyrighted characters or titles.

**Hero portrait (default):**
> "Original anime RPG hero, semi-realistic cel-shaded, athletic young protagonist in a dramatic three-quarter power stance, crimson and violet aura energy swirling, rim lighting, dark indigo void background with faint magic-circle motifs, high contrast, dynamic diagonal composition, video-game character-select splash art, no text."

**Rank-up evolved portrait (Lv 30+):**
> "Same original anime hero, now radiating intense gold-and-crimson aura, more elaborate energy effects, glowing eyes, particles rising, heroic low-angle shot, cinematic lighting, dark throne-room void, JRPG ultimate-form splash art, no logos, no text."

**Achievement badge (Legendary):**
> "Ornate hexagonal medallion badge, glowing gold metallic rim with shimmer, embossed clenched-fist sigil at center, dark enamel face, subtle particle sparkles, game UI trophy icon, centered 1:1, transparent background."

**System-window UI panel:**
> "Dark holographic RPG menu panel, glowing cyan thin border with one diagonally clipped corner, faint inner gradient, subtle scanline texture, Solo-Leveling-inspired system notification window, game UI asset, transparent background, no text."

**Level-up background:**
> "Radial energy burst, gold and crimson particles exploding outward from center, dark indigo background, anime power-up effect, dramatic light rays, motion energy, game level-up screen background, no characters, no text."

**App icon:**
> "Bold minimalist app icon, stylized ascending arrow merged with a flame and a star, crimson-to-violet gradient on dark indigo, glowing edges, modern anime-RPG game logo mark, flat-but-luminous, 1024x1024, no text."

---

## 9. Accessibility

- Maintain WCAG AA contrast for all text (ink/primary on bg/panel passes; verify accent-on-dark for body sizes — use accents for large/bold only).
- "Reduce Motion" disables takeovers/particles → fades.
- Color is never the *only* signal (✓ icons + labels accompany color states; weak-days carry an ⚠ glyph, not just red).
- Min tap target 48dp; tabular numerals for legibility.

---

## 10. Legal / Originality Guardrails

- **No** copyrighted characters, names, logos, music, or screenshots from JoJo, Solo Leveling, Persona, Dragon Quest, Habitica, etc.
- Flavor text evokes tropes (Stand, Hamon, Hunter ranks, "World Over Heaven") — keep these as light homage/parody within personal-use fair-use comfort; since the app is **private, single-user, non-distributed, non-commercial**, exposure is minimal, but originality is still the rule for all generated art.
- Use only OFL/CC0/owned fonts, icons, sounds, and images.
