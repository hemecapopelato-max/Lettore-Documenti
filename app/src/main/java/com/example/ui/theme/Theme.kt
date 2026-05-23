package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),          // Lavender accent
    onPrimary = Color(0xFF381E72),        // Deep purple contrast
    primaryContainer = Color(0xFF4F378B), // Mid purple container
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    background = Color(0xFF1C1B1F),       // Dark charcoal base
    onBackground = Color(0xFFE6E1E5),     // Off-white text
    surface = Color(0xFF2B2930),          // Warmer off-dark sheet surface
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF3B3943),
    onSurfaceVariant = Color(0xFFCAC4D0), // Medium-contrast subtext
    outline = Color(0xFF49454F),          // Material border outlines
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Lock to the cohesive Elegant Dark color scheme requested
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
