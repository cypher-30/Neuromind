package com.alvin.neuromind.ui.theme

import androidx.compose.ui.graphics.Color

// ── Semantic color vocabulary — "Organic" design system ─────────────────────
// Warm cream-and-sand ground, terracotta primary accent, sage second accent.
//   primary (terracotta) → structural / interactive brand chrome
//   secondary (sage)     → reserved for "Neuromind noticed" / coaching surfaces
//   error (tuned red)    → overdue / warning only — NOT part of the source
//                           Organic system (which has no error role); added
//                           here so HIGH priority and overdue stay distinct
//                           from MEDIUM priority (both otherwise terracotta).
// Priority mapping (single source of truth — see ui/theme/PriorityColors.kt):
//   HIGH → error, MEDIUM → primary(accent), LOW → secondary(accent-2)
//
// Light-mode surfaces are intentionally INVERTED from stock Material: surface
// (#EBDDC5) is darker than background (#F5EAD8) — cards read as sand set into
// a cream page. Do not "correct" this to a lighter surface.

// ── Ground ────────────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF5EAD8) // cream
val LightSurface     = Color(0xFFEBDDC5) // sand — darker than background, by design
val TextPrimary      = Color(0xFF201E1D)

val DarkBackground   = Color(0xFF2E2B25) // neutral-900
val DarkSurface      = Color(0xFF474238) // neutral-800
val TextPrimaryDark  = Color(0xFFF9F4ED) // neutral-100

// ── Neutral ramp (OKLCH-derived, shared lightness scale) ─────────────────────
val Neutral100 = Color(0xFFF9F4ED)
val Neutral200 = Color(0xFFEEE7DB)
val Neutral300 = Color(0xFFDCD3C4)
val Neutral400 = Color(0xFFC0B6A5)
val Neutral500 = Color(0xFFA19786)
val Neutral600 = Color(0xFF82796A)
val Neutral700 = Color(0xFF645C50)
val Neutral800 = Color(0xFF474238)
val Neutral900 = Color(0xFF2E2B25)

// ── Accent ramp — terracotta (primary) ───────────────────────────────────────
val Accent100 = Color(0xFFFFF2EB)
val Accent200 = Color(0xFFFFE1D0)
val Accent300 = Color(0xFFFFC6A5)
val Accent400 = Color(0xFFF6A06B)
val Accent500 = Color(0xFFD67F48) // base — close to the raw #C67139 accent
val Accent600 = Color(0xFFB2622D)
val Accent700 = Color(0xFF8C491A)
val Accent800 = Color(0xFF643312)
val Accent900 = Color(0xFF402310)
val AccentBase = Color(0xFFC67139) // --color-accent, used as the literal brand value

// ── Accent-2 ramp — sage (secondary) ─────────────────────────────────────────
val Accent2_100 = Color(0xFFF0FAE1)
val Accent2_200 = Color(0xFFE1EECC)
val Accent2_300 = Color(0xFFCCDBB2)
val Accent2_400 = Color(0xFFAEBF92)
val Accent2_500 = Color(0xFF8FA073)
val Accent2_600 = Color(0xFF728157)
val Accent2_700 = Color(0xFF56633F)
val Accent2_800 = Color(0xFF3D472B)
val Accent2_900 = Color(0xFF272E1B)
val Accent2Base = Color(0xFF7A8A5E) // --color-accent-2, literal brand value

// Container-role tokens (single source of truth for Theme.kt mappings)
val AccentContainerLight = Accent100
val AccentContainerDark = Color(0x29F6A06B) // accent-400 @16%
val Accent2ContainerLight = Accent2_100
val Accent2ContainerDark = Color(0x29AEBF92) // accent-2-400 @16%

// ── Danger ramp — the app's one deliberate departure from Organic ───────────
// Organic has no error role; it derives "danger" from accent-800, which makes
// HIGH and MEDIUM priority read as two values of one hue. This tuned brick-red
// (hue ~10°, versus terracotta's ~25°) keeps overdue/HIGH legible while
// staying warm against the cream ground.
val DangerLight          = Color(0xFFA8321F)
val DangerDark           = Color(0xFFFF8F73)
val DangerContainerLight = Color(0xFFFBDDD3)
val DangerContainerDark  = Color(0xFF5C2114)
val OnDangerContainerLight = Color(0xFF3A0E06)
val OnDangerContainerDark  = Color(0xFFFBDDD3)

// ── Dividers / on-colors ──────────────────────────────────────────────────────
val DividerLight = Color(0x29201E1D) // text @16%
val DividerDark  = Color(0x1FFFFFFF) // white @12%
val TextMutedLight = Color(0x94201E1D) // text @58%
val TextMutedDark  = Color(0x94F9F4ED)

// ── Splash / brand mark ───────────────────────────────────────────────────────
val SplashAccentLight = AccentBase
val SplashAccent2Light = Accent2Base
val SplashAccentLightSoft = Accent300

// ── Timetable category accents (derived from the Organic runtime's
//    categoryColorFor: Academic/Social → accent, Fitness/Personal → accent-2) ──
val CategoryAcademic = AccentBase
val CategorySocial   = AccentBase
val CategoryFitness  = Accent2Base
val CategoryPersonal = Accent2Base

// ── Legacy aliases so any lingering references compile (deprecated) ─────────
// TODO(redesign): remove once Splash/Dashboard/Timetable are rewritten (Phase 4/5).
@Deprecated("Use AccentBase", ReplaceWith("AccentBase"))
val DeepCalmingBlue = AccentBase
@Deprecated("Use Accent2Base", ReplaceWith("Accent2Base"))
val WarmOrange = Accent2Base
@Deprecated("Use DangerLight", ReplaceWith("DangerLight"))
val CriticalRed = DangerLight
@Deprecated("Splash restyled without a gradient bloom (Phase 4)")
val GradientStart = AccentBase
@Deprecated("Splash restyled without a gradient bloom (Phase 4)")
val GradientEnd = Accent700
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val CategorySocialLight = AccentBase
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val CategorySocialDark = Accent400
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val CategorySocialContainerLight = Accent100
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val CategorySocialContainerDark = Accent900
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val OnCategorySocialContainerLight = Accent800
@Deprecated("Timetable folds into Tasks with plain accent/accent2 category colors (Phase 4)")
val OnCategorySocialContainerDark = Accent100
