package com.example.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object FitTrackSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

object FitTrackRadius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 22.dp
    val xl = 28.dp
    val pill = 999.dp
}

object FitTrackSizes {
    val minTouchTarget = 48.dp
    val buttonHeight = 52.dp
    val compactButtonHeight = 44.dp
    val iconSm = 18.dp
    val iconMd = 22.dp
    val iconLg = 28.dp
    val avatarSm = 40.dp
    val avatarLg = 68.dp
    val bottomNavHeight = 72.dp
}

object FitTrackMotion {
    const val durationFast = 180
    const val durationMedium = 300
    const val durationSlow = 650
    const val splashHoldMs = 950L
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }
}
