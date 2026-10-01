package org.santayn.testing.web.controller.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.SelectedOptionRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.service.TestService;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultRestControllerScoreTests {

    @Mock private SubjectMembershipRepository subjectMembershipRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private CourseTemplateRepository courseTemplateRepository;
    @Mock private CourseVersionRepository courseVersionRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private TestAssignmentRepository testAssignmentRepository;
    @Mock private TestRepository testRepository;
    @Mock private LectureAssignmentRepository lectureAssignmentRepository;
    @Mock private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private PersonRepository personRepository;
    @Mock private TestAttemptRepository testAttemptRepository;
    @Mock private QuestionResponseRepository questionResponseRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private SelectedOptionRepository selectedOptionRepository;
    @Mock private LectureTestLinkService lectureTestLinkService;
    @Mock private TestService testService;
    @Mock private UserRegisterService userRegisterService;
    @Mock private Authentication authentication;

    @InjectMocks
    private ResultRestController controller;

    @Test
    void resultPercentUsesPersistedScoreAndQuestionPointsInsteadOfCorrectCount() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("student");
        when(userRegisterService.currentUser("student")).thenReturn(new UserRegisterService.CurrentUser(
                7,
                "student",
                true,
                42,
                "Student Example",
                null,
                Set.of("STUDENT"),
                Set.of()
        ));

        TestAttempt attempt = new TestAttempt();
        attempt.setId(1);
        attempt.setTestAssignmentId(100);
        attempt.setPersonId(42);
        attempt.setOrdinal(1);
        attempt.setStatus(TestService.ATTEMPT_STATUS_COMPLETED);
        attempt.setCompletedAt(Instant.parse("2026-09-30T12:00:00Z"));
        attempt.setScore(new BigDecimal("1.50"));

        TestAssignment assignment = new TestAssignment();
        assignment.setId(100);
        assignment.setTestId(77);

        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setId(77);
        test.setTitle("Weighted test");

        Person person = new Person();
        person.setId(42);
        person.setFirstName("Student");
        person.setLastName("Example");
        attempt.setTestAssignment(assignment);
        attempt.setPerson(person);

        Question firstQuestion = textQuestion(11L, "Question 1", "1.00");
        Question secondQuestion = textQuestion(12L, "Question 2", "3.00");

        QuestionResponse firstResponse = response(101L, 1, 11L, true, "1.00");
        firstResponse.setTestQuestion(firstQuestion);
        QuestionResponse secondResponse = response(102L, 1, 12L, false, "0.50");
        secondResponse.setTestQuestion(secondQuestion);

        when(testAttemptRepository.findResultAttempts(
                any(),
                eq(true),
                any(),
                eq(false),
                any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(attempt)));
        when(questionResponseRepository.findByTestAttemptIdIn(List.of(1)))
                .thenReturn(List.of(firstResponse, secondResponse));
        when(testRepository.findAllById(any())).thenReturn(List.of(test));
        when(personRepository.findById(42)).thenReturn(Optional.of(person));

        ResultRestController.ResultDataResponse result = controller.studentData(null, null, null, null, authentication);

        assertThat(result.attempts()).hasSize(1);
        ResultRestController.ResultAttemptResponse resultAttempt = result.attempts().get(0);
        assertThat(resultAttempt.score()).isEqualByComparingTo("1.50");
        assertThat(resultAttempt.maxScore()).isEqualByComparingTo("4.00");
        assertThat(resultAttempt.scorePercent()).isEqualByComparingTo("37.50");
        assertThat(resultAttempt.stats().right()).isEqualTo(1);
        assertThat(resultAttempt.stats().percent()).isEqualByComparingTo("37.50");
        assertThat(result.stats().score()).isEqualByComparingTo("1.50");
        assertThat(result.stats().maxScore()).isEqualByComparingTo("4.00");
        assertThat(result.stats().scorePercent()).isEqualByComparingTo("37.50");
        assertThat(result.totalAttemptCount()).isEqualTo(1);
        assertThat(result.page()).isNull();
        assertThat(result.pageSize()).isNull();
        verify(testAttemptRepository, never()).findAll();
    }

    private static Question textQuestion(long id, String text, String points) {
        Question question = new Question();
        question.setId(id);
        question.setType(QuestionTypeSupport.TYPE_TEXT);
        question.setQuestion(text);
        question.setPoints(new BigDecimal(points));
        return question;
    }

    private static QuestionResponse response(long id, int attemptId, long questionId, boolean correct, String points) {
        QuestionResponse response = new QuestionResponse();
        response.setId(id);
        response.setTestAttemptId(attemptId);
        response.setTestQuestionId(questionId);
        response.setAnswerText("answer");
        response.setCorrect(correct);
        response.setAwardedPoints(new BigDecimal(points));
        return response;
    }
}
