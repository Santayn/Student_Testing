package org.santayn.testing.mobile.core

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.santayn.testing.mobile.core.ui.StatusColors
import org.santayn.testing.mobile.core.ui.colorsFor
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Контраст текста по WCAG 2.1 (ГОСТ Р 52872-2019):
 * обычные темы — не ниже 4.5:1 (AA), «Высокий контраст» — не ниже 7:1 (AAA).
 */
class ContrastTests {

    private fun ratio(a: Color, b: Color): Double {
        val l1 = a.luminance().toDouble()
        val l2 = b.luminance().toDouble()
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    /** Пары «текст / фон», которые реально встречаются в интерфейсе. */
    private fun textPairs(c: ColorScheme, s: StatusColors): Map<String, Pair<Color, Color>> = mapOf(
        "onSurface/surface" to (c.onSurface to c.surface),
        "onBackground/background" to (c.onBackground to c.background),
        "onSurfaceVariant/surface" to (c.onSurfaceVariant to c.surface),
        "onSurfaceVariant/background" to (c.onSurfaceVariant to c.background),
        "onSurfaceVariant/surfaceVariant" to (c.onSurfaceVariant to c.surfaceVariant),
        "onSurfaceVariant/surfaceContainerHighest" to (c.onSurfaceVariant to c.surfaceContainerHighest),
        "primary/surface (текстовые кнопки)" to (c.primary to c.surface),
        "onPrimary/primary (кнопки)" to (c.onPrimary to c.primary),
        "error/surface" to (c.error to c.surface),
        "onError/error" to (c.onError to c.error),
        "successText/successSoft" to (s.successText to s.successSoft),
        "warningText/warningSoft" to (s.warningText to s.warningSoft),
        "dangerText/dangerSoft" to (s.dangerText to s.dangerSoft),
        "infoText/infoSoft" to (s.infoText to s.infoSoft),
        "primarySoftText/primarySoft" to (s.primarySoftText to s.primarySoft),
    )

    private fun check(dark: Boolean, highContrast: Boolean, minimum: Double) {
        val (scheme, status) = colorsFor(dark, highContrast)
        val failures = textPairs(scheme, status)
            .mapValues { (_, pair) -> ratio(pair.first, pair.second) }
            .filterValues { it < minimum }
        val theme = (if (dark) "тёмная" else "светлая") + if (highContrast) ", высокий контраст" else ""
        assertTrue(
            failures.isEmpty(),
            "Тема «$theme»: контраст ниже $minimum:1 — " +
                failures.entries.joinToString { "${it.key} = ${(it.value * 100).toInt() / 100.0}" },
        )
    }

    @Test fun lightThemeMeetsAA() = check(dark = false, highContrast = false, minimum = 4.5)
    @Test fun darkThemeMeetsAA() = check(dark = true, highContrast = false, minimum = 4.5)
    @Test fun lightHighContrastMeetsAAA() = check(dark = false, highContrast = true, minimum = 7.0)
    @Test fun darkHighContrastMeetsAAA() = check(dark = true, highContrast = true, minimum = 7.0)
}
