package org.santayn.testing.service;

public class AuthenticationRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public AuthenticationRateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
