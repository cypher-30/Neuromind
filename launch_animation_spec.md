# Neuromind — Launch Animation Spec

Companion to `APP_REDESIGN_BRIEF.md`. Target runtime: **native Jetpack Compose**
(`Animatable` / `spring` / `tween`), matching the existing `SplashScreen.kt`
implementation — no Lottie dependency. The frame table and keyframe blocks below
are written in a Lottie/CSS-keyframe style purely as a **spec notation** (useful
if this is ever mocked up in After Effects first), with a direct Compose mapping
provided at the end of each section.

- **Total duration:** 1800ms
- **Frame rate:** 60fps → **108 frames** total (frame 0 = 0ms, frame 107 = ~1783ms)
- **Canvas:** full-bleed gradient background, content centered

---

## Layers

| Layer | Content | Frames active |
|---|---|---|
| `bg` | Linear gradient, `#0F7A85 → #0B5E52`, diagonal | 0–107 (static, no animation) |
| `logo` | Icon mark (see SVG placeholder below) | 0–107 |
| `wordmark` | Text "Neuromind" | 27–107 |
| `tagline` | Text "Your intelligent study partner" | 27–107 |

---

## Frame-by-frame breakdown (60fps, 108 frames / 1800ms)

Grouped in 6-frame (100ms) increments — the practical granularity for hand
animation; interpolate linearly/spring between listed frames.

| Frame | ms | `logo` scale | `logo` opacity | `wordmark`/`tagline` opacity | Notes |
|---|---|---|---|---|---|
| 0 | 0 | 0.55 | 0.00 | 0.00 | Splash enters; blank gradient, logo pre-scaled down |
| 6 | 100 | 0.63 | 0.19 | 0.00 | Spring begins accelerating out |
| 12 | 200 | 0.74 | 0.39 | 0.00 | |
| 18 | 300 | 0.85 | 0.61 | 0.00 | |
| 24 | 400 | 0.94 | 0.83 | 0.00 | |
| 30 | 500 | 0.99 | 0.98 | 0.00 | Logo scale/fade nearly settled (spring, no overshoot) |
| 36 | 600 | 1.00 | 1.00 | 0.17 | Logo fully settled; text fade begins (overlaps tail of logo motion) |
| 42 | 700 | 1.00 | 1.00 | 0.39 | |
| 48 | 800 | 1.00 | 1.00 | 0.61 | |
| 54 | 900 | 1.00 | 1.00 | 0.83 | |
| 57 | 950 | 1.00 | 1.00 | 1.00 | Text fully visible |
| 57–107 | 950–1783 | 1.00 | 1.00 | 1.00 | Hold |
| 107 | 1783 | 1.00 | 1.00 | 1.00 | `onFinished()` fires, splash hands off to app |

### CSS-keyframe notation (spec form)

```css
@keyframes logo-reveal {
  0%   { transform: scale(0.55); opacity: 0; }
  33%  { transform: scale(0.99); opacity: 0.98; }  /* ~600ms of 1800ms */
  100% { transform: scale(1.00); opacity: 1; }
}

@keyframes text-fade {
  0%   { opacity: 0; }         /* starts at 27.7% = ~500ms */
  100% { opacity: 1; }         /* ends at ~53% = ~950ms */
}

.logo     { animation: logo-reveal 600ms cubic-bezier(0.0, 0.0, 0.2, 1) forwards; }
.wordmark,
.tagline  { animation: text-fade 450ms cubic-bezier(0.4, 0.0, 0.2, 1) 500ms forwards; }
```

### Compose implementation mapping (actual target)

```kotlin
val scale     = remember { Animatable(0.55f) }
val alpha     = remember { Animatable(0f) }
val textAlpha = remember { Animatable(0f) }

LaunchedEffect(Unit) {
    launch {
        scale.animateTo(
            targetValue = 1f,
            // No-bounce spring: settles smoothly instead of overshooting past 1.0x,
            // matching the "calm, low-pressure" brand tone.
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )
    }
    launch {
        alpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }
    delay(500)
    textAlpha.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
    delay(850)   // hold from ~950ms to 1800ms
    onFinished()
}
```

---

## Icon mark — SVG placeholder

Simplified mark per `APP_REDESIGN_BRIEF.md` (5 nodes instead of 6, direct
connections only, heavier stroke/fill for small-size legibility, single
sunset-orange accent node). Placeholder for design handoff — not final art.

```svg
<svg width="108" height="108" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
  <!-- Brain silhouette -->
  <path
    d="M 54,30 C 51,27 44,26 37,29 C 28,32 26,43 29,52 C 32,61 38,71 46,75 C 50,77 52,77 54,77 C 56,77 58,77 62,75 C 70,71 76,61 79,52 C 82,43 80,32 71,29 C 64,26 57,27 54,30 Z"
    fill="#FFFFFF" fill-opacity="0.30"
    stroke="#FFFFFF" stroke-opacity="0.95" stroke-width="3"
    stroke-linecap="round" stroke-linejoin="round" />

  <!-- Center fissure -->
  <path d="M 54,30 L 54,77"
    fill="none" stroke="#FFFFFF" stroke-opacity="0.55" stroke-width="2"
    stroke-linecap="round" />

  <!-- Simplified neural connections: 4 outer nodes -> center only -->
  <path d="M 36,42 L 54,50 M 36,64 L 54,50 M 72,42 L 54,50 M 72,64 L 54,50"
    fill="none" stroke="#FFFFFF" stroke-opacity="0.75" stroke-width="1.8"
    stroke-linecap="round" />

  <!-- Outer nodes (white) -->
  <circle cx="36" cy="42" r="4" fill="#FFFFFF" />
  <circle cx="36" cy="64" r="4" fill="#FFFFFF" />
  <circle cx="72" cy="42" r="4" fill="#FFFFFF" />
  <circle cx="72" cy="64" r="4" fill="#FFFFFF" />

  <!-- Accent center node (BrandSunsetLight) -->
  <circle cx="54" cy="50" r="5" fill="#F2884B" />
</svg>
```

Background (place behind the mark above, full 108×108 viewport):

```svg
<svg width="108" height="108" viewBox="0 0 108 108" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="108" y2="108" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#0F7A85" />
      <stop offset="50%" stop-color="#0C6C71" />
      <stop offset="100%" stop-color="#0B5E52" />
    </linearGradient>
  </defs>
  <rect width="108" height="108" fill="url(#bg)" />
</svg>
```
