package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LectureContentServiceIntegrationTests {

    @Autowired private LectureRepository lectureRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private LectureMaterialService lectureMaterialService;
    @Autowired private LectureTestLinkService lectureTestLinkService;

    @Test
    void replaceLectureTestsRejectsInactiveLectureMembership() {
        Lecture lecture = lectureWithMembership(1, 2, null);

        assertThatThrownBy(() -> lectureTestLinkService.replaceLectureTests(lecture.getId(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void uploadMaterialsRejectsInactiveLectureMembership() {
        Lecture lecture = lectureWithMembership(1, 2, null);

        assertThatThrownBy(() -> lectureMaterialService.upload(lecture.getId(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void deleteMaterialRejectsRemovedLectureMembership() {
        Lecture lecture = lectureWithMembership(1, 1, Instant.now());

        assertThatThrownBy(() -> lectureMaterialService.delete(lecture.getId(), 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    private Lecture lectureWithMembership(int role, int status, Instant removedAtUtc) {
        Subject subject = new Subject();
        subject.setName("Lecture content subject " + System.nanoTime());
        subject.setDescription("");
        subject = subjectRepository.saveAndFlush(subject);

        Person person = new Person();
        person.setFirstName("Lecture");
        person.setLastName("Content");
        person.setDateOfBirth(LocalDate.of(1980, 1, 1));
        person.setEmail("lecture-content-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(person.getId());
        membership.setRole(role);
        membership.setStatus(status);
        membership.setAssignedAtUtc(Instant.now());
        membership.setRemovedAtUtc(removedAtUtc);
        membership = subjectMembershipRepository.saveAndFlush(membership);

        Lecture lecture = new Lecture();
        lecture.setSubjectId(subject.getId());
        lecture.setSubjectMembershipId(membership.getId());
        lecture.setOrdinal(1);
        lecture.setTitle("Lecture content " + System.nanoTime());
        lecture.setDescription("");
        lecture.setContentFolderKey("lecture-content-" + System.nanoTime());
        lecture.setPublicVisible(false);
        return lectureRepository.saveAndFlush(lecture);
    }
}
