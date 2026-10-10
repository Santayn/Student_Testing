package org.santayn.testing.mobile.feature.admin

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import org.santayn.testing.mobile.core.ui.RetryBanner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadStateContent
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.data.api.FacultiesApi
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.MembershipsApi
import org.santayn.testing.mobile.data.api.SubjectsApi
import org.santayn.testing.mobile.data.api.UsersApi
import org.santayn.testing.mobile.data.model.FacultyDto
import org.santayn.testing.mobile.data.model.FacultyRequest
import org.santayn.testing.mobile.data.model.GroupDto
import org.santayn.testing.mobile.data.model.GroupRequest
import org.santayn.testing.mobile.data.model.MembershipDto
import org.santayn.testing.mobile.data.model.MembershipRequest
import org.santayn.testing.mobile.data.model.MembershipStatus
import org.santayn.testing.mobile.data.model.PersonDto
import org.santayn.testing.mobile.data.model.SubjectDto
import org.santayn.testing.mobile.data.model.SubjectRequest
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.STUDENT_GROUP_ROLE
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.navigation.AppNavigator

/*
 * Справочники администратора: views/admin/FacultiesView.vue, SubjectsAdminView.vue, GroupsView.vue,
 * FacultySubjectsView.vue.
 */

/** Текст ошибки удаления для записей со связями. */
internal fun deleteErrorMessage(e: Throwable, what: String): String =
    if (e is ApiException && (e.isConflict || e.status == 400)) "$what нельзя удалить: ${e.message}" else e.userMessage("Не удалось удалить")

// region Simple dictionary (faculties / subjects)

/** Универсальная форма справочника: название, код (опционально), описание, группа→факультет. */
data class DictForm(
    val id: Int? = null,
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val facultyId: Int? = null,
)

data class DictUiState<T>(
    val items: LoadState<List<T>> = LoadState.Loading,
    val form: DictForm? = null,
    val formError: String? = null,
    val saving: Boolean = false,
    val deleting: T? = null,
    val deleteBusy: Boolean = false,
)

abstract class DictViewModel<T>(private val cache: ContextCache) : BaseViewModel() {
    protected val _state = MutableStateFlow(DictUiState<T>())
    val state = _state.asStateFlow()

    protected abstract suspend fun fetch(): List<T>
    protected abstract suspend fun persist(form: DictForm)
    protected abstract suspend fun remove(item: T)
    protected abstract fun validate(form: DictForm): String?
    abstract val entityName: String

    private var loaded = false

    /**
     * Первая загрузка вызывается экраном, а не из init: в init базового класса
     * зависимости подклассов ещё не инициализированы.
     */
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        reload()
    }

    fun reload() = launchSafe { reloadInternal() }

    private suspend fun reloadInternal() {
        if (_state.value.items !is LoadState.Ready) _state.update { it.copy(items = LoadState.Loading) }
        try {
            _state.update { it.copy(items = LoadState.Ready(fetch())) }
        } catch (e: Exception) {
            _state.update { it.copy(items = LoadState.Error(e.userMessage())) }
        }
    }

    fun open(form: DictForm?) = _state.update { it.copy(form = form, formError = null) }
    fun update(transform: (DictForm) -> DictForm) = _state.update { s -> s.copy(form = s.form?.let(transform)) }

    fun save() {
        val form = _state.value.form ?: return
        validate(form)?.let { msg ->
            _state.update { it.copy(formError = msg) }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage()) } }) {
            persist(form)
            cache.invalidate()
            _state.update { it.copy(saving = false, form = null) }
            toast("Сохранено")
            reloadInternal()
        }
    }

    fun askDelete(item: T?) = _state.update { it.copy(deleting = item) }

    fun confirmDelete() {
        val item = _state.value.deleting ?: return
        _state.update { it.copy(deleteBusy = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(deleteBusy = false, deleting = null) }
            toast(deleteErrorMessage(e, entityName))
        }) {
            remove(item)
            cache.invalidate()
            _state.update { it.copy(deleteBusy = false, deleting = null) }
            toast("Удалено")
            reloadInternal()
        }
    }
}

class FacultiesViewModel(private val api: FacultiesApi, cache: ContextCache) : DictViewModel<FacultyDto>(cache) {
    override val entityName = "Факультет"
    override suspend fun fetch() = api.getAll().sortedByName { it.name }
    override fun validate(form: DictForm) = if (form.name.isBlank()) "Введите название факультета." else null
    override suspend fun persist(form: DictForm) {
        val request = FacultyRequest(form.name.trim(), form.code.trim().ifBlank { null }, form.description.trim().ifBlank { null })
        if (form.id == null) api.create(request) else api.update(form.id, request)
    }
    override suspend fun remove(item: FacultyDto) = api.remove(item.id)
}

class SubjectsAdminViewModel(private val api: SubjectsApi, cache: ContextCache) : DictViewModel<SubjectDto>(cache) {
    override val entityName = "Предмет"
    override suspend fun fetch() = api.getAll().sortedByName { it.name }
    override fun validate(form: DictForm) = if (form.name.isBlank()) "Введите название предмета." else null
    override suspend fun persist(form: DictForm) {
        val request = SubjectRequest(form.name.trim(), form.description.trim().ifBlank { null })
        if (form.id == null) api.create(request) else api.update(form.id, request)
    }
    override suspend fun remove(item: SubjectDto) = api.remove(item.id)
}

/**
 * Общий экран справочника: список карточек, поиск, создание/редактирование/удаление.
 */
@Composable
private fun <T> DictionaryScreen(
    title: String,
    vm: DictViewModel<T>,
    navigator: AppNavigator,
    itemKey: (T) -> Int,
    itemTitle: (T) -> String,
    itemSubtitle: (T) -> String?,
    toForm: (T) -> DictForm,
    searchText: (T) -> String,
    showCode: Boolean,
    extraFields: @Composable (DictForm) -> Unit = {},
    itemActions: @Composable (T) -> Unit = {},
    header: @Composable () -> Unit = {},
    extraFilter: (T) -> Boolean = { true },
) {
    CollectMessages(vm)
    androidx.compose.runtime.LaunchedEffect(vm) { vm.ensureLoaded() }
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    AppScreen(
        title = title,
        onBack = navigator::back,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { vm.open(DictForm()) }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Добавить") })
        },
    ) { padding ->
        LoadStateContent(state.items, onRetry = vm::reload, onRefresh = vm::reload) { items ->
            val filtered = items.filter { extraFilter(it) && (query.isBlank() || searchText(it).contains(query.trim(), true)) }
            ScreenList(contentPadding = screenPadding(padding)) {
                item { header() }
                item { SearchField(query, { query = it }) }
                if (filtered.isEmpty()) item { EmptyState(if (items.isEmpty()) "Записей пока нет" else "Ничего не найдено") }
                items(filtered, key = itemKey) { item ->
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(itemTitle(item), style = MaterialTheme.typography.titleMedium)
                                itemSubtitle(item)?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { vm.open(toForm(item)) }) { Icon(Icons.Default.Edit, "Изменить") }
                            IconButton(onClick = { vm.askDelete(item) }) { Icon(Icons.Default.Delete, "Удалить") }
                        }
                        itemActions(item)
                    }
                }
            }
        }
    }
    state.form?.let { form ->
        EditorDialog(
            title = if (form.id == null) "Новая запись" else "Редактирование",
            onDismiss = { vm.open(null) },
            onSave = vm::save,
            busy = state.saving,
            error = state.formError,
        ) {
            FormField(form.name, { v -> vm.update { it.copy(name = v) } }, "Название", required = true, maxLength = 200)
            if (showCode) FormField(form.code, { v -> vm.update { it.copy(code = v) } }, "Код", maxLength = 50)
            extraFields(form)
            FormField(form.description, { v -> vm.update { it.copy(description = v) } }, "Описание", singleLine = false, minLines = 3)
        }
    }
    state.deleting?.let { item ->
        ConfirmDialog(
            title = "Удалить запись?",
            message = "«${itemTitle(item)}» будет удалено. Если есть связанные данные, сервер отклонит удаление.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDelete,
            onDismiss = { vm.askDelete(null) },
        )
    }
}

@Composable
fun AdminFacultiesScreen(navigator: AppNavigator) {
    val vm = koinViewModel<FacultiesViewModel>()
    DictionaryScreen(
        title = "Факультеты", vm = vm, navigator = navigator,
        itemKey = { it.id }, itemTitle = { it.name },
        itemSubtitle = { listOfNotNull(it.code, it.description).filter { s -> s.isNotBlank() }.joinToString(" · ") },
        toForm = { DictForm(it.id, it.name, it.code.orEmpty(), it.description.orEmpty()) },
        searchText = { "${it.name} ${it.code.orEmpty()} ${it.description.orEmpty()}" },
        showCode = true,
    )
}

@Composable
fun AdminSubjectsScreen(navigator: AppNavigator) {
    val vm = koinViewModel<SubjectsAdminViewModel>()
    DictionaryScreen(
        title = "Справочник предметов", vm = vm, navigator = navigator,
        itemKey = { it.id }, itemTitle = { it.name }, itemSubtitle = { it.description },
        toForm = { DictForm(it.id, it.name, description = it.description.orEmpty()) },
        searchText = { "${it.name} ${it.description.orEmpty()}" },
        showCode = false,
    )
}

// endregion

// region Groups + members (GroupsView.vue, AdminGroupMembersDrawer.vue)

data class GroupMembersState(
    val group: GroupDto,
    val loading: Boolean = true,
    val memberships: List<MembershipDto> = emptyList(),
    val people: Map<Int, PersonDto> = emptyMap(),
    val students: List<PersonDto> = emptyList(),
    val busyPersonId: Int? = null,
    val removing: MembershipDto? = null,
    val error: String? = null,
)

class GroupsViewModel(
    private val groups: GroupsApi,
    private val faculties: FacultiesApi,
    private val memberships: MembershipsApi,
    private val users: UsersApi,
    cache: ContextCache,
) : DictViewModel<GroupDto>(cache) {
    override val entityName = "Группа"
    private val _faculties = MutableStateFlow<List<FacultyDto>>(emptyList())
    val facultyList = _faculties.asStateFlow()
    private val _members = MutableStateFlow<GroupMembersState?>(null)
    val members = _members.asStateFlow()

    override suspend fun fetch(): List<GroupDto> = coroutineScope {
        val f = async { faculties.getAll().sortedByName { it.name } }
        val g = groups.getAll().sortedByName { it.name }
        _faculties.value = f.await()
        g
    }

    override fun validate(form: DictForm) = when {
        form.name.isBlank() -> "Введите название группы."
        form.facultyId == null -> "Выберите факультет."
        else -> null
    }

    override suspend fun persist(form: DictForm) {
        val request = GroupRequest(form.name.trim(), form.code.trim().ifBlank { null }, form.facultyId)
        if (form.id == null) groups.create(request) else groups.update(form.id, request)
    }

    override suspend fun remove(item: GroupDto) = groups.remove(item.id)

    fun facultyName(id: Int?) = _faculties.value.firstOrNull { it.id == id }?.name

    fun openMembers(group: GroupDto?) {
        _members.value = group?.let { GroupMembersState(it) }
        if (group != null) loadMembers(group.id)
    }

    /** Повтор загрузки состава (кнопка «Повторить» и восстановление связи). */
    fun retryMembers() {
        val state = _members.value ?: return
        if (state.error != null) loadMembers(state.group.id)
    }

    private fun loadMembers(groupId: Int) = launchSafe(onError = { e ->
        _members.update { it?.copy(loading = false, error = e.userMessage()) }
    }) {
        _members.update { it?.copy(loading = true, error = null) }
        coroutineScope {
            val m = async { memberships.getGroupMemberships(groupId = groupId, activeOnly = false) }
            val people = async { users.getPeople() }
            val students = async { users.getPeople(role = "STUDENT") }
            val list = m.await()
            _members.update {
                it?.copy(
                    loading = false,
                    memberships = list,
                    people = people.await().associateBy { p -> p.id },
                    students = students.await().sortedByName { p -> p.fullName },
                )
            }
        }
    }

    fun addStudent(person: PersonDto) {
        val state = _members.value ?: return
        val paused = state.memberships.firstOrNull {
            it.personId == person.id && it.role == STUDENT_GROUP_ROLE && it.status != MembershipStatus.ACTIVE
        }
        _members.update { it?.copy(busyPersonId = person.id) }
        launchSafe(onError = { e ->
            _members.update { it?.copy(busyPersonId = null) }
            toast(e.userMessage("Не удалось добавить студента"))
        }) {
            if (paused != null) {
                memberships.updateGroupMembershipStatus(paused.id, MembershipStatus.ACTIVE)
            } else {
                memberships.addPersonToGroup(state.group.id, MembershipRequest(person.id, STUDENT_GROUP_ROLE, null))
            }
            _members.update { it?.copy(busyPersonId = null) }
            toast(if (paused != null) "${person.fullName} снова в составе группы" else "${person.fullName} добавлен в группу")
            loadMembers(state.group.id)
        }
    }

    fun askRemove(membership: MembershipDto?) = _members.update { it?.copy(removing = membership) }

    fun confirmRemove() {
        val state = _members.value ?: return
        val membership = state.removing ?: return
        launchSafe(onError = { e ->
            _members.update { it?.copy(removing = null) }
            toast(e.userMessage("Не удалось исключить студента"))
        }) {
            memberships.updateGroupMembershipStatus(membership.id, MembershipStatus.REMOVED)
            _members.update { it?.copy(removing = null) }
            toast("Студент исключён из группы")
            loadMembers(state.group.id)
        }
    }
}

@Composable
fun AdminGroupsScreen(navigator: AppNavigator) {
    val vm = koinViewModel<GroupsViewModel>()
    val faculties by vm.facultyList.collectAsState()
    val members by vm.members.collectAsState()
    var facultyFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    DictionaryScreen(
        title = "Группы", vm = vm, navigator = navigator,
        itemKey = { it.id }, itemTitle = { it.name },
        itemSubtitle = { listOfNotNull(it.code, vm.facultyName(it.facultyId)).joinToString(" · ") },
        toForm = { DictForm(it.id, it.name, it.code.orEmpty(), facultyId = it.facultyId) },
        searchText = { "${it.name} ${it.code.orEmpty()} ${vm.facultyName(it.facultyId).orEmpty()}" },
        showCode = true,
        header = {
            SelectField("Факультет", faculties.map { SelectOption(it.id, it.name) }, facultyFilter, { facultyFilter = it },
                allowClear = true, placeholder = "Все факультеты")
        },
        extraFields = { form ->
            SelectField("Факультет", faculties.map { SelectOption(it.id, it.name) }, form.facultyId,
                { id -> vm.update { it.copy(facultyId = id) } }, required = true)
        },
        extraFilter = { facultyFilter == null || it.facultyId == facultyFilter },
        itemActions = { group ->
            TextButton(onClick = { vm.openMembers(group) }) {
                Icon(Icons.Default.Groups, null)
                Text(" Состав группы")
            }
        },
    )
    members?.let { GroupMembersDialog(it, vm) }
}

@Composable
private fun GroupMembersDialog(state: GroupMembersState, vm: GroupsViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val active = state.memberships.filter { it.role == STUDENT_GROUP_ROLE && it.status == MembershipStatus.ACTIVE && it.removedAtUtc == null }
    val activeIds = active.mapNotNull { it.personId }.toSet()
    EditorDialog(
        title = "Состав: ${state.group.name}",
        onDismiss = { vm.openMembers(null) },
        onSave = { vm.openMembers(null) },
        saveText = "Готово",
    ) {
        if (state.loading) {
            LoadingState()
            return@EditorDialog
        }
        if (state.error != null) {
            ReloadOnReconnect { vm.retryMembers() }
            RetryBanner(state.error, onRetry = vm::retryMembers)
            return@EditorDialog
        }
        SectionTitle("В группе (${active.size})")
        if (active.isEmpty()) Text("В группе пока нет студентов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        active.forEach { m ->
            val person = state.people[m.personId]
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(person?.fullName ?: "Персона #${m.personId}")
                        person?.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { vm.askRemove(m) }) { Icon(Icons.Default.PersonRemove, "Исключить") }
                }
            }
        }
        SectionTitle("Добавить студента")
        SearchField(query, { query = it }, placeholder = "Поиск по имени или email")
        val available = state.students.filter { it.id !in activeIds }
            .filter { query.isBlank() || it.fullName.contains(query.trim(), true) || it.email?.contains(query.trim(), true) == true }
        if (available.isEmpty()) Text("Нет доступных студентов.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        available.take(50).forEach { person ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(person.fullName)
                        person.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    IconButton(onClick = { vm.addStudent(person) }, enabled = state.busyPersonId == null) {
                        Icon(Icons.Default.PersonAdd, "Добавить")
                    }
                }
            }
        }
    }
    state.removing?.let { m ->
        ConfirmDialog(
            title = "Исключить из группы?",
            message = "${state.people[m.personId]?.fullName ?: "Студент"} будет исключён. История сохранится.",
            confirmText = "Исключить",
            destructive = true,
            onConfirm = vm::confirmRemove,
            onDismiss = { vm.askRemove(null) },
        )
    }
}

// endregion

// region Faculty subjects (FacultySubjectsView.vue)

data class FacultySubjectsState(
    val faculties: List<FacultyDto> = emptyList(),
    val facultyId: Int? = null,
    val allSubjects: List<SubjectDto> = emptyList(),
    val assigned: List<SubjectDto> = emptyList(),
    val loading: Boolean = true,
    val busy: Int? = null,
    val removing: SubjectDto? = null,
    val error: String? = null,
)

class FacultySubjectsViewModel(
    private val faculties: FacultiesApi,
    private val subjects: SubjectsApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow(FacultySubjectsState())
    val state = _state.asStateFlow()

    private val showError: (Throwable) -> Unit = { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }

    init {
        reload()
    }

    /** Загрузка справочников (если их ещё нет) и предметов факультета; повтор после ошибки и при восстановлении связи. */
    fun reload() {
        launchSafe(onError = showError) {
            if (_state.value.faculties.isEmpty()) {
                _state.update { it.copy(loading = true, error = null) }
                coroutineScope {
                    val f = async { faculties.getAll().sortedByName { it.name } }
                    val s = async { subjects.getAll().sortedByName { it.name } }
                    val list = f.await()
                    _state.update {
                        it.copy(faculties = list, allSubjects = s.await(), facultyId = it.facultyId ?: list.firstOrNull()?.id, loading = false)
                    }
                }
            }
            loadAssigned()
        }
    }

    fun selectFaculty(id: Int?) {
        _state.update { it.copy(facultyId = id, assigned = emptyList()) }
        launchSafe(onError = showError) { loadAssigned() }
    }

    private suspend fun loadAssigned() {
        val id = _state.value.facultyId ?: return
        _state.update { it.copy(loading = true, error = null) }
        val assigned = faculties.getSubjects(id).sortedByName { it.name }
        _state.update { if (it.facultyId == id) it.copy(assigned = assigned, loading = false) else it }
    }

    fun add(subject: SubjectDto) {
        val facultyId = _state.value.facultyId ?: return
        _state.update { it.copy(busy = subject.id) }
        launchSafe(onError = { e -> _state.update { it.copy(busy = null) }; toast(e.userMessage()) }) {
            faculties.addSubject(facultyId, subject.id)
            cache.invalidate()
            _state.update { it.copy(busy = null) }
            toast("«${subject.name}» добавлен")
            loadAssigned()
        }
    }

    fun askRemove(subject: SubjectDto?) = _state.update { it.copy(removing = subject) }

    fun confirmRemove() {
        val facultyId = _state.value.facultyId ?: return
        val subject = _state.value.removing ?: return
        launchSafe(onError = { e -> _state.update { it.copy(removing = null) }; toast(deleteErrorMessage(e, "Связь")) }) {
            faculties.removeSubject(facultyId, subject.id)
            cache.invalidate()
            _state.update { it.copy(removing = null) }
            toast("«${subject.name}» убран из факультета")
            loadAssigned()
        }
    }
}

@Composable
fun AdminFacultySubjectsScreen(navigator: AppNavigator) {
    val vm = koinViewModel<FacultySubjectsViewModel>()
    ReloadOnReconnect { if (vm.state.value.error != null) vm.reload() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    AppScreen(title = "Предметы факультетов", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                AppCard {
                    SelectField("Факультет", state.faculties.map { SelectOption(it.id, it.name) }, state.facultyId, vm::selectFaculty)
                    SearchField(query, { query = it }, placeholder = "Поиск предмета")
                }
            }
            if (state.loading) item { LoadingState() }
            state.error?.let { error -> item { RetryBanner(error, onRetry = vm::reload) } }
            val matches = { s: SubjectDto -> query.isBlank() || s.name.contains(query.trim(), true) }
            val assignedIds = state.assigned.map { it.id }.toSet()
            item { SectionTitle("Читаются на факультете (${state.assigned.size})") }
            if (!state.loading && state.error == null && state.assigned.isEmpty()) item { Text("Предметы не назначены.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.assigned.filter(matches), key = { "a${it.id}" }) { subject ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.askRemove(subject) }) { Icon(Icons.Default.Delete, "Убрать") }
                    }
                }
            }
            item { SectionTitle("Доступные предметы") }
            items(state.allSubjects.filter { it.id !in assignedIds && matches(it) }, key = { "s${it.id}" }) { subject ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(subject.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.add(subject) }, enabled = state.busy == null) { Icon(Icons.Default.Add, "Добавить") }
                    }
                }
            }
        }
    }
    state.removing?.let { subject ->
        ConfirmDialog(
            title = "Убрать предмет?",
            message = "«${subject.name}» больше не будет читаться на факультете.",
            confirmText = "Убрать",
            destructive = true,
            onConfirm = vm::confirmRemove,
            onDismiss = { vm.askRemove(null) },
        )
    }
}

// endregion
