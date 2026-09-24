package com.alvin.neuromind.ui.theme

import androidx.compose.ui.unit.dp

// The app had no spacing scale before this redesign — 242 raw .dp literals
// spread across 15 UI files, each screen retyping its own convention. These
// values are the ones the Organic design's screens actually use; prefer them
// over a fresh literal when writing or touching a screen.
object Dimens {
    val ScreenPaddingHorizontal = 20.dp
    val ScreenPaddingTop = 28.dp
    val ScreenPaddingBottomWithNav = 100.dp // clears the sticky bottom nav
    val ScreenPaddingBottomNoNav = 40.dp

    val SectionGap = 22.dp
    val CardGap = 16.dp
    val ListGap = 10.dp
    val ChipGap = 8.dp
    val RowGap = 12.dp

    val CardPadding = 18.dp
    val CardPaddingCompact = 14.dp

    val CircleButtonSize = 36.dp
    val CircleButtonSizeLarge = 44.dp
    val FabSize = 56.dp

    val DividerThickness = 1.dp
}
