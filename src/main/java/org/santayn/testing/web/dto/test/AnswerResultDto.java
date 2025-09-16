// AnswerResultDto.java
package org.santayn.testing.web.dto.test;

public record AnswerResultDto(Integer questionId,
                              String questionText,
                              String correctAnswer,
                              String givenAnswer,
                              boolean correct) {}
