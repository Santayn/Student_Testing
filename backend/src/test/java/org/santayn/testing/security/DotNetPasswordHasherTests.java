package org.santayn.testing.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

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

    @Test
    void rejectsUnsupportedV3Prf() {
        byte[] hash = validV3Payload();
        writeNetworkByteOrder(hash, 1, 99);

        DotNetPasswordHasher.VerificationResult result =
                passwordHasher.verify(Base64.getEncoder().encodeToString(hash), "admin123");

        assertThat(result.success()).isFalse();
    }

    @Test
    void rejectsInvalidV3IterationCount() {
        byte[] hash = validV3Payload();
        writeNetworkByteOrder(hash, 5, 0);

        DotNetPasswordHasher.VerificationResult result =
                passwordHasher.verify(Base64.getEncoder().encodeToString(hash), "admin123");

        assertThat(result.success()).isFalse();
    }

    private static byte[] validV3Payload() {
        byte[] output = new byte[13 + 16 + 16];
        output[0] = 0x01;
        writeNetworkByteOrder(output, 1, 2);
        writeNetworkByteOrder(output, 5, 100_000);
        writeNetworkByteOrder(output, 9, 16);
        return output;
    }

    private static void writeNetworkByteOrder(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value >>> 24);
        buffer[offset + 1] = (byte) (value >>> 16);
        buffer[offset + 2] = (byte) (value >>> 8);
        buffer[offset + 3] = (byte) value;
    }
}
