package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight Fintech & Quantitative Analytics Palette (Dark)
val MidnightBg = Color(0xFF0A101D)
val MidnightSurface = Color(0xFF111A2C)
val MidnightSurfaceVariant = Color(0xFF18243C)
val MidnightOutline = Color(0xFF283858)

val GoldPrimary = Color(0xFFF5B731)
val GoldOnPrimary = Color(0xFF231900)
val GoldContainer = Color(0xFF3E2E04)
val GoldOnContainer = Color(0xFFFFDF8E)

val CyanSecondary = Color(0xFF38BDF8)
val CyanOnSecondary = Color(0xFF002538)
val CyanContainer = Color(0xFF0C354D)
val CyanOnContainer = Color(0xFFBFEAFF)

val EmeraldTertiary = Color(0xFF22C55E)
val EmeraldOnTertiary = Color(0xFF00240D)
val EmeraldContainer = Color(0xFF0A381B)
val EmeraldOnContainer = Color(0xFF9AF6B6)

val CrimsonError = Color(0xFFF43F5E)
val CrimsonOnError = Color(0xFFFFFFFF)
val CrimsonContainer = Color(0xFF4C0D1D)
val CrimsonOnContainer = Color(0xFFFFD9DF)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)

// Light Theme Counterparts
val LightBg = Color(0xFFF3F6FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE6ECF6)
val LightOutline = Color(0xFFCBD5E1)

val LightPrimary = Color(0xFF0F2C59)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD9E6FF)
val LightOnPrimaryContainer = Color(0xFF071B3D)

val LightSecondary = Color(0xFF0284C7)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD8F1FF)
val LightOnSecondaryContainer = Color(0xFF00304B)

val LightTertiary = Color(0xFF15803D)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFD7F8DF)
val LightOnTertiaryContainer = Color(0xFF052E16)

val LightError = Color(0xFFDC2626)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)

// Semantic domain colors exposed via CompositionLocal
data class AnalyticsColors(
    val goldAccent: Color,
    val hitGreen: Color,
    val hitGreenContainer: Color,
    val specialCrimson: Color,
    val specialCrimsonContainer: Color,
    val neutralBlue: Color,
    val neutralBlueContainer: Color,
    val warningAmber: Color,
    val warningAmberContainer: Color,
    val cardBorder: Color,
    val mutedText: Color,
)

val DarkAnalyticsColors = AnalyticsColors(
    goldAccent = GoldPrimary,
    hitGreen = Color(0xFF34D399),
    hitGreenContainer = Color(0xFF0C3B2B),
    specialCrimson = Color(0xFFFB7185),
    specialCrimsonContainer = Color(0xFF4A1021),
    neutralBlue = CyanSecondary,
    neutralBlueContainer = CyanContainer,
    warningAmber = Color(0xFFFBBF24),
    warningAmberContainer = Color(0xFF3B2C06),
    cardBorder = MidnightOutline,
    mutedText = TextSecondaryDark,
)

val LightAnalyticsColors = AnalyticsColors(
    goldAccent = Color(0xFFD97706),
    hitGreen = Color(0xFF15803D),
    hitGreenContainer = Color(0xFFDCFCE7),
    specialCrimson = Color(0xFFDC2626),
    specialCrimsonContainer = Color(0xFFFEE2E2),
    neutralBlue = Color(0xFF0284C7),
    neutralBlueContainer = Color(0xFFE0F2FE),
    warningAmber = Color(0xFFB45309),
    warningAmberContainer = Color(0xFFFEF3C7),
    cardBorder = LightOutline,
    mutedText = TextSecondaryLight,
)
