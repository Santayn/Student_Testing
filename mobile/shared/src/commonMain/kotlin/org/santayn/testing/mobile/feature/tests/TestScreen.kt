package org.santayn.testing.mobile.feature.tests

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.santayn.testing.mobile.data.api.ResultsApi
import org.santayn.testing.mobile.data.model.ResultItemDto
import org.santayn.testing.mobile.data.model.ReviewStatus
import org.santayn.testing.mobile.domain.GradingStatus
import org.santayn.testing.mobile.domain.gradingStatusNormalized
import org.santayn.testing.mobile.domain.score
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.core.ui.design.AnswerStyle
import org.santayn.testing.mobile.core.ui.AnswerOptionCard
import org.santayn.testing.mobile.core.util.formatDuration
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.EmptyState
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
    private val results: ResultsApi,
) : BaseViewModel() {

    private val _state = MutableStateFlow<LoadState<TestSession>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _answers = MutableStateFlow(AttemptAnswers())
    val answers = _answers.asStateFlow()
    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submit = _submit.asStateFlow()

    /** Свежее состояние ответов сданной попытки: обновляется, пока ИИ проверяет свободные ответы. */
    private val _review = MutableStateFlow<List<ResultItemDto>?>(null)
    val review = _review.asStateFlow()

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
            if (result.pendingReviewCount > 0) watchReview(result.attemptId)
        }
    }

    /** Опрашивает результат, пока ИИ не закончит проверку (не дольше двух минут). */
    private fun watchReview(attemptId: Int) {
        viewModelScope.launch {
            repeat(REVIEW_POLLS) {
                delay(REVIEW_POLL_MS)
                val attempt = runCatching {
                    results.getStudentData(testId = testId).attempts.firstOrNull { it.attemptId == attemptId }
                }.getOrNull()
                if (attempt != null) {
                    _review.value = attempt.results
                    if (attempt.results.none { it.reviewStatus == ReviewStatus.AI_PENDING }) return@launch
                }
            }
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
        const val REVIEW_POLL_MS = 2_000L
        const val REVIEW_POLLS = 60
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
    val review by vm.review.collectAsState()
    var confirm by rememberSaveable { mutableStateOf(false) }
    // Тест проходится по одному вопросу на экран; номер текущего переживает поворот экрана.
    var current by rememberSaveable(route.testId, route.assignmentId) { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushDraft() }

    val session = (state as? LoadState.Ready)?.data
    val title = session?.attempt?.test?.title ?: "Тест"

    val done = submit as? SubmitState.Done
    if (done != null && session != null) {
        TestResultScreen(
            title = title,
            result = done.result,
            review = review,
            onResults = { navigator.replace(ResultsRoute) },
            onBack = {
                if (route.lectureId != null) navigator.replace(LectureRoute(route.lectureId, route.subjectId)) else navigator.back()
            },
        )
        return
    }

    val questions = session?.questions.orEmpty()
    val index = current.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    val locked = submit != SubmitState.Idle
    val requestSubmit = {
        val unanswered = questions.count { !answers.isAnswered(it) }
        if (unanswered > 0) confirm = true else vm.submitAttempt()
    }

    AppScreen(
        title = title,
        subtitle = session?.attempt?.test?.duration?.let(::formatDuration)?.let { "Ограничение по времени: $it" },
        onBack = navigator::back,
        actions = {
            if (questions.isNotEmpty() && submit == SubmitState.Idle) {
                TextButton(onClick = requestSubmit) { Text("Завершить") }
            }
        },
        bottomBar = {
            if (questions.isNotEmpty()) {
                StepBar(
                    index = index,
                    total = questions.size,
                    answered = questions.count { answers.isAnswered(it) },
                    submit = submit,
                    onPrev = { current = index - 1 },
                    onNext = { current = index + 1 },
                    onSubmit = requestSubmit,
                )
            }
        },
    ) { padding ->
        // Попытку не перезапускаем при восстановлении связи — только повтор после ошибки.
        LoadStateContent(state, onRetry = vm::start, refreshOnReconnect = false) { data ->
            val scroll = rememberScrollState()
            LaunchedEffect(index) { scroll.scrollTo(0) }
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(screenPadding(padding)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (data.restoredFromDraft) Banner("Восстановлены сохранённые ответы этой попытки.", Tone.INFO)
                (submit as? SubmitState.Unknown)?.let { unknown ->
                    Banner(unknown.message, Tone.WARNING, title = "Результат отправки неизвестен")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BusyButton("Открыть результаты", onClick = { navigator.open(ResultsRoute) }, outlined = true)
                        BusyButton("Отправить снова", onClick = vm::unlockSubmit, outlined = true)
                    }
                }
                if (data.questions.isEmpty()) {
                    EmptyState("В тесте нет вопросов")
                } else {
                    QuestionProgress(data.questions, answers, index, onJump = { current = it })
                    val question = data.questions[index]
                    key(question.id) {
                        QuestionCard(index = index, question = question, answers = answers, enabled = !locked, onChange = vm::update)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.status.successText,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "Ответы сохраняются на телефоне — можно продолжить позже",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirm && session != null) {
        val unanswered = questions.count { !answers.isAnswered(it) }
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

/**
 * Прогресс по вопросам: «Вопрос N из M» (объявляется TalkBack при переходе) и сегменты-кнопки
 * для перехода к любому вопросу. Больше 10 вопросов — ряд номеров с прокруткой.
 */
@Composable
private fun QuestionProgress(
    questions: List<PublicQuestionDto>,
    answers: AttemptAnswers,
    current: Int,
    onJump: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Вопрос ${current + 1} из ${questions.size}",
            style = MaterialTheme.typography.labelLarge,
            color = AppTheme.status.primarySoftText,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (questions.size <= 10) {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                questions.forEachIndexed { i, q ->
                    val answered = answers.isAnswered(q)
                    val color = when {
                        i == current -> MaterialTheme.colorScheme.onBackground
                        answered -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        Modifier.weight(1f).heightIn(min = 48.dp)
                            .selectable(selected = i == current, role = Role.Tab, onClick = { onJump(i) })
                            .semantics {
                                contentDescription = "Вопрос ${i + 1}"
                                stateDescription = if (answered) "отвечен" else "без ответа"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(if (i == current) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(percent = 50)).background(color)
                        )
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                itemsIndexed(questions) { i, q ->
                    val answered = answers.isAnswered(q)
                    val selected = i == current
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .selectable(selected = selected, role = Role.Tab, onClick = { onJump(i) })
                            .semantics { stateDescription = if (answered) "отвечен" else "без ответа" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (answered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/** Нижняя панель шага: «Назад» и «Следующий вопрос», на последнем — «Завершить». */
@Composable
private fun StepBar(
    index: Int,
    total: Int,
    answered: Int,
    submit: SubmitState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Отвечено $answered из $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (index > 0) BusyButton("Назад", onClick = onPrev, outlined = true)
                if (index < total - 1) {
                    BusyButton(
                        "Следующий вопрос",
                        onClick = onNext,
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    BusyButton(
                        "Завершить тест",
                        onClick = onSubmit,
                        busy = submit == SubmitState.Submitting,
                        enabled = submit == SubmitState.Idle,
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
    val header: @Composable () -> Unit = {
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
    }
    when (Design.tokens.answerStyle) {
        AnswerStyle.CARDS -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            header()
            // Текст вопроса — заголовок: TalkBack/VoiceOver переходят между вопросами жестом «по заголовкам».
            Text(
                question.displayText,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            when (type) {
                QuestionType.SINGLE -> Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { option ->
                        AnswerOptionCard(
                            title = option.text,
                            selected = answers.single[question.id] == option.id,
                            multiple = false,
                            enabled = enabled,
                            onToggle = { onChange { it.withSingle(question.id, option.id) } },
                        )
                    }
                }
                QuestionType.MULTIPLE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    options.forEach { option ->
                        AnswerOptionCard(
                            title = option.text,
                            selected = option.id in answers.multiple[question.id].orEmpty(),
                            multiple = true,
                            enabled = enabled,
                            onToggle = { checked -> onChange { it.withMultiple(question.id, option.id, checked) } },
                        )
                    }
                }
                QuestionType.MATCHING -> MatchingBoard(question, answers, enabled, onChange)
                else -> AppCard { TextAnswerField(question, answers, enabled, onChange) }
            }
        }
        AnswerStyle.LIST -> AppCard {
            header()
            Text(
                question.displayText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
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
                QuestionType.MATCHING -> MatchingBoard(question, answers, enabled, onChange)
                else -> TextAnswerField(question, answers, enabled, onChange)
            }
        }
    }
}

@Composable
private fun TextAnswerField(
    question: PublicQuestionDto,
    answers: AttemptAnswers,
    enabled: Boolean,
    onChange: ((AttemptAnswers) -> AttemptAnswers) -> Unit,
) {
    FormField(
        value = answers.text[question.id].orEmpty(),
        onValueChange = { value -> onChange { it.withText(question.id, value) } },
        label = "Ваш ответ",
        singleLine = false,
        minLines = 3,
        enabled = enabled,
    )
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

@Composable
private fun TestResultScreen(
    title: String,
    result: SubmitAttemptResponse,
    review: List<ResultItemDto>?,
    onResults: () -> Unit,
    onBack: () -> Unit,
) {
    // Ответы сдачи и их свежее состояние идут в одном порядке (ответы сохраняются по порядку вопросов).
    val live = review?.takeIf { it.size == result.details.size }
    val aiChecking = if (live != null) live.any { it.reviewStatus == ReviewStatus.AI_PENDING } else result.pendingReviewCount > 0
    val teacherPending = live?.count { it.reviewStatus == ReviewStatus.NEEDS_TEACHER } ?: 0
    val shown = if (live == null) result else result.copy(
        correctCount = live.count { it.correct },
        score = live.sumOf { it.awardedPoints ?: 0.0 },
        pendingReviewCount = live.count { it.reviewPending },
    )
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { _ ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        ) {
            item { ResultHero(title = title, result = shown, onBack = onBack) }
            if (aiChecking) {
                item {
                    AppCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("ИИ проверяет свободные ответы", style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            } else if (teacherPending > 0) {
                item {
                    Banner("Ответов у преподавателя: $teacherPending", Tone.INFO, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            if (result.details.isNotEmpty()) {
                item { SectionTitle("Разбор ответов", modifier = Modifier.padding(horizontal = 20.dp)) }
            }
            itemsIndexed(result.details) { index, detail ->
                val item = live?.get(index)
                val reviewStatus = item?.reviewStatus ?: if (detail.pendingReview) ReviewStatus.AI_PENDING else null
                val pending = reviewStatus == ReviewStatus.AI_PENDING || reviewStatus == ReviewStatus.NEEDS_TEACHER
                val correct = item?.correct ?: detail.correct
                AppCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                    ) {
                        Text("Вопрос ${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        when {
                            reviewStatus == ReviewStatus.AI_PENDING -> StatusChip("Проверяет ИИ", Tone.INFO, busy = true)
                            pending -> StatusChip("У преподавателя", Tone.INFO)
                            item != null -> {
                                val status = item.gradingStatusNormalized()
                                val score = item.score()
                                StatusChip(
                                    "${status.label} · ${formatNumber(score.awarded)}/${formatNumber(score.maximum)}",
                                    when (status) {
                                        GradingStatus.CORRECT -> Tone.SUCCESS
                                        GradingStatus.PARTIAL -> Tone.WARNING
                                        else -> Tone.DANGER
                                    },
                                )
                            }
                            else -> StatusChip(if (correct) "Верно" else "Неверно", if (correct) Tone.SUCCESS else Tone.DANGER)
                        }
                    }
                    Text(detail.questionText.orEmpty(), style = MaterialTheme.typography.titleSmall)
                    AnswerView(detail.givenAnswer, detail.correctAnswer)
                    item?.aiFeedback?.takeIf { it.isNotBlank() && !pending }?.let { Banner(it, Tone.INFO, title = "Комментарий ИИ") }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BusyButton("К лекции", onClick = onBack, outlined = true, modifier = Modifier.weight(1f))
                    BusyButton("Все результаты", onClick = onResults, modifier = Modifier.weight(1f))
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
