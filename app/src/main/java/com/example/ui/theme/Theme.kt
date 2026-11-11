package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VerdantLightColorScheme = lightColorScheme(
    primary = ForestPrimaryLight,
    onPrimary = OnForestLight,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = OnSageContainerLight,
    secondary = TerracottaSecondaryLight,
    onSecondary = OnTerracottaLight,
    secondaryContainer = ClayContainerLight,
    onSecondaryContainer = OnClayContainerLight,
    tertiary = MeadowGoldTertiaryLight,
    onTertiary = OnMeadowGoldLight,
    tertiaryContainer = PollenContainerLight,
    onTertiaryContainer = OnPollenContainerLight,
    background = AlabasterBgLight,
    onBackground = DeepLeafInkLight,
    surface = AlabasterBgLight,
    onSurface = DeepLeafInkLight,
    surfaceVariant = SageLinenLight,
    onSurfaceVariant = MutedMossLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F1EA),
    surfaceContainer = SageLinenLight,
    surfaceContainerHigh = Color(0xFFE3E7DE),
    outline = DraftingOutlineLight,
    outlineVariant = DraftingHairlineLight,
)

private val VerdantDarkColorScheme = darkColorScheme(
    primary = BrightSageDark,
    onPrimary = OnBrightSageDark,
    primaryContainer = DeepSageContainerDark,
    onPrimaryContainer = OnDeepSageContainerDark,
    secondary = WarmClayDark,
    onSecondary = OnWarmClayDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = SoftPollenDark,
    onTertiary = OnSoftPollenDark,
    tertiaryContainer = OchreContainerDark,
    onTertiaryContainer = OnOchreContainerDark,
    background = MossCharcoalDark,
    onBackground = AlabasterInkDark,
    surface = MossCharcoalDark,
    onSurface = AlabasterInkDark,
    surfaceVariant = PineSlateDark,
    onSurfaceVariant = SoftSageTextDark,
    surfaceContainerLowest = Color(0xFF0D1410),
    surfaceContainerLow = Color(0xFF17211B),
    surfaceContainer = PineSlateDark,
    surfaceContainerHigh = Color(0xFF243129),
    outline = MossOutlineDark,
    outlineVariant = PineHairlineDark,
)

val VerdantShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun VerdantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) VerdantDarkColorScheme else VerdantLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VerdantTypography,
        shapes = VerdantShapes,
        content = content,
    )
}
