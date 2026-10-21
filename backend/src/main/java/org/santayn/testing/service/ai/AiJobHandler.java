package org.santayn.testing.service.ai;

import org.santayn.testing.models.ai.AiJob;

/** Обработчик фоновой задачи одного типа. */
public interface AiJobHandler {

    String type();

    /**
     * Выполняет задачу. Возвращает результат, который сохраняется в AiJob.resultJson
     * (null — без результата). Долгие шаги сообщают прогресс через context.
     */
    Object handle(AiJob job, AiJobContext context) throws Exception;

    /** Вызывается один раз, когда попытки исчерпаны и задача окончательно не удалась. */
    default void onFinalFailure(AiJob job, String message) {
    }

    default int maxAttempts() {
        return 2;
    }
}
