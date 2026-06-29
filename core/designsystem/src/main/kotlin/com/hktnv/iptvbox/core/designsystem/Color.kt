package com.hktnv.iptvbox.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

// SURFACES
internal val Background = Color(0xFF0B0E13)
internal val Surface = Color(0xFF151A21)
internal val SurfaceVariant = Color(0xFF1E242D)
internal val SurfaceBorder = Color(0xFF2A313B)
internal val CardTitleSurface = Color(0xFF11151B)
internal val FocusBorder = Color(0xFF73879D)

// ACCENT (tek vurgu, sadece ana aksiyon)
internal val Accent = Color(0xFF00C896)
internal val AccentPressed = Color(0xFF00A57C)
internal val AccentSubtle = Color(0xFF14352D)
internal val AccentText = Color(0xFF4FD1AB)

// BADGES
internal val BadgeFilm = Color(0xFF3A3320)
internal val BadgeFilmText = Color(0xFFD4B95E)

// TEXT
internal val TextPrimary = Color(0xFFE8EAED)
internal val TextSecondary = Color(0xFF9AA0A6)
internal val TextDisabled = Color(0xFF5A626C)

// STATUS
internal val Error = Color(0xFFE5484D)
internal val Warning = Color(0xFFD4A24E)

val ColorScheme.surfaceBorder: Color
    get() = outline

val ColorScheme.cardTitleSurface: Color
    get() = CardTitleSurface

val ColorScheme.focusBorder: Color
    get() = FocusBorder

val ColorScheme.accentPressed: Color
    get() = AccentPressed

val ColorScheme.accentSubtle: Color
    get() = primaryContainer

val ColorScheme.accentText: Color
    get() = onPrimaryContainer

val ColorScheme.badgeFilm: Color
    get() = tertiaryContainer

val ColorScheme.badgeFilmText: Color
    get() = onTertiaryContainer

val ColorScheme.textDisabled: Color
    get() = TextDisabled

val ColorScheme.warning: Color
    get() = Warning

val ColorScheme.transparent: Color
    get() = Color.Transparent
