package org.santayn.testing.mobile.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.santayn.testing.mobile.core.network.parseContentDispositionFileName
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.storage.OfflineFilesLimit
import org.santayn.testing.mobile.data.model.MatchingPairDto
import org.santayn.testing.mobile.data.model.MatchingPromptDto
import org.santayn.testing.mobile.data.model.PublicOptionDto
import org.santayn.testing.mobile.data.model.PublicQuestionDto
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.ResultAttemptDto
import org.santayn.testing.mobile.data.model.ResultItemDto
import org.santayn.testing.mobile.data.model.ResultStatsDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchingTests {
    @Test
    fun comparableTextNormalizesCaseYoAndPunctuation() {
        assertEquals("еж и елка", comparableText("  Ёж, и ЁЛКА!  "))
    }

    @Test
    fun validationRequiresTwoCompleteUniquePairs() {
        assertEquals(
            "Для вопроса на соответствие нужны минимум две пары.",
            matchingPairsValidationMessage(listOf(MatchingPairDto(1, "a", "b"))),
        )
        assertEquals(
            "Заполните обе части каждой пары соответствия.",
            matchingPairsValidationMessage(listOf(MatchingPairDto(1, "a", "b"), MatchingPairDto(2, "c", ""))),
        )
        assertEquals(
            "Значения в правой колонке должны быть уникальными.",
            matchingPairsValidationMessage(listOf(MatchingPairDto(1, "a", "X"), MatchingPairDto(2, "c", "x"))),
        )
        assertNull(matchingPairsValidationMessage(listOf(MatchingPairDto(1, "a", "b"), MatchingPairDto(2, "c", "d"))))
    }

    @Test
    fun parseMatchingDisplayNeedsAtLeastTwoPairs() {
        assertTrue(parseMatchingDisplay("просто -> текст").isEmpty())
        val pairs = parseMatchingDisplay("Лекция -> Раздел | Тема -> -")
        assertEquals(2, pairs.size)
        assertEquals("Раздел", pairs[0].right)
        assertEquals("", pairs[1].right)
    }

    @Test
    fun matchingResultComparesNormalizedRightParts() {
        val result = matchingResultPairs("A -> один | B -> Два", "A -> Один | B -> три")
        assertTrue(result[0].matches)
        assertFalse(result[1].matches)
    }
}

class AttemptAnswersTests {
    private val matching = PublicQuestionDto(
        id = 3,
        type = QuestionType.MATCHING,
        matchingPrompts = listOf(MatchingPromptDto(1, "Лекция"), MatchingPromptDto(2, "Тема")),
        matchingOptions = listOf("Раздел", "Материал"),
    )
    private val single = PublicQuestionDto(id = 1, type = QuestionType.SINGLE, options = listOf(PublicOptionDto(10), PublicOptionDto(11)))
    private val multiple = PublicQuestionDto(id = 2, type = QuestionType.MULTIPLE, options = listOf(PublicOptionDto(20), PublicOptionDto(21)))
    private val text = PublicQuestionDto(id = 4, type = QuestionType.TEXT)

    @Test
    fun eachRightPartIsUsedByOnePromptOnly() {
        var answers = AttemptAnswers()
            .withMatching(3, promptOrdinal = 1, rightText = "Раздел")
            .withMatching(3, promptOrdinal = 2, rightText = "Раздел")
        assertNull(answers.matchingRightFor(3, 1), "старая привязка должна быть снята")
        assertEquals("Раздел", answers.matchingRightFor(3, 2))

        answers = answers.withMatching(3, promptOrdinal = 2, rightText = null)
        assertFalse(answers.isAnswered(matching))
    }

    @Test
    fun singleWithoutOptionsIsTreatedAsText() {
        val q = PublicQuestionDto(id = 9, type = QuestionType.SINGLE)
        assertEquals(QuestionType.TEXT, effectiveType(q))
    }

    @Test
    fun submissionKeepsParallelArraysAndMatchingJson() {
        val answers = AttemptAnswers()
            .withSingle(1, 11)
            .withMultiple(2, 20, true)
            .withMultiple(2, 21, true)
            .withMatching(3, 2, "Материал")
            .withText(4, "Central Processing Unit")
        val request = buildSubmission(listOf(single, multiple, matching, text), answers)

        assertEquals(listOf(1L, 2L, 3L, 4L), request.questionIds)
        assertEquals(listOf(listOf(11L), listOf(20L, 21L), emptyList(), emptyList()), request.selectedOptionIds)
        assertEquals("", request.answers[0])
        assertEquals("Central Processing Unit", request.answers[3])

        val pairs = Json.parseToJsonElement(request.answers[2]).jsonObject["pairs"]!!.jsonArray
        assertEquals(2, pairs.size, "по одной записи на каждую правую часть")
        val byRight = pairs.associateBy { it.jsonObject["right"]!!.jsonPrimitive.content }
        assertEquals(2, byRight["Материал"]!!.jsonObject["ordinal"]!!.jsonPrimitive.int)
        assertEquals("Тема", byRight["Материал"]!!.jsonObject["left"]!!.jsonPrimitive.content)
        assertEquals(0, byRight["Раздел"]!!.jsonObject["ordinal"]!!.jsonPrimitive.int)
    }

    @Test
    fun signatureChangesWhenQuestionsChange() {
        val a = questionSignature(listOf(single, matching))
        val b = questionSignature(listOf(single, matching.copy(matchingOptions = listOf("Материал", "Раздел"))))
        val c = questionSignature(listOf(single, text))
        assertEquals(a, b, "перемешивание вариантов не меняет сигнатуру")
        assertTrue(a != c)
    }
}

class ResultScoringTests {
    @Test
    fun explicitPointsAreSummed() {
        val attempt = ResultAttemptDto(
            attemptId = 1,
            results = listOf(
                ResultItemDto(correct = true, questionPoints = 2.0, awardedPoints = 2.0),
                ResultItemDto(correct = false, questionPoints = 2.0, awardedPoints = 1.0),
                ResultItemDto(correct = false, questionPoints = 1.0, awardedPoints = 0.0),
            ),
        )
        val summary = attempt.scoreSummary()
        assertEquals(3.0, summary.score)
        assertEquals(5.0, summary.maxScore)
        assertEquals(60.0, summary.percent)
        assertEquals(1, summary.correct)
        assertEquals(1, summary.partial)
        assertEquals(1, summary.incorrect)
    }

    @Test
    fun statsAreUsedWithoutPoints() {
        val attempt = ResultAttemptDto(attemptId = 1, stats = ResultStatsDto(total = 4, right = 3))
        assertEquals(75.0, attempt.scoreSummary().percent)
    }

    @Test
    fun bestAttemptPicksHighestPercent() {
        val low = ResultAttemptDto(attemptId = 1, attemptOrdinal = 1, stats = ResultStatsDto(4, 1))
        val high = ResultAttemptDto(attemptId = 2, attemptOrdinal = 2, stats = ResultStatsDto(4, 3))
        assertEquals(2, bestAttempt(listOf(low, high))?.attemptId)
    }
}

class MiscTests {
    @Test
    fun workspaceRolePriorityAndPreference() {
        val roles = listOf("STUDENT", "TEACHER", "USER")
        assertEquals(WorkspaceRole.TEACHER, WorkspaceRole.resolve(roles, preferred = null))
        assertEquals(WorkspaceRole.STUDENT, WorkspaceRole.resolve(roles, preferred = "STUDENT"))
        assertEquals(WorkspaceRole.TEACHER, WorkspaceRole.resolve(roles, preferred = "ADMIN"))
        assertNull(WorkspaceRole.resolve(listOf("USER"), preferred = null))
    }

    @Test
    fun offlineLimitInputAcceptsMegabytesAndGigabytes() {
        assertEquals(500, OfflineFilesLimit.parse("500"))
        assertEquals(500, OfflineFilesLimit.parse(" 500 МБ "))
        assertEquals(2048, OfflineFilesLimit.parse("2 гб"))
        assertEquals(1536, OfflineFilesLimit.parse("1,5 GB"))
        assertNull(OfflineFilesLimit.parse("много"))
        assertEquals(100, OfflineFilesLimit.clamp(5))
        assertEquals(10240, OfflineFilesLimit.clamp(50_000))
        assertEquals("200 МБ", OfflineFilesLimit.title(200))
        assertEquals("1.5 ГБ", OfflineFilesLimit.title(1536))
        assertEquals("10 ГБ", OfflineFilesLimit.title(10240))
    }

    @Test
    fun contentDispositionPrefersUtf8Name() {
        assertEquals(
            "Лекция 1.pdf",
            parseContentDispositionFileName("attachment; filename=\"lecture.pdf\"; filename*=UTF-8''%D0%9B%D0%B5%D0%BA%D1%86%D0%B8%D1%8F%201.pdf"),
        )
        assertEquals("backup.sql", parseContentDispositionFileName("attachment; filename=\"backup.sql\""))
        assertNull(parseContentDispositionFileName(null))
    }
}
