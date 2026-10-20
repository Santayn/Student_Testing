package org.santayn.testing.service.ai;

/** ИИ не настроен, лимит исчерпан или провайдер не ответил. Сообщение показывается пользователю. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
