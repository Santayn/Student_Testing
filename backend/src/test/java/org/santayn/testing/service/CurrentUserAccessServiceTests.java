package org.santayn.testing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.SubjectMembershipLoadTypeRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserAccessServiceTests {

    @Mock private SubjectMembershipRepository subjectMembershipRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private LectureRepository lectureRepository;
    @Mock private CourseVersionRepository courseVersionRepository;
    @Mock private CourseTemplateRepository courseTemplateRepository;
    @Mock private TestRepository testRepository;
    @Mock private TestQuestionSelectionRuleRepository selectionRuleRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock private TeachingAssignmentEnrollmentRepository enrollmentRepository;
    @Mock private LectureAssignmentRepository lectureAssignmentRepository;
    @Mock private SubjectMembershipLoadTypeRepository subjectMembershipLoadTypeRepository;

    private StubUserRegisterService userRegisterService;
    private CurrentUserAccessService accessService;
    private Authentication authentication;

    @BeforeEach
    void currentTeacher() {
        userRegisterService = new StubUserRegisterService();
        accessService = new CurrentUserAccessService(
                userRegisterService,
                subjectMembershipRepository,
                topicRepository,
                questionRepository,
                questionOptionRepository,
                lectureRepository,
                courseVersionRepository,
                courseTemplateRepository,
                testRepository,
                selectionRuleRepository,
                groupMembershipRepository,
                teachingAssignmentRepository,
                enrollmentRepository,
                lectureAssignmentRepository,
                subjectMembershipLoadTypeRepository
        );
        authentication = new UsernamePasswordAuthenticationToken("teacher", "n/a", List.of());
        userRegisterService.currentUser = new UserRegisterService.CurrentUser(
                1,
                "teacher",
                true,
                10,
                "Current Teacher",
                null,
                Set.of("TEACHER"),
                Set.of()
        );
    }

    @Test
    void teacherCannotReadOrChangeForeignTest() {
        org.santayn.testing.models.test.Test foreignTest = new org.santayn.testing.models.test.Test();
        foreignTest.setId(7);
        foreignTest.setAuthorPersonId(99);
        when(testRepository.findById(7)).thenReturn(Optional.of(foreignTest));

        assertThatThrownBy(() -> accessService.requireTestOwner(authentication, 7))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherCannotReadForeignGroupStudent() {
        GroupMembership foreignStudent = new GroupMembership();
        foreignStudent.setId(22);
        foreignStudent.setGroupId(33);
        foreignStudent.setPersonId(99);
        foreignStudent.setRole(1);
        when(groupMembershipRepository.findById(22)).thenReturn(Optional.of(foreignStudent));
        when(subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(10)).thenReturn(List.of());
        when(teachingAssignmentRepository.findByGroupId(33)).thenReturn(List.of());

        assertThatThrownBy(() -> accessService.requireGroupMembershipOwner(authentication, 22))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherCannotUseInactiveSubjectMembershipForMutations() {
        SubjectMembership inactiveMembership = new SubjectMembership();
        inactiveMembership.setId(44);
        inactiveMembership.setSubjectId(7);
        inactiveMembership.setPersonId(10);
        inactiveMembership.setRole(1);
        inactiveMembership.setStatus(2);
        when(subjectMembershipRepository.findById(44)).thenReturn(Optional.of(inactiveMembership));

        assertThatThrownBy(() -> accessService.requireActiveSubjectMembershipOwner(authentication, 44))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherCannotMutateLectureFromInactiveSubjectMembership() {
        Lecture lecture = new Lecture();
        lecture.setId(55);
        lecture.setSubjectMembershipId(44);
        when(lectureRepository.findById(55)).thenReturn(Optional.of(lecture));

        SubjectMembership inactiveMembership = new SubjectMembership();
        inactiveMembership.setId(44);
        inactiveMembership.setSubjectId(7);
        inactiveMembership.setPersonId(10);
        inactiveMembership.setRole(1);
        inactiveMembership.setStatus(2);
        when(subjectMembershipRepository.findById(44)).thenReturn(Optional.of(inactiveMembership));

        assertThatThrownBy(() -> accessService.requireActiveLectureOwner(authentication, 55))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherCannotMutateQuestionFromInactiveTopicMembership() {
        Question question = new Question();
        question.setId(66L);
        question.setTopicId(77);
        when(questionRepository.findById(66L)).thenReturn(Optional.of(question));

        Topic topic = new Topic();
        topic.setId(77);
        topic.setSubjectMembershipId(44);
        when(topicRepository.findById(77)).thenReturn(Optional.of(topic));

        SubjectMembership inactiveMembership = new SubjectMembership();
        inactiveMembership.setId(44);
        inactiveMembership.setSubjectId(7);
        inactiveMembership.setPersonId(10);
        inactiveMembership.setRole(1);
        inactiveMembership.setStatus(2);
        when(subjectMembershipRepository.findById(44)).thenReturn(Optional.of(inactiveMembership));

        assertThatThrownBy(() -> accessService.requireActiveQuestionOwner(authentication, 66L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private static final class StubUserRegisterService extends UserRegisterService {
        private UserRegisterService.CurrentUser currentUser;

        private StubUserRegisterService() {
            super(null, null, null, null, false);
        }

        @Override
        public UserRegisterService.CurrentUser currentUser(String login) {
            return currentUser;
        }
    }
}
