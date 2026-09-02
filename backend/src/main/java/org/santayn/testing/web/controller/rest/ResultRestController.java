package org.santayn.testing.web.controller.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.question.SelectedOption;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.test.Test;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/results")
public class ResultRestController {

    private static final int SUBJECT_ROLE_TEACHER = 1;
    private static final int GROUP_ROLE_STUDENT = 1;

    private final SubjectMembershipRepository subjectMembershipRepository;
    private final SubjectRepository subjectRepository;
    private final CourseTemplateRepository courseTemplateRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final LectureRepository lectureRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final TestRepository testRepository;
    private final LectureAssignmentRepository lectureAssignmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionResponseRepository questionResponseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SelectedOptionRepository selectedOptionRepository;
    private final LectureTestLinkService lectureTestLinkService;
    private final TestService testService;
    private final UserRegisterService userRegisterService;

    public ResultRestController(SubjectMembershipRepository subjectMembershipRepository,
                                SubjectRepository subjectRepository,
                                CourseTemplateRepository courseTemplateRepository,
                                CourseVersionRepository courseVersionRepository,
                                LectureRepository lectureRepository,
                                TestAssignmentRepository testAssignmentRepository,
                                TestRepository testRepository,
                                LectureAssignmentRepository lectureAssignmentRepository,
                                TeachingAssignmentRepository teachingAssignmentRepository,
                                GroupMembershipRepository groupMembershipRepository,
                                GroupRepository groupRepository,
                                PersonRepository personRepository,
                                TestAttemptRepository testAttemptRepository,
                                QuestionResponseRepository questionResponseRepository,
                                QuestionRepository questionRepository,
                                QuestionOptionRepository questionOptionRepository,
                                SelectedOptionRepository selectedOptionRepository,
                                LectureTestLinkService lectureTestLinkService,
                                TestService testService,
                                UserRegisterService userRegisterService) {
        this.subjectMembershipRepository = subjectMembershipRepository;
        this.subjectRepository = subjectRepository;
        this.courseTemplateRepository = courseTemplateRepository;
        this.courseVersionRepository = courseVersionRepository;
        this.lectureRepository = lectureRepository;
        this.testAssignmentRepository = testAssignmentRepository;
        this.testRepository = testRepository;
        this.lectureAssignmentRepository = lectureAssignmentRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupRepository = groupRepository;
        this.personRepository = personRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionResponseRepository = questionResponseRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.selectedOptionRepository = selectedOptionRepository;
        this.lectureTestLinkService = lectureTestLinkService;
        this.testService = testService;
        this.userRegisterService = userRegisterService;
    }

    @GetMapping("/teacher/subjects")
    @Transactional(readOnly = true)
    public List<ResultSubjectResponse> teacherSubjects(Authentication authentication) {
        Integer personId = currentPersonId(authentication);
        return subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(personId)
                .stream()
                .filter(membership -> membership.getRole() == SUBJECT_ROLE_TEACHER)
                .map(this::subjectResponse)
                .flatMap(Optional::stream)
                .toList();
    }

    @GetMapping("/student/subjects")
    @Transactional(readOnly = true)
    public List<ResultSubjectResponse> studentSubjects(Authentication authentication) {
        Integer studentPersonId = currentPersonId(authentication);
        Set<Integer> subjectIds = subjectIdsForCurrentStudent(studentPersonId);
        return subjectIds.stream()
                .map(subjectRepository::findById)
                .flatMap(Optional::stream)
                .map(subject -> new ResultSubjectResponse(
                        subject.getId(),
                        subject.getName(),
                        subject.getDescription(),
                        null
                ))
                .toList();
    }

    @GetMapping("/teacher/lectures")
    @Transactional(readOnly = true)
    public List<ResultLectureResponse> lectures(@RequestParam Integer subjectId, Authentication authentication) {
        Integer teacherPersonId = currentPersonId(authentication);
        String subjectName = subjectRepository.findById(subjectId).map(Subject::getName).orElse("Предмет");
        return teacherLectures(subjectId, teacherPersonId)
                .stream()
                .map(lecture -> resultLectureResponse(lecture, subjectId, subjectName))
                .toList();
    }

    @GetMapping("/teacher/tests")
    @Transactional(readOnly = true)
    public List<ResultTestResponse> tests(@RequestParam Integer lectureId, Authentication authentication) {
        Integer teacherPersonId = currentPersonId(authentication);
        Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
        if (lecture == null || !lectureOwnedByTeacher(lecture, teacherPersonId)) {
            return List.of();
        }

        Set<Integer> testIds = new LinkedHashSet<>(lectureTestLinkService.findTestIdsByLectureId(lectureId));
        if (lecture.getLinkedTestId() != null) {
            testIds.add(lecture.getLinkedTestId());
        }

        return testIds.stream()
                .map(testRepository::findById)
                .flatMap(Optional::stream)
                .map(this::testResponse)
                .toList();
    }

    @GetMapping("/teacher/groups")
    @Transactional(readOnly = true)
    public List<ResultGroupResponse> groups(@RequestParam Integer testId, Authentication authentication) {
        UserRegisterService.CurrentUser user = currentUser(authentication);
        boolean admin = hasRole(user, "ADMIN");
        Integer teacherPersonId = admin ? null : requireCurrentPersonId(user);
        if (!admin && !teacherTestIds(teacherPersonId).contains(testId)) {
            throw new AccessDeniedException("Test does not belong to current teacher.");
        }
        Set<Integer> groupIds = new LinkedHashSet<>();
        for (TestAssignment testAssignment : testAssignmentRepository.findByTestId(testId)) {
            if (testAssignment.getCourseLectureId() != null) {
                lectureAssignmentRepository.findByCourseLectureId(testAssignment.getCourseLectureId())
                        .forEach(lectureAssignment -> teachingAssignmentRepository.findById(lectureAssignment.getTeachingAssignmentId())
                                .map(TeachingAssignment::getGroupId)
                                .ifPresent(groupIds::add));
            }
            if (testAssignment.getCourseVersionId() != null) {
                teachingAssignmentRepository.findByFilters(
                                null,
                                null,
                                testAssignment.getCourseVersionId(),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                        .stream()
                        .map(TeachingAssignment::getGroupId)
                        .forEach(groupIds::add);
            }
            testAttemptRepository.findByTestAssignmentId(testAssignment.getId())
                    .stream()
                    .map(TestAttempt::getPersonId)
                    .forEach(personId -> groupMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(personId)
                            .stream()
                            .filter(membership -> membership.getRole() == GROUP_ROLE_STUDENT)
                            .map(GroupMembership::getGroupId)
                            .forEach(groupIds::add));
        }
        if (!admin) {
            groupIds.retainAll(teacherGroupIds(teacherPersonId));
        }
        return groupIds.stream()
                .map(groupRepository::findById)
                .flatMap(Optional::stream)
                .map(group -> new ResultGroupResponse(group.getId(), group.getName(), group.getCode(), group.getFacultyId()))
                .toList();
    }

    @GetMapping("/teacher/students")
    @Transactional(readOnly = true)
    public List<ResultPersonResponse> students(@RequestParam Integer groupId, Authentication authentication) {
        UserRegisterService.CurrentUser user = currentUser(authentication);
        if (!hasRole(user, "ADMIN") && !teacherGroupIds(requireCurrentPersonId(user)).contains(groupId)) {
            throw new AccessDeniedException("Group does not belong to current teacher.");
        }
        return groupMembershipRepository.findByGroupIdAndRemovedAtUtcIsNull(groupId)
                .stream()
                .filter(membership -> membership.getRole() == GROUP_ROLE_STUDENT)
                .map(membership -> personRepository.findById(membership.getPersonId())
                        .map(person -> new ResultPersonResponse(
                                person.getId(),
                                person.getFirstName(),
                                person.getLastName(),
                                fullName(person),
                                person.getEmail()
                        )))
                .flatMap(Optional::stream)
                .toList();
    }

    @GetMapping("/teacher/data")
    @Transactional(readOnly = true)
    public ResultDataResponse data(@RequestParam(required = false) Integer subjectId,
                                   @RequestParam(required = false) Integer lectureId,
                                   @RequestParam(required = false) Integer testId,
                                   @RequestParam(required = false) Integer groupId,
                                   @RequestParam(required = false) Integer studentId,
                                   Authentication authentication) {
        UserRegisterService.CurrentUser user = currentUser(authentication);

        boolean admin = hasRole(user, "ADMIN");
        Integer teacherPersonId = admin ? null : requireCurrentPersonId(user);
        if (!admin) {
            requireTeacherResultFilters(teacherPersonId, subjectId, lectureId, testId, groupId, studentId);
        }
        return buildResultData(
                subjectId,
                lectureId,
                testId,
                groupId,
                studentId,
                teacherPersonId,
                true
        );
    }

    @GetMapping("/student/data")
    @Transactional(readOnly = true)
    public ResultDataResponse studentData(@RequestParam(required = false) Integer subjectId,
                                          @RequestParam(required = false) Integer testId,
                                          Authentication authentication) {
        Integer studentPersonId = currentPersonId(authentication);
        return buildResultData(
                subjectId,
                null,
                testId,
                null,
                studentPersonId,
                null,
                false
        );
    }

    private ResultDataResponse buildResultData(Integer subjectId,
                                               Integer lectureId,
                                               Integer testId,
                                               Integer groupId,
                                               Integer studentId,
                                               Integer teacherPersonId,
                                               boolean teacherMode) {
        Set<Integer> allowedPersonIds = teacherMode
                ? personIdsForFilter(groupId, studentId, teacherPersonId)
                : personIdsForCurrentStudent(studentId);
        Set<Integer> allowedTestIds = teacherMode
                ? testIdsForFilters(subjectId, lectureId, testId, teacherPersonId)
                : testIdsForCurrentStudent(subjectId, testId);
        boolean filterByTestContext = teacherMode
                ? teacherPersonId != null || subjectId != null || lectureId != null || testId != null
                : subjectId != null || testId != null;
        Map<Integer, TestAssignment> assignmentCache = new LinkedHashMap<>();
        Map<Integer, String> testNameCache = new LinkedHashMap<>();
        Map<Integer, String> personNameCache = new LinkedHashMap<>();
        Map<Long, Question> questionCache = new LinkedHashMap<>();
        List<ResultAttemptAggregate> attempts = new ArrayList<>();
        boolean filterByPersonContext = !teacherMode
                || teacherPersonId != null
                || groupId != null
                || studentId != null;

        for (TestAttempt attempt : testAttemptRepository.findAll()) {
            if (filterByPersonContext && !allowedPersonIds.contains(attempt.getPersonId())) {
                continue;
            }
            if (attempt.getStatus() == 1) {
                continue;
            }

            TestAssignment assignment = assignmentCache.computeIfAbsent(
                    attempt.getTestAssignmentId(),
                    id -> testAssignmentRepository.findById(id).orElse(null)
            );
            if (assignment == null || assignment.getTestId() == null) {
                continue;
            }
            if (filterByTestContext && !allowedTestIds.contains(assignment.getTestId())) {
                continue;
            }

            List<ResultItemResponse> results = new ArrayList<>();
            for (QuestionResponse response : questionResponseRepository.findByTestAttemptIdOrderByIdAsc(attempt.getId())) {
                Question question = questionCache.computeIfAbsent(
                        response.getTestQuestionId(),
                        id -> questionRepository.findById(id).orElse(null)
                );
                if (question == null) {
                    continue;
                }

                results.add(new ResultItemResponse(
                        question.getQuestion(),
                        givenAnswerDisplay(response, question),
                        teacherMode ? correctAnswerDisplay(question) : null,
                        Boolean.TRUE.equals(response.getCorrect())
                ));
            }

            String testName = testNameCache.computeIfAbsent(
                    assignment.getTestId(),
                    id -> testRepository.findById(id)
                            .map(test -> test.getTitle() == null || test.getTitle().isBlank() ? "Тест #" + id : test.getTitle())
                            .orElse("Тест #" + id)
            );
            String studentName = personNameCache.computeIfAbsent(
                    attempt.getPersonId(),
                    id -> personRepository.findById(id)
                            .map(person -> {
                                String name = fullName(person);
                                return name.isBlank() ? "Студент #" + id : name;
                            })
                            .orElse("Студент #" + id)
            );

            attempts.add(new ResultAttemptAggregate(
                    attempt.getId(),
                    assignment.getTestId(),
                    testName,
                    attempt.getPersonId(),
                    studentName,
                    attempt.getOrdinal(),
                    attempt.getCompletedAt(),
                    statsResponse(results),
                    results
            ));
        }

        attempts.sort((left, right) -> {
            if (left.completedAt() == null && right.completedAt() == null) {
                return Integer.compare(right.attemptId(), left.attemptId());
            }
            if (left.completedAt() == null) {
                return 1;
            }
            if (right.completedAt() == null) {
                return -1;
            }
            int completedAtCompare = right.completedAt().compareTo(left.completedAt());
            return completedAtCompare != 0
                    ? completedAtCompare
                    : Integer.compare(right.attemptId(), left.attemptId());
        });

        int total = attempts.stream().map(ResultAttemptAggregate::stats).mapToInt(ResultStatsResponse::total).sum();
        long right = attempts.stream().map(ResultAttemptAggregate::stats).mapToLong(ResultStatsResponse::right).sum();

        return new ResultDataResponse(
                statsResponse(total, right),
                testId == null ? null : testRepository.findById(testId).map(Test::getTitle).orElse(null),
                groupId == null ? null : groupRepository.findById(groupId).map(Group::getName).orElse(null),
                studentId == null ? null : personRepository.findById(studentId).map(this::fullName).orElse(null),
                attempts.size(),
                attempts.stream()
                        .map(attempt -> new ResultAttemptResponse(
                                attempt.attemptId(),
                                attempt.testId(),
                                attempt.testName(),
                                attempt.studentId(),
                                attempt.studentName(),
                                attempt.attemptOrdinal(),
                                attempt.completedAt(),
                                attempt.stats(),
                                attempt.results()
                        ))
                        .toList()
        );
    }

    private ResultStatsResponse statsResponse(List<ResultItemResponse> results) {
        return statsResponse(results.size(), results.stream().filter(ResultItemResponse::correct).count());
    }

    private ResultStatsResponse statsResponse(int total, long right) {
        BigDecimal percent = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(right * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        return new ResultStatsResponse(total, right, percent);
    }

    private Set<Integer> personIdsForFilter(Integer groupId, Integer studentId, Integer teacherPersonId) {
        Set<Integer> personIds = teacherPersonId == null
                ? new LinkedHashSet<>()
                : personIdsForTeacher(teacherPersonId);
        if (groupId != null) {
            Set<Integer> groupPersonIds = groupMembershipRepository.findByGroupIdAndRemovedAtUtcIsNull(groupId)
                    .stream()
                    .filter(membership -> membership.getRole() == GROUP_ROLE_STUDENT)
                    .map(GroupMembership::getPersonId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (teacherPersonId == null) {
                personIds.addAll(groupPersonIds);
            } else {
                personIds.retainAll(groupPersonIds);
            }
        }
        if (studentId != null) {
            if (teacherPersonId == null || personIds.contains(studentId)) {
                personIds.clear();
                personIds.add(studentId);
            } else {
                personIds.clear();
            }
        }
        return personIds;
    }

    private Set<Integer> personIdsForCurrentStudent(Integer studentPersonId) {
        if (studentPersonId == null) {
            return Set.of();
        }
        return Set.of(studentPersonId);
    }

    private Set<Integer> subjectIdsForCurrentStudent(Integer studentPersonId) {
        Set<Integer> subjectIds = new LinkedHashSet<>();

        subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(studentPersonId)
                .stream()
                .map(SubjectMembership::getSubjectId)
                .forEach(subjectIds::add);

        for (TestAttempt attempt : testAttemptRepository.findByPersonId(studentPersonId)) {
            if (attempt.getStatus() == 1) {
                continue;
            }
            TestAssignment assignment = testAssignmentRepository.findById(attempt.getTestAssignmentId()).orElse(null);
            if (assignment == null) {
                continue;
            }
            subjectIdsForAssignment(assignment).forEach(subjectIds::add);
        }

        return subjectIds;
    }

    private Set<Integer> testIdsForCurrentStudent(Integer subjectId, Integer testId) {
        if (subjectId == null && testId == null) {
            return Set.of();
        }
        Set<Integer> ids = subjectId == null
                ? testRepository.findAll().stream()
                    .map(Test::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                : testService.findAll(subjectId).stream()
                    .map(Test::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        if (testId != null) {
            ids.retainAll(Set.of(testId));
        }
        return ids;
    }

    private Set<Integer> subjectIdsForAssignment(TestAssignment assignment) {
        Set<Integer> subjectIds = new LinkedHashSet<>();

        if (assignment.getCourseLectureId() != null) {
            lectureRepository.findById(assignment.getCourseLectureId())
                    .map(Lecture::getSubjectId)
                    .ifPresent(subjectIds::add);
        }

        if (assignment.getCourseVersionId() != null) {
            courseVersionRepository.findById(assignment.getCourseVersionId())
                    .flatMap(version -> courseTemplateRepository.findById(version.getCourseTemplateId()))
                    .map(CourseTemplate::getSubjectId)
                    .ifPresent(subjectIds::add);
        }

        if (assignment.getTeachingAssignmentId() != null) {
            teachingAssignmentRepository.findById(assignment.getTeachingAssignmentId())
                    .flatMap(teachingAssignment -> subjectMembershipRepository.findById(teachingAssignment.getSubjectMembershipId()))
                    .map(SubjectMembership::getSubjectId)
                    .ifPresent(subjectIds::add);
        }

        return subjectIds;
    }

    private Set<Integer> testIdsForFilters(Integer subjectId, Integer lectureId, Integer testId, Integer teacherPersonId) {
        Set<Integer> testIds = teacherPersonId == null
                ? testRepository.findAll().stream()
                    .map(Test::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                : teacherTestIds(teacherPersonId);
        if (subjectId != null) {
            Set<Integer> subjectTestIds = testService.findAll(subjectId).stream()
                    .map(Test::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testIds.retainAll(subjectTestIds);
        }
        if (lectureId != null) {
            Set<Integer> lectureTestIds = new LinkedHashSet<>();
            Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
            if (lecture != null
                    && (teacherPersonId == null || lectureOwnedByTeacher(lecture, teacherPersonId))) {
                lectureTestLinkService.findTestIdsByLectureId(lectureId)
                        .forEach(lectureTestIds::add);
                testAssignmentRepository.findByCourseLectureId(lectureId)
                        .stream()
                        .map(TestAssignment::getTestId)
                        .forEach(lectureTestIds::add);
                if (lecture.getLinkedTestId() != null) {
                    lectureTestIds.add(lecture.getLinkedTestId());
                }
            }
            testIds.retainAll(lectureTestIds);
        }
        if (testId != null) {
            testIds.retainAll(Set.of(testId));
        }
        return testIds;
    }

    private void requireTeacherResultFilters(Integer teacherPersonId,
                                             Integer subjectId,
                                             Integer lectureId,
                                             Integer testId,
                                             Integer groupId,
                                             Integer studentId) {
        Set<Integer> subjectIds = teacherSubjectIds(teacherPersonId);
        if (subjectId != null && !subjectIds.contains(subjectId)) {
            throw new AccessDeniedException("Subject does not belong to current teacher.");
        }
        if (lectureId != null) {
            Lecture lecture = lectureRepository.findById(lectureId)
                    .orElseThrow(() -> new org.santayn.testing.service.ResourceNotFoundException(
                            "Course lecture not found: " + lectureId
                    ));
            if (!lectureOwnedByTeacher(lecture, teacherPersonId)) {
                throw new AccessDeniedException("Lecture does not belong to current teacher.");
            }
        }
        if (testId != null && !teacherTestIds(teacherPersonId).contains(testId)) {
            throw new AccessDeniedException("Test does not belong to current teacher.");
        }
        if (groupId != null && !teacherGroupIds(teacherPersonId).contains(groupId)) {
            throw new AccessDeniedException("Group does not belong to current teacher.");
        }
        if (studentId != null && !personIdsForTeacher(teacherPersonId).contains(studentId)) {
            throw new AccessDeniedException("Student is not taught by current teacher.");
        }
    }

    private Set<Integer> teacherSubjectIds(Integer teacherPersonId) {
        return subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(teacherPersonId)
                .stream()
                .filter(membership -> membership.getRole() == SUBJECT_ROLE_TEACHER)
                .map(SubjectMembership::getSubjectId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> teacherGroupIds(Integer teacherPersonId) {
        Set<Integer> membershipIds = subjectMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(teacherPersonId)
                .stream()
                .filter(membership -> membership.getRole() == SUBJECT_ROLE_TEACHER)
                .map(SubjectMembership::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return teachingAssignmentRepository.findAll().stream()
                .filter(assignment -> membershipIds.contains(assignment.getSubjectMembershipId()))
                .map(TeachingAssignment::getGroupId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> personIdsForTeacher(Integer teacherPersonId) {
        return teacherGroupIds(teacherPersonId).stream()
                .flatMap(groupId -> groupMembershipRepository.findByGroupIdAndRemovedAtUtcIsNull(groupId).stream())
                .filter(membership -> membership.getRole() == GROUP_ROLE_STUDENT)
                .map(GroupMembership::getPersonId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Integer> teacherTestIds(Integer teacherPersonId) {
        Set<Integer> ids = testRepository.findAll().stream()
                .filter(test -> teacherPersonId.equals(test.getAuthorPersonId()))
                .map(Test::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Integer subjectId : teacherSubjectIds(teacherPersonId)) {
            teacherLectures(subjectId, teacherPersonId).forEach(lecture -> {
                lectureTestLinkService.findTestIdsByLectureId(lecture.getId()).forEach(ids::add);
                testAssignmentRepository.findByCourseLectureId(lecture.getId())
                        .stream().map(TestAssignment::getTestId).forEach(ids::add);
                if (lecture.getLinkedTestId() != null) {
                    ids.add(lecture.getLinkedTestId());
                }
            });
        }
        return ids;
    }

    private List<CourseTemplate> teacherTemplates(Integer subjectId, Integer teacherPersonId) {
        return courseTemplateRepository.findByFilters(subjectId, teacherPersonId, false);
    }

    private List<Lecture> teacherLectures(Integer subjectId, Integer teacherPersonId) {
        LinkedHashMap<Integer, Lecture> lecturesById = new LinkedHashMap<>();

        subjectMembershipRepository.findByFilters(subjectId, teacherPersonId, null, true)
                .stream()
                .filter(membership -> membership.getRole() == SUBJECT_ROLE_TEACHER)
                .forEach(membership -> lectureRepository.findBySubjectMembershipIdOrderByOrdinalAsc(membership.getId())
                        .forEach(lecture -> lecturesById.put(lecture.getId(), lecture)));

        for (CourseTemplate template : teacherTemplates(subjectId, teacherPersonId)) {
            for (CourseVersion version : courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(template.getId())) {
                lectureRepository.findByCourseVersionIdOrderByOrdinalAsc(version.getId())
                        .forEach(lecture -> lecturesById.putIfAbsent(lecture.getId(), lecture));
            }
        }

        return lecturesById.values()
                .stream()
                .sorted((left, right) ->
                        Integer.compare(resultLectureVersionNumber(right), resultLectureVersionNumber(left))
                                != 0
                                ? Integer.compare(resultLectureVersionNumber(right), resultLectureVersionNumber(left))
                                : (left.getOrdinal() != right.getOrdinal()
                                   ? Integer.compare(left.getOrdinal(), right.getOrdinal())
                                   : String.valueOf(left.getTitle()).compareToIgnoreCase(String.valueOf(right.getTitle())))
                )
                .toList();
    }

    private boolean lectureOwnedByTeacher(Lecture lecture, Integer teacherPersonId) {
        if (lecture.getSubjectMembershipId() != null) {
            SubjectMembership membership = subjectMembershipRepository.findById(lecture.getSubjectMembershipId()).orElse(null);
            return membership != null && Objects.equals(membership.getPersonId(), teacherPersonId);
        }
        CourseVersion version = courseVersionRepository.findById(lecture.getCourseVersionId()).orElse(null);
        if (version == null) {
            return false;
        }
        CourseTemplate template = courseTemplateRepository.findById(version.getCourseTemplateId()).orElse(null);
        return template != null && Objects.equals(template.getAuthorPersonId(), teacherPersonId);
    }

    private ResultLectureResponse resultLectureResponse(Lecture lecture, Integer subjectId, String subjectName) {
        if (lecture.getCourseVersionId() != null) {
            CourseVersion version = courseVersionRepository.findById(lecture.getCourseVersionId()).orElse(null);
            CourseTemplate template = version == null ? null : courseTemplateRepository.findById(version.getCourseTemplateId()).orElse(null);
            return new ResultLectureResponse(
                    lecture.getId(),
                    lecture.getCourseVersionId(),
                    subjectId,
                    template != null ? template.getName() : subjectName,
                    version != null ? version.getVersionNumber() : 0,
                    lecture.getOrdinal(),
                    lecture.getTitle()
            );
        }

        return new ResultLectureResponse(
                lecture.getId(),
                null,
                subjectId,
                subjectName,
                0,
                lecture.getOrdinal(),
                lecture.getTitle()
        );
    }

    private int resultLectureVersionNumber(Lecture lecture) {
        if (lecture.getCourseVersionId() == null) {
            return 0;
        }
        return courseVersionRepository.findById(lecture.getCourseVersionId())
                .map(CourseVersion::getVersionNumber)
                .orElse(0);
    }

    private Optional<ResultSubjectResponse> subjectResponse(SubjectMembership membership) {
        return subjectRepository.findById(membership.getSubjectId())
                .map(subject -> new ResultSubjectResponse(
                        subject.getId(),
                        subject.getName(),
                        subject.getDescription(),
                        membership.getId()
                ));
    }

    private ResultTestResponse testResponse(Test test) {
        return new ResultTestResponse(test.getId(), test.getTitle(), test.getDescription(), test.getQuestionCount());
    }

    private String givenAnswerDisplay(QuestionResponse response, Question question) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            return QuestionTypeSupport.displaySubmittedMatchingPairs(question.getCorrectAnswer(), response.getAnswerText());
        }
        List<Long> selectedOptionIds = selectedOptionRepository.findByQuestionResponseId(response.getId())
                .stream()
                .map(SelectedOption::getQuestionOptionId)
                .toList();
        if (QuestionTypeSupport.usesSelectableOptions(question.getType()) && !selectedOptionIds.isEmpty()) {
            Map<Long, QuestionOption> options = questionOptionRepository.findAllById(selectedOptionIds)
                    .stream()
                    .collect(Collectors.toMap(QuestionOption::getId, option -> option, (left, right) -> left, LinkedHashMap::new));
            return selectedOptionIds.stream()
                    .map(options::get)
                    .filter(option -> option != null && question.getId().equals(option.getTestQuestionId()))
                    .map(QuestionOption::getText)
                    .collect(Collectors.joining(", "));
        }
        return response.getAnswerText();
    }

    private String correctAnswerDisplay(Question question) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            return QuestionTypeSupport.displayMatchingPairs(question.getCorrectAnswer());
        }
        if (!QuestionTypeSupport.usesSelectableOptions(question.getType())) {
            return question.getCorrectAnswer();
        }
        List<QuestionOption> options = questionOptionRepository.findByTestQuestionIdOrderByOrdinalAsc(question.getId())
                .stream()
                .filter(QuestionOption::isCorrect)
                .toList();
        if (!options.isEmpty()) {
            return options.stream().map(QuestionOption::getText).collect(Collectors.joining(", "));
        }
        return question.getCorrectAnswer();
    }

    private Integer currentPersonId(Authentication authentication) {
        return requireCurrentPersonId(currentUser(authentication));
    }

    private UserRegisterService.CurrentUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication is required.");
        }
        return userRegisterService.currentUser(authentication.getName());
    }

    private Integer requireCurrentPersonId(UserRegisterService.CurrentUser user) {
        if (user.personId() == null) {
            throw new IllegalArgumentException("Current user is not bound to a person.");
        }
        return user.personId();
    }

    private boolean hasRole(UserRegisterService.CurrentUser user, String roleName) {
        if (user == null || user.roles() == null || roleName == null) {
            return false;
        }
        String normalizedRoleName = roleName.trim().toUpperCase(java.util.Locale.ROOT);
        return user.roles()
                .stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase(java.util.Locale.ROOT))
                .anyMatch(role -> role.equals(normalizedRoleName) || role.equals("ROLE_" + normalizedRoleName));
    }

    private String fullName(Person person) {
        return java.util.stream.Stream.of(person.getLastName(), person.getFirstName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    public record ResultSubjectResponse(Integer id, String name, String description, Integer membershipId) {
    }

    public record ResultLectureResponse(Integer id, Integer courseVersionId, Integer subjectId, String courseName,
                                        int versionNumber, int ordinal, String title) {
    }

    public record ResultTestResponse(Integer id, String title, String description, int questionCount) {
    }

    public record ResultGroupResponse(Integer id, String name, String code, Integer facultyId) {
    }

    public record ResultPersonResponse(Integer id, String firstName, String lastName, String fullName, String email) {
    }

    public record ResultDataResponse(ResultStatsResponse stats,
                                     String selectedTestName,
                                     String selectedGroupName,
                                     String selectedStudentName,
                                     int attemptCount,
                                     List<ResultAttemptResponse> attempts) {
    }

    public record ResultStatsResponse(int total, long right, BigDecimal percent) {
    }

    public record ResultAttemptResponse(Integer attemptId,
                                        Integer testId,
                                        String testName,
                                        Integer studentId,
                                        String studentName,
                                        int attemptOrdinal,
                                        java.time.Instant completedAt,
                                        ResultStatsResponse stats,
                                        List<ResultItemResponse> results) {
    }

    public record ResultItemResponse(String questionText,
                                     String givenAnswer,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String correctAnswer,
                                     boolean correct) {
    }

    private record ResultAttemptAggregate(Integer attemptId,
                                          Integer testId,
                                          String testName,
                                          Integer studentId,
                                          String studentName,
                                          int attemptOrdinal,
                                          java.time.Instant completedAt,
                                          ResultStatsResponse stats,
                                          List<ResultItemResponse> results) {
    }
}
