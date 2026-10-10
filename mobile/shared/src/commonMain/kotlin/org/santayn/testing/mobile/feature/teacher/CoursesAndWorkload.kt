package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadStateContent
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.NavCard
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatGrid
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.SwitchRow
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatDateTime
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.api.CoursesApi
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.TeachingApi
import org.santayn.testing.mobile.data.model.CourseTemplateDto
import org.santayn.testing.mobile.data.model.CourseTemplateRequest
import org.santayn.testing.mobile.data.model.CourseVersionDto
import org.santayn.testing.mobile.data.model.CourseVersionRequest
import org.santayn.testing.mobile.data.model.CourseVersionUpdateRequest
import org.santayn.testing.mobile.data.model.TeachingAssignmentDto
import org.santayn.testing.mobile.data.model.TeachingAssignmentStatus
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.TeacherContextLoader
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.SubjectDetailsRoute
import org.santayn.testing.mobile.navigation.TeacherCoursesRoute

// region Course templates (views/teacher/CourseTemplatesView.vue)

data class TemplateForm(val id: Int? = null, val name: String = "", val publicVisible: Boolean = false)

data class VersionForm(
    val id: Int? = null,
    val versionNumber: String = "1",
    val title: String = "",
    val description: String = "",
    val changeNotes: String = "",
    val publishNow: Boolean = false,
)

data class CoursesUiState(
    val loading: Boolean = false,
    val templates: List<CourseTemplateDto> = emptyList(),
    val selectedTemplate: CourseTemplateDto? = null,
    val versions: List<CourseVersionDto> = emptyList(),
    val versionsLoading: Boolean = false,
    val error: String? = null,
    val templateForm: TemplateForm? = null,
    val versionForm: VersionForm? = null,
    val formError: String? = null,
    val saving: Boolean = false,
    val deleting: CourseTemplateDto? = null,
    val deleteBusy: Boolean = false,
)

class CourseTemplatesViewModel(
    private val route: TeacherCoursesRoute,
    val subjects: TeacherSubjectsHolder,
    private val courses: CoursesApi,
) : BaseViewModel() {
    val isAdmin = subjects.isAdmin
    private val _state = MutableStateFlow(CoursesUiState())
    val state = _state.asStateFlow()

    init {
        launchSafe {
            subjects.load(preferredSubjectId = route.subjectId)
            loadTemplates()
        }
    }

    /** Связь восстановилась: догружаем то, что не загрузилось, и обновляем списки. */
    fun reloadAfterReconnect() = launchSafe {
        if (subjects.needsLoad) subjects.load(preferredSubjectId = route.subjectId)
        loadTemplates()
    }

    fun selectMembership(id: Int?) {
        subjects.select(id)
        _state.update { it.copy(selectedTemplate = null, versions = emptyList()) }
        launchSafe { loadTemplates() }
    }

    private suspend fun loadTemplates() {
        val subjectId = subjects.state.value.selectedSubjectId ?: return
        _state.update { it.copy(loading = true, error = null) }
        try {
            val list = courses.getTemplates(subjectId).sortedBy { it.name.lowercase() }
            _state.update { s ->
                s.copy(loading = false, templates = list, selectedTemplate = list.firstOrNull { it.id == s.selectedTemplate?.id })
            }
            _state.value.selectedTemplate?.let { loadVersions(it.id) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun selectTemplate(template: CourseTemplateDto?) {
        _state.update { it.copy(selectedTemplate = template, versions = emptyList()) }
        if (template != null) launchSafe { loadVersions(template.id) }
    }

    private suspend fun loadVersions(templateId: Int) {
        _state.update { it.copy(versionsLoading = true) }
        val versions = try {
            courses.getVersions(templateId).sortedByDescending { it.versionNumber }
        } finally {
            _state.update { it.copy(versionsLoading = false) }
        }
        _state.update { if (it.selectedTemplate?.id == templateId) it.copy(versions = versions) else it }
    }

    fun openTemplate(template: CourseTemplateDto?) = _state.update {
        it.copy(templateForm = template?.let { t -> TemplateForm(t.id, t.name, t.publicVisible) } ?: TemplateForm(), formError = null)
    }

    fun updateTemplateForm(transform: (TemplateForm) -> TemplateForm) =
        _state.update { s -> s.copy(templateForm = s.templateForm?.let(transform)) }

    fun saveTemplate() {
        val form = _state.value.templateForm ?: return
        val subjectId = subjects.state.value.selectedSubjectId
        if (form.name.isBlank() || subjectId == null) {
            _state.update { it.copy(formError = "Введите название шаблона.") }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage()) } }) {
            val request = CourseTemplateRequest(subjectId, form.name.trim(), form.publicVisible)
            if (form.id == null) courses.createTemplate(request) else courses.updateTemplate(form.id, request)
            _state.update { it.copy(saving = false, templateForm = null) }
            toast("Шаблон сохранён")
            loadTemplates()
        }
    }

    fun askDelete(template: CourseTemplateDto?) = _state.update { it.copy(deleting = template) }

    fun confirmDelete() {
        val template = _state.value.deleting ?: return
        _state.update { it.copy(deleteBusy = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(deleteBusy = false, deleting = null) }
            toast(e.userMessage("Не удалось удалить шаблон"))
        }) {
            courses.removeTemplate(template.id)
            _state.update { it.copy(deleteBusy = false, deleting = null, selectedTemplate = null, versions = emptyList()) }
            toast("Шаблон удалён")
            loadTemplates()
        }
    }

    fun openVersion(version: CourseVersionDto?) {
        val next = (_state.value.versions.maxOfOrNull { it.versionNumber } ?: 0) + 1
        _state.update {
            it.copy(
                versionForm = version?.let { v ->
                    VersionForm(v.id, v.versionNumber.toString(), v.title, v.description.orEmpty(), v.changeNotes.orEmpty())
                } ?: VersionForm(versionNumber = next.toString()),
                formError = null,
            )
        }
    }

    fun updateVersionForm(transform: (VersionForm) -> VersionForm) =
        _state.update { s -> s.copy(versionForm = s.versionForm?.let(transform)) }

    fun saveVersion() {
        val form = _state.value.versionForm ?: return
        val template = _state.value.selectedTemplate ?: return
        val number = form.versionNumber.toIntOrNull()
        val error = when {
            number == null || number <= 0 -> "Номер версии должен быть больше нуля."
            form.title.isBlank() -> "Введите название версии."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(formError = error) }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage()) } }) {
            val description = form.description.trim().ifBlank { null }
            val notes = form.changeNotes.trim().ifBlank { null }
            if (form.id == null) {
                courses.createVersion(template.id, CourseVersionRequest(number!!, form.title.trim(), description, form.publishNow, notes))
            } else {
                courses.updateVersion(form.id, CourseVersionUpdateRequest(number!!, form.title.trim(), description, notes))
            }
            _state.update { it.copy(saving = false, versionForm = null) }
            toast("Версия сохранена")
            loadVersions(template.id)
        }
    }

    fun togglePublish(version: CourseVersionDto) {
        launchSafe {
            if (version.published) courses.unpublishVersion(version.id) else courses.publishVersion(version.id)
            toast(if (version.published) "Публикация снята" else "Версия опубликована")
            _state.value.selectedTemplate?.let { loadVersions(it.id) }
        }
    }

    fun closeForms() = _state.update { it.copy(templateForm = null, versionForm = null, formError = null) }
}

@Composable
fun CourseTemplatesScreen(route: TeacherCoursesRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<CourseTemplatesViewModel>(key = "courses-${route.subjectId}") { parametersOf(route, role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val picker by vm.subjects.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    AppScreen(title = "Шаблоны курса", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item { TeacherSubjectSelector(picker, vm::selectMembership, vm.isAdmin) }
            if (vm.isAdmin) {
                item { Banner("Администратор может редактировать существующие шаблоны, но не создавать новые.", Tone.INFO) }
            }
            if (picker.selected != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchField(query, { query = it }, Modifier.weight(1f), placeholder = "Поиск шаблона")
                    }
                    if (!vm.isAdmin) {
                        OutlinedButton(onClick = { vm.openTemplate(null) }) {
                            Icon(Icons.Default.Add, null)
                            Text(" Новый шаблон")
                        }
                    }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { Banner(state.error!!, Tone.DANGER) }
                picker.selected != null && state.templates.isEmpty() -> item {
                    EmptyState("Шаблонов пока нет", icon = Icons.Default.CollectionsBookmark)
                }
            }
            items(state.templates.filter { query.isBlank() || it.name.contains(query.trim(), true) }, key = { "t${it.id}" }) { template ->
                val selected = template.id == state.selectedTemplate?.id
                AppCard(onClick = { vm.selectTemplate(if (selected) null else template) }) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            StatusChip(if (template.publicVisible) "Видим всем" else "Только автору", if (template.publicVisible) Tone.SUCCESS else Tone.NEUTRAL)
                        }
                        IconButton(onClick = { vm.openTemplate(template) }) { Icon(Icons.Default.Edit, "Изменить") }
                        IconButton(onClick = { vm.askDelete(template) }) { Icon(Icons.Default.Delete, "Удалить") }
                    }
                    if (selected) {
                        SectionTitle("Версии") {
                            TextButton(onClick = { vm.openVersion(null) }) { Text("+ Версия") }
                        }
                        if (state.versionsLoading) LoadingState()
                        if (!state.versionsLoading && state.versions.isEmpty()) {
                            Text("Версий пока нет.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        state.versions.forEach { version ->
                            AppCard(onClick = { vm.openVersion(version) }) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusChip("v${version.versionNumber}", Tone.PRIMARY)
                                    StatusChip(if (version.published) "Опубликована" else "Черновик", if (version.published) Tone.SUCCESS else Tone.WARNING)
                                }
                                Text(version.title, style = MaterialTheme.typography.titleSmall)
                                version.publishedAtUtc?.let {
                                    Text("Опубликована ${formatDateTime(it)}", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { vm.togglePublish(version) }) {
                                    Text(if (version.published) "Снять с публикации" else "Опубликовать")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.templateForm?.let { form ->
        EditorDialog(
            title = if (form.id == null) "Новый шаблон" else "Шаблон курса",
            onDismiss = vm::closeForms,
            onSave = vm::saveTemplate,
            busy = state.saving,
            error = state.formError,
        ) {
            FormField(form.name, { v -> vm.updateTemplateForm { it.copy(name = v) } }, "Название", required = true)
            SwitchRow("Видим другим преподавателям", form.publicVisible, { v -> vm.updateTemplateForm { it.copy(publicVisible = v) } })
        }
    }
    state.versionForm?.let { form ->
        EditorDialog(
            title = if (form.id == null) "Новая версия" else "Версия ${form.versionNumber}",
            onDismiss = vm::closeForms,
            onSave = vm::saveVersion,
            busy = state.saving,
            error = state.formError,
        ) {
            FormField(form.versionNumber, { v -> vm.updateVersionForm { it.copy(versionNumber = v.filter(Char::isDigit)) } },
                "Номер версии", keyboardType = KeyboardType.Number, required = true)
            FormField(form.title, { v -> vm.updateVersionForm { it.copy(title = v) } }, "Название", required = true)
            FormField(form.description, { v -> vm.updateVersionForm { it.copy(description = v) } }, "Описание", singleLine = false, minLines = 3)
            FormField(form.changeNotes, { v -> vm.updateVersionForm { it.copy(changeNotes = v) } }, "Что изменилось", singleLine = false, minLines = 2)
            if (form.id == null) {
                SwitchRow("Опубликовать сразу", form.publishNow, { v -> vm.updateVersionForm { it.copy(publishNow = v) } })
            }
        }
    }
    state.deleting?.let { template ->
        ConfirmDialog(
            title = "Удалить шаблон?",
            message = "Шаблон «${template.name}» и его версии будут удалены.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDelete,
            onDismiss = { vm.askDelete(null) },
        )
    }
}

// endregion

// region Workload (views/teacher/TeacherWorkloadView.vue)

data class WorkloadItem(
    val assignment: TeachingAssignmentDto,
    val subjectId: Int?,
    val subjectName: String,
    val groupName: String,
    val loadTypeName: String?,
)

data class WorkloadData(val items: List<WorkloadItem>)

class WorkloadViewModel(
    private val session: SessionManager,
    private val loader: TeacherContextLoader,
    private val teaching: TeachingApi,
    private val groups: GroupsApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<WorkloadData>>(LoadState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = LoadState.Loading
        launchSafe(onError = { _state.value = LoadState.Error(it.userMessage()) }) {
            coroutineScope {
                val context = loader.load(session.currentUser?.personId, isAdmin = false)
                val loadTypes = async { runCatching { teaching.getLoadTypes() }.getOrDefault(emptyList()).associateBy { it.id } }
                val assignments = context.memberships.map { m ->
                    async { teaching.getAssignments(subjectMembershipId = m.id).map { a -> m to a } }
                }.awaitAll().flatten()
                val groupIds = assignments.mapNotNull { it.second.groupId }.toSet()
                val groupMap = groupIds.map { id ->
                    async { cache.load("group:$id", ContextCache.REFERENCE_TTL) { groups.getById(id) } }
                }.awaitAll().associateBy { it.id }
                val types = loadTypes.await()
                val subjectNames = context.subjects.associate { it.id to it.name }
                val items = assignments.map { (membership, a) ->
                    WorkloadItem(
                        assignment = a,
                        subjectId = membership.subjectId,
                        subjectName = subjectNames[membership.subjectId] ?: "Предмет #${membership.subjectId}",
                        groupName = a.groupId?.let { groupMap[it]?.name } ?: "Группа #${a.groupId}",
                        loadTypeName = a.loadTypeId?.let { types[it]?.name },
                    )
                }.sortedWith(compareBy({ it.subjectName.lowercase() }, { it.groupName.lowercase() }))
                _state.value = LoadState.Ready(WorkloadData(items))
            }
        }
    }
}

@Composable
fun WorkloadScreen(navigator: AppNavigator) {
    val vm = koinViewModel<WorkloadViewModel>()
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<Int?>(TeachingAssignmentStatus.ACTIVE) }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    var semester by rememberSaveable { mutableStateOf<Int?>(null) }

    AppScreen(title = "Моя нагрузка", onBack = navigator::back) { padding ->
        LoadStateContent(state, onRetry = vm::load) { data ->
            val years = data.items.map { it.assignment.academicYear }.distinct().sortedDescending()
            val filtered = data.items.filter { item ->
                val a = item.assignment
                (status == null || a.status == status) &&
                    (year == null || a.academicYear == year) &&
                    (semester == null || a.semester == semester) &&
                    (query.isBlank() || item.subjectName.contains(query.trim(), true) || item.groupName.contains(query.trim(), true))
            }
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    StatGrid(
                        listOf(
                            "Предметов" to filtered.map { it.subjectId }.distinct().size.toString(),
                            "Групп" to filtered.map { it.assignment.groupId }.distinct().size.toString(),
                            "Назначений" to filtered.size.toString(),
                            "Часов в неделю" to formatNumber(filtered.sumOf { it.assignment.hoursPerWeek ?: 0.0 }),
                        )
                    )
                }
                item {
                    AppCard {
                        SearchField(query, { query = it }, placeholder = "Предмет или группа")
                        SelectField("Учебный год", years.map { SelectOption(it, "$it/${it + 1}") }, year, { year = it },
                            allowClear = true, placeholder = "Все годы")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(semester == null, { semester = null }, { Text("Оба семестра") })
                            FilterChip(semester == 1, { semester = 1 }, { Text("1 семестр") })
                            FilterChip(semester == 2, { semester = 2 }, { Text("2 семестр") })
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(status == null, { status = null }, { Text("Все статусы") })
                            TeachingAssignmentStatus.all.forEach { s ->
                                FilterChip(status == s, { status = s }, { Text(TeachingAssignmentStatus.title(s)) })
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item { EmptyState("Нагрузки нет", message = "По выбранным фильтрам назначений нет.", icon = Icons.Default.Work) }
                }
                filtered.groupBy { it.subjectName }.forEach { (subjectName, list) ->
                    item { SectionTitle(subjectName) }
                    items(list, key = { "w${it.assignment.id}" }) { item ->
                        val a = item.assignment
                        AppCard(onClick = { item.subjectId?.let { navigator.open(SubjectDetailsRoute(it)) } }) {
                            Text(item.groupName, style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusChip(TeachingAssignmentStatus.title(a.status),
                                    if (a.status == TeachingAssignmentStatus.ACTIVE) Tone.SUCCESS else Tone.NEUTRAL)
                                item.loadTypeName?.let { StatusChip(it, Tone.INFO) }
                                StatusChip("${a.academicYear}/${a.academicYear + 1}, ${a.semester} сем.")
                                a.studyCourse?.let { StatusChip("$it курс") }
                                a.hoursPerWeek?.let { StatusChip("${formatNumber(it)} ч/нед") }
                            }
                            a.notes?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// endregion
