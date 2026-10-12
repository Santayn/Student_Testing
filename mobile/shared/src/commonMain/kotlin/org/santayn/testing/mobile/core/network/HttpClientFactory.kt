package org.santayn.testing.mobile.core.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.santayn.testing.mobile.core.platform.platformName
import io.ktor.client.plugins.logging.Logger as KtorLogger

/** Движок HTTP для платформы: OkHttp (Android) / Darwin (iOS). */
expect fun createHttpEngine(): HttpClientEngine

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
}

fun createHttpClient(json: Json = AppJson, engine: HttpClientEngine = createHttpEngine()): HttpClient =
    HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = ApiTimeouts.STANDARD_MS
            // Недоступный хост не должен держать экран дольше нескольких секунд: дальше — офлайн-данные.
            connectTimeoutMillis = 6_000
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = object : KtorLogger {
                override fun log(message: String) = Logger.withTag("HTTP").d { message }
            }
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
        defaultRequest {
            headers.append(HttpHeaders.Accept, "application/json")
            headers.append(HttpHeaders.UserAgent, "StudentTestingMobile/1.0 ($platformName)")
        }
    }

/** Преобразует транспортные ошибки (таймаут, нет сети) в [ApiException]. */
suspend fun <T> mapTransportErrors(block: suspend () -> T): T = try {
    block()
} catch (e: HttpRequestTimeoutException) {
    throw ApiException.timeout(e)
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    throw e
} catch (e: StaleSessionException) {
    throw e
} catch (e: ConnectTimeoutException) {
    throw ApiException.timeout(e)
} catch (e: SocketTimeoutException) {
    throw ApiException.timeout(e)
} catch (e: Exception) {
    throw ApiException.network(e)
}

/** Разбирает тело ошибки бэкенда. */
suspend fun HttpResponse.toApiException(json: Json = AppJson): ApiException {
    val text = runCatching { bodyAsText() }.getOrNull()
    val body = text?.takeIf { it.isNotBlank() }?.let {
        runCatching { json.decodeFromString(ErrorResponse.serializer(), it) }.getOrNull()
    }
    return ApiException.fromResponse(status.value, body)
}
