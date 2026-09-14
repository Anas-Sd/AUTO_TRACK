package com.autotrack.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF0B0F17)
val CardBg = Color(0xFF131A26)
val BorderColor = Color(0xFF1E293B)
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldDark = Color(0xFF059669)
val RoseExpense = Color(0xFFEF4444)
val AmberWarning = Color(0xFFF59E0B)
val BlueAccent = Color(0xFF3B82F6)
val TextMuted = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    secondary = BlueAccent,
    tertiary = AmberWarning,
    background = DarkBg,
    onBackground = Color(0xFFF8FAFC),
    surface = CardBg,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = BorderColor,
    onSurfaceVariant = TextMuted,
    error = RoseExpense,
    onError = Color.White
)

@Composable
fun AutoTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
