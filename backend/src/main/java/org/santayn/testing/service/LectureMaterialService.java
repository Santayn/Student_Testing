package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureMaterialRepository;
import org.santayn.testing.repository.LectureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
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

    private static final Logger log = LoggerFactory.getLogger(LectureMaterialService.class);
    private final LectureRepository lectureRepository;
    private final LectureMaterialRepository lectureMaterialRepository;
    private final ActiveTeacherSubjectMembershipService activeTeacherSubjectMembershipService;

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
        requireActiveLectureContext(lecture);
        List<MultipartFile> normalizedFiles = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (normalizedFiles.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required.");
        }

        Path lectureDir = lectureDirectory(lecture.getId());
        List<Path> stagedPaths = new ArrayList<>();
        List<Path> finalPaths = new ArrayList<>();
        registerUploadTransactionCleanup(stagedPaths, finalPaths);

        List<LectureMaterial> saved = new ArrayList<>();
        try {
            for (MultipartFile file : normalizedFiles) {
                String originalName = normalizeOriginalFileName(file.getOriginalFilename());
                String storedName = UUID.randomUUID() + "-" + originalName;
                Path target = lectureDir.resolve(storedName).normalize();
                if (!target.startsWith(lectureDir)) {
                    throw new IllegalArgumentException("Invalid file path.");
                }
                Path staging = lectureDir.resolve("." + storedName + ".uploading-" + UUID.randomUUID()).normalize();
                if (!staging.startsWith(lectureDir)) {
                    throw new IllegalArgumentException("Invalid staging file path.");
                }

                stagedPaths.add(staging);
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, staging, StandardCopyOption.REPLACE_EXISTING);
                }

                LectureMaterial material = new LectureMaterial();
                material.setCourseLectureId(lecture.getId());
                material.setFileName(originalName);
                material.setStoredPath(materialsRoot().relativize(target).toString().replace('\\', '/'));
                material.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : null);
                material.setSizeBytes(file.getSize());
                material.setUploadedAtUtc(Instant.now());
                saved.add(lectureMaterialRepository.save(material));

                moveAtomicallyIfPossible(staging, target);
                stagedPaths.remove(staging);
                finalPaths.add(target);
            }
            return saved;
        } catch (IOException error) {
            cleanupPaths(stagedPaths);
            cleanupPaths(finalPaths);
            throw new IllegalStateException("Failed to save lecture file.", error);
        } catch (RuntimeException error) {
            cleanupPaths(stagedPaths);
            cleanupPaths(finalPaths);
            throw error;
        }
    }

    @Transactional
    public void delete(Integer lectureId, Integer materialId) {
        requireActiveLectureContext(requireLecture(lectureId));
        LectureMaterial material = lectureMaterialRepository.findByIdAndCourseLectureId(materialId, lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture material not found: " + materialId));
        Path original = resolveMaterialPath(material);
        Path quarantined = null;

        if (Files.exists(original)) {
            quarantined = original.resolveSibling("." + original.getFileName() + ".deleting-" + UUID.randomUUID());
            try {
                moveAtomicallyIfPossible(original, quarantined);
            } catch (IOException error) {
                throw new IllegalStateException("Failed to stage lecture file deletion: " + material.getFileName(), error);
            }
        }

        boolean transactionCleanupRegistered = registerDeleteTransactionCleanup(original, quarantined);
        try {
            lectureMaterialRepository.delete(material);
            if (!transactionCleanupRegistered) {
                cleanupPath(quarantined);
            }
        } catch (RuntimeException error) {
            restoreQuarantinedFile(original, quarantined);
            throw error;
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

    private void requireActiveLectureContext(Lecture lecture) {
        if (lecture.getSubjectMembershipId() == null) {
            return;
        }
        activeTeacherSubjectMembershipService.requireActiveTeacher(lecture.getSubjectMembershipId());
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
        Path root = materialsRoot();
        Path path = root.resolve(material.getStoredPath()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid stored lecture material path.");
        }
        return path;
    }

    private void registerUploadTransactionCleanup(List<Path> stagedPaths, List<Path> finalPaths) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                cleanupPaths(stagedPaths);
                if (status != STATUS_COMMITTED) {
                    cleanupPaths(finalPaths);
                }
            }
        });
    }

    private boolean registerDeleteTransactionCleanup(Path original, Path quarantined) {
        if (quarantined == null || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    cleanupPath(quarantined);
                } else {
                    restoreQuarantinedFile(original, quarantined);
                }
            }
        });
        return true;
    }

    private static void moveAtomicallyIfPossible(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cleanupPaths(List<Path> paths) {
        for (Path path : List.copyOf(paths)) {
            cleanupPath(path);
        }
    }

    private static void cleanupPath(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException error) {
            log.warn("Failed to clean lecture material path {}", path, error);
        }
    }

    private static void restoreQuarantinedFile(Path original, Path quarantined) {
        if (original == null || quarantined == null || !Files.exists(quarantined)) {
            return;
        }
        try {
            moveAtomicallyIfPossible(quarantined, original);
        } catch (IOException error) {
            log.error("Failed to restore lecture material {} from transaction quarantine {}", original, quarantined, error);
        }
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
