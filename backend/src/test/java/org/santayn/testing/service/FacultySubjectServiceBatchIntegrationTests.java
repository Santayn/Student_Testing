package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.subject.FacultySubject;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.repository.FacultyRepository;
import org.santayn.testing.repository.FacultySubjectRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FacultySubjectServiceBatchIntegrationTests {

    @Autowired private FacultySubjectService facultySubjectService;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private FacultySubjectRepository facultySubjectRepository;

    @Test
    void replacesFacultySubjectsAsOneDesiredSet() {
        Faculty faculty = createFaculty("batch-success");
        Subject first = createSubject("Batch first");
        Subject second = createSubject("Batch second");
        Subject third = createSubject("Batch third");
        createLink(faculty.getId(), first.getId());

        var response = facultySubjectService.replaceSubjects(
                faculty.getId(),
                Set.of(second.getId(), third.getId())
        );

        assertThat(response).extracting(Subject::getId)
                .containsExactlyInAnyOrder(second.getId(), third.getId());
        assertThat(facultySubjectRepository.findByFacultyId(faculty.getId()))
                .extracting(FacultySubject::getSubjectId)
                .containsExactlyInAnyOrder(second.getId(), third.getId());
    }

    @Test
    void validationFailureLeavesExistingFacultySubjectsUntouched() {
        Faculty faculty = createFaculty("batch-rollback");
        Subject first = createSubject("Rollback first");
        Subject second = createSubject("Rollback second");
        createLink(faculty.getId(), first.getId());

        assertThatThrownBy(() -> facultySubjectService.replaceSubjects(
                faculty.getId(),
                Set.of(second.getId(), Integer.MAX_VALUE)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subjects not found");

        assertThat(facultySubjectRepository.findByFacultyId(faculty.getId()))
                .extracting(FacultySubject::getSubjectId)
                .containsExactly(first.getId());
    }

    private Faculty createFaculty(String suffix) {
        Faculty faculty = new Faculty();
        faculty.setName("Faculty " + suffix);
        faculty.setCode("FAC-" + suffix + "-" + System.nanoTime());
        return facultyRepository.saveAndFlush(faculty);
    }

    private Subject createSubject(String prefix) {
        Subject subject = new Subject();
        subject.setName(prefix + " " + System.nanoTime());
        return subjectRepository.saveAndFlush(subject);
    }

    private void createLink(Integer facultyId, Integer subjectId) {
        FacultySubject link = new FacultySubject();
        link.setFacultyId(facultyId);
        link.setSubjectId(subjectId);
        facultySubjectRepository.saveAndFlush(link);
    }
}
