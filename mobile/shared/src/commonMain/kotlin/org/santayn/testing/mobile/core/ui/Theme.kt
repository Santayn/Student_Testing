package org.santayn.testing.mobile.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.santayn.testing.mobile.core.storage.ThemeMode
import org.santayn.testing.mobile.resources.Res
import org.santayn.testing.mobile.resources.manrope

/*
 * Тема по токенам сайта: frontend/src/theme/tokens.css («Academic Navy»).
 *
 * Отличия от сайта ради контраста WCAG 2.1 AA (4.5:1 для текста, ГОСТ Р 52872-2019):
 * - светлая тема: приглушённый текст #64748B → #556476 (на #F1F5F9 было 4.3:1);
 * - тёмная тема: primary #4A78CF → #5A86D6 и тёмный текст на кнопках (было 3.8:1),
 *   тёмный текст на кнопках ошибки (было 3.3:1).
 * Проверяется тестом ContrastTests. Режим «Высокий контраст» — не ниже 7:1 (AAA).
 */

/** Семантические цвета, которых нет в Material ColorScheme. */
@Immutable
data class StatusColors(
    val success: Color,
    val successSoft: Color,
    val successText: Color,
    val warning: Color,
    val warningSoft: Color,
    val warningText: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerText: Color,
    val info: Color,
    val infoSoft: Color,
    val infoText: Color,
    val primarySoft: Color,
    val primarySoftText: Color,
    val shell: Color,
    val onShell: Color,
    val textMuted: Color,
)

internal val LightStatus = StatusColors(
    success = Color(0xFF16A34A), successSoft = Color(0xFFF0FDF4), successText = Color(0xFF166534),
    warning = Color(0xFFD97706), warningSoft = Color(0xFFFFFBEB), warningText = Color(0xFF92400E),
    danger = Color(0xFFDC2626), dangerSoft = Color(0xFFFEF2F2), dangerText = Color(0xFF991B1B),
    info = Color(0xFF0284C7), infoSoft = Color(0xFFF0F9FF), infoText = Color(0xFF075985),
    primarySoft = Color(0xFFDBEAFE), primarySoftText = Color(0xFF1E40AF),
    shell = Color(0xFF0F172A), onShell = Color(0xFFF8FAFC),
    textMuted = Color(0xFF556476),
)

internal val DarkStatus = StatusColors(
    success = Color(0xFF4FA56F), successSoft = Color(0xFF102319), successText = Color(0xFF9DCFAF),
    warning = Color(0xFFC39A52), warningSoft = Color(0xFF2A2113), warningText = Color(0xFFDBC28D),
    danger = Color(0xFFC86565), dangerSoft = Color(0xFF2A1718), dangerText = Color(0xFFDDA0A0),
    info = Color(0xFF5A9BC2), infoSoft = Color(0xFF102430), infoText = Color(0xFF9BC5DC),
    primarySoft = Color(0xFF172238), primarySoftText = Color(0xFFB8C9E8),
    shell = Color(0xFF080A0D), onShell = Color(0xFFD7DBE0),
    textMuted = Color(0xFF8E97A3),
)

internal val LightHighContrastStatus = StatusColors(
    success = Color(0xFF166534), successSoft = Color(0xFFF0FDF4), successText = Color(0xFF14532D),
    warning = Color(0xFF92400E), warningSoft = Color(0xFFFFFBEB), warningText = Color(0xFF78350F),
    danger = Color(0xFF991B1B), dangerSoft = Color(0xFFFEF2F2), dangerText = Color(0xFF7F1D1D),
    info = Color(0xFF075985), infoSoft = Color(0xFFF0F9FF), infoText = Color(0xFF0C4A6E),
    primarySoft = Color(0xFFDBEAFE), primarySoftText = Color(0xFF1E3A8A),
    shell = Color(0xFF000000), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF1F2937),
)

internal val DarkHighContrastStatus = StatusColors(
    success = Color(0xFF86EFAC), successSoft = Color(0xFF052E16), successText = Color(0xFFBBF7D0),
    warning = Color(0xFFFCD34D), warningSoft = Color(0xFF2A1E05), warningText = Color(0xFFFDE68A),
    danger = Color(0xFFFCA5A5), dangerSoft = Color(0xFF3B0A0A), dangerText = Color(0xFFFECACA),
    info = Color(0xFF7DD3FC), infoSoft = Color(0xFF082F49), infoText = Color(0xFFBAE6FD),
    primarySoft = Color(0xFF172554), primarySoftText = Color(0xFFBFDBFE),
    shell = Color(0xFF000000), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFFE5E7EB),
)

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF475569),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F9FF),
    onTertiaryContainer = Color(0xFF075985),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B),
    // Фон темнее карточек (на сайте #F8FAFC): на телефоне белые карточки на почти белом фоне
    // не читаются как отдельные блоки.
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF556476),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF93C5FD),
    scrim = Color(0xFF000000),
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF5A86D6),
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = Color(0xFF172238),
    onPrimaryContainer = Color(0xFFB8C9E8),
    secondary = Color(0xFFAAB1BB),
    onSecondary = Color(0xFF111419),
    secondaryContainer = Color(0xFF242A33),
    onSecondaryContainer = Color(0xFFD7DBE0),
    tertiary = Color(0xFF5A9BC2),
    onTertiary = Color(0xFFEDF3F7),
    tertiaryContainer = Color(0xFF102430),
    onTertiaryContainer = Color(0xFF9BC5DC),
    error = Color(0xFFC86565),
    onError = Color(0xFF0B0D10),
    errorContainer = Color(0xFF2A1718),
    onErrorContainer = Color(0xFFDDA0A0),
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFD7DBE0),
    surface = Color(0xFF111419),
    onSurface = Color(0xFFD7DBE0),
    surfaceVariant = Color(0xFF181C22),
    onSurfaceVariant = Color(0xFFAAB1BB),
    surfaceContainerLowest = Color(0xFF0B0D10),
    surfaceContainerLow = Color(0xFF111419),
    surfaceContainer = Color(0xFF14181D),
    surfaceContainerHigh = Color(0xFF181C22),
    surfaceContainerHighest = Color(0xFF20252C),
    outline = Color(0xFF3A424D),
    outlineVariant = Color(0xFF2A3038),
    inverseSurface = Color(0xFFD7DBE0),
    inverseOnSurface = Color(0xFF111419),
    inversePrimary = Color(0xFF2563EB),
    scrim = Color(0xFF000000),
)

/** Высокий контраст, светлый: чёрный текст на белом, тёмные границы. */
internal val LightHighContrastColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF1F2937),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF0C4A6E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F9FF),
    onTertiaryContainer = Color(0xFF0C4A6E),
    error = Color(0xFF991B1B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF1F2937),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF3F4F6),
    surfaceContainerHighest = Color(0xFFE5E7EB),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF4B5563),
    inverseSurface = Color(0xFF000000),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF93C5FD),
    scrim = Color(0xFF000000),
)

/** Высокий контраст, тёмный: белый текст на чёрном, светлые границы. */
internal val DarkHighContrastColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFFE5E7EB),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7DD3FC),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF082F49),
    onTertiaryContainer = Color(0xFFBAE6FD),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF3B0A0A),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFF9CA3AF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF1E3A8A),
    scrim = Color(0xFF000000),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

object AppTheme {
    val status: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}

@Composable
private fun manropeFamily(): FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { weight ->
        Font(
            resource = Res.font.manrope,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    }
)

@Composable
private fun appTypography(): Typography {
    val family = manropeFamily()
    val base = Typography()
    fun TextStyle.withFamily(weight: FontWeight? = null) =
        copy(fontFamily = family, fontWeight = weight ?: fontWeight)

    return Typography(
        displayLarge = base.displayLarge.withFamily(FontWeight.Bold),
        displayMedium = base.displayMedium.withFamily(FontWeight.Bold),
        displaySmall = base.displaySmall.withFamily(FontWeight.Bold),
        headlineLarge = base.headlineLarge.withFamily(FontWeight.Bold),
        headlineMedium = base.headlineMedium.withFamily(FontWeight.Bold),
        headlineSmall = base.headlineSmall.withFamily(FontWeight.SemiBold),
        titleLarge = base.titleLarge.withFamily(FontWeight.SemiBold).copy(fontSize = 20.sp),
        titleMedium = base.titleMedium.withFamily(FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFamily(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.withFamily(FontWeight.Normal),
        bodyMedium = base.bodyMedium.withFamily(FontWeight.Normal),
        bodySmall = base.bodySmall.withFamily(FontWeight.Normal),
        labelLarge = base.labelLarge.withFamily(FontWeight.SemiBold),
        labelMedium = base.labelMedium.withFamily(FontWeight.Medium),
        labelSmall = base.labelSmall.withFamily(FontWeight.Medium),
    )
}

/** Радиусы: контролы 8, карточки 12, диалоги 16 (tokens.css). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Палитра по режиму темы и настройке высокого контраста. */
internal fun colorsFor(dark: Boolean, highContrast: Boolean): Pair<ColorScheme, StatusColors> = when {
    dark && highContrast -> DarkHighContrastColors to DarkHighContrastStatus
    dark -> DarkColors to DarkStatus
    highContrast -> LightHighContrastColors to LightHighContrastStatus
    else -> LightColors to LightStatus
}

/**
 * Тема приложения.
 *
 * @param highContrast усиленная палитра (контраст текста не ниже 7:1).
 * @param textScale дополнительный множитель к системному размеру шрифта
 *   (системная настройка Android/iOS учитывается всегда).
 */
@Composable
fun StudentTestingTheme(
    themeMode: ThemeMode,
    highContrast: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val (colors, status) = colorsFor(dark, highContrast)
    val density = LocalDensity.current
    val scaledDensity = remember(density, textScale) {
        Density(density.density, density.fontScale * textScale)
    }
    CompositionLocalProvider(
        LocalStatusColors provides status,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(),
            shapes = AppShapes,
            content = content,
        )
    }
}
