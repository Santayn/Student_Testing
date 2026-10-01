package org.santayn.testing.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.repository.LectureMaterialRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LectureMaterialServiceAtomicityTests {

    @Mock private LectureRepository lectureRepository;
    @Mock private LectureMaterialRepository lectureMaterialRepository;
    @Mock private SubjectMembershipRepository subjectMembershipRepository;

    @TempDir
    Path tempDir;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadRemovesCommittedFileWhenDatabaseTransactionRollsBack() throws Exception {
        Lecture lecture = lecture(10);
        when(lectureRepository.findById(10)).thenReturn(Optional.of(lecture));
        when(lectureMaterialRepository.save(any(LectureMaterial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LectureMaterialService service = service();
        TransactionSynchronizationManager.initSynchronization();

        service.upload(10, List.of(new MockMultipartFile(
                "files",
                "notes.txt",
                "text/plain",
                "lecture notes".getBytes()
        )));

        Path lectureDir = tempDir.resolve("lecture-10");
        try (var files = Files.list(lectureDir)) {
            assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(1);
        }

        completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

        try (var files = Files.list(lectureDir)) {
            assertThat(files.filter(Files::isRegularFile).count()).isZero();
        }
    }

    @Test
    void deleteRestoresFileWhenDatabaseTransactionRollsBack() throws Exception {
        Lecture lecture = lecture(11);
        LectureMaterial material = material(20, 11, "lecture-11/material.txt");
        when(lectureRepository.findById(11)).thenReturn(Optional.of(lecture));
        when(lectureMaterialRepository.findByIdAndCourseLectureId(20, 11)).thenReturn(Optional.of(material));
        Path original = tempDir.resolve(material.getStoredPath());
        Files.createDirectories(original.getParent());
        Files.writeString(original, "content");
        LectureMaterialService service = service();
        TransactionSynchronizationManager.initSynchronization();

        service.delete(11, 20);

        assertThat(original).doesNotExist();
        verify(lectureMaterialRepository).delete(material);

        completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(original).exists();
        assertThat(Files.readString(original)).isEqualTo("content");
    }

    @Test
    void deleteRemovesQuarantinedFileAfterCommit() throws Exception {
        Lecture lecture = lecture(12);
        LectureMaterial material = material(21, 12, "lecture-12/material.txt");
        when(lectureRepository.findById(12)).thenReturn(Optional.of(lecture));
        when(lectureMaterialRepository.findByIdAndCourseLectureId(21, 12)).thenReturn(Optional.of(material));
        Path original = tempDir.resolve(material.getStoredPath());
        Files.createDirectories(original.getParent());
        Files.writeString(original, "content");
        LectureMaterialService service = service();
        TransactionSynchronizationManager.initSynchronization();

        service.delete(12, 21);
        completeSynchronization(TransactionSynchronization.STATUS_COMMITTED);

        assertThat(original).doesNotExist();
        try (var files = Files.list(original.getParent())) {
            assertThat(files.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    private LectureMaterialService service() {
        LectureMaterialService service = new LectureMaterialService(
                lectureRepository,
                lectureMaterialRepository,
                new ActiveTeacherSubjectMembershipService(subjectMembershipRepository)
        );
        ReflectionTestUtils.setField(service, "lectureMaterialsDir", tempDir.toString());
        return service;
    }

    private static Lecture lecture(int id) {
        Lecture lecture = new Lecture();
        lecture.setId(id);
        lecture.setTitle("Lecture " + id);
        lecture.setContentFolderKey("lecture-" + id);
        return lecture;
    }

    private static LectureMaterial material(int id, int lectureId, String storedPath) {
        LectureMaterial material = new LectureMaterial();
        material.setId(id);
        material.setCourseLectureId(lectureId);
        material.setFileName("material.txt");
        material.setStoredPath(storedPath);
        material.setSizeBytes(7);
        return material;
    }

    private static void completeSynchronization(int status) {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
