package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode {
  LIGHT,
  BLACK,
}

val LocalIsBlackTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
  primary = CaribeCobalt,
  onPrimary = Color.White,
  primaryContainer = CaribeCobaltContainer,
  onPrimaryContainer = OnCaribeCobaltContainer,
  secondary = Terracotta,
  onSecondary = Color.White,
  secondaryContainer = TerracottaContainer,
  onSecondaryContainer = OnTerracottaContainer,
  tertiary = PalmTeal,
  onTertiary = Color.White,
  tertiaryContainer = PalmTealContainer,
  onTertiaryContainer = OnPalmTealContainer,
  background = WarmCreamBackground,
  onBackground = InkPrimary,
  surface = PureCardSurface,
  onSurface = InkPrimary,
  surfaceVariant = WarmSurfaceVariant,
  onSurfaceVariant = InkSecondary,
  outline = WarmOutline,
  outlineVariant = WarmOutline.copy(alpha = 0.6f),
  error = UrgencyRed,
  onError = Color.White,
  errorContainer = UrgencyRedContainer,
  onErrorContainer = UrgencyRed,
)

private val PureBlackColorScheme = darkColorScheme(
  primary = BrandSkyBlue,
  onPrimary = Color(0xFF061B36),
  primaryContainer = BrandSkyBlueContainer,
  onPrimaryContainer = Color(0xFFDCEAFF),
  secondary = DarkTerracotta,
  onSecondary = Color(0xFF471009),
  secondaryContainer = Color(0xFF5D2118),
  onSecondaryContainer = TerracottaContainer,
  tertiary = DarkPalmTeal,
  onTertiary = Color(0xFF00382C),
  tertiaryContainer = Color(0xFF084B3D),
  onTertiaryContainer = PalmTealContainer,
  background = PureBlackBackground,
  onBackground = Color(0xFFF3F6FC),
  surface = PureBlackSurface,
  onSurface = Color(0xFFF3F6FC),
  surfaceVariant = PureBlackSurfaceVariant,
  onSurfaceVariant = Color(0xFFA9B4C7),
  outline = PureBlackOutline,
  outlineVariant = PureBlackOutline.copy(alpha = 0.7f),
  error = Color(0xFFFF8A80),
  onError = Color(0xFF4F0E0E),
  errorContainer = UrgencyRedDarkContainer,
  onErrorContainer = Color(0xFFFFDAD6),
)

val IaestudioShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun IaestudioTheme(
  themeMode: AppThemeMode = AppThemeMode.LIGHT,
  content: @Composable () -> Unit,
) {
  val isBlack = themeMode == AppThemeMode.BLACK
  val colorScheme = if (isBlack) PureBlackColorScheme else LightColorScheme

  CompositionLocalProvider(LocalIsBlackTheme provides isBlack) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = IaestudioTypography,
      shapes = IaestudioShapes,
      content = content,
    )
  }
}
