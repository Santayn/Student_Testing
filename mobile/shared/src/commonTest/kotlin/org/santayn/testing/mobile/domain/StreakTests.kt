package org.santayn.testing.mobile.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.santayn.testing.mobile.feature.home.streakDays
import org.santayn.testing.mobile.feature.home.streakLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/** Серия дней на главной «Зачёта»: дни подряд с хотя бы одной завершённой попыткой. */
class StreakTests {

    private val moscow: TimeZone = UtcOffset(hours = 3).asTimeZone()
    private val today = LocalDate(2026, 10, 11)

    /** Момент по местному (UTC+3) времени. */
    private fun at(date: String, time: String = "12:00:00") = Instant.parse("${date}T$time+03:00")

    private fun streak(vararg completedAt: Instant) = streakDays(completedAt.toList(), today, moscow)

    @Test
    fun noAttemptsMeansNoStreak() {
        assertEquals(0, streak())
    }

    @Test
    fun onlyTodayIsOneDay() {
        assertEquals(1, streak(at("2026-10-11")))
    }

    @Test
    fun threeDaysInARow() {
        assertEquals(3, streak(at("2026-10-11"), at("2026-10-10"), at("2026-10-09")))
    }

    @Test
    fun streakIsAliveWhenLastAttemptWasYesterday() {
        assertEquals(2, streak(at("2026-10-10"), at("2026-10-09")))
    }

    @Test
    fun gapCutsTheStreakToItsTail() {
        // 8 октября пропущено: считаются только 11, 10 и 9.
        assertEquals(3, streak(at("2026-10-11"), at("2026-10-10"), at("2026-10-09"), at("2026-10-07"), at("2026-10-06")))
    }

    @Test
    fun lastAttemptTwoDaysAgoMeansNoStreak() {
        assertEquals(0, streak(at("2026-10-09"), at("2026-10-08")))
    }

    @Test
    fun twoAttemptsOnOneDayCountOnce() {
        assertEquals(1, streak(at("2026-10-11", "09:00:00"), at("2026-10-11", "18:30:00")))
    }

    @Test
    fun attemptsAroundLocalMidnightAreDifferentDays() {
        // 23:30 и 00:30 по местному времени — это 20:30 и 21:30 одного дня по UTC.
        assertEquals(2, streak(at("2026-10-10", "23:30:00"), at("2026-10-11", "00:30:00")))
    }

    @Test
    fun labelUsesRussianPlurals() {
        assertEquals("1 день подряд", streakLabel(1))
        assertEquals("2 дня подряд", streakLabel(2))
        assertEquals("5 дней подряд", streakLabel(5))
        assertEquals("11 дней подряд", streakLabel(11))
        assertEquals("21 день подряд", streakLabel(21))
    }
}
