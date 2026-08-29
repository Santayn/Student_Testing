package org.santayn.testing.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String secret;
    private final String issuer;
    private final String audience;
    private final long shortAccessTokenMinutes;
    private final long longAccessTokenMinutes;
    private final long shortRefreshDays;
    private final long longRefreshDays;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret:CHANGE_ME_DEV_SECRET_1234567890_ABCDEFGHIJKLMNOPQRSTUVWXYZ}") String secret,
            @Value("${app.security.jwt.issuer:org.santayn.testing}") String issuer,
            @Value("${app.security.jwt.audience:org.santayn.testing}") String audience,
            @Value("${app.security.jwt.short-access-token-minutes:15}") long shortAccessTokenMinutes,
            @Value("${app.security.jwt.long-access-token-minutes:120}") long longAccessTokenMinutes,
            @Value("${app.security.jwt.short-refresh-days:7}") long shortRefreshDays,
            @Value("${app.security.jwt.long-refresh-days:30}") long longRefreshDays) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.issuer = issuer;
        this.audience = audience;
        this.shortAccessTokenMinutes = shortAccessTokenMinutes;
        this.longAccessTokenMinutes = longAccessTokenMinutes;
        this.shortRefreshDays = shortRefreshDays;
        this.longRefreshDays = longRefreshDays;
    }

    public GeneratedTokens generateTokens(User user, int lifetimeKind) {
        int normalizedLifetimeKind = normalizeLifetimeKind(lifetimeKind);
        Instant accessExpiresAt = Instant.now().plus(accessTokenMinutes(normalizedLifetimeKind), ChronoUnit.MINUTES);
        Instant refreshExpiresAt = Instant.now().plus(
                normalizedLifetimeKind == 2 ? longRefreshDays : shortRefreshDays,
                ChronoUnit.DAYS
        );

        String refreshToken = generateRefreshTokenValue();

        return new GeneratedTokens(
                generateAccessToken(user, accessExpiresAt),
                accessExpiresAt,
                refreshToken,
                computeRefreshTokenHash(refreshToken),
                refreshExpiresAt,
                normalizedLifetimeKind
        );
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user, Instant.now().plus(shortAccessTokenMinutes, ChronoUnit.MINUTES));
    }

    public String extractLogin(String token) {
        Map<String, Object> claims = extractAllClaims(token);
        Object uniqueName = claims.get("unique_name");
        if (uniqueName != null && !uniqueName.toString().isBlank()) {
            return uniqueName.toString();
        }

        Object name = claims.get("name");
        if (name != null && !name.toString().isBlank()) {
            return name.toString();
        }

        Object login = claims.get("login");
        return login == null ? null : login.toString();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Map<String, Object> claims = extractAllClaims(token);
            String login = extractLogin(token);
            String tokenIssuer = claims.get("iss") == null ? null : claims.get("iss").toString();
            String tokenAudience = claims.get("aud") == null ? null : claims.get("aud").toString();

            return login != null
                    && login.equals(userDetails.getUsername())
                    && issuer.equals(tokenIssuer)
                    && (audience == null || audience.isBlank() || audience.equals(tokenAudience))
                    && !isExpired(claims)
                    && isSignatureValid(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public int normalizeLifetimeKind(Integer lifetimeKind) {
        if (lifetimeKind == null) {
            return 1;
        }
        return lifetimeKind == 2 ? 2 : 1;
    }

    public String computeRefreshTokenHash(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02X", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not compute refresh token hash.", ex);
        }
    }

    public long getAccessTokenExpiresInSeconds() {
        return shortAccessTokenMinutes * 60L;
    }

    private String generateAccessToken(User user, Instant expiresAt) {
        try {
            Instant now = Instant.now();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Set<String> roles = roleNames(user);
            Set<String> permissions = permissionNames(user);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.getId() == null ? null : user.getId().toString());
            payload.put("unique_name", user.getLogin());
            payload.put("name", user.getLogin());
            payload.put("nameid", user.getId() == null ? null : user.getId().toString());
            payload.put("jti", UUID.randomUUID().toString().replace("-", ""));
            payload.put("login", user.getLogin());
            payload.put("userId", user.getId());
            payload.put("personId", user.getPersonId());
            payload.put("person_id", user.getPersonId());
            payload.put("roles", new ArrayList<>(roles));
            payload.put("permissions", new ArrayList<>(permissions));
            payload.put("role", new ArrayList<>(roles));
            payload.put("permission", new ArrayList<>(permissions));
            payload.put("iss", issuer);
            payload.put("aud", audience);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", expiresAt.getEpochSecond());

            String headerPart = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String payloadPart = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signaturePart = sign(headerPart + "." + payloadPart);

            return headerPart + "." + payloadPart + "." + signaturePart;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create JWT token.", ex);
        }
    }

    private Set<String> roleNames(User user) {
        Set<String> names = new LinkedHashSet<>();
        for (Role role : user.getRoles()) {
            if (role.getName() != null && !role.getName().isBlank()) {
                names.add(role.getName());
            }
        }
        return names;
    }

    private Set<String> permissionNames(User user) {
        Set<String> names = new LinkedHashSet<>();

        for (Permission permission : user.getPermissions()) {
            if (permission.getName() != null && !permission.getName().isBlank()) {
                names.add(permission.getName());
            }
        }

        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                if (permission.getName() != null && !permission.getName().isBlank()) {
                    names.add(permission.getName());
                }
            }
        }

        return names;
    }

    private Map<String, Object> extractAllClaims(String token) {
        try {
            validateTokenStructure(token);
            if (!isSignatureValid(token)) {
                throw new IllegalArgumentException("Invalid JWT signature.");
            }

            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);

            return objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT token.", ex);
        }
    }

    private boolean isExpired(Map<String, Object> claims) {
        Object expObject = claims.get("exp");
        if (!(expObject instanceof Number number)) {
            return true;
        }

        return Instant.now().isAfter(Instant.ofEpochSecond(number.longValue()));
    }

    private boolean isSignatureValid(String token) {
        try {
            validateTokenStructure(token);

            String[] parts = token.split("\\.");
            String data = parts[0] + "." + parts[1];
            String expectedSignature = sign(data);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8)
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
            throw new IllegalArgumentException("Token is empty.");
        }

        if (token.split("\\.").length != 3) {
            throw new IllegalArgumentException("JWT must contain 3 parts.");
        }
    }

    private String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private long accessTokenMinutes(int lifetimeKind) {
        return lifetimeKind == 2 ? longAccessTokenMinutes : shortAccessTokenMinutes;
    }

    public record GeneratedTokens(
            String accessToken,
            Instant accessTokenExpiresAtUtc,
            String refreshToken,
            String refreshTokenHash,
            Instant refreshTokenExpiresAtUtc,
            int lifetimeKind
    ) {
    }
}
