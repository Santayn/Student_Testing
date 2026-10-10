package org.santayn.testing.mobile.feature.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.santayn.testing.mobile.core.files.formatFileSize
import org.santayn.testing.mobile.core.files.toUploadFile
import org.santayn.testing.mobile.core.network.DownloadedFile
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CheckRow
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.DateField
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadStateContent
import org.santayn.testing.mobile.core.ui.MultiSelectField
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectField
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.SwitchRow
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatDateTime
import org.santayn.testing.mobile.data.api.DatabaseBackupsApi
import org.santayn.testing.mobile.data.api.RolesApi
import org.santayn.testing.mobile.data.api.UsersApi
import org.santayn.testing.mobile.data.model.PermissionDto
import org.santayn.testing.mobile.data.model.PermissionRequest
import org.santayn.testing.mobile.data.model.PersonDto
import org.santayn.testing.mobile.data.model.PersonRequest
import org.santayn.testing.mobile.data.model.RoleDto
import org.santayn.testing.mobile.data.model.RoleRequest
import org.santayn.testing.mobile.data.model.UserDto
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.navigation.AppNavigator

// region Users (views/admin/UsersView.vue, AdminUserDrawer.vue, AdminPersonEditor.vue)

data class UsersData(val users: List<UserDto>, val people: List<PersonDto>, val roles: List<RoleDto>)

data class UserForm(
    val user: UserDto,
    val active: Boolean,
    val personId: Int?,
    val roleIds: Set<Int>,
)

data class PersonForm(
    val id: Int? = null,
    val lastName: String = "",
    val firstName: String = "",
    val email: String = "",
    val phone: String = "",
    val dateOfBirth: LocalDate? = null,
)

class UsersViewModel(
    private val users: UsersApi,
    private val roles: RolesApi,
    private val session: SessionManager,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<UsersData>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _form = MutableStateFlow<UserForm?>(null)
    val form = _form.asStateFlow()
    private val _person = MutableStateFlow<PersonForm?>(null)
    val person = _person.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        load()
    }

    fun load() = launchSafe(onError = { _state.value = LoadState.Error(it.userMessage()) }) {
        coroutineScope {
            val u = async { users.getAll().sortedBy { it.login } }
            val p = async { users.getPeople().sortedByName { it.fullName } }
            val r = async { roles.getAll().sortedBy { it.name } }
            _state.value = LoadState.Ready(UsersData(u.await(), p.await(), r.await()))
        }
    }

    private val data get() = (_state.value as? LoadState.Ready)?.data

    fun open(user: UserDto?) {
        val d = data
        _error.value = null
        _form.value = user?.let { u ->
            UserForm(u, u.active, u.personId, d?.roles.orEmpty().filter { it.name in u.roles }.map { it.id }.toSet())
        }
    }

    fun update(transform: (UserForm) -> UserForm) = _form.update { it?.let(transform) }

    /** Изменения применяются только по «Сохранить» (как в AdminUserDrawer). */
    fun save() {
        val form = _form.value ?: return
        val original = form.user
        val d = data ?: return
        _busy.value = true
        _error.value = null
        launchSafe(onError = { e ->
            _busy.value = false
            _error.value = e.userMessage()
        }) {
            if (form.active != original.active) users.setActive(original.id, form.active)
            if (form.personId != original.personId) users.updatePersonBinding(original.id, form.personId)
            val originalRoleIds = d.roles.filter { it.name in original.roles }.map { it.id }.toSet()
            if (form.roleIds != originalRoleIds) users.updateRoles(original.id, form.roleIds.toList())
            cache.invalidate()
            _busy.value = false
            _form.value = null
            toast("Пользователь ${original.login} сохранён")
            if (original.id == session.currentUser?.userId) {
                runCatching { session.refreshIdentity() }
            }
            load()
        }
    }

    fun openPerson(person: PersonDto?) {
        _error.value = null
        _person.value = person?.let {
            PersonForm(it.id, it.lastName.orEmpty(), it.firstName.orEmpty(), it.email.orEmpty(), it.phone.orEmpty(),
                it.dateOfBirth?.take(10)?.let { d -> runCatching { LocalDate.parse(d) }.getOrNull() })
        } ?: PersonForm()
    }

    fun closePerson() {
        _person.value = null
    }

    fun updatePerson(transform: (PersonForm) -> PersonForm) = _person.update { it?.let(transform) }

    fun savePerson() {
        val p = _person.value ?: return
        val error = when {
            p.lastName.isBlank() -> "Введите фамилию."
            p.firstName.isBlank() -> "Введите имя."
            p.email.isNotBlank() && !p.email.contains('@') -> "Проверьте email."
            else -> null
        }
        if (error != null) {
            _error.value = error
            return
        }
        _busy.value = true
        _error.value = null
        launchSafe(onError = { e -> _busy.value = false; _error.value = e.userMessage() }) {
            val request = PersonRequest(
                firstName = p.firstName.trim(),
                lastName = p.lastName.trim(),
                dateOfBirth = p.dateOfBirth?.toString(),
                email = p.email.trim().ifBlank { null },
                phone = p.phone.trim().ifBlank { null },
            )
            val saved = if (p.id == null) users.createPerson(request) else users.updatePerson(p.id, request)
            _busy.value = false
            _person.value = null
            // Новая персона сразу выбирается для привязки.
            if (p.id == null) _form.update { it?.copy(personId = saved.id) }
            toast("Персона сохранена")
            val current = data
            if (current != null) {
                val people = (current.people.filter { it.id != saved.id } + saved).sortedByName { it.fullName }
                _state.value = LoadState.Ready(current.copy(people = people))
            }
        }
    }
}

enum class UserActiveFilter(val label: String) { ALL("Все"), ACTIVE("Активные"), BLOCKED("Заблокированные"), NO_PERSON("Без профиля") }

@Composable
fun AdminUsersScreen(navigator: AppNavigator) {
    val vm = koinViewModel<UsersViewModel>()
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val form by vm.form.collectAsState()
    val person by vm.person.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var roleFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var activeFilter by rememberSaveable { mutableStateOf(UserActiveFilter.ALL) }

    AppScreen(title = "Пользователи", onBack = navigator::back) { padding ->
        LoadStateContent(state, onRetry = { vm.load() }, onRefresh = { vm.load() }) { data ->
            val peopleById = data.people.associateBy { it.id }
            val filtered = data.users.filter { u ->
                val p = u.personId?.let { peopleById[it] }
                val text = listOfNotNull(u.login, p?.fullName, p?.email, p?.phone, u.roles.joinToString(" ")).joinToString(" ")
                (query.isBlank() || text.contains(query.trim(), true)) &&
                    (roleFilter == null || roleFilter in u.roles) &&
                    when (activeFilter) {
                        UserActiveFilter.ALL -> true
                        UserActiveFilter.ACTIVE -> u.active
                        UserActiveFilter.BLOCKED -> !u.active
                        UserActiveFilter.NO_PERSON -> u.personId == null
                    }
            }
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchField(query, { query = it }, placeholder = "Логин, ФИО, email, телефон, роль")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(roleFilter == null, { roleFilter = null }, { Text("Все роли") })
                            data.roles.forEach { r -> FilterChip(roleFilter == r.name, { roleFilter = r.name }, { Text(r.name) }) }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            UserActiveFilter.entries.forEach { f -> FilterChip(activeFilter == f, { activeFilter = f }, { Text(f.label) }) }
                        }
                        OutlinedButton(onClick = { vm.openPerson(null) }) {
                            Icon(Icons.Default.PersonAdd, null)
                            Text(" Новая персона")
                        }
                    }
                }
                item { SectionTitle("Найдено: ${filtered.size}") }
                if (filtered.isEmpty()) item { EmptyState("Пользователи не найдены", icon = Icons.Default.Badge) }
                items(filtered, key = { it.id }) { u ->
                    val p = u.personId?.let { peopleById[it] }
                    AppCard(onClick = { vm.open(u) }) {
                        Text(u.login, style = MaterialTheme.typography.titleMedium)
                        Text(p?.fullName ?: "Профиль не привязан", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!u.active) StatusChip("Заблокирован", Tone.DANGER)
                            u.roles.forEach { StatusChip(it, Tone.PRIMARY) }
                            if (u.roles.isEmpty()) StatusChip("Нет ролей", Tone.WARNING)
                        }
                    }
                }
            }
        }
    }

    val data = (state as? LoadState.Ready)?.data
    val err by vm.error.collectAsState()
    val busy by vm.busy.collectAsState()
    if (form != null && data != null) {
        val f = form!!
        val selectedRoles = data.roles.filter { it.id in f.roleIds }
        val effectivePermissions = selectedRoles.flatMap { it.permissions }.map { it.name }.distinct().sorted()
        EditorDialog(title = f.user.login, onDismiss = { vm.open(null) }, onSave = vm::save, busy = busy, error = if (person == null) err else null) {
            Text("Логин: ${f.user.login}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SwitchRow("Учётная запись активна", f.active, { v -> vm.update { it.copy(active = v) } },
                description = "Заблокированный пользователь не сможет войти")
            SelectField(
                "Привязанная персона",
                data.people.map { SelectOption(it.id, it.fullName, it.email) },
                f.personId,
                { id -> vm.update { it.copy(personId = id) } },
                allowClear = true,
                searchable = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.openPerson(null) }) { Text("Создать персону") }
                if (f.personId != null) {
                    OutlinedButton(onClick = { vm.openPerson(data.people.firstOrNull { it.id == f.personId }) }) {
                        Icon(Icons.Default.Edit, null)
                        Text(" Изменить")
                    }
                }
            }
            MultiSelectField("Роли", data.roles.map { SelectOption(it.id, it.name, it.description) }, f.roleIds,
                { ids -> vm.update { it.copy(roleIds = ids) } })
            SectionTitle("Итоговые права (${effectivePermissions.size})")
            Text(effectivePermissions.joinToString(", ").ifBlank { "—" }, style = MaterialTheme.typography.bodySmall)
            Banner("Изменения применяются только после нажатия «Сохранить». Новые роли вступят в силу при следующем обновлении сессии пользователя.", Tone.INFO)
        }
    }
    person?.let { p ->
        EditorDialog(
            title = if (p.id == null) "Новая персона" else "Персона",
            onDismiss = vm::closePerson,
            onSave = vm::savePerson,
            busy = busy,
            error = err,
        ) {
            FormField(p.lastName, { v -> vm.updatePerson { it.copy(lastName = v) } }, "Фамилия", required = true)
            FormField(p.firstName, { v -> vm.updatePerson { it.copy(firstName = v) } }, "Имя", required = true)
            FormField(p.email, { v -> vm.updatePerson { it.copy(email = v) } }, "Email", keyboardType = KeyboardType.Email)
            FormField(p.phone, { v -> vm.updatePerson { it.copy(phone = v) } }, "Телефон", keyboardType = KeyboardType.Phone)
            DateField("Дата рождения", p.dateOfBirth, { d -> vm.updatePerson { it.copy(dateOfBirth = d) } }, allowClear = true)
        }
    }
}

// endregion

// region Roles & permissions (views/admin/RolesPermissionsView.vue)

data class RolesData(val roles: List<RoleDto>, val permissions: List<PermissionDto>)

class RolesViewModel(private val api: RolesApi) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<RolesData>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _editing = MutableStateFlow<Pair<RoleDto, Set<Int>>?>(null)
    val editing = _editing.asStateFlow()
    private val _creating = MutableStateFlow<Boolean?>(null) // true — роль, false — право
    val creating = _creating.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    init {
        load()
    }

    fun load() = launchSafe(onError = { _state.value = LoadState.Error(it.userMessage()) }) {
        coroutineScope {
            val r = async { api.getAll().sortedBy { it.name } }
            val p = async { api.getPermissions().sortedBy { it.name } }
            _state.value = LoadState.Ready(RolesData(r.await(), p.await()))
        }
    }

    fun edit(role: RoleDto?) {
        _editing.value = role?.let { it to it.permissions.map { p -> p.id }.toSet() }
    }

    fun toggle(permissionId: Int, checked: Boolean) = _editing.update { current ->
        current?.let { (role, ids) -> role to (if (checked) ids + permissionId else ids - permissionId) }
    }

    fun savePermissions() {
        val (role, ids) = _editing.value ?: return
        _busy.value = true
        launchSafe(onError = { e -> _busy.value = false; toast(e.userMessage()) }) {
            api.setPermissions(role.id, ids.toList())
            _busy.value = false
            _editing.value = null
            toast("Права роли ${role.name} сохранены")
            load()
        }
    }

    fun openCreate(role: Boolean?) {
        _creating.value = role
    }

    fun create(name: String, description: String) {
        val isRole = _creating.value ?: return
        if (name.isBlank()) {
            toast("Введите название")
            return
        }
        _busy.value = true
        launchSafe(onError = { e -> _busy.value = false; toast(e.userMessage()) }) {
            if (isRole) {
                api.create(RoleRequest(name.trim().uppercase(), description.trim().ifBlank { null }))
            } else {
                api.createPermission(PermissionRequest(name.trim(), description.trim().ifBlank { null }))
            }
            _busy.value = false
            _creating.value = null
            toast(if (isRole) "Роль создана" else "Право создано")
            load()
        }
    }
}

@Composable
fun AdminRolesScreen(navigator: AppNavigator) {
    val vm = koinViewModel<RolesViewModel>()
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val editing by vm.editing.collectAsState()
    val creating by vm.creating.collectAsState()
    val busy by vm.busy.collectAsState()
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    AppScreen(title = "Роли и права", onBack = navigator::back) { padding ->
        LoadStateContent(state, onRetry = { vm.load() }, onRefresh = { vm.load() }) { data ->
            val usage = data.roles.flatMap { r -> r.permissions.map { it.id to r.name } }.groupBy({ it.first }, { it.second })
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(tab == 0, { tab = 0 }, { Text("Роли (${data.roles.size})") })
                        FilterChip(tab == 1, { tab = 1 }, { Text("Права (${data.permissions.size})") })
                    }
                    SearchField(query, { query = it })
                    OutlinedButton(onClick = { vm.openCreate(tab == 0) }) {
                        Icon(Icons.Default.Add, null)
                        Text(if (tab == 0) " Новая роль" else " Новое право")
                    }
                }
                if (tab == 0) {
                    items(data.roles.filter { query.isBlank() || it.name.contains(query.trim(), true) }, key = { "r${it.id}" }) { role ->
                        AppCard(onClick = { vm.edit(role) }) {
                            Text(role.name, style = MaterialTheme.typography.titleMedium)
                            role.description?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("Прав: ${role.permissions.size}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    items(data.permissions.filter { query.isBlank() || it.name.contains(query.trim(), true) }, key = { "p${it.id}" }) { p ->
                        AppCard {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            p.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            val roles = usage[p.id].orEmpty()
                            Text(if (roles.isEmpty()) "Не используется" else "Роли: ${roles.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    val data = (state as? LoadState.Ready)?.data
    editing?.let { (role, ids) ->
        var search by remember { mutableStateOf("") }
        EditorDialog(title = "Права: ${role.name}", onDismiss = { vm.edit(null) }, onSave = vm::savePermissions, busy = busy) {
            Text("Набор прав роли будет полностью заменён отмеченными.", style = MaterialTheme.typography.bodySmall)
            SearchField(search, { search = it })
            data?.permissions.orEmpty().filter { search.isBlank() || it.name.contains(search.trim(), true) }.forEach { p ->
                CheckRow(p.name, p.id in ids, { checked -> vm.toggle(p.id, checked) }, description = p.description)
            }
        }
    }
    creating?.let { isRole ->
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.openCreate(null) },
            title = { Text(if (isRole) "Новая роль" else "Новое право") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField(name, { name = it }, "Название", required = true)
                    FormField(description, { description = it }, "Описание")
                }
            },
            confirmButton = { TextButton(onClick = { vm.create(name, description) }, enabled = !busy) { Text("Создать") } },
            dismissButton = { TextButton(onClick = { vm.openCreate(null) }) { Text("Отмена") } },
        )
    }
}

// endregion

// region Database backups (views/admin/DatabaseBackupsView.vue)

data class BackupsState(
    val downloading: Boolean = false,
    val ready: DownloadedFile? = null,
    val restoreFile: PlatformFile? = null,
    val restoring: Boolean = false,
    val confirmRestore: Boolean = false,
    val message: Pair<Tone, String>? = null,
)

class BackupsViewModel(private val api: DatabaseBackupsApi, private val session: SessionManager) : BaseViewModel() {
    private val _state = MutableStateFlow(BackupsState())
    val state = _state.asStateFlow()

    fun createBackup() {
        _state.update { it.copy(downloading = true, message = null, ready = null) }
        launchSafe(onError = { e -> _state.update { it.copy(downloading = false, message = Tone.DANGER to e.userMessage("Не удалось создать резервную копию")) } }) {
            val file = api.create()
            _state.update { it.copy(downloading = false, ready = file) }
        }
    }

    /** Сохранить подготовленный файл в выбранное пользователем место. */
    fun writeTo(target: PlatformFile?) {
        val file = _state.value.ready ?: return
        if (target == null) return
        launchSafe(onError = { e -> _state.update { it.copy(message = Tone.DANGER to e.userMessage("Не удалось сохранить файл")) } }) {
            target.write(file.bytes)
            _state.update { it.copy(ready = null, message = Tone.SUCCESS to "Резервная копия сохранена: ${target.name}") }
        }
    }

    fun pickRestore(file: PlatformFile?) {
        if (file == null) return
        if (!file.name.endsWith(".sql", ignoreCase = true)) {
            _state.update { it.copy(message = Tone.DANGER to "Нужен файл резервной копии .sql") }
            return
        }
        _state.update { it.copy(restoreFile = file, message = null) }
    }

    fun askRestore(open: Boolean) = _state.update { it.copy(confirmRestore = open) }

    fun restore() {
        val file = _state.value.restoreFile ?: return
        _state.update { it.copy(confirmRestore = false, restoring = true, message = null) }
        launchSafe(onError = { e -> _state.update { it.copy(restoring = false, message = Tone.DANGER to e.userMessage("Не удалось восстановить базу данных")) } }) {
            val result = api.restore(file.toUploadFile())
            _state.update {
                it.copy(
                    restoring = false,
                    restoreFile = null,
                    message = Tone.SUCCESS to "База восстановлена из ${result.fileName ?: file.name} " +
                        "(${formatFileSize(result.sizeBytes)}, ${formatDateTime(result.restoredAtUtc)}). Рекомендуется войти заново.",
                )
            }
        }
    }

    fun relogin() = session.logout()
}

@Composable
fun AdminBackupsScreen(navigator: AppNavigator) {
    val vm = koinViewModel<BackupsViewModel>()
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val saver = rememberFileSaverLauncher(dialogSettings = FileKitDialogSettings.createDefault()) { file -> vm.writeTo(file) }
    val picker = rememberFilePickerLauncher(type = FileKitType.File(listOf("sql"))) { file -> vm.pickRestore(file) }

    // Как только файл получен — предлагаем выбрать место сохранения.
    LaunchedEffect(state.ready) {
        state.ready?.let { saver.launch(it.fileName.removeSuffix(".sql"), "sql") }
    }

    AppScreen(title = "Резервные копии", onBack = navigator::back) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            state.message?.let { (tone, text) -> item { Banner(text, tone) } }
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, null, tint = MaterialTheme.colorScheme.primary)
                        Text("  Выгрузка", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Полный SQL-дамп базы данных. Файлы материалов лекций в дамп не входят.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BusyButton("Скачать резервную копию", onClick = vm::createBackup, busy = state.downloading,
                        icon = Icons.Default.Download, modifier = Modifier.fillMaxWidth())
                    if (state.ready != null) {
                        OutlinedButton(onClick = { state.ready?.let { saver.launch(it.fileName.removeSuffix(".sql"), "sql") } }) {
                            Text("Сохранить ${state.ready!!.fileName} (${formatFileSize(state.ready!!.bytes.size.toLong())})")
                        }
                    }
                }
            }
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.error)
                        Text("  Восстановление", style = MaterialTheme.typography.titleMedium)
                    }
                    Banner("Восстановление перезапишет текущие данные. Перед восстановлением сделайте свежую резервную копию.", Tone.WARNING)
                    OutlinedButton(onClick = { picker.launch() }, enabled = !state.restoring) {
                        Icon(Icons.Default.UploadFile, null)
                        Text(" ${state.restoreFile?.name ?: "Выбрать файл .sql"}")
                    }
                    BusyButton("Восстановить", onClick = { vm.askRestore(true) }, busy = state.restoring,
                        enabled = state.restoreFile != null, modifier = Modifier.fillMaxWidth())
                    if (state.message?.first == Tone.SUCCESS && state.message?.second?.startsWith("База восстановлена") == true) {
                        OutlinedButton(onClick = vm::relogin) { Text("Войти заново") }
                    }
                }
            }
        }
    }
    if (state.confirmRestore) {
        ConfirmDialog(
            title = "Восстановить базу данных?",
            message = "Все текущие данные будут заменены данными из «${state.restoreFile?.name}». Действие нельзя отменить.",
            confirmText = "Восстановить",
            destructive = true,
            onConfirm = vm::restore,
            onDismiss = { vm.askRestore(false) },
        )
    }
}

// endregion
