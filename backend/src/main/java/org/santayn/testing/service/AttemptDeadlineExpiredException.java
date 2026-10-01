package org.santayn.testing.service;

public class AttemptDeadlineExpiredException extends IllegalArgumentException {

    public AttemptDeadlineExpiredException(Integer attemptId) {
        super("Test attempt deadline has expired: " + attemptId);
    }
}
