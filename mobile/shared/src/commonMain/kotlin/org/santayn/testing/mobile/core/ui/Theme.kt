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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.storage.ThemeMode
import org.santayn.testing.mobile.core.ui.design.BrandColors
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.core.ui.design.DesignTokens
import org.santayn.testing.mobile.core.ui.design.LocalBrandColors
import org.santayn.testing.mobile.core.ui.design.LocalDesign
import org.santayn.testing.mobile.core.ui.design.PolytechPalette
import org.santayn.testing.mobile.core.ui.design.ThemePalette
import org.santayn.testing.mobile.core.ui.design.polytechTokens

/*
 * Сборка темы приложения: тема оформления (core/ui/design) × светлая/тёмная схема × высокий контраст
 * × размер текста.
 *
 * Контраст текста — WCAG 2.1 AA (4.5:1, ГОСТ Р 52872-2019) во всех палитрах тем и AAA (7:1)
 * в режиме «Высокий контраст»; проверяется тестом ContrastTests для каждой темы.
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

// region Высокий контраст — общий для всех тем (формы и шрифты остаются от темы)

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

internal val LightHighContrastBrand = BrandColors(
    hero = Color(0xFF1E3A8A), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFFFFFFF),
    heroChip = Color(0xFF172E6E),
    accent = Color(0xFF166534), onAccent = Color(0xFFFFFFFF),
    highlight = Color(0xFF1E3A8A), onHighlight = Color(0xFFFFFFFF),
)

internal val DarkHighContrastBrand = BrandColors(
    hero = Color(0xFF172554), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFFFFFFF),
    heroChip = Color(0xFF000000),
    accent = Color(0xFF86EFAC), onAccent = Color(0xFF000000),
    highlight = Color(0xFFBFDBFE), onHighlight = Color(0xFF000000),
)

// endregion

val LocalStatusColors = staticCompositionLocalOf { PolytechPalette.lightStatus }

object AppTheme {
    val status: StatusColors
        @Composable @ReadOnlyComposable get() = LocalStatusColors.current
}

/** Палитра темы оформления. */
internal fun DesignTheme.palette(): ThemePalette = when (this) {
    DesignTheme.POLYTECH -> PolytechPalette
}

/** Токены темы оформления (шрифты грузятся из ресурсов — поэтому @Composable). */
@Composable
internal fun DesignTheme.tokens(): DesignTokens = when (this) {
    DesignTheme.POLYTECH -> polytechTokens()
}

/** Итоговые цвета: тема × светлая/тёмная × высокий контраст. */
internal data class ResolvedColors(val scheme: ColorScheme, val status: StatusColors, val brand: BrandColors)

internal fun colorsFor(theme: DesignTheme, dark: Boolean, highContrast: Boolean): ResolvedColors {
    val p = theme.palette()
    return when {
        dark && highContrast -> ResolvedColors(DarkHighContrastColors, DarkHighContrastStatus, DarkHighContrastBrand)
        highContrast -> ResolvedColors(LightHighContrastColors, LightHighContrastStatus, LightHighContrastBrand)
        dark -> ResolvedColors(p.dark, p.darkStatus, p.darkBrand)
        else -> ResolvedColors(p.light, p.lightStatus, p.lightBrand)
    }
}

/** Шкала Material 3: заголовки — акцидентный шрифт темы, остальное — основной. */
private fun appTypography(display: FontFamily, body: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.with(family: FontFamily, weight: FontWeight) = copy(fontFamily = family, fontWeight = weight)
    return Typography(
        displayLarge = base.displayLarge.with(display, FontWeight.SemiBold),
        displayMedium = base.displayMedium.with(display, FontWeight.SemiBold),
        displaySmall = base.displaySmall.with(display, FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.with(display, FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.with(display, FontWeight.SemiBold),
        headlineSmall = base.headlineSmall.with(display, FontWeight.SemiBold).copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = base.titleLarge.with(display, FontWeight.SemiBold).copy(fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = base.titleMedium.with(body, FontWeight.SemiBold).copy(fontSize = 17.sp),
        titleSmall = base.titleSmall.with(body, FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.with(body, FontWeight.Normal),
        bodyMedium = base.bodyMedium.with(body, FontWeight.Normal),
        bodySmall = base.bodySmall.with(body, FontWeight.Normal),
        labelLarge = base.labelLarge.with(body, FontWeight.SemiBold),
        labelMedium = base.labelMedium.with(body, FontWeight.Medium),
        labelSmall = base.labelSmall.with(body, FontWeight.Medium),
    )
}

/** Радиусы Material для компонентов, которые их берут из темы (диалоги, меню, поля). */
/**
 * Формы Material-компонентов из токенов темы, чтобы стандартные компоненты совпадали с фирменными:
 * extraSmall — поля ввода и меню, small — чипы-фильтры, medium — карточки,
 * large — плавающие кнопки (FAB), extraLarge — диалоги и нижние листы.
 */
private fun appShapes(tokens: DesignTokens): Shapes = Shapes(
    extraSmall = tokens.fieldShape,
    small = tokens.chipShape,
    medium = tokens.cardShape,
    large = tokens.buttonShape,
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Тема приложения.
 *
 * @param design тема оформления (настройка «Стиль оформления»).
 * @param highContrast усиленная палитра (контраст текста не ниже 7:1); у карточек появляется рамка.
 * @param textScale дополнительный множитель к системному размеру шрифта.
 */
@Composable
fun StudentTestingTheme(
    themeMode: ThemeMode,
    design: DesignTheme = DesignTheme.DEFAULT,
    highContrast: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = colorsFor(design, dark, highContrast)
    val baseTokens = design.tokens()
    // В высоком контрасте карточка белая на белом/чёрная на чёрном — нужна рамка.
    val tokens = if (highContrast) baseTokens.copy(cardBorder = true) else baseTokens
    val density = LocalDensity.current
    val scaledDensity = remember(density, textScale) { Density(density.density, density.fontScale * textScale) }
    CompositionLocalProvider(
        LocalStatusColors provides colors.status,
        LocalBrandColors provides colors.brand,
        LocalDesign provides tokens,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colors.scheme,
            typography = appTypography(tokens.displayFont, tokens.bodyFont),
            shapes = appShapes(tokens),
            content = content,
        )
    }
}
