package org.santayn.testing.service;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.LectureTestLinkRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.SelectedOptionRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestServiceAttemptLimitTests {

    @Mock private TestRepository testRepository;
    @Mock private TestAssignmentRepository testAssignmentRepository;
    @Mock private TestAttemptRepository testAttemptRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private QuestionResponseRepository questionResponseRepository;
    @Mock private SelectedOptionRepository selectedOptionRepository;
    @Mock private PersonRepository personRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock private TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository;
    @Mock private CourseVersionRepository courseVersionRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private LectureTestLinkRepository lectureTestLinkRepository;
    @Mock private TestQuestionSelectionRuleRepository selectionRuleRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private TextAnswerEvaluationService textAnswerEvaluationService;

    private TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService(
                testRepository,
                testAssignmentRepository,
                testAttemptRepository,
                questionRepository,
                questionOptionRepository,
                questionResponseRepository,
                selectedOptionRepository,
                personRepository,
                groupMembershipRepository,
                teachingAssignmentRepository,
                teachingAssignmentEnrollmentRepository,
                courseVersionRepository,
                lectureRepository,
                lectureTestLinkRepository,
                selectionRuleRepository,
                topicRepository,
                textAnswerEvaluationService,
                null
        );
    }

    @Test
    void attemptLimitDoesNotCountDifferentAssignmentsOfSameTest() {
        org.santayn.testing.models.test.Test test = test(5, 1);
        TestAssignment targetAssignment = activeAssignment(20, test.getId());
        TestAttempt previousAttempt = new TestAttempt();
        previousAttempt.setTestAssignmentId(19);
        previousAttempt.setPersonId(42);
        previousAttempt.setStatus(2);

        when(testAssignmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(targetAssignment));
        when(testRepository.findByIdForUpdate(5)).thenReturn(Optional.of(test));
        when(personRepository.existsById(42)).thenReturn(true);
        when(testAttemptRepository.findByTestAssignmentIdAndPersonId(20, 42)).thenReturn(List.of());
        when(testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt created = testService.startAttempt(20, 42, null);

        assertThat(created.getTestAssignmentId()).isEqualTo(20);
        assertThat(created.getPersonId()).isEqualTo(42);
        assertThat(created.getOrdinal()).isEqualTo(1);
        verify(testAttemptRepository).save(any(TestAttempt.class));
    }

    @Test
    void attemptLimitCountsOnlyTargetAssignment() {
        org.santayn.testing.models.test.Test test = test(5, 1);
        TestAssignment targetAssignment = activeAssignment(20, test.getId());
        TestAttempt previousAttempt = new TestAttempt();
        previousAttempt.setTestAssignmentId(20);
        previousAttempt.setPersonId(42);
        previousAttempt.setStatus(TestService.ATTEMPT_STATUS_COMPLETED);

        when(testAssignmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(targetAssignment));
        when(testRepository.findByIdForUpdate(5)).thenReturn(Optional.of(test));
        when(personRepository.existsById(42)).thenReturn(true);
        when(testAttemptRepository.findByTestAssignmentIdAndPersonId(20, 42)).thenReturn(List.of(previousAttempt));

        assertThatThrownBy(() -> testService.startAttempt(20, 42, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attempt limit exceeded for test assignment: 20");
        verify(testAttemptRepository, never()).save(any());
    }

    @Test
    void attemptsRemainingIgnoresInvalidatedAttempts() {
        org.santayn.testing.models.test.Test test = test(5, 2);
        TestAssignment assignment = activeAssignment(20, test.getId());
        when(testAssignmentRepository.findById(20)).thenReturn(Optional.of(assignment));
        when(testRepository.findById(5)).thenReturn(Optional.of(test));
        when(testAttemptRepository.countByTestAssignmentIdAndPersonIdAndStatusNot(
                20,
                42,
                TestService.ATTEMPT_STATUS_INVALIDATED
        )).thenReturn(1L);

        assertThat(testService.attemptsRemaining(20, 42)).isEqualTo(1);
    }

    @Test
    void invalidatedAttemptDoesNotConsumeAttemptLimitButKeepsOrdinalHistory() {
        org.santayn.testing.models.test.Test test = test(5, 1);
        TestAssignment targetAssignment = activeAssignment(20, test.getId());
        TestAttempt invalidatedAttempt = new TestAttempt();
        invalidatedAttempt.setTestAssignmentId(20);
        invalidatedAttempt.setPersonId(42);
        invalidatedAttempt.setOrdinal(1);
        invalidatedAttempt.setStatus(TestService.ATTEMPT_STATUS_INVALIDATED);

        when(testAssignmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(targetAssignment));
        when(testRepository.findByIdForUpdate(5)).thenReturn(Optional.of(test));
        when(personRepository.existsById(42)).thenReturn(true);
        when(testAttemptRepository.findByTestAssignmentIdAndPersonId(20, 42)).thenReturn(List.of(invalidatedAttempt));
        when(testAttemptRepository.save(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt created = testService.startAttempt(20, 42, null);

        assertThat(created.getStatus()).isEqualTo(TestService.ATTEMPT_STATUS_IN_PROGRESS);
        assertThat(created.getOrdinal()).isEqualTo(2);
    }

    @Test
    void invalidateAttemptStoresAuditFields() {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(99);
        attempt.setStatus(TestService.ATTEMPT_STATUS_COMPLETED);
        attempt.setCompletedAt(Instant.now().minusSeconds(30));
        when(testAttemptRepository.findById(99)).thenReturn(Optional.of(attempt));

        TestAttempt invalidated = testService.invalidateAttempt(99, "technical failure", "admin");

        assertThat(invalidated.getStatus()).isEqualTo(TestService.ATTEMPT_STATUS_INVALIDATED);
        assertThat(invalidated.getInvalidatedAtUtc()).isNotNull();
        assertThat(invalidated.getInvalidatedByLogin()).isEqualTo("admin");
        assertThat(invalidated.getInvalidationReason()).isEqualTo("technical failure");
    }

    @Test
    void parallelStartsAreProtectedByPessimisticAssignmentAndTestLocks() throws Exception {
        Lock assignmentLock = TestAssignmentRepository.class
                .getMethod("findByIdForUpdate", Integer.class)
                .getAnnotation(Lock.class);
        Lock testLock = TestRepository.class
                .getMethod("findByIdForUpdate", Integer.class)
                .getAnnotation(Lock.class);

        assertThat(assignmentLock).isNotNull();
        assertThat(assignmentLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(testLock).isNotNull();
        assertThat(testLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private static org.santayn.testing.models.test.Test test(int id, int attemptsAllowed) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setId(id);
        test.setAttemptsAllowed(attemptsAllowed);
        return test;
    }

    private static TestAssignment activeAssignment(int id, int testId) {
        TestAssignment assignment = new TestAssignment();
        assignment.setId(id);
        assignment.setTestId(testId);
        assignment.setStatus(2);
        assignment.setAvailableFromUtc(Instant.now().minusSeconds(60));
        assignment.setAvailableUntilUtc(Instant.now().plusSeconds(3600));
        return assignment;
    }
}
