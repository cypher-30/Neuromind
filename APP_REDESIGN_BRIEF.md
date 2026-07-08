# Neuromind — App Icon & Launch Animation Redesign Brief

Scope note: this brief covers the **app icon** and **launch/splash animation** only.
It builds on top of the "Teal & Sunset" visual system already implemented across
the rest of the app (currently on `main` as uncommitted work — see `HANDOFF.md`)
rather than replacing it. The goal here is to bring the icon and splash up to the
same production polish as the rest of the redesign, not to introduce a new palette.

---

## App Identity

- **App name:** Neuromind
- **Platform:** Android (Kotlin, Jetpack Compose, Material 3; min SDK 26, target SDK 34)
- **Primary purpose:** An intelligent planning partner for students — not a demanding
  task manager. Every feature targets cognitive load reduction: the app adapts to the
  user's energy, schedule, and habits rather than enforcing rigid adherence to a plan.
- **Target audience:** University students managing coursework, timetables, and
  fluctuating energy/mood alongside their workload.

---

## Current State

### Icon (`ic_launcher_foreground.xml` / `ic_launcher_background.xml` / `ic_neuromind_logo.xml`)

- **Shape:** Standard 108×108dp adaptive icon viewport; mark is centered in a
  safe zone with generous padding.
- **Symbol:** A brain silhouette (two-lobe organic outline, low-opacity fill,
  white stroke) overlaid with a simplified neural network — 6 nodes connected by
  thin lines converging toward a center point, with one accent node (sunset-orange)
  at the visual center.
- **Colors:** White/translucent-white strokes and nodes (`#22FFFFFF`–`#FFFFFFFF`
  range) on a diagonal linear gradient background running teal → deep seagreen-teal
  (`#0F7A85 → #0C6C71 → #0B5E52`). Single accent color: sunset-orange `#F2884B`
  on the center node.
- **Variants:** One shared vector mark reused for both the launcher foreground and
  the in-app splash logo (`ic_neuromind_logo.xml`, viewport-matched so it scales
  identically). No dedicated dark-mode or Android 13+ monochrome/themed-icon layer
  exists yet.

### Launch animation (`ui/splash/SplashScreen.kt`)

- **Implementation:** Native Jetpack Compose (`Animatable` + `spring` + `tween`),
  no Lottie or external animation library.
- **Background:** Same teal → seagreen linear gradient as the icon
  (`GradientStart #0F7A85 → GradientEnd #0B5E52`).
- **Sequence (~1.9s total):**
  1. Logo scales from `0.55×` → `1.0×` using a bouncy spring
     (`DampingRatioMediumBouncy`, `StiffnessMediumLow`) while fading in over 550ms
     (`FastOutSlowInEasing`).
  2. After a 450ms delay, the wordmark "Neuromind" and tagline "Your intelligent
     study partner" fade in together over 500ms.
  3. The screen holds for 900ms, then calls `onFinished()` to hand off to the app.
- **Known gap:** the spring's bounce is tuned for general-purpose motion, not for
  the calm/low-cognitive-load tone the rest of the app is establishing — it
  slightly overshoots before settling, which reads as more energetic than the
  brand's intended "planning partner" feel.

---

## Redesign Goals

### Direction

**Mood: Calm & Focused.** Given the app's core positioning — reducing cognitive
load, adapting to the user rather than demanding from them — the icon and launch
moment should read as settled and reassuring, not attention-grabbing. Concretely:
keep the existing teal/sunset/seagreen palette (it's already validated across the
rest of the app), but simplify the icon mark for small-size legibility and remove
the bounce overshoot from the launch animation in favor of a single gentle settle.

### Icon redesign specs

- **Shape:** Keep the standard adaptive-icon safe zone (66dp mark inside a 108dp
  viewport). No shape change needed — the gradient background and safe-zone sizing
  already work correctly.
- **Icon mark concept:** Retain the brain + neural-node mark — it's a strong,
  legible metaphor for planning/cognition and is already established as the brand
  symbol (splash, in-app references). Simplify it for launcher-size legibility:
  - Reduce from 6 nodes + 11 connection lines down to 4–5 nodes and their direct
    connections only — the current density can blur into a smudge at 48dp
    launcher size on a home screen.
  - Increase stroke weight and fill opacity slightly (current `#22FFFFFF`–`#33FFFFFF`
    fills are too faint to read at small sizes against some wallpapers).
  - Keep exactly one accent node in `BrandSunsetLight` (`#F2884B`) as the visual
    "spark" — this ties the icon to the in-app secondary/coaching color role
    (`ui/theme/PriorityColors.kt` semantic: secondary = "Neuromind is coaching you").
- **Color tokens:**
  | Role | Hex | Source |
  |---|---|---|
  | Background gradient start | `#0F7A85` | `BrandTealLight` / `GradientStart` |
  | Background gradient end | `#0B5E52` | `GradientEnd` |
  | Mark stroke/fill (white family) | `#FFFFFF` @ reduced opacity | — |
  | Accent node | `#F2884B` | `BrandSunsetLight` |
- **Dark/light variants:** The gradient background is dark enough that a single
  icon works on both system themes as-is. Add one new asset for Android 13+
  themed (monochrome) icons — a single-color silhouette of the simplified mark,
  since Android tints monochrome icons itself and ignores source color.
- **Format:** Deliver as vector (`ImageVector`/`AdaptiveIconDrawable` layers, as
  today) plus a 1024×1024 PNG master for store listing use.

### Launch/splash animation specs

- **Duration:** 1.8s total (tightened from the current ~1.9s; still within the
  1.5–2.5s recommended range).
- **Animation type:** Single-phase gentle **reveal + settle** — no bounce
  overshoot. Logo fades and scales up together, wordmark and tagline follow, then
  a short hold.
- **Easing curves:**
  - Logo scale: `spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)`
    (replaces the current `DampingRatioMediumBouncy` — removes overshoot while
    keeping organic, non-linear motion).
  - Logo/text alpha: `FastOutSlowInEasing`, unchanged — already reads as calm.
- **Key frames (plain language):**
  1. **0ms:** Blank gradient background, logo at 55% scale, fully transparent.
  2. **0–600ms:** Logo scales 55% → 100% and fades 0% → 100% opacity, settling
     without overshoot (no bounce past 100%).
  3. **500–950ms:** Wordmark "Neuromind" and tagline fade in together (overlaps
     the tail of the logo animation slightly, rather than waiting for a hard gap,
     so the sequence feels continuous rather than staged).
  4. **950–1800ms:** Hold on the fully-revealed splash.
  5. **1800ms:** Hand off to the app (`onFinished()`).
- **Lottie vs. native:** **Native Compose**, matching the current implementation
  and the rest of the codebase's animation approach (`Animatable`/`spring`/`tween`
  is already used throughout — Focus Mode's countdown timer, `AnimatedContent`
  transitions, etc.). No new dependency needed; see `launch_animation_spec.md`
  for the full frame breakdown and Compose implementation mapping.

---

## Design Tokens

Pulled from `ui/theme/Color.kt`, `Type.kt`, and `Shape.kt` — these are the
existing, live tokens the icon/animation redesign should stay consistent with.

### Color

| Token | Light | Dark |
|---|---|---|
| Primary (teal) | `#0F7A85` | `#4FC3BE` |
| Secondary (sunset — "coaching" surfaces only) | `#F2884B` | `#FFA873` |
| Tertiary (seagreen — positive/on-track state) | `#2E9E8F` | `#6FD6C4` |
| Background | `#F6F3EC` (warm sand) | `#12181A` (teal-tinted, not pure black) |
| Surface | `#FFFFFF` | `#1B2426` |
| Text primary | `#10262A` | `#E7F1EF` |
| Error (overdue/warning only) | `#D65A3A` | `#FF8A6B` |

### Typography

Material 3 default type scale (`androidx.compose.material3.Typography`), system
default font family (`FontFamily.Default`) — no custom display/body font is
currently loaded. Display/headline weights run `Bold`–`Black`; body/label weights
run `Normal`–`Medium`. If a custom typeface is introduced for the redesign, it
should replace `FontFamily.Default` uniformly across `Type.kt` rather than
per-screen.

### Shape (border radius scale)

| Token | Radius |
|---|---|
| extraSmall | 8dp |
| small | 12dp |
| medium | 18dp |
| large | 28dp |
| extraLarge | 36dp |

### Spacing

No formal spacing-scale constant exists in the codebase today (`Shape.kt` covers
radius only) — screens use ad hoc `dp` values in `Modifier.padding()`. Out of
scope for this brief, but worth flagging as a future token to formalize.

---

## Deliverables Checklist

- [ ] App icon (1024×1024 master, adaptive icon layers: background + foreground)
- [ ] Android 13+ monochrome/themed icon layer
- [ ] Splash/launch animation implemented natively in `SplashScreen.kt`
      (see `launch_animation_spec.md` for frame-by-frame spec)
- [ ] Figma component library (icon mark + splash keyframes, for design handoff)
- [ ] Dark mode variant check (gradient background already dark-compatible —
      verify contrast against both system themes)
- [ ] Export specs for Android (vector XML + adaptive icon layers); iOS N/A —
      Neuromind is Android-only

---

## References & Inspiration

No external references supplied yet. Internal reference points to design against:

- The existing "Teal & Sunset" semantic system (`ui/theme/Color.kt`) — the
  redesign should read as a natural extension of it, not a departure.
- `NeuromindTopBar` and the Dashboard gradient hero header — the only other
  places the brand gradient currently appears; the splash should feel like the
  same visual family.
- The app's own positioning language ("intelligent planning partner," "adapts to
  energy," "not a demanding task manager") as the tone-setting brief for motion
  design — calm, adaptive, low-pressure.
