package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.AppFilterChip
import org.santayn.testing.mobile.data.model.LectureAssignmentDto
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.domain.hidesAllLectures
import org.santayn.testing.mobile.domain.StudentLectureSource
import org.santayn.testing.mobile.domain.studentLectureSource
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import org.santayn.testing.mobile.navigation.TeacherLecturesRoute
import org.santayn.testing.mobile.data.api.LecturesApi
import org.santayn.testing.mobile.core.ui.AppOutlinedButton
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/**
 * Где используется версия курса: сколько в ней лекций и каким группам она выдана через учебную нагрузку
 * (TeachingAssignment.courseVersionId). Без лекций выданная версия «пустая»: студенты группы не увидят лекций.
 */
data class VersionUsage(val lectureCount: Int?, val groups: List<String>, val emptyForGroups: List<String> = emptyList())

/** Фильтр шаблонов по видимости. */
enum class TemplateVisibilityFilter(val label: String) { ALL("Все"), VISIBLE("Видимые"), HIDDEN("Скрытые") }

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
    val usage: Map<Int, VersionUsage> = emptyMap(),
    /** Версия, публикацию которой нужно подтвердить (у шаблона уже есть опубликованная). */
    val confirmPublish: CourseVersionDto? = null,
    val visibilityFilter: TemplateVisibilityFilter = TemplateVisibilityFilter.ALL,
)

class CourseTemplatesViewModel(
    private val route: TeacherCoursesRoute,
    val subjects: TeacherSubjectsHolder,
    private val courses: CoursesApi,
    private val lectures: LecturesApi,
    private val teaching: TeachingApi,
    private val groups: GroupsApi,
    private val cache: ContextCache,
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
        _state.update { it.copy(selectedTemplate = template, versions = emptyList(), usage = emptyMap()) }
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
        val usage = runCatching { loadUsage(versions) }.getOrDefault(emptyMap())
        _state.update { if (it.selectedTemplate?.id == templateId) it.copy(usage = usage) else it }
    }

    /** Лекции версий и группы, которым версии выданы в нагрузке выбранного предмета. */
    private suspend fun loadUsage(versions: List<CourseVersionDto>): Map<Int, VersionUsage> = coroutineScope {
        if (versions.isEmpty()) return@coroutineScope emptyMap()
        val membershipId = subjects.state.value.selectedMembershipId
        val counts = versions.map { v ->
            async { v.id to runCatching { lectures.getAll(courseVersionId = v.id).size }.getOrNull() }
        }
        val assignments = membershipId?.let { id ->
            runCatching { teaching.getAssignments(subjectMembershipId = id) }.getOrDefault(emptyList())
        }.orEmpty().filter { a -> a.courseVersionId != null && versions.any { it.id == a.courseVersionId } }
        val groupNames = assignments.mapNotNull { it.groupId }.distinct().map { id ->
            async { id to runCatching { cache.load("group:$id", ContextCache.REFERENCE_TTL) { groups.getById(id) }.name }.getOrNull() }
        }.awaitAll().toMap()
        val lectureAssignments = assignments.map { a ->
            async { a.id to runCatching { teaching.getLectureAssignments(a.id) }.getOrDefault(emptyList()) }
        }.awaitAll().toMap()
        val lectureCounts = counts.awaitAll().toMap()
        val now = nowInstant()
        versions.associate { v ->
            val issued = assignments.filter { it.courseVersionId == v.id }
            val groupTitle = { a: TeachingAssignmentDto -> a.groupId?.let { groupNames[it] } ?: "Группа #${a.groupId}" }
            val texts = issued.map { a ->
                val status = if (a.status == TeachingAssignmentStatus.ACTIVE) "" else ", ${TeachingAssignmentStatus.title(a.status).lowercase()}"
                val source = studentLectureSource(a.courseVersionId, lectureAssignments[a.id].orEmpty(), lectureCounts[v.id], now)
                val extra = if (source is StudentLectureSource.Assigned) ", назначено лекций отдельно: ${source.count}" else ""
                "${groupTitle(a)} (${a.academicYear}/${a.academicYear + 1}, ${a.semester} сем.$status$extra)"
            }
            val empty = issued.filter { a ->
                a.status == TeachingAssignmentStatus.ACTIVE &&
                    studentLectureSource(a.courseVersionId, lectureAssignments[a.id].orEmpty(), lectureCounts[v.id], now).hidesAllLectures
            }.map(groupTitle)
            v.id to VersionUsage(lectureCounts[v.id], texts, empty)
        }
    }

    fun selectVisibilityFilter(filter: TemplateVisibilityFilter) = _state.update { it.copy(visibilityFilter = filter) }

    /** Маршрут к лекциям версии (создание лекций внутри версии курса). */
    fun lecturesRoute(version: CourseVersionDto) =
        TeacherLecturesRoute(subjectId = subjects.state.value.selectedSubjectId, courseVersionId = version.id)

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
            // Шаблон с версиями сервер не удаляет, а скрывает (publicVisible = false) — сообщаем, что произошло на самом деле.
            val hadVersions = runCatching { courses.getVersions(template.id).isNotEmpty() }.getOrDefault(false)
            courses.removeTemplate(template.id)
            _state.update { it.copy(deleteBusy = false, deleting = null, selectedTemplate = null, versions = emptyList()) }
            toast(if (hadVersions) "Шаблон скрыт: у него есть версии, они сохранены" else "Шаблон удалён")
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

    /** Публикация снимает текущую опубликованную версию шаблона — сначала спрашиваем. */
    fun requestTogglePublish(version: CourseVersionDto) {
        val other = _state.value.versions.firstOrNull { it.published && it.id != version.id }
        if (!version.published && other != null) {
            _state.update { it.copy(confirmPublish = version) }
        } else {
            togglePublish(version)
        }
    }

    fun dismissPublish() = _state.update { it.copy(confirmPublish = null) }

    fun togglePublish(version: CourseVersionDto) {
        _state.update { it.copy(confirmPublish = null) }
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
            item {
                Banner(
                    "Шаблон — сам курс, версия — его состояние (лекции версии). Студенты видят курс, когда администратор " +
                        "выдаёт версию группе в «Учебной нагрузке». У шаблона опубликована только одна версия.",
                    Tone.INFO,
                    title = "Как курс попадает к студентам",
                )
            }
            if (picker.selected != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchField(query, { query = it }, Modifier.weight(1f), placeholder = "Поиск шаблона")
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TemplateVisibilityFilter.entries.forEach { f ->
                            AppFilterChip(state.visibilityFilter == f, { vm.selectVisibilityFilter(f) }, f.label)
                        }
                    }
                    if (!vm.isAdmin) {
                        AppOutlinedButton(onClick = { vm.openTemplate(null) }) {
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
            val visibleTemplates = state.templates.filter {
                (query.isBlank() || it.name.contains(query.trim(), true) || it.id.toString() == query.trim()) &&
                    when (state.visibilityFilter) {
                        TemplateVisibilityFilter.ALL -> true
                        TemplateVisibilityFilter.VISIBLE -> it.publicVisible
                        TemplateVisibilityFilter.HIDDEN -> !it.publicVisible
                    }
            }
            items(visibleTemplates, key = { "t${it.id}" }) { template ->
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
                            VersionCard(
                                version = version,
                                usage = state.usage[version.id],
                                onEdit = { vm.openVersion(version) },
                                onLectures = { navigator.open(vm.lecturesRoute(version)) },
                                onTogglePublish = { vm.requestTogglePublish(version) },
                            )
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
            message = "Если у шаблона «${template.name}» нет версий, он будет удалён. Если версии есть — шаблон " +
                "станет скрытым, а версии, их лекции и выдача группам сохранятся.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDelete,
            onDismiss = { vm.askDelete(null) },
        )
    }
    state.confirmPublish?.let { version ->
        val current = state.versions.firstOrNull { it.published && it.id != version.id }
        ConfirmDialog(
            title = "Опубликовать v${version.versionNumber}?",
            message = "Сейчас опубликована v${current?.versionNumber} «${current?.title}». У шаблона может быть только одна " +
                "опубликованная версия, поэтому с неё публикация будет снята.",
            confirmText = "Опубликовать",
            onConfirm = { vm.togglePublish(version) },
            onDismiss = vm::dismissPublish,
        )
    }
}

@Composable
private fun VersionCard(
    version: CourseVersionDto,
    usage: VersionUsage?,
    onEdit: () -> Unit,
    onLectures: () -> Unit,
    onTogglePublish: () -> Unit,
) {
    AppCard(onClick = onEdit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusChip("v${version.versionNumber}", Tone.PRIMARY)
            StatusChip(if (version.published) "Опубликована" else "Черновик", if (version.published) Tone.SUCCESS else Tone.WARNING)
            usage?.lectureCount?.let { StatusChip("Лекций: $it", if (it == 0) Tone.WARNING else Tone.NEUTRAL) }
        }
        Text(version.title, style = MaterialTheme.typography.titleSmall)
        version.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        version.changeNotes?.takeIf { it.isNotBlank() }?.let {
            Text("Что изменилось: $it", style = MaterialTheme.typography.bodySmall)
        }
        version.publishedAtUtc?.takeIf { version.published }?.let {
            Text("Опубликована ${formatDateTime(it)}", style = MaterialTheme.typography.bodySmall)
        }
        if (usage != null) {
            Text(
                if (usage.groups.isEmpty()) "Группам не выдана" else "Выдана: " + usage.groups.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (usage.emptyForGroups.isNotEmpty()) {
                Banner(
                    "В версии нет лекций, поэтому студенты групп ${usage.emptyForGroups.joinToString(", ")} сейчас не видят " +
                        "ни одной лекции предмета. Добавьте лекции в версию («Лекции версии»).",
                    Tone.DANGER,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppOutlinedButton(onClick = onLectures) {
                Icon(Icons.Default.LibraryBooks, null)
                Text(" Лекции версии")
            }
            TextButton(onClick = onTogglePublish, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (version.published) "Снять с публикации" else "Опубликовать")
            }
        }
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
    /** Версия курса, выданная группе этой нагрузкой («vN · название»). */
    val courseVersion: String? = null,
)

data class WorkloadData(val items: List<WorkloadItem>)

class WorkloadViewModel(
    private val session: SessionManager,
    private val loader: TeacherContextLoader,
    private val teaching: TeachingApi,
    private val groups: GroupsApi,
    private val courses: CoursesApi,
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
                val versionIds = assignments.mapNotNull { it.second.courseVersionId }.toSet()
                val versionMap = versionIds.map { id ->
                    async { id to runCatching { courses.getVersion(id) }.getOrNull() }
                }.awaitAll().toMap()
                val types = loadTypes.await()
                val subjectNames = context.subjects.associate { it.id to it.name }
                val items = assignments.map { (membership, a) ->
                    WorkloadItem(
                        assignment = a,
                        subjectId = membership.subjectId,
                        subjectName = subjectNames[membership.subjectId] ?: "Предмет #${membership.subjectId}",
                        groupName = a.groupId?.let { groupMap[it]?.name } ?: "Группа #${a.groupId}",
                        loadTypeName = a.loadTypeId?.let { types[it]?.name },
                        courseVersion = a.courseVersionId?.let { id ->
                            versionMap[id]?.let { "v${it.versionNumber} · ${it.title}" } ?: "версия #$id"
                        },
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
                            AppFilterChip(semester == null, { semester = null }, "Оба семестра")
                            AppFilterChip(semester == 1, { semester = 1 }, "1 семестр")
                            AppFilterChip(semester == 2, { semester = 2 }, "2 семестр")
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AppFilterChip(status == null, { status = null }, "Все статусы")
                            TeachingAssignmentStatus.all.forEach { s ->
                                AppFilterChip(status == s, { status = s }, TeachingAssignmentStatus.title(s))
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
                            Text(
                                item.courseVersion?.let { "Курс: $it" } ?: "Версия курса не выбрана — студенты видят лекции предмета",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
