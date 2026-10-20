package org.santayn.testing.service.ai;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/** Файлы задач ИИ: загруженные сканы (до распознавания) и сгенерированные картинки. */
@Component
public class AiFileStorage {

    private final AiProperties properties;

    public AiFileStorage(AiProperties properties) {
        this.properties = properties;
    }

    /** Новая папка для одной партии сканов. */
    public Path newScanBatch() {
        return createDirectories(root().resolve("scans").resolve(UUID.randomUUID().toString()));
    }

    public String saveImage(byte[] content, String extension) {
        Path directory = createDirectories(root().resolve("images"));
        Path target = directory.resolve(UUID.randomUUID() + "." + extension);
        try {
            Files.write(target, content);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to save generated image.", error);
        }
        return relative(target);
    }

    public String relative(Path path) {
        return root().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    public Path resolve(String relativePath) {
        Path path = root().resolve(relativePath).normalize();
        if (!path.startsWith(root())) {
            throw new IllegalArgumentException("Invalid AI file path.");
        }
        return path;
    }

    public void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory) || !directory.normalize().startsWith(root())) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // остатки удалятся при следующей очистке
                }
            });
        } catch (IOException ignored) {
            // папки уже нет
        }
    }

    private Path root() {
        return createDirectories(Path.of(properties.getStorage().getDir()).toAbsolutePath().normalize());
    }

    private static Path createDirectories(Path path) {
        try {
            return Files.createDirectories(path);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to initialize AI storage: " + path, error);
        }
    }
}
