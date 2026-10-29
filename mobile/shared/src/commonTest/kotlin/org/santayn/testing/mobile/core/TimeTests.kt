package org.santayn.testing.mobile.core

import kotlinx.datetime.LocalDate
import org.santayn.testing.mobile.core.util.formatDayRu
import kotlin.test.Test
import kotlin.test.assertEquals

/** Дата на главной «Кафедры»: день недели и месяц в родительном падеже. */
class TimeTests {

    @Test
    fun weekdayAndMonthInGenitive() {
        assertEquals("Пятница, 10 октября", formatDayRu(LocalDate(2025, 10, 10)))
    }

    @Test
    fun mondayAndSunday() {
        assertEquals("Понедельник, 5 января", formatDayRu(LocalDate(2026, 1, 5)))
        assertEquals("Воскресенье, 11 января", formatDayRu(LocalDate(2026, 1, 11)))
    }

    @Test
    fun monthsWhoseGenitiveDiffersFromNominative() {
        assertEquals("Пятница, 1 мая", formatDayRu(LocalDate(2026, 5, 1)))
        assertEquals("Суббота, 1 августа", formatDayRu(LocalDate(2026, 8, 1)))
        assertEquals("Четверг, 31 декабря", formatDayRu(LocalDate(2026, 12, 31)))
    }
}
