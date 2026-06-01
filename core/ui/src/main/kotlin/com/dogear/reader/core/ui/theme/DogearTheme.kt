package com.dogear.reader.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dogear.reader.core.model.AppPalette
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.ThemeMode

/**
 * The app theme. Decoupled from the reading-surface theme (which the reader will own). Resolves
 * dark/light from [themeMode], the color scheme from the chosen [palette] (with optional
 * Material You dynamic color on Android 12+), and typography from the chosen [uiFont].
 */
@Composable
fun DogearTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppPalette = AppPalette.INK,
    uiFont: FontChoice = FontChoice.INTER,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (palette == AppPalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        staticColorScheme(palette, dark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dogearTypography(uiFont),
        content = content,
    )
}
