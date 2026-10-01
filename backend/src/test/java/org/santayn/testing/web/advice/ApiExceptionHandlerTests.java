package org.santayn.testing.web.advice;

import org.junit.jupiter.api.Test;
import org.santayn.testing.service.ActiveStudentGroupConflictException;
import org.santayn.testing.service.AuthenticationRateLimitException;
import org.santayn.testing.service.ResourceInUseException;
import org.santayn.testing.web.dto.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();


    @Test
    void authenticationRateLimitReturns429AndRetryAfter() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthenticationRateLimit(
                new AuthenticationRateLimitException("Too many failed login attempts.", 42)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("42");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(429);
        assertThat(response.getBody().code()).isEqualTo("AUTH_RATE_LIMITED");
        assertThat(response.getBody().requestId()).isNotBlank();
    }

    @Test
    void activeStudentGroupConflictContainsCurrentGroupContext() {
        ResponseEntity<ErrorResponse> response = handler.handleActiveStudentGroupConflict(
                new ActiveStudentGroupConflictException(100, 9)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("STUDENT_GROUP_CONFLICT");
        assertThat(response.getBody().requestId()).isNotBlank();
        assertThat(response.getBody().details()).singleElement().satisfies(detail -> {
            assertThat(detail.get("currentGroupId")).isEqualTo(100);
            assertThat(detail.get("currentMembershipId")).isEqualTo(9);
        });
    }
    @Test
    void resourceInUseReturns409WithDependencyContext() {
        ResponseEntity<ErrorResponse> response = handler.handleResourceInUse(
                new ResourceInUseException(
                        "test",
                        7,
                        "Test cannot be deleted while attempts exist.",
                        java.util.Map.of("testAttempts", 3L)
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_IN_USE");
        assertThat(response.getBody().details()).singleElement().satisfies(detail -> {
            assertThat(detail.get("resourceType")).isEqualTo("test");
            assertThat(detail.get("resourceId")).isEqualTo(7);
            assertThat(detail.get("testAttempts")).isEqualTo(3L);
        });
    }

}
