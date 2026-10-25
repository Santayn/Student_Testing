package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/** Генерация картинки через Images API в формате OpenAI (у Yandex — модель aliceai-image-art). */
@Component
public class AiImageClient {

    /** Ограничение Yandex на длину описания картинки. */
    static final int MAX_PROMPT_LENGTH = 500;

    private final AiProperties properties;
    private final AiProviderEndpoints endpoints;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiImageClient(AiProperties properties,
                         AiProviderEndpoints endpoints,
                         HttpClient aiHttpClient,
                         ObjectMapper objectMapper) {
        this.properties = properties;
        this.endpoints = endpoints;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return properties.textEnabled()
                && properties.getImages().isGenerationEnabled()
                && !endpoints.isGigaChat();
    }

    public byte[] generate(String prompt) {
        if (!isAvailable()) {
            throw new AiUnavailableException("Генерация картинок недоступна для выбранного провайдера ИИ.");
        }
        String normalizedPrompt = prompt == null ? "" : prompt.trim();
        if (normalizedPrompt.isEmpty()) {
            throw new IllegalArgumentException("Опишите, что должно быть на картинке.");
        }
        if (normalizedPrompt.length() > MAX_PROMPT_LENGTH) {
            normalizedPrompt = normalizedPrompt.substring(0, MAX_PROMPT_LENGTH);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", endpoints.imageModel(properties.getModels().getImage()));
        body.put("prompt", normalizedPrompt);
        body.put("size", properties.getImages().getSize());
        if (!endpoints.isYandex()) {
            body.put("response_format", "b64_json");
        }
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoints.baseUrl() + "/images/generations"))
                    .timeout(Duration.ofSeconds(Math.max(60, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            endpoints.headers().forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiUnavailableException(
                        OpenAiCompatibleChatClient.describeError(response.statusCode()),
                        OpenAiCompatibleChatClient.isRetryable(response.statusCode())
                );
            }
            JsonNode data = objectMapper.readTree(response.body()).path("data");
            String encoded = data.isArray() && !data.isEmpty() ? data.get(0).path("b64_json").asText("") : "";
            if (encoded.isEmpty()) {
                throw new AiUnavailableException("Провайдер ИИ не вернул картинку.");
            }
            return Base64.getDecoder().decode(encoded);
        } catch (AiUnavailableException | IllegalArgumentException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Генерация картинки прервана.", error);
        } catch (Exception error) {
            throw new AiUnavailableException("Не удалось сгенерировать картинку: " + error.getClass().getSimpleName(), error);
        }
    }
}
