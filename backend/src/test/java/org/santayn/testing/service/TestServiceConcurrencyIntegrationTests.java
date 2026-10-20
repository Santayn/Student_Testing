package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:student_test_concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestServiceConcurrencyIntegrationTests {

    @Autowired private TestService testService;
    @Autowired private PersonRepository personRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private org.santayn.testing.repository.QuestionRepository questionRepository;

    @Test
    void parallelStartCannotExceedAttemptLimit() throws Exception {
        Person person = new Person();
        person.setFirstName("Parallel");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail("parallel-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Parallel start test");
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        test = testRepository.saveAndFlush(test);

        TestAssignment firstAssignment = activeAssignment(test.getId());
        TestAssignment secondAssignment = activeAssignment(test.getId());

        Integer personId = person.getId();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> startAttempt(
                    startGate, firstAssignment.getId(), personId
            ));
            Future<Object> second = executor.submit(() -> startAttempt(
                    startGate, secondAssignment.getId(), personId
            ));
            startGate.countDown();

            List<Object> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertThat(outcomes).filteredOn(TestAttempt.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(RuntimeException.class::isInstance).hasSize(1);
            assertThat(testAttemptRepository.countByTestIdAndPersonId(test.getId(), personId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void deletesTestThatStudentsAlreadyStarted() {
        Person person = new Person();
        person.setFirstName("Deleted");
        person.setLastName("Test");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail("deleted-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        person = personRepository.saveAndFlush(person);

        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle("Test with attempts");
        test.setAttemptsAllowed(2);
        test.setQuestionCount(1);
        test = testRepository.saveAndFlush(test);
        org.santayn.testing.models.question.Question question = new org.santayn.testing.models.question.Question();
        question.setTestId(test.getId());
        question.setType(org.santayn.testing.models.question.QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion("Вопрос");
        question.setCorrectAnswer("ответ");
        question = questionRepository.saveAndFlush(question);
        TestAssignment assignment = activeAssignment(test.getId());
        TestAttempt attempt = testService.startOrResumeAttemptWithRandomQuestions(assignment.getId(), person.getId(), null)
                .attempt();
        testService.submitResponse(attempt.getId(), question.getId(), "ответ", List.of());

        testService.delete(test.getId());

        assertThat(testRepository.existsById(test.getId())).isFalse();
        assertThat(testAssignmentRepository.findByTestId(test.getId())).isEmpty();
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

    private Object startAttempt(CountDownLatch startGate, Integer assignmentId, Integer personId) {
        try {
            startGate.await(10, TimeUnit.SECONDS);
            return testService.startAttempt(assignmentId, personId, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new IllegalStateException("Parallel start was interrupted", exception);
        } catch (RuntimeException exception) {
            return exception;
        }
    }
}
