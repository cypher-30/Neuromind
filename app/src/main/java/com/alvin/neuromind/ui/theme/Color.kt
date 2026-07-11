package com.alvin.neuromind.ui.theme

import androidx.compose.ui.graphics.Color

// ── Semantic color vocabulary ───────────────────────────────────────────────
// One meaning per role, applied identically across every screen:
//   primary (teal)    → structural / interactive brand chrome
//   secondary (sunset) → reserved for "Neuromind is coaching you" surfaces
//                         (Suggestion cards, AI Plan, rebalance nudges, Assistant)
//   tertiary (seagreen) → positive / on-track / completed-good state
//   error (ember)      → overdue / warning only
// Priority mapping (single source of truth — see ui/theme/PriorityColors.kt):
//   HIGH → error, MEDIUM → tertiary, LOW → secondary

// ── Brand Colors — "Teal & Sunset" ──────────────────────────────────────────
val BrandTealLight       = Color(0xFF0F7A85)   // deep teal — primary (light)
val BrandTealDark        = Color(0xFF4FC3BE)   // softer teal for dark surfaces
val BrandSunsetLight     = Color(0xFFF2884B)   // sunset orange — accent (light)
val BrandSunsetDark      = Color(0xFFFFA873)   // soft amber-orange for dark surfaces
val BrandSeagreenLight   = Color(0xFF2E9E8F)   // seagreen tertiary (light)
val BrandSeagreenDark    = Color(0xFF6FD6C4)   // vivid mint-teal for dark surfaces

// ── Containers ────────────────────────────────────────────────────────────────
val PrimaryContainerLight    = Color(0xFFC9EAE8)
val PrimaryContainerDark     = Color(0xFF08434A)
val SecondaryContainerLight  = Color(0xFFFCE0CC)
val SecondaryContainerDark   = Color(0xFF7A3D14)
// Tertiary containers are pushed toward sage/olive-green (rather than a paler
// teal) so they stay visually distinct from primaryContainer wherever both
// appear side by side (Timetable Academic vs. Fitness, Insights stat cards) —
// a near-identical pale teal/mint pairing was indistinguishable at a glance.
val TertiaryContainerLight   = Color(0xFFD3ECC0)
val TertiaryContainerDark    = Color(0xFF2C4A1D)

// ── On-Container ──────────────────────────────────────────────────────────────
val OnPrimaryContainerLight  = Color(0xFF04282B)
val OnPrimaryContainerDark   = Color(0xFFC9EAE8)
val OnSecondaryContainerLight = Color(0xFF3D1D00)
val OnSecondaryContainerDark = Color(0xFFFCE0CC)
val OnTertiaryContainerLight = Color(0xFF23390F)
val OnTertiaryContainerDark  = Color(0xFFD3ECC0)

// ── Backgrounds & Surfaces ────────────────────────────────────────────────────
val LightBackground      = Color(0xFFF6F3EC)   // warm sand — not stock Material white
val DarkBackground        = Color(0xFF12181A)   // teal-tinted near-dark — deliberately not pure black
val LightSurface         = Color(0xFFFFFFFF)
val DarkSurface          = Color(0xFF1B2426)
val SurfaceVariantLight  = Color(0xFFEAE6DA)
val SurfaceVariantDark   = Color(0xFF26312F)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary          = Color(0xFF10262A)   // teal-ink
val TextPrimaryDark      = Color(0xFFE7F1EF)
val OutlineLight         = Color(0xFF8B958F)
val OutlineDark          = Color(0xFF6E7C78)

// ── Error (overdue / warning only) ─────────────────────────────────────────────
val ErrorLight           = Color(0xFFD65A3A)   // ember
val ErrorDark            = Color(0xFFFF8A6B)
val ErrorContainerLight  = Color(0xFFFFDACF)
val ErrorContainerDark   = Color(0xFF6E2210)
val OnErrorContainerLight = Color(0xFF3A0900)
val OnErrorContainerDark  = Color(0xFFFFDACF)

// ── Gradient Endpoints (splash + Dashboard hero header) ────────────────────────
val GradientStart        = Color(0xFF0F7A85)   // brand teal
val GradientEnd          = Color(0xFF0B5E52)   // deep seagreen-teal

// ── Timetable category accent (Social events) ──────────────────────────────────
// Timetable needs 4 visually distinct categories (Academic/Fitness/Social/
// Personal) — a legitimate categorical use, separate from the semantic brand
// roles above. Academic/Fitness/Personal borrow primary/tertiary/surface
// (which already fit loosely), but Social has no honest semantic role to
// borrow, so it gets its own dedicated plum accent instead of secondary
// (which is reserved for "Neuromind is coaching you" surfaces).
val CategorySocialLight            = Color(0xFF8E5AA0)
val CategorySocialDark             = Color(0xFFDBA9E8)
val CategorySocialContainerLight   = Color(0xFFF5DEF7)
val CategorySocialContainerDark    = Color(0xFF4C2856)
val OnCategorySocialContainerLight = Color(0xFF33163B)
val OnCategorySocialContainerDark  = Color(0xFFF5DEF7)

// ── Legacy aliases so existing code compiles without changes (deprecated) ──────
val DeepCalmingBlue      = BrandTealLight
val WarmOrange           = BrandSunsetLight
val CriticalRed          = ErrorLight
