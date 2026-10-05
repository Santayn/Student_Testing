package org.santayn.testing.service;

public interface TextAnswerEvaluationService {

    TextAnswerEvaluationResult evaluate(String questionText, String expectedRaw, String actualRaw);

    default boolean isCorrect(String questionText, String expectedRaw, String actualRaw) {
        return evaluate(questionText, expectedRaw, actualRaw).correct();
    }
}
