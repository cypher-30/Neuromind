package com.alvin.neuromind.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// One consistent quiet TopAppBar used by every non-Dashboard top-level and
// pushed screen, so chrome doesn't drift screen-to-screen. Dashboard keeps its
// own gradient hero header — this is intentionally NOT used there. Focus Mode
// uses the transparent variant to preserve its immersive full-bleed body while
// still getting a conventional exit affordance.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuromindTopBar(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    useCloseIcon: Boolean = false,
    transparent: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        if (useCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (useCloseIcon) "Close" else "Back"
                    )
                }
            }
        },
        actions = actions,
        colors = if (transparent) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}
