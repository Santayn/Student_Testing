// src/main/java/org/santayn/testing/web/dto/question/QuestionListItemDto.java
package org.santayn.testing.web.dto.question;

import org.santayn.testing.models.question.Question;

/**
 * Короткая карточка вопроса для списков по теме.
 * Содержит id, текст и правильный ответ (одна буква).
 */
public record QuestionListItemDto(
        Integer id,
        String text,
        String correctAnswer
) {
    public static QuestionListItemDto from(Question q) {
        return new QuestionListItemDto(
                q.getId(),
                q.getText(),
                q.getCorrectAnswer()
        );
    }
}
