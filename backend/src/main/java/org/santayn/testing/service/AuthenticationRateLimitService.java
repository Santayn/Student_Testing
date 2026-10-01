package org.santayn.testing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthenticationRateLimitService {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final ConcurrentMap<String, FailureState> loginFailures = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FailureState> refreshFailures = new ConcurrentHashMap<>();
    private final int loginMaxFailures;
    private final Duration loginWindow;
    private final Duration loginBlock;
    private final int refreshMaxFailures;
    private final Duration refreshWindow;
    private final Duration refreshBlock;

    public AuthenticationRateLimitService(
            @Value("${app.security.auth-rate-limit.login.max-failures:5}") int loginMaxFailures,
            @Value("${app.security.auth-rate-limit.login.window-seconds:300}") long loginWindowSeconds,
            @Value("${app.security.auth-rate-limit.login.block-seconds:60}") long loginBlockSeconds,
            @Value("${app.security.auth-rate-limit.refresh.max-failures:10}") int refreshMaxFailures,
            @Value("${app.security.auth-rate-limit.refresh.window-seconds:300}") long refreshWindowSeconds,
            @Value("${app.security.auth-rate-limit.refresh.block-seconds:60}") long refreshBlockSeconds) {
        this.loginMaxFailures = Math.max(1, loginMaxFailures);
        this.loginWindow = Duration.ofSeconds(Math.max(1, loginWindowSeconds));
        this.loginBlock = Duration.ofSeconds(Math.max(1, loginBlockSeconds));
        this.refreshMaxFailures = Math.max(1, refreshMaxFailures);
        this.refreshWindow = Duration.ofSeconds(Math.max(1, refreshWindowSeconds));
        this.refreshBlock = Duration.ofSeconds(Math.max(1, refreshBlockSeconds));
    }

    public void checkLoginAllowed(String normalizedLogin, String ipAddress) {
        checkAllowed(loginFailures, loginKey(normalizedLogin, ipAddress), "Too many failed login attempts.");
    }

    public void recordLoginFailure(String normalizedLogin, String ipAddress) {
        recordFailure(
                loginFailures,
                loginKey(normalizedLogin, ipAddress),
                loginMaxFailures,
                loginWindow,
                loginBlock
        );
    }

    public void recordLoginSuccess(String normalizedLogin, String ipAddress) {
        loginFailures.remove(loginKey(normalizedLogin, ipAddress));
    }

    public void checkRefreshAllowed(String ipAddress) {
        checkAllowed(refreshFailures, refreshKey(ipAddress), "Too many failed refresh attempts.");
    }

    public void recordRefreshFailure(String ipAddress) {
        recordFailure(
                refreshFailures,
                refreshKey(ipAddress),
                refreshMaxFailures,
                refreshWindow,
                refreshBlock
        );
    }

    public void recordRefreshSuccess(String ipAddress) {
        refreshFailures.remove(refreshKey(ipAddress));
    }

    private void checkAllowed(ConcurrentMap<String, FailureState> failures,
                              String key,
                              String message) {
        FailureState state = failures.get(key);
        if (state == null) {
            return;
        }
        Instant now = Instant.now();
        if (state.blockedUntil() != null && state.blockedUntil().isAfter(now)) {
            long retryAfterSeconds = Math.max(1, Duration.between(now, state.blockedUntil()).toSeconds() + 1);
            throw new AuthenticationRateLimitException(message, retryAfterSeconds);
        }
        if (state.blockedUntil() != null || state.windowStartedAt().plus(state.window()).isBefore(now)) {
            failures.remove(key, state);
        }
    }

    private void recordFailure(ConcurrentMap<String, FailureState> failures,
                               String key,
                               int maxFailures,
                               Duration window,
                               Duration block) {
        Instant now = Instant.now();
        ensureCapacity(failures, now);
        failures.compute(key, (ignored, previous) -> {
            FailureState current = previous;
            if (current != null && current.blockedUntil() != null && current.blockedUntil().isAfter(now)) {
                return current;
            }
            if (current == null
                    || current.blockedUntil() != null
                    || current.windowStartedAt().plus(window).isBefore(now)) {
                current = new FailureState(0, now, null, window);
            }
            int nextFailures = current.failures() + 1;
            Instant blockedUntil = nextFailures >= maxFailures ? now.plus(block) : null;
            return new FailureState(nextFailures, current.windowStartedAt(), blockedUntil, window);
        });
    }

    private void ensureCapacity(ConcurrentMap<String, FailureState> failures, Instant now) {
        if (failures.size() < MAX_TRACKED_KEYS) {
            return;
        }
        failures.entrySet().removeIf(entry -> expired(entry.getValue(), now));
        if (failures.size() < MAX_TRACKED_KEYS) {
            return;
        }
        failures.entrySet().stream()
                .min(java.util.Comparator.comparing(entry -> entry.getValue().windowStartedAt()))
                .ifPresent(entry -> failures.remove(entry.getKey(), entry.getValue()));
    }

    private static boolean expired(FailureState state, Instant now) {
        if (state.blockedUntil() != null) {
            return !state.blockedUntil().isAfter(now);
        }
        return !state.windowStartedAt().plus(state.window()).isAfter(now);
    }

    private static String loginKey(String normalizedLogin, String ipAddress) {
        String login = normalizedLogin == null ? "" : normalizedLogin.trim().toLowerCase(Locale.ROOT);
        return login + "|" + normalizeIp(ipAddress);
    }

    private static String refreshKey(String ipAddress) {
        return normalizeIp(ipAddress);
    }

    private static String normalizeIp(String ipAddress) {
        return ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress.trim();
    }

    private record FailureState(int failures,
                                Instant windowStartedAt,
                                Instant blockedUntil,
                                Duration window) {
    }
}
