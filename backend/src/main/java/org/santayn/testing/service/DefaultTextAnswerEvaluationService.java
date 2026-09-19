package org.santayn.testing.service;

import org.springframework.stereotype.Service;

@Service
public class DefaultTextAnswerEvaluationService implements TextAnswerEvaluationService {

    @Override
    public TextAnswerEvaluationResult evaluate(String questionText, String expectedRaw, String actualRaw) {
        if (TextAnswerEvaluator.isCorrect(expectedRaw, actualRaw)) {
            return TextAnswerEvaluationResult.CORRECT;
        }
        if (TextAnswerEvaluator.isPartiallyCorrect(expectedRaw, actualRaw)) {
            return TextAnswerEvaluationResult.PARTIALLY_CORRECT;
        }
        return TextAnswerEvaluationResult.INCORRECT;
    }
}
