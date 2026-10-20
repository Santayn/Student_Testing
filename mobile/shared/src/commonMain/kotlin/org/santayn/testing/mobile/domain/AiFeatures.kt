package org.santayn.testing.mobile.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.santayn.testing.mobile.data.api.AiApi
import org.santayn.testing.mobile.data.model.AiJobDto
import org.santayn.testing.mobile.data.model.AiStatusDto

/**
 * Что из ИИ включено на сервере (общая настройка) и ожидание фоновых задач ИИ.
 * Статус грузится один раз: кнопки ИИ показываются только для доступных функций.
 */
class AiFeatures(private val api: AiApi, private val scope: CoroutineScope) {
    private val _status = MutableStateFlow<AiStatusDto?>(null)
    val status: StateFlow<AiStatusDto?> = _status.asStateFlow()
    private var loading: Job? = null

    fun ensureLoaded() {
        if (_status.value != null || loading?.isActive == true) return
        loading = scope.launch { _status.value = runCatching { api.getStatus() }.getOrNull() }
    }

    /** Опрашивает задачу, пока она в очереди или выполняется. */
    suspend fun awaitJob(job: AiJobDto, onProgress: (String?) -> Unit = {}): AiJobDto {
        var current = job
        var failures = 0
        while (current.active) {
            delay(POLL_INTERVAL_MS)
            current = try {
                api.getJob(current.id).also { failures = 0 }
            } catch (e: Exception) {
                if (++failures >= MAX_POLL_FAILURES) throw e
                current
            }
            onProgress(current.progress)
        }
        return current
    }

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
        const val MAX_POLL_FAILURES = 5
    }
}
