package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Поиск картинок со свободными лицензиями для слайдов: Wikimedia Commons и Openverse.
 * Автор и лицензия возвращаются вместе с картинкой — они попадают в заметки слайда.
 */
@Service
public class ImageSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageSearchService.class);

    private final AiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageSearchService(AiProperties properties, HttpClient aiHttpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = aiHttpClient;
        this.objectMapper = objectMapper;
    }

    public boolean isAvailable() {
        return properties.getImages().isSearchEnabled();
    }

    public List<FoundImage> search(String query, int limit) {
        if (!isAvailable()) {
            throw new AiUnavailableException("Поиск картинок выключен.");
        }
        String normalized = AiText.trimToNull(query);
        if (normalized == null) {
            throw new IllegalArgumentException("Введите, что искать.");
        }
        int perSource = Math.max(2, Math.min(12, limit) / 2);
        List<FoundImage> results = new ArrayList<>();
        results.addAll(safe(() -> wikimedia(normalized, perSource)));
        results.addAll(safe(() -> openverse(normalized, perSource)));
        return results;
    }

    List<FoundImage> wikimedia(String query, int limit) throws Exception {
        String url = "https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search"
                + "&gsrnamespace=6&gsrlimit=" + limit
                + "&gsrsearch=" + encode(query + " filetype:bitmap")
                + "&prop=imageinfo&iiprop=url%7Cextmetadata%7Cmime&iiurlwidth=1280"
                + "&iiextmetadatafilter=Artist%7CLicenseShortName%7CLicenseUrl";
        JsonNode pages = get(url).path("query").path("pages");
        List<FoundImage> images = new ArrayList<>();
        pages.forEach(page -> {
            JsonNode info = page.path("imageinfo").path(0);
            String mime = info.path("mime").asText("");
            String imageUrl = info.path("thumburl").asText(info.path("url").asText(""));
            if (!(mime.equals("image/jpeg") || mime.equals("image/png")) || !RemoteImageFetcher.isAllowed(imageUrl)) {
                return;
            }
            JsonNode metadata = info.path("extmetadata");
            images.add(new FoundImage(
                    "wikimedia",
                    page.path("title").asText("").replaceFirst("^File:", ""),
                    imageUrl,
                    stripHtml(metadata.path("Artist").path("value").asText("")),
                    metadata.path("LicenseShortName").path("value").asText(""),
                    metadata.path("LicenseUrl").path("value").asText(""),
                    info.path("descriptionurl").asText("")
            ));
        });
        return images;
    }

    List<FoundImage> openverse(String query, int limit) throws Exception {
        String url = "https://api.openverse.org/v1/images/?page_size=" + limit + "&q=" + encode(query);
        List<FoundImage> images = new ArrayList<>();
        for (JsonNode item : get(url).path("results")) {
            String thumbnail = item.path("thumbnail").asText("");
            if (!RemoteImageFetcher.isAllowed(thumbnail)) {
                continue;
            }
            String license = (item.path("license").asText("") + " " + item.path("license_version").asText("")).trim();
            images.add(new FoundImage(
                    "openverse",
                    item.path("title").asText(""),
                    thumbnail,
                    item.path("creator").asText(""),
                    license.toUpperCase(java.util.Locale.ROOT),
                    item.path("license_url").asText(""),
                    item.path("foreign_landing_url").asText("")
            ));
        }
        return images;
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", RemoteImageFetcher.USER_AGENT)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Image search returned " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private static List<FoundImage> safe(SearchCall call) {
        try {
            return call.run();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception error) {
            // Один банк недоступен — показываем результаты другого.
            LOGGER.info("Image search source failed: {}", error.getMessage());
            return List.of();
        }
    }

    static String stripHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FunctionalInterface
    private interface SearchCall {
        List<FoundImage> run() throws Exception;
    }

    /** imageUrl — адрес картинки на разрешённом хосте; превью и сборка презентации берут её отсюда. */
    public record FoundImage(String source,
                             String title,
                             String imageUrl,
                             String author,
                             String license,
                             String licenseUrl,
                             String pageUrl) {
        public String attribution() {
            StringBuilder builder = new StringBuilder();
            if (title != null && !title.isBlank()) {
                builder.append(title);
            }
            if (author != null && !author.isBlank()) {
                builder.append(builder.isEmpty() ? "" : " — ").append(author);
            }
            if (license != null && !license.isBlank()) {
                builder.append(builder.isEmpty() ? "" : ", ").append(license);
            }
            if (pageUrl != null && !pageUrl.isBlank()) {
                builder.append(builder.isEmpty() ? "" : ". ").append(pageUrl);
            }
            return builder.toString();
        }
    }
}
