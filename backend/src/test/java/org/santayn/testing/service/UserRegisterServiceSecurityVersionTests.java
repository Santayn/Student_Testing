package org.santayn.testing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RefreshTokenRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.security.DotNetPasswordHasher;
import org.santayn.testing.security.JwtService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceSecurityVersionTests {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DotNetPasswordHasher passwordHasher;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;

    private UserRegisterService service;

    @BeforeEach
    void setUp() {
        service = new UserRegisterService(
                userRepository,
                refreshTokenRepository,
                passwordHasher,
                jwtService,
                authenticationRateLimitService,
                false
        );
    }

    @Test
    void changingPasswordBumpsSecurityVersionAndRevokesRefreshTokens() {
        User user = new User();
        user.setId(25);
        user.setLogin("student");
        user.setPasswordHash("old-hash");
        user.setActive(true);
        user.setSecurityVersion(7);

        when(userRepository.findWithSecurityByLogin("student")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("old-hash", "old-password"))
                .thenReturn(new DotNetPasswordHasher.VerificationResult(true, false));
        when(passwordHasher.hashPassword("new-password")).thenReturn("new-hash");
        when(refreshTokenRepository.findByUserIdAndRevokedAtUtcIsNullAndExpiresAtUtcAfter(
                org.mockito.ArgumentMatchers.eq(25),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of());

        service.changePassword("student", "old-password", "new-password", "127.0.0.1");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getSecurityVersion()).isEqualTo(8);
    }
}
