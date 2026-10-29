package org.santayn.testing.mobile.core

import app.cash.turbine.test
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.santayn.testing.mobile.core.network.ApiClient
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.network.AppJson
import org.santayn.testing.mobile.core.network.ConnectivityObserver
import org.santayn.testing.mobile.core.network.NetworkState
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.SessionState
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.KeyValueStore
import org.santayn.testing.mobile.core.storage.MemoryBlobStore
import org.santayn.testing.mobile.core.storage.ResponseCache
import org.santayn.testing.mobile.core.util.nowInstant
import org.santayn.testing.mobile.data.model.AuthTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

private class Store : KeyValueStore {
    val map = mutableMapOf<String, String>()
    override fun getString(key: String) = map[key]
    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
    override fun keys() = map.keys
}

private class FakeConnectivity(connected: Boolean = true) : ConnectivityObserver {
    override val isConnected = MutableStateFlow(connected)
}

/** Бэкенд, который можно «выключить»: тогда соединение обрывается, как при недоступном сервере. */
private class SwitchableBackend {
    var up = true
    var gateway502 = false
    /** Сколько ближайших запросов ответить 502 (кратковременный сбой). */
    var transientFailures = 0
    var dataCalls = 0
    var body = """{"value":1}"""

    fun tokens() = AuthTokens(
        accessToken = "access",
        accessTokenExpiresAtUtc = (nowInstant() + 15.minutes).toString(),
        refreshToken = "refresh",
        refreshTokenExpiresAtUtc = (nowInstant() + 60.minutes).toString(),
    )

    val engine = MockEngine { request ->
        if (!up) throw Exception("Connection refused")
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        when (request.url.encodedPath) {
            "/api/v1/auth/me" -> respond(
                """{"userId":7,"login":"student","personId":5,"roles":["STUDENT"],"permissions":[]}""",
                HttpStatusCode.OK, json,
            )
            "/api/v1/data" -> {
                dataCalls++
                when {
                    gateway502 -> respond("Bad gateway", HttpStatusCode.BadGateway)
                    transientFailures > 0 -> {
                        transientFailures--
                        respond("Bad gateway", HttpStatusCode.BadGateway)
                    }
                    else -> respond(body, HttpStatusCode.OK, json)
                }
            }
            else -> respond("""{"code":"not_found"}""", HttpStatusCode.NotFound, json)
        }
    }
}

private class Env(
    val backend: SwitchableBackend,
    val connectivity: FakeConnectivity,
    val session: SessionManager,
    val api: ApiClient,
    val network: NetworkStatus,
    val secure: Store,
)

private fun TestScope.env(
    backend: SwitchableBackend = SwitchableBackend(),
    connectivity: FakeConnectivity = FakeConnectivity(),
    secure: Store = Store(),
    probeResult: () -> Boolean = { backend.up },
): Env {
    val http = HttpClient(backend.engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(AppJson) }
        install(HttpTimeout)
    }
    val settings = AppSettings(Store().apply { putString("server_url", "http://test/api/v1") })
    if (secure.getString("auth_tokens") == null) {
        secure.putString("auth_tokens", AppJson.encodeToString(AuthTokens.serializer(), backend.tokens()))
    }
    val scope: CoroutineScope = backgroundScope
    val session = SessionManager(http, settings, secure, AppJson, scope)
    val network = NetworkStatus(connectivity, scope) { probeResult() }
    val cache = ResponseCache(MemoryBlobStore(), AppJson)
    val api = ApiClient(http, session, settings, AppJson, network, cache)
    return Env(backend, connectivity, session, api, network, secure)
}

class OfflineCacheTests {

    @Test
    fun getFallsBackToCachedResponseWhenServerIsDown() = runTest {
        val e = env()
        e.session.restore()
        assertEquals(mapOf("value" to 1), e.api.get<Map<String, Int>>("/data"))

        e.backend.up = false
        assertEquals(mapOf("value" to 1), e.api.get<Map<String, Int>>("/data"), "должен вернуться сохранённый ответ")
        assertEquals(NetworkState.SERVER_UNAVAILABLE, e.network.state.value)
        assertTrue(e.network.cachedDataSince.value != null, "UI должен знать, что показаны сохранённые данные")
    }

    @Test
    fun withoutCacheTheErrorSaysWhatHappened() = runTest {
        val e = env()
        e.session.restore()

        // Сеть на устройстве выключена: «Нет подключения к интернету», а не «сервер недоступен».
        e.connectivity.isConnected.value = false
        e.backend.up = false
        runCurrent()
        val offline = assertFailsWith<ApiException> { e.api.get<Map<String, Int>>("/data") }
        assertTrue(offline.isOffline)
        assertEquals(ApiException.MSG_OFFLINE, offline.message)
        assertEquals(0, e.backend.dataCalls)

        // Сеть есть, но сервер не отвечает.
        e.connectivity.isConnected.value = true
        runCurrent()
        val down = assertFailsWith<ApiException> { e.api.get<Map<String, Int>>("/data") }
        assertTrue(down.isNetworkError && !down.isOffline)
        assertEquals(NetworkState.SERVER_UNAVAILABLE, e.network.state.value)
    }

    @Test
    fun transientGatewayErrorIsRetriedWhenNothingIsCached() = runTest {
        val e = env()
        e.session.restore()
        e.backend.transientFailures = 2
        assertEquals(mapOf("value" to 1), e.api.get<Map<String, Int>>("/data"))
        assertEquals(3, e.backend.dataCalls, "исходный запрос + два повтора")
    }

    @Test
    fun cachedDataIsShownImmediatelyOnFirstFailure() = runTest {
        val e = env()
        e.session.restore()
        e.api.get<Map<String, Int>>("/data")
        e.backend.dataCalls = 0
        e.backend.gateway502 = true
        assertEquals(mapOf("value" to 1), e.api.get<Map<String, Int>>("/data"))
        assertEquals(1, e.backend.dataCalls, "с сохранёнными данными повторов не ждём")
    }

    @Test
    fun knownOutageServesCacheWithoutWaitingForNetwork() = runTest {
        val e = env()
        e.session.restore()
        e.api.get<Map<String, Int>>("/data")
        e.backend.gateway502 = true
        e.api.get<Map<String, Int>>("/data")
        runCurrent() // state — stateIn в фоновом scope теста
        assertEquals(NetworkState.SERVER_UNAVAILABLE, e.network.state.value)

        e.backend.dataCalls = 0
        assertEquals(mapOf("value" to 1), e.api.get<Map<String, Int>>("/data"))
        assertEquals(0, e.backend.dataCalls, "при известной недоступности сервера — сразу из кеша")
    }

    @Test
    fun cacheIsPerUserAndQuery() = runTest {
        val cache = ResponseCache(MemoryBlobStore(), AppJson)
        cache.put("1|http://s/api/v1/data?a=1", "one")
        assertEquals("one", cache.get("1|http://s/api/v1/data?a=1")?.body)
        assertNull(cache.get("2|http://s/api/v1/data?a=1"))
        assertNull(cache.get("1|http://s/api/v1/data?a=2"))
        cache.clear()
        assertNull(cache.get("1|http://s/api/v1/data?a=1"))
    }

    @Test
    fun cacheEvictsOldestEntriesOverLimit() = runTest {
        val cache = ResponseCache(MemoryBlobStore(), AppJson, maxBytes = 10, maxEntries = 100)
        cache.put("a", "12345")
        cache.put("b", "12345")
        cache.put("c", "12345")
        assertNull(cache.get("a"), "самая старая запись должна быть удалена")
        assertEquals("12345", cache.get("c")?.body)
    }
}

class NetworkStatusTests {

    @Test
    fun reconnectedFiresWhenDeviceComesBackOnline() = runTest {
        val connectivity = FakeConnectivity(connected = false)
        val network = NetworkStatus(connectivity, backgroundScope) { true }
        runCurrent()
        assertEquals(NetworkState.NO_INTERNET, network.state.value)
        network.reconnected.test {
            connectivity.isConnected.value = true
            awaitItem()
            assertEquals(NetworkState.ONLINE, network.state.value)
        }
    }

    @Test
    fun serverRecoveryIsDetectedByProbe() = runTest {
        var serverUp = false
        val network = NetworkStatus(FakeConnectivity(), backgroundScope) { serverUp }
        network.reportServerUnreachable()
        runCurrent()
        assertEquals(NetworkState.SERVER_UNAVAILABLE, network.state.value)
        network.reconnected.test {
            serverUp = true
            // Периодическая проба (виртуальное время runTest).
            awaitItem()
            assertEquals(NetworkState.ONLINE, network.state.value)
        }
    }

    @Test
    fun serverIsNotBlamedWhenDeviceIsOffline() = runTest {
        val network = NetworkStatus(FakeConnectivity(connected = false), backgroundScope) { false }
        network.reportServerUnreachable()
        runCurrent()
        assertEquals(NetworkState.NO_INTERNET, network.state.value)
    }
}

class OfflineSessionTests {

    @Test
    fun restoreWithoutNetworkUsesSavedProfile() = runTest {
        val secure = Store()
        val first = env(secure = secure)
        first.session.restore()
        assertIs<SessionState.LoggedIn>(first.session.state.value)

        // Перезапуск приложения без связи с сервером.
        val backend = SwitchableBackend().apply { up = false }
        val second = env(backend = backend, secure = secure)
        second.session.restore()
        val state = assertIs<SessionState.LoggedIn>(second.session.state.value)
        assertEquals(7, state.user.userId)
        assertTrue(second.session.needsRevalidation)

        backend.up = true
        second.session.revalidate()
        assertTrue(!second.session.needsRevalidation)
    }

    @Test
    fun logoutForgetsSavedProfile() = runTest {
        val secure = Store()
        val e = env(secure = secure)
        e.session.restore()
        e.session.logout()
        assertNull(secure.getString("current_user"))
    }
}
