package org.santayn.testing.mobile.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.santayn.testing.mobile.core.ui.ResolvedColors
import org.santayn.testing.mobile.core.ui.colorsFor
import org.santayn.testing.mobile.core.ui.design.DesignTheme
import org.santayn.testing.mobile.core.ui.design.KafedraSubjectAccentsDark
import org.santayn.testing.mobile.core.ui.design.KafedraSubjectAccentsLight
import org.santayn.testing.mobile.core.ui.design.ZachetStreakTextDark
import org.santayn.testing.mobile.core.ui.design.ZachetStreakTextLight
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Контраст текста по WCAG 2.1 (ГОСТ Р 52872-2019) для КАЖДОЙ темы оформления:
 * обычные палитры — не ниже 4.5:1 (AA), «Высокий контраст» — не ниже 7:1 (AAA).
 * Новая тема в DesignTheme проверяется автоматически.
 */
class ContrastTests {

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    /** Пары «текст / фон», которые реально встречаются в интерфейсе. */
    private fun textPairs(colors: ResolvedColors): Map<String, Pair<Color, Color>> {
        val c = colors.scheme
        val s = colors.status
        val b = colors.brand
        return mapOf(
            "onSurface/surface" to (c.onSurface to c.surface),
            "onBackground/background" to (c.onBackground to c.background),
            "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
            "onSurfaceVariant/background" to (c.onSurfaceVariant to c.background),
            "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
            "onSurfaceVariant/surfaceContainerHigh" to (c.onSurfaceVariant to c.surfaceContainerHigh),
            "onSurfaceVariant/surfaceContainerHighest" to (c.onSurfaceVariant to c.surfaceContainerHighest),
            "primary/surface (текстовые кнопки)" to (c.primary to c.surface),
            "primary/background" to (c.primary to c.background),
            "onPrimary/primary (кнопки)" to (c.onPrimary to c.primary),
            "error/surface" to (c.error to c.surface),
            "onError/error" to (c.onError to c.error),
            "successText/successSoft" to (s.successText to s.successSoft),
            "successText/surface" to (s.successText to c.surface),
            "warningText/warningSoft" to (s.warningText to s.warningSoft),
            "dangerText/dangerSoft" to (s.dangerText to s.dangerSoft),
            "dangerText/surface" to (s.dangerText to c.surface),
            "infoText/infoSoft" to (s.infoText to s.infoSoft),
            "primarySoftText/primarySoft" to (s.primarySoftText to s.primarySoft),
            "primarySoftText/surface" to (s.primarySoftText to c.surface),
            "onHero/hero (фирменная панель)" to (b.onHero to b.hero),
            "onHeroMuted/hero" to (b.onHeroMuted to b.hero),
            "onHero/heroChip (чипы на панели)" to (b.onHero to b.heroChip),
            "onAccent/accent (акцентная кнопка, плитка)" to (b.onAccent to b.accent),
            "onHighlight/highlight (плитка)" to (b.onHighlight to b.highlight),
            "accentOnSurface/surface (число в плитке итога, обводка выбранного ответа)" to (b.accentOnSurface to c.surface),
            "surface/accentOnSurface (галочка выбранного ответа)" to (c.surface to b.accentOnSurface),
        )
    }

    private fun check(theme: DesignTheme, dark: Boolean, highContrast: Boolean, minimum: Double): List<String> {
        val colors = colorsFor(theme, dark, highContrast)
        val mode = (if (dark) "тёмная" else "светлая") + if (highContrast) ", высокий контраст" else ""
        return textPairs(colors)
            .mapValues { (_, pair) -> ratio(pair.first, pair.second) }
            .filterValues { it < minimum }
            .map { (name, value) -> "${theme.title} ($mode): $name = ${(value * 100).toInt() / 100.0} < $minimum" }
    }

    @Test
    fun everyThemeMeetsAA() {
        val failures = DesignTheme.entries.flatMap { theme ->
            check(theme, dark = false, highContrast = false, minimum = 4.5) +
                check(theme, dark = true, highContrast = false, minimum = 4.5)
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test
    fun highContrastMeetsAAA() {
        val failures = DesignTheme.entries.flatMap { theme ->
            check(theme, dark = false, highContrast = true, minimum = 7.0) +
                check(theme, dark = true, highContrast = true, minimum = 7.0)
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Текст серии дней на главной «Зачёта» — на чипе цвета surface, в обоих режимах. */
    @Test
    fun zachetStreakTextIsReadable() {
        val failures = listOf(
            "тёмная" to (ZachetStreakTextDark to colorsFor(DesignTheme.ZACHET, dark = true, highContrast = false)),
            "светлая" to (ZachetStreakTextLight to colorsFor(DesignTheme.ZACHET, dark = false, highContrast = false)),
        ).mapNotNull { (mode, pair) ->
            val (text, colors) = pair
            val value = ratio(text, colors.scheme.surface)
            if (value < 4.5) "Зачёт ($mode): текст серии = ${(value * 100).toInt() / 100.0} < 4.5" else null
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Полоски предметов на главной «Кафедры» — графика: не ниже 3:1 к поверхности карточки. */
    @Test
    fun kafedraSubjectAccentsAreVisible() {
        val failures = listOf(
            "светлая" to (KafedraSubjectAccentsLight to colorsFor(DesignTheme.KAFEDRA, dark = false, highContrast = false)),
            "тёмная" to (KafedraSubjectAccentsDark to colorsFor(DesignTheme.KAFEDRA, dark = true, highContrast = false)),
            "светлая, высокий контраст" to (KafedraSubjectAccentsLight to colorsFor(DesignTheme.KAFEDRA, dark = false, highContrast = true)),
            "тёмная, высокий контраст" to (KafedraSubjectAccentsDark to colorsFor(DesignTheme.KAFEDRA, dark = true, highContrast = true)),
        ).flatMap { (mode, pair) ->
            val (accents, colors) = pair
            accents.mapIndexedNotNull { index, accent ->
                val value = ratio(accent, colors.scheme.surface)
                if (value < 3.0) "Кафедра ($mode): полоска $index = ${(value * 100).toInt() / 100.0} < 3.0" else null
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
