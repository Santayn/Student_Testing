package org.santayn.testing.service.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Настройки ИИ (app.ai.*). Провайдер выбирается одной переменной APP_AI_PROVIDER:
 * <ul>
 *     <li>{@code none} — ИИ выключен (по умолчанию);</li>
 *     <li>{@code yandex} — Yandex AI Studio: тексты, распознавание сканов и картинки;</li>
 *     <li>{@code gigachat} — GigaChat API (только тексты);</li>
 *     <li>{@code openai} — любой OpenAI-совместимый API: Ollama, DeepSeek, прокси-агрегаторы.</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    public static final String PROVIDER_NONE = "none";
    public static final String PROVIDER_YANDEX = "yandex";
    public static final String PROVIDER_GIGACHAT = "gigachat";
    public static final String PROVIDER_OPENAI = "openai";

    private String provider = PROVIDER_NONE;

    /** Базовый адрес API; пусто — адрес по умолчанию для провайдера. */
    private String baseUrl = "";

    private String apiKey = "";

    /** Каталог Yandex Cloud: подставляется в имена моделей и заголовки. */
    private String folderId = "";

    /** Схема заголовка Authorization; пусто — Api-Key для Yandex, Bearer для остальных. */
    private String authScheme = "";

    private int timeoutSeconds = 90;

    /** Лимит токенов за календарный месяц (вход + выход), 0 — без лимита. */
    private long monthlyTokenLimit = 20_000_000L;

    private Models models = new Models();
    private Grading grading = new Grading();
    private Ocr ocr = new Ocr();
    private Images images = new Images();
    private Presentation presentation = new Presentation();
    private GigaChat gigachat = new GigaChat();
    private Worker worker = new Worker();
    private Storage storage = new Storage();

    public String normalizedProvider() {
        String value = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case PROVIDER_YANDEX, PROVIDER_GIGACHAT, PROVIDER_OPENAI -> value;
            default -> PROVIDER_NONE;
        };
    }

    public boolean textEnabled() {
        return !PROVIDER_NONE.equals(normalizedProvider());
    }

    @Getter
    @Setter
    public static class Models {
        /** Проверка ответов и подсказка критериев: дешёвая быстрая модель. */
        private String grading = "aliceai-llm-flash";
        /** Разметка распознанного текста лекции. */
        private String lecture = "aliceai-llm-flash";
        /** План презентации: модель посильнее. */
        private String presentation = "aliceai-llm";
        /** Генерация картинок. */
        private String image = "aliceai-image-art-3.0";
    }

    @Getter
    @Setter
    public static class Grading {
        private boolean enabled = true;
        /** Ниже этой уверенности ответ уходит преподавателю в очередь «На проверку». */
        private BigDecimal reviewThreshold = new BigDecimal("0.7");
        private int maxAttempts = 3;
    }

    @Getter
    @Setter
    public static class Ocr {
        /** none | yandex | tesseract; пусто — yandex для провайдера yandex, иначе tesseract. */
        private String provider = "";
        private String tesseractPath = "tesseract";
        private String tesseractLanguages = "rus+eng";
        /** Распознавание через Yandex: печатный текст — page. */
        private String yandexModel = "page";
    }

    @Getter
    @Setter
    public static class Images {
        private boolean generationEnabled = true;
        private boolean searchEnabled = true;
        private String size = "1024x1024";
    }

    @Getter
    @Setter
    public static class Presentation {
        /**
         * Сколько символов текста лекции и материалов уходит модели для плана.
         * Groq на бесплатном тарифе принимает до 8000 токенов в минуту (≈ 20 000 символов по-русски).
         */
        private int maxSourceChars = 60_000;
    }

    @Getter
    @Setter
    public static class GigaChat {
        private String authUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
        private String scope = "GIGACHAT_API_PERS";
    }

    @Getter
    @Setter
    public static class Worker {
        private boolean enabled = true;
        private long pollIntervalMs = 2000;
        private int threads = 3;
    }

    @Getter
    @Setter
    public static class Storage {
        /** Временные файлы задач: загруженные сканы, сгенерированные картинки. */
        private String dir = "uploads/ai";
    }
}
