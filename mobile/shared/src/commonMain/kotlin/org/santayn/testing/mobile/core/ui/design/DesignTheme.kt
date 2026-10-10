package org.santayn.testing.mobile.core.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.core.ui.StatusColors

/*
 * Темы оформления приложения.
 *
 * Тема — это не только цвета: шрифты, форма кнопок и карточек, вид нижней навигации и заголовков
 * разделов, плюс отдельные компоновки нескольких «фирменных» экранов (главная, шапка лекции,
 * итог теста). Всё остальное перекрашивается через общие компоненты core/ui/Components.kt,
 * которые читают [LocalDesign].
 *
 * Светлая/тёмная схема, высокий контраст и размер текста — отдельные настройки, они работают
 * поверх любой темы.
 *
 * Как добавить тему: recommendations/mobile-design-themes/README.md.
 */

/** Темы оформления. Порядок = порядок в настройках. */
enum class DesignTheme(val title: String, val description: String) {
    POLYTECH(
        title = "Политех",
        description = "Фирменный стиль КубГТУ: синий, крупные заголовки, плитки",
    ),
    ;

    companion object {
        val DEFAULT = POLYTECH

        fun fromName(name: String?): DesignTheme = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Вид заголовков разделов (SectionTitle). */
enum class SectionTitleStyle {
    /** Акцидентный шрифт темы, цвет основного текста (A «Политех»). */
    DISPLAY,

    /** Мелкие прописные с разрядкой, приглушённый цвет (B «Кафедра»). */
    CAPS,

    /** Жирный основной шрифт крупнее обычного (C «Зачёт»). */
    BOLD,
}

/** Вид вопроса теста и вариантов ответа. */
enum class AnswerStyle {
    /**
     * Вопрос крупным заголовком без подложки, каждый вариант — отдельная карточка;
     * выбранная обведена цветом primary (A «Политех»; с cardDepth — «объёмные» плитки C).
     */
    CARDS,

    /** Вопрос и варианты в одной карточке, варианты — строки списка (B «Кафедра»). */
    LIST,
}

/** Вид нижней навигации главных вкладок. */
enum class NavBarStyle {
    /** Плавающая «пилюля»: активная вкладка — цветная капсула с иконкой и подписью (A). */
    FLOATING_PILL,

    /** Классическая панель Material на всю ширину с подписями (B). */
    CLASSIC,
}

/**
 * Фирменные цвета темы сверх Material ColorScheme.
 *
 * - hero — фон фирменных панелей (шапка главной, итог теста), текст на нём — onHero / onHeroMuted;
 * - accent — второй акцент для главных действий («Пройти тест») и плиток;
 * - highlight — третий цвет плиток и выделений.
 */
@Immutable
data class BrandColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    /** Полупрозрачные элементы на hero (чипы, кнопки-иконки): фон и текст. */
    val heroChip: Color,
    val accent: Color,
    val onAccent: Color,
    val highlight: Color,
    val onHighlight: Color,
)

/**
 * Токены темы: шрифты, формы, размеры и стиль компонентов.
 * Цвета живут в MaterialTheme.colorScheme, AppTheme.status и [BrandColors].
 */
@Immutable
data class DesignTokens(
    val theme: DesignTheme,
    /** Заголовки экранов и крупные числа. */
    val displayFont: FontFamily,
    /** Основной текст, подписи, кнопки. */
    val bodyFont: FontFamily,
    val buttonShape: Shape,
    val buttonHeight: Dp,
    /** «Объёмная» кнопка: нижний край высотой depth темнее кнопки (0 — плоская). */
    val buttonDepth: Dp,
    val cardShape: Shape,
    /** Тонкая рамка вокруг карточек (нужна, когда карточка почти не отличается от фона). */
    val cardBorder: Boolean,
    /** «Объёмная» карточка: нижний край толщиной depth (0 — без него). */
    val cardDepth: Dp,
    val chipShape: Shape,
    val fieldShape: Shape,
    /** Иконка-плитка в карточках-ссылках, аватар-заглушки. */
    val tileShape: Shape,
    /** Кнопки-иконки в верхней панели в круглой подложке цвета surface. */
    val iconButtonsOnSurface: Boolean,
    /** Верхняя панель цвета фона экрана (иначе — цвета surface). */
    val topBarOnBackground: Boolean,
    val sectionTitle: SectionTitleStyle,
    val navBar: NavBarStyle,
    val answerStyle: AnswerStyle,
)

val LocalDesign = staticCompositionLocalOf<DesignTokens> { error("DesignTokens не заданы: оберните в StudentTestingTheme") }
val LocalBrandColors = staticCompositionLocalOf<BrandColors> { error("BrandColors не заданы: оберните в StudentTestingTheme") }

object Design {
    val tokens: DesignTokens
        @Composable @ReadOnlyComposable get() = LocalDesign.current

    val brand: BrandColors
        @Composable @ReadOnlyComposable get() = LocalBrandColors.current
}

/** Палитра темы: светлая и тёмная версии Material-схемы, статусных и фирменных цветов. */
@Immutable
data class ThemePalette(
    val light: ColorScheme,
    val dark: ColorScheme,
    val lightStatus: StatusColors,
    val darkStatus: StatusColors,
    val lightBrand: BrandColors,
    val darkBrand: BrandColors,
)

/** Формы, общие для «мягких» тем; отдельная тема может задать свои. */
internal object Shapes {
    val pill = RoundedCornerShape(percent = 50)
    fun rounded(radius: Dp) = RoundedCornerShape(radius)
    val none = RoundedCornerShape(0.dp)
}
