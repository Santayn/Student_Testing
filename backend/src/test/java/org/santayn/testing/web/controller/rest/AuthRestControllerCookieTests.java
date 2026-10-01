package org.santayn.testing.web.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthRestControllerCookieTests {

    private UserRegisterService userRegisterService;
    private AuthRestController controller;

    @BeforeEach
    void setUp() {
        userRegisterService = mock(UserRegisterService.class);
        controller = new AuthRestController(
                userRegisterService,
                "student_test_refresh",
                "student_test_csrf",
                false,
                "Lax"
        );
    }

    @Test
    void loginWritesRefreshTokenOnlyToHttpOnlyCookieAndHidesItFromJson() throws Exception {
        UserRegisterService.AuthTokens tokens = tokens("secret-refresh");
        when(userRegisterService.login(eq("student"), eq("password"), eq(1), anyString(), anyString()))
                .thenReturn(tokens);

        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();

        UserRegisterService.AuthTokens body = controller.login(
                new AuthRestController.LoginRequest("student", "password", 1),
                request,
                response
        );

        assertThat(body.accessToken()).isEqualTo("access-token");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> {
                    assertThat(cookie).contains("student_test_refresh=secret-refresh");
                    assertThat(cookie).contains("Path=/api/v1/auth");
                    assertThat(cookie).contains("HttpOnly");
                    assertThat(cookie).contains("SameSite=Lax");
                });

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(body);
        assertThat(json).contains("\"accessToken\":\"access-token\"");
        assertThat(json).doesNotContain("refreshToken");
        assertThat(json).doesNotContain("secret-refresh");
    }

    @Test
    void refreshRequiresMatchingDoubleSubmitCsrfAndRotatesCookie() {
        UserRegisterService.AuthTokens tokens = tokens("new-refresh");
        when(userRegisterService.refresh(eq("old-refresh"), anyString(), anyString()))
                .thenReturn(tokens);

        MockHttpServletRequest request = request();
        request.setCookies(
                new Cookie("student_test_refresh", "old-refresh"),
                new Cookie("student_test_csrf", "csrf-value")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refresh("csrf-value", request, response);

        verify(userRegisterService).refresh(
                eq("old-refresh"),
                eq("127.0.0.1"),
                eq("JUnit")
        );
        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("student_test_refresh=new-refresh")
                        .contains("HttpOnly"));
    }

    @Test
    void refreshRejectsMissingOrMismatchedCsrf() {
        MockHttpServletRequest request = request();
        request.setCookies(
                new Cookie("student_test_refresh", "old-refresh"),
                new Cookie("student_test_csrf", "cookie-token")
        );

        assertThatThrownBy(() -> controller.refresh(
                "wrong-token",
                request,
                new MockHttpServletResponse()
        )).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokeUsesCookieRefreshTokenAndClearsSessionCookies() {
        MockHttpServletRequest request = request();
        request.setCookies(
                new Cookie("student_test_refresh", "refresh-to-revoke"),
                new Cookie("student_test_csrf", "csrf-value")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.revoke("csrf-value", request, response);

        verify(userRegisterService).revoke(
                "refresh-to-revoke",
                "127.0.0.1"
        );
        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("student_test_refresh=")
                        .contains("Max-Age=0"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .contains("student_test_csrf=")
                        .contains("Max-Age=0"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }

    private UserRegisterService.AuthTokens tokens(String refreshToken) {
        Instant now = Instant.now();
        return new UserRegisterService.AuthTokens(
                "Bearer",
                "access-token",
                now.plusSeconds(900),
                refreshToken,
                now.plusSeconds(3600),
                1,
                null
        );
    }
}
