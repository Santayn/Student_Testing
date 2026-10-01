package org.santayn.testing.service;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.user.RefreshToken;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RefreshTokenRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.security.DotNetPasswordHasher;
import org.santayn.testing.security.JwtService;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceRefreshTests {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;

    private UserRegisterService userRegisterService;

    @BeforeEach
    void setUp() {
        userRegisterService = new UserRegisterService(
                userRepository,
                refreshTokenRepository,
                new DotNetPasswordHasher(),
                jwtService,
                authenticationRateLimitService,
                false
        );
    }

    @Test
    void refreshRotatesTokenUsingLockedLookup() {
        RefreshToken storedToken = activeRefreshToken();
        User user = activeUser();
        Instant accessExpiresAt = Instant.now().plusSeconds(900);
        Instant refreshExpiresAt = Instant.now().plusSeconds(86400);
        JwtService.GeneratedTokens generatedTokens = new JwtService.GeneratedTokens(
                "new-access-token",
                accessExpiresAt,
                "new-refresh-token",
                "NEW_TOKEN_HASH",
                refreshExpiresAt,
                2
        );

        when(jwtService.computeRefreshTokenHash("old-refresh-token")).thenReturn("OLD_TOKEN_HASH");
        when(refreshTokenRepository.findByTokenHashForUpdate("OLD_TOKEN_HASH")).thenReturn(Optional.of(storedToken));
        when(userRepository.findWithSecurityById(7)).thenReturn(Optional.of(user));
        when(jwtService.generateTokens(user, 2)).thenReturn(generatedTokens);

        UserRegisterService.AuthTokens authTokens =
                userRegisterService.refresh("old-refresh-token", "192.0.2.44", "refresh-test-agent");

        assertThat(authTokens.accessToken()).isEqualTo("new-access-token");
        assertThat(authTokens.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(authTokens.refreshTokenExpiresAtUtc()).isEqualTo(refreshExpiresAt);

        assertThat(storedToken.getRevokedAtUtc()).isNotNull();
        assertThat(storedToken.getRevokedByIp()).isEqualTo("192.0.2.44");
        assertThat(storedToken.getReplacedByTokenHash()).isEqualTo("NEW_TOKEN_HASH");

        ArgumentCaptor<RefreshToken> replacementCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(replacementCaptor.capture());
        RefreshToken replacement = replacementCaptor.getValue();
        assertThat(replacement.getUserId()).isEqualTo(7);
        assertThat(replacement.getTokenHash()).isEqualTo("NEW_TOKEN_HASH");
        assertThat(replacement.getLifetimeKind()).isEqualTo(2);
        assertThat(replacement.getCreatedByIp()).isEqualTo("192.0.2.44");
        assertThat(replacement.getCreatedByUserAgent()).isEqualTo("refresh-test-agent");

        verify(refreshTokenRepository).findByTokenHashForUpdate("OLD_TOKEN_HASH");
        verify(refreshTokenRepository, never()).findByTokenHash("OLD_TOKEN_HASH");
    }

    @Test
    void revokeLocksAndRevokesTheCurrentRotationChain() {
        RefreshToken oldToken = activeRefreshToken();
        oldToken.setReplacedByTokenHash("NEW_TOKEN_HASH");

        RefreshToken replacement = activeRefreshToken();
        replacement.setId(12);
        replacement.setTokenHash("NEW_TOKEN_HASH");
        replacement.setCreatedAtUtc(Instant.now().minusSeconds(10));

        when(jwtService.computeRefreshTokenHash("old-refresh-token")).thenReturn("OLD_TOKEN_HASH");
        when(refreshTokenRepository.findByTokenHashForUpdate("OLD_TOKEN_HASH"))
                .thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.findByTokenHashForUpdate("NEW_TOKEN_HASH"))
                .thenReturn(Optional.of(replacement));

        userRegisterService.revoke("old-refresh-token", "192.0.2.44");

        assertThat(oldToken.getRevokedAtUtc()).isNotNull();
        assertThat(replacement.getRevokedAtUtc()).isNotNull();
        assertThat(oldToken.getRevokedByIp()).isEqualTo("192.0.2.44");
        assertThat(replacement.getRevokedByIp()).isEqualTo("192.0.2.44");
        verify(refreshTokenRepository).findByTokenHashForUpdate("OLD_TOKEN_HASH");
        verify(refreshTokenRepository).findByTokenHashForUpdate("NEW_TOKEN_HASH");
    }

    @Test
    void lockedRefreshLookupUsesPessimisticWrite() throws Exception {
        Lock lock = RefreshTokenRepository.class
                .getMethod("findByTokenHashForUpdate", String.class)
                .getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void revokedRefreshTokenDoesNotIssueReplacement() {
        RefreshToken storedToken = activeRefreshToken();
        storedToken.setRevokedAtUtc(Instant.now().minusSeconds(1));

        when(jwtService.computeRefreshTokenHash("old-refresh-token")).thenReturn("OLD_TOKEN_HASH");
        when(refreshTokenRepository.findByTokenHashForUpdate("OLD_TOKEN_HASH")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> userRegisterService.refresh("old-refresh-token", "192.0.2.44", "agent"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("already been revoked");

        verify(refreshTokenRepository, never()).save(org.mockito.ArgumentMatchers.any(RefreshToken.class));
    }

    private static RefreshToken activeRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setId(11);
        token.setUserId(7);
        token.setTokenHash("OLD_TOKEN_HASH");
        token.setLifetimeKind(2);
        token.setCreatedAtUtc(Instant.now().minusSeconds(60));
        token.setExpiresAtUtc(Instant.now().plusSeconds(3600));
        return token;
    }

    private static User activeUser() {
        User user = new User();
        user.setId(7);
        user.setLogin("refresh-user");
        user.setActive(true);
        return user;
    }
}
