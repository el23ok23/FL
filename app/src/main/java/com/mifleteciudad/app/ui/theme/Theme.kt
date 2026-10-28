package com.mifleteciudad.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Marca: naranja "flete" + neutros cálidos. Sin tipografías externas (más liviano).
private val Brand = Color(0xFFFF5A1F)
private val BrandDark = Color(0xFFFF8A4C)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3D5),
    onPrimaryContainer = Color(0xFF5A1E00),
    secondary = Color(0xFF1F2937),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF111827),
    tertiary = Color(0xFF0E7490),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF4FA),
    onTertiaryContainer = Color(0xFF083344),
    background = Color(0xFFFAFAF9),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280),
    surfaceContainer = Color(0xFFF5F5F4),
    surfaceContainerHigh = Color(0xFFEFEFED),
    surfaceContainerLow = Color(0xFFFCFCFB),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = Color(0xFF3A1200),
    primaryContainer = Color(0xFF6B2A05),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF273244),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFF4FA),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF161920),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF1F232C),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = Color(0xFF1B1F27),
    surfaceContainerHigh = Color(0xFF242934),
    surfaceContainerLow = Color(0xFF13161C),
    outline = Color(0xFF3A3F4B),
    outlineVariant = Color(0xFF2A2F3A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5B1A1A),
    onErrorContainer = Color(0xFFFECACA),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val AppTypography: Typography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1.5).sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun MiFleteTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
