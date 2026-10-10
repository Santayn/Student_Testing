package org.santayn.testing.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.santayn.testing.mobile.core.files.OfflineFiles
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.storage.ResponseCache
import org.santayn.testing.mobile.core.ui.LocalNetworkStatus
import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import org.santayn.testing.mobile.core.ui.currentNetworkState
import org.santayn.testing.mobile.core.ui.offlineCopy
import org.santayn.testing.mobile.core.session.SessionState
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.LocalSnackbarHostState
import org.santayn.testing.mobile.core.ui.StudentTestingTheme
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.feature.auth.AccountPendingScreen
import org.santayn.testing.mobile.feature.auth.LoginScreen
import org.santayn.testing.mobile.navigation.MainShell

/**
 * Корень приложения. Koin должен быть запущен платформой до вызова (см. initKoin).
 */
@Composable
fun App() {
    val settings = koinInject<AppSettings>()
    val session = koinInject<SessionManager>()
    val cache = koinInject<ContextCache>()
    val network = koinInject<NetworkStatus>()
    val responseCache = koinInject<ResponseCache>()
    val offlineFiles = koinInject<OfflineFiles>()
    val themeMode by settings.themeMode.collectAsState()
    val textScale by settings.textScale.collectAsState()
    val highContrast by settings.highContrast.collectAsState()
    val state by session.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        if (state is SessionState.Restoring) {
            session.restore()
            // Сессию не удалось проверить из-за связи — сразу переходим на сохранённые данные.
            if (session.needsRevalidation) network.reportServerUnreachable()
        }
    }

    // Сессия, восстановленная без сети, проверяется при появлении связи.
    LaunchedEffect(Unit) {
        network.reconnected.collect { session.revalidate() }
    }

    // Офлайн-данные принадлежат пользователю: при выходе удаляются.
    val loggedOut = state is SessionState.LoggedOut
    LaunchedEffect(loggedOut) {
        if (loggedOut) {
            responseCache.clear()
            offlineFiles.clear()
        }
    }

    // Кэш контекста обучения привязан к сессии и рабочей роли.
    val loggedIn = state as? SessionState.LoggedIn
    LaunchedEffect(loggedIn?.user?.userId, loggedIn?.workspaceRole) { cache.invalidate() }

    StudentTestingTheme(themeMode, highContrast = highContrast, textScale = textScale.factor) {
        CompositionLocalProvider(
            LocalSnackbarHostState provides snackbarHostState,
            LocalNetworkStatus provides network,
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (val current = state) {
                    SessionState.Restoring -> LoadingState(message = "Восстанавливаем сессию…")
                    is SessionState.RestoreFailed -> RestoreFailedScreen(current.message)
                    is SessionState.LoggedOut -> LoginScreen(notice = current.notice)
                    is SessionState.LoggedIn -> {
                        val role = current.workspaceRole
                        if (role == null || !current.hasAccess) {
                            AccountPendingScreen(current.user)
                        } else {
                            // Смена пользователя или роли — новый граф навигации с главной.
                            key(current.user.userId, role) {
                                MainShell(role = role)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Сессию не удалось восстановить из-за связи, а сохранённого профиля нет (первый запуск после обновления). */
@Composable
private fun RestoreFailedScreen(message: String) {
    val session = koinInject<SessionManager>()
    val scope = rememberCoroutineScope()
    val offline = offlineCopy(currentNetworkState())
    ReloadOnReconnect { scope.launch { session.restore() } }
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                offline?.title ?: "Нет связи с сервером",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(offline?.hint ?: message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { scope.launch { session.restore() } }) { Text("Повторить") }
            OutlinedButton(onClick = { session.logout() }) { Text("Выйти") }
        }
    }
}
