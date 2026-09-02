package org.santayn.testing.service;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.LectureRepository;
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
    @Mock private TestQuestionSelectionRuleRepository selectionRuleRepository;
    @Mock private TopicRepository topicRepository;

    @InjectMocks
    private TestService testService;

    @Test
    void attemptLimitIsSharedAcrossDifferentAssignmentsOfSameTest() {
        org.santayn.testing.models.test.Test test = test(5, 1);
        TestAssignment targetAssignment = activeAssignment(20, test.getId());
        TestAttempt previousAttempt = new TestAttempt();
        previousAttempt.setTestAssignmentId(19);
        previousAttempt.setPersonId(42);
        previousAttempt.setStatus(2);

        when(testAssignmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(targetAssignment));
        when(testRepository.findByIdForUpdate(5)).thenReturn(Optional.of(test));
        when(personRepository.existsById(42)).thenReturn(true);
        when(testAttemptRepository.findByTestIdAndPersonId(5, 42)).thenReturn(List.of(previousAttempt));

        assertThatThrownBy(() -> testService.startAttempt(20, 42, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attempt limit exceeded");
        verify(testAttemptRepository, never()).save(any());
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
