package org.santayn.testing.mobile.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.decodeURLPart
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.delay
import kotlinx.io.RawSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.ResponseCache

/** Файл для multipart-загрузки. */
class UploadFile(
    val name: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
)

/** Скачанный файл. */
class DownloadedFile(
    val fileName: String,
    val contentType: String?,
    val bytes: ByteArray,
)

/**
 * HTTP-клиент API — порт frontend/src/api/http.js:
 * Bearer-токен, проактивный refresh, один повтор после 401, отбрасывание ответов старой сессии.
 *
 * Сверх веба:
 * - отмечает в [network], доступен ли сервер, и отличает «нет сети на устройстве» от «сервер недоступен»;
 * - GET-запросы повторяет при кратковременных сбоях связи;
 * - успешные GET-ответы сохраняет в [cache] и отдаёт их, когда связи нет.
 */
class ApiClient(
    private val http: HttpClient,
    private val session: SessionManager,
    private val settings: AppSettings,
    @PublishedApi internal val json: Json,
    private val network: NetworkStatus? = null,
    private val cache: ResponseCache? = null,
) {
    private fun url(path: String): String = settings.serverUrl.value.trimEnd('/') + path

    suspend fun execute(
        method: HttpMethod,
        path: String,
        timeoutMs: Long = ApiTimeouts.STANDARD_MS,
        authenticated: Boolean = true,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = try {
        executeAuthenticated(method, path, timeoutMs, authenticated, configure).also { response ->
            if (response.status.value in 502..504) network?.reportServerUnreachable() else network?.reportServerReachable()
        }
    } catch (e: ApiException) {
        throw classifyConnectivity(e)
    }

    /** Без сети на устройстве — «Нет подключения к интернету», иначе сервер помечается недоступным. */
    private fun classifyConnectivity(e: ApiException): ApiException {
        if (!e.isConnectivityProblem || e.isOffline) return e
        if (network?.isDeviceOffline == true) return ApiException.offline(e)
        network?.reportServerUnreachable()
        return e
    }

    private suspend fun executeAuthenticated(
        method: HttpMethod,
        path: String,
        timeoutMs: Long,
        authenticated: Boolean,
        configure: HttpRequestBuilder.() -> Unit,
    ): HttpResponse {
        val epochAtStart = session.epoch
        val unlimited = timeoutMs == ApiTimeouts.FILE_TRANSFER_MS

        suspend fun send(token: String?): HttpResponse = mapTransportErrors {
            http.request(url(path)) {
                this.method = method
                timeout {
                    requestTimeoutMillis = if (unlimited) HttpTimeoutConfig.INFINITE_TIMEOUT_MS else timeoutMs
                    if (unlimited) socketTimeoutMillis = 60_000
                }
                if (token != null) bearerAuth(token)
                configure()
            }
        }

        val token = if (authenticated) {
            session.validAccessToken() ?: throw ApiException.fromResponse(401, null)
        } else {
            null
        }

        var response = send(token)

        if (authenticated && response.status.value == 401 && token != null) {
            val renewed = session.recoverFromUnauthorized(token)
            if (renewed != null) {
                response = send(renewed)
            }
            if (response.status.value == 401) {
                session.invalidate()
            }
        }

        if (authenticated && session.epoch != epochAtStart) {
            throw StaleSessionException()
        }

        if (!response.status.isSuccess()) {
            throw response.toApiException(json)
        }
        return response
    }

    suspend inline fun <reified T> get(
        path: String,
        params: Map<String, Any?> = emptyMap(),
        timeoutMs: Long = ApiTimeouts.STANDARD_MS,
    ): T {
        val text = getText(path, params, timeoutMs)
        return if (T::class == Unit::class) Unit as T else json.decodeFromString<T>(text)
    }

    /**
     * GET с повторами и офлайн-кешем.
     *
     * - Сбой связи и есть сохранённый ответ этого пользователя на тот же URL — он возвращается сразу.
     * - Сохранённого ответа нет: при сетевой ошибке или 502–504 до двух повторов с паузой
     *   (таймаут не повторяется — он и так долгий).
     */
    @PublishedApi
    internal suspend fun getText(path: String, params: Map<String, Any?>, timeoutMs: Long): String {
        val key = cacheKey(path, params)
        // Связи уже нет (сеть выключена или сервер не ответил) — сохранённые данные отдаём сразу,
        // не дожидаясь очередного отказа соединения. Восстановление связи отслеживает NetworkStatus.
        val outageKnown = network != null && network.state.value != NetworkState.ONLINE
        if (key != null && outageKnown) {
            cache?.get(key)?.let { cached ->
                network?.reportCachedData(cached.savedAt)
                return cached.body
            }
        }
        var attempt = 0
        while (true) {
            try {
                val text = execute(HttpMethod.Get, path, timeoutMs) {
                    params.forEach { (name, value) -> if (value != null) parameter(name, value) }
                }.bodyAsText()
                if (key != null) cache?.put(key, text)
                return text
            } catch (e: ApiException) {
                // Есть сохранённые данные — показываем их сразу: экран обновится сам,
                // когда NetworkStatus увидит, что связь вернулась.
                if (key != null && e.isConnectivityProblem) {
                    val cached = cache?.get(key)
                    if (cached != null) {
                        network?.reportCachedData(cached.savedAt)
                        return cached.body
                    }
                }
                // Без кеша повторяем «свежий» сбой: если о недоступности уже известно, ждать нечего.
                val retryable = (e.isNetworkError || e.isGatewayError) && !e.isOffline && !outageKnown
                if (retryable && attempt < GET_RETRY_DELAYS_MS.size) {
                    delay(GET_RETRY_DELAYS_MS[attempt++])
                    continue
                }
                throw e
            }
        }
    }

    /** Ключ кеша: пользователь + сервер + путь + отсортированные параметры. Без сессии не кешируем. */
    private fun cacheKey(path: String, params: Map<String, Any?>): String? {
        val userId = session.currentUser?.userId ?: return null
        val query = params.filterValues { it != null }.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        return "$userId|${settings.serverUrl.value.trimEnd('/')}$path?$query"
    }

    suspend inline fun <reified T, reified B : Any> post(
        path: String,
        body: B,
        timeoutMs: Long = ApiTimeouts.STANDARD_MS,
    ): T = execute(HttpMethod.Post, path, timeoutMs) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.bodyOrUnit()

    suspend inline fun <reified T> postEmpty(
        path: String,
        timeoutMs: Long = ApiTimeouts.STANDARD_MS,
    ): T = execute(HttpMethod.Post, path, timeoutMs).bodyOrUnit()

    suspend inline fun <reified T, reified B : Any> put(
        path: String,
        body: B,
    ): T = execute(HttpMethod.Put, path) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }.bodyOrUnit()

    suspend inline fun <reified T> putEmpty(path: String): T =
        execute(HttpMethod.Put, path).bodyOrUnit()

    suspend fun delete(path: String) {
        execute(HttpMethod.Delete, path)
    }

    /** multipart/form-data: файлы в поле [fileField] + простые поля. */
    suspend inline fun <reified T> postMultipart(
        path: String,
        fileField: String,
        files: List<UploadFile>,
        fields: Map<String, Any?> = emptyMap(),
    ): T = execute(HttpMethod.Post, path, ApiTimeouts.FILE_TRANSFER_MS) {
        setBody(multipartBody(fileField, files, fields))
    }.bodyOrUnit()

    /** Скачивание файла с авторизацией (blob-загрузка во фронте). */
    suspend fun download(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        fallbackName: String = "file",
    ): DownloadedFile {
        val response = execute(method, path, ApiTimeouts.FILE_TRANSFER_MS) {
            headers.remove(HttpHeaders.Accept)
            headers.append(HttpHeaders.Accept, "*/*")
        }
        val disposition = response.headers[HttpHeaders.ContentDisposition]
        return DownloadedFile(
            fileName = parseContentDispositionFileName(disposition) ?: fallbackName,
            contentType = response.headers[HttpHeaders.ContentType],
            bytes = response.body(),
        )
    }

    /**
     * Потоковое скачивание файла: тело ответа пишется в [open] кусками, без загрузки в память
     * (материалы лекций — до 50 МБ, в том числе видео).
     *
     * @param open открывает приёмник по имени файла из Content-Disposition.
     * @param onProgress доля скачанного 0..1 или null, если размер неизвестен.
     * @return имя файла.
     */
    suspend fun downloadTo(
        path: String,
        fallbackName: String,
        open: suspend (fileName: String) -> RawSink,
        onProgress: (Float?) -> Unit = {},
    ): String = try {
        downloadStreaming(path, fallbackName, open, onProgress)
    } catch (e: ApiException) {
        throw classifyConnectivity(e)
    }

    private suspend fun downloadStreaming(
        path: String,
        fallbackName: String,
        open: suspend (fileName: String) -> RawSink,
        onProgress: (Float?) -> Unit,
    ): String {
        val epochAtStart = session.epoch
        var token = session.validAccessToken() ?: throw ApiException.fromResponse(401, null)
        repeat(2) { attempt ->
            val result: String? = mapTransportErrors {
                http.prepareRequest(url(path)) {
                    method = HttpMethod.Get
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = 60_000
                    }
                    bearerAuth(token)
                    headers.remove(HttpHeaders.Accept)
                    headers.append(HttpHeaders.Accept, "*/*")
                }.execute { response ->
                    if (response.status.value == 401 && attempt == 0) return@execute null
                    if (response.status.value in 502..504) network?.reportServerUnreachable() else network?.reportServerReachable()
                    if (session.epoch != epochAtStart) throw StaleSessionException()
                    if (!response.status.isSuccess()) {
                        if (response.status.value == 401) session.invalidate()
                        throw response.toApiException(json)
                    }
                    val fileName = parseContentDispositionFileName(response.headers[HttpHeaders.ContentDisposition]) ?: fallbackName
                    val total = response.contentLength()?.takeIf { it > 0 }
                    val channel = response.bodyAsChannel()
                    open(fileName).buffered().use { sink ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        onProgress(if (total != null) 0f else null)
                        while (true) {
                            val n = channel.readAvailable(buffer, 0, buffer.size)
                            if (n < 0) break
                            if (n == 0) continue
                            sink.write(buffer, 0, n)
                            read += n
                            if (total != null) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                    fileName
                }
            }
            if (result != null) return result
            // 401 на первой попытке: токен обновил другой запрос или он истёк — повторяем один раз.
            token = session.recoverFromUnauthorized(token) ?: run {
                session.invalidate()
                throw ApiException.fromResponse(401, null)
            }
        }
        throw ApiException.fromResponse(401, null)
    }

    private companion object {
        val GET_RETRY_DELAYS_MS = longArrayOf(400, 1_200)
    }
}

fun multipartBody(
    fileField: String,
    files: List<UploadFile>,
    fields: Map<String, Any?>,
): MultiPartFormDataContent = MultiPartFormDataContent(
    formData {
        fields.forEach { (key, value) -> if (value != null) append(key, value.toString()) }
        files.forEach { file ->
            append(
                fileField,
                file.bytes,
                Headers.build {
                    append(HttpHeaders.ContentType, file.contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name.replace("\"", "")}\"")
                },
            )
        }
    }
)

suspend inline fun <reified T> HttpResponse.bodyOrUnit(): T =
    if (T::class == Unit::class) Unit as T else body()

/**
 * Имя файла из Content-Disposition: сначала `filename*=UTF-8''...`, затем `filename="..."`.
 */
fun parseContentDispositionFileName(header: String?): String? {
    if (header.isNullOrBlank()) return null
    val extended = Regex("""filename\*\s*=\s*([^;]+)""", RegexOption.IGNORE_CASE).find(header)
    if (extended != null) {
        val raw = extended.groupValues[1].trim().trim('"')
        val encoded = raw.substringAfter("''", raw)
        val decoded = runCatching { encoded.decodeURLPart() }.getOrNull()
        if (!decoded.isNullOrBlank()) return decoded
    }
    val plain = Regex("""filename\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE).find(header)
    return plain?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
}
