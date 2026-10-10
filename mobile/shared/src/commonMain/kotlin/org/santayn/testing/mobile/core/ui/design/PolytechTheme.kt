package org.santayn.testing.mobile.core.ui.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import org.santayn.testing.mobile.core.ui.StatusColors
import org.santayn.testing.mobile.resources.Res
import org.santayn.testing.mobile.resources.onest
import org.santayn.testing.mobile.resources.unbounded

/*
 * Тема A «Политех» — фирменный стиль сайта kubstu.ru (бренд «Кубанский Политех», 2026):
 * электрический синий #2E54DC, ярко-зелёный #11D56B, небесный #9BD5FF, светлый фон #F1F9FF,
 * крупные скругления, кнопки-«пилюли», плитки-бенто. Шрифты: Unbounded (заголовки) + Onest (текст).
 * Макеты: recommendations/mobile-design-themes/mockups/varianty-dizaina.html, ряд A.
 *
 * Контраст всех пар текст/фон проверяет ContrastTests (≥ 4.5:1).
 */

internal val PolytechLight = lightColorScheme(
    primary = Color(0xFF2E54DC),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE3EAFF),
    onPrimaryContainer = Color(0xFF1D3CA8),
    secondary = Color(0xFF045BA7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCEBFA),
    onSecondaryContainer = Color(0xFF03457F),
    tertiary = Color(0xFF0B6B37),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDDF8E8),
    onTertiaryContainer = Color(0xFF0B6B37),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE1DC),
    onErrorContainer = Color(0xFF9B1C0C),
    background = Color(0xFFF1F9FF),
    onBackground = Color(0xFF0B1630),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1630),
    surfaceVariant = Color(0xFFE8F0FA),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF6FAFF),
    surfaceContainerHigh = Color(0xFFEDF4FD),
    surfaceContainerHighest = Color(0xFFE3ECF8),
    outline = Color(0xFF8E9BB8),
    outlineVariant = Color(0xFFD5E0F2),
    inverseSurface = Color(0xFF0B1630),
    inverseOnSurface = Color(0xFFF1F9FF),
    inversePrimary = Color(0xFF9DB2FF),
    scrim = Color(0xFF000000),
)

internal val PolytechDark = darkColorScheme(
    primary = Color(0xFF8EA7FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF1E2E66),
    onPrimaryContainer = Color(0xFFD6DFFF),
    secondary = Color(0xFF7FB8F0),
    onSecondary = Color(0xFF0B1020),
    secondaryContainer = Color(0xFF16304F),
    onSecondaryContainer = Color(0xFFCFE4FA),
    tertiary = Color(0xFF4BE38F),
    onTertiary = Color(0xFF06281A),
    tertiaryContainer = Color(0xFF0E3B2A),
    onTertiaryContainer = Color(0xFFA8F2C8),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A06),
    errorContainer = Color(0xFF3D1A1A),
    onErrorContainer = Color(0xFFFFCFC9),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE8ECF6),
    surface = Color(0xFF141B2E),
    onSurface = Color(0xFFE8ECF6),
    surfaceVariant = Color(0xFF1C2540),
    onSurfaceVariant = Color(0xFFAEB8CF),
    surfaceContainerLowest = Color(0xFF0B1020),
    surfaceContainerLow = Color(0xFF111829),
    surfaceContainer = Color(0xFF141B2E),
    surfaceContainerHigh = Color(0xFF1C2540),
    surfaceContainerHighest = Color(0xFF243052),
    outline = Color(0xFF5B6A8E),
    outlineVariant = Color(0xFF2C3756),
    inverseSurface = Color(0xFFE8ECF6),
    inverseOnSurface = Color(0xFF141B2E),
    inversePrimary = Color(0xFF2E54DC),
    scrim = Color(0xFF000000),
)

internal val PolytechLightStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFFDDF8E8), successText = Color(0xFF0B6B37),
    warning = Color(0xFFF59E0B), warningSoft = Color(0xFFFFF1D6), warningText = Color(0xFF8A4B00),
    danger = Color(0xFFE5484D), dangerSoft = Color(0xFFFFE1DC), dangerText = Color(0xFF9B1C0C),
    info = Color(0xFF1179D5), infoSoft = Color(0xFFE0F0FF), infoText = Color(0xFF045BA7),
    primarySoft = Color(0xFFE3EAFF), primarySoftText = Color(0xFF1D3CA8),
    shell = Color(0xFF2E54DC), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF4B5563),
)

internal val PolytechDarkStatus = StatusColors(
    success = Color(0xFF11D56B), successSoft = Color(0xFF0E3B2A), successText = Color(0xFF8CF0B8),
    warning = Color(0xFFF5B544), warningSoft = Color(0xFF3A2A0C), warningText = Color(0xFFFFD27A),
    danger = Color(0xFFFF8A80), dangerSoft = Color(0xFF3D1A1A), dangerText = Color(0xFFFFB4AB),
    info = Color(0xFF7FB8F0), infoSoft = Color(0xFF102A44), infoText = Color(0xFFA9D3FF),
    primarySoft = Color(0xFF1E2E66), primarySoftText = Color(0xFFC9D5FF),
    shell = Color(0xFF1D2A55), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFFAEB8CF),
)

internal val PolytechLightBrand = BrandColors(
    hero = Color(0xFF2E54DC), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFDCE4FA),
    heroChip = Color(0xFF2546BC),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF06281A),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0B1630),
)

internal val PolytechDarkBrand = BrandColors(
    hero = Color(0xFF1D2E6B), onHero = Color(0xFFFFFFFF), onHeroMuted = Color(0xFFC9D5FF),
    heroChip = Color(0xFF2A3D85),
    accent = Color(0xFF11D56B), onAccent = Color(0xFF06281A),
    highlight = Color(0xFF9BD5FF), onHighlight = Color(0xFF0B1630),
)

internal val PolytechPalette = ThemePalette(
    light = PolytechLight,
    dark = PolytechDark,
    lightStatus = PolytechLightStatus,
    darkStatus = PolytechDarkStatus,
    lightBrand = PolytechLightBrand,
    darkBrand = PolytechDarkBrand,
)

/** Вариативный шрифт из ресурсов: по начертанию на каждый вес. */
@Composable
internal fun variableFont(resource: org.jetbrains.compose.resources.FontResource, weights: List<Int>): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(
                resource = resource,
                weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
            )
        }
    )

@Composable
internal fun polytechTokens(): DesignTokens = DesignTokens(
    theme = DesignTheme.POLYTECH,
    displayFont = variableFont(Res.font.unbounded, listOf(500, 600, 700)),
    bodyFont = variableFont(Res.font.onest, listOf(400, 500, 600, 700)),
    buttonShape = Shapes.pill,
    buttonHeight = 52.dp,
    buttonDepth = 0.dp,
    cardShape = Shapes.rounded(20.dp),
    cardBorder = false,
    cardDepth = 0.dp,
    chipShape = Shapes.pill,
    fieldShape = Shapes.rounded(14.dp),
    tileShape = Shapes.rounded(12.dp),
    iconButtonsOnSurface = true,
    topBarOnBackground = true,
    sectionTitle = SectionTitleStyle.DISPLAY,
    navBar = NavBarStyle.FLOATING_PILL,
    answerStyle = AnswerStyle.CARDS,
)
