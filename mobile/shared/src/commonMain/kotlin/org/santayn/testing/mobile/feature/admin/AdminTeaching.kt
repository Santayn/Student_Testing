package org.santayn.testing.mobile.feature.admin

import org.santayn.testing.mobile.core.ui.RetryBanner
import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.MultiSelectField
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatGrid
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.data.api.CoursesApi
import org.santayn.testing.mobile.data.api.FacultiesApi
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.MembershipsApi
import org.santayn.testing.mobile.data.api.SubjectsApi
import org.santayn.testing.mobile.data.api.TeachingApi
import org.santayn.testing.mobile.data.api.UsersApi
import org.santayn.testing.mobile.data.model.CourseVersionDto
import org.santayn.testing.mobile.data.model.FacultyDto
import org.santayn.testing.mobile.data.model.GroupDto
import org.santayn.testing.mobile.data.model.LoadTypeDto
import org.santayn.testing.mobile.data.model.LoadTypeRequest
import org.santayn.testing.mobile.data.model.MembershipDto
import org.santayn.testing.mobile.data.model.MembershipRequest
import org.santayn.testing.mobile.data.model.MembershipStatus
import org.santayn.testing.mobile.data.model.MembershipUpdateRequest
import org.santayn.testing.mobile.data.model.PersonDto
import org.santayn.testing.mobile.data.model.SubjectDto
import org.santayn.testing.mobile.data.model.TeachingAssignmentDto
import org.santayn.testing.mobile.data.model.TeachingAssignmentRequest
import org.santayn.testing.mobile.data.model.TeachingAssignmentStatus
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.TEACHER_SUBJECT_ROLE
import org.santayn.testing.mobile.domain.isAssignableTeacherMembership
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.navigation.AppNavigator

// region Teacher subjects (views/admin/TeacherSubjectsView.vue)

data class TeacherSubjectsState(
    val loading: Boolean = true,
    val teachers: List<PersonDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val teacherId: Int? = null,
    val memberships: List<MembershipDto> = emptyList(),
    val busySubjectId: Int? = null,
    val adding: SubjectDto? = null,
    val removing: MembershipDto? = null,
    val error: String? = null,
)

class TeacherSubjectsViewModel(
    private val users: UsersApi,
    private val subjectsApi: SubjectsApi,
    private val memberships: MembershipsApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TeacherSubjectsState())
    val state = _state.asStateFlow()

    init {
        loadReference()
    }

    private val showError: (Throwable) -> Unit = { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }

    /** Справочники преподавателей и предметов. */
    fun loadReference() {
        launchSafe(onError = showError) {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val t = async { users.getPeople(role = "TEACHER").sortedByName { it.fullName } }
                val s = async { subjectsApi.getAll().sortedByName { it.name } }
                _state.update { it.copy(loading = false, teachers = t.await(), subjects = s.await()) }
            }
            loadMemberships()
        }
    }

    /** Повтор после ошибки (кнопка «Повторить» и восстановление связи). */
    fun retry() {
        if (_state.value.teachers.isEmpty()) loadReference() else launchSafe(onError = showError) { loadMemberships() }
    }

    fun selectTeacher(id: Int?) {
        _state.update { it.copy(teacherId = id, memberships = emptyList()) }
        launchSafe(onError = showError) { loadMemberships() }
    }

    private suspend fun loadMemberships() {
        val personId = _state.value.teacherId ?: return
        _state.update { it.copy(loading = true, error = null) }
        val list = memberships.getSubjectMemberships(personId = personId, activeOnly = false)
            .filter { it.role == TEACHER_SUBJECT_ROLE }
        _state.update { if (it.teacherId == personId) it.copy(loading = false, memberships = list) else it }
    }

    fun askAdd(subject: SubjectDto?) = _state.update { it.copy(adding = subject) }

    /** utils/teacherSubjectAssignment.js: восстановить приостановленное или создать новое назначение. */
    fun confirmAdd(notes: String) {
        val personId = _state.value.teacherId ?: return
        val subject = _state.value.adding ?: return
        val paused = _state.value.memberships.firstOrNull { it.subjectId == subject.id && it.status != MembershipStatus.ACTIVE }
        _state.update { it.copy(adding = null, busySubjectId = subject.id) }
        launchSafe(onError = { e -> _state.update { it.copy(busySubjectId = null) }; toast(e.userMessage()) }) {
            val note = notes.trim().ifBlank { null }
            if (paused != null) {
                memberships.updateSubjectMembership(paused.id, MembershipUpdateRequest(MembershipStatus.ACTIVE, note ?: paused.notes))
            } else {
                memberships.addPersonToSubject(subject.id, MembershipRequest(personId, TEACHER_SUBJECT_ROLE, note))
            }
            cache.invalidate()
            _state.update { it.copy(busySubjectId = null) }
            toast(if (paused != null) "Назначение «${subject.name}» восстановлено" else "«${subject.name}» назначен преподавателю")
            loadMemberships()
        }
    }

    fun askRemove(m: MembershipDto?) = _state.update { it.copy(removing = m) }

    fun confirmRemove() {
        val m = _state.value.removing ?: return
        launchSafe(onError = { e -> _state.update { it.copy(removing = null) }; toast(e.userMessage()) }) {
            memberships.updateSubjectMembershipStatus(m.id, MembershipStatus.REMOVED)
            cache.invalidate()
            _state.update { it.copy(removing = null) }
            toast("Назначение снято. История сохранена — его можно восстановить.")
            loadMemberships()
        }
    }
}

@Composable
fun AdminTeacherSubjectsScreen(navigator: AppNavigator) {
    val vm = koinViewModel<TeacherSubjectsViewModel>()
    ReloadOnReconnect { if (vm.state.value.error != null) vm.retry() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    AppScreen(title = "Преподаватели и предметы", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    SelectField(
                        "Преподаватель",
                        state.teachers.map { SelectOption(it.id, it.fullName, it.email) },
                        state.teacherId,
                        vm::selectTeacher,
                        placeholder = "Выберите преподавателя",
                        searchable = true,
                    )
                }
            }
            if (state.loading) item { LoadingState() }
            state.error?.let { error -> item { RetryBanner(error, onRetry = vm::retry) } }
            if (state.teacherId != null && !state.loading && state.error == null) {
                val subjectName = { id: Int? -> state.subjects.firstOrNull { it.id == id }?.name ?: "Предмет #$id" }
                val active = state.memberships.filter { it.isAssignableTeacherMembership() }
                val inactive = state.memberships.filter { !it.isAssignableTeacherMembership() }
                val activeSubjectIds = active.mapNotNull { it.subjectId }.toSet()
                item { SectionTitle("Назначенные предметы (${active.size})") }
                if (active.isEmpty()) item { Text("Предметы не назначены.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(active, key = { "m${it.id}" }) { m ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(subjectName(m.subjectId), style = MaterialTheme.typography.titleSmall)
                                m.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                            IconButton(onClick = { vm.askRemove(m) }) { Icon(Icons.Default.Delete, "Снять") }
                        }
                    }
                }
                if (inactive.isNotEmpty()) {
                    item { SectionTitle("История (снятые и приостановленные)") }
                    items(inactive, key = { "h${it.id}" }) { m ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(subjectName(m.subjectId))
                                    StatusChip(MembershipStatus.title(m.status), Tone.WARNING)
                                }
                                if (m.subjectId !in activeSubjectIds) {
                                    val subject = state.subjects.firstOrNull { it.id == m.subjectId }
                                    if (subject != null) {
                                        IconButton(onClick = { vm.askAdd(subject) }) { Icon(Icons.Default.Restore, "Восстановить") }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SectionTitle("Доступные предметы")
                    SearchField(query, { query = it }, placeholder = "Поиск предмета")
                }
                items(
                    state.subjects.filter { it.id !in activeSubjectIds && (query.isBlank() || it.name.contains(query.trim(), true)) },
                    key = { "s${it.id}" },
                ) { subject ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(subject.name, modifier = Modifier.weight(1f))
                            IconButton(onClick = { vm.askAdd(subject) }, enabled = state.busySubjectId == null) {
                                Icon(Icons.Default.Add, "Назначить")
                            }
                        }
                    }
                }
            }
        }
    }
    state.adding?.let { subject ->
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.askAdd(null) },
            title = { Text("Назначить «${subject.name}»?") },
            text = { FormField(notes, { notes = it }, "Примечание (необязательно)", singleLine = false, minLines = 2) },
            confirmButton = { TextButton(onClick = { vm.confirmAdd(notes) }) { Text("Назначить") } },
            dismissButton = { TextButton(onClick = { vm.askAdd(null) }) { Text("Отмена") } },
        )
    }
    state.removing?.let {
        ConfirmDialog(
            title = "Снять назначение?",
            message = "Преподаватель больше не будет вести этот предмет. История сохранится, назначение можно восстановить.",
            confirmText = "Снять",
            destructive = true,
            onConfirm = vm::confirmRemove,
            onDismiss = { vm.askRemove(null) },
        )
    }
}

// endregion

// region Teaching assignments (views/admin/TeachingAssignmentsView.vue)

data class PeriodContext(val academicYear: Int, val semester: Int, val studyCourse: Int)

data class AssignmentForm(
    val id: Int? = null,
    val subjectId: Int? = null,
    val membershipId: Int? = null,
    val loadTypeId: Int? = null,
    val groupIds: Set<Int> = emptySet(),
    val hoursPerWeek: String = "2",
    val academicYear: String = "",
    val semester: Int = 1,
    val studyCourse: String = "1",
    val courseVersionId: Int? = null,
    val status: Int = TeachingAssignmentStatus.ACTIVE,
    val notes: String = "",
)

data class TeachingState(
    val loading: Boolean = true,
    val faculties: List<FacultyDto> = emptyList(),
    val facultyId: Int? = null,
    val period: PeriodContext,
    val facultySubjects: List<SubjectDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val loadTypes: List<LoadTypeDto> = emptyList(),
    val people: Map<Int, PersonDto> = emptyMap(),
    val subjectMemberships: List<MembershipDto> = emptyList(),
    val assignments: List<TeachingAssignmentDto> = emptyList(),
    val form: AssignmentForm? = null,
    val versions: List<CourseVersionDto> = emptyList(),
    val formError: String? = null,
    val saving: Boolean = false,
    val loadTypesOpen: Boolean = false,
    val loadTypeForm: Pair<Int?, Pair<String, String>>? = null,
    val error: String? = null,
)

class TeachingAssignmentsViewModel(
    private val faculties: FacultiesApi,
    private val groupsApi: GroupsApi,
    private val teaching: TeachingApi,
    private val memberships: MembershipsApi,
    private val users: UsersApi,
    private val courses: CoursesApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow(TeachingState(period = defaultPeriod()))
    val state = _state.asStateFlow()

    private fun defaultPeriod(): PeriodContext {
        val now = nowInstant().toLocalDateTime(TimeZone.currentSystemDefault())
        val year = if (now.month.ordinal + 1 >= 8) now.year else now.year - 1
        val semester = if (now.month.ordinal + 1 in 2..7) 2 else 1
        return PeriodContext(year, semester, 1)
    }

    private val showError: (Throwable) -> Unit = { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }

    init {
        loadAll()
    }

    /** Справочники, затем данные факультета. */
    private fun loadAll() {
        launchSafe(onError = showError) {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val f = async { faculties.getAll().sortedByName { it.name } }
                val types = async { teaching.getLoadTypes() }
                val people = async { users.getPeople() }
                val sm = async { memberships.getSubjectMemberships(activeOnly = true) }
                val list = f.await()
                _state.update {
                    it.copy(
                        faculties = list,
                        facultyId = it.facultyId ?: list.firstOrNull()?.id,
                        loadTypes = types.await().sortedByName { t -> t.name },
                        people = people.await().associateBy { p -> p.id },
                        subjectMemberships = sm.await().filter { m -> m.role == TEACHER_SUBJECT_ROLE },
                    )
                }
                loadFaculty()
            }
        }
    }

    fun selectFaculty(id: Int?) {
        _state.update { it.copy(facultyId = id) }
        launchSafe(onError = showError) { loadFaculty() }
    }

    fun setPeriod(period: PeriodContext) {
        _state.update { it.copy(period = period) }
        launchSafe(onError = showError) { loadAssignments() }
    }

    private suspend fun loadFaculty() = coroutineScope {
        val facultyId = _state.value.facultyId ?: run {
            _state.update { it.copy(loading = false) }
            return@coroutineScope
        }
        _state.update { it.copy(loading = true, error = null) }
        val subjects = async { faculties.getSubjects(facultyId).sortedByName { it.name } }
        val groups = async { groupsApi.getAll(facultyId).sortedByName { it.name } }
        _state.update { it.copy(facultySubjects = subjects.await(), groups = groups.await()) }
        loadAssignments()
    }

    private suspend fun loadAssignments() {
        val s = _state.value
        val facultyId = s.facultyId ?: return
        _state.update { it.copy(loading = true, error = null) }
        val list = teaching.getAssignments(
            facultyId = facultyId,
            academicYear = s.period.academicYear,
            semester = s.period.semester,
            studyCourse = s.period.studyCourse,
        )
        _state.update { it.copy(loading = false, assignments = list) }
    }

    fun reload() {
        if (_state.value.faculties.isEmpty()) loadAll() else launchSafe(onError = showError) { loadFaculty() }
    }

    fun membership(id: Int?) = _state.value.subjectMemberships.firstOrNull { it.id == id }

    fun teacherName(membershipId: Int?): String {
        val m = membership(membershipId) ?: return "Назначение #$membershipId"
        return _state.value.people[m.personId]?.fullName ?: "Преподаватель #${m.personId}"
    }

    fun subjectName(membershipId: Int?): String {
        val subjectId = membership(membershipId)?.subjectId
        return _state.value.facultySubjects.firstOrNull { it.id == subjectId }?.name ?: "Предмет #${subjectId ?: "?"}"
    }

    fun groupName(id: Int?) = _state.value.groups.firstOrNull { it.id == id }?.name ?: "Группа #$id"

    // region editor

    fun openCreate() {
        val p = _state.value.period
        _state.update {
            it.copy(
                form = AssignmentForm(academicYear = p.academicYear.toString(), semester = p.semester, studyCourse = p.studyCourse.toString()),
                formError = null,
                versions = emptyList(),
            )
        }
    }

    fun openEdit(a: TeachingAssignmentDto) {
        val subjectId = membership(a.subjectMembershipId)?.subjectId
        _state.update {
            it.copy(
                form = AssignmentForm(
                    id = a.id, subjectId = subjectId, membershipId = a.subjectMembershipId, loadTypeId = a.loadTypeId,
                    groupIds = setOfNotNull(a.groupId), hoursPerWeek = formatNumber(a.hoursPerWeek ?: 0.0),
                    academicYear = a.academicYear.toString(), semester = a.semester,
                    studyCourse = a.studyCourse?.toString().orEmpty(), courseVersionId = a.courseVersionId,
                    status = a.status, notes = a.notes.orEmpty(),
                ),
                formError = null,
            )
        }
        subjectId?.let { loadVersions(it) }
    }

    fun updateForm(transform: (AssignmentForm) -> AssignmentForm) {
        val before = _state.value.form?.subjectId
        _state.update { s -> s.copy(form = s.form?.let(transform)) }
        val after = _state.value.form?.subjectId
        if (after != before) {
            _state.update { s -> s.copy(form = s.form?.copy(membershipId = null, courseVersionId = null), versions = emptyList()) }
            after?.let { loadVersions(it) }
        }
    }

    private fun loadVersions(subjectId: Int) = launchSafe {
        val versions = courses.getTemplates(subjectId).map { t -> async { courses.getVersions(t.id) } }.awaitAll().flatten()
        _state.update { it.copy(versions = versions) }
    }

    fun closeForm() = _state.update { it.copy(form = null, formError = null) }

    fun save() {
        val form = _state.value.form ?: return
        val hours = form.hoursPerWeek.replace(',', '.').toDoubleOrNull()
        val year = form.academicYear.toIntOrNull()
        val error = when {
            form.subjectId == null -> "Выберите предмет."
            form.membershipId == null -> "Выберите преподавателя."
            form.loadTypeId == null -> "Выберите тип нагрузки."
            form.groupIds.isEmpty() -> "Выберите группу."
            hours == null || hours < 0 -> "Часы в неделю должны быть числом не меньше нуля."
            year == null || year < 2000 -> "Укажите учебный год."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(formError = error) }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage()) } }) {
            val fresh = memberships.getSubjectMembership(form.membershipId!!)
            if (form.status in listOf(TeachingAssignmentStatus.ACTIVE, TeachingAssignmentStatus.DRAFT) && !fresh.isAssignableTeacherMembership()) {
                error("Назначение преподавателя на предмет больше не активно.")
            }
            // Привязываем тип нагрузки к назначению преподавателя (если уже привязан — сервер вернёт конфликт).
            try {
                teaching.addLoadTypeToSubjectMembership(form.membershipId, form.loadTypeId!!)
            } catch (e: ApiException) {
                if (!e.isConflict && e.status != 400) throw e
            }
            fun request(groupId: Int) = TeachingAssignmentRequest(
                subjectMembershipId = form.membershipId,
                groupId = groupId,
                loadTypeId = form.loadTypeId,
                courseVersionId = form.courseVersionId,
                semester = form.semester,
                studyCourse = form.studyCourse.toIntOrNull(),
                academicYear = year!!,
                hoursPerWeek = hours,
                status = form.status,
                notes = form.notes.trim().ifBlank { null },
            )
            if (form.id == null) {
                form.groupIds.forEach { teaching.createAssignment(request(it)) }
            } else {
                teaching.updateAssignment(form.id, request(form.groupIds.first()))
            }
            cache.invalidate()
            _state.update { it.copy(saving = false, form = null) }
            toast("Нагрузка сохранена")
            loadAssignments()
        }
    }

    // endregion

    // region load types

    fun openLoadTypes(open: Boolean) = _state.update { it.copy(loadTypesOpen = open, loadTypeForm = null) }
    fun editLoadType(type: LoadTypeDto?) = _state.update {
        it.copy(loadTypeForm = (type?.id) to ((type?.name ?: "") to (type?.description ?: "")))
    }
    fun updateLoadTypeForm(name: String, description: String) =
        _state.update { s -> s.copy(loadTypeForm = s.loadTypeForm?.let { it.first to (name to description) }) }

    fun saveLoadType() {
        val (id, values) = _state.value.loadTypeForm ?: return
        val (name, description) = values
        if (name.isBlank()) {
            toast("Введите название типа нагрузки")
            return
        }
        launchSafe {
            val request = LoadTypeRequest(name.trim(), description.trim().ifBlank { null })
            if (id == null) teaching.createLoadType(request) else teaching.updateLoadType(id, request)
            val types = teaching.getLoadTypes().sortedByName { it.name }
            _state.update { it.copy(loadTypes = types, loadTypeForm = null) }
            toast("Тип нагрузки сохранён")
        }
    }

    // endregion
}

@Composable
fun AdminTeachingScreen(navigator: AppNavigator) {
    val vm = koinViewModel<TeachingAssignmentsViewModel>()
    ReloadOnReconnect { vm.reload() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    val p = state.period

    AppScreen(
        title = "Учебная нагрузка",
        onBack = navigator::back,
        actions = { IconButton(onClick = { vm.openLoadTypes(true) }) { Icon(Icons.Default.Tune, "Типы нагрузки") } },
        floatingActionButton = {
            if (state.facultyId != null) {
                ExtendedFloatingActionButton(onClick = vm::openCreate, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Назначение") })
            }
        },
    ) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    SelectField("Факультет", state.faculties.map { SelectOption(it.id, it.name) }, state.facultyId, vm::selectFaculty)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField("Учебный год", (p.academicYear - 3..p.academicYear + 2).map { SelectOption(it, "$it/${it + 1}") },
                            p.academicYear, { y -> if (y != null) vm.setPeriod(p.copy(academicYear = y)) }, Modifier.weight(1f))
                        SelectField("Курс", (1..6).map { SelectOption(it, "$it курс") }, p.studyCourse,
                            { c -> if (c != null) vm.setPeriod(p.copy(studyCourse = c)) }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(p.semester == 1, { vm.setPeriod(p.copy(semester = 1)) }, { Text("1 семестр") })
                        FilterChip(p.semester == 2, { vm.setPeriod(p.copy(semester = 2)) }, { Text("2 семестр") })
                    }
                }
            }
            val filtered = state.assignments.filter { a ->
                (statusFilter == null || a.status == statusFilter) &&
                    (query.isBlank() || listOf(vm.subjectName(a.subjectMembershipId), vm.teacherName(a.subjectMembershipId), vm.groupName(a.groupId))
                        .any { it.contains(query.trim(), true) })
            }
            item {
                StatGrid(
                    listOf(
                        "Назначений" to state.assignments.size.toString(),
                        "Активных" to state.assignments.count { it.status == TeachingAssignmentStatus.ACTIVE }.toString(),
                        "Преподавателей" to state.assignments.mapNotNull { vm.membership(it.subjectMembershipId)?.personId }.distinct().size.toString(),
                        "Часов в неделю" to formatNumber(state.assignments.filter { it.status == TeachingAssignmentStatus.ACTIVE }
                            .sumOf { it.hoursPerWeek ?: 0.0 }),
                    )
                )
            }
            item {
                SearchField(query, { query = it }, placeholder = "Предмет, преподаватель, группа")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(statusFilter == null, { statusFilter = null }, { Text("Все") })
                    TeachingAssignmentStatus.all.forEach { s ->
                        FilterChip(statusFilter == s, { statusFilter = s }, { Text(TeachingAssignmentStatus.title(s)) })
                    }
                }
            }
            if (state.loading) item { LoadingState() }
            state.error?.let { error -> item { RetryBanner(error, onRetry = vm::reload) } }
            if (!state.loading && state.error == null && filtered.isEmpty()) {
                item { EmptyState("Нагрузки нет", message = "Для выбранного периода назначений не найдено.", icon = Icons.Default.Work) }
            }
            items(filtered, key = { it.id }) { a ->
                AppCard(onClick = { vm.openEdit(a) }) {
                    Text(vm.subjectName(a.subjectMembershipId), style = MaterialTheme.typography.titleMedium)
                    Text("${vm.teacherName(a.subjectMembershipId)} · ${vm.groupName(a.groupId)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusChip(TeachingAssignmentStatus.title(a.status), if (a.status == TeachingAssignmentStatus.ACTIVE) Tone.SUCCESS else Tone.NEUTRAL)
                        state.loadTypes.firstOrNull { it.id == a.loadTypeId }?.let { StatusChip(it.name, Tone.INFO) }
                        a.hoursPerWeek?.let { StatusChip("${formatNumber(it)} ч/нед") }
                    }
                }
            }
        }
    }

    state.form?.let { form -> AssignmentEditor(form, state, vm) }
    if (state.loadTypesOpen) LoadTypesDialog(state, vm)
}

@Composable
private fun AssignmentEditor(form: AssignmentForm, state: TeachingState, vm: TeachingAssignmentsViewModel) {
    val teachers = state.subjectMemberships.filter { it.subjectId == form.subjectId && it.isAssignableTeacherMembership() }
    EditorDialog(
        title = if (form.id == null) "Новое назначение" else "Назначение #${form.id}",
        onDismiss = vm::closeForm,
        onSave = vm::save,
        busy = state.saving,
        error = state.formError,
    ) {
        SelectField("Предмет", state.facultySubjects.map { SelectOption(it.id, it.name) }, form.subjectId,
            { id -> vm.updateForm { it.copy(subjectId = id) } }, required = true)
        SelectField(
            "Преподаватель",
            teachers.map { SelectOption(it.id, state.people[it.personId]?.fullName ?: "Преподаватель #${it.personId}") },
            form.membershipId,
            { id -> vm.updateForm { it.copy(membershipId = id) } },
            required = true,
            enabled = form.subjectId != null,
            placeholder = if (form.subjectId != null && teachers.isEmpty()) "Нет преподавателей предмета" else "Выберите преподавателя",
        )
        SelectField("Тип нагрузки", state.loadTypes.map { SelectOption(it.id, it.name) }, form.loadTypeId,
            { id -> vm.updateForm { it.copy(loadTypeId = id) } }, required = true)
        if (form.id == null) {
            MultiSelectField("Группы", state.groups.map { SelectOption(it.id, it.name) }, form.groupIds,
                { ids -> vm.updateForm { it.copy(groupIds = ids) } })
        } else {
            SelectField("Группа", state.groups.map { SelectOption(it.id, it.name) }, form.groupIds.firstOrNull(),
                { id -> vm.updateForm { it.copy(groupIds = setOfNotNull(id)) } })
        }
        FormField(form.hoursPerWeek, { v -> vm.updateForm { it.copy(hoursPerWeek = v) } }, "Часов в неделю", keyboardType = KeyboardType.Decimal)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormField(form.academicYear, { v -> vm.updateForm { it.copy(academicYear = v.filter(Char::isDigit).take(4)) } }, "Учебный год",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            FormField(form.studyCourse, { v -> vm.updateForm { it.copy(studyCourse = v.filter(Char::isDigit).take(1)) } }, "Курс",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(form.semester == 1, { vm.updateForm { it.copy(semester = 1) } }, { Text("1 семестр") })
            FilterChip(form.semester == 2, { vm.updateForm { it.copy(semester = 2) } }, { Text("2 семестр") })
        }
        SelectField("Версия курса", state.versions.map { SelectOption(it.id, "v${it.versionNumber} · ${it.title}") }, form.courseVersionId,
            { id -> vm.updateForm { it.copy(courseVersionId = id) } }, allowClear = true, placeholder = "Не выбрана")
        SelectField("Статус", TeachingAssignmentStatus.all.map { SelectOption(it, TeachingAssignmentStatus.title(it)) }, form.status,
            { s -> if (s != null) vm.updateForm { it.copy(status = s) } })
        FormField(form.notes, { v -> vm.updateForm { it.copy(notes = v) } }, "Примечание", singleLine = false, minLines = 2)
    }
}

@Composable
private fun LoadTypesDialog(state: TeachingState, vm: TeachingAssignmentsViewModel) {
    EditorDialog(
        title = "Типы нагрузки",
        onDismiss = { vm.openLoadTypes(false) },
        onSave = { vm.openLoadTypes(false) },
        saveText = "Готово",
    ) {
        state.loadTypes.forEach { type ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(type.name, style = MaterialTheme.typography.titleSmall)
                        type.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    IconButton(onClick = { vm.editLoadType(type) }) { Icon(Icons.Default.Edit, "Изменить") }
                }
            }
        }
        TextButton(onClick = { vm.editLoadType(null) }) { Text("+ Новый тип нагрузки") }
        state.loadTypeForm?.let { (id, values) ->
            AppCard {
                Text(if (id == null) "Новый тип" else "Редактирование", style = MaterialTheme.typography.titleSmall)
                FormField(values.first, { vm.updateLoadTypeForm(it, values.second) }, "Название", required = true)
                FormField(values.second, { vm.updateLoadTypeForm(values.first, it) }, "Описание", singleLine = false)
                Row {
                    TextButton(onClick = vm::saveLoadType) { Text("Сохранить") }
                    TextButton(onClick = { vm.openLoadTypes(true) }) { Text("Отмена") }
                }
            }
        }
        if (state.loadTypes.isEmpty()) Banner("Типов нагрузки пока нет.", Tone.INFO)
    }
}

// endregion
