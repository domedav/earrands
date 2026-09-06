package com.domedav.ballanceometer.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders as GlanceColorProviders

@Composable
fun BallanceometerGlanceTheme(
    dynamicColor: Boolean = true,
    content: @Composable @GlanceComposable () -> Unit
) {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(
            colors = GlanceColorProviders(LightColorScheme, DarkColorScheme),
            content = content
        )
    }
}
