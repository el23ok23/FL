package com.fletaba.app.ui.theme

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

// Marca Fletaba: los colores del logo. Celeste profundo (camión y fondo) y dorado del sol.
// Sin tipografías externas (más liviano).
val FletabaBlue = Color(0xFF1B7BE0)
val FletabaBlueDark = Color(0xFF8FC4FF)
val FletabaOrange = Color(0xFFE8A317)      // dorado del sol (acento)
val FletabaOrangeDark = Color(0xFFF6C453)

private val LightColors = lightColorScheme(
    primary = FletabaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF07305E),
    secondary = Color(0xFF1F2937),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF111827),
    tertiary = FletabaOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEFC2),
    onTertiaryContainer = Color(0xFF4A3300),
    background = Color(0xFFF4F8FE),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9F0FA),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainer = Color(0xFFF1F3F9),
    surfaceContainerHigh = Color(0xFFE9ECF5),
    surfaceContainerLow = Color(0xFFFBFCFE),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = FletabaBlueDark,
    onPrimary = Color(0xFF07194A),
    primaryContainer = Color(0xFF0F4A8A),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFFCBD5E1),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF273244),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = FletabaOrangeDark,
    onTertiary = Color(0xFF3A1200),
    tertiaryContainer = Color(0xFF5C4200),
    onTertiaryContainer = Color(0xFFFFEFC2),
    background = Color(0xFF0B0F1A),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF131927),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1C2435),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainer = Color(0xFF18202F),
    surfaceContainerHigh = Color(0xFF202A3C),
    surfaceContainerLow = Color(0xFF101623),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF253044),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5B1A1A),
    onErrorContainer = Color(0xFFFECACA),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val AppTypography: Typography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Aclara un color mezclándolo con blanco (para la variante del modo oscuro). */
private fun lighten(c: Color, amount: Float): Color = Color(
    red = c.red + (1f - c.red) * amount,
    green = c.green + (1f - c.green) * amount,
    blue = c.blue + (1f - c.blue) * amount,
    alpha = 1f,
)

/** Oscurece un color mezclándolo con negro. */
private fun darken(c: Color, amount: Float): Color = Color(c.red * (1f - amount), c.green * (1f - amount), c.blue * (1f - amount), 1f)

private fun luminance(c: Color): Float = 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue

/**
 * Tema con los colores de la marca del cliente: [primary] para botones, origen y precio;
 * [accent] para destino y detalles. Con los valores por defecto es la paleta de Fletaba.
 */
@Composable
fun FletabaTheme(primary: Color = FletabaBlue, accent: Color = FletargOrangeDefault, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (primary == FletabaBlue && accent == FletargOrangeDefault) {
        if (dark) DarkColors else LightColors
    } else if (dark) {
        val p = lighten(primary, 0.45f)
        val a = lighten(accent, 0.3f)
        DarkColors.copy(
            primary = p, onPrimary = darken(primary, 0.75f), primaryContainer = darken(primary, 0.45f), onPrimaryContainer = lighten(primary, 0.8f),
            tertiary = a, onTertiary = darken(accent, 0.75f), tertiaryContainer = darken(accent, 0.55f), onTertiaryContainer = lighten(accent, 0.8f),
        )
    } else {
        LightColors.copy(
            primary = primary, onPrimary = if (luminance(primary) > 0.6f) Color(0xFF0F172A) else Color.White,
            primaryContainer = lighten(primary, 0.82f), onPrimaryContainer = darken(primary, 0.6f),
            tertiary = accent, onTertiary = if (luminance(accent) > 0.6f) Color(0xFF3B2A00) else Color.White,
            tertiaryContainer = lighten(accent, 0.75f), onTertiaryContainer = darken(accent, 0.65f),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
