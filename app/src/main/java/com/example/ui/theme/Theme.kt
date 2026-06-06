package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val EditorialLightScheme = lightColorScheme(
  primary = EditorialPrimary,
  onPrimary = EditorialOnPrimary,
  primaryContainer = EditorialPrimaryContainer,
  onPrimaryContainer = EditorialOnPrimaryContainer,
  secondary = EditorialSecondary,
  onSecondary = EditorialOnSecondary,
  secondaryContainer = EditorialSecondaryContainer,
  onSecondaryContainer = EditorialOnSecondaryContainer,
  background = EditorialBackground,
  onBackground = EditorialOnBackground,
  surface = EditorialSurface,
  onSurface = EditorialOnSurface,
  surfaceVariant = EditorialSurfaceVariant,
  onSurfaceVariant = EditorialOnSurfaceVariant,
  outline = EditorialOutline,
  outlineVariant = EditorialOutlineVariant
)

private val EditorialDarkScheme = darkColorScheme(
  primary = Color(0xFF9CCC9C),
  onPrimary = Color(0xFF0F3818),
  primaryContainer = Color(0xFF224E28),
  onPrimaryContainer = Color(0xFFD8E7D3),
  secondary = Color(0xFFE4C56E),
  onSecondary = Color(0xFF3B3000),
  secondaryContainer = Color(0xFF554600),
  onSecondaryContainer = Color(0xFFF2E7D3),
  background = Color(0xFF111411),
  onBackground = Color(0xFFF7FAF4),
  surface = Color(0xFF1E221E),
  onSurface = Color(0xFFEDF1EC),
  surfaceVariant = Color(0xFF292E28),
  onSurfaceVariant = Color(0xFFC1C9BE),
  outline = Color(0xFF8D938A),
  outlineVariant = Color(0xFF424940)
)

private val DarkColorScheme = EditorialDarkScheme

private val LightColorScheme = EditorialLightScheme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to apply hand-crafted Editorial styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
