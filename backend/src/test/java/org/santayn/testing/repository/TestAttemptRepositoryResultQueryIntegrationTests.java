package org.santayn.testing.repository;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestAttemptRepositoryResultQueryIntegrationTests {

    @Autowired private PersonRepository personRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;

    @Test
    void scopesResultAttemptsInDatabaseAndPaginatesThem() {
        Person firstStudent = personRepository.save(person("First", "Student", "first-results@example.test"));
        Person secondStudent = personRepository.save(person("Second", "Student", "second-results@example.test"));

        org.santayn.testing.models.test.Test firstTest = testRepository.save(test("First result test"));
        org.santayn.testing.models.test.Test secondTest = testRepository.save(test("Second result test"));
        TestAssignment firstAssignment = testAssignmentRepository.save(assignment(firstTest.getId()));
        TestAssignment secondAssignment = testAssignmentRepository.save(assignment(secondTest.getId()));

        TestAttempt matching = testAttemptRepository.save(attempt(
                firstAssignment.getId(),
                firstStudent.getId(),
                TestService.ATTEMPT_STATUS_COMPLETED
        ));
        testAttemptRepository.save(attempt(
                secondAssignment.getId(),
                firstStudent.getId(),
                TestService.ATTEMPT_STATUS_EXPIRED
        ));
        testAttemptRepository.save(attempt(
                firstAssignment.getId(),
                secondStudent.getId(),
                TestService.ATTEMPT_STATUS_COMPLETED
        ));
        testAttemptRepository.save(attempt(
                firstAssignment.getId(),
                firstStudent.getId(),
                TestService.ATTEMPT_STATUS_IN_PROGRESS
        ));
        testAttemptRepository.flush();

        Page<TestAttempt> scoped = testAttemptRepository.findResultAttempts(
                List.of(TestService.ATTEMPT_STATUS_IN_PROGRESS, TestService.ATTEMPT_STATUS_INVALIDATED),
                true,
                List.of(firstStudent.getId()),
                true,
                List.of(firstTest.getId()),
                PageRequest.of(0, 10)
        );

        assertThat(scoped.getTotalElements()).isEqualTo(1);
        assertThat(scoped.getContent()).extracting(TestAttempt::getId).containsExactly(matching.getId());
        assertThat(scoped.getContent().get(0).getTestAssignment().getTestId()).isEqualTo(firstTest.getId());
        assertThat(scoped.getContent().get(0).getPerson().getId()).isEqualTo(firstStudent.getId());

        Page<TestAttempt> paged = testAttemptRepository.findResultAttempts(
                List.of(TestService.ATTEMPT_STATUS_IN_PROGRESS, TestService.ATTEMPT_STATUS_INVALIDATED),
                false,
                List.of(-1),
                false,
                List.of(-1),
                PageRequest.of(0, 1)
        );

        assertThat(paged.getContent()).hasSize(1);
        assertThat(paged.getTotalElements()).isEqualTo(3);
        assertThat(paged.getTotalPages()).isEqualTo(3);
    }

    private static Person person(String firstName, String lastName, String email) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setDateOfBirth(LocalDate.of(2000, 1, 1));
        person.setEmail(email);
        person.setPhone("");
        return person;
    }

    private static org.santayn.testing.models.test.Test test(String title) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle(title);
        test.setAttemptsAllowed(3);
        test.setQuestionCount(1);
        return test;
    }

    private static TestAssignment assignment(Integer testId) {
        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(testId);
        assignment.setScope(1);
        assignment.setAvailableFromUtc(Instant.parse("2026-09-01T00:00:00Z"));
        assignment.setAvailableUntilUtc(Instant.parse("2026-10-31T00:00:00Z"));
        assignment.setStatus(1);
        return assignment;
    }

    private static TestAttempt attempt(Integer assignmentId, Integer personId, int status) {
        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(assignmentId);
        attempt.setPersonId(personId);
        attempt.setOrdinal(1);
        attempt.setStatus(status);
        attempt.setStartedAt(Instant.parse("2026-09-30T10:00:00Z"));
        if (status != TestService.ATTEMPT_STATUS_IN_PROGRESS) {
            attempt.setCompletedAt(Instant.parse("2026-09-30T10:10:00Z"));
        }
        return attempt;
    }
}
