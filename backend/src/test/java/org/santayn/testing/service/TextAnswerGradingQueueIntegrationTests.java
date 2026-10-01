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
import org.santayn.testing.repository.TextAnswerGradingJobRepository;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:text_answer_grading;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR",
        "app.text-answer-grading.worker.enabled=false",
        "app.text-answer-grading.worker.max-attempts=1"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TextAnswerGradingQueueIntegrationTests {

    @Autowired private PersonRepository personRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionResponseRepository questionResponseRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TextAnswerGradingJobRepository gradingJobRepository;
    @Autowired private TextAnswerGradingQueueService gradingQueueService;
    @Autowired private DefaultTextAnswerEvaluationService evaluationService;
    @Autowired private TestService testService;

    @Test
    void textAnswerIsQueuedThenAppliedOutsideSubmitTransaction() {
        Fixture fixture = fixture("async-success", "server validates deadline", BigDecimal.valueOf(2));

        QuestionResponse submitted = testService.submitResponse(
                fixture.attempt().getId(),
                fixture.question().getId(),
                "Server validates deadline",
                null
        );

        assertThat(submitted.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_PENDING);
        assertThat(submitted.getCorrect()).isNull();
        assertThat(submitted.getAwardedPoints()).isNull();
        assertThat(gradingJobRepository.findByQuestionResponseId(submitted.getId())).isPresent();

        TestAttempt completed = testService.completeAttempt(fixture.attempt().getId());
        assertThat(completed.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_PENDING);

        TextAnswerGradingQueueService.GradingTask task = gradingQueueService.claimNext().orElseThrow();
        gradingQueueService.complete(
                task,
                evaluationService.evaluate(task.questionText(), task.expectedAnswer(), task.actualAnswer())
        );

        QuestionResponse graded = questionResponseRepository.findById(submitted.getId()).orElseThrow();
        TestAttempt refreshedAttempt = testAttemptRepository.findById(fixture.attempt().getId()).orElseThrow();
        assertThat(graded.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_GRADED);
        assertThat(graded.getCorrect()).isTrue();
        assertThat(graded.getAwardedPoints()).isEqualByComparingTo("2.00");
        assertThat(refreshedAttempt.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_GRADED);
        assertThat(refreshedAttempt.getScore()).isEqualByComparingTo("2.00");
    }


    @Test
    void staleWorkerCannotCompleteOrFailAReclaimedJob() {
        Fixture fixture = fixture("async-lease", "expected answer", BigDecimal.ONE);

        QuestionResponse submitted = testService.submitResponse(
                fixture.attempt().getId(),
                fixture.question().getId(),
                "expected answer",
                null
        );
        testService.completeAttempt(fixture.attempt().getId());

        TextAnswerGradingQueueService.GradingTask firstLease = gradingQueueService.claimNext().orElseThrow();
        var claimedJob = gradingJobRepository.findByQuestionResponseId(submitted.getId()).orElseThrow();
        claimedJob.setUpdatedAtUtc(Instant.now().minus(10, ChronoUnit.MINUTES));
        gradingJobRepository.saveAndFlush(claimedJob);

        TextAnswerGradingQueueService.GradingTask secondLease = gradingQueueService.claimNext().orElseThrow();
        assertThat(secondLease.claimToken()).isNotEqualTo(firstLease.claimToken());

        gradingQueueService.fail(firstLease, new TextAnswerEvaluationUnavailableException("stale worker"));
        var afterStaleFailure = gradingJobRepository.findById(secondLease.jobId()).orElseThrow();
        assertThat(afterStaleFailure.getStatus()).isEqualTo(TextAnswerGradingQueueService.JOB_PROCESSING);
        assertThat(afterStaleFailure.getClaimToken()).isEqualTo(secondLease.claimToken());

        gradingQueueService.complete(
                secondLease,
                evaluationService.evaluate(
                        secondLease.questionText(),
                        secondLease.expectedAnswer(),
                        secondLease.actualAnswer()
                )
        );

        QuestionResponse graded = questionResponseRepository.findById(submitted.getId()).orElseThrow();
        var completedJob = gradingJobRepository.findById(secondLease.jobId()).orElseThrow();
        assertThat(graded.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_GRADED);
        assertThat(graded.getAwardedPoints()).isEqualByComparingTo("1.00");
        assertThat(completedJob.getStatus()).isEqualTo(TextAnswerGradingQueueService.JOB_COMPLETED);
        assertThat(completedJob.getClaimToken()).isNull();
    }

    @Test
    void exhaustedEvaluationFailureIsNotConvertedIntoIncorrectAnswer() {
        Fixture fixture = fixture("async-failure", "expected semantic answer", BigDecimal.ONE);

        QuestionResponse submitted = testService.submitResponse(
                fixture.attempt().getId(),
                fixture.question().getId(),
                "different wording",
                null
        );
        testService.completeAttempt(fixture.attempt().getId());

        TextAnswerGradingQueueService.GradingTask task = gradingQueueService.claimNext().orElseThrow();
        gradingQueueService.fail(task, new TextAnswerEvaluationUnavailableException("LLM unavailable"));

        QuestionResponse failed = questionResponseRepository.findById(submitted.getId()).orElseThrow();
        TestAttempt refreshedAttempt = testAttemptRepository.findById(fixture.attempt().getId()).orElseThrow();
        assertThat(failed.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_FAILED);
        assertThat(failed.getCorrect()).isNull();
        assertThat(failed.getAwardedPoints()).isNull();
        assertThat(failed.getGradingError()).contains("LLM unavailable");
        assertThat(refreshedAttempt.getGradingStatus()).isEqualTo(TextAnswerGradingQueueService.GRADING_FAILED);
    }

    private Fixture fixture(String suffix, String expectedAnswer, BigDecimal points) {
        Person person = new Person();
        person.setFirstName("Async");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(suffix + "-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Async grading " + System.nanoTime());
        test.setDuration(LocalTime.of(0, 30));
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        test = testRepository.saveAndFlush(test);

        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(test.getId());
        assignment.setScope(1);
        assignment.setAvailableFromUtc(Instant.now().minusSeconds(60));
        assignment.setAvailableUntilUtc(Instant.now().plusSeconds(3600));
        assignment.setStatus(2);
        assignment = testAssignmentRepository.saveAndFlush(assignment);

        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(assignment.getId());
        attempt.setPersonId(person.getId());
        attempt.setOrdinal(1);
        attempt.setStatus(TestService.ATTEMPT_STATUS_IN_PROGRESS);
        attempt.setStartedAt(Instant.now());
        attempt = testAttemptRepository.saveAndFlush(attempt);

        Question question = new Question();
        question.setTestId(test.getId());
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Explain the rule.");
        question.setCorrectAnswer(expectedAnswer);
        question.setPoints(points);
        question.setOrdinal(1);
        question.setActive(true);
        question = questionRepository.saveAndFlush(question);

        QuestionResponse response = new QuestionResponse();
        response.setTestAttemptId(attempt.getId());
        response.setTestQuestionId(question.getId());
        response.setGradingStatus(TextAnswerGradingQueueService.GRADING_GRADED);
        questionResponseRepository.saveAndFlush(response);

        return new Fixture(attempt, question);
    }

    private record Fixture(TestAttempt attempt, Question question) {
    }
}
