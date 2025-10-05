// src/main/java/org/santayn/testing/web/dto/question/QuestionDto.java
package org.santayn.testing.web.dto.question;

import org.santayn.testing.models.question.Question;

public record QuestionDto(Integer id, String text, String correctAnswer) {
    public static QuestionDto from(Question q){
        return new QuestionDto(q.getId(), q.getText(), q.getCorrectAnswer());
    }
}
