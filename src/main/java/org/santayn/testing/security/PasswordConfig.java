package org.santayn.testing.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder(DotNetPasswordHasher passwordHasher) {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return passwordHasher.hashPassword(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return passwordHasher.verify(encodedPassword, rawPassword.toString()).success();
            }
        };
    }
}
