package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat Completions в формате OpenAI. Им говорят Yandex AI Studio, GigaChat, Ollama, DeepSeek
 * и прокси-агрегаторы — провайдер меняется настройкой, без кода.
 */
@Component
public class OpenAiCompatibleChatClient implements AiChatClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiCompatibleChatClient.class);

    private final AiProperties properties;
    private final AiProviderEndpoints endpoints;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatClient(AiProperties properties,
                                      AiProviderEndpoints endpoints,
                                      HttpClient aiHttpClient,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.endpoints = endpoints;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResult complete(AiChatRequest request) {
        endpoints.requireConfigured();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", endpoints.textModel(request.model()));
        body.put("messages", request.messages().stream()
                .map(message -> Map.of("role", message.role(), "content", message.content()))
                .toList());
        body.put("temperature", request.temperature());
        body.put("max_tokens", request.maxTokens());
        body.put("stream", false);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoints.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            endpoints.headers().forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiUnavailableException(describeError(response.statusCode()), isRetryable(response.statusCode()));
            }
            return parse(response.body());
        } catch (AiUnavailableException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Запрос к ИИ прерван.", error);
        } catch (java.net.http.HttpTimeoutException error) {
            throw new AiUnavailableException("ИИ не ответил вовремя. Попробуйте позже.", error);
        } catch (Exception error) {
            LOGGER.warn("AI chat request failed: {}", error.getClass().getSimpleName());
            throw new AiUnavailableException("Не удалось обратиться к ИИ: " + error.getClass().getSimpleName(), error);
        }
    }

    AiChatResult parse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        String text = "";
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isTextual()) {
                text = content.asText();
            } else if (content.isArray()) {
                StringBuilder builder = new StringBuilder();
                content.forEach(part -> builder.append(part.path("text").asText("")));
                text = builder.toString();
            }
        }
        JsonNode usage = root.path("usage");
        return new AiChatResult(
                stripThinking(text),
                usage.path("prompt_tokens").asLong(0),
                usage.path("completion_tokens").asLong(0)
        );
    }

    /** Рассуждающие модели (Qwen, DeepSeek) иногда пишут ход мыслей в <think>…</think> прямо в ответ. */
    static String stripThinking(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }

    static String describeError(int status) {
        return switch (status) {
            case 401 -> "Провайдер ИИ отклонил ключ доступа (код 401). Проверьте APP_AI_API_KEY.";
            case 403 -> "Провайдер ИИ отказал в доступе (код 403): проверьте APP_AI_API_KEY и что провайдер"
                    + " доступен из сети сервера (некоторые API блокируют отдельные страны и VPN).";
            case 404 -> "Провайдер ИИ не нашёл модель или адрес API (код 404). Проверьте настройки моделей.";
            case 413 -> "Слишком много текста для модели ИИ (код 413): сократите материалы лекции"
                    + " или уменьшите APP_AI_PRESENTATION_MAX_SOURCE_CHARS.";
            case 429 -> "Провайдер ИИ временно ограничил частоту запросов. Попробуйте позже.";
            default -> status >= 500
                    ? "Провайдер ИИ временно недоступен (код " + status + ")."
                    : "Провайдер ИИ отклонил запрос (код " + status + ").";
        };
    }

    /** Лимит частоты и сбои сервера проходят сами; остальные отказы (ключ, модель, размер) — нет. */
    static boolean isRetryable(int status) {
        return status == 408 || status == 429 || status >= 500;
    }

    /** Удобный вызов: системная инструкция + сообщение пользователя. */
    public AiChatResult ask(String model, String system, String user, double temperature, int maxTokens) {
        return complete(new AiChatRequest(
                model,
                List.of(Message.system(system), Message.user(user)),
                temperature,
                maxTokens
        ));
    }
}
