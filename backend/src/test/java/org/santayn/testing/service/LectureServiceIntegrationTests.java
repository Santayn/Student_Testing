package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LectureServiceIntegrationTests {

    @Autowired private PersonRepository personRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private LectureService lectureService;

    @Test
    void createAllowsActiveTeacherMembership() {
        Subject subject = subject("active-lecture-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, null);

        Lecture lecture = lectureService.create(
                subject.getId(),
                membership.getId(),
                null,
                1,
                "Active lecture",
                null,
                "active-lecture-folder",
                null,
                false
        );

        assertThat(lecture.getId()).isNotNull();
        assertThat(lecture.getSubjectId()).isEqualTo(subject.getId());
        assertThat(lecture.getSubjectMembershipId()).isEqualTo(membership.getId());
    }

    @Test
    void createRejectsInactiveTeacherMembership() {
        Subject subject = subject("inactive-lecture-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 2, null);

        assertThatThrownBy(() -> lectureService.create(
                subject.getId(),
                membership.getId(),
                null,
                1,
                "Inactive lecture",
                null,
                "inactive-lecture-folder",
                null,
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsRemovedTeacherMembership() {
        Subject subject = subject("removed-lecture-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, Instant.now());

        assertThatThrownBy(() -> lectureService.create(
                subject.getId(),
                membership.getId(),
                null,
                1,
                "Removed lecture",
                null,
                "removed-lecture-folder",
                null,
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsNonTeacherMembership() {
        Subject subject = subject("student-lecture-subject");
        SubjectMembership membership = subjectMembership(subject, 2, 1, null);

        assertThatThrownBy(() -> lectureService.create(
                subject.getId(),
                membership.getId(),
                null,
                1,
                "Student lecture",
                null,
                "student-lecture-folder",
                null,
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void updateRejectsInactiveTargetMembership() {
        Subject activeSubject = subject("update-active-lecture-subject");
        SubjectMembership activeMembership = subjectMembership(activeSubject, 1, 1, null);
        Lecture lecture = lectureService.create(
                activeSubject.getId(),
                activeMembership.getId(),
                null,
                1,
                "Existing lecture",
                null,
                "existing-lecture-folder",
                null,
                false
        );

        Subject inactiveSubject = subject("update-inactive-lecture-subject");
        SubjectMembership inactiveMembership = subjectMembership(inactiveSubject, 1, 2, null);

        assertThatThrownBy(() -> lectureService.update(
                lecture.getId(),
                inactiveSubject.getId(),
                inactiveMembership.getId(),
                null,
                1,
                "Moved lecture",
                null,
                "moved-lecture-folder",
                null,
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    private Subject subject(String suffix) {
        Subject subject = new Subject();
        subject.setName("Subject " + suffix + " " + System.nanoTime());
        subject.setDescription("");
        return subjectRepository.saveAndFlush(subject);
    }

    private SubjectMembership subjectMembership(Subject subject, int role, int status, Instant removedAtUtc) {
        Person person = new Person();
        person.setFirstName("Lecture");
        person.setLastName("Teacher");
        person.setDateOfBirth(LocalDate.of(1980, 1, 1));
        person.setEmail("lecture-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(person.getId());
        membership.setRole(role);
        membership.setStatus(status);
        membership.setAssignedAtUtc(Instant.now());
        membership.setRemovedAtUtc(removedAtUtc);
        return subjectMembershipRepository.saveAndFlush(membership);
    }
}
