package org.santayn.testing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.group.GroupMembership;
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
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserAccessServiceTests {

    @Mock private UserRegisterService userRegisterService;
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
    @Mock private Authentication authentication;

    @InjectMocks
    private CurrentUserAccessService accessService;

    @BeforeEach
    void currentTeacher() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("teacher");
        when(userRegisterService.currentUser("teacher")).thenReturn(new UserRegisterService.CurrentUser(
                1,
                "teacher",
                true,
                10,
                "Current Teacher",
                null,
                Set.of("TEACHER"),
                Set.of()
        ));
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
}
