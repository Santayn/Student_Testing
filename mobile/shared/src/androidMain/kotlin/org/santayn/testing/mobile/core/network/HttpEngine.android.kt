package org.santayn.testing.mobile.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool

/**
 * Общий пул соединений OkHttp. При смене сети (выход из авиарежима, переключение Wi-Fi ↔ мобильная,
 * переподключение VPN) старые keep-alive соединения «зависают» до таймаута — пул сбрасывается
 * из [AndroidConnectivityObserver].
 */
internal val sharedConnectionPool = ConnectionPool()

actual fun createHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        connectionPool(sharedConnectionPool)
        retryOnConnectionFailure(true)
    }
}
