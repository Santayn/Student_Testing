package org.santayn.testing.service;

import org.springframework.stereotype.Service;

@Service
public class DefaultTextAnswerEvaluationService implements TextAnswerEvaluationService {

    @Override
    public boolean isCorrect(String questionText, String expectedRaw, String actualRaw) {
        return TextAnswerEvaluator.isCorrect(expectedRaw, actualRaw);
    }
}
