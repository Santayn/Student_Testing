package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.question.SelectedOption;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.models.test.TestQuestionSelectionRule;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.models.teacher.TeachingAssignmentEnrollment;
import org.santayn.testing.repository.CourseVersionRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.SelectedOptionRepository;
import org.santayn.testing.repository.TeachingAssignmentRepository;
import org.santayn.testing.repository.TeachingAssignmentEnrollmentRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestService {


    private final TestRepository testRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionResponseRepository questionResponseRepository;
    private final SelectedOptionRepository selectedOptionRepository;
    private final PersonRepository personRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final TeachingAssignmentEnrollmentRepository teachingAssignmentEnrollmentRepository;
    private final CourseVersionRepository courseVersionRepository;
    private final LectureRepository lectureRepository;
    private final TestQuestionSelectionRuleRepository selectionRuleRepository;
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public List<Test> findAll() {
        return sortTests(testRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<Test> findAll(Integer subjectId) {
        return subjectId == null
                ? findAll()
                : sortTests(testRepository.findBySubjectId(subjectId));
    }

    @Transactional(readOnly = true)
    public Test get(Integer id) {
        return testRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean belongsToSubject(Integer testId, Integer subjectId) {
        if (subjectId == null) {
            return testRepository.existsById(testId);
        }
        return testRepository.existsInSubject(testId, subjectId);
    }

    @Transactional
    public Test create(String title, String description, LocalTime duration, int attemptsAllowed, int questionCount) {
        return create(title, description, duration, attemptsAllowed, questionCount, List.of());
    }

    @Transactional
    public Test create(String title,
                       String description,
                       LocalTime duration,
                       int attemptsAllowed,
                       int questionCount,
                       List<SelectionRuleInput> selectionRules) {
        Test test = new Test();
        test.setTitle(FacultyService.requireText(title, "Title"));
        test.setDescription(FacultyService.trimToNull(description));
        test.setDuration(duration);
        test.setAttemptsAllowed(Math.max(1, attemptsAllowed));
        test.setQuestionCount(Math.max(1, questionCount));
        test = testRepository.save(test);
        replaceSelectionRules(test, selectionRules);
        return test;
    }

    @Transactional
    public Test update(Integer testId, String title, String description, LocalTime duration, int attemptsAllowed, int questionCount) {
        return update(testId, title, description, duration, attemptsAllowed, questionCount, null);
    }

    @Transactional
    public Test update(Integer testId,
                       String title,
                       String description,
                       LocalTime duration,
                       int attemptsAllowed,
                       int questionCount,
                       List<SelectionRuleInput> selectionRules) {
        Test test = get(testId);
        test.setTitle(FacultyService.requireText(title, "Title"));
        test.setDescription(FacultyService.trimToNull(description));
        test.setDuration(duration);
        test.setAttemptsAllowed(Math.max(1, attemptsAllowed));
        test.setQuestionCount(Math.max(1, questionCount));
        if (selectionRules != null) {
            replaceSelectionRules(test, selectionRules);
        }
        return test;
    }

    @Transactional(readOnly = true)
    public List<TestQuestionSelectionRule> findSelectionRules(Integer testId) {
        if (!testRepository.existsById(testId)) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }
        return selectionRuleRepository.findByTestIdOrderByOrdinalAsc(testId);
    }

    @Transactional
    public List<TestQuestionSelectionRule> replaceSelectionRules(Integer testId, List<SelectionRuleInput> selectionRules) {
        Test test = get(testId);
        return replaceSelectionRules(test, selectionRules);
    }

    @Transactional(readOnly = true)
    public List<TestAssignment> findAssignments(Integer testId,
                                                Integer courseVersionId,
                                                Integer courseLectureId,
                                                Integer teachingAssignmentId) {
        if (testId != null) {
            return testAssignmentRepository.findByTestId(testId);
        }
        if (courseVersionId != null) {
            return testAssignmentRepository.findByCourseVersionId(courseVersionId);
        }
        if (courseLectureId != null) {
            return testAssignmentRepository.findByCourseLectureId(courseLectureId);
        }
        if (teachingAssignmentId != null) {
            return testAssignmentRepository.findByTeachingAssignmentId(teachingAssignmentId);
        }
        return testAssignmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TestAssignment getAssignment(Integer id) {
        return testAssignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test assignment not found: " + id));
    }

    @Transactional
    public TestAssignment assign(Integer testId,
                                 int scope,
                                 Integer courseVersionId,
                                 Integer courseLectureId,
                                 Integer teachingAssignmentId,
                                 Instant availableFromUtc,
                                 Instant availableUntilUtc,
                                 int status) {
        if (!testRepository.existsById(testId)) {
            throw new IllegalArgumentException("Test not found: " + testId);
        }
        requireAssignmentScope(scope, courseVersionId, courseLectureId, teachingAssignmentId);
        requireAssignmentTimeRange(availableFromUtc, availableUntilUtc);
        requireAssignmentUnique(testId, scope, courseVersionId, courseLectureId, teachingAssignmentId);

        TestAssignment assignment = new TestAssignment();
        assignment.setTestId(testId);
        assignment.setScope(scope);
        assignment.setCourseVersionId(courseVersionId);
        assignment.setCourseLectureId(courseLectureId);
        assignment.setTeachingAssignmentId(teachingAssignmentId);
        assignment.setAvailableFromUtc(availableFromUtc);
        assignment.setAvailableUntilUtc(availableUntilUtc);
        assignment.setStatus(normalizeStatus(status, 1, 4, 1));
        return testAssignmentRepository.save(assignment);
    }

    @Transactional
    public TestAssignment updateAssignment(Integer assignmentId,
                                           int scope,
                                           Integer courseVersionId,
                                           Integer courseLectureId,
                                           Integer teachingAssignmentId,
                                           Instant availableFromUtc,
                                           Instant availableUntilUtc,
                                           int status) {
        TestAssignment assignment = getAssignment(assignmentId);
        requireAssignmentScope(scope, courseVersionId, courseLectureId, teachingAssignmentId);
        requireAssignmentTimeRange(availableFromUtc, availableUntilUtc);
        requireAssignmentUnique(assignment.getTestId(), scope, courseVersionId, courseLectureId, teachingAssignmentId, assignmentId);

        assignment.setScope(scope);
        assignment.setCourseVersionId(courseVersionId);
        assignment.setCourseLectureId(courseLectureId);
        assignment.setTeachingAssignmentId(teachingAssignmentId);
        assignment.setAvailableFromUtc(availableFromUtc);
        assignment.setAvailableUntilUtc(availableUntilUtc);
        assignment.setStatus(normalizeStatus(status, 1, 4, 1));
        return assignment;
    }

    @Transactional
    public TestAssignment updateAssignmentStatus(Integer assignmentId, int status) {
        TestAssignment assignment = getAssignment(assignmentId);
        assignment.setStatus(normalizeStatus(status, 1, 4, 1));
        return assignment;
    }

    @Transactional(readOnly = true)
    public List<TestAttempt> findAttempts(Integer testAssignmentId, Integer personId) {
        if (testAssignmentId != null && personId != null) {
            return testAttemptRepository.findByTestAssignmentIdAndPersonId(testAssignmentId, personId);
        }
        if (testAssignmentId != null) {
            return testAttemptRepository.findByTestAssignmentId(testAssignmentId);
        }
        if (personId != null) {
            return testAttemptRepository.findByPersonId(personId);
        }
        return testAttemptRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TestAttempt getAttempt(Integer id) {
        return testAttemptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test attempt not found: " + id));
    }

    @Transactional
    public TestAttempt startAttempt(Integer testAssignmentId, Integer personId, Integer teachingAssignmentEnrollmentId) {
        TestAssignment assignment = testAssignmentRepository.findById(testAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Test assignment not found: " + testAssignmentId));
        if (!personRepository.existsById(personId)) {
            throw new IllegalArgumentException("Person not found: " + personId);
        }
        TeachingAssignmentEnrollment enrollment = null;
        if (teachingAssignmentEnrollmentId != null) {
            enrollment = teachingAssignmentEnrollmentRepository.findById(teachingAssignmentEnrollmentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Teaching assignment enrollment not found: " + teachingAssignmentEnrollmentId
                    ));
            requireEnrollmentBelongsToPerson(enrollment, personId);
        }
        requireAssignmentAvailable(assignment);

        Test test = get(assignment.getTestId());
        long startedAttempts = testAttemptRepository.countByTestAssignmentIdAndPersonId(testAssignmentId, personId);
        if (startedAttempts >= test.getAttemptsAllowed()) {
            throw new IllegalArgumentException("Attempt limit exceeded for test assignment: " + testAssignmentId);
        }

        TestAttempt attempt = new TestAttempt();
        attempt.setTestAssignmentId(testAssignmentId);
        attempt.setPersonId(personId);
        attempt.setTeachingAssignmentEnrollmentId(enrollment == null ? null : enrollment.getId());
        attempt.setOrdinal((int) startedAttempts + 1);
        attempt.setStatus(1);
        attempt.setStartedAt(Instant.now());
        return testAttemptRepository.save(attempt);
    }

    @Transactional
    public TestAttemptQuestionSet startOrResumeAttemptWithRandomQuestions(Integer testAssignmentId,
                                                                          Integer personId,
                                                                          Integer teachingAssignmentEnrollmentId) {
        TestAssignment assignment = testAssignmentRepository.findById(testAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Test assignment not found: " + testAssignmentId));
        if (!personRepository.existsById(personId)) {
            throw new IllegalArgumentException("Person not found: " + personId);
        }
        if (teachingAssignmentEnrollmentId != null) {
            TeachingAssignmentEnrollment enrollment = teachingAssignmentEnrollmentRepository.findById(teachingAssignmentEnrollmentId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Teaching assignment enrollment not found: " + teachingAssignmentEnrollmentId
                    ));
            requireEnrollmentBelongsToPerson(enrollment, personId);
        }
        requireAssignmentAvailable(assignment);

        TestAttempt existingAttempt = testAttemptRepository.findByTestAssignmentIdAndPersonId(testAssignmentId, personId)
                .stream()
                .filter(attempt -> attempt.getStatus() == 1)
                .min(Comparator.comparing(TestAttempt::getStartedAt))
                .orElse(null);
        if (existingAttempt != null) {
            List<Question> existingQuestions = questionsForAttempt(existingAttempt.getId());
            if (!existingQuestions.isEmpty()) {
                return new TestAttemptQuestionSet(existingAttempt, existingQuestions);
            }
            List<Question> selectedQuestions = randomQuestionsForTest(assignment.getTestId());
            createEmptyResponses(existingAttempt.getId(), selectedQuestions);
            return new TestAttemptQuestionSet(existingAttempt, selectedQuestions);
        }

        TestAttempt attempt = startAttempt(testAssignmentId, personId, teachingAssignmentEnrollmentId);
        List<Question> selectedQuestions = randomQuestionsForTest(assignment.getTestId());
        createEmptyResponses(attempt.getId(), selectedQuestions);
        return new TestAttemptQuestionSet(attempt, selectedQuestions);
    }

    @Transactional(readOnly = true)
    public List<Question> randomQuestionsForTest(Integer testId) {
        return selectRandomQuestionsForTest(testId);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> findResponses(Integer testAttemptId) {
        return questionResponseRepository.findByTestAttemptIdOrderByIdAsc(testAttemptId);
    }

    @Transactional(readOnly = true)
    public List<SelectedOption> findSelectedOptions(Long questionResponseId) {
        return selectedOptionRepository.findByQuestionResponseId(questionResponseId);
    }

    @Transactional
    public QuestionResponse submitResponse(Integer testAttemptId,
                                           Long testQuestionId,
                                           String answerText,
                                           List<Long> selectedOptionIds,
                                           BigDecimal awardedPoints,
                                           Boolean correct) {
        TestAttempt attempt = getAttempt(testAttemptId);
        if (attempt.getStatus() != 1) {
            throw new IllegalArgumentException("Test attempt is not in progress: " + testAttemptId);
        }
        TestAssignment assignment = testAssignmentRepository.findById(attempt.getTestAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Test assignment not found: " + attempt.getTestAssignmentId()));
        Question question = questionRepository.findById(testQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Test question not found: " + testQuestionId));

        QuestionResponse response = questionResponseRepository
                .findByTestAttemptIdAndTestQuestionId(testAttemptId, testQuestionId)
                .orElse(null);
        if (!questionBelongsToAssignmentTest(question, assignment)) {
            throw new IllegalArgumentException("Question does not belong to attempt test.");
        }
        if (response == null) {
            response = new QuestionResponse();
            response.setTestAttemptId(testAttemptId);
            response.setTestQuestionId(testQuestionId);
        }
        response.setAnswerText(FacultyService.trimToNull(answerText));
        applyAutomaticScore(response, question, selectedOptionIds, awardedPoints, correct);
        response = questionResponseRepository.save(response);
        replaceSelectedOptions(response.getId(), question, selectedOptionIds);
        return response;
    }

    @Transactional
    public TestAttempt completeAttempt(Integer testAttemptId, BigDecimal score, Integer status) {
        TestAttempt attempt = getAttempt(testAttemptId);
        int normalizedStatus = normalizeStatus(status, 2, 3, 2);
        attempt.setStatus(normalizedStatus);
        attempt.setCompletedAt(Instant.now());
        attempt.setScore(score == null ? calculateScore(testAttemptId) : requireNonNegative(score, "Score"));
        return attempt;
    }

    private void applyAutomaticScore(QuestionResponse response,
                                     Question question,
                                     List<Long> selectedOptionIds,
                                     BigDecimal awardedPoints,
                                     Boolean correct) {
        if (awardedPoints != null || correct != null) {
            response.setAwardedPoints(awardedPoints == null ? null : requireNonNegative(awardedPoints, "AwardedPoints"));
            response.setCorrect(correct);
            return;
        }

        if (QuestionTypeSupport.usesSelectableOptions(question.getType())) {
            List<QuestionOption> questionOptions = questionOptionRepository.findByTestQuestionIdOrderByOrdinalAsc(question.getId());
            if (questionOptions.isEmpty()) {
                response.setCorrect(false);
                response.setAwardedPoints(BigDecimal.ZERO);
                return;
            }

            Set<Long> selectedIds = new HashSet<>(selectedOptionIds == null ? List.of() : selectedOptionIds);
            Set<Long> correctIds = questionOptions
                    .stream()
                    .filter(QuestionOption::isCorrect)
                    .map(QuestionOption::getId)
                    .collect(Collectors.toSet());
            boolean automaticallyCorrect = !correctIds.isEmpty() && selectedIds.equals(correctIds);
            response.setCorrect(automaticallyCorrect);
            response.setAwardedPoints(automaticallyCorrect ? question.getPoints() : BigDecimal.ZERO);
            return;
        }

        if (QuestionTypeSupport.isMatching(question.getType())) {
            boolean automaticallyCorrect = isMatchingAnswerCorrect(question.getCorrectAnswer(), response.getAnswerText());
            response.setCorrect(automaticallyCorrect);
            response.setAwardedPoints(automaticallyCorrect ? question.getPoints() : BigDecimal.ZERO);
            return;
        }

        boolean automaticallyCorrect = isTextAnswerCorrect(question.getCorrectAnswer(), response.getAnswerText());
        response.setCorrect(automaticallyCorrect);
        response.setAwardedPoints(automaticallyCorrect ? question.getPoints() : BigDecimal.ZERO);
    }

    private void replaceSelectedOptions(Long questionResponseId, Question question, List<Long> selectedOptionIds) {
        if (selectedOptionIds == null) {
            return;
        }

        List<SelectedOption> existing = selectedOptionRepository.findByQuestionResponseId(questionResponseId);
        selectedOptionRepository.deleteAll(existing);
        selectedOptionRepository.flush();

        Set<Long> selectedIds = new HashSet<>(selectedOptionIds);
        if (selectedIds.isEmpty()) {
            return;
        }

        List<QuestionOption> options = questionOptionRepository.findAllById(selectedIds);
        if (options.size() != selectedIds.size()) {
            throw new IllegalArgumentException("One or more question options were not found.");
        }
        for (QuestionOption option : options) {
            if (!question.getId().equals(option.getTestQuestionId())) {
                throw new IllegalArgumentException("Question option does not belong to the response question.");
            }
            SelectedOption selectedOption = new SelectedOption();
            selectedOption.setQuestionResponseId(questionResponseId);
            selectedOption.setQuestionOptionId(option.getId());
            selectedOptionRepository.save(selectedOption);
        }
    }

    private boolean isMatchingAnswerCorrect(String expectedRaw, String actualRaw) {
        List<QuestionTypeSupport.MatchingPair> expectedPairs = QuestionTypeSupport.parseMatchingPairs(expectedRaw);
        List<QuestionTypeSupport.MatchingPair> actualPairs = QuestionTypeSupport.parseMatchingPairs(actualRaw);
        if (expectedPairs.isEmpty() || actualPairs.isEmpty()) {
            return false;
        }

        Map<Integer, String> expectedByOrdinal = new LinkedHashMap<>();
        for (QuestionTypeSupport.MatchingPair pair : expectedPairs) {
            String normalizedRight = QuestionTypeSupport.normalizeComparable(pair.right());
            if (pair.ordinal() <= 0 || normalizedRight == null) {
                return false;
            }
            expectedByOrdinal.put(pair.ordinal(), normalizedRight);
        }

        Map<Integer, String> actualByOrdinal = new LinkedHashMap<>();
        for (QuestionTypeSupport.MatchingPair pair : actualPairs) {
            if (pair.ordinal() <= 0) {
                continue;
            }
            actualByOrdinal.put(pair.ordinal(), QuestionTypeSupport.normalizeComparable(pair.right()));
        }

        if (!actualByOrdinal.keySet().containsAll(expectedByOrdinal.keySet())) {
            return false;
        }

        for (Map.Entry<Integer, String> entry : expectedByOrdinal.entrySet()) {
            String actualValue = actualByOrdinal.get(entry.getKey());
            if (!Objects.equals(entry.getValue(), actualValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTextAnswerCorrect(String expectedRaw, String actualRaw) {
        return TextAnswerEvaluator.isCorrect(expectedRaw, actualRaw);
    }

    private BigDecimal calculateScore(Integer testAttemptId) {
        return questionResponseRepository.findByTestAttemptId(testAttemptId)
                .stream()
                .map(QuestionResponse::getAwardedPoints)
                .filter(points -> points != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Question> selectRandomQuestionsForTest(Integer testId) {
        Test test = get(testId);
        List<TestQuestionSelectionRule> rules = selectionRuleRepository.findByTestIdOrderByOrdinalAsc(testId);
        if (!rules.isEmpty()) {
            List<Question> selectedQuestions = new ArrayList<>();
            Set<Long> selectedQuestionIds = new HashSet<>();
            for (TestQuestionSelectionRule rule : rules) {
                List<Question> selectedForRule = randomQuestionsForRule(rule, selectedQuestionIds);
                selectedQuestions.addAll(selectedForRule);
                selectedForRule.stream().map(Question::getId).forEach(selectedQuestionIds::add);
            }
            if (selectedQuestions.isEmpty()) {
                throw new IllegalArgumentException("Test has no active questions for configured topics: " + testId);
            }
            Collections.shuffle(selectedQuestions);
            return List.copyOf(selectedQuestions);
        }

        List<Question> questions = new ArrayList<>(questionRepository.findByTestIdAndActiveTrueOrderByOrdinalAsc(testId));
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("Test has no active questions: " + testId);
        }

        Collections.shuffle(questions);
        int limit = Math.min(test.getQuestionCount(), questions.size());
        return List.copyOf(questions.subList(0, limit));
    }

    private List<Question> randomQuestionsForRule(TestQuestionSelectionRule rule, Set<Long> alreadySelectedQuestionIds) {
        List<Question> selected = new ArrayList<>();
        selectRequiredQuestionType(rule, QuestionTypeSupport.TYPE_SINGLE, rule.getSingleAnswerQuestionCount(), selected, alreadySelectedQuestionIds);
        selectRequiredQuestionType(rule, QuestionTypeSupport.TYPE_MULTIPLE, rule.getMultipleAnswerQuestionCount(), selected, alreadySelectedQuestionIds);
        selectRequiredQuestionType(rule, QuestionTypeSupport.TYPE_MATCHING, rule.getMatchingQuestionCount(), selected, alreadySelectedQuestionIds);
        selectRequiredQuestionType(rule, QuestionTypeSupport.TYPE_TEXT, rule.getTextQuestionCount(), selected, alreadySelectedQuestionIds);

        int remaining = rule.getQuestionCount() - selected.size();
        if (remaining > 0) {
            List<Question> candidates = questionsForRule(rule)
                    .stream()
                    .filter(question -> !alreadySelectedQuestionIds.contains(question.getId()))
                    .filter(question -> selected.stream().noneMatch(selectedQuestion -> selectedQuestion.getId().equals(question.getId())))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(candidates);
            requireEnoughQuestions(rule, "any", candidates.size(), remaining);
            selected.addAll(candidates.subList(0, remaining));
        }

        return selected;
    }

    private void selectRequiredQuestionType(TestQuestionSelectionRule rule,
                                            int questionType,
                                            int count,
                                            List<Question> selected,
                                            Set<Long> alreadySelectedQuestionIds) {
        if (count <= 0) {
            return;
        }

        List<Question> candidates = questionsForRule(rule, questionType)
                .stream()
                .filter(question -> !alreadySelectedQuestionIds.contains(question.getId()))
                .filter(question -> selected.stream().noneMatch(selectedQuestion -> selectedQuestion.getId().equals(question.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(candidates);
        requireEnoughQuestions(rule, "type " + questionType, candidates.size(), count);
        selected.addAll(candidates.subList(0, count));
    }

    private static void requireEnoughQuestions(TestQuestionSelectionRule rule, String bucket, int available, int required) {
        if (available < required) {
            String target = rule.getTopicId() == null
                    ? "lecture " + rule.getCourseLectureId()
                    : "topic " + rule.getTopicId();
            throw new IllegalArgumentException(
                    "Not enough active questions for test " + rule.getTestId()
                            + ", " + target
                            + ", " + bucket
                            + ": required " + required
                            + ", available " + available
            );
        }
    }

    private List<Question> questionsForAttempt(Integer testAttemptId) {
        return questionResponseRepository.findByTestAttemptIdOrderByIdAsc(testAttemptId)
                .stream()
                .map(response -> questionRepository.findById(response.getTestQuestionId()).orElse(null))
                .filter(question -> question != null && question.isActive())
                .toList();
    }

    private void createEmptyResponses(Integer attemptId, List<Question> questions) {
        for (Question question : questions) {
            QuestionResponse response = new QuestionResponse();
            response.setTestAttemptId(attemptId);
            response.setTestQuestionId(question.getId());
            questionResponseRepository.save(response);
        }
    }

    private List<TestQuestionSelectionRule> replaceSelectionRules(Test test, List<SelectionRuleInput> selectionRules) {
        List<TestQuestionSelectionRule> existingRules = selectionRuleRepository.findByTestIdOrderByOrdinalAsc(test.getId());
        if (!existingRules.isEmpty()) {
            selectionRuleRepository.deleteAll(existingRules);
            selectionRuleRepository.flush();
        }

        if (selectionRules == null || selectionRules.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueTargets = new HashSet<>();
        List<TestQuestionSelectionRule> savedRules = new ArrayList<>();
        int ordinal = 1;
        int totalQuestionCount = 0;

        for (SelectionRuleInput input : selectionRules) {
            if (input == null) {
                throw new IllegalArgumentException("Selection rule must not be null.");
            }
            SelectionRuleContext context = resolveSelectionRuleContext(input.courseLectureId(), input.topicId());
            String targetKey = context.topicId() == null
                    ? "lecture:" + context.courseLectureId()
                    : "topic:" + context.topicId();
            if (!uniqueTargets.add(targetKey)) {
                throw new AuthConflictException("Duplicate question topic in test selection rules: " + targetKey);
            }
            requireSelectionRuleCounts(input);
            requireSelectionRulePoolAvailability(test.getId(), context, input);

            TestQuestionSelectionRule rule = new TestQuestionSelectionRule();
            rule.setTestId(test.getId());
            rule.setCourseLectureId(context.courseLectureId());
            rule.setTopicId(context.topicId());
            rule.setQuestionCount(input.questionCount());
            rule.setTextQuestionCount(input.textQuestionCount());
            rule.setSingleAnswerQuestionCount(input.singleAnswerQuestionCount());
            rule.setMultipleAnswerQuestionCount(input.multipleAnswerQuestionCount());
            rule.setMatchingQuestionCount(input.matchingQuestionCount());
            rule.setOrdinal(input.ordinal() <= 0 ? ordinal : input.ordinal());
            savedRules.add(selectionRuleRepository.save(rule));
            totalQuestionCount += input.questionCount();
            ordinal++;
        }

        test.setQuestionCount(Math.max(1, totalQuestionCount));
        return savedRules;
    }

    private SelectionRuleContext resolveSelectionRuleContext(Integer courseLectureId, Integer topicId) {
        if (topicId == null) {
            if (courseLectureId == null || !lectureRepository.existsById(courseLectureId)) {
                throw new IllegalArgumentException("Course lecture not found: " + courseLectureId);
            }
            return new SelectionRuleContext(courseLectureId, null);
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
        Integer resolvedLectureId = topic.getCourseLectureId();
        if (courseLectureId != null && !lectureRepository.existsById(courseLectureId)) {
            throw new IllegalArgumentException("Course lecture not found: " + courseLectureId);
        }
        if (courseLectureId != null && resolvedLectureId != null && !resolvedLectureId.equals(courseLectureId)) {
            throw new IllegalArgumentException("Topic " + topicId + " does not belong to lecture " + courseLectureId + ".");
        }
        return new SelectionRuleContext(courseLectureId != null ? courseLectureId : resolvedLectureId, topic.getId());
    }

    private static void requireSelectionRuleCounts(SelectionRuleInput input) {
        if (input.questionCount() < 1) {
            throw new IllegalArgumentException("QuestionCount must be positive.");
        }
        if (input.textQuestionCount() < 0
                || input.singleAnswerQuestionCount() < 0
                || input.multipleAnswerQuestionCount() < 0
                || input.matchingQuestionCount() < 0) {
            throw new IllegalArgumentException("Question type counts must be non-negative.");
        }
        int requiredByType = input.textQuestionCount()
                + input.singleAnswerQuestionCount()
                + input.multipleAnswerQuestionCount()
                + input.matchingQuestionCount();
        if (requiredByType > input.questionCount()) {
            throw new IllegalArgumentException("Question type counts must not exceed total topic question count.");
        }
    }

    private void requireSelectionRulePoolAvailability(Integer testId,
                                                      SelectionRuleContext context,
                                                      SelectionRuleInput input) {
        if (context.topicId() == null) {
            return;
        }

        requireEnoughQuestions(
                topicRuleTarget(testId, context),
                "any",
                questionRepository.findByTopicIdAndTestIdIsNullAndActiveTrueOrderByOrdinalAsc(context.topicId()).size(),
                input.questionCount()
        );
        requireEnoughQuestions(
                topicRuleTarget(testId, context),
                QuestionTypeSupport.label(QuestionTypeSupport.TYPE_SINGLE),
                questionRepository.findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(context.topicId(), QuestionTypeSupport.TYPE_SINGLE).size(),
                input.singleAnswerQuestionCount()
        );
        requireEnoughQuestions(
                topicRuleTarget(testId, context),
                QuestionTypeSupport.label(QuestionTypeSupport.TYPE_MULTIPLE),
                questionRepository.findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(context.topicId(), QuestionTypeSupport.TYPE_MULTIPLE).size(),
                input.multipleAnswerQuestionCount()
        );
        requireEnoughQuestions(
                topicRuleTarget(testId, context),
                QuestionTypeSupport.label(QuestionTypeSupport.TYPE_MATCHING),
                questionRepository.findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(context.topicId(), QuestionTypeSupport.TYPE_MATCHING).size(),
                input.matchingQuestionCount()
        );
        requireEnoughQuestions(
                topicRuleTarget(testId, context),
                QuestionTypeSupport.label(QuestionTypeSupport.TYPE_TEXT),
                questionRepository.findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(context.topicId(), QuestionTypeSupport.TYPE_TEXT).size(),
                input.textQuestionCount()
        );
    }

    private void requireAssignmentScope(int scope,
                                        Integer courseVersionId,
                                        Integer courseLectureId,
                                        Integer teachingAssignmentId) {
        if (scope < 1 || scope > 4) {
            throw new IllegalArgumentException("Scope must be between 1 and 4.");
        }
        if (scope == 1 && (courseVersionId != null || courseLectureId != null || teachingAssignmentId != null)) {
            throw new IllegalArgumentException("Global test assignment must not have course targets.");
        }
        if (scope == 2) {
            if (courseVersionId == null || courseLectureId != null || teachingAssignmentId != null) {
                throw new IllegalArgumentException("Course test assignment requires CourseVersionId only.");
            }
            if (!courseVersionRepository.existsById(courseVersionId)) {
                throw new IllegalArgumentException("Course version not found: " + courseVersionId);
            }
        }
        if (scope == 3) {
            if (courseLectureId == null || courseVersionId != null || teachingAssignmentId != null) {
                throw new IllegalArgumentException("Lecture test assignment requires CourseLectureId only.");
            }
            if (!lectureRepository.existsById(courseLectureId)) {
                throw new IllegalArgumentException("Course lecture not found: " + courseLectureId);
            }
        }
        if (scope == 4) {
            if (teachingAssignmentId == null || courseVersionId != null || courseLectureId != null) {
                throw new IllegalArgumentException("Group test assignment requires TeachingAssignmentId only.");
            }
            if (!teachingAssignmentRepository.existsById(teachingAssignmentId)) {
                throw new IllegalArgumentException("Teaching assignment not found: " + teachingAssignmentId);
            }
        }
    }

    private void requireAssignmentUnique(Integer testId,
                                         int scope,
                                         Integer courseVersionId,
                                         Integer courseLectureId,
                                         Integer teachingAssignmentId) {
        boolean exists = switch (scope) {
            case 1 -> testAssignmentRepository.existsByTestIdAndScope(testId, scope);
            case 2 -> testAssignmentRepository.existsByTestIdAndCourseVersionId(testId, courseVersionId);
            case 3 -> testAssignmentRepository.existsByTestIdAndCourseLectureId(testId, courseLectureId);
            case 4 -> testAssignmentRepository.existsByTestIdAndTeachingAssignmentId(testId, teachingAssignmentId);
            default -> false;
        };
        if (exists) {
            throw new AuthConflictException("Test assignment already exists for this scope and target.");
        }
    }

    private void requireAssignmentUnique(Integer testId,
                                         int scope,
                                         Integer courseVersionId,
                                         Integer courseLectureId,
                                         Integer teachingAssignmentId,
                                         Integer excludedAssignmentId) {
        boolean exists = switch (scope) {
            case 1 -> testAssignmentRepository.existsByTestIdAndScopeAndIdNot(testId, scope, excludedAssignmentId);
            case 2 -> testAssignmentRepository.existsByTestIdAndCourseVersionIdAndIdNot(testId, courseVersionId, excludedAssignmentId);
            case 3 -> testAssignmentRepository.existsByTestIdAndCourseLectureIdAndIdNot(testId, courseLectureId, excludedAssignmentId);
            case 4 -> testAssignmentRepository.existsByTestIdAndTeachingAssignmentIdAndIdNot(testId, teachingAssignmentId, excludedAssignmentId);
            default -> false;
        };
        if (exists) {
            throw new AuthConflictException("Test assignment already exists for this scope and target.");
        }
    }

    private static void requireAssignmentTimeRange(Instant availableFromUtc, Instant availableUntilUtc) {
        if (availableFromUtc == null || availableUntilUtc == null) {
            throw new IllegalArgumentException("AvailableFromUtc and AvailableUntilUtc are required.");
        }
        if (!availableFromUtc.isBefore(availableUntilUtc)) {
            throw new IllegalArgumentException("AvailableFromUtc must be before AvailableUntilUtc.");
        }
    }

    private static void requireAssignmentAvailable(TestAssignment assignment) {
        Instant now = Instant.now();
        if (assignment.getStatus() != 2) {
            throw new IllegalArgumentException("Test assignment is not active: " + assignment.getId());
        }
        if (now.isBefore(assignment.getAvailableFromUtc()) || !now.isBefore(assignment.getAvailableUntilUtc())) {
            throw new IllegalArgumentException("Test assignment is not available now: " + assignment.getId());
        }
    }

    private void requireEnrollmentBelongsToPerson(TeachingAssignmentEnrollment enrollment, Integer personId) {
        boolean belongsToPerson = groupMembershipRepository.findById(enrollment.getGroupMembershipId())
                .map(groupMembership -> Objects.equals(groupMembership.getPersonId(), personId))
                .orElse(false);
        if (!belongsToPerson) {
            throw new IllegalArgumentException(
                    "Teaching assignment enrollment does not belong to person: " + enrollment.getId()
            );
        }
    }

    private static int normalizeStatus(Integer status, int min, int max, int defaultStatus) {
        int actual = status == null || status == 0 ? defaultStatus : status;
        if (actual < min || actual > max) {
            throw new IllegalArgumentException("Status must be between " + min + " and " + max + ".");
        }
        return actual;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be non-negative.");
        }
        return value;
    }

    private boolean questionBelongsToAssignmentTest(Question question, TestAssignment assignment) {
        if (Objects.equals(question.getTestId(), assignment.getTestId())) {
            return true;
        }
        if (question.getTestId() != null || question.getTopicId() == null) {
            return false;
        }
        return selectionRuleRepository.findByTestIdOrderByOrdinalAsc(assignment.getTestId())
                .stream()
                .anyMatch(rule -> Objects.equals(rule.getTopicId(), question.getTopicId()));
    }

    public record TestAttemptQuestionSet(TestAttempt attempt, List<Question> questions) {
    }

    public record SelectionRuleInput(Integer courseLectureId,
                                     Integer topicId,
                                     int questionCount,
                                     int textQuestionCount,
                                     int singleAnswerQuestionCount,
                                     int multipleAnswerQuestionCount,
                                     int matchingQuestionCount,
                                     int ordinal) {
    }

    private List<Question> questionsForRule(TestQuestionSelectionRule rule) {
        return rule.getTopicId() == null
                ? questionRepository.findByTestIdAndCourseLectureIdAndActiveTrueOrderByOrdinalAsc(rule.getTestId(), rule.getCourseLectureId())
                : questionRepository.findByTopicIdAndTestIdIsNullAndActiveTrueOrderByOrdinalAsc(rule.getTopicId());
    }

    private List<Question> questionsForRule(TestQuestionSelectionRule rule, int questionType) {
        return rule.getTopicId() == null
                ? questionRepository.findByTestIdAndCourseLectureIdAndTypeAndActiveTrueOrderByOrdinalAsc(
                rule.getTestId(),
                rule.getCourseLectureId(),
                questionType
        )
                : questionRepository.findByTopicIdAndTestIdIsNullAndTypeAndActiveTrueOrderByOrdinalAsc(
                rule.getTopicId(),
                questionType
        );
    }

    private static TestQuestionSelectionRule topicRuleTarget(Integer testId, SelectionRuleContext context) {
        TestQuestionSelectionRule rule = new TestQuestionSelectionRule();
        rule.setTestId(testId);
        rule.setCourseLectureId(context.courseLectureId());
        rule.setTopicId(context.topicId());
        return rule;
    }

    private static List<Test> sortTests(List<Test> tests) {
        return tests.stream()
                .sorted(Comparator
                        .comparing((Test test) -> test.getTitle() == null
                                ? ""
                                : test.getTitle().toLowerCase(Locale.ROOT))
                        .thenComparing(Test::getId))
                .toList();
    }

    private record SelectionRuleContext(Integer courseLectureId, Integer topicId) {
    }
}
