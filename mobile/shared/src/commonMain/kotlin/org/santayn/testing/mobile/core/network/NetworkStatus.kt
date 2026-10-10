package org.santayn.testing.mobile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.santayn.testing.mobile.core.storage.AppSettings
import kotlin.time.Instant

/** Состояние связи с бэкендом. */
enum class NetworkState {
    ONLINE,

    /** На устройстве нет сети (авиарежим, Wi-Fi и мобильные данные выключены). */
    NO_INTERNET,

    /** Сеть есть, но сервер не отвечает или шлюз вернул 502–504. */
    SERVER_UNAVAILABLE,
}

/** Состояние сети устройства по данным ОС (ConnectivityManager / NWPathMonitor). */
interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
}

/** Наблюдатель для тестов и превью: сеть всегда есть. */
object AlwaysConnected : ConnectivityObserver {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
}

/**
 * Сводное состояние связи: сеть устройства + доступность сервера.
 *
 * Доступность сервера отмечает [ApiClient] по результатам запросов. Пока сервер недоступен,
 * периодически выполняется проба, а при восстановлении связи испускается [reconnected] —
 * экраны по нему перезагружают данные.
 */
class NetworkStatus(
    private val connectivity: ConnectivityObserver,
    scope: CoroutineScope,
    private val probe: suspend () -> Boolean,
) {
    private val serverReachable = MutableStateFlow(true)

    val state: StateFlow<NetworkState> = combine(connectivity.isConnected, serverReachable) { connected, reachable ->
        when {
            !connected -> NetworkState.NO_INTERNET
            !reachable -> NetworkState.SERVER_UNAVAILABLE
            else -> NetworkState.ONLINE
        }
    }.stateIn(scope, SharingStarted.Eagerly, initialState())

    private val _cachedDataSince = MutableStateFlow<Instant?>(null)

    /** Самая старая дата сохранённых данных, показанных во время проблем со связью. */
    val cachedDataSince: StateFlow<Instant?> = _cachedDataSince.asStateFlow()

    private val _reconnected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Связь восстановилась после NO_INTERNET или SERVER_UNAVAILABLE. */
    val reconnected: SharedFlow<Unit> = _reconnected.asSharedFlow()

    val isDeviceOffline: Boolean get() = !connectivity.isConnected.value

    init {
        scope.launch {
            var previous = state.value
            state.collectLatest { current ->
                val wasOffline = previous != NetworkState.ONLINE
                previous = current
                if (current == NetworkState.ONLINE && wasOffline) {
                    // Сеть только что поднялась (DHCP, переподключение VPN) — даём ей устояться,
                    // иначе первая же перезагрузка экрана может упасть по таймауту.
                    delay(RECONNECT_SETTLE_MS)
                    _cachedDataSince.value = null
                    _reconnected.tryEmit(Unit)
                }
            }
        }
        // Пока сервер недоступен — периодическая проба.
        scope.launch {
            state.collectLatest { current ->
                if (current == NetworkState.SERVER_UNAVAILABLE) {
                    var interval = PROBE_INITIAL_MS
                    while (true) {
                        delay(interval)
                        if (checkServer()) break
                        interval = (interval * 2).coerceAtMost(PROBE_MAX_MS)
                    }
                }
            }
        }
        // Сеть устройства вернулась, а сервер помечен недоступным — проверяем сразу.
        scope.launch {
            connectivity.isConnected.drop(1).filter { it }.collect {
                if (!serverReachable.value) checkServer()
            }
        }
    }

    private fun initialState() =
        if (connectivity.isConnected.value) NetworkState.ONLINE else NetworkState.NO_INTERNET

    fun reportServerReachable() {
        serverReachable.value = true
    }

    fun reportServerUnreachable() {
        // Без сети на устройстве о сервере ничего сказать нельзя.
        if (connectivity.isConnected.value) serverReachable.value = false
    }

    /** На экран выведены сохранённые данные от [savedAt]. */
    fun reportCachedData(savedAt: Instant) {
        _cachedDataSince.update { current -> if (current == null || savedAt < current) savedAt else current }
    }

    /** Проверка доступности сервера (кнопка «Проверить» и периодическая проба). */
    suspend fun checkServer(): Boolean {
        if (!connectivity.isConnected.value) return false
        val ok = runCatching { probe() }.getOrDefault(false)
        serverReachable.value = ok
        return ok
    }

    companion object {
        const val PROBE_INITIAL_MS = 5_000L
        const val PROBE_MAX_MS = 60_000L
        const val RECONNECT_SETTLE_MS = 1_000L
        private const val PROBE_TIMEOUT_MS = 5_000L

        /** Любой HTTP-ответ, кроме 502–504 от шлюза, означает, что бэкенд жив. */
        fun httpProbe(http: HttpClient, settings: AppSettings): suspend () -> Boolean = {
            val response = http.get(settings.serverUrl.value.trimEnd('/') + "/auth/me") {
                timeout { requestTimeoutMillis = PROBE_TIMEOUT_MS }
            }
            response.status.value !in 502..504
        }
    }
}
