package org.santayn.testing.service;

import java.math.BigDecimal;

public record TextAnswerEvaluationResult(boolean correct, BigDecimal scoreRatio) {

    public static final TextAnswerEvaluationResult CORRECT = new TextAnswerEvaluationResult(true, BigDecimal.ONE);
    public static final TextAnswerEvaluationResult PARTIALLY_CORRECT = new TextAnswerEvaluationResult(false, new BigDecimal("0.5"));
    public static final TextAnswerEvaluationResult INCORRECT = new TextAnswerEvaluationResult(false, BigDecimal.ZERO);

    public TextAnswerEvaluationResult {
        if (scoreRatio == null) {
            scoreRatio = BigDecimal.ZERO;
        }
        if (scoreRatio.compareTo(BigDecimal.ZERO) < 0) {
            scoreRatio = BigDecimal.ZERO;
        } else if (scoreRatio.compareTo(BigDecimal.ONE) > 0) {
            scoreRatio = BigDecimal.ONE;
        }
    }
}
