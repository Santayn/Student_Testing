package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:test_attempt_deadlines;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestServiceDeadlineIntegrationTests {

    @Autowired private PersonRepository personRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionResponseRepository questionResponseRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestService testService;

    @Test
    void submitExpiresAttemptWhenTestDurationElapsed() {
        Person person = person("duration-submit");
        org.santayn.testing.models.test.Test test = test("Duration deadline", LocalTime.of(0, 0, 1));
        TestAssignment assignment = assignment(test, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        TestAttempt attempt = attempt(assignment, person, Instant.now().minusSeconds(5));
        Question question = question(test);
        QuestionResponse response = response(attempt, question, BigDecimal.valueOf(0.25), "original");

        assertThatThrownBy(() -> testService.submitResponse(attempt.getId(), question.getId(), "late", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deadline has expired");

        TestAttempt expired = testAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(TestService.ATTEMPT_STATUS_EXPIRED);
        assertThat(expired.getCompletedAt()).isCloseTo(attempt.getStartedAt().plusSeconds(1), within(1, ChronoUnit.MICROS));
        assertThat(expired.getScore()).isEqualByComparingTo("0.25");
        assertThat(questionResponseRepository.findById(response.getId()).orElseThrow().getAnswerText())
                .isEqualTo("original");
    }

    @Test
    void completeExpiresAttemptWhenAssignmentDeadlinePassed() {
        Person person = person("assignment-complete");
        org.santayn.testing.models.test.Test test = test("Assignment deadline", LocalTime.of(0, 10));
        Instant assignmentDeadline = Instant.now().minusSeconds(2);
        TestAssignment assignment = assignment(test, Instant.now().minusSeconds(3600), assignmentDeadline);
        TestAttempt attempt = attempt(assignment, person, Instant.now().minusSeconds(30));
        Question question = question(test);
        response(attempt, question, BigDecimal.valueOf(0.50), "answer");

        assertThatThrownBy(() -> testService.completeAttempt(attempt.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deadline has expired");

        TestAttempt expired = testAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(TestService.ATTEMPT_STATUS_EXPIRED);
        assertThat(expired.getCompletedAt()).isCloseTo(assignmentDeadline, within(1, ChronoUnit.MICROS));
        assertThat(expired.getScore()).isEqualByComparingTo("0.50");
    }

    @Test
    void effectiveDeadlineUsesEarlierAssignmentOrDurationLimit() {
        Person person = person("effective-deadline");
        Instant now = Instant.now();
        org.santayn.testing.models.test.Test test = test("Effective deadline", LocalTime.of(0, 5));
        TestAssignment assignment = assignment(test, now.minusSeconds(60), now.plusSeconds(120));
        TestAttempt attempt = attempt(assignment, person, now.minusSeconds(30));

        assertThat(testService.effectiveDeadlineUtc(attempt))
                .isCloseTo(assignment.getAvailableUntilUtc(), within(1, ChronoUnit.MICROS));

        assignment.setAvailableUntilUtc(now.plusSeconds(600));
        testAssignmentRepository.saveAndFlush(assignment);

        assertThat(testService.effectiveDeadlineUtc(attempt))
                .isCloseTo(attempt.getStartedAt().plusSeconds(300), within(1, ChronoUnit.MICROS));
    }

    private Person person(String suffix) {
        Person person = new Person();
        person.setFirstName("Deadline");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(suffix + "-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        return personRepository.saveAndFlush(person);
    }

    private org.santayn.testing.models.test.Test test(String title, LocalTime duration) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle(title + " " + System.nanoTime());
        test.setDuration(duration);
        test.setAttemptsAllowed(2);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }

    private TestAssignment assignment(org.santayn.testing.models.test.Test test,
                                      Instant availableFromUtc,
                                      Instant availableUntilUtc) {
        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(test.getId());
        assignment.setScope(1);
        assignment.setAvailableFromUtc(availableFromUtc);
        assignment.setAvailableUntilUtc(availableUntilUtc);
        assignment.setStatus(2);
        return testAssignmentRepository.saveAndFlush(assignment);
    }

    private TestAttempt attempt(TestAssignment assignment, Person person, Instant startedAtUtc) {
        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(assignment.getId());
        attempt.setPersonId(person.getId());
        attempt.setOrdinal(1);
        attempt.setStatus(TestService.ATTEMPT_STATUS_IN_PROGRESS);
        attempt.setStartedAt(startedAtUtc);
        return testAttemptRepository.saveAndFlush(attempt);
    }

    private Question question(org.santayn.testing.models.test.Test test) {
        Question question = new Question();
        question.setTestId(test.getId());
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Explain deadline handling.");
        question.setCorrectAnswer("Server checks deadlines.");
        question.setPoints(BigDecimal.ONE);
        question.setOrdinal(1);
        question.setActive(true);
        return questionRepository.saveAndFlush(question);
    }

    private QuestionResponse response(TestAttempt attempt, Question question, BigDecimal points, String answerText) {
        QuestionResponse response = new QuestionResponse();
        response.setTestAttemptId(attempt.getId());
        response.setTestQuestionId(question.getId());
        response.setAnswerText(answerText);
        response.setCorrect(points.compareTo(BigDecimal.ZERO) > 0);
        response.setAwardedPoints(points);
        return questionResponseRepository.saveAndFlush(response);
    }
}
