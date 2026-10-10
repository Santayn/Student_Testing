package org.santayn.testing.mobile.feature.profile

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.SessionState
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.InfoRow
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatDate
import org.santayn.testing.mobile.data.api.GroupsApi
import org.santayn.testing.mobile.data.api.TeachingApi
import org.santayn.testing.mobile.data.model.CurrentUser
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.StudentContextLoader
import org.santayn.testing.mobile.domain.TeacherContextLoader
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.feature.auth.ServerDialog
import org.santayn.testing.mobile.navigation.AppNavigator

/** Учебный контекст в профиле (useProfileContext.js). */
data class ProfileContext(
    val studentGroups: List<String> = emptyList(),
    val studentFaculties: List<String> = emptyList(),
    val studentSubjects: List<String> = emptyList(),
    val teacherSubjects: List<String> = emptyList(),
    val teacherGroups: List<String> = emptyList(),
)

class ProfileViewModel(
    private val session: SessionManager,
    private val studentLoader: StudentContextLoader,
    private val teacherLoader: TeacherContextLoader,
    private val teaching: TeachingApi,
    private val groups: GroupsApi,
    private val cache: ContextCache,
) : BaseViewModel() {

    private val _context = MutableStateFlow<LoadState<ProfileContext>>(LoadState.Loading)
    val context = _context.asStateFlow()

    init {
        load()
    }

    fun load() {
        _context.value = LoadState.Loading
        launchSafe(onError = { _context.value = LoadState.Error(it.userMessage()) }) {
            val user = session.currentUser ?: return@launchSafe
            val roles = WorkspaceRole.available(user.roles)
            var result = ProfileContext()
            if (WorkspaceRole.STUDENT in roles) {
                val ctx = studentLoader.load(user.personId)
                result = result.copy(
                    studentGroups = ctx.groups.map { it.name },
                    studentFaculties = ctx.faculties.map { it.name },
                    studentSubjects = ctx.subjects.map { it.name },
                )
            }
            if (WorkspaceRole.TEACHER in roles && user.personId != null) {
                val ctx = teacherLoader.load(user.personId, isAdmin = false)
                val assignments = ctx.memberships.map { m ->
                    viewModelScope.async { teaching.getAssignments(subjectMembershipId = m.id) }
                }.awaitAll().flatten()
                val groupIds = assignments.mapNotNull { it.groupId }.toSet()
                val groupNames = if (groupIds.isEmpty()) {
                    emptyList()
                } else {
                    cache.load("groups:catalog", ContextCache.REFERENCE_TTL) { groups.getAll() }
                        .filter { it.id in groupIds }.sortedByName { it.name }.map { it.name }
                }
                result = result.copy(teacherSubjects = ctx.subjects.map { it.name }, teacherGroups = groupNames)
            }
            _context.value = LoadState.Ready(result)
        }
    }
}

@Composable
fun ProfileScreen(navigator: AppNavigator) {
    val session = koinInject<SessionManager>()
    val settings = koinInject<AppSettings>()
    val viewModel = koinViewModel<ProfileViewModel>()
    ReloadOnReconnect { viewModel.load() }
    CollectMessages(viewModel)
    val state by session.state.collectAsState()
    val loggedIn = state as? SessionState.LoggedIn ?: return
    val user = loggedIn.user
    val context by viewModel.context.collectAsState()
    val server by settings.serverUrl.collectAsState()

    var passwordDialog by rememberSaveable { mutableStateOf(false) }
    var serverDialog by rememberSaveable { mutableStateOf(false) }
    var logoutConfirm by rememberSaveable { mutableStateOf(false) }

    AppScreen(title = "Профиль") { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item { AccountCard(user, loggedIn) }

            if (loggedIn.availableRoles.size > 1) {
                item { SectionTitle("Рабочая роль") }
                item {
                    AppCard {
                        Text(
                            "От роли зависят разделы приложения.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            loggedIn.availableRoles.forEach { role ->
                                FilterChip(
                                    selected = role == loggedIn.workspaceRole,
                                    onClick = { session.setWorkspaceRole(role) },
                                    label = { Text(role.title) },
                                )
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Личные данные") }
            item {
                AppCard {
                    val person = user.person
                    if (person == null) {
                        Text("Профиль не привязан. Обратитесь к администратору.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        InfoRow("Фамилия", person.lastName)
                        InfoRow("Имя", person.firstName)
                        InfoRow("Дата рождения", formatDate(person.dateOfBirth))
                        InfoRow("Email", person.email)
                        InfoRow("Телефон", person.phone)
                        Text(
                            "Личные данные изменяет администратор.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionTitle("Учебный контекст") }
            item {
                when (val c = context) {
                    LoadState.Loading -> AppCard { LoadingState(Modifier.fillMaxWidth()) }
                    is LoadState.Error -> Banner(c.message, Tone.DANGER, title = "Не удалось загрузить контекст")
                    is LoadState.Ready -> ContextCard(c.data, WorkspaceRole.available(user.roles))
                }
            }

            item { SectionTitle("Оформление и специальные возможности") }
            item { AppCard { DisplaySettings() } }

            item { SectionTitle("Данные для работы без сети") }
            item { AppCard { OfflineStorageSettings() } }

            item { SectionTitle("Безопасность") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BusyButton("Сменить пароль", onClick = { passwordDialog = true }, outlined = true,
                        icon = Icons.Default.Key, modifier = Modifier.fillMaxWidth())
                    BusyButton("Адрес сервера", onClick = { serverDialog = true }, outlined = true,
                        icon = Icons.Default.Dns, modifier = Modifier.fillMaxWidth())
                    BusyButton("Выйти", onClick = { logoutConfirm = true }, icon = Icons.AutoMirrored.Filled.Logout,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (passwordDialog) ChangePasswordDialog(onDismiss = { passwordDialog = false })
    if (serverDialog) {
        ServerDialog(
            current = server,
            onDismiss = { serverDialog = false },
            onSave = { settings.setServerUrl(it); serverDialog = false },
            onReset = { settings.resetServerUrl(); serverDialog = false },
        )
    }
    if (logoutConfirm) {
        AlertDialog(
            onDismissRequest = { logoutConfirm = false },
            title = { Text("Выйти из аккаунта?") },
            confirmButton = { TextButton(onClick = { session.logout() }) { Text("Выйти") } },
            dismissButton = { TextButton(onClick = { logoutConfirm = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun AccountCard(user: CurrentUser, state: SessionState.LoggedIn) {
    AppCard {
        Text(user.displayName, style = MaterialTheme.typography.titleLarge)
        Text("Логин: ${user.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            state.availableRoles.forEach { role ->
                StatusChip(role.title, if (role == state.workspaceRole) Tone.PRIMARY else Tone.NEUTRAL)
            }
            if (!user.isActive) StatusChip("Неактивен", Tone.DANGER)
        }
    }
}

@Composable
private fun ContextCard(context: ProfileContext, roles: List<WorkspaceRole>) {
    AppCard {
        if (WorkspaceRole.STUDENT in roles) {
            InfoRow("Группы", context.studentGroups.joinToString(", ").ifBlank { "Нет активной группы" })
            InfoRow("Факультеты", context.studentFaculties.joinToString(", "))
            InfoRow("Предметы", context.studentSubjects.joinToString(", "))
        }
        if (WorkspaceRole.TEACHER in roles) {
            InfoRow("Преподаваемые предметы", context.teacherSubjects.joinToString(", ").ifBlank { "Не назначены" })
            InfoRow("Группы", context.teacherGroups.joinToString(", "))
        }
        if (WorkspaceRole.STUDENT !in roles && WorkspaceRole.TEACHER !in roles) {
            Text("Администратор работает со всеми предметами и группами.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Смена пароля (composables/profile/usePasswordChange.js). После успеха — выход. */
@Composable
private fun ChangePasswordDialog(onDismiss: () -> Unit) {
    val session = koinInject<SessionManager>()
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var nextError by remember { mutableStateOf<String?>(null) }
    var repeatError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Смена пароля") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) Banner(error!!, Tone.DANGER)
                FormField(current, { current = it }, "Текущий пароль", password = true, keyboardType = KeyboardType.Password)
                FormField(next, { next = it; nextError = null }, "Новый пароль", error = nextError, password = true,
                    keyboardType = KeyboardType.Password)
                FormField(repeat, { repeat = it; repeatError = null }, "Повторите новый пароль", error = repeatError,
                    password = true, keyboardType = KeyboardType.Password)
                Text(
                    "После смены пароля потребуется войти заново на всех устройствах.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && current.isNotEmpty(),
                onClick = {
                    nextError = when {
                        next.length < 6 -> "Минимум 6 символов"
                        next.length > 200 -> "Не более 200 символов"
                        next == current -> "Новый пароль должен отличаться от текущего"
                        else -> null
                    }
                    repeatError = if (repeat != next) "Пароли не совпадают" else null
                    if (nextError != null || repeatError != null) return@TextButton
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            session.changePassword(current, next)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: ApiException) {
                            error = if (e.isUnauthorized) "Текущий пароль указан неверно." else e.message
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(if (busy) "Сохранение…" else "Сменить") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") } },
    )
}
