package org.santayn.testing.service.ai;

/** ИИ не настроен, лимит исчерпан или провайдер не ответил. Сообщение показывается пользователю. */
public class AiUnavailableException extends RuntimeException {

    /** false — повтор не поможет (неверный ключ, слишком большой запрос): задача сразу завершается ошибкой. */
    private final boolean retryable;

    public AiUnavailableException(String message) {
        this(message, true);
    }

    public AiUnavailableException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.retryable = true;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
