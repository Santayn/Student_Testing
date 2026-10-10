package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.files.toUploadFile
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CheckRow
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.SwitchRow
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.api.QuestionsApi
import org.santayn.testing.mobile.data.api.TopicsApi
import org.santayn.testing.mobile.data.model.MatchingPairDto
import org.santayn.testing.mobile.data.model.OptionRequest
import org.santayn.testing.mobile.data.model.QuestionDto
import org.santayn.testing.mobile.data.model.QuestionOptionDto
import org.santayn.testing.mobile.data.model.QuestionRequest
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.QuestionUpdateRequest
import org.santayn.testing.mobile.data.model.TopicDto
import org.santayn.testing.mobile.domain.matchingPairsValidationMessage
import org.santayn.testing.mobile.domain.normalizeMatchingPairs
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.TeacherQuestionsRoute
import org.santayn.testing.mobile.navigation.TeacherTestCreateRoute

/*
 * Банк вопросов — views/teacher/QuestionsView.vue и composables/questions.
 */

data class QuestionForm(
    val id: Long? = null,
    val question: String = "",
    val type: Int = QuestionType.SINGLE,
    val points: String = "1",
    val ordinal: String = "1",
    val correctAnswer: String = "",
    val pairs: List<MatchingPairDto> = listOf(MatchingPairDto(1), MatchingPairDto(2)),
    val active: Boolean = true,
)

data class OptionForm(
    val id: Long? = null,
    val text: String = "",
    val ordinal: String = "1",
    val correct: Boolean = false,
)

enum class ActiveFilter(val label: String) { ALL("Все"), ACTIVE("Активные"), HIDDEN("Скрытые") }

data class QuestionsUiState(
    val topics: List<TopicDto> = emptyList(),
    val topicId: Int? = null,
    val loading: Boolean = false,
    val questions: List<QuestionDto> = emptyList(),
    val error: String? = null,
    val form: QuestionForm? = null,
    val formError: String? = null,
    val saving: Boolean = false,
    val options: List<QuestionOptionDto> = emptyList(),
    val optionsLoading: Boolean = false,
    val optionForm: OptionForm? = null,
    val optionError: String? = null,
    val optionSaving: Boolean = false,
    val importOpen: Boolean = false,
    val importFile: PlatformFile? = null,
    val importing: Boolean = false,
    val importResult: String? = null,
)

class QuestionsViewModel(
    private val route: TeacherQuestionsRoute,
    val subjects: TeacherSubjectsHolder,
    private val topicsApi: TopicsApi,
    private val questionsApi: QuestionsApi,
) : BaseViewModel() {
    private val _state = MutableStateFlow(QuestionsUiState())
    val state = _state.asStateFlow()

    init {
        launchSafe {
            subjects.load(preferredSubjectId = route.subjectId)
            loadTopics(route.topicId)
        }
    }

    fun selectMembership(id: Int?) {
        subjects.select(id)
        launchSafe { loadTopics(null) }
    }

    private suspend fun loadTopics(preferredTopicId: Int?) {
        val membershipId = subjects.state.value.selectedMembershipId
        if (membershipId == null) {
            _state.update { it.copy(topics = emptyList(), topicId = null, questions = emptyList()) }
            return
        }
        val topics = topicsApi.getAll(subjectMembershipId = membershipId).sortedWith(compareBy({ it.ordinal }, { it.id }))
        val topicId = topics.firstOrNull { it.id == preferredTopicId }?.id ?: topics.singleOrNull()?.id
        _state.update { it.copy(topics = topics, topicId = topicId, questions = emptyList()) }
        loadQuestions()
    }

    fun selectTopic(id: Int?) {
        _state.update { it.copy(topicId = id, questions = emptyList()) }
        launchSafe { loadQuestions() }
    }

    fun refresh() = launchSafe { loadQuestions() }

    /** Связь восстановилась: догружаем то, что не загрузилось, и обновляем списки. */
    fun reloadAfterReconnect() = launchSafe {
        if (subjects.needsLoad) {
            subjects.load(preferredSubjectId = route.subjectId)
            loadTopics(route.topicId)
        } else {
            loadQuestions()
        }
    }

    private suspend fun loadQuestions() {
        val topicId = _state.value.topicId ?: return
        _state.update { it.copy(loading = true, error = null) }
        try {
            val list = questionsApi.getAll(topicId = topicId).sortedWith(compareBy({ it.ordinal }, { it.id }))
            _state.update { it.copy(loading = false, questions = list) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    // region question editor

    fun openCreate() {
        val next = (_state.value.questions.maxOfOrNull { it.ordinal } ?: 0) + 1
        _state.update { it.copy(form = QuestionForm(ordinal = next.toString()), formError = null, options = emptyList()) }
    }

    fun openEdit(q: QuestionDto) {
        _state.update {
            it.copy(
                form = QuestionForm(
                    id = q.id,
                    question = q.question,
                    type = q.type,
                    points = formatNumber(q.points ?: 1.0),
                    ordinal = q.ordinal.toString(),
                    correctAnswer = q.correctAnswer.orEmpty(),
                    pairs = q.matchingPairs.sortedBy { p -> p.ordinal }.ifEmpty { listOf(MatchingPairDto(1), MatchingPairDto(2)) },
                    active = q.active,
                ),
                formError = null,
                options = emptyList(),
            )
        }
        if (QuestionType.usesOptions(q.type)) loadOptions(q.id)
    }

    fun updateForm(transform: (QuestionForm) -> QuestionForm) = _state.update { s -> s.copy(form = s.form?.let(transform)) }

    fun closeForm() = _state.update { it.copy(form = null, formError = null, optionForm = null) }

    fun saveQuestion() {
        val form = _state.value.form ?: return
        val topicId = _state.value.topicId
        val points = form.points.replace(',', '.').toDoubleOrNull()
        val ordinal = form.ordinal.toIntOrNull()
        val error = when {
            subjects.state.value.selected == null || topicId == null -> "Выберите предмет и тему."
            form.question.isBlank() -> "Введите текст вопроса."
            form.question.trim().length > 2000 -> "Текст вопроса не может быть длиннее 2000 символов."
            points == null || points < 0 -> "Количество баллов должно быть числом не меньше нуля."
            ordinal == null || ordinal <= 0 -> "Порядковый номер должен быть целым числом больше нуля."
            form.type == QuestionType.MATCHING -> matchingPairsValidationMessage(form.pairs)
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(formError = error) }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage("Не удалось сохранить вопрос")) } }) {
            subjects.ensureSelectedActive()
            val correct = if (form.type == QuestionType.TEXT) form.correctAnswer.trim().ifBlank { null } else null
            val pairs = if (form.type == QuestionType.MATCHING) normalizeMatchingPairs(form.pairs) else emptyList()
            val saved = if (form.id == null) {
                questionsApi.create(
                    QuestionRequest(null, null, topicId, form.type, form.question.trim(), points, ordinal!!, correct, pairs)
                )
            } else {
                questionsApi.update(
                    form.id,
                    QuestionUpdateRequest(null, topicId, form.type, form.question.trim(), points, ordinal!!, correct, pairs, form.active),
                )
            }
            val wasCreate = form.id == null
            _state.update { it.copy(saving = false, form = it.form?.copy(id = saved.id)) }
            toast(if (wasCreate) "Вопрос создан" else "Вопрос сохранён")
            loadQuestions()
            if (wasCreate && QuestionType.usesOptions(saved.type)) {
                toast("Теперь добавьте варианты ответа")
                loadOptions(saved.id)
            } else if (!QuestionType.usesOptions(saved.type)) {
                closeForm()
            }
        }
    }

    fun toggleActive(q: QuestionDto) {
        launchSafe {
            questionsApi.updateActive(q.id, !q.active)
            _state.update { s -> s.copy(questions = s.questions.map { if (it.id == q.id) it.copy(active = !q.active) else it }) }
            toast(if (q.active) "Вопрос скрыт" else "Вопрос активирован")
        }
    }

    // endregion

    // region options

    private fun loadOptions(questionId: Long) {
        _state.update { it.copy(optionsLoading = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(optionsLoading = false) }
            toast(e.userMessage("Не удалось загрузить варианты"))
        }) {
            val options = questionsApi.getOptions(questionId).sortedWith(compareBy({ it.ordinal }, { it.id }))
            _state.update { it.copy(optionsLoading = false, options = options) }
        }
    }

    fun openOption(option: QuestionOptionDto?) {
        val next = (_state.value.options.maxOfOrNull { it.ordinal } ?: 0) + 1
        _state.update {
            it.copy(
                optionForm = option?.let { o -> OptionForm(o.id, o.text, o.ordinal.toString(), o.correct) }
                    ?: OptionForm(ordinal = next.toString()),
                optionError = null,
            )
        }
    }

    fun updateOption(transform: (OptionForm) -> OptionForm) = _state.update { s -> s.copy(optionForm = s.optionForm?.let(transform)) }

    fun closeOption() = _state.update { it.copy(optionForm = null, optionError = null) }

    fun saveOption() {
        val questionId = _state.value.form?.id ?: return
        val form = _state.value.optionForm ?: return
        val ordinal = form.ordinal.toIntOrNull()
        val error = when {
            form.text.isBlank() -> "Введите текст варианта."
            ordinal == null || ordinal <= 0 -> "Порядковый номер должен быть больше нуля."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(optionError = error) }
            return
        }
        _state.update { it.copy(optionSaving = true) }
        launchSafe(onError = { e -> _state.update { it.copy(optionSaving = false, optionError = e.userMessage()) } }) {
            val request = OptionRequest(form.text.trim(), ordinal!!, form.correct)
            if (form.id == null) questionsApi.createOption(questionId, request) else questionsApi.updateOption(form.id, request)
            _state.update { it.copy(optionSaving = false, optionForm = null) }
            loadOptions(questionId)
        }
    }

    // endregion

    // region import

    fun openImport(open: Boolean) = _state.update { it.copy(importOpen = open, importFile = null, importResult = null) }

    fun setImportFile(file: PlatformFile?) = _state.update { it.copy(importFile = file) }

    fun runImport() {
        val file = _state.value.importFile ?: return
        val topicId = _state.value.topicId
        if (topicId == null) {
            toast("Выберите тему для импорта")
            return
        }
        if (!file.name.endsWith(".docx", ignoreCase = true)) {
            toast("Поддерживаются только файлы .docx")
            return
        }
        _state.update { it.copy(importing = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(importing = false) }
            toast(e.userMessage("Не удалось импортировать вопросы"))
        }) {
            val result = questionsApi.importFile(file.toUploadFile(), topicId)
            _state.update {
                it.copy(
                    importing = false,
                    importResult = "Импортировано вопросов: ${result.importedQuestions}, вариантов: ${result.importedOptions}.",
                )
            }
            loadQuestions()
        }
    }

    // endregion
}

@Composable
fun QuestionsScreen(route: TeacherQuestionsRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<QuestionsViewModel>(key = "questions-${route.subjectId}-${route.topicId}") { parametersOf(route, role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val picker by vm.subjects.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    var activeFilter by rememberSaveable { mutableStateOf(ActiveFilter.ALL) }

    AppScreen(
        title = "Вопросы",
        onBack = navigator::back,
        actions = {
            if (state.topicId != null) {
                IconButton(onClick = { vm.openImport(true) }) { Icon(Icons.Default.UploadFile, "Импорт из Word") }
            }
        },
        floatingActionButton = {
            if (state.topicId != null) {
                ExtendedFloatingActionButton(onClick = vm::openCreate, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Вопрос") })
            }
        },
    ) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                TeacherSubjectSelector(picker, vm::selectMembership, vm.subjects.isAdmin) {
                    if (picker.selected != null) {
                        SelectField(
                            label = "Тема",
                            options = state.topics.map { SelectOption(it.id, "${it.ordinal}. ${it.name}") },
                            selected = state.topicId,
                            onSelect = vm::selectTopic,
                            placeholder = if (state.topics.isEmpty()) "Сначала создайте тему" else "Выберите тему",
                        )
                    }
                }
            }
            if (state.topicId != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchField(query, { query = it }, placeholder = "Поиск по тексту вопроса")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(typeFilter == null, { typeFilter = null }, { Text("Все типы") })
                            QuestionType.all.forEach { t ->
                                FilterChip(typeFilter == t, { typeFilter = if (typeFilter == t) null else t }, { Text(QuestionType.title(t)) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ActiveFilter.entries.forEach { f ->
                                FilterChip(activeFilter == f, { activeFilter = f }, { Text(f.label) })
                            }
                        }
                        OutlinedButton(onClick = { navigator.open(TeacherTestCreateRoute(picker.selectedSubjectId, state.topicId)) }) {
                            Text("Создать тест по теме")
                        }
                    }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { Banner(state.error!!, Tone.DANGER) }
                state.topicId != null && state.questions.isEmpty() -> item {
                    EmptyState("Вопросов пока нет", message = "Добавьте вопрос или импортируйте их из Word.", icon = Icons.Default.Quiz)
                }
            }
            val filtered = state.questions.filter { q ->
                (query.isBlank() || q.question.contains(query.trim(), true)) &&
                    (typeFilter == null || q.type == typeFilter) &&
                    when (activeFilter) {
                        ActiveFilter.ALL -> true
                        ActiveFilter.ACTIVE -> q.active
                        ActiveFilter.HIDDEN -> !q.active
                    }
            }
            if (state.questions.isNotEmpty()) item { SectionTitle("Найдено: ${filtered.size} из ${state.questions.size}") }
            items(filtered, key = { it.id }) { q ->
                AppCard(onClick = { vm.openEdit(q) }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusChip("№${q.ordinal}", Tone.PRIMARY)
                        StatusChip(QuestionType.title(q.type))
                        StatusChip("Баллы: ${formatNumber(q.points)}")
                        StatusChip(if (q.active) "Активен" else "Скрыт", if (q.active) Tone.SUCCESS else Tone.WARNING)
                    }
                    Text(q.question, style = MaterialTheme.typography.bodyLarge, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Показывать в тестах", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = q.active, onCheckedChange = { vm.toggleActive(q) })
                    }
                }
            }
        }
    }

    state.form?.let { form -> QuestionEditor(form, state, vm) }

    if (state.importOpen) ImportDialog(state, vm)
}

@Composable
private fun QuestionEditor(form: QuestionForm, state: QuestionsUiState, vm: QuestionsViewModel) {
    EditorDialog(
        title = if (form.id == null) "Новый вопрос" else "Вопрос №${form.ordinal}",
        onDismiss = vm::closeForm,
        onSave = vm::saveQuestion,
        busy = state.saving,
        error = state.formError,
    ) {
        FormField(form.question, { v -> vm.updateForm { it.copy(question = v) } }, "Текст вопроса",
            singleLine = false, minLines = 3, maxLength = 2000, required = true)
        SelectField(
            label = "Тип",
            options = QuestionType.all.map { SelectOption(it, QuestionType.title(it)) },
            selected = form.type,
            onSelect = { t -> if (t != null) vm.updateForm { it.copy(type = t) } },
            enabled = form.id == null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.points, { v -> vm.updateForm { it.copy(points = v) } }, "Баллы",
                keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            FormField(form.ordinal, { v -> vm.updateForm { it.copy(ordinal = v.filter(Char::isDigit)) } }, "Порядок",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        if (form.id != null) {
            SwitchRow("Активен", form.active, { v -> vm.updateForm { it.copy(active = v) } },
                description = "Скрытые вопросы не попадают в новые попытки")
        }
        when (form.type) {
            QuestionType.TEXT -> FormField(
                form.correctAnswer, { v -> vm.updateForm { it.copy(correctAnswer = v) } }, "Правильный ответ",
                singleLine = false, minLines = 2,
                supportingText = "Можно указать несколько допустимых вариантов через символ | или с новой строки.",
            )
            QuestionType.MATCHING -> MatchingPairsEditor(form.pairs) { pairs -> vm.updateForm { it.copy(pairs = pairs) } }
            else -> OptionsSection(form, state, vm)
        }
    }

    state.optionForm?.let { option ->
        AlertDialog(
            onDismissRequest = vm::closeOption,
            title = { Text(if (option.id == null) "Новый вариант" else "Вариант ответа") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.optionError != null) Banner(state.optionError, Tone.DANGER)
                    FormField(option.text, { v -> vm.updateOption { it.copy(text = v) } }, "Текст", singleLine = false, minLines = 2)
                    FormField(option.ordinal, { v -> vm.updateOption { it.copy(ordinal = v.filter(Char::isDigit)) } }, "Порядок",
                        keyboardType = KeyboardType.Number)
                    CheckRow("Правильный вариант", option.correct, { v -> vm.updateOption { it.copy(correct = v) } })
                }
            },
            confirmButton = {
                TextButton(onClick = vm::saveOption, enabled = !state.optionSaving) { Text(if (state.optionSaving) "Сохранение…" else "Сохранить") }
            },
            dismissButton = { TextButton(onClick = vm::closeOption) { Text("Отмена") } },
        )
    }
}

@Composable
private fun OptionsSection(form: QuestionForm, state: QuestionsUiState, vm: QuestionsViewModel) {
    SectionTitle("Варианты ответа")
    if (form.id == null) {
        Banner("Сначала сохраните вопрос — после этого можно добавить варианты ответа.", Tone.INFO)
        return
    }
    if (state.optionsLoading) LoadingState(Modifier.fillMaxWidth())
    if (form.type == QuestionType.SINGLE && state.options.count { it.correct } > 1) {
        Banner("Для вопроса с одним вариантом должен быть отмечен ровно один правильный ответ.", Tone.WARNING)
    }
    state.options.forEach { option ->
        AppCard(onClick = { vm.openOption(option) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${option.ordinal}.", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Text(option.text, modifier = Modifier.weight(1f))
                if (option.correct) StatusChip("Верный", Tone.SUCCESS)
            }
        }
    }
    OutlinedButton(onClick = { vm.openOption(null) }) {
        Icon(Icons.Default.Add, null)
        Text(" Добавить вариант")
    }
}

/** Редактор пар соответствия (components/questions/MatchingPairsEditor.vue). */
@Composable
fun MatchingPairsEditor(pairs: List<MatchingPairDto>, onChange: (List<MatchingPairDto>) -> Unit) {
    SectionTitle("Пары соответствия")
    Text(
        "Слева — элементы колонки А, справа — соответствующие элементы колонки Б. Минимум две пары.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    pairs.forEachIndexed { index, pair ->
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Пара ${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (pairs.size > 2) {
                    IconButton(onClick = {
                        onChange(pairs.filterIndexed { i, _ -> i != index }.mapIndexed { i, p -> p.copy(ordinal = i + 1) })
                    }) { Icon(Icons.Default.Delete, "Удалить пару") }
                }
            }
            FormField(pair.left, { v -> onChange(pairs.mapIndexed { i, p -> if (i == index) p.copy(left = v) else p }) }, "Колонка А")
            FormField(pair.right, { v -> onChange(pairs.mapIndexed { i, p -> if (i == index) p.copy(right = v) else p }) }, "Колонка Б")
        }
    }
    OutlinedButton(onClick = { onChange(pairs + MatchingPairDto(pairs.size + 1)) }) {
        Icon(Icons.Default.Add, null)
        Text(" Добавить пару")
    }
}

@Composable
private fun ImportDialog(state: QuestionsUiState, vm: QuestionsViewModel) {
    val picker = rememberFilePickerLauncher(type = FileKitType.File(listOf("docx"))) { file -> vm.setImportFile(file) }
    AlertDialog(
        onDismissRequest = { if (!state.importing) vm.openImport(false) },
        title = { Text("Импорт из Word") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Вопросы из файла .docx будут добавлены в выбранную тему. " +
                        "Правильные варианты отмечаются знаком * или + в конце, (+), [x] или словом «(правильно)».",
                    style = MaterialTheme.typography.bodySmall,
                )
                val topic = state.topics.firstOrNull { it.id == state.topicId }
                Text("Тема: ${topic?.name ?: "—"}", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { picker.launch() }, enabled = !state.importing) {
                    Icon(Icons.Default.UploadFile, null)
                    Text(" ${state.importFile?.name ?: "Выбрать файл .docx"}")
                }
                if (state.importResult != null) Banner(state.importResult, Tone.SUCCESS)
            }
        },
        confirmButton = {
            if (state.importResult == null) {
                BusyButton("Импортировать", onClick = vm::runImport, busy = state.importing, enabled = state.importFile != null)
            } else {
                TextButton(onClick = { vm.openImport(false) }) { Text("Готово") }
            }
        },
        dismissButton = {
            if (state.importResult == null) TextButton(onClick = { vm.openImport(false) }, enabled = !state.importing) { Text("Отмена") }
        },
    )
}
