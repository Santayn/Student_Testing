package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TopicRepository;
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
class TopicServiceIntegrationTests {

    @Autowired private PersonRepository personRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private TopicService topicService;

    @Test
    void createAllowsActiveTeacherMembership() {
        Subject subject = subject("active-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, null);

        Topic topic = topicService.create(
                subject.getId(),
                null,
                membership.getId(),
                1,
                "Active topic",
                null
        );

        assertThat(topic.getId()).isNotNull();
        assertThat(topic.getSubjectId()).isEqualTo(subject.getId());
        assertThat(topic.getSubjectMembershipId()).isEqualTo(membership.getId());
    }

    @Test
    void createRejectsInactiveTeacherMembership() {
        Subject subject = subject("inactive-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 2, null);

        assertThatThrownBy(() -> topicService.create(
                subject.getId(),
                null,
                membership.getId(),
                1,
                "Inactive topic",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsRemovedTeacherMembership() {
        Subject subject = subject("removed-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, Instant.now());

        assertThatThrownBy(() -> topicService.create(
                subject.getId(),
                null,
                membership.getId(),
                1,
                "Removed topic",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsNonTeacherMembership() {
        Subject subject = subject("student-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 2, 1, null);

        assertThatThrownBy(() -> topicService.create(
                subject.getId(),
                null,
                membership.getId(),
                1,
                "Student topic",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void updateRejectsInactiveTargetMembership() {
        Subject subject = subject("update-topic-subject");
        SubjectMembership activeMembership = subjectMembership(subject, 1, 1, null);
        SubjectMembership inactiveMembership = subjectMembership(subject, 1, 2, null);
        Topic topic = topicRepository.saveAndFlush(topic(subject, activeMembership));

        assertThatThrownBy(() -> topicService.update(
                topic.getId(),
                subject.getId(),
                null,
                inactiveMembership.getId(),
                2,
                "Moved topic",
                null
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
        person.setFirstName("Topic");
        person.setLastName("Teacher");
        person.setDateOfBirth(LocalDate.of(1980, 1, 1));
        person.setEmail("topic-" + System.nanoTime() + "@test.local");
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

    private static Topic topic(Subject subject, SubjectMembership membership) {
        Topic topic = new Topic();
        topic.setSubjectId(subject.getId());
        topic.setSubjectMembershipId(membership.getId());
        topic.setOrdinal(1);
        topic.setName("Existing topic");
        topic.setDescription("");
        return topic;
    }
}
