package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.LectureRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Autowired private LectureRepository lectureRepository;

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

        TestAssignment assignment = activeAssignment(test.getId());

        Integer personId = person.getId();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> first = executor.submit(() -> startAttempt(
                    startGate, assignment.getId(), personId
            ));
            Future<Object> second = executor.submit(() -> startAttempt(
                    startGate, assignment.getId(), personId
            ));
            startGate.countDown();

            List<Object> outcomes = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );

            assertThat(outcomes).filteredOn(TestAttempt.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(RuntimeException.class::isInstance).hasSize(1);
            assertThat(testAttemptRepository.countByTestAssignmentIdAndPersonId(assignment.getId(), personId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void deletingTestWithRecordedAttemptIsRejectedAndHistoryIsPreserved() {
        Person person = persistedPerson("delete-history");
        org.santayn.testing.models.test.Test test = persistedTest("Delete history test", 1);
        TestAssignment assignment = activeAssignment(test.getId());

        TestAttempt attempt = testService.startAttempt(assignment.getId(), person.getId(), null);

        assertThatThrownBy(() -> testService.delete(test.getId()))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("cannot be deleted");

        assertThat(testRepository.existsById(test.getId())).isTrue();
        assertThat(testAssignmentRepository.existsById(assignment.getId())).isTrue();
        assertThat(testAttemptRepository.existsById(attempt.getId())).isTrue();
    }

    @Test
    void assignmentTargetCannotChangeAfterAttemptExists() {
        Person person = persistedPerson("assignment-target");
        org.santayn.testing.models.test.Test test = persistedTest("Immutable assignment target", 2);
        TestAssignment assignment = activeAssignment(test.getId());
        testService.startAttempt(assignment.getId(), person.getId(), null);

        Lecture lecture = new Lecture();
        lecture.setOrdinal(1);
        lecture.setTitle("Target lecture");
        lecture.setContentFolderKey("target-" + System.nanoTime());
        lecture.setPublicVisible(true);
        lecture = lectureRepository.saveAndFlush(lecture);
        Integer lectureId = lecture.getId();

        assertThatThrownBy(() -> testService.updateAssignment(
                assignment.getId(),
                3,
                null,
                lectureId,
                null,
                assignment.getAvailableFromUtc(),
                assignment.getAvailableUntilUtc(),
                assignment.getStatus()
        ))
                .isInstanceOf(ResourceInUseException.class)
                .hasMessageContaining("target cannot be changed");

        TestAssignment unchanged = testAssignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(unchanged.getScope()).isEqualTo(1);
        assertThat(unchanged.getCourseLectureId()).isNull();
    }

    private Person persistedPerson(String prefix) {
        Person person = new Person();
        person.setFirstName("Integrity");
        person.setLastName("Student");
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(prefix + "-" + System.nanoTime() + "@test.local");
        person.setPhone("");
        return personRepository.saveAndFlush(person);
    }

    private org.santayn.testing.models.test.Test persistedTest(String title, int attemptsAllowed) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle(title);
        test.setAttemptsAllowed(attemptsAllowed);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
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
