package com.austinhodak.thehideout.ui.common

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.austinhodak.thehideout.NavViewModel

// Expanded window width (Material window size classes): the main drawer is permanent from here up.
const val PERMANENT_DRAWER_MIN_WIDTH_DP = 840

/** Opens the modal drawer; renders nothing when the drawer is permanently visible. */
@Composable
fun DrawerMenuButton(
    navViewModel: NavViewModel,
    tint: Color = Color.Unspecified,
) {
    if (LocalConfiguration.current.screenWidthDp >= PERMANENT_DRAWER_MIN_WIDTH_DP) return
    IconButton(onClick = { navViewModel.isDrawerOpen.value = true }) {
        Icon(Icons.Filled.Menu, contentDescription = null, tint = tint)
    }
}
