package org.santayn.testing.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class DotNetPasswordHasher {

    private static final byte FORMAT_MARKER_V2 = 0x00;
    private static final byte FORMAT_MARKER_V3 = 0x01;
    private static final int PRF_HMACSHA1 = 0;
    private static final int PRF_HMACSHA256 = 1;
    private static final int PRF_HMACSHA512 = 2;
    private static final int ITERATION_COUNT = 100_000;
    private static final int SALT_SIZE = 16;
    private static final int SUBKEY_SIZE = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);

        byte[] subkey = pbkdf2(password, salt, PRF_HMACSHA512, ITERATION_COUNT, SUBKEY_SIZE);
        byte[] output = new byte[13 + salt.length + subkey.length];

        output[0] = FORMAT_MARKER_V3;
        writeNetworkByteOrder(output, 1, PRF_HMACSHA512);
        writeNetworkByteOrder(output, 5, ITERATION_COUNT);
        writeNetworkByteOrder(output, 9, salt.length);
        System.arraycopy(salt, 0, output, 13, salt.length);
        System.arraycopy(subkey, 0, output, 13 + salt.length, subkey.length);

        return Base64.getEncoder().encodeToString(output);
    }

    public VerificationResult verify(String hashedPassword, String providedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            return VerificationResult.failed();
        }

        if (hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$") || hashedPassword.startsWith("$2y$")) {
            boolean matches = bcrypt.matches(providedPassword, hashedPassword);
            return matches ? VerificationResult.success(true) : VerificationResult.failed();
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(hashedPassword);

            if (decoded.length == 0) {
                return VerificationResult.failed();
            }

            if (decoded[0] == FORMAT_MARKER_V2) {
                return verifyV2(decoded, providedPassword);
            }

            if (decoded[0] == FORMAT_MARKER_V3) {
                return verifyV3(decoded, providedPassword);
            }

            return VerificationResult.failed();
        } catch (IllegalArgumentException ex) {
            return VerificationResult.failed();
        }
    }

    private VerificationResult verifyV2(byte[] decoded, String providedPassword) {
        if (decoded.length != 1 + SALT_SIZE + SUBKEY_SIZE) {
            return VerificationResult.failed();
        }

        byte[] salt = Arrays.copyOfRange(decoded, 1, 1 + SALT_SIZE);
        byte[] expectedSubkey = Arrays.copyOfRange(decoded, 1 + SALT_SIZE, decoded.length);
        byte[] actualSubkey = pbkdf2(providedPassword, salt, PRF_HMACSHA1, 1_000, expectedSubkey.length);

        boolean matches = MessageDigest.isEqual(actualSubkey, expectedSubkey);
        return matches ? VerificationResult.success(true) : VerificationResult.failed();
    }

    private VerificationResult verifyV3(byte[] decoded, String providedPassword) {
        if (decoded.length < 13) {
            return VerificationResult.failed();
        }

        int prf = readNetworkByteOrder(decoded, 1);
        int iterationCount = readNetworkByteOrder(decoded, 5);
        int saltLength = readNetworkByteOrder(decoded, 9);

        if (saltLength < 0 || decoded.length < 13 + saltLength) {
            return VerificationResult.failed();
        }

        byte[] salt = Arrays.copyOfRange(decoded, 13, 13 + saltLength);
        byte[] expectedSubkey = Arrays.copyOfRange(decoded, 13 + saltLength, decoded.length);

        if (expectedSubkey.length == 0) {
            return VerificationResult.failed();
        }

        byte[] actualSubkey = pbkdf2(providedPassword, salt, prf, iterationCount, expectedSubkey.length);
        boolean matches = MessageDigest.isEqual(actualSubkey, expectedSubkey);
        boolean needsRehash = prf != PRF_HMACSHA512 || iterationCount < ITERATION_COUNT;

        return matches ? VerificationResult.success(needsRehash) : VerificationResult.failed();
    }

    private static byte[] pbkdf2(String password, byte[] salt, int prf, int iterationCount, int derivedKeyLength) {
        try {
            String algorithm = switch (prf) {
                case PRF_HMACSHA1 -> "HmacSHA1";
                case PRF_HMACSHA256 -> "HmacSHA256";
                case PRF_HMACSHA512 -> "HmacSHA512";
                default -> throw new IllegalArgumentException("Unsupported PBKDF2 PRF: " + prf);
            };

            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec key = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(key);

            int hashLength = mac.getMacLength();
            int blockCount = (int) Math.ceil((double) derivedKeyLength / hashLength);
            byte[] derivedKey = new byte[derivedKeyLength];
            int outputOffset = 0;

            for (int blockIndex = 1; blockIndex <= blockCount; blockIndex++) {
                byte[] block = pbkdf2Block(mac, salt, iterationCount, blockIndex);
                int bytesToCopy = Math.min(hashLength, derivedKeyLength - outputOffset);
                System.arraycopy(block, 0, derivedKey, outputOffset, bytesToCopy);
                outputOffset += bytesToCopy;
            }

            return derivedKey;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not compute PBKDF2 hash.", ex);
        }
    }

    private static byte[] pbkdf2Block(Mac mac, byte[] salt, int iterationCount, int blockIndex) {
        mac.update(salt);
        mac.update(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(blockIndex).array());

        byte[] u = mac.doFinal();
        byte[] result = u.clone();

        for (int iteration = 2; iteration <= iterationCount; iteration++) {
            u = mac.doFinal(u);
            for (int i = 0; i < result.length; i++) {
                result[i] ^= u[i];
            }
        }

        return result;
    }

    private static void writeNetworkByteOrder(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value >>> 24);
        buffer[offset + 1] = (byte) (value >>> 16);
        buffer[offset + 2] = (byte) (value >>> 8);
        buffer[offset + 3] = (byte) value;
    }

    private static int readNetworkByteOrder(byte[] buffer, int offset) {
        return ((buffer[offset] & 0xff) << 24)
                | ((buffer[offset + 1] & 0xff) << 16)
                | ((buffer[offset + 2] & 0xff) << 8)
                | (buffer[offset + 3] & 0xff);
    }

    public record VerificationResult(boolean success, boolean requiresRehash) {
        static VerificationResult failed() {
            return new VerificationResult(false, false);
        }

        static VerificationResult success(boolean requiresRehash) {
            return new VerificationResult(true, requiresRehash);
        }
    }
}
