package org.santayn.testing.mobile.core.network

import kotlinx.serialization.Serializable

/** Тело ошибки бэкенда: ErrorResponse.java. */
@Serializable
data class ErrorResponse(
    val code: String? = null,
    val message: String? = null,
    val details: List<ErrorDetail> = emptyList(),
    val traceId: String? = null,
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val issue: String? = null,
)

/**
 * Нормализованная ошибка API (аналог normalizeApiError из frontend/src/api/error.js).
 *
 * @property status HTTP-статус или null для сетевых ошибок/таймаутов.
 */
class ApiException(
    val status: Int?,
    val code: String?,
    override val message: String,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val isNetworkError: Boolean = false,
    val isTimeout: Boolean = false,
    /** Сеть отключена на устройстве (а не недоступен сервер). */
    val isOffline: Boolean = false,
    val traceId: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    val isUnauthorized get() = status == 401
    val isForbidden get() = status == 403
    val isNotFound get() = status == 404
    val isConflict get() = status == 409

    /** Шлюз nginx не достучался до бэкенда. */
    val isGatewayError get() = status in 502..504

    /** Проблема связи: запрос не дошёл до бэкенда или ответ не получен. */
    val isConnectivityProblem get() = isNetworkError || isTimeout || isGatewayError

    fun fieldError(field: String): String? = fieldErrors[field]?.firstOrNull()

    companion object {
        const val MSG_DEFAULT = "Не удалось выполнить запрос"
        const val MSG_TIMEOUT = "Сервер слишком долго отвечает"
        const val MSG_NETWORK = "Сервер недоступен. Проверьте адрес сервера или попробуйте позже."
        const val MSG_OFFLINE = "Нет подключения к интернету"

        fun timeout(cause: Throwable? = null) = ApiException(
            status = null, code = "timeout", message = MSG_TIMEOUT, isTimeout = true, cause = cause,
        )

        fun network(cause: Throwable? = null) = ApiException(
            status = null, code = "network", message = MSG_NETWORK, isNetworkError = true, cause = cause,
        )

        /** Сетевая ошибка при отключённой сети устройства. */
        fun offline(cause: Throwable? = null) = ApiException(
            status = null, code = "offline", message = MSG_OFFLINE,
            isNetworkError = true, isOffline = true, cause = cause,
        )

        fun fromResponse(status: Int, body: ErrorResponse?): ApiException {
            val fieldErrors = body?.details.orEmpty()
                .filter { !it.field.isNullOrBlank() }
                .groupBy({ it.field!! }, { it.issue ?: "" })
            val details = body?.details.orEmpty()
                .mapNotNull { d -> listOfNotNull(d.field, d.issue).joinToString(": ").ifBlank { null } }
                .joinToString("; ")
            val base = body?.message?.takeIf { it.isNotBlank() } ?: defaultMessageFor(status)
            return ApiException(
                status = status,
                code = body?.code,
                message = if (details.isNotBlank() && body?.code == "validation_failed") "$base: $details" else base,
                fieldErrors = fieldErrors,
                traceId = body?.traceId,
            )
        }

        fun defaultMessageFor(status: Int): String = when (status) {
            400 -> "Проверьте введённые данные"
            401 -> "Сессия завершена. Войдите в систему снова."
            403 -> "Недостаточно прав для выполнения операции."
            404 -> "Запрошенные данные не найдены"
            409 -> "Конфликт данных: обновите экран и повторите попытку"
            413 -> "Файл слишком большой"
            in 502..504 -> MSG_NETWORK
            in 500..599 -> "Сервис временно недоступен. Попробуйте позже."
            else -> MSG_DEFAULT
        }
    }
}

/** Запрос относится к завершённой сессии — результат нужно молча отбросить. */
class StaleSessionException : Exception("Запрос относится к завершённой сессии")

/** Человекочитаемое сообщение для любой ошибки. */
fun Throwable.userMessage(fallback: String = ApiException.MSG_DEFAULT): String = when (this) {
    is ApiException -> message
    is StaleSessionException -> fallback
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}
