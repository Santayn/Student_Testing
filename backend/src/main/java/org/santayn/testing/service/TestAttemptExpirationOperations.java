package org.santayn.testing.service;

import java.time.Instant;

public interface TestAttemptExpirationOperations {

    void expireAttempt(Integer attemptId, Instant expiredAtUtc);
}
