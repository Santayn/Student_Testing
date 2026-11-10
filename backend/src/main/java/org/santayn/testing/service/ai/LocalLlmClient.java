package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собственная дообученная LLM, запущенная в Ollama (app.ai.local.llm-base-url — корень Ollama,
 * например http://ollama:11434). Используется для подсказки критериев.
 */
@Component
public class LocalLlmClient {

    private static final Duration TAGS_CACHE = Duration.ofSeconds(30);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile Boolean modelReady;
    private volatile Instant checkedAt = Instant.EPOCH;

    public LocalLlmClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public boolean isConfigured() {
        return AiText.trimToNull(properties.getLocal().getLlmBaseUrl()) != null
                && AiText.trimToNull(properties.getLocal().getCriteriaModel()) != null;
    }

    /** Модель скачана в Ollama (проверка по /api/tags, кэш 30 с). */
    public boolean isModelReady() {
        if (!isConfigured()) {
            return false;
        }
        if (modelReady != null && Instant.now().isBefore(checkedAt.plus(TAGS_CACHE))) {
            return modelReady;
        }
        boolean ready = false;
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(uri("/api/tags")).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 200) {
                String wanted = properties.getLocal().getCriteriaModel().trim();
                for (JsonNode model : objectMapper.readTree(response.body()).path("models")) {
                    String name = model.path("name").asText("");
                    if (name.equals(wanted) || name.equals(wanted + ":latest")) {
                        ready = true;
                        break;
                    }
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // Ollama не запущена — модель недоступна
        }
        modelReady = ready;
        checkedAt = Instant.now();
        return ready;
    }

    /** Ответ модели (JSON-режим). */
    public String ask(String system, String user, int maxTokens) {
        if (!isConfigured()) {
            throw new AiUnavailableException("Собственная языковая модель не подключена (APP_AI_LOCAL_LLM_BASE_URL).", false);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getLocal().getCriteriaModel().trim());
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));
        body.put("temperature", 0.2);
        body.put("max_tokens", maxTokens);
        body.put("response_format", Map.of("type", "json_object"));
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(uri("/v1/chat/completions"))
                            .timeout(Duration.ofSeconds(Math.max(10, properties.getLocal().getLlmTimeoutSeconds())))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() == 404) {
                throw new AiUnavailableException("Модель «" + properties.getLocal().getCriteriaModel()
                        + "» не найдена в Ollama — загрузите её (ollama create).", false);
            }
            if (response.statusCode() != 200) {
                throw new AiUnavailableException("Собственная языковая модель ответила с ошибкой (код " + response.statusCode() + ").");
            }
            JsonNode choices = objectMapper.readTree(response.body()).path("choices");
            String text = choices.path(0).path("message").path("content").asText("");
            return text.replaceAll("(?s)<think>.*?</think>", "").trim();
        } catch (AiUnavailableException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Запрос к собственной модели прерван.", error);
        } catch (java.net.http.HttpTimeoutException error) {
            throw new AiUnavailableException("Собственная модель не ответила вовремя.", error);
        } catch (Exception error) {
            throw new AiUnavailableException("Собственная языковая модель недоступна: " + error.getClass().getSimpleName(), error);
        }
    }

    private URI uri(String path) {
        String base = properties.getLocal().getLlmBaseUrl().trim();
        return URI.create(base.endsWith("/") ? base.substring(0, base.length() - 1) + path : base + path);
    }
}
