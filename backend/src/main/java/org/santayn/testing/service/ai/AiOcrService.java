package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Распознавание текста на изображении страницы.
 * <ul>
 *     <li>{@code yandex} — Yandex Vision OCR, около 0,13 ₽ за страницу печатного текста;</li>
 *     <li>{@code tesseract} — бесплатно на нашем сервере (пакеты tesseract-ocr, tesseract-ocr-rus).</li>
 * </ul>
 */
@Service
public class AiOcrService {

    public static final String OCR_NONE = "none";
    public static final String OCR_YANDEX = "yandex";
    public static final String OCR_TESSERACT = "tesseract";

    private static final Logger LOGGER = LoggerFactory.getLogger(AiOcrService.class);
    /** Ограничение Vision OCR на размер файла. */
    private static final int YANDEX_MAX_BYTES = 10 * 1024 * 1024;

    private final AiProperties properties;
    private final AiProviderEndpoints endpoints;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile Boolean tesseractInstalled;

    public AiOcrService(AiProperties properties,
                        AiProviderEndpoints endpoints,
                        HttpClient aiHttpClient,
                        ObjectMapper objectMapper) {
        this.properties = properties;
        this.endpoints = endpoints;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    /** Какой распознаватель будет использован. */
    public String engine() {
        String configured = AiProviderEndpoints.trim(properties.getOcr().getProvider()).toLowerCase(Locale.ROOT);
        if (OCR_NONE.equals(configured)) {
            return OCR_NONE;
        }
        if (OCR_YANDEX.equals(configured)) {
            return OCR_YANDEX;
        }
        if (OCR_TESSERACT.equals(configured)) {
            return isTesseractInstalled() ? OCR_TESSERACT : OCR_NONE;
        }
        if (endpoints.isYandex() && !AiProviderEndpoints.trim(properties.getApiKey()).isEmpty()) {
            return OCR_YANDEX;
        }
        return isTesseractInstalled() ? OCR_TESSERACT : OCR_NONE;
    }

    public boolean isAvailable() {
        return !OCR_NONE.equals(engine());
    }

    /**
     * @param image  содержимое JPEG или PNG
     * @param format JPEG | PNG
     */
    public String recognize(byte[] image, String format) {
        return switch (engine()) {
            case OCR_YANDEX -> recognizeWithYandex(image, format);
            case OCR_TESSERACT -> recognizeWithTesseract(image, format);
            default -> throw new AiUnavailableException(
                    "Распознавание сканов не настроено: нужен Yandex Vision OCR или установленный tesseract."
            );
        };
    }

    private String recognizeWithYandex(byte[] image, String format) {
        if (image.length > YANDEX_MAX_BYTES) {
            throw new IllegalArgumentException("Страница скана больше 10 МБ — уменьшите разрешение фото.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mimeType", format);
        body.put("languageCodes", List.of("ru", "en"));
        body.put("model", properties.getOcr().getYandexModel());
        body.put("content", Base64.getEncoder().encodeToString(image));
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoints.yandexOcrUrl()))
                    .timeout(Duration.ofSeconds(Math.max(30, properties.getTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            builder.header("Authorization", "Api-Key " + endpoints.yandexApiKey().get());
            builder.header("x-folder-id", endpoints.requireFolderId());
            builder.header("x-data-logging-enabled", "false");
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new AiUnavailableException(
                        "Распознавание текста не удалось: " + OpenAiCompatibleChatClient.describeError(response.statusCode())
                );
            }
            return extractYandexText(objectMapper.readTree(response.body()));
        } catch (AiUnavailableException | IllegalArgumentException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Распознавание текста прервано.", error);
        } catch (Exception error) {
            throw new AiUnavailableException("Не удалось распознать страницу: " + error.getClass().getSimpleName(), error);
        }
    }

    static String extractYandexText(JsonNode root) {
        JsonNode annotation = root.path("result").path("textAnnotation");
        String fullText = annotation.path("fullText").asText("");
        if (!fullText.isBlank()) {
            return fullText;
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode block : annotation.path("blocks")) {
            for (JsonNode line : block.path("lines")) {
                builder.append(line.path("text").asText("")).append('\n');
            }
            builder.append('\n');
        }
        return builder.toString().trim();
    }

    private String recognizeWithTesseract(byte[] image, String format) {
        Path input = null;
        try {
            input = Files.createTempFile("ocr-page-", "PNG".equalsIgnoreCase(format) ? ".png" : ".jpg");
            Files.write(input, image);
            Process process = new ProcessBuilder(
                    properties.getOcr().getTesseractPath(),
                    input.toString(),
                    "stdout",
                    "-l",
                    properties.getOcr().getTesseractLanguages(),
                    "--psm",
                    "3"
            ).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] output = process.getInputStream().readAllBytes();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AiUnavailableException("Распознавание страницы заняло слишком много времени.");
            }
            if (process.exitValue() != 0) {
                throw new AiUnavailableException("tesseract не смог распознать страницу (код " + process.exitValue() + ").");
            }
            return new String(output, StandardCharsets.UTF_8).trim();
        } catch (AiUnavailableException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("Распознавание текста прервано.", error);
        } catch (IOException error) {
            throw new AiUnavailableException("Не удалось запустить tesseract: " + error.getMessage(), error);
        } finally {
            if (input != null) {
                try {
                    Files.deleteIfExists(input);
                } catch (IOException ignored) {
                    // временный файл удалит система
                }
            }
        }
    }

    private boolean isTesseractInstalled() {
        Boolean cached = tesseractInstalled;
        if (cached != null) {
            return cached;
        }
        boolean installed;
        try {
            Process process = new ProcessBuilder(properties.getOcr().getTesseractPath(), "--version")
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().readAllBytes();
            installed = process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            installed = false;
        } catch (IOException error) {
            installed = false;
        }
        if (!installed) {
            LOGGER.info("tesseract is not installed; scan recognition needs Yandex Vision OCR.");
        }
        tesseractInstalled = installed;
        return installed;
    }
}
