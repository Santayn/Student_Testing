package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionServiceIntegrationTests {

    @Autowired private LectureRepository lectureRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionService questionService;

    @Test
    void createAllowsActiveTopicMembership() {
        Subject subject = subject("active-question-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, null);
        Topic topic = topic(subject, membership, null, 1);

        Question question = questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Active topic question",
                BigDecimal.ONE,
                1,
                null,
                null
        );

        assertThat(question.getId()).isNotNull();
        assertThat(question.getTopicId()).isEqualTo(topic.getId());
    }

    @Test
    void createRejectsInactiveTopicMembership() {
        Subject subject = subject("inactive-question-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 2, null);
        Topic topic = topic(subject, membership, null, 1);

        assertThatThrownBy(() -> questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Inactive topic question",
                BigDecimal.ONE,
                1,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsRemovedTopicMembership() {
        Subject subject = subject("removed-question-topic-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 1, Instant.now());
        Topic topic = topic(subject, membership, null, 1);

        assertThatThrownBy(() -> questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Removed topic question",
                BigDecimal.ONE,
                1,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void createRejectsInactiveLectureMembership() {
        Subject subject = subject("inactive-question-lecture-subject");
        SubjectMembership membership = subjectMembership(subject, 1, 2, null);
        Lecture lecture = lecture(subject, membership, 1);
        org.santayn.testing.models.test.Test test = test("Inactive lecture test");

        assertThatThrownBy(() -> questionService.create(
                test.getId(),
                lecture.getId(),
                null,
                QuestionTypeSupport.TYPE_SINGLE,
                "Inactive lecture question",
                BigDecimal.ONE,
                1,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Active teacher subject membership is required");
    }

    @Test
    void updateRejectsInactiveTargetTopicMembership() {
        Subject activeSubject = subject("update-active-question-subject");
        SubjectMembership activeMembership = subjectMembership(activeSubject, 1, 1, null);
        Topic activeTopic = topic(activeSubject, activeMembership, null, 1);
        Question question = questionService.create(
                null,
                null,
                activeTopic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Existing question",
                BigDecimal.ONE,
                1,
                null,
                null
        );

        Subject inactiveSubject = subject("update-inactive-question-subject");
        SubjectMembership inactiveMembership = subjectMembership(inactiveSubject, 1, 2, null);
        Topic inactiveTopic = topic(inactiveSubject, inactiveMembership, null, 1);

        assertThatThrownBy(() -> questionService.update(
                question.getId(),
                null,
                inactiveTopic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Moved question",
                BigDecimal.ONE,
                1,
                null,
                null,
                true
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
        person.setFirstName("Question");
        person.setLastName("Teacher");
        person.setDateOfBirth(LocalDate.of(1980, 1, 1));
        person.setEmail("question-" + System.nanoTime() + "@test.local");
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

    private Topic topic(Subject subject, SubjectMembership membership, Lecture lecture, int ordinal) {
        Topic topic = new Topic();
        topic.setSubjectId(subject.getId());
        topic.setSubjectMembershipId(membership.getId());
        topic.setCourseLectureId(lecture == null ? null : lecture.getId());
        topic.setOrdinal(ordinal);
        topic.setName("Question topic " + System.nanoTime());
        topic.setDescription("");
        return topicRepository.saveAndFlush(topic);
    }

    private Lecture lecture(Subject subject, SubjectMembership membership, int ordinal) {
        Lecture lecture = new Lecture();
        lecture.setSubjectId(subject.getId());
        lecture.setSubjectMembershipId(membership.getId());
        lecture.setOrdinal(ordinal);
        lecture.setTitle("Question lecture " + System.nanoTime());
        lecture.setDescription("");
        lecture.setContentFolderKey("question-lecture-" + System.nanoTime());
        lecture.setPublicVisible(false);
        return lectureRepository.saveAndFlush(lecture);
    }

    private org.santayn.testing.models.test.Test test(String title) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle(title + " " + System.nanoTime());
        test.setDescription("");
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }
}
