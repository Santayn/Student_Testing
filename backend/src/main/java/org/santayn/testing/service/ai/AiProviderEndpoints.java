package org.santayn.testing.service.ai;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Адреса, имена моделей и заголовки авторизации для выбранного провайдера.
 * Ключ API никогда не попадает в сообщения об ошибках и логи.
 */
@Component
public class AiProviderEndpoints {

    static final String YANDEX_BASE_URL = "https://ai.api.cloud.yandex.net/v1";
    static final String YANDEX_OCR_URL = "https://ai.api.cloud.yandex.net/ocr/v1/recognizeText";
    static final String GIGACHAT_BASE_URL = "https://gigachat.devices.sberbank.ru/api/v1";
    static final String OPENAI_BASE_URL = "https://api.openai.com/v1";

    private final AiProperties properties;
    private final GigaChatTokenProvider gigaChatTokenProvider;

    public AiProviderEndpoints(AiProperties properties, GigaChatTokenProvider gigaChatTokenProvider) {
        this.properties = properties;
        this.gigaChatTokenProvider = gigaChatTokenProvider;
    }

    public String provider() {
        return properties.normalizedProvider();
    }

    public boolean isYandex() {
        return AiProperties.PROVIDER_YANDEX.equals(provider());
    }

    public boolean isGigaChat() {
        return AiProperties.PROVIDER_GIGACHAT.equals(provider());
    }

    public String baseUrl() {
        String configured = trim(properties.getBaseUrl());
        if (!configured.isEmpty()) {
            return stripTrailingSlash(configured);
        }
        return switch (provider()) {
            case AiProperties.PROVIDER_YANDEX -> YANDEX_BASE_URL;
            case AiProperties.PROVIDER_GIGACHAT -> GIGACHAT_BASE_URL;
            default -> OPENAI_BASE_URL;
        };
    }

    /** Имя текстовой модели: у Yandex — URI вида gpt://каталог/модель. */
    public String textModel(String model) {
        String name = trim(model);
        if (isYandex() && !name.contains("://")) {
            return "gpt://" + requireFolderId() + "/" + name;
        }
        return name;
    }

    /** Имя модели картинок: у Yandex — URI вида art://каталог/модель. */
    public String imageModel(String model) {
        String name = trim(model);
        if (isYandex() && !name.contains("://")) {
            return "art://" + requireFolderId() + "/" + name;
        }
        return name;
    }

    /** Заголовки для запросов к API провайдера (включая Authorization). */
    public Map<String, String> headers() {
        requireConfigured();
        Map<String, String> headers = new LinkedHashMap<>();
        if (isGigaChat()) {
            headers.put("Authorization", "Bearer " + gigaChatTokenProvider.accessToken());
            return headers;
        }
        String key = trim(properties.getApiKey());
        if (!key.isEmpty()) {
            headers.put("Authorization", authScheme() + " " + key);
        }
        if (isYandex()) {
            headers.put("OpenAI-Project", requireFolderId());
            headers.put("x-folder-id", requireFolderId());
            // Не разрешаем провайдеру сохранять запросы для обучения: в них ответы студентов.
            headers.put("x-data-logging-enabled", "false");
        }
        return headers;
    }

    public String yandexOcrUrl() {
        return YANDEX_OCR_URL;
    }

    public Supplier<String> yandexApiKey() {
        return () -> trim(properties.getApiKey());
    }

    public void requireConfigured() {
        if (!properties.textEnabled()) {
            throw new AiUnavailableException(
                    "ИИ не настроен: администратор должен указать APP_AI_PROVIDER и APP_AI_API_KEY."
            );
        }
        if (!AiProperties.PROVIDER_OPENAI.equals(provider()) && trim(properties.getApiKey()).isEmpty()) {
            throw new AiUnavailableException("ИИ не настроен: не указан ключ API (APP_AI_API_KEY).");
        }
    }

    String requireFolderId() {
        String folderId = trim(properties.getFolderId());
        if (folderId.isEmpty()) {
            throw new AiUnavailableException("ИИ не настроен: для Yandex нужен идентификатор каталога (APP_AI_FOLDER_ID).");
        }
        return folderId;
    }

    private String authScheme() {
        String scheme = trim(properties.getAuthScheme());
        if (!scheme.isEmpty()) {
            return scheme;
        }
        return isYandex() ? "Api-Key" : "Bearer";
    }

    static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
