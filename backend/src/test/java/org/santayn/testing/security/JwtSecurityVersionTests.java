package org.santayn.testing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.santayn.testing.models.user.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecurityVersionTests {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            "test-secret-that-is-at-least-32-characters-long",
            "test-issuer",
            "test-audience",
            15,
            120,
            7,
            30
    );

    @Test
    void accessTokenIsRejectedAfterSecurityVersionChanges() {
        User user = new User();
        user.setId(15);
        user.setLogin("student");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setSecurityVersion(3);
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("student")
                .password("hash")
                .authorities(List.of())
                .build();

        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token, details, 3)).isTrue();
        assertThat(jwtService.isTokenValid(token, details, 4)).isFalse();
    }
}
