package org.santayn.testing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.question.ResponseReview;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.TestAssignment;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.service.ai.TextAnswerGradingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Проверка ответов преподавателем: очередь «На проверку», ручная оценка, подтверждение оценки ИИ
 * и повторная проверка ИИ. Каждое изменение пересчитывает итоговый балл попытки.
 */
@Service
public class ResponseReviewService {

    private static final int MAX_QUEUE_SIZE = 200;

    private final QuestionResponseRepository responseRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final TestRepository testRepository;
    private final PersonRepository personRepository;
    private final TeacherTestScopeService testScopeService;
    private final AttemptScoreService attemptScoreService;
    private final TextAnswerGradingService gradingService;
    private final ObjectMapper objectMapper;

    public ResponseReviewService(QuestionResponseRepository responseRepository,
                                 QuestionRepository questionRepository,
                                 TestAttemptRepository attemptRepository,
                                 TestAssignmentRepository assignmentRepository,
                                 TestRepository testRepository,
                                 PersonRepository personRepository,
                                 TeacherTestScopeService testScopeService,
                                 AttemptScoreService attemptScoreService,
                                 TextAnswerGradingService gradingService,
                                 ObjectMapper objectMapper) {
        this.responseRepository = responseRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.assignmentRepository = assignmentRepository;
        this.testRepository = testRepository;
        this.personRepository = personRepository;
        this.testScopeService = testScopeService;
        this.attemptScoreService = attemptScoreService;
        this.gradingService = gradingService;
        this.objectMapper = objectMapper;
    }

    /**
     * @param teacherPersonId преподаватель; null — администратор (видит все тесты)
     * @param status          needs_teacher | ai_pending | ai_graded | teacher_confirmed | all
     */
    @Transactional(readOnly = true)
    public ReviewQueue queue(Integer teacherPersonId, String status, Integer testId) {
        Set<Integer> testIds = teacherPersonId == null
                ? testScopeService.allTestIds()
                : testScopeService.teacherTestIds(teacherPersonId);
        if (testId != null) {
            if (!testIds.contains(testId)) {
                throw new AccessDeniedException("Test does not belong to current teacher.");
            }
            testIds = Set.of(testId);
        }
        Map<String, Long> counts = new HashMap<>();
        for (String name : List.of("needs_teacher", "ai_pending", "ai_graded", "teacher_confirmed")) {
            counts.put(name, 0L);
        }
        if (testIds.isEmpty()) {
            return new ReviewQueue(List.of(), counts);
        }
        List<Integer> countedStatuses = List.of(
                ResponseReview.NEEDS_TEACHER,
                ResponseReview.AI_PENDING,
                ResponseReview.AI_GRADED,
                ResponseReview.TEACHER_CONFIRMED
        );
        for (Object[] row : responseRepository.countForReview(countedStatuses, testIds)) {
            counts.put(ResponseReview.name(((Number) row[0]).intValue()), ((Number) row[1]).longValue());
        }

        List<Integer> statuses = statusesFor(status);
        Map<Integer, String> testNames = new HashMap<>();
        Map<Integer, String> personNames = new HashMap<>();
        List<ReviewItem> items = responseRepository.findForReview(statuses, testIds, PageRequest.of(0, MAX_QUEUE_SIZE))
                .stream()
                .map(response -> toItem(response, testNames, personNames))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ReviewQueue(items, counts);
    }

    @Transactional
    public ReviewItem grade(Integer teacherPersonId, Integer gradedByPersonId, Long responseId,
                            BigDecimal awardedPoints, String comment) {
        QuestionResponse response = requireReviewable(teacherPersonId, responseId);
        Question question = requireQuestion(response);
        BigDecimal maxPoints = question.getPoints() == null ? BigDecimal.ONE : question.getPoints();
        if (awardedPoints == null || awardedPoints.signum() < 0 || awardedPoints.compareTo(maxPoints) > 0) {
            throw new IllegalArgumentException("Баллы должны быть от 0 до " + maxPoints.stripTrailingZeros().toPlainString() + ".");
        }
        BigDecimal points = awardedPoints.setScale(2, RoundingMode.HALF_UP);
        response.setAwardedPoints(points);
        response.setCorrect(points.compareTo(maxPoints) == 0);
        response.setGradedBy(ResponseReview.GRADED_BY_TEACHER);
        response.setReviewStatus(ResponseReview.TEACHER_CONFIRMED);
        response.setTeacherComment(truncate(FacultyService.trimToNull(comment), 2000));
        response.setGradedByPersonId(gradedByPersonId);
        response.setGradedAtUtc(Instant.now());
        attemptScoreService.recalculateIfCompleted(response.getTestAttemptId());
        return toItem(response, new HashMap<>(), new HashMap<>());
    }

    /** Подтвердить баллы как есть (оценку ИИ или правил). */
    @Transactional
    public ReviewItem accept(Integer teacherPersonId, Integer gradedByPersonId, Long responseId) {
        QuestionResponse response = requireReviewable(teacherPersonId, responseId);
        if (response.reviewStatusOrNone() == ResponseReview.AI_PENDING) {
            throw new IllegalArgumentException("Ответ ещё проверяется ИИ — дождитесь оценки или поставьте баллы вручную.");
        }
        response.setReviewStatus(ResponseReview.TEACHER_CONFIRMED);
        response.setGradedByPersonId(gradedByPersonId);
        response.setGradedAtUtc(Instant.now());
        return toItem(response, new HashMap<>(), new HashMap<>());
    }

    /** Отправить ответ на повторную проверку ИИ (например, после правки критериев). */
    @Transactional
    public ReviewItem regrade(Integer teacherPersonId, Long responseId) {
        QuestionResponse response = requireReviewable(teacherPersonId, responseId);
        Question question = requireQuestion(response);
        if (!QuestionTypeSupport.isText(question.getType())) {
            throw new IllegalArgumentException("ИИ проверяет только вопросы со свободным ответом.");
        }
        if (FacultyService.trimToNull(response.getAnswerText()) == null) {
            throw new IllegalArgumentException("Студент не дал ответа — проверять нечего.");
        }
        gradingService.requestRegrade(response);
        return toItem(response, new HashMap<>(), new HashMap<>());
    }

    private QuestionResponse requireReviewable(Integer teacherPersonId, Long responseId) {
        QuestionResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new ResourceNotFoundException("Question response not found: " + responseId));
        TestAttempt attempt = attemptRepository.findById(response.getTestAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Test attempt not found: " + response.getTestAttemptId()));
        if (attempt.getStatus() != 2) {
            throw new IllegalArgumentException("Попытка ещё не завершена.");
        }
        if (teacherPersonId != null) {
            Integer testId = assignmentRepository.findById(attempt.getTestAssignmentId())
                    .map(TestAssignment::getTestId)
                    .orElse(null);
            if (testId == null || !testScopeService.teacherTestIds(teacherPersonId).contains(testId)) {
                throw new AccessDeniedException("Test does not belong to current teacher.");
            }
        }
        return response;
    }

    private Question requireQuestion(QuestionResponse response) {
        return questionRepository.findById(response.getTestQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + response.getTestQuestionId()));
    }

    private ReviewItem toItem(QuestionResponse response, Map<Integer, String> testNames, Map<Integer, String> personNames) {
        Question question = questionRepository.findById(response.getTestQuestionId()).orElse(null);
        TestAttempt attempt = attemptRepository.findById(response.getTestAttemptId()).orElse(null);
        if (question == null || attempt == null) {
            return null;
        }
        Integer testId = assignmentRepository.findById(attempt.getTestAssignmentId())
                .map(TestAssignment::getTestId)
                .orElse(null);
        String testName = testId == null ? null : testNames.computeIfAbsent(testId, id -> testRepository.findById(id)
                .map(Test::getTitle)
                .orElse("Тест #" + id));
        String studentName = personNames.computeIfAbsent(attempt.getPersonId(), id -> personRepository.findById(id)
                .map(ResponseReviewService::fullName)
                .orElse("Студент #" + id));
        JsonNode details = readDetails(response.getAiDetailsJson());
        return new ReviewItem(
                response.getId(),
                attempt.getId(),
                testId,
                testName,
                attempt.getPersonId(),
                studentName,
                question.getId(),
                question.getType(),
                question.getQuestion(),
                question.getPoints(),
                question.getCorrectAnswer(),
                question.getGradingCriteria(),
                response.getAnswerText(),
                response.getAwardedPoints(),
                Boolean.TRUE.equals(response.getCorrect()),
                ResponseReview.name(response.reviewStatusOrNone()),
                ResponseReview.gradedByName(response.getGradedBy()),
                response.getAiScore(),
                response.getAiConfidence(),
                response.getAiFeedback(),
                strings(details, "matched"),
                strings(details, "missing"),
                details != null && details.path("suspicious").asBoolean(false),
                response.getTeacherComment(),
                attempt.getCompletedAt()
        );
    }

    private JsonNode readDetails(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception error) {
            return null;
        }
    }

    private static List<String> strings(JsonNode details, String field) {
        List<String> values = new ArrayList<>();
        if (details != null) {
            details.path(field).forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    static List<Integer> statusesFor(String status) {
        String value = status == null ? "needs_teacher" : status.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "ai_pending" -> List.of(ResponseReview.AI_PENDING);
            case "ai_graded" -> List.of(ResponseReview.AI_GRADED);
            case "teacher_confirmed" -> List.of(ResponseReview.TEACHER_CONFIRMED);
            case "all" -> List.of(
                    ResponseReview.NEEDS_TEACHER,
                    ResponseReview.AI_PENDING,
                    ResponseReview.AI_GRADED,
                    ResponseReview.TEACHER_CONFIRMED
            );
            default -> List.of(ResponseReview.NEEDS_TEACHER);
        };
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String fullName(Person person) {
        return Stream.of(person.getLastName(), person.getFirstName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    public record ReviewQueue(List<ReviewItem> items, Map<String, Long> counts) {
    }

    public record ReviewItem(Long responseId,
                             Integer attemptId,
                             Integer testId,
                             String testName,
                             Integer studentId,
                             String studentName,
                             Long questionId,
                             int questionType,
                             String questionText,
                             BigDecimal questionPoints,
                             String correctAnswer,
                             String gradingCriteria,
                             String answerText,
                             BigDecimal awardedPoints,
                             boolean correct,
                             String reviewStatus,
                             String gradedBy,
                             BigDecimal aiScore,
                             BigDecimal aiConfidence,
                             String aiFeedback,
                             List<String> matched,
                             List<String> missing,
                             boolean suspicious,
                             String teacherComment,
                             Instant completedAt) {
    }
}
