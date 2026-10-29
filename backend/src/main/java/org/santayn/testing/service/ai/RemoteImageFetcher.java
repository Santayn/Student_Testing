package org.santayn.testing.service.ai;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

/**
 * Скачивает картинки из банков свободных изображений. Только https и только хосты из списка —
 * чтобы через сервер нельзя было обратиться к произвольному адресу (в том числе внутреннему).
 */
@Component
public class RemoteImageFetcher {

    static final Set<String> ALLOWED_HOSTS = Set.of(
            "upload.wikimedia.org",
            // С 2026 г. Commons отдаёт превью (thumburl) с отдельного хоста.
            "thumb.wikimedia.org",
            "api.openverse.org"
    );
    static final String USER_AGENT = "StudentTesting/1.0 (university learning platform)";
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;

    private final HttpClient httpClient;

    public RemoteImageFetcher(HttpClient aiHttpClient) {
        this.httpClient = aiHttpClient;
    }

    public static boolean isAllowed(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && ALLOWED_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
                    && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public Image fetch(String url) {
        String current = url;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            if (!isAllowed(current)) {
                throw new IllegalArgumentException("Картинку можно взять только из Wikimedia Commons или Openverse.");
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(current))
                        .timeout(Duration.ofSeconds(25))
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "image/*")
                        .GET()
                        .build();
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                if (status / 100 == 3) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    response.body().close();
                    if (location == null) {
                        break;
                    }
                    current = URI.create(current).resolve(location).toString();
                    continue;
                }
                if (status / 100 != 2) {
                    response.body().close();
                    throw new AiUnavailableException("Не удалось загрузить картинку (код " + status + ").");
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
                if (!contentType.startsWith("image/")) {
                    response.body().close();
                    throw new AiUnavailableException("По ссылке не картинка.");
                }
                return new Image(readLimited(response.body()), contentType);
            } catch (AiUnavailableException | IllegalArgumentException error) {
                throw error;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AiUnavailableException("Загрузка картинки прервана.", error);
            } catch (Exception error) {
                throw new AiUnavailableException("Не удалось загрузить картинку: " + error.getClass().getSimpleName(), error);
            }
        }
        throw new AiUnavailableException("Слишком много перенаправлений при загрузке картинки.");
    }

    private static byte[] readLimited(InputStream input) throws java.io.IOException {
        try (input) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) {
                    throw new AiUnavailableException("Картинка больше 10 МБ.");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    public record Image(byte[] content, String contentType) {
    }
}
