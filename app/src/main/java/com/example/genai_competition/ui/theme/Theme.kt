package com.example.genai_competition.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TutorDarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = MidnightBlue,
    primaryContainer = GoldAccentDark,
    onPrimaryContainer = MidnightBlue,
    secondary = TextSecondary,
    onSecondary = MidnightBlue,
    background = MidnightBlue,
    onBackground = TextPrimary,
    surface = SteelShadow,
    onSurface = TextPrimary,
    surfaceVariant = SlateMist,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
    outlineVariant = OutlineColor,
    inverseSurface = GoldAccent,
    inverseOnSurface = MidnightBlue,
    surfaceTint = GoldAccent
)

private val TutorLightColorScheme = lightColorScheme(
    primary = GoldAccentDark,
    onPrimary = MidnightBlue,
    primaryContainer = GoldAccent,
    onPrimaryContainer = MidnightBlue,
    secondary = SlateMist,
    onSecondary = TextPrimary,
    background = Color(0xFFF7F4EC),
    onBackground = Color(0xFF1A2233),
    surface = Color(0xFFFBFAF6),
    onSurface = Color(0xFF1A2233),
    surfaceVariant = Color(0xFFE4E6EF),
    onSurfaceVariant = Color(0xFF4A5266),
    outline = Color(0xFF7E8799),
    outlineVariant = Color(0xFFABB2C3),
    inverseSurface = Color(0xFF2C3446),
    inverseOnSurface = TextPrimary,
    surfaceTint = GoldAccentDark
)

@Composable
fun GenAI_CompetitionTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) TutorDarkColorScheme else TutorLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
