package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val LocalWallxDarkTheme = compositionLocalOf { false }

private val LightMonochromeColorScheme = lightColorScheme(
    primary = JetBlack,
    onPrimary = OnJetBlack,
    primaryContainer = SilverContainer,
    onPrimaryContainer = OnSilverContainer,
    secondary = CharcoalBlack,
    onSecondary = PureWhite,
    secondaryContainer = SilverSurface,
    onSecondaryContainer = JetBlack,
    tertiary = ChromeSilver,
    onTertiary = PureWhite,
    background = AlabasterWhite,
    onBackground = JetBlack,
    surface = SilverSurfaceLowest,
    onSurface = JetBlack,
    surfaceVariant = SilverSurface,
    onSurfaceVariant = GraphiteText,
    surfaceContainerLowest = SilverSurfaceLowest,
    surfaceContainerLow = SilverSurfaceLowest,
    surfaceContainer = SilverSurfaceLow,
    surfaceContainerHigh = SilverSurface,
    surfaceContainerHighest = SilverSurfaceHigh,
    outline = SilverOutline,
    outlineVariant = SilverOutlineVariant,
    error = CrimsonError,
    onError = PureWhite,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = OnCrimsonErrorContainer
)

private val DarkMonochromeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = JetBlack,
    primaryContainer = DarkSurfaceHigh,
    onPrimaryContainer = PlatinumWhite,
    secondary = BrushedSilverLight,
    onSecondary = JetBlack,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = PlatinumWhite,
    tertiary = ChromeSilver,
    onTertiary = JetBlack,
    background = DarkObsidianBg,
    onBackground = PlatinumWhite,
    surface = DarkSurfaceLow,
    onSurface = PlatinumWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkSilverText,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    outline = DarkSilverOutline,
    outlineVariant = DarkSilverOutlineVariant,
    error = DarkCrimsonError,
    onError = JetBlack,
    errorContainer = DarkCrimsonErrorContainer,
    onErrorContainer = DarkOnCrimsonErrorContainer
)

val WallxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun WallxTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkMonochromeColorScheme else LightMonochromeColorScheme
    CompositionLocalProvider(LocalWallxDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = scheme,
            typography = WallxTypography,
            shapes = WallxShapes,
            content = content
        )
    }
}
