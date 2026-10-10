package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.data.api.TopicsApi
import org.santayn.testing.mobile.data.model.TopicDto
import org.santayn.testing.mobile.data.model.TopicRequest
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.TeacherQuestionsRoute
import org.santayn.testing.mobile.navigation.TeacherTestCreateRoute
import org.santayn.testing.mobile.navigation.TeacherTopicsRoute

/*
 * Темы предмета — views/teacher/TopicLibraryView.vue.
 */

data class TopicForm(
    val id: Int? = null,
    val ordinal: String = "1",
    val name: String = "",
    val description: String = "",
)

data class TopicsUiState(
    val loading: Boolean = false,
    val topics: List<TopicDto> = emptyList(),
    val error: String? = null,
    val form: TopicForm? = null,
    val formError: String? = null,
    val saving: Boolean = false,
    val deleting: TopicDto? = null,
    val deleteBusy: Boolean = false,
)

class TopicsViewModel(
    private val subjectId: Int?,
    val subjects: TeacherSubjectsHolder,
    private val topicsApi: TopicsApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TopicsUiState())
    val state = _state.asStateFlow()

    init {
        launchSafe(onError = { toast(it.userMessage()) }) {
            subjects.load(preferredSubjectId = subjectId)
            loadTopics()
        }
    }

    fun selectMembership(id: Int?) {
        subjects.select(id)
        launchSafe { loadTopics() }
    }

    fun refresh() = launchSafe { loadTopics() }

    /** Связь восстановилась: догружаем то, что не загрузилось, и обновляем списки. */
    fun reloadAfterReconnect() = launchSafe {
        if (subjects.needsLoad) subjects.load(preferredSubjectId = subjectId)
        loadTopics()
    }

    private suspend fun loadTopics() {
        val membershipId = subjects.state.value.selectedMembershipId
        if (membershipId == null) {
            _state.update { it.copy(topics = emptyList(), loading = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val topics = topicsApi.getAll(subjectMembershipId = membershipId)
                .sortedWith(compareBy({ it.ordinal }, { it.id }))
            _state.update { it.copy(loading = false, topics = topics) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun openCreate() {
        val next = (_state.value.topics.maxOfOrNull { it.ordinal } ?: 0) + 1
        _state.update { it.copy(form = TopicForm(ordinal = next.toString()), formError = null) }
    }

    fun openEdit(topic: TopicDto) = _state.update {
        it.copy(form = TopicForm(topic.id, topic.ordinal.toString(), topic.name, topic.description.orEmpty()), formError = null)
    }

    fun updateForm(transform: (TopicForm) -> TopicForm) = _state.update { s -> s.copy(form = s.form?.let(transform)) }

    fun closeForm() = _state.update { it.copy(form = null, formError = null) }

    fun save() {
        val form = _state.value.form ?: return
        val ordinal = form.ordinal.toIntOrNull()
        val error = when {
            form.name.isBlank() -> "Введите название темы."
            ordinal == null || ordinal <= 0 -> "Порядковый номер должен быть целым числом больше нуля."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(formError = error) }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e ->
            _state.update { it.copy(saving = false, formError = e.userMessage("Не удалось сохранить тему")) }
        }) {
            val membership = subjects.ensureSelectedActive()
            val request = TopicRequest(
                subjectId = membership.subjectId,
                courseLectureId = null,
                subjectMembershipId = membership.membershipId,
                ordinal = ordinal!!,
                name = form.name.trim(),
                description = form.description.trim().ifBlank { null },
            )
            if (form.id == null) topicsApi.create(request) else topicsApi.update(form.id, request)
            cache.invalidate()
            _state.update { it.copy(saving = false, form = null) }
            toast(if (form.id == null) "Тема создана" else "Тема сохранена")
            loadTopics()
        }
    }

    fun askDelete(topic: TopicDto?) = _state.update { it.copy(deleting = topic) }

    fun confirmDelete() {
        val topic = _state.value.deleting ?: return
        _state.update { it.copy(deleteBusy = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(deleteBusy = false, deleting = null) }
            toast(
                if (e is ApiException && (e.isConflict || e.status == 400)) {
                    "Тему нельзя удалить: ${e.message}"
                } else {
                    e.userMessage("Не удалось удалить тему")
                }
            )
        }) {
            topicsApi.remove(topic.id)
            _state.update { it.copy(deleteBusy = false, deleting = null) }
            toast("Тема удалена")
            loadTopics()
        }
    }
}

@Composable
fun TopicsScreen(route: TeacherTopicsRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<TopicsViewModel>(key = "topics-${route.subjectId}") { parametersOf(route, role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val picker by vm.subjects.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var sortByName by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = "Темы предмета",
        onBack = navigator::back,
        floatingActionButton = {
            if (picker.selected != null) {
                ExtendedFloatingActionButton(
                    onClick = vm::openCreate,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Тема") },
                )
            }
        },
    ) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item { TeacherSubjectSelector(picker, vm::selectMembership, vm.subjects.isAdmin) }
            if (picker.selected != null) {
                item {
                    SearchField(query, { query = it }, placeholder = "Поиск по названию и описанию")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { sortByName = !sortByName }) {
                            Text(if (sortByName) "Сортировка: по названию" else "Сортировка: по порядку")
                        }
                    }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { EmptyState("Ошибка загрузки", message = state.error) }
                picker.selected != null && state.topics.isEmpty() -> item {
                    EmptyState("Тем пока нет", message = "Создайте первую тему предмета.", icon = Icons.Default.Topic)
                }
            }
            val filtered = state.topics
                .filter {
                    query.isBlank() || it.name.contains(query.trim(), true) ||
                        it.description?.contains(query.trim(), true) == true
                }
                .let { list -> if (sortByName) list.sortedBy { it.name.lowercase() } else list }
            items(filtered, key = { it.id }) { topic ->
                AppCard {
                    Row {
                        StatusChip("№${topic.ordinal}", Tone.PRIMARY)
                    }
                    Text(topic.name, style = MaterialTheme.typography.titleMedium)
                    topic.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            navigator.open(TeacherQuestionsRoute(picker.selectedSubjectId, topic.id))
                        }) {
                            Icon(Icons.Default.Quiz, null)
                            Text(" Вопросы")
                        }
                        TextButton(onClick = {
                            navigator.open(TeacherTestCreateRoute(picker.selectedSubjectId, topic.id))
                        }) {
                            Icon(Icons.Default.PlaylistAddCheck, null)
                            Text(" Тест")
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.openEdit(topic) }) { Icon(Icons.Default.Edit, "Изменить") }
                        IconButton(onClick = { vm.askDelete(topic) }) { Icon(Icons.Default.Delete, "Удалить") }
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        EditorDialog(
            title = if (form.id == null) "Новая тема" else "Редактирование темы",
            onDismiss = vm::closeForm,
            onSave = vm::save,
            busy = state.saving,
            error = state.formError,
        ) {
            FormField(form.ordinal, { v -> vm.updateForm { it.copy(ordinal = v.filter(Char::isDigit)) } },
                "Порядковый номер", keyboardType = KeyboardType.Number, required = true)
            FormField(form.name, { v -> vm.updateForm { it.copy(name = v) } }, "Название", required = true, maxLength = 200)
            FormField(form.description, { v -> vm.updateForm { it.copy(description = v) } }, "Описание",
                singleLine = false, minLines = 3, maxLength = 2000)
        }
    }

    state.deleting?.let { topic ->
        ConfirmDialog(
            title = "Удалить тему?",
            message = "Тема «${topic.name}» будет удалена. Если к ней привязаны вопросы, сервер может отклонить удаление.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDelete,
            onDismiss = { vm.askDelete(null) },
        )
    }
}
