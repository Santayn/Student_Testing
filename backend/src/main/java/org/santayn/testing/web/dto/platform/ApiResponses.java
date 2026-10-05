package org.santayn.testing.web.dto.platform;

import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.faculty.FacultyMembership;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureAssignment;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.models.lecture.StudentLectureProgress;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.question.SelectedOption;
import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.subject.SubjectMembershipLoadType;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.models.teacher.TeachingLoadType;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T, R> List<R> list(Collection<T> items, Function<T, R> mapper) {
        return items.stream().map(mapper).toList();
    }

    public static FacultyResponse faculty(Faculty item) {
        return new FacultyResponse(item.getId(), item.getName(), item.getCode(), item.getDescription());
    }

    public static GroupResponse group(Group item) {
        return new GroupResponse(item.getId(), item.getName(), item.getCode(), item.getFacultyId());
    }

    public static SubjectResponse subject(Subject item) {
        return new SubjectResponse(item.getId(), item.getName(), item.getDescription());
    }

    public static PersonResponse person(Person item) {
        return new PersonResponse(
                item.getId(),
                item.getFirstName(),
                item.getLastName(),
                item.getDateOfBirth(),
                item.getEmail(),
                item.getPhone()
        );
    }

    public static UserResponse user(User item) {
        return new UserResponse(
                item.getId(),
                item.getLogin(),
                item.isActive(),
                item.getPersonId(),
                roleNames(item),
                permissionNames(item)
        );
    }

    public static RoleResponse role(Role item) {
        return new RoleResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                list(item.getPermissions(), ApiResponses::permission)
        );
    }

    public static PermissionResponse permission(Permission item) {
        return new PermissionResponse(item.getId(), item.getName(), item.getDescription());
    }

    public static FacultyMembershipResponse facultyMembership(FacultyMembership item) {
        return new FacultyMembershipResponse(
                item.getId(),
                item.getFacultyId(),
                item.getPersonId(),
                item.getRole(),
                item.getStatus(),
                item.getAssignedAtUtc(),
                item.getRemovedAtUtc(),
                item.getNotes()
        );
    }

    public static GroupMembershipResponse groupMembership(GroupMembership item) {
        return new GroupMembershipResponse(
                item.getId(),
                item.getGroupId(),
                item.getPersonId(),
                item.getRole(),
                item.getStatus(),
                item.getAssignedAtUtc(),
                item.getRemovedAtUtc(),
                item.getNotes()
        );
    }

    public static SubjectMembershipResponse subjectMembership(SubjectMembership item) {
        return new SubjectMembershipResponse(
                item.getId(),
                item.getSubjectId(),
                item.getPersonId(),
                item.getRole(),
                item.getStatus(),
                item.getAssignedAtUtc(),
                item.getRemovedAtUtc(),
                item.getNotes()
        );
    }

    public static TeachingLoadTypeResponse teachingLoadType(TeachingLoadType item) {
        return new TeachingLoadTypeResponse(item.getId(), item.getName(), item.getDescription());
    }

    public static SubjectMembershipLoadTypeResponse subjectMembershipLoadType(SubjectMembershipLoadType item) {
        return new SubjectMembershipLoadTypeResponse(
                item.getId(),
                item.getSubjectMembershipId(),
                item.getTeachingLoadTypeId(),
                item.getStatus(),
                item.getAssignedAtUtc(),
                item.getRemovedAtUtc(),
                item.getNotes()
        );
    }

    public static CourseTemplateResponse courseTemplate(CourseTemplate item) {
        return new CourseTemplateResponse(
                item.getId(),
                item.getSubjectId(),
                item.getAuthorPersonId(),
                item.getName(),
                item.isPublicVisible(),
                item.getCreatedAt()
        );
    }

    public static CourseVersionResponse courseVersion(CourseVersion item) {
        return new CourseVersionResponse(
                item.getId(),
                item.getCourseTemplateId(),
                item.getVersionNumber(),
                item.getTitle(),
                item.getDescription(),
                item.isPublished(),
                item.getCreatedByPersonId(),
                item.getPublishedAtUtc(),
                item.getPublishedByPersonId(),
                item.getChangeNotes(),
                item.getCreatedAt()
        );
    }

    public static LectureResponse lecture(Lecture item) {
        return new LectureResponse(
                item.getId(),
                item.getSubjectId(),
                item.getSubjectMembershipId(),
                item.getCourseVersionId(),
                item.getOrdinal(),
                item.getTitle(),
                item.getDescription(),
                item.getContentFolderKey(),
                item.getLinkedTestId(),
                item.isPublicVisible()
        );
    }

    public static LectureMaterialResponse lectureMaterial(LectureMaterial item) {
        return new LectureMaterialResponse(
                item.getId(),
                item.getCourseLectureId(),
                item.getFileName(),
                item.getContentType(),
                item.getSizeBytes(),
                item.getUploadedAtUtc()
        );
    }

    public static TopicResponse topic(Topic item) {
        return new TopicResponse(
                item.getId(),
                item.getSubjectId(),
                item.getSubjectMembershipId(),
                item.getCourseLectureId(),
                item.getOrdinal(),
                item.getName(),
                item.getDescription()
        );
    }

    public static TeachingAssignmentResponse teachingAssignment(TeachingAssignment item) {
        return new TeachingAssignmentResponse(
                item.getId(),
                item.getSubjectMembershipId(),
                item.getGroupId(),
                item.getLoadTypeId(),
                item.getCourseVersionId(),
                item.getSemester(),
                item.getStudyCourse(),
                item.getAcademicYear(),
                item.getHoursPerWeek(),
                item.getStatus(),
                item.getNotes()
        );
    }

    public static TeachingAssignmentEnrollmentResponse teachingAssignmentEnrollment(TeachingAssignmentEnrollment item) {
        return new TeachingAssignmentEnrollmentResponse(
                item.getId(),
                item.getTeachingAssignmentId(),
                item.getGroupMembershipId(),
                item.getGroupId(),
                item.getStatus(),
                item.getEnrolledAtUtc(),
                item.getRemovedAtUtc()
        );
    }

    public static LectureAssignmentResponse lectureAssignment(LectureAssignment item) {
        return new LectureAssignmentResponse(
                item.getId(),
                item.getTeachingAssignmentId(),
                item.getCourseLectureId(),
                item.getAvailableFromUtc(),
                item.getDueToUtc(),
                item.getClosedAtUtc(),
                item.isRequired(),
                item.getMinProgressPercent(),
                item.getStatus(),
                item.getCreatedAtUtc(),
                item.getSnapshotCourseVersionId(),
                item.getSnapshotGroupId(),
                item.getSnapshotTeacherPersonId(),
                item.getSnapshotSemester(),
                item.getSnapshotAcademicYear()
        );
    }

    public static StudentLectureProgressResponse studentLectureProgress(StudentLectureProgress item) {
        return new StudentLectureProgressResponse(
                item.getId(),
                item.getLectureAssignmentId(),
                item.getTeachingAssignmentEnrollmentId(),
                item.getStatus(),
                item.getProgressPercent(),
                item.getFirstOpenedAtUtc(),
                item.getLastVisitedAtUtc(),
                item.getCompletedAtUtc(),
                item.getCompletionSource(),
                item.getCompletedByPersonId(),
                item.getLastPositionSeconds(),
                item.getTimeSpentSeconds(),
                item.getCreatedAtUtc(),
                item.getUpdatedAtUtc()
        );
    }

    public static TestResponse test(Test item) {
        return new TestResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getDuration(),
                item.getAttemptsAllowed(),
                item.getQuestionCount()
        );
    }

    public static QuestionResponseDto question(Question item) {
        return new QuestionResponseDto(
                item.getId(),
                item.getTestId(),
                item.getCourseLectureId(),
                item.getTopicId(),
                item.getType(),
                item.getQuestion(),
                item.getPoints(),
                item.getOrdinal(),
                item.getCorrectAnswer(),
                matchingPairs(item),
                item.isActive()
        );
    }

    public static QuestionOptionResponse questionOption(QuestionOption item) {
        return new QuestionOptionResponse(
                item.getId(),
                item.getTestQuestionId(),
                item.getText(),
                item.getOrdinal(),
                item.isCorrect()
        );
    }

    public static TestQuestionSelectionRuleResponse testQuestionSelectionRule(TestQuestionSelectionRule item) {
        return new TestQuestionSelectionRuleResponse(
                item.getId(),
                item.getTestId(),
                item.getCourseLectureId(),
                item.getTopicId(),
                item.getQuestionCount(),
                item.getTextQuestionCount(),
                item.getSingleAnswerQuestionCount(),
                item.getMultipleAnswerQuestionCount(),
                item.getMatchingQuestionCount(),
                item.getOrdinal()
        );
    }

    public static TestAssignmentResponse testAssignment(TestAssignment item) {
        return new TestAssignmentResponse(
                item.getId(),
                item.getTestId(),
                item.getScope(),
                item.getCourseVersionId(),
                item.getCourseLectureId(),
                item.getTeachingAssignmentId(),
                item.getAvailableFromUtc(),
                item.getAvailableUntilUtc(),
                item.getStatus()
        );
    }

    public static TestAttemptResponse testAttempt(TestAttempt item) {
        return new TestAttemptResponse(
                item.getId(),
                item.getOrdinal(),
                item.getTestAssignmentId(),
                item.getPersonId(),
                item.getTeachingAssignmentEnrollmentId(),
                item.getStatus(),
                item.getStartedAt(),
                item.getCompletedAt(),
                item.getScore()
        );
    }

    public static QuestionAnswerResponse questionResponse(QuestionResponse item) {
        return new QuestionAnswerResponse(
                item.getId(),
                item.getTestAttemptId(),
                item.getTestQuestionId(),
                item.getAnswerText(),
                item.getCorrect(),
                item.getAwardedPoints()
        );
    }

    public static SelectedOptionResponse selectedOption(SelectedOption item) {
        return new SelectedOptionResponse(item.getId(), item.getQuestionResponseId(), item.getQuestionOptionId());
    }

    private static List<MatchingPairResponse> matchingPairs(Question question) {
        return QuestionTypeSupport.parseMatchingPairs(question.getCorrectAnswer())
                .stream()
                .map(pair -> new MatchingPairResponse(pair.ordinal(), pair.left(), pair.right()))
                .toList();
    }

    private static Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> permissionNames(User user) {
        Set<String> names = user.getPermissions().stream()
                .map(Permission::getName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));

        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                if (permission.getName() != null && !permission.getName().isBlank()) {
                    names.add(permission.getName());
                }
            }
        }

        return names;
    }

    public record FacultyResponse(Integer id, String name, String code, String description) {
    }

    public record GroupResponse(Integer id, String name, String code, Integer facultyId) {
    }

    public record SubjectResponse(Integer id, String name, String description) {
    }

    public record PersonResponse(Integer id, String firstName, String lastName, LocalDate dateOfBirth, String email, String phone) {
    }

    public record UserResponse(Integer id, String login, boolean active, Integer personId, Set<String> roles, Set<String> permissions) {
    }

    public record RoleResponse(Integer id, String name, String description, List<PermissionResponse> permissions) {
    }

    public record PermissionResponse(Integer id, String name, String description) {
    }

    public record FacultyMembershipResponse(Integer id, Integer facultyId, Integer personId, int role, int status,
                                            Instant assignedAtUtc, Instant removedAtUtc, String notes) {
    }

    public record GroupMembershipResponse(Integer id, Integer groupId, Integer personId, int role, int status,
                                          Instant assignedAtUtc, Instant removedAtUtc, String notes) {
    }

    public record SubjectMembershipResponse(Integer id, Integer subjectId, Integer personId, int role, int status,
                                            Instant assignedAtUtc, Instant removedAtUtc, String notes) {
    }

    public record TeachingLoadTypeResponse(Integer id, String name, String description) {
    }

    public record SubjectMembershipLoadTypeResponse(Integer id, Integer subjectMembershipId, Integer teachingLoadTypeId,
                                                    int status, Instant assignedAtUtc, Instant removedAtUtc, String notes) {
    }

    public record CourseTemplateResponse(Integer id, Integer subjectId, Integer authorPersonId, String name,
                                         boolean publicVisible, Instant createdAt) {
    }

    public record CourseVersionResponse(Integer id, Integer courseTemplateId, int versionNumber, String title,
                                        String description, boolean published, Integer createdByPersonId,
                                        Instant publishedAtUtc, Integer publishedByPersonId, String changeNotes,
                                        Instant createdAt) {
    }

    public record LectureResponse(Integer id, Integer subjectId, Integer subjectMembershipId, Integer courseVersionId,
                                  int ordinal, String title, String description, String contentFolderKey,
                                  Integer linkedTestId, boolean publicVisible) {
    }

    public record LectureMaterialResponse(Integer id,
                                          Integer courseLectureId,
                                          String fileName,
                                          String contentType,
                                          long sizeBytes,
                                          Instant uploadedAtUtc) {
    }

    public record TopicResponse(Integer id, Integer subjectId, Integer subjectMembershipId, Integer courseLectureId,
                                int ordinal, String name, String description) {
    }

    public record TeachingAssignmentResponse(Integer id, Integer subjectMembershipId, Integer groupId, Integer loadTypeId,
                                             Integer courseVersionId, int semester, Integer studyCourse, int academicYear,
                                             BigDecimal hoursPerWeek, int status, String notes) {
    }

    public record TeachingAssignmentEnrollmentResponse(Integer id, Integer teachingAssignmentId, Integer groupMembershipId,
                                                       Integer groupId, int status, Instant enrolledAtUtc,
                                                       Instant removedAtUtc) {
    }

    public record LectureAssignmentResponse(Integer id, Integer teachingAssignmentId, Integer courseLectureId,
                                            Instant availableFromUtc, Instant dueToUtc, Instant closedAtUtc,
                                            boolean required, int minProgressPercent, int status, Instant createdAtUtc,
                                            Integer snapshotCourseVersionId, Integer snapshotGroupId,
                                            Integer snapshotTeacherPersonId, int snapshotSemester,
                                            int snapshotAcademicYear) {
    }

    public record StudentLectureProgressResponse(Integer id, Integer lectureAssignmentId,
                                                 Integer teachingAssignmentEnrollmentId, int status,
                                                 int progressPercent, Instant firstOpenedAtUtc,
                                                 Instant lastVisitedAtUtc, Instant completedAtUtc,
                                                 Integer completionSource, Integer completedByPersonId,
                                                 Long lastPositionSeconds, Long timeSpentSeconds,
                                                 Instant createdAtUtc, Instant updatedAtUtc) {
    }

    public record TestResponse(Integer id, String title, String description, LocalTime duration,
                               int attemptsAllowed, int questionCount) {
    }

    public record QuestionResponseDto(Long id, Integer testId, Integer courseLectureId, Integer topicId, int type,
                                      String question, BigDecimal points, int ordinal, String correctAnswer,
                                      List<MatchingPairResponse> matchingPairs,
                                      boolean active) {
    }

    public record QuestionOptionResponse(Long id, Long testQuestionId, String text, int ordinal, boolean correct) {
    }

    public record MatchingPairResponse(int ordinal, String left, String right) {
    }

    public record TestQuestionSelectionRuleResponse(Long id, Integer testId, Integer courseLectureId, Integer topicId,
                                                    int questionCount, int textQuestionCount,
                                                    int singleAnswerQuestionCount, int multipleAnswerQuestionCount,
                                                    int matchingQuestionCount,
                                                    int ordinal) {
    }

    public record TestAssignmentResponse(Integer id, Integer testId, int scope, Integer courseVersionId,
                                         Integer courseLectureId, Integer teachingAssignmentId, Instant availableFromUtc,
                                         Instant availableUntilUtc, int status) {
    }

    public record TestAttemptResponse(Integer id, int ordinal, Integer testAssignmentId, Integer personId,
                                      Integer teachingAssignmentEnrollmentId, int status, Instant startedAt,
                                      Instant completedAt, BigDecimal score) {
    }

    public record QuestionAnswerResponse(Long id, Integer testAttemptId, Long testQuestionId, String answerText,
                                         Boolean correct, BigDecimal awardedPoints) {
    }

    public record SelectedOptionResponse(Long id, Long questionResponseId, Long questionOptionId) {
    }
}
