package com.guptrix.app.ui.theme

import androidx.compose.ui.graphics.Color

// Obsidian Dark Theme
val BackgroundDark = Color(0xFF101216)
val SurfaceDark = Color(0xFF181B20)
val SurfaceCardDark = Color(0xFF1E222A)
val BorderSubtleDark = Color(0xFF282D37)
val PrimaryAccentDark = Color(0xFF3B82F6)
val TextPrimaryDark = Color(0xFFF3F4F6)
val TextSecondaryDark = Color(0xFF9CA3AF)
val ErrorDark = Color(0xFFEF4444)

// Clean Light Theme
val BackgroundLight = Color(0xFFF9FAFB)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceCardLight = Color(0xFFFFFFFF)
val BorderSubtleLight = Color(0xFFE5E7EB)
val PrimaryAccentLight = Color(0xFF2563EB)
val TextPrimaryLight = Color(0xFF111827)
val TextSecondaryLight = Color(0xFF6B7280)
val ErrorLight = Color(0xFFDC2626)

// Note Accent Palette
object NoteColors {
    fun getBackgroundColor(colorName: String, isDark: Boolean): Color {
        return if (isDark) {
            when (colorName.lowercase()) {
                "yellow" -> Color(0xFF2A230B)
                "green" -> Color(0xFF0D2818)
                "blue" -> Color(0xFF0F223D)
                "pink" -> Color(0xFF331323)
                "purple" -> Color(0xFF241238)
                "orange" -> Color(0xFF33170E)
                else -> SurfaceCardDark
            }
        } else {
            when (colorName.lowercase()) {
                "yellow" -> Color(0xFFFEF9C3)
                "green" -> Color(0xFFDCFCE7)
                "blue" -> Color(0xFFDBEAFE)
                "pink" -> Color(0xFFFCE7F3)
                "purple" -> Color(0xFFF3E8FF)
                "orange" -> Color(0xFFFFEDD5)
                else -> SurfaceCardLight
            }
        }
    }

    fun getBorderColor(colorName: String, isDark: Boolean): Color {
        return if (isDark) {
            when (colorName.lowercase()) {
                "yellow" -> Color(0xFF5E4E14)
                "green" -> Color(0xFF1B4D2E)
                "blue" -> Color(0xFF1E3A8A)
                "pink" -> Color(0xFF652042)
                "purple" -> Color(0xFF4C2870)
                "orange" -> Color(0xFF602E1D)
                else -> BorderSubtleDark
            }
        } else {
            when (colorName.lowercase()) {
                "yellow" -> Color(0xFFFDE047)
                "green" -> Color(0xFF86EFAC)
                "blue" -> Color(0xFF93C5FD)
                "pink" -> Color(0xFFF9A8D4)
                "purple" -> Color(0xFFD8B4FE)
                "orange" -> Color(0xFFFDBA74)
                else -> BorderSubtleLight
            }
        }
    }
}
