package org.santayn.testing.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.PendingReason
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.pendingReason
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CheckRow
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LocalNetworkStatus
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.data.model.CurrentUser
import org.santayn.testing.mobile.feature.profile.DisplaySettingsDialog

private const val LOGIN_MAX = 100
private const val PASSWORD_MIN = 6
private const val PASSWORD_MAX = 200

/**
 * Экран входа (views/auth/LoginView.vue) + регистрация (RegisterView.vue).
 * Регистрация на сервере может быть выключена — тогда бэкенд вернёт ошибку.
 */
@Composable
fun LoginScreen(notice: String?) {
    val session = koinInject<SessionManager>()
    val settings = koinInject<AppSettings>()
    val scope = rememberCoroutineScope()
    val serverUrl by settings.serverUrl.collectAsState()

    var registerMode by rememberSaveable { mutableStateOf(false) }
    var login by rememberSaveable { mutableStateOf(settings.lastLogin.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    var remember by rememberSaveable { mutableStateOf(true) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var repeatError by remember { mutableStateOf<String?>(null) }
    var serverDialog by rememberSaveable { mutableStateOf(false) }
    var displayDialog by rememberSaveable { mutableStateOf(false) }
    val network = LocalNetworkStatus.current

    fun validate(): Boolean {
        loginError = when {
            login.isBlank() -> "Введите логин"
            login.trim().length > LOGIN_MAX -> "Не более $LOGIN_MAX символов"
            else -> null
        }
        passwordError = when {
            password.isEmpty() -> "Введите пароль"
            password.length < PASSWORD_MIN -> "Минимум $PASSWORD_MIN символов"
            password.length > PASSWORD_MAX -> "Не более $PASSWORD_MAX символов"
            else -> null
        }
        repeatError = if (registerMode && repeat != password) "Пароли не совпадают" else null
        return loginError == null && passwordError == null && repeatError == null
    }

    fun submit() {
        if (busy || !validate()) return
        busy = true
        error = null
        scope.launch {
            try {
                if (registerMode) session.register(login, password) else session.login(login, password, remember)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                error = when {
                    registerMode && e.isForbidden ->
                        "Самостоятельная регистрация отключена. Обратитесь к администратору."
                    registerMode && e.isUnauthorized -> "Проверьте логин и пароль и повторите попытку."
                    e.isNetworkError && network?.isDeviceOffline == true -> ApiException.MSG_OFFLINE
                    else -> e.message
                }
            } catch (e: Exception) {
                error = e.userMessage()
            } finally {
                busy = false
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(AppTheme.status.shell),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = AppTheme.status.onShell, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("STUDENT TESTING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (registerMode) "Регистрация" else "Вход",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (registerMode) {
                            "После регистрации администратор должен привязать профиль и назначить роль."
                        } else {
                            "Войдите, чтобы продолжить работу в системе."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (notice != null && !registerMode) Banner(notice, Tone.SUCCESS)
                    if (error != null) Banner(error!!, Tone.DANGER)

                    FormField(
                        value = login,
                        onValueChange = { login = it; loginError = null },
                        label = "Логин",
                        error = loginError,
                        maxLength = LOGIN_MAX,
                        required = true,
                    )
                    FormField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = "Пароль",
                        error = passwordError,
                        password = !showPassword,
                        keyboardType = KeyboardType.Password,
                        maxLength = PASSWORD_MAX,
                        required = true,
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль",
                                )
                            }
                        },
                    )
                    if (registerMode) {
                        FormField(
                            value = repeat,
                            onValueChange = { repeat = it; repeatError = null },
                            label = "Повторите пароль",
                            error = repeatError,
                            password = !showPassword,
                            keyboardType = KeyboardType.Password,
                            maxLength = PASSWORD_MAX,
                            required = true,
                        )
                    } else {
                        CheckRow(
                            title = "Запомнить меня",
                            description = "Сессия будет действовать 30 дней",
                            checked = remember,
                            onCheckedChange = { remember = it },
                        )
                    }
                    BusyButton(
                        text = if (registerMode) "Зарегистрироваться" else "Войти",
                        onClick = ::submit,
                        busy = busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            registerMode = !registerMode
                            error = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            if (registerMode) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        // Поверх формы: адрес сервера и «версия для слабовидящих» (крупный текст, контраст, тема) — до входа.
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            IconButton(onClick = { serverDialog = true }) {
                Icon(Icons.Default.Dns, contentDescription = "Адрес сервера")
            }
            IconButton(onClick = { displayDialog = true }) {
                Icon(Icons.Default.AccessibilityNew, contentDescription = "Специальные возможности")
            }
        }
    }

    if (displayDialog) DisplaySettingsDialog(onDismiss = { displayDialog = false })
    if (serverDialog) {
        ServerDialog(
            current = serverUrl,
            onDismiss = { serverDialog = false },
            onSave = {
                settings.setServerUrl(it)
                serverDialog = false
            },
            onReset = {
                settings.resetServerUrl()
                serverDialog = false
            },
        )
    }
}

@Composable
fun ServerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Адрес сервера") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Адрес API бэкенда. Можно указать только хост, например 192.168.0.10 — " +
                        "тогда будет использован http://192.168.0.10/api/v1 (nginx фронтенда).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FormField(value = value, onValueChange = { value = it }, label = "URL", keyboardType = KeyboardType.Uri)
                Text(
                    "Эмулятор Android: http://10.0.2.2:8080/api/v1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("По умолчанию") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

/**
 * Аккаунт ожидает доступа (views/auth/AccountPendingView.vue).
 */
@Composable
fun AccountPendingScreen(user: CurrentUser) {
    val session = koinInject<SessionManager>()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checkedOnce by remember { mutableStateOf(false) }

    val reasonText = when (user.pendingReason()) {
        PendingReason.NO_ROLE_AND_PERSON ->
            "Администратор ещё не привязал к учётной записи профиль (персону) и не назначил рабочую роль."
        PendingReason.NO_ROLE -> "Администратор ещё не назначил учётной записи рабочую роль: студент, преподаватель или администратор."
        PendingReason.NO_PERSON -> "Администратор ещё не привязал к учётной записи профиль (персону)."
        null -> "Доступ появился — нажмите «Проверить доступ»."
    }

    Box(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = AppTheme.status.warning, modifier = Modifier.size(40.dp))
                Text("Доступ ещё не настроен", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text("Вы вошли как ${user.login}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(reasonText, textAlign = TextAlign.Center)
                if (checkedOnce && error == null) {
                    Banner("Доступ пока не изменился. Попробуйте позже.", Tone.WARNING)
                }
                if (error != null) Banner(error!!, Tone.DANGER)
                BusyButton(
                    text = "Проверить доступ",
                    busy = busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                session.refreshIdentity()
                                checkedOnce = true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.userMessage()
                            } finally {
                                busy = false
                            }
                        }
                    },
                )
                BusyButton(text = "Выйти", outlined = true, modifier = Modifier.fillMaxWidth(), onClick = { session.logout() })
            }
        }
    }
}
