// src/main/java/org/santayn/testing/web/controller/rest/PublicTestRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.AnswerResultRepository;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.service.TestService;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.test.AnswerResultDto;
import org.santayn.testing.web.dto.test.QuestionDto;
import org.santayn.testing.web.dto.test.SubmitRequest;
import org.santayn.testing.web.dto.test.SubmitResponse;
import org.santayn.testing.web.dto.test.TestLoadResponse;
import org.santayn.testing.web.dto.test.TestShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/tests")
public class PublicTestRestController {

    private final TestService testService;
    private final QuestionService questionService;
    private final UserService userService;
    private final AnswerResultRepository answerRepo;

    public PublicTestRestController(TestService testService,
                                    QuestionService questionService,
                                    UserService userService,
                                    AnswerResultRepository answerRepo) {
        this.testService = testService;
        this.questionService = questionService;
        this.userService = userService;
        this.answerRepo = answerRepo;
    }

    /** Загрузка теста + случайные вопросы (JSON) */
    @GetMapping("/{testId}")
    public TestLoadResponse load(@PathVariable Integer testId) {
        Test test = testService.getTestById(testId);
        if (test == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Тест не найден");
        }

        List<Question> qs = questionService.getRandomQuestionsByTopic(
                test.getTopic().getId(), test.getQuestionCount());

        Integer studentId = currentStudentIdOrNull();

        return new TestLoadResponse(
                new TestShortDto(test.getId(), test.getDescription(), test.getQuestionCount()),
                qs.stream().map(q -> new QuestionDto(q.getId(), q.getText())).toList(),
                studentId
        );
    }

    /**
     * Отправка ответов (JSON).
     * Ответы сопоставляются по идентификатору вопроса, а не по индексу —
     * так исключаем «переворачивание» при произвольном порядке JPA.
     */
    @PostMapping("/{testId}/submit")
    public SubmitResponse submit(@PathVariable Integer testId,
                                 @RequestBody SubmitRequest req) {

        if (req == null || req.questionIds() == null || req.answers() == null
                || req.questionIds().size() != req.answers().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверные данные ответа");
        }

        Integer studentId = currentStudentIdRequired();

        // Загружаем вопросы и индексируем по ID
        List<Question> loaded = questionService.findQuestionsByIds(req.questionIds());
        Map<Integer, Question> byId = loaded.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        if (byId.size() != req.questionIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некоторые вопросы не найдены");
        }

        int correctCount = 0;
        List<AnswerResult> entities = new ArrayList<>(req.questionIds().size());
        List<AnswerResultDto> details = new ArrayList<>(req.questionIds().size());

        // Идём в исходном порядке questionIds
        for (int i = 0; i < req.questionIds().size(); i++) {
            Integer qid = req.questionIds().get(i);
            Question q = byId.get(qid);

            String givenRaw = req.answers().get(i);
            String givenNorm = normalize(givenRaw);
            String correctNorm = normalize(q.getCorrectAnswer());

            boolean ok = givenNorm.equals(correctNorm);
            if (ok) correctCount++;

            entities.add(new AnswerResult(
                    null,
                    testId,
                    studentId,
                    q.getId(),
                    q.getText(),
                    q.getCorrectAnswer(),
                    givenRaw == null ? "" : givenRaw.trim(),
                    ok
            ));

            details.add(new AnswerResultDto(
                    q.getId(),
                    q.getText(),
                    q.getCorrectAnswer(),
                    givenRaw,
                    ok
            ));
        }

        answerRepo.saveAll(entities);

        return new SubmitResponse(
                testId,
                studentId,
                correctCount,
                req.questionIds().size(),
                details
        );
    }

    /* ===== helpers ===== */

    private Integer currentStudentIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        User u = userService.getUserByUsername(auth.getName());
        return (u != null && u.getStudent() != null) ? u.getStudent().getId() : null;
    }

    private Integer currentStudentIdRequired() {
        Integer id = currentStudentIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Нужно войти как студент");
        }
        return id;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String out = s.trim().toLowerCase(Locale.ROOT);
        // небольшая нормализация для русской 'ё'
        out = out.replace('ё', 'е');
        return out;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> runtimeAs404(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
