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
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.SelectedOptionRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.service.CurrentUserAccessService;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.service.TestService;
import org.santayn.testing.service.TextAnswerGradingQueueService;
import org.santayn.testing.service.TextAnswerEvaluator;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String TEXT_ANSWER_REVIEW_NOTE =
            "Необходима дополнительная проверка преподавателя: ответ зачтен по смыслу, но не совпадает с эталоном.";

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
    private final QuestionOptionRepository questionOptionRepository;
    private final SelectedOptionRepository selectedOptionRepository;
    private final LectureTestLinkService lectureTestLinkService;
    private final CurrentUserAccessService accessService;
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
                                QuestionOptionRepository questionOptionRepository,
                                SelectedOptionRepository selectedOptionRepository,
                                LectureTestLinkService lectureTestLinkService,
                                CurrentUserAccessService accessService,
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
        this.questionOptionRepository = questionOptionRepository;
        this.selectedOptionRepository = selectedOptionRepository;
        this.lectureTestLinkService = lectureTestLinkService;
        this.accessService = accessService;
        this.testService = testService;
        this.userRegisterService = userRegisterService;
    }

    @GetMapping("/teacher/subjects")
    @Transactional(readOnly = true)
    public List<ResultSubjectResponse> teacherSubjects(Authentication authentication) {
        if (accessService.hasGlobalAcademicScope(authentication)) {
            return subjectRepository.findAll().stream()
                    .map(subject -> new ResultSubjectResponse(
                            subject.getId(),
                            subject.getName(),
                            subject.getDescription(),
                            null
                    ))
                    .toList();
        }
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
        Integer teacherPersonId = accessService.hasGlobalAcademicScope(authentication)
                ? null
                : currentPersonId(authentication);
        String subjectName = subjectRepository.findById(subjectId).map(Subject::getName).orElse("Предмет");
        return teacherLectures(subjectId, teacherPersonId)
                .stream()
                .map(lecture -> resultLectureResponse(lecture, subjectId, subjectName))
                .toList();
    }

    @GetMapping("/teacher/tests")
    @Transactional(readOnly = true)
    public List<ResultTestResponse> tests(@RequestParam Integer lectureId, Authentication authentication) {
        boolean globalAcademicScope = accessService.hasGlobalAcademicScope(authentication);
        Integer teacherPersonId = globalAcademicScope ? null : currentPersonId(authentication);
        Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
        if (lecture == null || (!globalAcademicScope && !lectureOwnedByTeacher(lecture, teacherPersonId))) {
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
        boolean globalAcademicScope = accessService.hasGlobalAcademicScope(authentication);
        Integer teacherPersonId = globalAcademicScope ? null : requireCurrentPersonId(user);
        if (!globalAcademicScope && !teacherTestIds(teacherPersonId).contains(testId)) {
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
        if (!globalAcademicScope) {
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
        if (!accessService.hasGlobalAcademicScope(authentication)
                && !teacherGroupIds(requireCurrentPersonId(user)).contains(groupId)) {
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
                                   @RequestParam(required = false) Integer page,
                                   @RequestParam(required = false) Integer size,
                                   Authentication authentication) {
        UserRegisterService.CurrentUser user = currentUser(authentication);

        boolean globalAcademicScope = accessService.hasGlobalAcademicScope(authentication);
        Integer teacherPersonId = globalAcademicScope ? null : requireCurrentPersonId(user);
        if (!globalAcademicScope) {
            requireTeacherResultFilters(teacherPersonId, subjectId, lectureId, testId, groupId, studentId);
        }
        return buildResultData(
                subjectId,
                lectureId,
                testId,
                groupId,
                studentId,
                teacherPersonId,
                true,
                page,
                size
        );
    }

    @GetMapping("/student/data")
    @Transactional(readOnly = true)
    public ResultDataResponse studentData(@RequestParam(required = false) Integer subjectId,
                                          @RequestParam(required = false) Integer testId,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size,
                                          Authentication authentication) {
        Integer studentPersonId = currentPersonId(authentication);
        return buildResultData(
                subjectId,
                null,
                testId,
                null,
                studentPersonId,
                null,
                false,
                page,
                size
        );
    }

    private ResultDataResponse buildResultData(Integer subjectId,
                                               Integer lectureId,
                                               Integer testId,
                                               Integer groupId,
                                               Integer studentId,
                                               Integer teacherPersonId,
                                               boolean teacherMode,
                                               Integer page,
                                               Integer size) {
        boolean filterByPersonContext = !teacherMode
                || teacherPersonId != null
                || groupId != null
                || studentId != null;
        boolean filterByTestContext = teacherMode
                ? teacherPersonId != null || subjectId != null || lectureId != null || testId != null
                : subjectId != null || testId != null;

        Set<Integer> allowedPersonIds = filterByPersonContext
                ? (teacherMode
                    ? personIdsForFilter(groupId, studentId, teacherPersonId)
                    : personIdsForCurrentStudent(studentId))
                : Set.of();
        Set<Integer> allowedTestIds = filterByTestContext
                ? (teacherMode
                    ? testIdsForFilters(subjectId, lectureId, testId, teacherPersonId)
                    : testIdsForCurrentStudent(subjectId, testId))
                : Set.of();

        Pageable pageable = resultPageable(page, size);
        Page<TestAttempt> attemptPage = testAttemptRepository.findResultAttempts(
                List.of(TestService.ATTEMPT_STATUS_IN_PROGRESS, TestService.ATTEMPT_STATUS_INVALIDATED),
                filterByPersonContext,
                repositoryFilterValues(allowedPersonIds),
                filterByTestContext,
                repositoryFilterValues(allowedTestIds),
                pageable
        );
        List<TestAttempt> candidateAttempts = attemptPage.getContent();
        List<Integer> attemptIds = candidateAttempts.stream()
                .map(TestAttempt::getId)
                .toList();

        List<QuestionResponse> responses = attemptIds.isEmpty()
                ? List.of()
                : questionResponseRepository.findByTestAttemptIdIn(attemptIds);
        responses = responses.stream()
                .sorted(Comparator
                        .comparing(QuestionResponse::getTestAttemptId)
                        .thenComparing(QuestionResponse::getId))
                .toList();
        Map<Integer, List<QuestionResponse>> responsesByAttempt = responses.stream()
                .collect(Collectors.groupingBy(
                        QuestionResponse::getTestAttemptId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Long> responseIds = responses.stream()
                .map(QuestionResponse::getId)
                .toList();
        Map<Long, List<SelectedOption>> selectedOptionsByResponse = responseIds.isEmpty()
                ? Map.of()
                : selectedOptionRepository.findByQuestionResponseIdIn(responseIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            SelectedOption::getQuestionResponseId,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

        List<Long> questionIds = responses.stream()
                .map(QuestionResponse::getTestQuestionId)
                .distinct()
                .toList();
        Map<Long, List<QuestionOption>> optionsByQuestion = !teacherMode || questionIds.isEmpty()
                ? Map.of()
                : questionOptionRepository.findByTestQuestionIdInOrderByTestQuestionIdAscOrdinalAsc(questionIds)
                    .stream()
                    .collect(Collectors.groupingBy(
                            QuestionOption::getTestQuestionId,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

        Set<Integer> candidateTestIds = candidateAttempts.stream()
                .map(TestAttempt::getTestAssignment)
                .filter(Objects::nonNull)
                .map(TestAssignment::getTestId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Integer, String> testNames = candidateTestIds.isEmpty()
                ? Map.of()
                : testRepository.findAllById(candidateTestIds)
                    .stream()
                    .collect(Collectors.toMap(
                            Test::getId,
                            test -> test.getTitle() == null || test.getTitle().isBlank()
                                    ? "Тест #" + test.getId()
                                    : test.getTitle(),
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));

        List<ResultAttemptAggregate> attempts = new ArrayList<>();
        for (TestAttempt attempt : candidateAttempts) {
            TestAssignment assignment = attempt.getTestAssignment();
            if (assignment == null || assignment.getTestId() == null) {
                continue;
            }

            List<ResultItemResponse> results = new ArrayList<>();
            int resultTotal = 0;
            long resultRight = 0;
            BigDecimal resultMaxScore = BigDecimal.ZERO;
            BigDecimal fallbackScore = BigDecimal.ZERO;
            for (QuestionResponse response : responsesByAttempt.getOrDefault(attempt.getId(), List.of())) {
                Question question = response.getTestQuestion();
                if (question == null) {
                    continue;
                }
                boolean correct = Boolean.TRUE.equals(response.getCorrect());
                resultTotal++;
                if (correct) {
                    resultRight++;
                }
                resultMaxScore = resultMaxScore.add(nonNegative(question.getPoints()));
                fallbackScore = fallbackScore.add(nonNegative(response.getAwardedPoints()));

                results.add(new ResultItemResponse(
                        question.getQuestion(),
                        givenAnswerDisplay(response, question, selectedOptionsByResponse),
                        teacherMode ? correctAnswerDisplay(question, optionsByQuestion) : null,
                        teacherMode ? correct : null,
                        teacherMode ? question.getPoints() : null,
                        teacherMode ? response.getAwardedPoints() : null,
                        teacherMode ? gradingStatus(response, question) : null,
                        teacherMode ? gradingNote(response, question) : null
                ));
            }

            String testName = testNames.getOrDefault(
                    assignment.getTestId(),
                    "Тест #" + assignment.getTestId()
            );
            Person person = attempt.getPerson();
            String studentName;
            if (person == null) {
                studentName = "Студент #" + attempt.getPersonId();
            } else {
                String fullName = fullName(person);
                studentName = fullName.isBlank() ? "Студент #" + attempt.getPersonId() : fullName;
            }

            BigDecimal attemptScore = attempt.getScore() == null
                    ? fallbackScore
                    : nonNegative(attempt.getScore());
            ResultStatsResponse attemptStats = statsResponse(
                    resultTotal,
                    resultRight,
                    attemptScore,
                    resultMaxScore
            );

            attempts.add(new ResultAttemptAggregate(
                    attempt.getId(),
                    assignment.getTestId(),
                    testName,
                    attempt.getPersonId(),
                    studentName,
                    attempt.getOrdinal(),
                    attempt.getCompletedAt(),
                    attemptStats,
                    results
            ));
        }

        int total = attempts.stream().map(ResultAttemptAggregate::stats).mapToInt(ResultStatsResponse::total).sum();
        long right = attempts.stream().map(ResultAttemptAggregate::stats).mapToLong(ResultStatsResponse::right).sum();
        BigDecimal score = attempts.stream()
                .map(ResultAttemptAggregate::stats)
                .map(ResultStatsResponse::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxScore = attempts.stream()
                .map(ResultAttemptAggregate::stats)
                .map(ResultStatsResponse::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResultDataResponse(
                statsResponse(total, right, score, maxScore),
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
                                attempt.stats().score(),
                                attempt.stats().maxScore(),
                                attempt.stats().scorePercent(),
                                attempt.stats(),
                                attempt.results()
                        ))
                        .toList(),
                pageable.isPaged() ? attemptPage.getNumber() : null,
                pageable.isPaged() ? attemptPage.getSize() : null,
                attemptPage.getTotalElements(),
                pageable.isPaged() ? attemptPage.getTotalPages() : null
        );
    }

    private Pageable resultPageable(Integer page, Integer size) {
        if (page == null && size == null) {
            return Pageable.unpaged();
        }
        int normalizedPage = page == null ? 0 : page;
        int normalizedSize = size == null ? 100 : size;
        if (normalizedPage < 0) {
            throw new IllegalArgumentException("Result page must be greater than or equal to 0.");
        }
        if (normalizedSize < 1 || normalizedSize > 500) {
            throw new IllegalArgumentException("Result page size must be between 1 and 500.");
        }
        return PageRequest.of(normalizedPage, normalizedSize);
    }

    private List<Integer> repositoryFilterValues(Set<Integer> values) {
        return values == null || values.isEmpty() ? List.of(-1) : new ArrayList<>(values);
    }

    private ResultStatsResponse statsResponse(int total,
                                              long right,
                                              BigDecimal score,
                                              BigDecimal maxScore) {
        BigDecimal safeScore = nonNegative(score);
        BigDecimal safeMaxScore = nonNegative(maxScore);
        BigDecimal scorePercent = safeMaxScore.signum() == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : safeScore.multiply(BigDecimal.valueOf(100))
                    .divide(safeMaxScore, 2, RoundingMode.HALF_UP);
        return new ResultStatsResponse(
                total,
                right,
                scorePercent,
                safeScore,
                safeMaxScore,
                scorePercent
        );
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private String gradingStatus(QuestionResponse response, Question question) {
        if (TextAnswerGradingQueueService.GRADING_PENDING.equals(response.getGradingStatus())) {
            return "pending";
        }
        if (TextAnswerGradingQueueService.GRADING_FAILED.equals(response.getGradingStatus())) {
            return "failed";
        }
        if (Boolean.TRUE.equals(response.getCorrect())) {
            return "correct";
        }
        if (isPartiallyCreditedTextAnswer(response, question)) {
            return "partial";
        }
        return "incorrect";
    }

    private String gradingNote(QuestionResponse response, Question question) {
        if (TextAnswerGradingQueueService.GRADING_PENDING.equals(response.getGradingStatus())) {
            return "Text answer grading is pending.";
        }
        if (TextAnswerGradingQueueService.GRADING_FAILED.equals(response.getGradingStatus())) {
            return (response.getGradingError() == null || response.getGradingError().isBlank())
                    ? "Text answer grading failed and requires review."
                    : response.getGradingError();
        }
        if (!requiresTextAnswerTeacherReview(response, question)) {
            return null;
        }
        return TEXT_ANSWER_REVIEW_NOTE;
    }

    private boolean requiresTextAnswerTeacherReview(QuestionResponse response, Question question) {
        return QuestionTypeSupport.isText(question.getType())
                && Boolean.TRUE.equals(response.getCorrect())
                && !TextAnswerEvaluator.isExactAcceptedAnswer(question.getCorrectAnswer(), response.getAnswerText());
    }

    private boolean isPartiallyCreditedTextAnswer(QuestionResponse response, Question question) {
        return QuestionTypeSupport.isText(question.getType())
                && !Boolean.TRUE.equals(response.getCorrect())
                && response.getAwardedPoints() != null
                && response.getAwardedPoints().compareTo(BigDecimal.ZERO) > 0;
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
            if (attempt.getStatus() == TestService.ATTEMPT_STATUS_IN_PROGRESS
                    || attempt.getStatus() == TestService.ATTEMPT_STATUS_INVALIDATED) {
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
        if (subjectId == null) {
            return testId == null ? Set.of() : Set.of(testId);
        }
        Set<Integer> ids = testService.findAll(subjectId).stream()
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
                ? null
                : new LinkedHashSet<>(teacherTestIds(teacherPersonId));
        if (subjectId != null) {
            Set<Integer> subjectTestIds = testService.findAll(subjectId).stream()
                    .map(Test::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            testIds = intersectResultFilter(testIds, subjectTestIds);
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
            testIds = intersectResultFilter(testIds, lectureTestIds);
        }
        if (testId != null) {
            testIds = intersectResultFilter(testIds, Set.of(testId));
        }
        return testIds == null ? Set.of() : testIds;
    }

    private Set<Integer> intersectResultFilter(Set<Integer> current, Set<Integer> constraint) {
        if (current == null) {
            return new LinkedHashSet<>(constraint);
        }
        current.retainAll(constraint);
        return current;
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
        if (membershipIds.isEmpty()) {
            return Set.of();
        }
        return teachingAssignmentRepository.findBySubjectMembershipIdIn(membershipIds).stream()
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
        Set<Integer> ids = testRepository.findByAuthorPersonId(teacherPersonId).stream()
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

    private String givenAnswerDisplay(QuestionResponse response,
                                      Question question,
                                      Map<Long, List<SelectedOption>> selectedOptionsByResponse) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            return QuestionTypeSupport.displaySubmittedMatchingPairs(question.getCorrectAnswer(), response.getAnswerText());
        }
        List<QuestionOption> selectedOptions = selectedOptionsByResponse
                .getOrDefault(response.getId(), List.of())
                .stream()
                .map(SelectedOption::getQuestionOption)
                .filter(Objects::nonNull)
                .filter(option -> question.getId().equals(option.getTestQuestionId()))
                .toList();
        if (QuestionTypeSupport.usesSelectableOptions(question.getType()) && !selectedOptions.isEmpty()) {
            return selectedOptions.stream()
                    .map(QuestionOption::getText)
                    .collect(Collectors.joining(", "));
        }
        return response.getAnswerText();
    }

    private String correctAnswerDisplay(Question question,
                                        Map<Long, List<QuestionOption>> optionsByQuestion) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            return QuestionTypeSupport.displayMatchingPairs(question.getCorrectAnswer());
        }
        if (!QuestionTypeSupport.usesSelectableOptions(question.getType())) {
            return question.getCorrectAnswer();
        }
        List<QuestionOption> options = optionsByQuestion.getOrDefault(question.getId(), List.of())
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
                                     List<ResultAttemptResponse> attempts,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) Integer page,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) Integer pageSize,
                                     long totalAttemptCount,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) Integer totalPages) {
    }

    public record ResultStatsResponse(int total,
                                      long right,
                                      BigDecimal percent,
                                      BigDecimal score,
                                      BigDecimal maxScore,
                                      BigDecimal scorePercent) {
    }

    public record ResultAttemptResponse(Integer attemptId,
                                        Integer testId,
                                        String testName,
                                        Integer studentId,
                                        String studentName,
                                        int attemptOrdinal,
                                        java.time.Instant completedAt,
                                        BigDecimal score,
                                        BigDecimal maxScore,
                                        BigDecimal scorePercent,
                                        ResultStatsResponse stats,
                                        List<ResultItemResponse> results) {
    }

    public record ResultItemResponse(String questionText,
                                     String givenAnswer,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String correctAnswer,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) Boolean correct,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal questionPoints,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal awardedPoints,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String gradingStatus,
                                     @JsonInclude(JsonInclude.Include.NON_NULL) String gradingNote) {
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
