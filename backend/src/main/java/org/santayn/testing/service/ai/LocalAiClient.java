package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Клиент сервиса собственных моделей (local-ai). Работает в локальной сети, без внешних запросов
 * и без расхода токенов.
 */
@Component
public class LocalAiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAiClient.class);
    private static final Duration HEALTH_CACHE = Duration.ofSeconds(15);

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile JsonNode cachedHealth;
    private volatile Instant healthCheckedAt = Instant.EPOCH;

    public LocalAiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public boolean isConfigured() {
        return AiText.trimToNull(properties.getLocal().getBaseUrl()) != null;
    }

    /** Состояние моделей сервиса (кэшируется на 15 с); null — сервис не настроен или не отвечает. */
    public JsonNode health() {
        if (!isConfigured()) {
            return null;
        }
        if (Instant.now().isBefore(healthCheckedAt.plus(HEALTH_CACHE))) {
            return cachedHealth;
        }
        JsonNode health;
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(uri("/health")).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            health = response.statusCode() == 200 ? objectMapper.readTree(response.body()) : null;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            health = null;
        } catch (Exception error) {
            LOGGER.info("local-ai is not reachable: {}", error.getClass().getSimpleName());
            health = null;
        }
        cachedHealth = health;
        healthCheckedAt = Instant.now();
        return health;
    }

    /** Готова ли модель сервиса: grader (проверка ответов) или layout (разметка сканов). */
    public boolean isModelReady(String model) {
        JsonNode health = health();
        return health != null && health.path(model).path("ready").asBoolean(false);
    }

    TextAnswerGradingService.Verdict grade(String question, String reference, String criteria,
                                           BigDecimal points, String answer) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question == null ? "" : question);
        body.put("reference", reference);
        body.put("criteria", criteria);
        body.put("points", points == null ? 1.0 : points.doubleValue());
        body.put("answer", answer == null ? "" : answer);
        JsonNode root = post("/v1/grade", body);
        return new TextAnswerGradingService.Verdict(
                clamp(root.path("score").asDouble(0)),
                clamp(root.path("confidence").asDouble(0)),
                strings(root.path("matched")),
                strings(root.path("missing")),
                AiText.trimToNull(root.path("feedback").asText(null)),
                root.path("suspicious").asBoolean(false)
        );
    }

    JsonNode post(String path, Object body) {
        return post(path, body, Duration.ofSeconds(Math.max(5, properties.getLocal().getTimeoutSeconds())));
    }

    JsonNode post(String path, Object body, Duration timeout) {
        if (!isConfigured()) {
            throw new AiUnavailableException("Собственные модели не подключены (APP_AI_LOCAL_BASE_URL).", false);
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(uri(path))
                            .timeout(timeout)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 503) {
                throw new AiUnavailableException("Собственная модель не загружена — проверьте сервис local-ai.");
            }
            if (response.statusCode() == 422 || response.statusCode() == 400) {
                throw new IllegalArgumentException(detail(response.body(), "Страницы не удалось распознать."));
            }
            if (response.statusCode() != 200) {
                throw new AiUnavailableException("Сервис собственных моделей ответил с ошибкой (код " + response.statusCode() + ").");
            }
            return objectMapper.readTree(response.body());
        } catch (AiUnavailableException | IllegalArgumentException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Запрос к собственной модели прерван.", error);
        } catch (Exception error) {
            throw new AiUnavailableException("Сервис собственных моделей недоступен: " + error.getClass().getSimpleName(), error);
        }
    }

    private String detail(String body, String fallback) {
        try {
            String detail = AiText.trimToNull(objectMapper.readTree(body).path("detail").asText(null));
            return detail == null ? fallback : detail;
        } catch (Exception error) {
            return fallback;
        }
    }

    private URI uri(String path) {
        String base = properties.getLocal().getBaseUrl().trim();
        return URI.create(base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path);
    }

    private static List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                String text = AiText.trimToNull(item.asText(null));
                if (text != null && values.size() < 10) {
                    values.add(AiJobService.truncate(text, 300));
                }
            });
        }
        return values;
    }

    private static double clamp(double value) {
        return Double.isNaN(value) ? 0 : Math.max(0, Math.min(1, value));
    }
}
