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
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceRateLimitTests {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DotNetPasswordHasher passwordHasher;
    @Mock private JwtService jwtService;

    private UserRegisterService service;

    @BeforeEach
    void setUp() {
        AuthenticationRateLimitService rateLimitService = new AuthenticationRateLimitService(
                2, 300, 60,
                2, 300, 60
        );
        service = new UserRegisterService(
                userRepository,
                refreshTokenRepository,
                passwordHasher,
                jwtService,
                rateLimitService,
                false
        );
    }

    @Test
    void repeatedBadPasswordsLeadToTemporaryLoginBlock() {
        User user = new User();
        user.setId(5);
        user.setLogin("student");
        user.setPasswordHash("hash");
        user.setActive(true);
        when(userRepository.findWithSecurityByLogin("student")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("hash", "wrong-password"))
                .thenReturn(new DotNetPasswordHasher.VerificationResult(false, false));

        assertThatThrownBy(() -> service.login("student", "wrong-password", 1, "192.0.2.20", "agent"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.login("student", "wrong-password", 1, "192.0.2.20", "agent"))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> service.login("student", "wrong-password", 1, "192.0.2.20", "agent"))
                .isInstanceOf(AuthenticationRateLimitException.class);
    }
}
