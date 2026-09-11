package org.santayn.testing.service;

public interface TextAnswerEvaluationService {

    boolean isCorrect(String questionText, String expectedRaw, String actualRaw);
}
