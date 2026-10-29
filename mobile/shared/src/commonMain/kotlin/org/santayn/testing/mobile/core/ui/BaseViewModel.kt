package org.santayn.testing.mobile.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.santayn.testing.mobile.core.network.StaleSessionException
import org.santayn.testing.mobile.core.network.userMessage

/**
 * База ViewModel: одноразовые сообщения (toast/snackbar) и безопасный запуск корутин.
 */
abstract class BaseViewModel : ViewModel() {

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** Одноразовые сообщения для снэкбара. */
    val messages = _messages.receiveAsFlow()

    protected fun toast(message: String) {
        _messages.trySend(message)
    }

    /**
     * Запуск с обработкой ошибок: [StaleSessionException] молча игнорируется,
     * остальные ошибки передаются в [onError] (по умолчанию — снэкбар).
     */
    protected fun launchSafe(
        onError: (Throwable) -> Unit = { toast(it.userMessage()) },
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: StaleSessionException) {
            // Ответ относится к завершённой сессии — отбрасываем.
        } catch (e: Throwable) {
            onError(e)
        }
    }
}
