package org.santayn.testing.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.santayn.testing.models.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class JwtService {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final String issuer;
    private final long accessTokenMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret:CHANGE_ME_DEV_SECRET_1234567890_ABCDEFGHIJKLMNOPQRSTUVWXYZ}") String secret,
            @Value("${app.security.jwt.issuer:org.santayn.testing}") String issuer,
            @Value("${app.security.jwt.access-token-minutes:120}") long accessTokenMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole() != null ? user.getRole().getName() : null);
        claims.put("authorities", buildAuthorities(user));

        if (user.getStudent() != null) {
            claims.put("studentId", user.getStudent().getId());
        }
        if (user.getTeacher() != null) {
            claims.put("teacherId", user.getTeacher().getId());
        }

        return generateToken(claims, user.getLogin());
    }

    public String extractLogin(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object subject = claims.get("sub");
        return subject == null ? null : subject.toString();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Map<String, Object> claims = extractAllClaims(token);
            String login = claims.get("sub") == null ? null : claims.get("sub").toString();
            String tokenIssuer = claims.get("iss") == null ? null : claims.get("iss").toString();

            return login != null
                    && login.equals(userDetails.getUsername())
                    && issuer.equals(tokenIssuer)
                    && !isExpired(claims)
                    && isSignatureValid(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenMinutes * 60L;
    }

    private String generateToken(Map<String, Object> claims, String subject) {
        try {
            long now = Instant.now().getEpochSecond();
            long exp = Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES).getEpochSecond();

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>(claims);
            payload.put("sub", subject);
            payload.put("iss", issuer);
            payload.put("iat", now);
            payload.put("exp", exp);

            String headerPart = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String payloadPart = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signaturePart = sign(headerPart + "." + payloadPart);

            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception ex) {
            throw new IllegalStateException("Не удалось создать JWT token", ex);
        }
    }

    private Map<String, Object> extractAllClaims(String token) {
        try {
            validateTokenStructure(token);
            if (!isSignatureValid(token)) {
                throw new IllegalArgumentException("Неверная подпись JWT");
            }

            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);

            return objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Невалидный JWT token", ex);
        }
    }

    private boolean isExpired(Map<String, Object> claims) {
        Object expObject = claims.get("exp");
        if (!(expObject instanceof Number number)) {
            return true;
        }

        long exp = number.longValue();
        return Instant.now().isAfter(Instant.ofEpochSecond(exp));
    }

    private boolean isSignatureValid(String token) {
        try {
            validateTokenStructure(token);

            String[] parts = token.split("\\.");
            String data = parts[0] + "." + parts[1];
            String actualSignature = parts[2];
            String expectedSignature = sign(data);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    actualSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            return false;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signature);
    }

    private void validateTokenStructure(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Пустой token");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT должен содержать 3 части");
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private List<String> buildAuthorities(User user) {
        if (user.getRole() == null || user.getRole().getName() == null || user.getRole().getName().isBlank()) {
            return List.of("ROLE_USER", "USER");
        }

        String roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);
        return List.of("ROLE_" + roleName, roleName);
    }
}