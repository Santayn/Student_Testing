package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectMembershipLoadTypeRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CurrentUserAccessService {

    private final UserRegisterService userRegisterService;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final LectureRepository lectureRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final CourseTemplateRepository courseTemplateRepository;
    private final TestRepository testRepository;
    private final TestQuestionSelectionRuleRepository selectionRuleRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeachingAssignmentEnrollmentRepository enrollmentRepository;
    private final LectureAssignmentRepository lectureAssignmentRepository;
    private final SubjectMembershipLoadTypeRepository subjectMembershipLoadTypeRepository;

    public UserRegisterService.CurrentUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication is required.");
        }
        return userRegisterService.currentUser(authentication.getName());
    }

    public boolean isAdmin(Authentication authentication) {
        return hasRole(currentUser(authentication), "ADMIN");
    }

    public boolean isTeacherOrAdmin(Authentication authentication) {
        UserRegisterService.CurrentUser user = currentUser(authentication);
        return hasRole(user, "ADMIN") || hasRole(user, "TEACHER");
    }

    public boolean isTeacher(Authentication authentication) {
        return hasRole(currentUser(authentication), "TEACHER");
    }

    public Integer currentPersonId(Authentication authentication) {
        Integer personId = currentUser(authentication).personId();
        if (personId == null) {
            throw new AccessDeniedException("Current user is not bound to a person.");
        }
        return personId;
    }

    public void requireSubjectMembershipOwner(Authentication authentication, Integer subjectMembershipId) {
        if (isAdmin(authentication)) {
            return;
        }
        if (subjectMembershipId == null) {
            throw new AccessDeniedException("Subject membership is required.");
        }
        SubjectMembership membership = subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject membership not found: " + subjectMembershipId
                ));
        if (membership.getRole() != 1
                || !Objects.equals(membership.getPersonId(), currentPersonId(authentication))) {
            throw new AccessDeniedException("Subject membership belongs to another user.");
        }
    }

    public void requireSubjectMembershipRead(Authentication authentication, Integer subjectMembershipId) {
        var membership = subjectMembershipRepository.findById(subjectMembershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject membership not found: " + subjectMembershipId
                ));
        if (isAdmin(authentication)) {
            return;
        }
        Integer personId = currentPersonId(authentication);
        if (Objects.equals(membership.getPersonId(), personId)) {
            return;
        }
        var currentGroupIds = groupMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(personId)
                .stream()
                .map(groupMembership -> groupMembership.getGroupId())
                .collect(java.util.stream.Collectors.toSet());
        boolean visibleThroughAssignment = teachingAssignmentRepository
                .findBySubjectMembershipId(subjectMembershipId)
                .stream()
                .anyMatch(assignment -> currentGroupIds.contains(assignment.getGroupId()));
        if (!visibleThroughAssignment) {
            throw new AccessDeniedException("Subject membership is not visible to current user.");
        }
    }

    public void requireTopicOwner(Authentication authentication, Integer topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
        requireSubjectMembershipOwner(authentication, topic.getSubjectMembershipId());
    }

    public void requireSubjectOwner(Authentication authentication, Integer subjectId) {
        if (isAdmin(authentication)) {
            return;
        }
        if (!subjectMembershipRepository.existsBySubjectIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(
                subjectId, currentPersonId(authentication), 1
        )) {
            throw new AccessDeniedException("Subject does not belong to current teacher.");
        }
    }

    public void requireTestOwner(Authentication authentication, Integer testId) {
        var test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
        if (isAdmin(authentication)) {
            return;
        }
        Integer personId = currentPersonId(authentication);
        if (test.getAuthorPersonId() != null) {
            if (!Objects.equals(test.getAuthorPersonId(), personId)) {
                throw new AccessDeniedException("Test belongs to another user.");
            }
            return;
        }
        var rules = selectionRuleRepository.findByTestIdOrderByOrdinalAsc(testId);
        if (rules.isEmpty()) {
            throw new AccessDeniedException("Legacy test without an owner is admin-only.");
        }
        for (var rule : rules) {
            boolean hasOwnedTarget = false;
            if (rule.getTopicId() != null) {
                requireTopicOwner(authentication, rule.getTopicId());
                hasOwnedTarget = true;
            }
            if (rule.getCourseLectureId() != null) {
                requireLectureOwner(authentication, rule.getCourseLectureId());
                hasOwnedTarget = true;
            }
            if (!hasOwnedTarget) {
                throw new AccessDeniedException("Legacy test has an ownerless selection rule.");
            }
        }
    }

    public boolean canManageTest(Authentication authentication, Integer testId) {
        try {
            requireTestOwner(authentication, testId);
            return true;
        } catch (AccessDeniedException ignored) {
            return false;
        }
    }

    public void requireCourseTemplateOwner(Authentication authentication, Integer templateId) {
        var template = courseTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Course template not found: " + templateId));
        if (!isAdmin(authentication)
                && !Objects.equals(template.getAuthorPersonId(), currentPersonId(authentication))) {
            throw new AccessDeniedException("Course template belongs to another user.");
        }
    }

    public void requireCourseVersionOwner(Authentication authentication, Integer versionId) {
        Integer templateId = courseVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Course version not found: " + versionId))
                .getCourseTemplateId();
        requireCourseTemplateOwner(authentication, templateId);
    }

    public void requireGroupMember(Authentication authentication, Integer groupId) {
        if (isAdmin(authentication)) {
            return;
        }
        if (!groupMembershipRepository.existsByGroupIdAndPersonIdAndRoleAndRemovedAtUtcIsNull(
                groupId, currentPersonId(authentication), 1
        )) {
            throw new AccessDeniedException("Current student is not a member of this group.");
        }
    }

    public void requireGroupMembershipOwner(Authentication authentication, Integer groupMembershipId) {
        var membership = groupMembershipRepository.findById(groupMembershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Group membership not found: " + groupMembershipId
                ));
        if (isAdmin(authentication)) {
            return;
        }
        Integer personId = currentPersonId(authentication);
        if (Objects.equals(membership.getPersonId(), personId)) {
            return;
        }
        if (isTeacher(authentication)) {
            var ownedSubjectMembershipIds = subjectMembershipRepository
                    .findByPersonIdAndRemovedAtUtcIsNull(personId)
                    .stream()
                    .filter(subjectMembership -> subjectMembership.getRole() == 1)
                    .map(SubjectMembership::getId)
                    .collect(java.util.stream.Collectors.toSet());
            boolean teachesGroup = teachingAssignmentRepository.findByGroupId(membership.getGroupId())
                    .stream()
                    .anyMatch(assignment -> ownedSubjectMembershipIds.contains(assignment.getSubjectMembershipId()));
            if (teachesGroup) {
                return;
            }
        }
        throw new AccessDeniedException("Group membership belongs to another person.");
    }

    public void requireTeachingAssignmentOwner(Authentication authentication, Integer assignmentId) {
        var assignment = teachingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment not found: " + assignmentId));
        requireSubjectMembershipOwner(authentication, assignment.getSubjectMembershipId());
    }

    public void requireTeachingAssignmentAccess(Authentication authentication, Integer assignmentId) {
        if (isTeacherOrAdmin(authentication)) {
            requireTeachingAssignmentOwner(authentication, assignmentId);
            return;
        }
        var assignment = teachingAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment not found: " + assignmentId));
        requireGroupMember(authentication, assignment.getGroupId());
    }

    public void requireEnrollmentAccess(Authentication authentication, Integer enrollmentId) {
        var enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teaching assignment enrollment not found: " + enrollmentId
                ));
        if (isTeacherOrAdmin(authentication)) {
            requireTeachingAssignmentOwner(authentication, enrollment.getTeachingAssignmentId());
        } else {
            requireGroupMembershipOwner(authentication, enrollment.getGroupMembershipId());
        }
    }

    public void requireLectureAssignmentOwner(Authentication authentication, Integer lectureAssignmentId) {
        Integer teachingAssignmentId = lectureAssignmentRepository.findById(lectureAssignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lecture assignment not found: " + lectureAssignmentId
                ))
                .getTeachingAssignmentId();
        requireTeachingAssignmentOwner(authentication, teachingAssignmentId);
    }

    public void requireSubjectLoadTypeOwner(Authentication authentication, Integer subjectLoadTypeId) {
        Integer subjectMembershipId = subjectMembershipLoadTypeRepository.findById(subjectLoadTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject load type not found: " + subjectLoadTypeId
                ))
                .getSubjectMembershipId();
        requireSubjectMembershipOwner(authentication, subjectMembershipId);
    }

    public void requireQuestionOwner(Authentication authentication, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        requireQuestionContextOwner(authentication, question.getTopicId(), question.getCourseLectureId(), question.getTestId());
    }

    public void requireOptionOwner(Authentication authentication, Long optionId) {
        Long questionId = questionOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question option not found: " + optionId))
                .getTestQuestionId();
        requireQuestionOwner(authentication, questionId);
    }

    public void requireQuestionContextOwner(Authentication authentication,
                                            Integer topicId,
                                            Integer courseLectureId,
                                            Integer testId) {
        if (isAdmin(authentication)) {
            return;
        }
        boolean hasContext = false;
        if (topicId != null) {
            requireTopicOwner(authentication, topicId);
            hasContext = true;
        }
        if (courseLectureId != null) {
            requireLectureOwner(authentication, courseLectureId);
            hasContext = true;
        }
        if (testId != null) {
            requireTestOwner(authentication, testId);
            hasContext = true;
        }
        if (!hasContext) {
            throw new AccessDeniedException(
                    "Teacher questions must belong to an owned test, topic or lecture."
            );
        }
    }

    public void requireLectureOwner(Authentication authentication, Integer lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Course lecture not found: " + lectureId));
        if (isAdmin(authentication)) {
            return;
        }
        if (lecture.getSubjectMembershipId() != null) {
            requireSubjectMembershipOwner(authentication, lecture.getSubjectMembershipId());
            return;
        }
        Integer personId = currentPersonId(authentication);
        boolean owned = courseVersionRepository.findById(lecture.getCourseVersionId())
                .flatMap(version -> courseTemplateRepository.findById(version.getCourseTemplateId()))
                .map(template -> Objects.equals(template.getAuthorPersonId(), personId))
                .orElse(false);
        if (!owned) {
            throw new AccessDeniedException("Course lecture belongs to another user.");
        }
    }

    private static boolean hasRole(UserRegisterService.CurrentUser user, String expectedRole) {
        if (user.roles() == null) {
            return false;
        }
        String expected = expectedRole.toUpperCase(Locale.ROOT);
        return user.roles().stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .anyMatch(role -> role.equals(expected) || role.equals("ROLE_" + expected));
    }
}
