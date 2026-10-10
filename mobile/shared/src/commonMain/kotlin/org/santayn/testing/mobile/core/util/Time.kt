package org.santayn.testing.mobile.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round
import kotlin.time.Clock
import kotlin.time.Instant

fun parseInstantOrNull(value: String?): Instant? =
    value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun nowInstant(): Instant = Clock.System.now()

private fun Int.pad2() = toString().padStart(2, '0')

/** «дд.мм.гггг чч:мм» в часовом поясе устройства. */
fun formatDateTime(iso: String?): String {
    val instant = parseInstantOrNull(iso) ?: return iso.orEmpty()
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.day.pad2()}.${local.month.ordinal.plus(1).pad2()}.${local.year} " +
        "${local.hour.pad2()}:${local.minute.pad2()}"
}

/** «дд.мм.гггг» из YYYY-MM-DD. */
fun formatDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return ""
    val parts = isoDate.take(10).split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else isoDate
}

/** «дд.мм.гггг» → «гггг-мм-дд» или null при неверном формате. */
fun parseRuDate(value: String): String? {
    val parts = value.trim().split('.')
    if (parts.size != 3) return null
    val (d, m, y) = parts.map { it.toIntOrNull() ?: return null }
    if (d !in 1..31 || m !in 1..12 || y !in 1900..2100) return null
    return "${y.toString().padStart(4, '0')}-${m.pad2()}-${d.pad2()}"
}

/** Длительность теста из LocalTime «HH:mm[:ss]» → «30 мин», «1 ч 15 мин». */
fun formatDuration(value: String?): String? {
    val parts = value?.split(':')?.mapNotNull { it.toIntOrNull() } ?: return null
    if (parts.size < 2) return null
    val hours = parts[0]
    val minutes = parts[1]
    if (hours == 0 && minutes == 0) return null
    return listOfNotNull(
        if (hours > 0) "$hours ч" else null,
        if (minutes > 0) "$minutes мин" else null,
    ).joinToString(" ")
}

/** Число без лишних нулей (баллы, проценты). */
fun formatNumber(value: Double?): String {
    if (value == null) return "—"
    val rounded = round(value * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
