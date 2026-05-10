package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureMaterialRepository;
import org.santayn.testing.repository.LectureRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LectureMaterialService {

    private final LectureRepository lectureRepository;
    private final LectureMaterialRepository lectureMaterialRepository;

    @Value("${app.storage.lecture-materials-dir:uploads/lecture-materials}")
    private String lectureMaterialsDir;

    @Transactional(readOnly = true)
    public List<LectureMaterial> findByLectureId(Integer lectureId) {
        requireLecture(lectureId);
        return lectureMaterialRepository.findByCourseLectureIdOrderByIdAsc(lectureId);
    }

    @Transactional
    public List<LectureMaterial> upload(Integer lectureId, List<MultipartFile> files) {
        Lecture lecture = requireLecture(lectureId);
        List<MultipartFile> normalizedFiles = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (normalizedFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required.");
        }

        Path lectureDir = lectureDirectory(lecture.getId());
        List<LectureMaterial> saved = new ArrayList<>();
        for (MultipartFile file : normalizedFiles) {
            String originalName = normalizeOriginalFileName(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + "-" + originalName;
            Path target = lectureDir.resolve(storedName).normalize();
            if (!target.startsWith(lectureDir)) {
                throw new IllegalArgumentException("Invalid file path.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException error) {
                throw new IllegalStateException("Failed to save lecture file: " + originalName, error);
            }

            LectureMaterial material = new LectureMaterial();
            material.setCourseLectureId(lecture.getId());
            material.setFileName(originalName);
            material.setStoredPath(materialsRoot().relativize(target).toString().replace('\\', '/'));
            material.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : null);
            material.setSizeBytes(file.getSize());
            material.setUploadedAtUtc(Instant.now());
            saved.add(lectureMaterialRepository.save(material));
        }
        return saved;
    }

    @Transactional
    public void delete(Integer lectureId, Integer materialId) {
        LectureMaterial material = lectureMaterialRepository.findByIdAndCourseLectureId(materialId, lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture material not found: " + materialId));
        lectureMaterialRepository.delete(material);
        try {
            Files.deleteIfExists(resolveMaterialPath(material));
        } catch (IOException error) {
            throw new IllegalStateException("Failed to delete lecture file: " + material.getFileName(), error);
        }
    }

    @Transactional(readOnly = true)
    public StoredLectureMaterial load(Integer lectureId, Integer materialId) {
        requireLecture(lectureId);
        LectureMaterial material = lectureMaterialRepository.findByIdAndCourseLectureId(materialId, lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture material not found: " + materialId));
        Path path = resolveMaterialPath(material);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Lecture material file not found: " + material.getFileName());
        }
        return new StoredLectureMaterial(material, path);
    }

    private Lecture requireLecture(Integer lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + lectureId));
    }

    private Path materialsRoot() {
        Path root = Path.of(lectureMaterialsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to initialize lecture materials directory.", error);
        }
        return root;
    }

    private Path lectureDirectory(Integer lectureId) {
        Path directory = materialsRoot().resolve("lecture-" + lectureId).normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to initialize lecture directory: " + lectureId, error);
        }
        return directory;
    }

    private Path resolveMaterialPath(LectureMaterial material) {
        Path path = materialsRoot().resolve(material.getStoredPath()).normalize();
        if (!path.startsWith(materialsRoot())) {
            throw new IllegalArgumentException("Invalid stored lecture material path.");
        }
        return path;
    }

    private static String normalizeOriginalFileName(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename).trim();
        if (!StringUtils.hasText(cleaned)) {
            return "lecture-file.bin";
        }
        cleaned = cleaned.replace('\\', '_').replace('/', '_');
        if (".".equals(cleaned) || "..".equals(cleaned)) {
            return "lecture-file.bin";
        }
        return cleaned.toLowerCase(Locale.ROOT).endsWith(".") ? cleaned + "bin" : cleaned;
    }

    public record StoredLectureMaterial(LectureMaterial material, Path path) {
    }
}
