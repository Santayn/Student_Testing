package org.santayn.testing.mobile.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.santayn.testing.mobile.core.network.ApiClient
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.AppJson
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.SessionState
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.data.model.AuthTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes

private class MemoryStore : KeyValueStore {
    val map = mutableMapOf<String, String>()
    override fun getString(key: String) = map[key]
    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
    override fun keys() = map.keys
}

/**
 * Фейковый бэкенд: выдаёт токены «access-N»/«refresh-N», ротирует refresh,
 * защищённый ресурс /data принимает только актуальный access-токен.
 */
private class FakeBackend {
    var tokenVersion = 1
    var refreshCalls = 0
    var revokedRefresh = mutableSetOf<String>()
    var dataCalls = 0
    var rejectRefresh = false

    fun tokens(version: Int, expiresInMinutes: Int = 15) = AuthTokens(
        accessToken = "access-$version",
        accessTokenExpiresAtUtc = (nowInstant() + expiresInMinutes.minutes).toString(),
        refreshToken = "refresh-$version",
        refreshTokenExpiresAtUtc = (nowInstant() + 60.minutes).toString(),
    )

    suspend fun MockRequestHandleScope.handle(request: HttpRequestData) = when (request.url.encodedPath) {
        "/api/v1/auth/refresh" -> {
            refreshCalls++
            delay(50)
            if (rejectRefresh) {
                json("""{"code":"unauthorized","message":"Refresh token has already been revoked"}""", HttpStatusCode.Unauthorized)
            } else {
                tokenVersion++
                json(AppJson.encodeToString(AuthTokens.serializer(), tokens(tokenVersion)))
            }
        }
        "/api/v1/auth/me" -> json(
            """{"userId":1,"login":"student","personId":5,"roles":["STUDENT"],"permissions":[]}"""
        )
        "/api/v1/data" -> {
            dataCalls++
            val auth = request.headers[HttpHeaders.Authorization]
            if (auth == "Bearer access-$tokenVersion") json("""{"ok":true}""")
            else json("""{"code":"unauthorized","message":"Unauthorized"}""", HttpStatusCode.Unauthorized)
        }
        else -> json("""{"code":"not_found","message":"not found"}""", HttpStatusCode.NotFound)
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
}

class SessionAndApiClientTests {

    private fun setup(backend: FakeBackend, savedTokens: AuthTokens?): Triple<SessionManager, ApiClient, MemoryStore> {
        val http = HttpClient(MockEngine { request -> with(backend) { handle(request) } }) {
            expectSuccess = false
            install(ContentNegotiation) { json(AppJson) }
            install(HttpTimeout)
        }
        val settingsStore = MemoryStore().apply { putString("server_url", "http://test/api/v1") }
        val secure = MemoryStore()
        if (savedTokens != null) secure.putString("auth_tokens", AppJson.encodeToString(AuthTokens.serializer(), savedTokens))
        val settings = AppSettings(settingsStore)
        val session = SessionManager(http, settings, secure, AppJson, CoroutineScope(SupervisorJob() + Dispatchers.Default))
        return Triple(session, ApiClient(http, session, settings, AppJson), secure)
    }

    @Test
    fun restoreLoadsUserAndWorkspaceRole() = runTest {
        val backend = FakeBackend()
        val (session, _, _) = setup(backend, backend.tokens(1))
        session.restore()
        val state = assertIs<SessionState.LoggedIn>(session.state.value)
        assertEquals(WorkspaceRole.STUDENT, state.workspaceRole)
    }

    @Test
    fun concurrentRequestsWithExpiredTokenRefreshOnlyOnce() = runTest {
        val backend = FakeBackend()
        // Access-токен истекает через 0 минут — нужен проактивный refresh.
        val (session, api, _) = setup(backend, backend.tokens(1, expiresInMinutes = 0))
        val results = (1..5).map { async { api.get<Map<String, Boolean>>("/data") } }.awaitAll()
        assertEquals(5, results.size)
        assertEquals(1, backend.refreshCalls, "refresh-токен одноразовый: обновление должно быть single-flight")
    }

    @Test
    fun unauthorizedResponseTriggersOneRefreshAndRetry() = runTest {
        val backend = FakeBackend()
        val (session, api, _) = setup(backend, backend.tokens(1))
        // Сервер уже «повернул» токены (например, вход с другого устройства) — текущий access недействителен.
        backend.tokenVersion = 2
        val result = api.get<Map<String, Boolean>>("/data")
        assertEquals(true, result["ok"])
        assertEquals(1, backend.refreshCalls)
        assertEquals(2, backend.dataCalls, "исходный запрос + один повтор")
    }

    @Test
    fun rejectedRefreshLogsOut() = runTest {
        val backend = FakeBackend().apply { rejectRefresh = true }
        val (session, api, secure) = setup(backend, backend.tokens(1, expiresInMinutes = 0))
        assertFailsWith<ApiException> { api.get<Map<String, Boolean>>("/data") }
        assertIs<SessionState.LoggedOut>(session.state.value)
        assertEquals(null, secure.getString("auth_tokens"), "токены должны быть удалены")
    }
}
