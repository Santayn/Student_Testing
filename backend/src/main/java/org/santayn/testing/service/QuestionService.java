package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.question.QuestionOption;
import org.santayn.testing.models.question.QuestionTypeSupport;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.QuestionOptionRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.TestRepository;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.TestQuestionSelectionRuleRepository;
import org.santayn.testing.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final TestRepository testRepository;
    private final LectureRepository lectureRepository;
    private final TestQuestionSelectionRuleRepository selectionRuleRepository;
    private final TopicRepository topicRepository;
    private final QuestionDocxImportParser docxImportParser;

    @Transactional(readOnly = true)
    public List<Question> findAll(Integer testId, Integer topicId) {
        if (topicId != null) {
            return questionRepository.findByTopicIdAndTestIdIsNullOrderByOrdinalAsc(topicId);
        }
        if (testId != null) {
            return questionRepository.findByTestIdOrderByOrdinalAsc(testId);
        }
        throw new IllegalArgumentException("Either testId or topicId is required.");
    }

    @Transactional(readOnly = true)
    public Question get(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<QuestionOption> findOptions(Long questionId) {
        return optionRepository.findByTestQuestionIdOrderByOrdinalAsc(questionId);
    }

    @Transactional(readOnly = true)
    public QuestionOption getOption(Long optionId) {
        return optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Question option not found: " + optionId));
    }

    @Transactional
    public Question create(Integer testId,
                           Integer courseLectureId,
                           Integer topicId,
                           int type,
                           String questionText,
                           BigDecimal points,
                           int ordinal,
                           String correctAnswer,
                           List<QuestionTypeSupport.MatchingPair> matchingPairs) {
        QuestionTarget target = resolveQuestionTarget(testId, courseLectureId, topicId);
        QuestionContext context = resolveQuestionContext(courseLectureId, topicId);
        requireQuestionType(type);
        BigDecimal normalizedPoints = points == null ? BigDecimal.ONE : points;
        requireNonNegative(normalizedPoints, "Points");
        int normalizedOrdinal = Math.max(1, ordinal);
        if (ordinalExists(target.testId(), context.topicId(), normalizedOrdinal, null)) {
            throw new AuthConflictException("Question ordinal already exists for target.");
        }

        Question question = new Question();
        question.setTestId(target.testId());
        question.setCourseLectureId(context.courseLectureId());
        question.setTopicId(context.topicId());
        question.setType(type);
        question.setQuestion(FacultyService.requireText(questionText, "Question"));
        question.setPoints(normalizedPoints);
        question.setOrdinal(normalizedOrdinal);
        question.setCorrectAnswer(normalizeStoredCorrectAnswer(type, correctAnswer, matchingPairs));
        question.setActive(true);
        return questionRepository.save(question);
    }

    @Transactional
    public Question update(Long questionId,
                           Integer courseLectureId,
                           Integer topicId,
                           int type,
                           String questionText,
                           BigDecimal points,
                           int ordinal,
                           String correctAnswer,
                           List<QuestionTypeSupport.MatchingPair> matchingPairs,
                           boolean active) {
        Question question = get(questionId);
        QuestionContext context = resolveQuestionContext(courseLectureId, topicId);
        requireQuestionType(type);
        BigDecimal normalizedPoints = points == null ? BigDecimal.ONE : points;
        requireNonNegative(normalizedPoints, "Points");
        int normalizedOrdinal = Math.max(1, ordinal);
        if (ordinalExists(question.getTestId(), context.topicId(), normalizedOrdinal, questionId)) {
            throw new AuthConflictException("Question ordinal already exists for target.");
        }

        question.setCourseLectureId(context.courseLectureId());
        question.setTopicId(context.topicId());
        question.setType(type);
        question.setQuestion(FacultyService.requireText(questionText, "Question"));
        question.setPoints(normalizedPoints);
        question.setOrdinal(normalizedOrdinal);
        question.setCorrectAnswer(normalizeStoredCorrectAnswer(type, correctAnswer, matchingPairs));
        question.setActive(active);
        return question;
    }

    @Transactional
    public QuestionOption addOption(Long questionId, String text, int ordinal, boolean correct) {
        Question question = get(questionId);
        requireSelectableQuestion(question);
        requireSingleChoiceHasOnlyOneCorrectOption(question, null, correct);
        int normalizedOrdinal = Math.max(1, ordinal);
        if (optionRepository.existsByTestQuestionIdAndOrdinal(questionId, normalizedOrdinal)) {
            throw new AuthConflictException("Question option ordinal already exists: " + questionId + "/" + normalizedOrdinal);
        }

        QuestionOption option = new QuestionOption();
        option.setTestQuestionId(questionId);
        option.setText(FacultyService.requireText(text, "Text"));
        option.setOrdinal(normalizedOrdinal);
        option.setCorrect(correct);
        return optionRepository.save(option);
    }

    @Transactional
    public QuestionOption updateOption(Long optionId, String text, int ordinal, boolean correct) {
        QuestionOption option = getOption(optionId);
        Question question = get(option.getTestQuestionId());
        requireSelectableQuestion(question);
        requireSingleChoiceHasOnlyOneCorrectOption(question, optionId, correct);
        int normalizedOrdinal = Math.max(1, ordinal);
        if (optionRepository.existsByTestQuestionIdAndOrdinalAndIdNot(option.getTestQuestionId(), normalizedOrdinal, optionId)) {
            throw new AuthConflictException("Question option ordinal already exists: " + option.getTestQuestionId() + "/" + normalizedOrdinal);
        }

        option.setText(FacultyService.requireText(text, "Text"));
        option.setOrdinal(normalizedOrdinal);
        option.setCorrect(correct);
        return option;
    }

    @Transactional
    public Question setActive(Long questionId, boolean active) {
        Question question = get(questionId);
        question.setActive(active);
        return question;
    }

    @Transactional
    public QuestionImportResult importDocx(Integer testId, Integer courseLectureId, Integer topicId, MultipartFile file) {
        QuestionTarget target = resolveQuestionTarget(testId, courseLectureId, topicId);
        Test test = target.testId() == null
                ? null
                : testRepository.findById(target.testId())
                  .orElseThrow(() -> new IllegalArgumentException("Test not found: " + target.testId()));
        QuestionContext context = resolveQuestionContext(courseLectureId, topicId);
        requireDocxFile(file);

        List<QuestionDocxImportParser.ParsedQuestion> parsedQuestions;
        try {
            parsedQuestions = docxImportParser.parse(file.getInputStream());
        } catch (IOException error) {
            throw new IllegalArgumentException("Failed to read DOCX file.", error);
        }

        int nextOrdinal = nextOrdinal(target.testId(), context.topicId());
        List<Question> savedQuestions = new ArrayList<>();
        int importedOptions = 0;

        for (QuestionDocxImportParser.ParsedQuestion parsedQuestion : parsedQuestions) {
            Question question = new Question();
            question.setTestId(target.testId());
            question.setCourseLectureId(context.courseLectureId());
            question.setTopicId(context.topicId());
            question.setType(parsedQuestion.type());
            question.setQuestion(FacultyService.requireText(parsedQuestion.question(), "Question"));
            question.setPoints(parsedQuestion.points());
            question.setOrdinal(nextOrdinal++);
            question.setCorrectAnswer(normalizeStoredCorrectAnswer(parsedQuestion.type(), parsedQuestion.correctAnswer(), List.of()));
            question.setActive(true);
            question = questionRepository.save(question);
            savedQuestions.add(question);

            int optionOrdinal = 1;
            for (QuestionDocxImportParser.ParsedOption parsedOption : parsedQuestion.options()) {
                QuestionOption option = new QuestionOption();
                option.setTestQuestionId(question.getId());
                option.setText(FacultyService.requireText(parsedOption.text(), "Text"));
                option.setOrdinal(optionOrdinal++);
                option.setCorrect(parsedOption.correct());
                optionRepository.save(option);
                importedOptions++;
            }
        }

        if (test != null && selectionRuleRepository.findByTestIdOrderByOrdinalAsc(test.getId()).isEmpty()) {
            test.setQuestionCount(Math.max(1, (int) questionRepository.countByTestId(test.getId())));
        }
        return new QuestionImportResult(savedQuestions, importedOptions);
    }


    private void requireSelectableQuestion(Question question) {
        if (!QuestionTypeSupport.usesSelectableOptions(question.getType())) {
            throw new IllegalArgumentException("Options can be edited only for single-choice and multiple-choice questions.");
        }
    }

    private void requireSingleChoiceHasOnlyOneCorrectOption(Question question, Long editedOptionId, boolean newCorrectValue) {
        if (!QuestionTypeSupport.isSingleChoice(question.getType()) || !newCorrectValue) {
            return;
        }

        boolean anotherCorrectOptionExists = optionRepository.findByTestQuestionIdOrderByOrdinalAsc(question.getId())
                .stream()
                .anyMatch(option -> option.isCorrect() && (editedOptionId == null || !editedOptionId.equals(option.getId())));
        if (anotherCorrectOptionExists) {
            throw new IllegalArgumentException("Single-choice question can contain only one correct option.");
        }
    }

    private QuestionTarget resolveQuestionTarget(Integer testId, Integer courseLectureId, Integer topicId) {
        if (testId != null) {
            if (!testRepository.existsById(testId)) {
                throw new IllegalArgumentException("Test not found: " + testId);
            }
            return new QuestionTarget(testId);
        }
        if (topicId != null) {
            QuestionContext context = resolveQuestionContext(courseLectureId, topicId);
            if (context.topicId() == null) {
                throw new IllegalArgumentException("TopicId is required for topic question bank.");
            }
            return new QuestionTarget(null);
        }
        throw new IllegalArgumentException("Either testId or topicId is required.");
    }

    private QuestionContext resolveQuestionContext(Integer courseLectureId, Integer topicId) {
        if (topicId == null) {
            requireLectureExists(courseLectureId);
            return new QuestionContext(courseLectureId, null);
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));
        Integer resolvedLectureId = topic.getCourseLectureId();
        if (resolvedLectureId != null) {
            requireLectureExists(resolvedLectureId);
        }
        if (courseLectureId != null && resolvedLectureId != null && !courseLectureId.equals(resolvedLectureId)) {
            throw new IllegalArgumentException("Topic " + topicId + " does not belong to lecture " + courseLectureId + ".");
        }
        Integer effectiveLectureId = courseLectureId != null ? courseLectureId : resolvedLectureId;
        requireLectureExists(effectiveLectureId);
        return new QuestionContext(effectiveLectureId, topic.getId());
    }

    private void requireLectureExists(Integer courseLectureId) {
        if (courseLectureId != null && !lectureRepository.existsById(courseLectureId)) {
            throw new IllegalArgumentException("Course lecture not found: " + courseLectureId);
        }
    }

    private boolean ordinalExists(Integer testId, Integer topicId, int ordinal, Long excludedQuestionId) {
        if (testId != null) {
            return excludedQuestionId == null
                    ? questionRepository.existsByTestIdAndOrdinal(testId, ordinal)
                    : questionRepository.existsByTestIdAndOrdinalAndIdNot(testId, ordinal, excludedQuestionId);
        }
        if (topicId == null) {
            throw new IllegalArgumentException("TopicId is required for topic question bank.");
        }
        return excludedQuestionId == null
                ? questionRepository.existsByTopicIdAndTestIdIsNullAndOrdinal(topicId, ordinal)
                : questionRepository.existsByTopicIdAndTestIdIsNullAndOrdinalAndIdNot(topicId, ordinal, excludedQuestionId);
    }

    private int nextOrdinal(Integer testId, Integer topicId) {
        if (testId != null) {
            return questionRepository.findMaxOrdinalByTestId(testId) + 1;
        }
        if (topicId == null) {
            throw new IllegalArgumentException("TopicId is required for topic question bank.");
        }
        return questionRepository.findMaxOrdinalByTopicId(topicId) + 1;
    }

    private static void requireQuestionType(int type) {
        if (!QuestionTypeSupport.isSupported(type)) {
            throw new IllegalArgumentException("Question type must be between 1 and 4.");
        }
    }

    private static String normalizeStoredCorrectAnswer(int type,
                                                       String correctAnswer,
                                                       List<QuestionTypeSupport.MatchingPair> matchingPairs) {
        if (QuestionTypeSupport.isMatching(type)) {
            List<QuestionTypeSupport.MatchingPair> normalizedPairs = normalizeMatchingPairs(matchingPairs);
            if (normalizedPairs.size() < 2) {
                throw new IllegalArgumentException("Matching question must contain at least two pairs.");
            }
            return QuestionTypeSupport.serializeMatchingPairs(normalizedPairs);
        }

        if (QuestionTypeSupport.isText(type)) {
            String normalizedCorrectAnswer = FacultyService.trimToNull(correctAnswer);
            if (normalizedCorrectAnswer == null) {
                throw new IllegalArgumentException("Text question must contain at least one accepted answer.");
            }
            return normalizedCorrectAnswer;
        }

        return null;
    }

    private static List<QuestionTypeSupport.MatchingPair> normalizeMatchingPairs(List<QuestionTypeSupport.MatchingPair> matchingPairs) {
        if (matchingPairs == null || matchingPairs.isEmpty()) {
            return List.of();
        }

        List<QuestionTypeSupport.MatchingPair> normalizedPairs = new ArrayList<>();
        java.util.Set<String> normalizedLeftValues = new java.util.HashSet<>();
        java.util.Set<String> normalizedRightValues = new java.util.HashSet<>();
        int nextOrdinal = 1;

        for (QuestionTypeSupport.MatchingPair rawPair : matchingPairs) {
            if (rawPair == null) {
                continue;
            }
            String left = FacultyService.trimToNull(rawPair.left());
            String right = FacultyService.trimToNull(rawPair.right());
            if (left == null && right == null) {
                continue;
            }
            if (left == null || right == null) {
                throw new IllegalArgumentException("Matching pair must contain both left and right values.");
            }

            String normalizedLeft = QuestionTypeSupport.normalizeComparable(left);
            String normalizedRight = QuestionTypeSupport.normalizeComparable(right);
            if (normalizedLeft == null || normalizedRight == null) {
                throw new IllegalArgumentException("Matching pair values must not be blank.");
            }
            if (!normalizedLeftValues.add(normalizedLeft)) {
                throw new IllegalArgumentException("Matching pair left values must be unique.");
            }
            if (!normalizedRightValues.add(normalizedRight)) {
                throw new IllegalArgumentException("Matching pair right values must be unique.");
            }

            int ordinal = rawPair.ordinal() > 0 ? rawPair.ordinal() : nextOrdinal;
            normalizedPairs.add(new QuestionTypeSupport.MatchingPair(ordinal, left, right));
            nextOrdinal++;
        }

        return normalizedPairs.stream()
                .sorted(java.util.Comparator.comparingInt(QuestionTypeSupport.MatchingPair::ordinal))
                .toList();
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(field + " must be non-negative.");
        }
    }

    private static void requireDocxFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("DOCX file is required.");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx files are supported.");
        }
    }

    public record QuestionImportResult(List<Question> questions, int importedOptions) {
    }

    private record QuestionTarget(Integer testId) {
    }

    private record QuestionContext(Integer courseLectureId, Integer topicId) {
    }
}
