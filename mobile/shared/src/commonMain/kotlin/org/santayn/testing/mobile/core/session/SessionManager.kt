package org.santayn.testing.mobile.core.session

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.toApiException
import org.santayn.testing.mobile.core.network.mapTransportErrors
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.core.util.parseInstantOrNull
import org.santayn.testing.mobile.data.model.AuthTokens
import org.santayn.testing.mobile.data.model.ChangePasswordRequest
import org.santayn.testing.mobile.data.model.CurrentUser
import org.santayn.testing.mobile.data.model.LoginRequest
import org.santayn.testing.mobile.data.model.RefreshRequest
import kotlin.time.Duration.Companion.seconds

sealed interface SessionState {
    /** Восстановление сессии при запуске. */
    data object Restoring : SessionState

    /** Не удалось восстановить из-за сети: токены сохранены, можно повторить. */
    data class RestoreFailed(val message: String) : SessionState

    data class LoggedOut(val notice: String? = null) : SessionState

    /**
     * @property workspaceRole активная рабочая роль; null — аккаунт ожидает доступа
     * (нет роли STUDENT/TEACHER/ADMIN или не привязана персона).
     */
    data class LoggedIn(
        val user: CurrentUser,
        val workspaceRole: WorkspaceRole?,
    ) : SessionState {
        val availableRoles: List<WorkspaceRole> get() = WorkspaceRole.available(user.roles)
        val hasAccess: Boolean get() = workspaceRole != null && user.hasWorkspaceAccess()
    }
}

/**
 * Управление сессией — порт frontend/src/stores/auth.js.
 *
 * - access/refresh токены хранятся в защищённом хранилище;
 * - refresh single-flight (refresh-токен ротируется и одноразовый);
 * - «эпоха» сессии увеличивается при каждом входе/выходе, ответы старой эпохи отбрасываются.
 */
class SessionManager(
    private val http: HttpClient,
    private val settings: AppSettings,
    private val secureStore: KeyValueStore,
    private val json: Json,
    private val appScope: CoroutineScope,
) {
    private val log = Logger.withTag("Session")
    private val refreshMutex = Mutex()

    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _epoch = MutableStateFlow(0)
    val epoch: Int get() = _epoch.value

    @kotlin.concurrent.Volatile
    private var tokens: AuthTokens? = loadTokens()

    private val baseUrl: String get() = settings.serverUrl.value

    val currentUser: CurrentUser?
        get() = (state.value as? SessionState.LoggedIn)?.user

    val workspaceRole: WorkspaceRole?
        get() = (state.value as? SessionState.LoggedIn)?.workspaceRole

    // region lifecycle

    /**
     * Восстановление сессии при запуске (auth.js: initialize).
     *
     * Если профиль сохранён, приложение открывается сразу с ним, а сессия проверяется в фоне:
     * запуск не ждёт сервер, и без связи можно работать с сохранёнными данными.
     * Сервер отклонил токены — выход; нет связи — проверка повторится при её появлении ([revalidate]).
     */
    suspend fun restore() {
        val saved = tokens
        if (saved == null) {
            _state.value = SessionState.LoggedOut()
            return
        }
        val cached = loadUser()
        if (cached != null) applyUser(cached) else _state.value = SessionState.Restoring
        val epochAtStart = epoch
        try {
            val access = validAccessToken() ?: run {
                if (epoch == epochAtStart) _state.value = SessionState.LoggedOut()
                return
            }
            val user = fetchMe(access)
            if (epoch != epochAtStart) return
            needsRevalidation = false
            applyUser(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (epoch != epochAtStart) return
            if (e.isNetworkError || e.isTimeout || (e.status ?: 0) >= 500) {
                if (cached != null) needsRevalidation = true else _state.value = SessionState.RestoreFailed(e.message)
            } else {
                clearLocal(notice = null)
            }
        }
    }

    /**
     * Сессия восстановлена без связи с сервером — при появлении сети нужно проверить,
     * что токены ещё действительны и права пользователя не изменились.
     */
    @kotlin.concurrent.Volatile
    var needsRevalidation: Boolean = false
        private set

    /** Проверка сессии, восстановленной офлайн. Ошибки связи оставляют офлайн-режим. */
    suspend fun revalidate() {
        if (!needsRevalidation || state.value !is SessionState.LoggedIn) return
        try {
            reloadUser()
            needsRevalidation = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.isUnauthorized) clearLocal(notice = "Сессия завершена. Войдите в систему снова.")
        }
    }

    suspend fun login(login: String, password: String, remember: Boolean) {
        val response = authCall {
            http.post("$baseUrl/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    LoginRequest(
                        login = login.trim(),
                        password = password,
                        lifetimeKind = if (remember) AuthTokens.LIFETIME_EXTENDED else null,
                    )
                )
            }
        }
        val newTokens: AuthTokens = response.body()
        startSession(newTokens)
        settings.lastLogin = login.trim()
    }

    suspend fun register(login: String, password: String) {
        val response = authCall {
            http.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(login = login.trim(), password = password))
            }
        }
        startSession(response.body())
        settings.lastLogin = login.trim()
    }

    private suspend fun startSession(newTokens: AuthTokens) {
        val epochAtStart = bumpEpoch()
        saveTokens(newTokens)
        val user = fetchMe(newTokens.accessToken)
        if (epoch != epochAtStart) return
        applyUser(user)
    }

    /** Повторная загрузка пользователя (GET /auth/me). */
    suspend fun reloadUser() {
        val access = validAccessToken() ?: return
        val epochAtStart = epoch
        val user = fetchMe(access)
        if (epoch == epochAtStart) applyUser(user)
    }

    /**
     * Обновить токены и пользователя (auth.js: refreshIdentity).
     * Роли зашиты в JWT, поэтому после изменения ролей админом нужна новая пара токенов.
     */
    suspend fun refreshIdentity() {
        val current = tokens ?: return
        val refreshed = refresh(current.accessToken) ?: return
        val epochAtStart = epoch
        val user = fetchMe(refreshed)
        if (epoch == epochAtStart) applyUser(user)
    }

    /** Выход: локальное состояние очищается сразу, revoke — best-effort. */
    fun logout(notice: String? = null) {
        val captured = tokens
        clearLocal(notice)
        if (captured != null) {
            appScope.launch {
                runCatching {
                    http.post("$baseUrl/auth/revoke") {
                        bearerAuth(captured.accessToken)
                        contentType(ContentType.Application.Json)
                        setBody(RefreshRequest(captured.refreshToken))
                    }
                }.onFailure { log.d { "revoke failed: ${it.message}" } }
            }
        }
    }

    /**
     * Смена пароля. Бэкенд отзывает все refresh-сессии, поэтому после успеха — выход.
     * 401 здесь означает «неверный текущий пароль», а не истёкшую сессию.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val access = validAccessToken() ?: throw ApiException.fromResponse(401, null)
        authCall(unauthorizedMessage = "Текущий пароль указан неверно.") {
            http.post("$baseUrl/auth/change-password") {
                bearerAuth(access)
                contentType(ContentType.Application.Json)
                setBody(ChangePasswordRequest(currentPassword, newPassword))
            }
        }
        clearLocal(notice = "Пароль изменён. Войдите с новым паролем.")
    }

    fun setWorkspaceRole(role: WorkspaceRole) {
        _state.update { current ->
            if (current is SessionState.LoggedIn && role in current.availableRoles) {
                settings.preferredWorkspaceRole = role.apiName
                current.copy(workspaceRole = role)
            } else {
                current
            }
        }
    }

    // endregion

    // region tokens for ApiClient

    /**
     * Действующий access-токен для запроса. Если до истечения меньше 30 секунд —
     * сначала обновляет его (auth.js: ensureAccessToken).
     * @return null, если сессии нет или её не удалось продлить.
     */
    suspend fun validAccessToken(): String? {
        val current = tokens ?: return null
        val expiresAt = parseInstantOrNull(current.accessTokenExpiresAtUtc)
        val expiresSoon = expiresAt == null || expiresAt - nowInstant() < REFRESH_SKEW
        return if (expiresSoon) refresh(current.accessToken) else current.accessToken
    }

    /**
     * Вызывается после 401 на обычном запросе.
     * Если токен уже обновил другой запрос — возвращает новый без повторного refresh.
     */
    suspend fun recoverFromUnauthorized(usedAccessToken: String): String? = refresh(usedAccessToken)

    /** Сессия больше недействительна (повторный 401). */
    fun invalidate() {
        if (tokens != null) clearLocal(notice = "Сессия завершена. Войдите в систему снова.")
    }

    /**
     * Single-flight refresh. [staleAccessToken] — токен, с которым вызывающий
     * обнаружил проблему: если в хранилище уже другой токен, refresh не нужен.
     */
    private suspend fun refresh(staleAccessToken: String): String? = refreshMutex.withLock {
        val current = tokens ?: return@withLock null
        if (current.accessToken != staleAccessToken) {
            return@withLock current.accessToken
        }
        val epochAtStart = epoch
        val response = mapTransportErrors {
            http.post("$baseUrl/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(current.refreshToken))
            }
        }
        if (!response.status.isSuccess()) {
            val error = response.toApiException(json)
            if (response.status.value in 400..499) {
                log.i { "refresh rejected: ${response.status}" }
                if (epoch == epochAtStart) clearLocal(notice = "Сессия завершена. Войдите в систему снова.")
                return@withLock null
            }
            throw error
        }
        val refreshed: AuthTokens = response.body()
        if (epoch != epochAtStart) return@withLock null
        saveTokens(refreshed)
        refreshed.accessToken
    }

    // endregion

    // region internals

    private suspend fun fetchMe(accessToken: String): CurrentUser {
        val response = mapTransportErrors {
            http.get("$baseUrl/auth/me") { bearerAuth(accessToken) }
        }
        if (!response.status.isSuccess()) throw response.toApiException(json)
        return response.body()
    }

    private fun applyUser(user: CurrentUser) {
        val role = if (user.hasWorkspaceAccess()) {
            val previous = (state.value as? SessionState.LoggedIn)?.workspaceRole?.apiName
            WorkspaceRole.resolve(user.roles, previous ?: settings.preferredWorkspaceRole)
        } else {
            null
        }
        saveUser(user)
        _state.value = SessionState.LoggedIn(user, role)
    }

    private suspend fun authCall(
        unauthorizedMessage: String = "Неверный логин или пароль.",
        block: suspend () -> HttpResponse,
    ): HttpResponse {
        val response = mapTransportErrors { block() }
        if (!response.status.isSuccess()) {
            val error = response.toApiException(json)
            throw when (response.status.value) {
                401 -> ApiException(401, error.code, unauthorizedMessage, error.fieldErrors)
                403 -> ApiException(
                    403, error.code,
                    error.message.takeIf { it.isNotBlank() } ?: "Недостаточно прав для выполнения операции.",
                )
                409 -> ApiException(409, error.code, "Пользователь с таким логином уже существует.")
                else -> error
            }
        }
        return response
    }

    private fun bumpEpoch(): Int {
        _epoch.update { it + 1 }
        return epoch
    }

    private fun clearLocal(notice: String?) {
        bumpEpoch()
        tokens = null
        needsRevalidation = false
        secureStore.putString(KEY_TOKENS, null)
        secureStore.putString(KEY_USER, null)
        _state.value = SessionState.LoggedOut(notice)
    }

    private fun saveTokens(value: AuthTokens) {
        tokens = value
        secureStore.putString(KEY_TOKENS, json.encodeToString(AuthTokens.serializer(), value))
    }

    private fun saveUser(user: CurrentUser) {
        secureStore.putString(KEY_USER, json.encodeToString(CurrentUser.serializer(), user))
    }

    private fun loadUser(): CurrentUser? = secureStore.getString(KEY_USER)?.let {
        runCatching { json.decodeFromString(CurrentUser.serializer(), it) }.getOrNull()
    }

    private fun loadTokens(): AuthTokens? = secureStore.getString(KEY_TOKENS)?.let {
        runCatching { json.decodeFromString(AuthTokens.serializer(), it) }.getOrNull()
    }

    // endregion

    private companion object {
        const val KEY_TOKENS = "auth_tokens"
        const val KEY_USER = "current_user"
        val REFRESH_SKEW = 30.seconds
    }
}
