package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.DateField
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.MultiSelectField
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatGrid
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.endOfDayUtcIso
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.ui.startOfDayUtcIso
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.QuestionsApi
import org.santayn.testing.mobile.data.api.TeachingApi
import org.santayn.testing.mobile.data.api.TestsApi
import org.santayn.testing.mobile.data.api.TopicsApi
import org.santayn.testing.mobile.data.model.QuestionDto
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.SelectionRuleRequest
import org.santayn.testing.mobile.data.model.TestAssignmentRequest
import org.santayn.testing.mobile.data.model.TestAssignmentStatus
import org.santayn.testing.mobile.data.model.TestRequest
import org.santayn.testing.mobile.data.model.TopicDto
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.TeacherTestCreateRoute

/*
 * Мастер создания теста — views/teacher/TestEditorView.vue,
 * composables/tests/useTestEditorState.js, useTestEditorSaveFlow.js, utils/createTestWithAssignments.js.
 */

/** Группа и учебные назначения преподавателя в ней. */
data class GroupTarget(val groupId: Int, val name: String, val assignmentIds: List<Int>)

data class TestForm(
    val title: String = "",
    val description: String = "",
    val questionCount: String = "1",
    val attemptsAllowed: String = "1",
    val textCount: String = "0",
    val singleCount: String = "0",
    val multipleCount: String = "0",
    val matchingCount: String = "0",
    val status: Int = TestAssignmentStatus.ACTIVE,
    val availableFrom: LocalDate? = null,
    val availableUntil: LocalDate? = null,
)

data class TestEditorUiState(
    val topics: List<TopicDto> = emptyList(),
    val topicId: Int? = null,
    val groups: List<GroupTarget> = emptyList(),
    val selectedGroups: Set<Int> = emptySet(),
    val questions: List<QuestionDto> = emptyList(),
    val loadingContext: Boolean = false,
    val form: TestForm = TestForm(),
    val saving: Boolean = false,
    val notice: Pair<Tone, String>? = null,
)

class TestEditorViewModel(
    private val route: TeacherTestCreateRoute,
    val subjects: TeacherSubjectsHolder,
    private val topicsApi: TopicsApi,
    private val teaching: TeachingApi,
    private val groupsApi: GroupsApi,
    private val questionsApi: QuestionsApi,
    private val testsApi: TestsApi,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TestEditorUiState(form = defaultForm()))
    val state = _state.asStateFlow()

    init {
        launchSafe {
            subjects.load(preferredSubjectId = route.subjectId)
            loadContext(route.topicId)
        }
    }

    /** Связь восстановилась: догружаем справочники, если они не загрузились. Введённые данные не сбрасываются. */
    fun reloadAfterReconnect() {
        if (!subjects.needsLoad) return
        launchSafe {
            subjects.load(preferredSubjectId = route.subjectId)
            loadContext(route.topicId)
        }
    }

    private fun defaultForm(): TestForm {
        val today = nowInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return TestForm(availableFrom = today, availableUntil = today.plus(DatePeriod(days = 30)))
    }

    fun selectMembership(id: Int?) {
        subjects.select(id)
        launchSafe { loadContext(null) }
    }

    private suspend fun loadContext(preferredTopicId: Int?) = coroutineScope {
        val membershipId = subjects.state.value.selectedMembershipId
        _state.update { it.copy(topics = emptyList(), topicId = null, groups = emptyList(), selectedGroups = emptySet(), questions = emptyList()) }
        if (membershipId == null) return@coroutineScope
        _state.update { it.copy(loadingContext = true) }
        try {
            val topicsDeferred = async { topicsApi.getAll(subjectMembershipId = membershipId) }
            val assignmentsDeferred = async { teaching.getAssignments(subjectMembershipId = membershipId, status = 1) }
            val topics = topicsDeferred.await().sortedWith(compareBy({ it.ordinal }, { it.id }))
            val assignments = assignmentsDeferred.await()
            val byGroup = assignments.filter { it.groupId != null }.groupBy { it.groupId!! }
            val groups = byGroup.keys.map { id -> async { runCatching { groupsApi.getById(id) }.getOrNull() } }.awaitAll()
                .filterNotNull().associateBy { it.id }
            val targets = byGroup.map { (groupId, list) ->
                GroupTarget(groupId, groups[groupId]?.name ?: "Группа #$groupId", list.map { it.id }.distinct())
            }.sortedBy { it.name.lowercase() }
            val topicId = topics.firstOrNull { it.id == preferredTopicId }?.id
            _state.update { it.copy(topics = topics, groups = targets, topicId = topicId, loadingContext = false) }
            if (topicId != null) loadQuestions(topicId)
        } catch (e: Exception) {
            _state.update { it.copy(loadingContext = false, notice = Tone.DANGER to e.userMessage()) }
        }
    }

    fun selectTopic(id: Int?) {
        _state.update { it.copy(topicId = id, questions = emptyList()) }
        if (id != null) launchSafe { loadQuestions(id) }
    }

    private suspend fun loadQuestions(topicId: Int) {
        val questions = questionsApi.getAll(topicId = topicId)
        _state.update { if (it.topicId == topicId) it.copy(questions = questions) else it }
    }

    fun setGroups(ids: Set<Int>) = _state.update { it.copy(selectedGroups = ids) }

    fun updateForm(transform: (TestForm) -> TestForm) = _state.update { it.copy(form = transform(it.form), notice = null) }

    data class Counts(val total: Int, val single: Int, val multiple: Int, val matching: Int, val text: Int)

    fun counts(questions: List<QuestionDto>): Counts {
        val active = questions.filter { it.active }
        return Counts(
            total = active.size,
            single = active.count { it.type == QuestionType.SINGLE },
            multiple = active.count { it.type == QuestionType.MULTIPLE },
            matching = active.count { it.type == QuestionType.MATCHING },
            text = active.count { it.type == QuestionType.TEXT },
        )
    }

    private fun String.num() = toIntOrNull() ?: 0

    fun fixedCount(form: TestForm) = form.textCount.num() + form.singleCount.num() + form.multipleCount.num() + form.matchingCount.num()

    /** useTestEditorState.validationError. */
    private fun validationError(s: TestEditorUiState): String? {
        val f = s.form
        val total = f.questionCount.num()
        val c = counts(s.questions)
        return when {
            subjects.state.value.selected == null || s.topicId == null -> "Выберите предмет и тему."
            s.selectedGroups.isEmpty() -> "Выберите хотя бы одну группу."
            total < 1 -> "Количество вопросов должно быть больше нуля."
            fixedCount(f) > total -> "Сумма вопросов по типам не может превышать общее количество."
            c.total < total -> "В теме недостаточно активных вопросов."
            c.text < f.textCount.num() || c.single < f.singleCount.num() ||
                c.multiple < f.multipleCount.num() || c.matching < f.matchingCount.num() ->
                "В теме недостаточно вопросов выбранных типов."
            f.title.isBlank() -> "Введите название теста."
            f.availableFrom == null || f.availableUntil == null -> "Укажите период доступности теста."
            f.availableFrom > f.availableUntil -> "Дата окончания должна быть не раньше даты начала."
            else -> null
        }
    }

    fun create() {
        val s = _state.value
        validationError(s)?.let { msg ->
            _state.update { it.copy(notice = Tone.DANGER to msg) }
            return
        }
        val assignmentIds = s.groups.filter { it.groupId in s.selectedGroups }.flatMap { it.assignmentIds }.distinct()
        if (assignmentIds.isEmpty()) {
            _state.update { it.copy(notice = Tone.DANGER to "Не удалось определить учебные назначения для выбранных групп.") }
            return
        }
        val f = s.form
        val total = f.questionCount.num()
        _state.update { it.copy(saving = true, notice = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, notice = Tone.DANGER to e.userMessage("Не удалось создать тест")) } }) {
            subjects.ensureSelectedActive()
            val test = testsApi.create(
                TestRequest(
                    title = f.title.trim(),
                    description = f.description.trim().ifBlank { null },
                    duration = null,
                    attemptsAllowed = f.attemptsAllowed.num().coerceAtLeast(1),
                    questionCount = total,
                    selectionRules = listOf(
                        SelectionRuleRequest(
                            courseLectureId = null,
                            topicId = s.topicId,
                            questionCount = total,
                            textQuestionCount = f.textCount.num(),
                            singleAnswerQuestionCount = f.singleCount.num(),
                            multipleAnswerQuestionCount = f.multipleCount.num(),
                            matchingQuestionCount = f.matchingCount.num(),
                            ordinal = 1,
                        )
                    ),
                )
            )
            try {
                assignmentIds.forEach { assignmentId ->
                    testsApi.createAssignments(
                        test.id,
                        TestAssignmentRequest(
                            scope = 4,
                            courseVersionId = null,
                            courseLectureId = null,
                            teachingAssignmentId = assignmentId,
                            availableFromUtc = f.availableFrom!!.startOfDayUtcIso(),
                            availableUntilUtc = f.availableUntil!!.endOfDayUtcIso(),
                            status = f.status,
                        ),
                    )
                }
            } catch (cause: Exception) {
                // Откат: удаляем созданный тест (createTestWithAssignments.js).
                val rollback = runCatching { testsApi.delete(test.id) }
                val base = cause.userMessage("Не удалось создать назначение теста")
                val message = if (rollback.isSuccess) {
                    "$base. Создание теста отменено, частичные назначения удалены."
                } else {
                    "$base. Не удалось автоматически удалить незавершённый тест #${test.id}. " +
                        "Не создавайте тест повторно, пока тест #${test.id} не будет удалён вручную."
                }
                _state.update { it.copy(saving = false, notice = Tone.DANGER to message) }
                return@launchSafe
            }
            _state.update {
                it.copy(
                    saving = false,
                    notice = Tone.SUCCESS to "Тест #${test.id} создан и назначен выбранным группам. " +
                        "Привяжите его к лекции в разделе «Лекции», чтобы студенты увидели тест.",
                    form = defaultForm(),
                    selectedGroups = emptySet(),
                )
            }
        }
    }
}

@Composable
fun TestEditorScreen(route: TeacherTestCreateRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<TestEditorViewModel>(key = "test-create-${route.subjectId}-${route.topicId}") { parametersOf(route, role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val picker by vm.subjects.state.collectAsState()
    val f = state.form
    val counts = vm.counts(state.questions)

    AppScreen(title = "Создать тест", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                StepCard("1. Предмет, группы и тема") {
                    TeacherSubjectSelector(picker, vm::selectMembership, vm.subjects.isAdmin)
                    if (picker.selected != null) {
                        MultiSelectField(
                            label = "Группы",
                            options = state.groups.map { SelectOption(it.groupId, it.name) },
                            selected = state.selectedGroups,
                            onChange = vm::setGroups,
                            placeholder = if (state.groups.isEmpty() && !state.loadingContext) "Нет активной нагрузки по группам" else "Выберите группы",
                            enabled = state.groups.isNotEmpty(),
                        )
                        SelectField(
                            "Тема",
                            state.topics.map { SelectOption(it.id, "${it.ordinal}. ${it.name}") },
                            state.topicId,
                            vm::selectTopic,
                            placeholder = if (state.topics.isEmpty()) "В предмете нет тем" else "Выберите тему",
                        )
                    }
                }
            }
            item {
                StepCard("2. Описание") {
                    FormField(f.title, { v -> vm.updateForm { it.copy(title = v) } }, "Название теста", required = true, maxLength = 200)
                    FormField(f.description, { v -> vm.updateForm { it.copy(description = v) } }, "Описание",
                        singleLine = false, minLines = 2, maxLength = 2000)
                }
            }
            item {
                StepCard("3. Состав вопросов") {
                    if (state.topicId == null) {
                        Text("Выберите тему. После этого можно точно настроить состав вопросов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        StatGrid(
                            listOf(
                                "Активных в теме" to counts.total.toString(),
                                "Один вариант" to counts.single.toString(),
                                "Несколько" to counts.multiple.toString(),
                                "Сопоставление" to counts.matching.toString(),
                                "Текстовые" to counts.text.toString(),
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(f.questionCount, "Всего вопросов", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(questionCount = v) } }
                        NumberField(f.attemptsAllowed, "Попыток", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(attemptsAllowed = v) } }
                    }
                    Text("Фиксированное количество по типам (остальные выбираются случайно):",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(f.singleCount, "Один вариант", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(singleCount = v) } }
                        NumberField(f.multipleCount, "Несколько", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(multipleCount = v) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(f.matchingCount, "Сопоставление", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(matchingCount = v) } }
                        NumberField(f.textCount, "Текстовые", Modifier.weight(1f)) { v -> vm.updateForm { it.copy(textCount = v) } }
                    }
                    val automatic = ((f.questionCount.toIntOrNull() ?: 0) - vm.fixedCount(f)).coerceAtLeast(0)
                    Text("Фиксировано по типам: ${vm.fixedCount(f)}. Остальные случайно: $automatic.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                StepCard("4. Назначение") {
                    SelectField(
                        "Статус назначения",
                        TestAssignmentStatus.all.map { SelectOption(it, TestAssignmentStatus.title(it)) },
                        f.status,
                        { s -> if (s != null) vm.updateForm { it.copy(status = s) } },
                    )
                    DateField("Доступен с", f.availableFrom, { d -> vm.updateForm { it.copy(availableFrom = d) } })
                    DateField("Доступен до (включительно)", f.availableUntil, { d -> vm.updateForm { it.copy(availableUntil = d) } })
                }
            }
            state.notice?.let { (tone, text) -> item { Banner(text, tone) } }
            item {
                BusyButton("Создать тест", onClick = vm::create, busy = state.saving, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StepCard(title: String, content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun NumberField(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    FormField(value, { onChange(it.filter(Char::isDigit).take(4)) }, label, keyboardType = KeyboardType.Number, modifier = modifier)
}
