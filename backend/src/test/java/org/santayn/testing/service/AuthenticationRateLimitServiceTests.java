package org.santayn.testing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationRateLimitServiceTests {

    @Test
    void loginIsBlockedAfterConfiguredNumberOfFailures() {
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                2, 300, 60,
                3, 300, 60
        );

        service.checkLoginAllowed("student", "192.0.2.10");
        service.recordLoginFailure("student", "192.0.2.10");
        service.checkLoginAllowed("student", "192.0.2.10");
        service.recordLoginFailure("student", "192.0.2.10");

        assertThatThrownBy(() -> service.checkLoginAllowed("student", "192.0.2.10"))
                .isInstanceOf(AuthenticationRateLimitException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((AuthenticationRateLimitException) exception).getRetryAfterSeconds()
                ).isPositive());
    }

    @Test
    void successfulLoginClearsFailureState() {
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                2, 300, 60,
                3, 300, 60
        );

        service.recordLoginFailure("student", "192.0.2.10");
        service.recordLoginSuccess("student", "192.0.2.10");
        service.recordLoginFailure("student", "192.0.2.10");

        service.checkLoginAllowed("student", "192.0.2.10");
    }

    @Test
    void refreshFailuresAreRateLimitedByClientAddress() {
        AuthenticationRateLimitService service = new AuthenticationRateLimitService(
                5, 300, 60,
                2, 300, 60
        );

        service.recordRefreshFailure("192.0.2.11");
        service.recordRefreshFailure("192.0.2.11");

        assertThatThrownBy(() -> service.checkRefreshAllowed("192.0.2.11"))
                .isInstanceOf(AuthenticationRateLimitException.class);
    }
}
