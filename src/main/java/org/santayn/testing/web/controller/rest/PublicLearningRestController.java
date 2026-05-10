package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.course.CourseTemplate;
import org.santayn.testing.models.course.CourseVersion;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureAssignment;
import org.santayn.testing.models.lecture.LectureMaterial;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.subject.SubjectMembership;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.CourseTemplateRepository;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.LectureAssignmentRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.SubjectMembershipRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.service.LectureMaterialService;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.service.TestService;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
@RequestMapping({"/api/public/learning", "/api/v1/public/learning"})
public class PublicLearningRestController {

    private static final int GROUP_ROLE_STUDENT = 1;
    private static final int ACTIVE_GROUP_MEMBERSHIP_STATUS = 1;
    private static final int ACTIVE_TEACHING_ASSIGNMENT_STATUS = 1;
    private static final int ACTIVE_LECTURE_ASSIGNMENT_STATUS = 1;

    private final CourseTemplateRepository courseTemplateRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final LectureAssignmentRepository lectureAssignmentRepository;
    private final LectureRepository lectureRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectMembershipRepository subjectMembershipRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final LectureTestLinkService lectureTestLinkService;
    private final LectureMaterialService lectureMaterialService;
    private final TestService testService;
    private final UserRegisterService userRegisterService;

    public PublicLearningRestController(CourseTemplateRepository courseTemplateRepository,
                                        CourseVersionRepository courseVersionRepository,
                                        GroupMembershipRepository groupMembershipRepository,
                                        LectureAssignmentRepository lectureAssignmentRepository,
                                        LectureRepository lectureRepository,
                                        SubjectRepository subjectRepository,
                                        SubjectMembershipRepository subjectMembershipRepository,
                                        TeachingAssignmentRepository teachingAssignmentRepository,
                                        TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository,
                                        TestAssignmentRepository testAssignmentRepository,
                                        TestRepository testRepository,
                                        QuestionRepository questionRepository,
                                        QuestionOptionRepository questionOptionRepository,
                                        LectureTestLinkService lectureTestLinkService,
                                        LectureMaterialService lectureMaterialService,
                                        TestService testService,
                                        UserRegisterService userRegisterService) {
        this.courseTemplateRepository = courseTemplateRepository;
        this.courseVersionRepository = courseVersionRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.lectureAssignmentRepository = lectureAssignmentRepository;
        this.lectureRepository = lectureRepository;
        this.subjectRepository = subjectRepository;
        this.subjectMembershipRepository = subjectMembershipRepository;
        this.teachingAssignmentRepository = teachingAssignmentRepository;
        this.teachingAssignmentEnrollmentRepository = teachingAssignmentEnrollmentRepository;
        this.testAssignmentRepository = testAssignmentRepository;
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.questionOptionRepository = questionOptionRepository;
        this.lectureTestLinkService = lectureTestLinkService;
        this.lectureMaterialService = lectureMaterialService;
        this.testService = testService;
        this.userRegisterService = userRegisterService;
    }

    @GetMapping("/subjects/{subjectId}")
    @Transactional(readOnly = true)
    public PublicSubjectResponse subject(@PathVariable Integer subjectId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        requireSubjectAccess(context, subjectId);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
        return new PublicSubjectResponse(subject.getId(), subject.getName(), subject.getDescription());
    }

    @GetMapping("/subjects/{subjectId}/lectures")
    @Transactional(readOnly = true)
    public List<PublicLectureResponse> subjectLectures(@PathVariable Integer subjectId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        requireSubjectAccess(context, subjectId);
        return accessibleLecturesForSubject(context, subjectId);
    }

    @GetMapping("/lectures/{lectureId}")
    @Transactional(readOnly = true)
    public PublicLectureResponse lecture(@PathVariable Integer lectureId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        Lecture lecture = requireAccessibleLecture(context, lectureId);
        return lectureResponse(lecture);
    }

    @GetMapping("/lectures/{lectureId}/tests")
    @Transactional
    public List<PublicTestResponse> lectureTests(@PathVariable Integer lectureId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        Lecture lecture = requireAccessibleLecture(context, lectureId);
        LinkedHashMap<Integer, PublicTestResponse> responsesByTestId = new LinkedHashMap<>();
        for (Test test : linkedTestsForLecture(lecture)) {
            Optional<TestAssignment> assignment = accessibleAssignmentForLinkedTest(context, test.getId());
            if (assignment.isEmpty()) {
                lectureTestLinkService.ensureLectureTestAvailability(lecture.getId(), test.getId());
                assignment = accessibleAssignmentForLinkedTest(context, test.getId());
            }
            responsesByTestId.putIfAbsent(
                    test.getId(),
                    testResponse(
                            test,
                            assignment.map(TestAssignment::getId).orElse(null),
                            assignment.isPresent(),
                            assignment.isPresent()
                                    ? null
                                    : "Тест привязан к лекции, но сейчас недоступен студенту."
                    )
            );
        }
        return responsesByTestId.values().stream().toList();
    }

    @GetMapping("/lectures/{lectureId}/materials")
    @Transactional(readOnly = true)
    public List<PublicLectureMaterialResponse> lectureMaterials(@PathVariable Integer lectureId,
                                                                Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        requireAccessibleLecture(context, lectureId);
        return lectureMaterialService.findByLectureId(lectureId)
                .stream()
                .map(this::lectureMaterialResponse)
                .toList();
    }

    @GetMapping("/lectures/{lectureId}/materials/{materialId}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadLectureMaterial(@PathVariable Integer lectureId,
                                                            @PathVariable Integer materialId,
                                                            Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        requireAccessibleLecture(context, lectureId);
        LectureMaterialService.StoredLectureMaterial storedMaterial = lectureMaterialService.load(lectureId, materialId);
        Resource resource = new FileSystemResource(storedMaterial.path());
        MediaType mediaType = MediaTypeFactory.getMediaType(storedMaterial.material().getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(storedMaterial.material().getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(mediaType)
                .contentLength(storedMaterial.material().getSizeBytes())
                .body(resource);
    }

    @GetMapping("/tests/{testId}")
    @Transactional(readOnly = true)
    public PublicTestLoadResponse test(@PathVariable Integer testId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        TestAssignment assignment = activeAccessibleAssignment(context, testId);
        Test test = testRepository.findById(assignment.getTestId())
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + assignment.getTestId()));
        List<PublicQuestionResponse> questions = testService.randomQuestionsForTest(test.getId())
                .stream()
                .map(this::questionResponse)
                .toList();
        return new PublicTestLoadResponse(testResponse(test, assignment.getId()), questions);
    }

    @PostMapping("/test-assignments/{assignmentId}/attempts/start")
    @Transactional
    public PublicTestAttemptLoadResponse startAttempt(@PathVariable Integer assignmentId, Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        TestAssignment assignment = requireAccessibleTestAssignment(context, assignmentId);
        Test test = testRepository.findById(assignment.getTestId())
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + assignment.getTestId()));
        Integer enrollmentId = findEnrollmentIdForTestAssignment(context, assignment).orElse(null);
        TestService.TestAttemptQuestionSet questionSet = testService.startOrResumeAttemptWithRandomQuestions(
                assignmentId,
                context.personId(),
                enrollmentId
        );
        List<PublicQuestionResponse> questions = questionSet.questions()
                .stream()
                .map(this::questionResponse)
                .toList();
        return new PublicTestAttemptLoadResponse(
                questionSet.attempt().getId(),
                assignment.getId(),
                testResponse(test, assignment.getId()),
                questions
        );
    }

    @PostMapping("/tests/{testId}/submit")
    @Transactional
    public PublicSubmitResponse submit(@PathVariable Integer testId,
                                       @RequestBody PublicSubmitRequest request,
                                       Authentication authentication) {
        StudentLearningContext context = studentLearningContext(authentication);
        TestAssignment assignment = activeAccessibleAssignment(context, testId);
        Integer enrollmentId = findEnrollmentIdForTestAssignment(context, assignment).orElse(null);
        TestAttempt attempt = testService.startAttempt(assignment.getId(), context.personId(), enrollmentId);
        return submitAttemptResponses(attempt, null, request);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    @Transactional
    public PublicSubmitResponse submitAttempt(@PathVariable Integer attemptId,
                                              @RequestBody PublicSubmitRequest request,
                                              Authentication authentication) {
        Integer personId = currentPersonId(authentication);
        TestAttempt attempt = testService.getAttempt(attemptId);
        if (!attempt.getPersonId().equals(personId)) {
            throw new IllegalArgumentException("Test attempt does not belong to current user.");
        }
        if (attempt.getStatus() != 1) {
            throw new IllegalArgumentException("Test attempt is not in progress: " + attemptId);
        }

        List<Long> selectedQuestionIds = testService.findResponses(attemptId)
                .stream()
                .map(QuestionResponse::getTestQuestionId)
                .toList();
        if (selectedQuestionIds.isEmpty()) {
            throw new IllegalArgumentException("Test attempt has no selected questions: " + attemptId);
        }

        return submitAttemptResponses(attempt, selectedQuestionIds, request);
    }

    private PublicSubmitResponse submitAttemptResponses(TestAttempt attempt,
                                                        List<Long> selectedQuestionIds,
                                                        PublicSubmitRequest request) {
        List<Long> questionIds = request.questionIds() == null ? List.of() : request.questionIds();
        List<String> answers = request.answers() == null ? List.of() : request.answers();
        List<List<Long>> selectedOptions = request.selectedOptionIds() == null ? List.of() : request.selectedOptionIds();
        List<Long> actualQuestionIds = selectedQuestionIds == null ? questionIds : selectedQuestionIds;
        Map<Long, Integer> submittedQuestionIndexes = new LinkedHashMap<>();
        for (int index = 0; index < questionIds.size(); index++) {
            Long questionId = questionIds.get(index);
            if (questionId != null && !submittedQuestionIndexes.containsKey(questionId)) {
                submittedQuestionIndexes.put(questionId, index);
            }
        }

        List<PublicSubmitDetailResponse> details = new ArrayList<>();
        int correctCount = 0;

        for (Long questionId : actualQuestionIds) {
            Integer submittedIndex = submittedQuestionIndexes.get(questionId);
            String answer = submittedIndex != null && submittedIndex < answers.size() ? answers.get(submittedIndex) : null;
            List<Long> selectedIds = submittedIndex != null && submittedIndex < selectedOptions.size() && selectedOptions.get(submittedIndex) != null
                    ? selectedOptions.get(submittedIndex)
                    : List.of();

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
            QuestionResponse response = testService.submitResponse(
                    attempt.getId(),
                    questionId,
                    answer,
                    selectedIds,
                    null,
                    null
            );
            if (Boolean.TRUE.equals(response.getCorrect())) {
                correctCount++;
            }
            details.add(new PublicSubmitDetailResponse(
                    question.getQuestion(),
                    givenAnswerDisplay(question, answer, selectedIds),
                    correctAnswerDisplay(question),
                    Boolean.TRUE.equals(response.getCorrect())
            ));
        }

        TestAttempt completed = testService.completeAttempt(attempt.getId(), null, 2);
        return new PublicSubmitResponse(attempt.getId(), completed.getScore(), correctCount, actualQuestionIds.size(), details);
    }

    private StudentLearningContext studentLearningContext(Authentication authentication) {
        Integer personId = currentPersonId(authentication);
        List<GroupMembership> memberships = groupMembershipRepository.findByFilters(null, personId, null, true)
                .stream()
                .filter(this::isActiveStudentMembership)
                .toList();
        if (memberships.isEmpty()) {
            throw new IllegalArgumentException("Current user has no active student group.");
        }

        LinkedHashMap<Integer, TeachingAssignment> assignmentsById = new LinkedHashMap<>();
        for (GroupMembership membership : memberships) {
            teachingAssignmentRepository.findByGroupId(membership.getGroupId())
                    .stream()
                    .filter(this::isActiveTeachingAssignment)
                    .forEach(assignment -> assignmentsById.put(assignment.getId(), assignment));
        }

        List<TeachingAssignmentEnrollment> enrollments = new ArrayList<>();
        for (GroupMembership membership : memberships) {
            List<TeachingAssignmentEnrollment> membershipEnrollments = teachingAssignmentEnrollmentRepository
                    .findByGroupMembershipId(membership.getId())
                    .stream()
                    .filter(this::isActiveEnrollment)
                    .toList();
            enrollments.addAll(membershipEnrollments);
            for (TeachingAssignmentEnrollment enrollment : membershipEnrollments) {
                teachingAssignmentRepository.findById(enrollment.getTeachingAssignmentId())
                        .filter(this::isActiveTeachingAssignment)
                        .ifPresent(assignment -> assignmentsById.put(assignment.getId(), assignment));
            }
        }

        Map<Integer, SubjectMembership> subjectMembershipsById = subjectMembershipRepository.findAllById(
                        assignmentsById.values()
                                .stream()
                                .map(TeachingAssignment::getSubjectMembershipId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(
                        SubjectMembership::getId,
                        membership -> membership,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return new StudentLearningContext(
                personId,
                List.copyOf(assignmentsById.values()),
                List.copyOf(enrollments),
                Map.copyOf(assignmentsById),
                Map.copyOf(subjectMembershipsById)
        );
    }

    private void requireSubjectAccess(StudentLearningContext context, Integer subjectId) {
        boolean hasAccess = context.assignments()
                .stream()
                .map(assignment -> subjectIdOf(context, assignment))
                .anyMatch(subjectId::equals);
        if (!hasAccess) {
            throw new IllegalArgumentException("Subject is not assigned to current student: " + subjectId);
        }
    }

    private Lecture requireAccessibleLecture(StudentLearningContext context, Integer lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found: " + lectureId));
        if (!hasLectureAccess(context, lecture)) {
            throw new IllegalArgumentException("Lecture is not assigned to current student: " + lectureId);
        }
        return lecture;
    }

    private TestAssignment requireAccessibleTestAssignment(StudentLearningContext context, Integer assignmentId) {
        TestAssignment assignment = testAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Test assignment not found: " + assignmentId));
        if (!hasTestAssignmentAccess(context, assignment)) {
            throw new IllegalArgumentException("Test assignment is not available for current student: " + assignmentId);
        }
        return assignment;
    }

    private List<PublicLectureResponse> accessibleLecturesForSubject(StudentLearningContext context, Integer subjectId) {
        LinkedHashMap<Integer, Lecture> lecturesById = new LinkedHashMap<>();
        for (TeachingAssignment assignment : context.assignments()) {
            if (!subjectId.equals(subjectIdOf(context, assignment))) {
                continue;
            }

            List<LectureAssignment> lectureAssignments = activeLectureAssignments(assignment.getId());
            if (!lectureAssignments.isEmpty()) {
                for (LectureAssignment lectureAssignment : lectureAssignments) {
                    lectureRepository.findById(lectureAssignment.getCourseLectureId())
                            .filter(lecture -> subjectId.equals(subjectIdOfLecture(lecture)))
                            .ifPresent(lecture -> lecturesById.put(lecture.getId(), lecture));
                }
                continue;
            }

            if (assignment.getCourseVersionId() != null) {
                lectureRepository.findByCourseVersionIdOrderByOrdinalAsc(assignment.getCourseVersionId())
                        .forEach(lecture -> lecturesById.put(lecture.getId(), lecture));
                continue;
            }

            if (assignment.getSubjectMembershipId() != null) {
                lectureRepository.findBySubjectMembershipIdOrderByOrdinalAsc(assignment.getSubjectMembershipId())
                        .forEach(lecture -> lecturesById.put(lecture.getId(), lecture));
                continue;
            }

            Integer teacherPersonId = teacherPersonIdOf(context, assignment);
            for (CourseVersion version : publishedCourseVersionsForSubject(subjectId, teacherPersonId)) {
                lectureRepository.findByCourseVersionIdOrderByOrdinalAsc(version.getId())
                        .forEach(lecture -> lecturesById.put(lecture.getId(), lecture));
            }
        }

        return lecturesById.values()
                .stream()
                .map(this::lectureResponse)
                .sorted(Comparator.comparing(PublicLectureResponse::courseName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PublicLectureResponse::versionNumber)
                        .thenComparing(PublicLectureResponse::ordinal))
                .toList();
    }

    private boolean hasLectureAccess(StudentLearningContext context, Lecture lecture) {
        return context.assignments()
                .stream()
                .anyMatch(assignment -> assignmentGrantsLecture(context, assignment, lecture));
    }

    private List<Test> linkedTestsForLecture(Lecture lecture) {
        LinkedHashMap<Integer, Test> testsById = new LinkedHashMap<>();
        if (lecture.getLinkedTestId() != null) {
            testRepository.findById(lecture.getLinkedTestId())
                    .ifPresent(test -> testsById.put(test.getId(), test));
        }
        for (Test test : lectureTestLinkService.findTestsByLectureId(lecture.getId())) {
            testsById.putIfAbsent(test.getId(), test);
        }
        return testsById.values().stream().toList();
    }

    private boolean assignmentGrantsLecture(StudentLearningContext context, TeachingAssignment assignment, Lecture lecture) {
        Integer assignmentSubjectId = subjectIdOf(context, assignment);
        Integer lectureSubjectId = subjectIdOfLecture(lecture);
        if (!Objects.equals(assignmentSubjectId, lectureSubjectId)) {
            return false;
        }

        List<LectureAssignment> lectureAssignments = activeLectureAssignments(assignment.getId());
        if (!lectureAssignments.isEmpty()) {
            return lectureAssignments.stream()
                    .anyMatch(item -> Objects.equals(item.getCourseLectureId(), lecture.getId()));
        }

        if (assignment.getCourseVersionId() != null) {
            return lecture.getCourseVersionId() != null
                    && Objects.equals(assignment.getCourseVersionId(), lecture.getCourseVersionId());
        }

        if (lecture.getSubjectMembershipId() != null) {
            return Objects.equals(assignment.getSubjectMembershipId(), lecture.getSubjectMembershipId());
        }

        return publishedCourseVersionIdsForSubject(
                lectureSubjectId,
                teacherPersonIdOf(context, assignment)
        ).contains(lecture.getCourseVersionId());
    }

    private boolean hasTestAssignmentAccess(StudentLearningContext context, TestAssignment assignment) {
        Instant now = Instant.now();
        if (!isActive(assignment, now)) {
            return false;
        }
        if (assignment.getTeachingAssignmentId() != null) {
            return context.assignmentsById().containsKey(assignment.getTeachingAssignmentId());
        }
        if (assignment.getCourseLectureId() != null) {
            return lectureRepository.findById(assignment.getCourseLectureId())
                    .map(lecture -> hasLectureAccess(context, lecture))
                    .orElse(false);
        }
        if (assignment.getCourseVersionId() != null) {
            Integer subjectId = subjectIdForCourseVersion(assignment.getCourseVersionId());
            return context.assignments()
                    .stream()
                    .filter(teachingAssignment -> Objects.equals(subjectIdOf(context, teachingAssignment), subjectId))
                    .anyMatch(teachingAssignment -> {
                        if (teachingAssignment.getCourseVersionId() != null) {
                            return Objects.equals(teachingAssignment.getCourseVersionId(), assignment.getCourseVersionId());
                        }
                        return publishedCourseVersionIdsForSubject(
                                subjectId,
                                teacherPersonIdOf(context, teachingAssignment)
                        ).contains(assignment.getCourseVersionId());
                    });
        }
        return false;
    }

    private TestAssignment activeAccessibleAssignment(StudentLearningContext context, Integer testId) {
        return testAssignmentRepository.findByTestId(testId)
                .stream()
                .filter(assignment -> hasTestAssignmentAccess(context, assignment))
                .min(Comparator.comparing(TestAssignment::getAvailableUntilUtc))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active test assignment was not found for current student and test: " + testId
                ));
    }

    private Optional<TestAssignment> accessibleAssignmentForLinkedTest(StudentLearningContext context, Integer testId) {
        return testAssignmentRepository.findByTestId(testId)
                .stream()
                .filter(assignment -> hasTestAssignmentAccess(context, assignment))
                .min(Comparator.comparing(TestAssignment::getAvailableUntilUtc));
    }

    private Optional<Integer> findEnrollmentIdForTestAssignment(StudentLearningContext context, TestAssignment assignment) {
        return context.enrollments()
                .stream()
                .filter(enrollment -> enrollmentMatchesTestAssignment(context, enrollment, assignment))
                .map(TeachingAssignmentEnrollment::getId)
                .min(Integer::compareTo);
    }

    private boolean enrollmentMatchesTestAssignment(StudentLearningContext context,
                                                    TeachingAssignmentEnrollment enrollment,
                                                    TestAssignment assignment) {
        TeachingAssignment teachingAssignment = context.assignmentsById().get(enrollment.getTeachingAssignmentId());
        if (teachingAssignment == null) {
            return false;
        }

        if (assignment.getTeachingAssignmentId() != null) {
            return Objects.equals(enrollment.getTeachingAssignmentId(), assignment.getTeachingAssignmentId());
        }
        if (assignment.getCourseLectureId() != null) {
            return lectureRepository.findById(assignment.getCourseLectureId())
                    .map(lecture -> assignmentGrantsLecture(context, teachingAssignment, lecture))
                    .orElse(false);
        }
        if (assignment.getCourseVersionId() != null) {
            Integer assignmentSubjectId = subjectIdOf(context, teachingAssignment);
            Integer courseVersionSubjectId = subjectIdForCourseVersion(assignment.getCourseVersionId());
            if (!Objects.equals(assignmentSubjectId, courseVersionSubjectId)) {
                return false;
            }
            if (teachingAssignment.getCourseVersionId() != null) {
                return Objects.equals(teachingAssignment.getCourseVersionId(), assignment.getCourseVersionId());
            }
            return publishedCourseVersionIdsForSubject(
                    courseVersionSubjectId,
                    teacherPersonIdOf(context, teachingAssignment)
            ).contains(assignment.getCourseVersionId());
        }
        return false;
    }

    private Integer subjectIdOf(StudentLearningContext context, TeachingAssignment assignment) {
        SubjectMembership membership = context.subjectMembershipsById().get(assignment.getSubjectMembershipId());
        return membership == null ? null : membership.getSubjectId();
    }

    private Integer teacherPersonIdOf(StudentLearningContext context, TeachingAssignment assignment) {
        SubjectMembership membership = context.subjectMembershipsById().get(assignment.getSubjectMembershipId());
        return membership == null ? null : membership.getPersonId();
    }

    private Integer subjectIdOfLecture(Lecture lecture) {
        if (lecture.getSubjectId() != null) {
            return lecture.getSubjectId();
        }
        if (lecture.getCourseVersionId() != null) {
            return subjectIdForCourseVersion(lecture.getCourseVersionId());
        }
        throw new IllegalArgumentException("Lecture is not bound to a subject: " + lecture.getId());
    }

    private Integer subjectIdForCourseVersion(Integer courseVersionId) {
        CourseVersion version = courseVersionRepository.findById(courseVersionId)
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + courseVersionId));
        CourseTemplate template = courseTemplateRepository.findById(version.getCourseTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Course template not found: " + version.getCourseTemplateId()));
        return template.getSubjectId();
    }

    private List<CourseVersion> publishedCourseVersionsForSubject(Integer subjectId, Integer authorPersonId) {
        if (subjectId == null || authorPersonId == null) {
            return List.of();
        }
        List<CourseVersion> versions = new ArrayList<>();
        for (CourseTemplate template : courseTemplateRepository.findByFilters(subjectId, authorPersonId, false)) {
            courseVersionRepository.findByCourseTemplateIdOrderByVersionNumberDesc(template.getId())
                    .stream()
                    .filter(CourseVersion::isPublished)
                    .forEach(versions::add);
        }
        return versions;
    }

    private Set<Integer> publishedCourseVersionIdsForSubject(Integer subjectId, Integer authorPersonId) {
        return publishedCourseVersionsForSubject(subjectId, authorPersonId)
                .stream()
                .map(CourseVersion::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<LectureAssignment> activeLectureAssignments(Integer teachingAssignmentId) {
        Instant now = Instant.now();
        return lectureAssignmentRepository.findByTeachingAssignmentId(teachingAssignmentId)
                .stream()
                .filter(assignment -> assignment.getStatus() == ACTIVE_LECTURE_ASSIGNMENT_STATUS)
                .filter(assignment -> assignment.getAvailableFromUtc() == null || !now.isBefore(assignment.getAvailableFromUtc()))
                .filter(assignment -> assignment.getClosedAtUtc() == null || now.isBefore(assignment.getClosedAtUtc()))
                .toList();
    }

    private boolean isActiveStudentMembership(GroupMembership membership) {
        return membership.getRole() == GROUP_ROLE_STUDENT
                && membership.getStatus() == ACTIVE_GROUP_MEMBERSHIP_STATUS
                && membership.getRemovedAtUtc() == null;
    }

    private boolean isActiveTeachingAssignment(TeachingAssignment assignment) {
        return assignment.getStatus() == ACTIVE_TEACHING_ASSIGNMENT_STATUS;
    }

    private boolean isActiveEnrollment(TeachingAssignmentEnrollment enrollment) {
        return enrollment.getRemovedAtUtc() == null
                && (enrollment.getStatus() == 1 || enrollment.getStatus() == 2);
    }

    private PublicLectureResponse lectureResponse(Lecture lecture) {
        if (lecture.getCourseVersionId() == null) {
            Integer subjectId = subjectIdOfLecture(lecture);
            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));
            return new PublicLectureResponse(
                    lecture.getId(),
                    null,
                    subjectId,
                    null,
                    subject.getName(),
                    null,
                    0,
                    null,
                    lecture.getOrdinal(),
                    lecture.getTitle(),
                    lecture.getDescription(),
                    lecture.getContentFolderKey(),
                    lecture.getContentFolderKey(),
                    lecture.isPublicVisible()
            );
        }

        CourseVersion version = courseVersionRepository.findById(lecture.getCourseVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Course version not found: " + lecture.getCourseVersionId()));
        CourseTemplate template = courseTemplateRepository.findById(version.getCourseTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Course template not found: " + version.getCourseTemplateId()));
        return new PublicLectureResponse(
                lecture.getId(),
                lecture.getCourseVersionId(),
                template.getSubjectId(),
                template.getId(),
                template.getName(),
                version.getId(),
                version.getVersionNumber(),
                version.getTitle(),
                lecture.getOrdinal(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getContentFolderKey(),
                lecture.getContentFolderKey(),
                lecture.isPublicVisible()
        );
    }

    private PublicTestResponse testResponse(Test test,
                                            Integer assignmentId,
                                            boolean available,
                                            String statusMessage) {
        return new PublicTestResponse(
                test.getId(),
                assignmentId,
                test.getTitle(),
                test.getDescription(),
                test.getDuration() == null ? null : test.getDuration().toString(),
                test.getAttemptsAllowed(),
                test.getQuestionCount(),
                available,
                statusMessage
        );
    }

    private PublicTestResponse testResponse(Test test, Integer assignmentId) {
        return testResponse(test, assignmentId, true, null);
    }

    private PublicLectureMaterialResponse lectureMaterialResponse(LectureMaterial material) {
        return new PublicLectureMaterialResponse(
                material.getId(),
                material.getFileName(),
                material.getContentType(),
                material.getSizeBytes(),
                material.getUploadedAtUtc()
        );
    }

    private PublicQuestionResponse questionResponse(Question question) {
        List<PublicQuestionOptionResponse> options = questionOptionRepository.findByTestQuestionIdOrderByOrdinalAsc(question.getId())
                .stream()
                .map(option -> new PublicQuestionOptionResponse(option.getId(), option.getText(), option.getOrdinal()))
                .toList();
        List<QuestionTypeSupport.MatchingPair> matchingPairs = QuestionTypeSupport.parseMatchingPairs(question.getCorrectAnswer());
        List<PublicQuestionMatchingPromptResponse> matchingPrompts = matchingPairs.stream()
                .map(pair -> new PublicQuestionMatchingPromptResponse(pair.ordinal(), pair.left()))
                .toList();
        List<String> matchingOptions = matchingPairs.stream()
                .map(QuestionTypeSupport.MatchingPair::right)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(matchingOptions);
        return new PublicQuestionResponse(
                question.getId(),
                question.getType(),
                question.getQuestion(),
                question.getQuestion(),
                question.getPoints(),
                question.getOrdinal(),
                options,
                matchingPrompts,
                List.copyOf(matchingOptions)
        );
    }

    private static boolean isActive(TestAssignment assignment, Instant now) {
        return assignment.getStatus() == 2
                && !now.isBefore(assignment.getAvailableFromUtc())
                && now.isBefore(assignment.getAvailableUntilUtc());
    }

    private String givenAnswerDisplay(Question question, String answer, List<Long> selectedOptionIds) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            String display = QuestionTypeSupport.displayMatchingPairs(answer);
            return display.isBlank() ? null : display;
        }
        List<QuestionOption> selectedOptions = questionOptionRepository.findAllById(
                selectedOptionIds == null ? Set.of() : selectedOptionIds.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
        if (!selectedOptions.isEmpty()) {
            return selectedOptions.stream()
                    .sorted(Comparator.comparing(QuestionOption::getOrdinal))
                    .map(QuestionOption::getText)
                    .collect(Collectors.joining(", "));
        }
        return answer == null || answer.isBlank() ? null : answer.trim();
    }

    private String correctAnswerDisplay(Question question) {
        if (QuestionTypeSupport.isMatching(question.getType())) {
            return QuestionTypeSupport.displayMatchingPairs(question.getCorrectAnswer());
        }
        if (!QuestionTypeSupport.usesSelectableOptions(question.getType())) {
            return question.getCorrectAnswer();
        }
        List<QuestionOption> correctOptions = questionOptionRepository.findByTestQuestionIdOrderByOrdinalAsc(question.getId())
                .stream()
                .filter(QuestionOption::isCorrect)
                .toList();
        if (!correctOptions.isEmpty()) {
            return correctOptions.stream()
                    .map(QuestionOption::getText)
                    .collect(Collectors.joining(", "));
        }
        return question.getCorrectAnswer();
    }

    private Integer currentPersonId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication is required.");
        }
        Integer personId = userRegisterService.currentUser(authentication.getName()).personId();
        if (personId == null) {
            throw new IllegalArgumentException("Current user is not bound to a person.");
        }
        return personId;
    }

    private record StudentLearningContext(Integer personId,
                                          List<TeachingAssignment> assignments,
                                          List<TeachingAssignmentEnrollment> enrollments,
                                          Map<Integer, TeachingAssignment> assignmentsById,
                                          Map<Integer, SubjectMembership> subjectMembershipsById) {
    }

    public record PublicLectureResponse(Integer id,
                                        Integer courseVersionId,
                                        Integer subjectId,
                                        Integer courseTemplateId,
                                        String courseName,
                                        Integer versionId,
                                        int versionNumber,
                                        String versionTitle,
                                        int ordinal,
                                        String title,
                                        String description,
                                        String content,
                                        String contentFolderKey,
                                        boolean publicVisible) {
    }

    public record PublicSubjectResponse(Integer id, String name, String description) {
    }

    public record PublicTestResponse(Integer id,
                                     Integer assignmentId,
                                     String title,
                                     String description,
                                     String duration,
                                     int attemptsAllowed,
                                     int questionCount,
                                     boolean available,
                                     String statusMessage) {
    }

    public record PublicLectureMaterialResponse(Integer id,
                                                String fileName,
                                                String contentType,
                                                long sizeBytes,
                                                Instant uploadedAtUtc) {
    }

    public record PublicTestLoadResponse(PublicTestResponse test, List<PublicQuestionResponse> questions) {
    }

    public record PublicTestAttemptLoadResponse(Integer attemptId,
                                                Integer assignmentId,
                                                PublicTestResponse test,
                                                List<PublicQuestionResponse> questions) {
    }

    public record PublicQuestionResponse(Long id,
                                         int type,
                                         String text,
                                         String question,
                                         BigDecimal points,
                                         int ordinal,
                                         List<PublicQuestionOptionResponse> options,
                                         List<PublicQuestionMatchingPromptResponse> matchingPrompts,
                                         List<String> matchingOptions) {
    }

    public record PublicQuestionOptionResponse(Long id, String text, int ordinal) {
    }

    public record PublicQuestionMatchingPromptResponse(int ordinal, String text) {
    }

    public record PublicSubmitRequest(List<Long> questionIds,
                                      List<String> answers,
                                      List<List<Long>> selectedOptionIds) {
    }

    public record PublicSubmitResponse(Integer attemptId,
                                       BigDecimal score,
                                       int correctCount,
                                       int totalCount,
                                       List<PublicSubmitDetailResponse> details) {
    }

    public record PublicSubmitDetailResponse(String questionText,
                                             String givenAnswer,
                                             String correctAnswer,
                                             boolean correct) {
    }
}
