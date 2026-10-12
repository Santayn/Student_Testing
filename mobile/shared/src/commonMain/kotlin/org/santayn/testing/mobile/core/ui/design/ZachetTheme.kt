package org.santayn.testing.mobile.core.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.DarkHighContrastStatus
import org.santayn.testing.mobile.core.ui.LightHighContrastStatus
import org.santayn.testing.mobile.core.ui.StatusColors
import org.santayn.testing.mobile.resources.Res
import org.santayn.testing.mobile.resources.rubik

/*
 * Тема C «Зачёт» — тёмная «игровая»: глубокий тёмно-синий фон #0A1A3F, ярко-зелёный #11D56B и небесный #9BD5FF,
 * «объёмные» кнопки и карточки (нижний край темнее), подписи кнопок прописными, навигация из иконок,
 * кольца прогресса, серия дней подряд. Шрифт: Rubik (один на всё, веса до 800).
 * Тёмная палитра — основная, светлая — для тех, у кого выбран светлый режим.
 * Макеты: recommendations/mobile-design-themes/mockups/varianty-dizaina.html, ряд C.
 *
 * Контраст всех пар текст/фон проверяет ContrastTests (≥ 4.5:1).
 */

internal val ZachetDark = darkColorScheme(
    primary = Color(0xFF9BD5FF),
    onPrimary = Color(0xFF0A1A3F),
    primaryContainer = Color(0xFF1B3573),
    onPrimaryContainer = Color(0xFFC9E9FF),
    secondary = Color(0xFF11D56B),
    onSecondary = Color(0xFF04261A),
    secondaryContainer = Color(0xFF0E3B2A),
    onSecondaryContainer = Color(0xFF7CF0B0),
    tertiary = Color(0xFFFFB86B),
    onTertiary = Color(0xFF2A1A06),
    tertiaryContainer = Color(0xFF2A1A06),
    onTertiaryContainer = Color(0xFFFFD4A3),
    error = Color(0xFFFFA8A0),
    onError = Color(0xFF3D0A06),
    errorContainer = Color(0xFF3D1A1A),
    onErrorContainer = Color(0xFFFFECEA),
    background = Color(0xFF0A1A3F),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF13285C),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1B3573),
    onSurfaceVariant = Color(0xFFB4C2E3),
    surfaceContainerLowest = Color(0xFF0A1A3F),
    surfaceContainerLow = Color(0xFF0F2150),
    surfaceContainer = Color(0xFF13285C),
    surfaceContainerHigh = Color(0xFF1B3573),
    surfaceContainerHighest = Color(0xFF22407F),
    outline = Color(0xFF6D86C7),
    outlineVariant = Color(0xFF26407F),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF13285C),
    inversePrimary = Color(0xFF1F48C9),
    scrim = Color(0xFF000000),
)

internal val ZachetLight = lightColorScheme(
    primary = Color(0xFF1F48C9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF142F86),
    secondary = Color(0xFF0B6B37),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF8E8),
    onSecondaryContainer = Color(0xFF0B6B37),
    tertiary = Color(0xFF8A4B00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF1D6),
    onTertiaryContainer = Color(0xFF8A4B00),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DC),
    onErrorContainer = Color(0xFF9B1C0C),
    background = Color(0xFFEEF3FF),
    onBackground = Color(0xFF0A1A3F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1A3F),
    surfaceVariant = Color(0xFFE1E9FA),
    onSurfaceVariant = Color(0xFF3D4F7A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F8FF),
    surfaceContainerHigh = Color(0xFFE1E9FA),
    surfaceContainerHighest = Color(0xFFD3DEF5),
    outline = Color(0xFF6F80AD),
    outlineVariant = Color(0xFFC9D5EE),
    inverseSurface = Color(0xFF0A1A3F),
    inverseOnSurface = Color(0xFFEEF3FF),
    inversePrimary = Color(0xFF9BD5FF),
    scrim = Color(0xFF000000),
)

internal val ZachetDarkStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFF0E3B2A), successText = Color(0xFF7CF0B0),
    warning = Color(0xFFFFB86B), warningSoft = Color(0xFF2A1A06), warningText = Color(0xFFFFD4A3),
    danger = Color(0xFFFF8A80), dangerSoft = Color(0xFF3D1A1A), dangerText = Color(0xFFFFA8A0),
    info = Color(0xFF9BD5FF), infoSoft = Color(0xFF102A55), infoText = Color(0xFFC9E9FF),
    primarySoft = Color(0xFF1B3573), primarySoftText = Color(0xFFC9E9FF),
    shell = Color(0xFF13285C), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFFB4C2E3),
)

internal val ZachetLightStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFFDDF8E8), successText = Color(0xFF0B6B37),
    warning = Color(0xFFF59E0B), warningSoft = Color(0xFFFFF1D6), warningText = Color(0xFF8A4B00),
    danger = Color(0xFFE5484D), dangerSoft = Color(0xFFFFE1DC), dangerText = Color(0xFF9B1C0C),
    info = Color(0xFF1179D5), infoSoft = Color(0xFFE0F0FF), infoText = Color(0xFF045BA7),
    primarySoft = Color(0xFFDCE6FF), primarySoftText = Color(0xFF142F86),
    shell = Color(0xFF0A1A3F), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF3D4F7A),
)

internal val ZachetDarkBrand = BrandColors(
    hero = Color(0xFF13285C), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFB4C2E3),
    heroChip = Color(0xFF1B3573),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF04261A), accentOnSurface = Color(0xFF11D56B),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0A1A3F),
)

// В светлом режиме фирменные панели остаются тёмно-синими — это узнаваемая часть темы.
// Яркий зелёный на белом — 1.9:1, поэтому для линий и чисел на surface берётся тёмно-зелёный.
internal val ZachetLightBrand = BrandColors(
    hero = Color(0xFF0A1A3F), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFB4C2E3),
    heroChip = Color(0xFF1B3573),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF04261A), accentOnSurface = Color(0xFF0B6B37),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0A1A3F),
)

internal val ZachetPalette = ThemePalette(
    light = ZachetLight,
    dark = ZachetDark,
    lightStatus = ZachetLightStatus,
    darkStatus = ZachetDarkStatus,
    lightBrand = ZachetLightBrand,
    darkBrand = ZachetDarkBrand,
)

/** «Серия дней»: значок огня (декоративный) и цвет текста на тёмной и на светлой поверхности. */
internal val ZachetStreakIcon = Color(0xFFFF9F43)
internal val ZachetStreakTextDark = Color(0xFFFFB86B)
internal val ZachetStreakTextLight = Color(0xFF8A4B00)

/** Цвет текста серии под текущую поверхность; в «Высоком контрасте» — предупреждающий текст общей палитры (≥ 7:1). */
@Composable
@ReadOnlyComposable
internal fun zachetStreakTextColor(): Color {
    val status = AppTheme.status
    return when {
        status == LightHighContrastStatus || status == DarkHighContrastStatus -> status.warningText
        MaterialTheme.colorScheme.surface.luminance() < 0.5f -> ZachetStreakTextDark
        else -> ZachetStreakTextLight
    }
}

@Composable
internal fun zachetTokens(): DesignTokens {
    val rubik = variableFont(Res.font.rubik, listOf(400, 500, 600, 700, 800))
    return DesignTokens(
        theme = DesignTheme.ZACHET,
        displayFont = rubik,
        bodyFont = rubik,
        buttonShape = Shapes.rounded(18.dp),
        buttonHeight = 58.dp,
        buttonDepth = 5.dp,
        buttonLabelUppercase = true,
        cardShape = Shapes.rounded(18.dp),
        cardBorder = false,
        cardDepth = 4.dp,
        chipShape = Shapes.rounded(10.dp),
        fieldShape = Shapes.rounded(14.dp),
        tileShape = Shapes.rounded(14.dp),
        iconButtonsOnSurface = true,
        iconButtonShape = RoundedCornerShape(14.dp),
        topBarOnBackground = true,
        largeTopLevelTitle = false,
        sectionTitle = SectionTitleStyle.BOLD,
        navBar = NavBarStyle.FLOATING_ICONS,
        answerStyle = AnswerStyle.CARDS,
        selectedAnswerUsesAccent = true,
    )
}
