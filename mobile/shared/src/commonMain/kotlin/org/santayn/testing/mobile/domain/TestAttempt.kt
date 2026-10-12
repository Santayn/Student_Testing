package org.santayn.testing.mobile.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.data.model.MatchingPairDto
import org.santayn.testing.mobile.data.model.PublicQuestionDto
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.SubmitAttemptRequest
import org.santayn.testing.mobile.data.model.SubmitAttemptResponse

// region Matching (utils/matchingPairs.js)

private val nonLetterOrDigit = Regex("[^\\p{L}\\p{N}]+")
private val spaces = Regex("\\s+")

/**
 * Нормализация для сравнения, как на бэкенде (QuestionTypeSupport) и во фронте:
 * нижний регистр, ё→е, всё кроме букв/цифр — пробел.
 * (NFKC-нормализация в common Kotlin недоступна; для обычного текста результат совпадает.)
 */
fun comparableText(value: String?): String =
    (value ?: "").trim().lowercase()
        .replace('ё', 'е')
        .replace(nonLetterOrDigit, " ")
        .replace(spaces, " ")
        .trim()

fun normalizeMatchingPairs(pairs: List<MatchingPairDto>): List<MatchingPairDto> =
    pairs.mapIndexed { index, pair -> MatchingPairDto(index + 1, pair.left.trim(), pair.right.trim()) }
        .filter { it.left.isNotEmpty() || it.right.isNotEmpty() }

/** Сообщение валидации пар (matchingPairsValidationMessage) или null. */
fun matchingPairsValidationMessage(pairs: List<MatchingPairDto>): String? {
    val normalized = normalizeMatchingPairs(pairs)
    if (normalized.size < 2) return "Для вопроса на соответствие нужны минимум две пары."
    if (normalized.any { it.left.isEmpty() || it.right.isEmpty() }) {
        return "Заполните обе части каждой пары соответствия."
    }
    val left = normalized.map { comparableText(it.left) }
    val right = normalized.map { comparableText(it.right) }
    if (left.toSet().size != left.size) return "Значения в левой колонке должны быть уникальными."
    if (right.toSet().size != right.size) return "Значения в правой колонке должны быть уникальными."
    return null
}

/** Разбор строки вида «left -> right | left2 -> right2» (parseMatchingDisplay). */
fun parseMatchingDisplay(value: String?): List<MatchingPairDto> {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return emptyList()
    val pairs = text.split(Regex("\\s*\\|\\s*")).mapIndexedNotNull { index, segment ->
        val separator = segment.indexOf("->")
        if (separator < 0) return@mapIndexedNotNull null
        val left = segment.substring(0, separator).trim()
        val rawRight = segment.substring(separator + 2).trim()
        if (left.isEmpty()) null else MatchingPairDto(index + 1, left, if (rawRight == "-") "" else rawRight)
    }
    return if (pairs.size >= 2) pairs else emptyList()
}

data class MatchingResultPair(
    val ordinal: Int,
    val left: String,
    val givenRight: String,
    val correctRight: String,
    val matches: Boolean,
)

fun matchingResultPairs(givenAnswer: String?, correctAnswer: String? = null): List<MatchingResultPair> {
    val given = parseMatchingDisplay(givenAnswer)
    val correct = parseMatchingDisplay(correctAnswer)
    val source = correct.ifEmpty { given }
    return source.mapIndexed { index, pair ->
        val g = given.getOrNull(index)
        val c = correct.getOrNull(index)
        val givenRight = g?.right.orEmpty()
        val correctRight = c?.right.orEmpty()
        MatchingResultPair(
            ordinal = index + 1,
            left = c?.left?.ifEmpty { null } ?: g?.left?.ifEmpty { null } ?: pair.left,
            givenRight = givenRight,
            correctRight = correctRight,
            matches = correct.isNotEmpty() && comparableText(givenRight).isNotEmpty() &&
                comparableText(givenRight) == comparableText(correctRight),
        )
    }
}

// endregion

// region Answers state

/**
 * Ответы на вопросы попытки.
 * matching: questionId → (текст правой части → ordinal левой подсказки).
 * Ключ — текст, потому что сервер перемешивает matchingOptions при каждом start/resume.
 */
@Serializable
data class AttemptAnswers(
    val single: Map<Long, Long> = emptyMap(),
    val multiple: Map<Long, Set<Long>> = emptyMap(),
    val text: Map<Long, String> = emptyMap(),
    val matching: Map<Long, Map<String, Int>> = emptyMap(),
) {
    fun withSingle(questionId: Long, optionId: Long?) =
        copy(single = if (optionId == null) single - questionId else single + (questionId to optionId))

    fun withMultiple(questionId: Long, optionId: Long, checked: Boolean): AttemptAnswers {
        val current = multiple[questionId].orEmpty()
        val next = if (checked) current + optionId else current - optionId
        return copy(multiple = multiple + (questionId to next))
    }

    fun withText(questionId: Long, value: String) = copy(text = text + (questionId to value))

    /**
     * Назначить правой части [rightText] подсказку [promptOrdinal] (или снять при null).
     * Каждая подсказка используется не более одного раза (assignMatchingRightIndex).
     */
    fun withMatching(questionId: Long, promptOrdinal: Int, rightText: String?): AttemptAnswers {
        val current = matching[questionId].orEmpty().filterValues { it != promptOrdinal }.toMutableMap()
        if (rightText != null) current[rightText] = promptOrdinal
        return copy(matching = matching + (questionId to current))
    }

    /** Выбранная правая часть для подсказки. */
    fun matchingRightFor(questionId: Long, promptOrdinal: Int): String? =
        matching[questionId]?.entries?.firstOrNull { it.value == promptOrdinal }?.key

    fun isAnswered(question: PublicQuestionDto): Boolean = when (effectiveType(question)) {
        QuestionType.SINGLE -> single[question.id] != null
        QuestionType.MULTIPLE -> multiple[question.id].orEmpty().isNotEmpty()
        QuestionType.MATCHING -> matching[question.id].orEmpty().isNotEmpty()
        else -> text[question.id].orEmpty().isNotBlank()
    }

    fun matchedCount(questionId: Long): Int = matching[questionId].orEmpty().size
}

/** Тип с учётом отсутствия вариантов: single/multiple без вариантов — текст (как TestView.vue). */
fun effectiveType(question: PublicQuestionDto): Int = when {
    question.type == QuestionType.SINGLE && question.options.isNotEmpty() -> QuestionType.SINGLE
    question.type == QuestionType.MULTIPLE && question.options.isNotEmpty() -> QuestionType.MULTIPLE
    question.type == QuestionType.MATCHING -> QuestionType.MATCHING
    else -> QuestionType.TEXT
}

/** Сборка тела submit (TestView.vue: buildSubmission). */
fun buildSubmission(questions: List<PublicQuestionDto>, answers: AttemptAnswers): SubmitAttemptRequest {
    val ids = mutableListOf<Long>()
    val texts = mutableListOf<String>()
    val options = mutableListOf<List<Long>>()
    questions.forEach { q ->
        ids.add(q.id)
        when (effectiveType(q)) {
            QuestionType.SINGLE -> {
                texts.add("")
                options.add(listOfNotNull(answers.single[q.id]))
            }
            QuestionType.MULTIPLE -> {
                texts.add("")
                options.add(answers.multiple[q.id].orEmpty().toList())
            }
            QuestionType.MATCHING -> {
                texts.add(serializeMatchingAnswer(q, answers))
                options.add(emptyList())
            }
            else -> {
                texts.add(answers.text[q.id].orEmpty())
                options.add(emptyList())
            }
        }
    }
    return SubmitAttemptRequest(ids, texts, options)
}

/**
 * JSON `{"pairs":[{ordinal, left, right}]}` — по одной записи на каждую правую часть,
 * ordinal = 0, если правой части ничего не сопоставлено.
 */
fun serializeMatchingAnswer(question: PublicQuestionDto, answers: AttemptAnswers): String {
    val promptByOrdinal = question.matchingPrompts.associate { it.ordinal to it.text }
    val selections = answers.matching[question.id].orEmpty()
    val json = buildJsonObject {
        put("pairs", buildJsonArray {
            question.matchingOptions.forEach { right ->
                val ordinal = selections[right] ?: 0
                add(buildJsonObject {
                    put("ordinal", ordinal)
                    put("left", promptByOrdinal[ordinal].orEmpty())
                    put("right", right)
                })
            }
        })
    }
    return json.toString()
}

// endregion

// region Drafts (utils/testAttemptDraft.js) — в отличие от веба хранятся постоянно

@Serializable
data class AttemptDraft(
    val version: Int = DRAFT_VERSION,
    val testId: Int,
    val assignmentId: Int,
    val attemptId: Int,
    val questionSignature: String,
    val answers: AttemptAnswers,
)

const val DRAFT_VERSION = 1

/** Сигнатура набора вопросов: черновик применяется только к той же попытке и тем же вопросам. */
fun questionSignature(questions: List<PublicQuestionDto>): String =
    questions.joinToString("|") { q ->
        buildString {
            append(q.id).append(':').append(q.type)
            append(':').append(q.options.map { it.id }.sorted().joinToString(","))
            append(':').append(q.matchingPrompts.map { it.ordinal }.sorted().joinToString(","))
            append(':').append(q.matchingOptions.map { comparableText(it) }.sorted().joinToString(","))
        }
    }

/** Результат завершённой попытки — чтобы показать его после возврата на экран. */
@Serializable
data class CompletedAttempt(
    val testId: Int,
    val assignmentId: Int,
    val result: SubmitAttemptResponse,
    val questionTexts: List<String> = emptyList(),
)

class AttemptDraftStore(
    private val store: KeyValueStore,
    private val json: Json,
) {
    private fun key(testId: Int, assignmentId: Int) = "student-test-draft:v$DRAFT_VERSION:$testId:$assignmentId"
    private fun pendingKey(attemptId: Int) = "student-test-submit-pending:$attemptId"

    fun load(testId: Int, assignmentId: Int, attemptId: Int, signature: String): AttemptAnswers? {
        val raw = store.getString(key(testId, assignmentId)) ?: return null
        val draft = runCatching { json.decodeFromString(AttemptDraft.serializer(), raw) }.getOrNull()
        if (draft == null || draft.version != DRAFT_VERSION || draft.attemptId != attemptId ||
            draft.questionSignature != signature
        ) {
            clear(testId, assignmentId)
            return null
        }
        return draft.answers
    }

    fun save(draft: AttemptDraft) {
        store.putString(key(draft.testId, draft.assignmentId), json.encodeToString(AttemptDraft.serializer(), draft))
    }

    fun clear(testId: Int, assignmentId: Int) {
        store.putString(key(testId, assignmentId), null)
    }

    /**
     * Отметка «отправка начата, результат неизвестен» (сеть/таймаут при submit).
     * Пока она стоит, повторная отправка блокируется (useTestAttemptLifecycle.js).
     */
    fun markSubmitUnknown(attemptId: Int, value: Boolean) {
        store.putString(pendingKey(attemptId), if (value) "1" else null)
    }

    fun isSubmitUnknown(attemptId: Int): Boolean = store.getString(pendingKey(attemptId)) == "1"
}

// endregion
