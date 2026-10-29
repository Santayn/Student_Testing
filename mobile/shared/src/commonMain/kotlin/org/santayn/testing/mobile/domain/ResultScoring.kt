package org.santayn.testing.mobile.domain

import org.santayn.testing.mobile.data.model.ResultAttemptDto
import org.santayn.testing.mobile.data.model.ResultItemDto
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/*
 * Порт frontend/src/utils/resultScoring.js.
 */

enum class GradingStatus(val label: String) {
    CORRECT("Верно"),
    PARTIAL("Частично верно"),
    INCORRECT("Неверно"),
}

private fun Double?.nonNegative(): Double? = this?.let { max(0.0, it) }
private fun round2(value: Double) = round(value * 100) / 100

fun ResultItemDto.gradingStatusNormalized(): GradingStatus {
    when (gradingStatus?.trim()?.lowercase()) {
        "correct" -> return GradingStatus.CORRECT
        "partial" -> return GradingStatus.PARTIAL
        "incorrect" -> return GradingStatus.INCORRECT
    }
    if (correct) return GradingStatus.CORRECT
    val maximum = questionPoints.nonNegative()
    val awarded = awardedPoints.nonNegative()
    if (maximum != null && maximum > 0 && awarded != null && awarded > 0) {
        return if (awarded >= maximum) GradingStatus.CORRECT else GradingStatus.PARTIAL
    }
    return GradingStatus.INCORRECT
}

data class ItemScore(val awarded: Double, val maximum: Double, val explicit: Boolean)

fun ResultItemDto.score(): ItemScore {
    val explicitMax = questionPoints.nonNegative()
    val maximum = if (explicitMax != null && explicitMax > 0) explicitMax else 1.0
    val explicitAwarded = awardedPoints.nonNegative()
    val awarded = explicitAwarded?.let { min(it, maximum) }
        ?: if (gradingStatusNormalized() == GradingStatus.CORRECT) maximum else 0.0
    return ItemScore(round2(awarded), round2(maximum), explicitMax != null || explicitAwarded != null)
}

data class AttemptScore(
    val score: Double,
    val maxScore: Double,
    val percent: Double,
    val correct: Int,
    val partial: Int,
    val incorrect: Int,
)

fun ResultAttemptDto.scoreSummary(): AttemptScore {
    val items = results.map { it.score() }
    val counts = results.groupingBy { it.gradingStatusNormalized() }.eachCount()
    val fromItems = if (items.any { it.explicit }) {
        val score = items.sumOf { it.awarded }
        val maxScore = items.sumOf { it.maximum }
        if (maxScore > 0) Triple(round2(score), round2(maxScore), round2(score * 100 / maxScore)) else null
    } else {
        null
    }
    val (score, maxScore, percent) = fromItems ?: run {
        val total = max(0, stats?.total ?: 0).toDouble()
        val right = min(max(0L, stats?.right ?: 0).toDouble(), total)
        val pct = stats?.percent?.let { min(100.0, max(0.0, it)) } ?: if (total > 0) right * 100 / total else 0.0
        Triple(round2(right), round2(total), round2(pct))
    }
    return AttemptScore(
        score = score,
        maxScore = maxScore,
        percent = percent,
        correct = counts[GradingStatus.CORRECT] ?: 0,
        partial = counts[GradingStatus.PARTIAL] ?: 0,
        incorrect = counts[GradingStatus.INCORRECT] ?: 0,
    )
}

/** Лучшая попытка студента по тесту (StudentBestAttemptScoring). */
fun bestAttempt(attempts: List<ResultAttemptDto>): ResultAttemptDto? =
    attempts.maxWithOrNull(
        compareBy<ResultAttemptDto>({ it.scoreSummary().percent }, { it.scoreSummary().score }, { -(it.attemptOrdinal) })
    )
