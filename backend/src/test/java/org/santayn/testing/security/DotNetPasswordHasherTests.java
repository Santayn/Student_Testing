package org.santayn.testing.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DotNetPasswordHasherTests {

    private final DotNetPasswordHasher passwordHasher = new DotNetPasswordHasher();

    @Test
    void verifiesGeneratedHash() {
        String hash = passwordHasher.hashPassword("admin123");

        DotNetPasswordHasher.VerificationResult result = passwordHasher.verify(hash, "admin123");

        assertThat(result.success()).isTrue();
        assertThat(result.requiresRehash()).isFalse();
    }

    @Test
    void rejectsWrongPassword() {
        String hash = passwordHasher.hashPassword("admin123");

        DotNetPasswordHasher.VerificationResult result = passwordHasher.verify(hash, "wrong-password");

        assertThat(result.success()).isFalse();
    }
}
