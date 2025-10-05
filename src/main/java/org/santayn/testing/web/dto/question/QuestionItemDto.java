// src/main/java/org/santayn/testing/web/dto/question/QuestionItemDto.java
package org.santayn.testing.web.dto.question;

import org.santayn.testing.models.question.Question;

public record QuestionItemDto(Integer id, String text, String correctAnswer) {
    public static QuestionItemDto from(Question q) {
        return new QuestionItemDto(q.getId(), q.getText(), q.getCorrectAnswer());
    }
}
