package org.santayn.testing.web.controller.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final UserRegisterService userRegisterService;
    private final String refreshCookieName;
    private final String csrfCookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthRestController(
            UserRegisterService userRegisterService,
            @Value("${app.security.refresh-cookie.name:student_test_refresh}") String refreshCookieName,
            @Value("${app.security.refresh-cookie.csrf-cookie-name:student_test_csrf}") String csrfCookieName,
            @Value("${app.security.refresh-cookie.secure:true}") boolean cookieSecure,
            @Value("${app.security.refresh-cookie.same-site:Lax}") String cookieSameSite) {
        this.userRegisterService = userRegisterService;
        this.refreshCookieName = refreshCookieName;
        this.csrfCookieName = csrfCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/login")
    public UserRegisterService.AuthTokens login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        UserRegisterService.AuthTokens tokens = userRegisterService.login(
                request.login(),
                request.password(),
                request.lifetimeKind(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        writeRefreshCookie(httpResponse, tokens);
        noStore(httpResponse);
        return tokens;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterService.AuthTokens register(@Valid @RequestBody RegisterRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        UserRegisterService.AuthTokens tokens = userRegisterService.register(
                request.login(),
                request.password(),
                request.lifetimeKind(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        writeRefreshCookie(httpResponse, tokens);
        noStore(httpResponse);
        return tokens;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie(token).toString());
        noStore(response);
        return new CsrfResponse(token);
    }

    @PostMapping("/refresh")
    public UserRegisterService.AuthTokens refresh(
            @RequestHeader(name = CSRF_HEADER, required = false) String csrfToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        requireCsrf(httpRequest, csrfToken);
        UserRegisterService.AuthTokens tokens = userRegisterService.refresh(
                requireRefreshToken(httpRequest),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        writeRefreshCookie(httpResponse, tokens);
        noStore(httpResponse);
        return tokens;
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@RequestHeader(name = CSRF_HEADER, required = false) String csrfToken,
                       HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) {
        requireCsrf(httpRequest, csrfToken);
        try {
            userRegisterService.revoke(
                    requireRefreshToken(httpRequest),
                    httpRequest.getRemoteAddr()
            );
        } finally {
            clearRefreshCookie(httpResponse);
            clearCsrfCookie(httpResponse);
        }
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication,
                               HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        userRegisterService.changePassword(
                requireLogin(authentication),
                request.currentPassword(),
                request.newPassword(),
                httpRequest.getRemoteAddr()
        );
        clearRefreshCookie(httpResponse);
        clearCsrfCookie(httpResponse);
    }

    @GetMapping("/config")
    public UserRegisterService.PublicAuthConfiguration config() {
        return userRegisterService.publicAuthConfiguration();
    }

    @GetMapping("/me")
    public UserRegisterService.CurrentUser me(Authentication authentication) {
        return userRegisterService.currentUser(requireLogin(authentication));
    }

    private void noStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
    }

    private void writeRefreshCookie(HttpServletResponse response, UserRegisterService.AuthTokens tokens) {
        long maxAgeSeconds = Math.max(
                0,
                Duration.between(Instant.now(), tokens.refreshTokenExpiresAtUtc()).getSeconds()
        );
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, tokens.refreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie csrfCookie(String token) {
        return ResponseCookie.from(csrfCookieName, token)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .build();
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(refreshCookieName, "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .sameSite(cookieSameSite)
                        .path("/api/v1/auth")
                        .maxAge(Duration.ZERO)
                        .build()
                        .toString()
        );
    }

    private void clearCsrfCookie(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(csrfCookieName, "")
                        .httpOnly(false)
                        .secure(cookieSecure)
                        .sameSite(cookieSameSite)
                        .path("/")
                        .maxAge(Duration.ZERO)
                        .build()
                        .toString()
        );
    }

    private String requireRefreshToken(HttpServletRequest request) {
        String token = cookieValue(request, refreshCookieName);
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh session cookie is missing.");
        }
        return token;
    }

    private void requireCsrf(HttpServletRequest request, String headerValue) {
        String cookieValue = cookieValue(request, csrfCookieName);
        if (headerValue == null || cookieValue == null || !constantTimeEquals(headerValue, cookieValue)) {
            throw new AccessDeniedException("Invalid CSRF token.");
        }
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String requireLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication is required.");
        }
        return authentication.getName();
    }

    public record LoginRequest(
            @NotBlank @Size(max = 100) String login,
            @NotBlank @Size(min = 6, max = 200) String password,
            Integer lifetimeKind
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String login,
            @NotBlank @Size(min = 6, max = 200) String password,
            Integer lifetimeKind
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(min = 6, max = 200) String currentPassword,
            @NotBlank @Size(min = 6, max = 200) String newPassword
    ) {
    }

    public record CsrfResponse(String csrfToken) {
    }
}
