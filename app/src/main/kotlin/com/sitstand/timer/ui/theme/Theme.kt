package com.sitstand.timer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BambooTone,
    onPrimary = SlateBlueDeep,
    primaryContainer = SlateBlueMatte,
    onPrimaryContainer = WarmWhite,
    secondary = BambooWarm,
    onSecondary = SlateBlueDeep,
    secondaryContainer = SlateBlueLight,
    onSecondaryContainer = BambooLight,
    tertiary = BambooLight,
    onTertiary = SlateBlueDeep,
    background = SlateBlueDeep,
    onBackground = WarmWhite,
    surface = SlateBlueSurface,
    onSurface = WarmWhite,
    surfaceVariant = SlateBlueMatte,
    onSurfaceVariant = BambooLight,
    surfaceContainerLowest = SlateBlueDeep,
    surfaceContainerLow = SlateBlueSurface,
    surfaceContainer = SlateBlueSurface,
    surfaceContainerHigh = SlateBlueMatte,
    surfaceContainerHighest = SlateBlueMatte,
    outline = SlateBlueLight
)

private val LightColorScheme = lightColorScheme(
    primary = SlateBlueMatte,
    onPrimary = WarmWhite,
    primaryContainer = BambooLight,
    onPrimaryContainer = SlateBlueDeep,
    secondary = BambooWarm,
    onSecondary = SlateBlueDeep,
    secondaryContainer = BambooLight,
    onSecondaryContainer = SlateBlueDeep,
    tertiary = SlateBlueLight,
    onTertiary = WarmWhite,
    background = BambooSubtle,
    onBackground = SlateBlueDeep,
    surface = WarmWhite,
    onSurface = SlateBlueDeep,
    surfaceVariant = BambooLight,
    onSurfaceVariant = SlateBlueMatte,
    surfaceContainerLowest = WarmWhite,
    surfaceContainerLow = BambooSubtle,
    surfaceContainer = WarmWhite,
    surfaceContainerHigh = BambooLight,
    surfaceContainerHighest = BambooLight,
    outline = BambooWarm
)

@Composable
fun SitStandTimerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
