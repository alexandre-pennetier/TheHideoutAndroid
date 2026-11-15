package com.austinhodak.thehideout.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Screens were designed for opaque system bars. With edge-to-edge enforced (targetSdk 35+), keep content out
 * of the bars, cutouts and IME, and paint the bar areas with [barColor]. Nested uses add nothing because the
 * padding consumes the insets.
 */
@Composable
fun SystemBarsBox(
    barColor: Color,
    content: @Composable () -> Unit,
) {
    Box(Modifier.background(barColor).windowInsetsPadding(WindowInsets.safeDrawing)) {
        content()
    }
}
