package com.squishey.stoolsentry.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ThroneColorScheme = darkColorScheme(
    primary = Throne.Gold,
    onPrimary = Throne.OnGold,
    primaryContainer = Color(0xFF4A3420),
    onPrimaryContainer = Throne.GoldLight,
    inversePrimary = Throne.GoldDeep,
    secondary = Throne.GoldDim,
    onSecondary = Throne.OnGold,
    secondaryContainer = Throne.CardRaised,
    onSecondaryContainer = Throne.Porcelain,
    tertiary = Throne.Rose,
    onTertiary = Throne.OnGold,
    tertiaryContainer = Throne.RoseContainer,
    onTertiaryContainer = Throne.Rose,
    background = Throne.Espresso,
    onBackground = Throne.Porcelain,
    surface = Throne.Espresso,
    onSurface = Throne.Porcelain,
    surfaceVariant = Throne.Card,
    onSurfaceVariant = Throne.TextMuted,
    surfaceTint = Throne.Gold,
    inverseSurface = Throne.Porcelain,
    inverseOnSurface = Throne.Espresso,
    error = Throne.Error,
    onError = Throne.OnGold,
    errorContainer = Throne.RoseContainer,
    onErrorContainer = Throne.Rose,
    outline = Throne.Outline,
    outlineVariant = Throne.OutlineSoft,
    scrim = Color(0xCC000000),
    surfaceBright = Throne.CardHighest,
    surfaceDim = Throne.Espresso,
    surfaceContainerLowest = Throne.Espresso,
    surfaceContainerLow = Throne.Walnut,
    surfaceContainer = Throne.Card,
    surfaceContainerHigh = Throne.CardRaised,
    surfaceContainerHighest = Throne.CardHighest,
)

private val ThroneShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun StoolSentryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThroneColorScheme,
        typography = ThroneTypography,
        shapes = ThroneShapes,
        content = content,
    )
}
