package org.santayn.testing.mobile.feature.tests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CheckRow
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadStateContent
import org.santayn.testing.mobile.core.ui.RadioRow
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.SpeakButton
import org.santayn.testing.mobile.core.ui.StatGrid
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.api.LearningApi
import org.santayn.testing.mobile.data.model.AttemptLoadDto
import org.santayn.testing.mobile.data.model.PublicOptionDto
import org.santayn.testing.mobile.data.model.PublicQuestionDto
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.SubmitAttemptResponse
import org.santayn.testing.mobile.domain.AttemptAnswers
import org.santayn.testing.mobile.domain.AttemptDraft
import org.santayn.testing.mobile.domain.AttemptDraftStore
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.buildSubmission
import org.santayn.testing.mobile.domain.effectiveType
import org.santayn.testing.mobile.domain.matchingResultPairs
import org.santayn.testing.mobile.domain.questionSignature
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.LectureRoute
import org.santayn.testing.mobile.navigation.ResultsRoute
import org.santayn.testing.mobile.navigation.TestRoute

/*
 * Прохождение теста — views/tests/TestView.vue + composables/tests/useTestAttemptLifecycle.js.
 */

data class TestSession(
    val attempt: AttemptLoadDto,
    val questions: List<PublicQuestionDto>,
    val signature: String,
    val restoredFromDraft: Boolean,
)

sealed interface SubmitState {
    data object Idle : SubmitState
    data object Submitting : SubmitState

    /** Сеть/таймаут при отправке: результат неизвестен, повторная отправка заблокирована. */
    data class Unknown(val message: String) : SubmitState
    data class Done(val result: SubmitAttemptResponse) : SubmitState
}

class TestViewModel(
    private val testId: Int,
    private val assignmentId: Int,
    private val learning: LearningApi,
    private val drafts: AttemptDraftStore,
    private val cache: ContextCache,
) : BaseViewModel() {

    private val _state = MutableStateFlow<LoadState<TestSession>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _answers = MutableStateFlow(AttemptAnswers())
    val answers = _answers.asStateFlow()
    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submit = _submit.asStateFlow()

    private var saveJob: Job? = null

    init {
        start()
    }

    /** Старт или продолжение попытки: сервер возвращает ту же незавершённую попытку. */
    fun start() {
        _state.value = LoadState.Loading
        launchSafe(onError = { _state.value = LoadState.Error(it.userMessage("Не удалось начать тест")) }) {
            val attempt = learning.startAttempt(assignmentId)
            val questions = attempt.questions.sortedWith(compareBy({ it.ordinal }, { it.id }))
            val signature = questionSignature(questions)
            val restored = drafts.load(testId, assignmentId, attempt.attemptId, signature)
            _answers.value = restored ?: AttemptAnswers()
            if (drafts.isSubmitUnknown(attempt.attemptId)) {
                _submit.value = SubmitState.Unknown(
                    "Предыдущая отправка этой попытки не подтвердилась. Проверьте раздел «Результаты»."
                )
            }
            _state.value = LoadState.Ready(TestSession(attempt, questions, signature, restored != null))
        }
    }

    fun update(transform: (AttemptAnswers) -> AttemptAnswers) {
        if (_submit.value != SubmitState.Idle) return
        _answers.update(transform)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = launchSafe(onError = {}) {
            delay(DRAFT_DEBOUNCE_MS)
            persistDraft()
        }
    }

    /** Немедленное сохранение (приложение уходит в фон). */
    fun flushDraft() {
        saveJob?.cancel()
        persistDraft()
    }

    private fun persistDraft() {
        val session = (_state.value as? LoadState.Ready)?.data ?: return
        if (_submit.value is SubmitState.Done) return
        drafts.save(
            AttemptDraft(
                testId = testId,
                assignmentId = assignmentId,
                attemptId = session.attempt.attemptId,
                questionSignature = session.signature,
                answers = _answers.value,
            )
        )
    }

    fun submitAttempt() {
        val session = (_state.value as? LoadState.Ready)?.data ?: return
        if (_submit.value != SubmitState.Idle) return
        flushDraft()
        _submit.value = SubmitState.Submitting
        val attemptId = session.attempt.attemptId
        launchSafe(onError = { error ->
            val unknown = error is ApiException && (error.isNetworkError || error.isTimeout)
            if (unknown) {
                drafts.markSubmitUnknown(attemptId, true)
                _submit.value = SubmitState.Unknown(
                    "Не удалось получить ответ сервера. Попытка могла быть отправлена — " +
                        "проверьте раздел «Результаты», прежде чем отправлять повторно."
                )
            } else {
                drafts.markSubmitUnknown(attemptId, false)
                _submit.value = SubmitState.Idle
                toast(error.userMessage("Не удалось отправить ответы"))
            }
        }) {
            drafts.markSubmitUnknown(attemptId, true)
            val result = learning.submitAttempt(attemptId, buildSubmission(session.questions, _answers.value))
            drafts.markSubmitUnknown(attemptId, false)
            drafts.clear(testId, assignmentId)
            cache.invalidate()
            _submit.value = SubmitState.Done(result)
        }
    }

    /** Пользователь проверил результаты и хочет повторить отправку вручную. */
    fun unlockSubmit() {
        val session = (_state.value as? LoadState.Ready)?.data ?: return
        drafts.markSubmitUnknown(session.attempt.attemptId, false)
        _submit.value = SubmitState.Idle
    }

    private companion object {
        const val DRAFT_DEBOUNCE_MS = 250L
    }
}

@Composable
fun TestScreen(route: TestRoute, navigator: AppNavigator) {
    val vm = koinViewModel<TestViewModel>(key = "test-${route.testId}-${route.assignmentId}") {
        parametersOf(route.testId, route.assignmentId)
    }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val answers by vm.answers.collectAsState()
    val submit by vm.submit.collectAsState()
    var confirm by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }

    val session = (state as? LoadState.Ready)?.data
    val title = session?.attempt?.test?.title ?: "Тест"

    val done = submit as? SubmitState.Done
    if (done != null && session != null) {
        TestResultScreen(
            title = title,
            result = done.result,
            onResults = { navigator.replace(ResultsRoute) },
            onBack = {
                if (route.lectureId != null) navigator.replace(LectureRoute(route.lectureId, route.subjectId)) else navigator.back()
            },
        )
        return
    }

    AppScreen(
        title = title,
        onBack = navigator::back,
        bottomBar = {
            if (session != null) {
                SubmitBar(
                    answered = session.questions.count { answers.isAnswered(it) },
                    total = session.questions.size,
                    submit = submit,
                    onSubmit = {
                        val unanswered = session.questions.count { !answers.isAnswered(it) }
                        if (unanswered > 0) confirm = true else vm.submitAttempt()
                    },
                )
            }
        },
    ) { padding ->
        // Попытку не перезапускаем при восстановлении связи — только повтор после ошибки.
        LoadStateContent(state, onRetry = vm::start, refreshOnReconnect = false) { data ->
            val locked = submit != SubmitState.Idle
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.attempt.test?.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (data.restoredFromDraft) Banner("Восстановлены сохранённые ответы этой попытки.", Tone.INFO)
                        (submit as? SubmitState.Unknown)?.let { unknown ->
                            Banner(unknown.message, Tone.WARNING, title = "Результат отправки неизвестен")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BusyButton("Открыть результаты", onClick = { navigator.open(ResultsRoute) }, outlined = true)
                                BusyButton("Отправить снова", onClick = vm::unlockSubmit, outlined = true)
                            }
                        }
                        Text(
                            "Ответы сохраняются на устройстве автоматически.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                itemsIndexed(data.questions, key = { _, q -> q.id }) { index, question ->
                    QuestionCard(
                        index = index,
                        question = question,
                        answers = answers,
                        enabled = !locked,
                        onChange = vm::update,
                    )
                }
            }
        }
    }

    if (confirm && session != null) {
        val unanswered = session.questions.count { !answers.isAnswered(it) }
        ConfirmDialog(
            title = "Отправить ответы?",
            message = "Без ответа осталось вопросов: $unanswered. Они будут засчитаны как неверные.",
            confirmText = "Отправить",
            onConfirm = {
                confirm = false
                vm.submitAttempt()
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun SubmitBar(answered: Int, total: Int, submit: SubmitState, onSubmit: () -> Unit) {
    Surface(shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Отвечено $answered из $total", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                BusyButton(
                    text = "Завершить",
                    onClick = onSubmit,
                    busy = submit == SubmitState.Submitting,
                    enabled = submit == SubmitState.Idle && total > 0,
                )
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else answered.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun QuestionCard(
    index: Int,
    question: PublicQuestionDto,
    answers: AttemptAnswers,
    enabled: Boolean,
    onChange: ((AttemptAnswers) -> AttemptAnswers) -> Unit,
) {
    val type = effectiveType(question)
    val answered = answers.isAnswered(question)
    val options = question.options.sortedWith(compareBy({ it.ordinal }, { it.id }))
    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                StatusChip("Вопрос ${index + 1}", Tone.PRIMARY)
                StatusChip(QuestionType.title(type))
                question.points?.let { StatusChip("Баллы: ${formatNumber(it)}") }
                if (answered) StatusChip("Отвечено", Tone.SUCCESS)
            }
            SpeakButton(key = "question-${question.id}", text = { spokenQuestion(index, question, type, options) })
        }
        Spacer(Modifier.size(4.dp))
        // Текст вопроса — заголовок: TalkBack/VoiceOver переходят между вопросами жестом «по заголовкам».
        Text(
            question.displayText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.size(4.dp))
        when (type) {
            QuestionType.SINGLE -> Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    RadioRow(
                        title = option.text,
                        selected = answers.single[question.id] == option.id,
                        enabled = enabled,
                        onClick = { onChange { it.withSingle(question.id, option.id) } },
                    )
                }
            }
            QuestionType.MULTIPLE -> Column {
                options.forEach { option ->
                    CheckRow(
                        title = option.text,
                        checked = option.id in answers.multiple[question.id].orEmpty(),
                        enabled = enabled,
                        onCheckedChange = { checked -> onChange { it.withMultiple(question.id, option.id, checked) } },
                    )
                }
            }
            QuestionType.MATCHING -> MatchingQuestion(question, answers, enabled, onChange)
            else -> FormField(
                value = answers.text[question.id].orEmpty(),
                onValueChange = { value -> onChange { it.withText(question.id, value) } },
                label = "Ваш ответ",
                singleLine = false,
                minLines = 3,
                enabled = enabled,
            )
        }
    }
}

/** Текст для кнопки «Прочитать вслух»: номер, вопрос и варианты ответа. */
private fun spokenQuestion(
    index: Int,
    question: PublicQuestionDto,
    type: Int,
    options: List<PublicOptionDto>,
): String = buildString {
    append("Вопрос ${index + 1}. ")
    append(question.displayText.trimEnd('.', ' ')).append(". ")
    when (type) {
        QuestionType.SINGLE, QuestionType.MULTIPLE -> {
            append(if (type == QuestionType.SINGLE) "Выберите один вариант. " else "Выберите все верные варианты. ")
            options.forEachIndexed { i, option -> append("${i + 1}: ${option.text.trimEnd('.', ' ')}. ") }
        }
        QuestionType.MATCHING -> {
            append("Сопоставьте. ")
            question.matchingPrompts.sortedBy { it.ordinal }.forEach { append(it.text.trimEnd('.', ' ')).append(". ") }
            append("Варианты: ")
            append(question.matchingOptions.joinToString("; "))
        }
        else -> append("Введите ответ текстом.")
    }
}

/**
 * Сопоставление (components/tests/StudentMatchingQuestion.vue):
 * для каждой левой подсказки выбирается правая часть; каждую правую часть можно использовать один раз.
 */
@Composable
private fun MatchingQuestion(
    question: PublicQuestionDto,
    answers: AttemptAnswers,
    enabled: Boolean,
    onChange: ((AttemptAnswers) -> AttemptAnswers) -> Unit,
) {
    val prompts = question.matchingPrompts.sortedBy { it.ordinal }
    val selections = answers.matching[question.id].orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Сопоставлено ${selections.size} из ${prompts.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        prompts.forEach { prompt ->
            val current = answers.matchingRightFor(question.id, prompt.ordinal)
            val available = question.matchingOptions.filter { right ->
                val owner = selections[right]
                owner == null || owner == prompt.ordinal
            }
            Column {
                // Подсказка входит в описание поля ниже — программе чтения с экрана не дублируем.
                Text(
                    prompt.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                SelectField(
                    label = "Соответствие",
                    accessibilityLabel = "Соответствие для «${prompt.text}»",
                    options = available.map { SelectOption(it, it) },
                    selected = current,
                    allowClear = true,
                    enabled = enabled,
                    placeholder = "Выберите вариант",
                    onSelect = { right -> onChange { it.withMatching(question.id, prompt.ordinal, right) } },
                )
            }
        }
    }
}

@Composable
private fun TestResultScreen(
    title: String,
    result: SubmitAttemptResponse,
    onResults: () -> Unit,
    onBack: () -> Unit,
) {
    AppScreen(title = "Результат", subtitle = title, onBack = onBack) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    Text(
                        "Тест завершён",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    StatGrid(
                        listOf(
                            "Правильных" to "${result.correctCount} из ${result.totalCount}",
                            "Баллы" to formatNumber(result.score),
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BusyButton("Мои результаты", onClick = onResults)
                        BusyButton("К лекции", onClick = onBack, outlined = true)
                    }
                }
            }
            itemsIndexed(result.details) { index, detail ->
                AppCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                    ) {
                        Icon(
                            if (detail.correct) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (detail.correct) "Верно" else "Неверно",
                            tint = if (detail.correct) AppTheme.status.success else AppTheme.status.danger,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Вопрос ${index + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(detail.questionText.orEmpty(), style = MaterialTheme.typography.titleSmall)
                    AnswerView(detail.givenAnswer, detail.correctAnswer)
                }
            }
        }
    }
}

/** Ответ: для matching — список пар, иначе текст. */
@Composable
fun AnswerView(given: String?, correct: String?) {
    val pairs = matchingResultPairs(given, correct)
    if (pairs.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            pairs.forEach { pair ->
                // Верность пары передаётся не только цветом, но и значком с подписью.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    if (pair.correctRight.isNotBlank()) {
                        Icon(
                            if (pair.matches) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (pair.matches) "Верно" else "Неверно",
                            tint = if (pair.matches) AppTheme.status.success else AppTheme.status.danger,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "${pair.left} → ${pair.givenRight.ifBlank { "—" }}" +
                            if (pair.correctRight.isNotBlank() && !pair.matches) "  (верно: ${pair.correctRight})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            pair.correctRight.isBlank() -> MaterialTheme.colorScheme.onSurface
                            pair.matches -> AppTheme.status.successText
                            else -> AppTheme.status.dangerText
                        },
                    )
                }
            }
        }
    } else {
        Text(
            "Ваш ответ: ${given?.takeIf { it.isNotBlank() } ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!correct.isNullOrBlank()) {
            Text("Правильный ответ: $correct", style = MaterialTheme.typography.bodyMedium, color = AppTheme.status.successText)
        }
    }
}
