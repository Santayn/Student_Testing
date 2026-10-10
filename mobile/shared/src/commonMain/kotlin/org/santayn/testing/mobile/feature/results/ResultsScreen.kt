package org.santayn.testing.mobile.feature.results

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatGrid
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatDateTime
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.api.LecturesApi
import org.santayn.testing.mobile.data.api.ResultsApi
import org.santayn.testing.mobile.data.api.SubjectsApi
import org.santayn.testing.mobile.data.model.ResultAttemptDto
import org.santayn.testing.mobile.data.model.ResultDataDto
import org.santayn.testing.mobile.domain.GradingStatus
import org.santayn.testing.mobile.domain.bestAttempt
import org.santayn.testing.mobile.domain.gradingStatusNormalized
import org.santayn.testing.mobile.domain.scoreSummary
import org.santayn.testing.mobile.domain.score
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.feature.tests.AnswerView
import org.santayn.testing.mobile.navigation.AppNavigator

/*
 * Результаты — views/results/ResultsView.vue, composables/results.
 */

data class ResultFilters(
    val subjects: List<SelectOption<Int>> = emptyList(),
    val lectures: List<SelectOption<Int>> = emptyList(),
    val tests: List<SelectOption<Int>> = emptyList(),
    val groups: List<SelectOption<Int>> = emptyList(),
    val students: List<SelectOption<Int>> = emptyList(),
    val subjectId: Int? = null,
    val lectureId: Int? = null,
    val testId: Int? = null,
    val groupId: Int? = null,
    val studentId: Int? = null,
    val loadingOptions: Boolean = false,
)

data class ResultsUiState(
    val filters: ResultFilters = ResultFilters(),
    val loading: Boolean = false,
    val error: String? = null,
    val data: ResultDataDto? = null,
    /** Для студента: тесты из полученных попыток. */
    val studentTests: List<SelectOption<Int>> = emptyList(),
)

class ResultsViewModel(
    private val role: WorkspaceRole,
    private val results: ResultsApi,
    private val subjectsApi: SubjectsApi,
    private val lecturesApi: LecturesApi,
) : BaseViewModel() {

    val teacherMode = role != WorkspaceRole.STUDENT
    private val isAdmin = role == WorkspaceRole.ADMIN

    private val _state = MutableStateFlow(ResultsUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadSubjects()
        if (!teacherMode) loadResults()
    }

    /** Связь восстановилась: догружаем то, что не загрузилось, и обновляем списки. */
    fun reloadAfterReconnect() {
        val current = _state.value
        if (current.filters.subjects.isEmpty()) loadSubjects()
        if (!teacherMode || current.data != null || current.error != null) loadResults()
    }

    private fun setFilters(transform: (ResultFilters) -> ResultFilters) =
        _state.update { it.copy(filters = transform(it.filters)) }

    private fun loadSubjects() {
        setFilters { it.copy(loadingOptions = true) }
        launchSafe(onError = {
            setFilters { f -> f.copy(loadingOptions = false) }
            toast(it.userMessage("Не удалось загрузить предметы"))
        }) {
            val options = when {
                !teacherMode -> results.getStudentSubjects().map { SelectOption(it.id, it.name) }
                isAdmin -> subjectsApi.getAll().sortedByName { it.name }.map { SelectOption(it.id, it.name) }
                else -> results.getTeacherSubjects().map { SelectOption(it.id, it.name) }
            }
            setFilters { it.copy(subjects = options, loadingOptions = false) }
        }
    }

    fun selectSubject(id: Int?) {
        setFilters {
            it.copy(subjectId = id, lectureId = null, testId = null, groupId = null, studentId = null,
                lectures = emptyList(), tests = emptyList(), groups = emptyList(), students = emptyList())
        }
        _state.update { it.copy(data = null) }
        if (!teacherMode) {
            setFilters { it.copy(testId = null) }
            loadResults()
            return
        }
        if (id == null) return
        loadOptions {
            val lectures = if (isAdmin) {
                lecturesApi.getAll(subjectId = id).sortedBy { it.ordinal }.map { SelectOption(it.id, "${it.ordinal}. ${it.title}") }
            } else {
                results.getTeacherLectures(id).sortedBy { it.ordinal }.map {
                    SelectOption(it.id, "${it.ordinal}. ${it.title}", it.courseName?.let { c -> "$c, версия ${it.versionNumber}" })
                }
            }
            setFilters { it.copy(lectures = lectures) }
        }
    }

    fun selectLecture(id: Int?) {
        setFilters { it.copy(lectureId = id, testId = null, groupId = null, studentId = null, tests = emptyList(), groups = emptyList(), students = emptyList()) }
        _state.update { it.copy(data = null) }
        if (id == null) return
        loadOptions {
            val tests = if (isAdmin) {
                lecturesApi.getTests(id).map { SelectOption(it.id, it.title) }
            } else {
                results.getTeacherTests(id).map { SelectOption(it.id, it.title) }
            }
            setFilters { it.copy(tests = tests) }
        }
    }

    fun selectTest(id: Int?) {
        if (!teacherMode) {
            setFilters { it.copy(testId = id) }
            loadResults()
            return
        }
        setFilters { it.copy(testId = id, groupId = null, studentId = null, groups = emptyList(), students = emptyList()) }
        _state.update { it.copy(data = null) }
        if (id == null) return
        loadOptions {
            val groups = results.getTeacherGroups(id).map { SelectOption(it.id, it.name, it.code) }
            setFilters { it.copy(groups = groups) }
        }
    }

    fun selectGroup(id: Int?) {
        setFilters { it.copy(groupId = id, studentId = null, students = emptyList()) }
        _state.update { it.copy(data = null) }
        if (id == null) return
        loadOptions {
            val students = results.getTeacherStudents(id).map { SelectOption(it.id, it.displayName, it.email) }
            setFilters { it.copy(students = students) }
        }
    }

    fun selectStudent(id: Int?) {
        setFilters { it.copy(studentId = id) }
        _state.update { it.copy(data = null) }
    }

    private fun loadOptions(block: suspend () -> Unit) {
        setFilters { it.copy(loadingOptions = true) }
        launchSafe(onError = {
            setFilters { f -> f.copy(loadingOptions = false) }
            toast(it.userMessage("Не удалось загрузить варианты фильтра"))
        }) {
            block()
            setFilters { it.copy(loadingOptions = false) }
        }
    }

    fun loadResults() {
        val f = _state.value.filters
        loadJob?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        loadJob = launchSafe(onError = {
            _state.update { s -> s.copy(loading = false, error = it.userMessage("Не удалось загрузить результаты тестирования.")) }
        }) {
            val data = if (teacherMode) {
                results.getTeacherData(f.subjectId, f.lectureId, f.testId, f.groupId, f.studentId)
            } else {
                results.getStudentData(subjectId = f.subjectId, testId = f.testId)
            }
            _state.update { s ->
                val tests = if (!teacherMode && f.testId == null) {
                    data.attempts.mapNotNull { a -> a.testId?.let { SelectOption(it, a.testName ?: "Тест #$it") } }
                        .distinctBy { it.value }
                } else {
                    s.studentTests
                }
                s.copy(loading = false, data = data, studentTests = tests)
            }
        }
    }
}

@Composable
fun ResultsScreen(role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<ResultsViewModel> { parametersOf(role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val f = state.filters

    AppScreen(title = "Результаты", onBack = if (role == WorkspaceRole.ADMIN) navigator::back else null) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    Text(
                        if (vm.teacherMode) {
                            "Выберите предмет, лекцию, тест, группу и студента и нажмите «Показать результаты»."
                        } else {
                            "Необязательно: сузьте список до предмета или конкретного теста."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SelectField("Предмет", f.subjects, f.subjectId, vm::selectSubject, allowClear = true, placeholder = "Все предметы")
                    if (vm.teacherMode) {
                        SelectField("Лекция", f.lectures, f.lectureId, vm::selectLecture, allowClear = true,
                            enabled = f.subjectId != null, placeholder = "Все лекции")
                        SelectField("Тест", f.tests, f.testId, vm::selectTest, allowClear = true,
                            enabled = f.lectureId != null, placeholder = "Все тесты")
                        SelectField("Группа", f.groups, f.groupId, vm::selectGroup, allowClear = true,
                            enabled = f.testId != null, placeholder = "Все группы")
                        SelectField("Студент", f.students, f.studentId, vm::selectStudent, allowClear = true,
                            enabled = f.groupId != null, placeholder = "Все студенты")
                        Spacer(Modifier.size(4.dp))
                        BusyButton(
                            "Показать результаты",
                            onClick = vm::loadResults,
                            busy = state.loading,
                            enabled = f.subjectId != null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        SelectField("Тест", state.studentTests, f.testId, vm::selectTest, allowClear = true,
                            placeholder = "Все тесты")
                    }
                }
            }

            if (state.error != null) item { Banner(state.error!!, Tone.DANGER) }

            val data = state.data
            if (state.loading && data == null) {
                item { LoadingState(Modifier.fillMaxWidth()) }
            } else if (data != null) {
                item { SummaryCard(data, vm.teacherMode, f.testId != null) }
                if (data.attempts.isEmpty()) {
                    item { EmptyState("Попыток нет", message = "Завершённых попыток по выбранным фильтрам нет.", icon = Icons.Default.Assessment) }
                } else {
                    item { SectionTitle("Попытки (${data.attempts.size})") }
                    items(data.attempts, key = { it.attemptId }) { attempt ->
                        AttemptCard(attempt, showStudent = vm.teacherMode)
                    }
                }
            } else if (vm.teacherMode) {
                item { EmptyState("Результаты не загружены", message = "Выберите фильтры и нажмите «Показать результаты».") }
            }
        }
    }
}

@Composable
private fun SummaryCard(data: ResultDataDto, teacherMode: Boolean, testSelected: Boolean) {
    AppCard {
        listOfNotNull(data.selectedTestName, data.selectedGroupName, data.selectedStudentName)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { Text(it.joinToString(" · "), style = MaterialTheme.typography.titleSmall) }
        val stats = data.stats
        if (!teacherMode && testSelected) {
            val best = bestAttempt(data.attempts)
            if (best != null) {
                val s = best.scoreSummary()
                Text("Лучшая попытка №${best.attemptOrdinal}", style = MaterialTheme.typography.titleMedium)
                StatGrid(
                    listOf(
                        "Баллы" to "${formatNumber(s.score)} из ${formatNumber(s.maxScore)}",
                        "Процент" to "${formatNumber(s.percent)}%",
                        "Полностью верных" to "${stats?.right ?: 0} из ${stats?.total ?: 0}",
                        "Попыток" to "${data.attempts.size}",
                    )
                )
                return@AppCard
            }
        }
        StatGrid(
            listOf(
                "Попыток" to "${data.attempts.size}",
                "Правильных" to "${stats?.right ?: 0} из ${stats?.total ?: 0}",
                "Процент" to "${formatNumber(stats?.percent)}%",
            )
        )
        if (!teacherMode && !testSelected && data.attempts.isNotEmpty()) {
            Text(
                "Выберите конкретный тест — итог будет показан по его лучшей попытке.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Попытка с раскрывающимися ответами (components/results/ResultAttemptCard.vue). */
@Composable
private fun AttemptCard(attempt: ResultAttemptDto, showStudent: Boolean) {
    var expanded by rememberSaveable(attempt.attemptId) { mutableStateOf(false) }
    val summary = attempt.scoreSummary()
    AppCard(
        onClick = { expanded = !expanded },
        modifier = Modifier.semantics { stateDescription = if (expanded) "подробности открыты" else "подробности скрыты" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(attempt.testName ?: "Тест", style = MaterialTheme.typography.titleMedium)
                val meta = listOfNotNull(
                    if (showStudent) attempt.studentName else null,
                    "Попытка №${attempt.attemptOrdinal}",
                    attempt.completedAt?.let { formatDateTime(it) },
                ).joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusChip(
                "${formatNumber(summary.score)} / ${formatNumber(summary.maxScore)} (${formatNumber(summary.percent)}%)",
                when {
                    summary.percent >= 80 -> Tone.SUCCESS
                    summary.percent >= 50 -> Tone.WARNING
                    else -> Tone.DANGER
                },
            )
            if (summary.correct > 0) StatusChip("Верно: ${summary.correct}", Tone.SUCCESS)
            if (summary.partial > 0) StatusChip("Частично: ${summary.partial}", Tone.WARNING)
            if (summary.incorrect > 0) StatusChip("Неверно: ${summary.incorrect}", Tone.DANGER)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                attempt.results.forEachIndexed { index, item ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val status = item.gradingStatusNormalized()
                    val score = item.score()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Вопрос ${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        StatusChip(
                            "${status.label} · ${formatNumber(score.awarded)}/${formatNumber(score.maximum)}",
                            when (status) {
                                GradingStatus.CORRECT -> Tone.SUCCESS
                                GradingStatus.PARTIAL -> Tone.WARNING
                                GradingStatus.INCORRECT -> Tone.DANGER
                            },
                        )
                    }
                    Text(item.questionText.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    AnswerView(item.givenAnswer, item.correctAnswer)
                    if (!item.gradingNote.isNullOrBlank()) {
                        Text(
                            item.gradingNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
