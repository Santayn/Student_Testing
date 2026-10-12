package org.santayn.testing.mobile.core.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.santayn.testing.mobile.core.ui.StatusColors
import org.santayn.testing.mobile.resources.Res
import org.santayn.testing.mobile.resources.golos_text

/*
 * Тема B «Кафедра» — спокойный академический стиль: серый фон #F4F6F9, белые карточки с тонкой рамкой,
 * сгруппированные списки, мелкие прописные заголовки разделов, плоская панель вкладок.
 * Цвет — только у действий и состояний. Шрифт: Golos Text (один на всё).
 * Макеты: recommendations/mobile-design-themes/mockups/varianty-dizaina.html, ряд B.
 *
 * Контраст всех пар текст/фон проверяет ContrastTests (≥ 4.5:1).
 */

internal val KafedraLight = lightColorScheme(
    primary = Color(0xFF045BA7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F0FA),
    onPrimaryContainer = Color(0xFF03457F),
    secondary = Color(0xFF0E8A6A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE7F5F0),
    onSecondaryContainer = Color(0xFF0B6B4F),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDF3E7),
    onTertiaryContainer = Color(0xFF8A4106),
    error = Color(0xFFB42318),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDEDE7),
    onErrorContainer = Color(0xFFA3330A),
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0E1F3D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1F3D),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF5B6678),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8F9FB),
    surfaceContainerHigh = Color(0xFFEEF1F6),
    surfaceContainerHighest = Color(0xFFE3E8EF),
    outline = Color(0xFF7A8496),          // границы полей ввода: ≥ 3:1 к белому
    outlineVariant = Color(0xFFE3E8EF),   // тонкие разделители и рамки карточек (декор)
    inverseSurface = Color(0xFF0E1F3D),
    inverseOnSurface = Color(0xFFF4F6F9),
    inversePrimary = Color(0xFF7DB3EE),
    scrim = Color(0xFF000000),
)

internal val KafedraDark = darkColorScheme(
    primary = Color(0xFF7DB3EE),
    onPrimary = Color(0xFF0B1420),
    primaryContainer = Color(0xFF12304F),
    onPrimaryContainer = Color(0xFFCFE4FA),
    secondary = Color(0xFF3FBF97),
    onSecondary = Color(0xFF07201A),
    secondaryContainer = Color(0xFF10291F),
    onSecondaryContainer = Color(0xFF8EE0C3),
    tertiary = Color(0xFFE8A25C),
    onTertiary = Color(0xFF2E1F0E),
    tertiaryContainer = Color(0xFF2E1F0E),
    onTertiaryContainer = Color(0xFFF5C792),
    error = Color(0xFFF2A08F),
    onError = Color(0xFF2A0E08),
    errorContainer = Color(0xFF33170E),
    onErrorContainer = Color(0xFFF7B9A3),
    background = Color(0xFF0E1420),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF161D2B),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1E2636),
    onSurfaceVariant = Color(0xFFA3ADBF),
    surfaceContainerLowest = Color(0xFF0E1420),
    surfaceContainerLow = Color(0xFF121826),
    surfaceContainer = Color(0xFF161D2B),
    surfaceContainerHigh = Color(0xFF1E2636),
    surfaceContainerHighest = Color(0xFF262F42),
    outline = Color(0xFF6B7689),
    outlineVariant = Color(0xFF2A3344),
    inverseSurface = Color(0xFFE6EAF2),
    inverseOnSurface = Color(0xFF161D2B),
    inversePrimary = Color(0xFF045BA7),
    scrim = Color(0xFF000000),
)

internal val KafedraLightStatus = StatusColors(
    success = Color(0xFF0E8A6A), successSoft = Color(0xFFE7F5F0), successText = Color(0xFF0B6B4F),
    warning = Color(0xFFB45309), warningSoft = Color(0xFFFDF3E7), warningText = Color(0xFF8A4106),
    danger = Color(0xFFC2410C), dangerSoft = Color(0xFFFDEDE7), dangerText = Color(0xFFA3330A),
    info = Color(0xFF045BA7), infoSoft = Color(0xFFE6F0FA), infoText = Color(0xFF03457F),
    primarySoft = Color(0xFFE6F0FA), primarySoftText = Color(0xFF03457F),
    shell = Color(0xFF045BA7), onShell = Color(0xFFFFFFFF),
    textMuted = Color(0xFF5B6678),
)

internal val KafedraDarkStatus = StatusColors(
    success = Color(0xFF3FBF97), successSoft = Color(0xFF10291F), successText = Color(0xFF8EE0C3),
    warning = Color(0xFFE8A25C), warningSoft = Color(0xFF2E1F0E), warningText = Color(0xFFF5C792),
    danger = Color(0xFFF08A67), dangerSoft = Color(0xFF33170E), dangerText = Color(0xFFF7B9A3),
    info = Color(0xFF7DB3EE), infoSoft = Color(0xFF10243A), infoText = Color(0xFFA9D0F5),
    primarySoft = Color(0xFF12304F), primarySoftText = Color(0xFFCFE4FA),
    shell = Color(0xFF161D2B), onShell = Color(0xFFE6EAF2),
    textMuted = Color(0xFFA3ADBF),
)

// В «Кафедре» нет цветных панелей: «hero» — обычная белая (тёмная) поверхность,
// акцент совпадает с primary. Значения нужны, т.к. BrandColors читают общие компоненты
// (BusyButton(accent = true), чипы итога и т.п.).
internal val KafedraLightBrand = BrandColors(
    hero = Color(0xFFFFFFFF), onHero = Color(0xFF0E1F3D), onHeroMuted = Color(0xFF5B6678),
    heroChip = Color(0xFFEEF1F6),
    accent = Color(0xFF045BA7), onAccent = Color(0xFFFFFFFF),
    highlight = Color(0xFFE6F0FA), onHighlight = Color(0xFF03457F),
)

internal val KafedraDarkBrand = BrandColors(
    hero = Color(0xFF161D2B), onHero = Color(0xFFE6EAF2), onHeroMuted = Color(0xFFA3ADBF),
    heroChip = Color(0xFF1E2636),
    accent = Color(0xFF7DB3EE), onAccent = Color(0xFF0B1420),
    highlight = Color(0xFF12304F), onHighlight = Color(0xFFCFE4FA),
)

internal val KafedraPalette = ThemePalette(
    light = KafedraLight,
    dark = KafedraDark,
    lightStatus = KafedraLightStatus,
    darkStatus = KafedraDarkStatus,
    lightBrand = KafedraLightBrand,
    darkBrand = KafedraDarkBrand,
)

/** Цвета полосок предметов на главной (по subject.id % 5): декоративные, ≥ 3:1 к поверхности карточки. */
internal val KafedraSubjectAccentsLight = listOf(
    Color(0xFF045BA7), Color(0xFF0E8A6A), Color(0xFFB45309), Color(0xFF6D28D9), Color(0xFFBE185D),
)

internal val KafedraSubjectAccentsDark = listOf(
    Color(0xFF7DB3EE), Color(0xFF3FBF97), Color(0xFFE8A25C), Color(0xFFB79BF5), Color(0xFFF08FBF),
)

/** Полоски предметов под текущую поверхность: на тёмной (в т. ч. высококонтрастной) — светлые. */
@Composable
@ReadOnlyComposable
internal fun kafedraSubjectAccents(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) KafedraSubjectAccentsDark else KafedraSubjectAccentsLight

/** Явные стили фирменных экранов «Кафедры» (макет, ряд B); семейство шрифта — из темы. */
internal object KafedraType {
    /** Крупный заголовок экрана: «Сегодня», «Тест завершён». */
    val largeTitle: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em,
        )

    val lectureTitle: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em,
        )

    val lectureBody: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 27.sp)

    /** Надзаголовок прописными с разрядкой (текст приводится к прописным в месте вызова). */
    val overline: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.labelLarge.copy(
            fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em,
        )

    val rowTitle: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
        )

    val rowCaption: TextStyle
        @Composable @ReadOnlyComposable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
internal fun kafedraTokens(): DesignTokens {
    val golos = variableFont(Res.font.golos_text, listOf(400, 500, 600, 700))
    return DesignTokens(
        theme = DesignTheme.KAFEDRA,
        displayFont = golos,
        bodyFont = golos,
        buttonShape = Shapes.rounded(12.dp),
        buttonHeight = 52.dp,
        buttonDepth = 0.dp,
        cardShape = Shapes.rounded(14.dp),
        cardBorder = true,                 // белая карточка на #F4F6F9 + рамка outlineVariant
        cardDepth = 0.dp,
        chipShape = Shapes.rounded(8.dp),
        fieldShape = Shapes.rounded(12.dp),
        tileShape = Shapes.rounded(10.dp),
        iconButtonsOnSurface = false,      // кнопки-иконки без подложки
        topBarOnBackground = true,
        largeTopLevelTitle = true,
        sectionTitle = SectionTitleStyle.CAPS,
        navBar = NavBarStyle.TAB_BAR,
        answerStyle = AnswerStyle.LIST,
    )
}
