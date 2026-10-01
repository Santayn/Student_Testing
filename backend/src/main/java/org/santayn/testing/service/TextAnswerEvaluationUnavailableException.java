package org.santayn.testing.service;

public class TextAnswerEvaluationUnavailableException extends RuntimeException {

    public TextAnswerEvaluationUnavailableException(String message) {
        super(message);
    }

    public TextAnswerEvaluationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
