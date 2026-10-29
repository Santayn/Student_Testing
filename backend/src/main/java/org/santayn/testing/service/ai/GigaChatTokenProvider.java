package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Токен доступа GigaChat: обменивается ключ авторизации (Basic) на токен на 30 минут.
 * Сертификаты GigaChat выданы «Russian Trusted Root CA» — его нужно добавить в truststore JVM.
 */
@Component
public class GigaChatTokenProvider {

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String token;
    private Instant expiresAt = Instant.EPOCH;

    public GigaChatTokenProvider(AiProperties properties, HttpClient aiHttpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    public synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(expiresAt.minusSeconds(60))) {
            return token;
        }
        String key = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (key.isEmpty()) {
            throw new AiUnavailableException("ИИ не настроен: не указан ключ авторизации GigaChat (APP_AI_API_KEY).");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getGigachat().getAuthUrl()))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Basic " + key)
                    .header("RqUID", UUID.randomUUID().toString())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "scope=" + URLEncoder.encode(properties.getGigachat().getScope(), StandardCharsets.UTF_8)
                    ))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiUnavailableException("GigaChat не выдал токен доступа (код " + response.statusCode() + ").");
            }
            JsonNode root = objectMapper.readTree(response.body());
            token = root.path("access_token").asText(null);
            long expiresAtMillis = root.path("expires_at").asLong(0);
            expiresAt = expiresAtMillis > 0
                    ? Instant.ofEpochMilli(expiresAtMillis)
                    : Instant.now().plus(Duration.ofMinutes(25));
            if (token == null || token.isBlank()) {
                throw new AiUnavailableException("GigaChat вернул пустой токен доступа.");
            }
            return token;
        } catch (AiUnavailableException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Запрос токена GigaChat прерван.", error);
        } catch (Exception error) {
            throw new AiUnavailableException("Не удалось получить токен GigaChat: " + error.getClass().getSimpleName(), error);
        }
    }
}
