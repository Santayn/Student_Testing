package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assessment_definition_immutability;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AssessmentDefinitionImmutabilityIntegrationTests {

    @Autowired private TestService testService;
    @Autowired private QuestionService questionService;
    @Autowired private PersonRepository personRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMembershipRepository subjectMembershipRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository questionOptionRepository;
    @Autowired private QuestionResponseRepository questionResponseRepository;

    @Test
    void testDefinitionAndUsedQuestionBecomeImmutableAfterFirstAttempt() {
        Person person = persistedPerson();
        org.santayn.testing.models.test.Test test = persistedTest();
        TestAssignment assignment = activeAssignment(test.getId());

        Question question = questionService.create(
                test.getId(),
                null,
                null,
                QuestionTypeSupport.TYPE_SINGLE,
                "Original question",
                BigDecimal.ONE,
                1,
                null,
                List.of()
        );
        QuestionOption correctOption = questionService.addOption(question.getId(), "Correct", 1, true);
        questionService.addOption(question.getId(), "Wrong", 2, false);

        TestService.TestAttemptQuestionSet started = testService.startAttemptWithRandomQuestions(
                assignment.getId(),
                person.getId(),
                null
        );

        assertThat(started.questions()).extracting(Question::getId).containsExactly(question.getId());
        assertThat(questionResponseRepository.existsByTestQuestionId(question.getId())).isTrue();

        assertThatThrownBy(() -> testService.update(
                test.getId(),
                "Changed title",
                "Changed description",
                LocalTime.of(0, 30),
                2,
                1
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> testService.replaceSelectionRules(test.getId(), List.of()))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> questionService.update(
                question.getId(),
                null,
                null,
                QuestionTypeSupport.TYPE_SINGLE,
                "Rewritten historical question",
                BigDecimal.TEN,
                1,
                null,
                List.of(),
                true
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> questionService.updateOption(correctOption.getId(), "Changed option", 1, true))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> questionService.addOption(question.getId(), "New option", 3, false))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> questionService.setActive(question.getId(), false))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        assertThatThrownBy(() -> questionService.create(
                test.getId(),
                null,
                null,
                QuestionTypeSupport.TYPE_SINGLE,
                "Late question",
                BigDecimal.ONE,
                2,
                null,
                List.of()
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be changed");

        Question unchangedQuestion = questionRepository.findById(question.getId()).orElseThrow();
        QuestionOption unchangedOption = questionOptionRepository.findById(correctOption.getId()).orElseThrow();
        org.santayn.testing.models.test.Test unchangedTest = testRepository.findById(test.getId()).orElseThrow();

        assertThat(unchangedQuestion.getQuestion()).isEqualTo("Original question");
        assertThat(unchangedQuestion.getPoints()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(unchangedOption.getText()).isEqualTo("Correct");
        assertThat(unchangedTest.getTitle()).isEqualTo("Immutable assessment");
        assertThat(unchangedTest.getAttemptsAllowed()).isEqualTo(2);
    }

    @Test
    void referencedTopicQuestionBankFreezesAsWholeAfterFirstAttempt() {
        Person person = persistedPerson();
        org.santayn.testing.models.test.Test test = persistedTest();
        TestAssignment assignment = activeAssignment(test.getId());
        Subject subject = persistedSubject();
        SubjectMembership membership = persistedTeacherMembership(subject);
        Topic topic = persistedTopic(subject, membership);

        Question first = questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Pool question A",
                BigDecimal.ONE,
                1,
                null,
                List.of()
        );
        questionService.addOption(first.getId(), "A correct", 1, true);
        questionService.addOption(first.getId(), "A wrong", 2, false);

        Question second = questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Pool question B",
                BigDecimal.ONE,
                2,
                null,
                List.of()
        );
        questionService.addOption(second.getId(), "B correct", 1, true);
        questionService.addOption(second.getId(), "B wrong", 2, false);

        testService.replaceSelectionRules(test.getId(), List.of(
                new TestService.SelectionRuleInput(
                        null,
                        topic.getId(),
                        1,
                        0,
                        1,
                        0,
                        0,
                        1
                )
        ));

        Question allowedBeforeFirstAttempt = questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Pool question C",
                BigDecimal.ONE,
                3,
                null,
                List.of()
        );
        questionService.addOption(allowedBeforeFirstAttempt.getId(), "C correct", 1, true);
        questionService.addOption(allowedBeforeFirstAttempt.getId(), "C wrong", 2, false);

        TestService.TestAttemptQuestionSet started = testService.startAttemptWithRandomQuestions(
                assignment.getId(),
                person.getId(),
                null
        );

        assertThat(started.questions()).hasSize(1);
        Long selectedQuestionId = started.questions().get(0).getId();
        Question neverSelectedQuestion = List.of(first, second, allowedBeforeFirstAttempt).stream()
                .filter(question -> !question.getId().equals(selectedQuestionId))
                .findFirst()
                .orElseThrow();

        assertThat(questionResponseRepository.existsByTestQuestionId(selectedQuestionId)).isTrue();
        assertThat(questionResponseRepository.existsByTestQuestionId(neverSelectedQuestion.getId())).isFalse();

        assertThatThrownBy(() -> questionService.update(
                neverSelectedQuestion.getId(),
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Rewritten question that was never selected",
                BigDecimal.TEN,
                neverSelectedQuestion.getOrdinal(),
                null,
                List.of(),
                true
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("question bank cannot be changed");

        assertThatThrownBy(() -> questionService.addOption(neverSelectedQuestion.getId(), "Late option", 99, false))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("question bank cannot be changed");

        assertThatThrownBy(() -> questionService.setActive(neverSelectedQuestion.getId(), false))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("question bank cannot be changed");

        assertThatThrownBy(() -> questionService.create(
                null,
                null,
                topic.getId(),
                QuestionTypeSupport.TYPE_SINGLE,
                "Late pool question",
                BigDecimal.ONE,
                4,
                null,
                List.of()
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("question bank cannot be changed");

        Question unchanged = questionRepository.findById(neverSelectedQuestion.getId()).orElseThrow();
        assertThat(unchanged.getQuestion()).doesNotContain("Rewritten");
        assertThat(unchanged.isActive()).isTrue();
        assertThat(questionRepository.countByTopicIdAndTestIdIsNull(topic.getId())).isEqualTo(3);
    }

    private Person persistedPerson() {
        Person person = new Person();
        person.setFirstName("History");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail("history-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        return personRepository.saveAndFlush(person);
    }

    private org.santayn.testing.models.test.Test persistedTest() {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Immutable assessment");
        test.setAttemptsAllowed(2);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }

    private Subject persistedSubject() {
        Subject subject = new Subject();
        subject.setName("Immutable topic subject " + System.nanoTime());
        subject.setDescription("");
        return subjectRepository.saveAndFlush(subject);
    }

    private SubjectMembership persistedTeacherMembership(Subject subject) {
        Person teacher = new Person();
        teacher.setFirstName("History");
        teacher.setLastName("Teacher");
        teacher.setDateOfBirth(LocalDate.of(1980, 1, 1));
        teacher.setEmail("history-teacher-" + System.nanoTime() + "@test.local");
        teacher.setPhone("");
        teacher = personRepository.saveAndFlush(teacher);

        SubjectMembership membership = new SubjectMembership();
        membership.setSubjectId(subject.getId());
        membership.setPersonId(teacher.getId());
        membership.setRole(1);
        membership.setStatus(1);
        membership.setAssignedAtUtc(Instant.now());
        membership.setRemovedAtUtc(null);
        return subjectMembershipRepository.saveAndFlush(membership);
    }

    private Topic persistedTopic(Subject subject, SubjectMembership membership) {
        Topic topic = new Topic();
        topic.setSubjectId(subject.getId());
        topic.setSubjectMembershipId(membership.getId());
        topic.setCourseLectureId(null);
        topic.setOrdinal(1);
        topic.setName("Immutable topic bank " + System.nanoTime());
        topic.setDescription("");
        return topicRepository.saveAndFlush(topic);
    }

    private TestAssignment activeAssignment(Integer testId) {
        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(testId);
        assignment.setScope(1);
        assignment.setAvailableFromUtc(Instant.now().minusSeconds(60));
        assignment.setAvailableUntilUtc(Instant.now().plusSeconds(3600));
        assignment.setStatus(2);
        return testAssignmentRepository.saveAndFlush(assignment);
    }
}
