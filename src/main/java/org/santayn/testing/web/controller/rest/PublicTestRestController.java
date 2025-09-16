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
import org.santayn.testing.web.dto.test.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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
        if (test == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Тест не найден");

        var qs = questionService.getRandomQuestionsByTopic(
                test.getTopic().getId(), test.getQuestionCount());

        Integer studentId = currentStudentIdOrNull();

        return new TestLoadResponse(
                new TestShortDto(test.getId(), test.getDescription(), test.getQuestionCount()),
                qs.stream().map(q -> new QuestionDto(q.getId(), q.getText())).toList(),
                studentId
        );
    }

    /** Отправка ответов (JSON) */
    @PostMapping("/{testId}/submit")
    public SubmitResponse submit(@PathVariable Integer testId,
                                 @RequestBody SubmitRequest req) {

        if (req == null || req.questionIds() == null || req.answers() == null
                || req.questionIds().size() != req.answers().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неверные данные ответа");
        }

        Integer studentId = currentStudentIdRequired();

        var questions = questionService.findQuestionsByIds(req.questionIds());
        var toSave = new ArrayList<AnswerResult>();
        var resultDtos = new ArrayList<AnswerResultDto>();
        int correctCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            String given = (req.answers().get(i) == null ? "" : req.answers().get(i)).trim();
            boolean ok = given.equalsIgnoreCase(q.getCorrectAnswer().trim());
            if (ok) correctCount++;

            toSave.add(new AnswerResult(
                    null, testId, studentId,
                    q.getId(), q.getText(), q.getCorrectAnswer(),
                    given, ok
            ));

            resultDtos.add(new AnswerResultDto(
                    q.getId(), q.getText(), q.getCorrectAnswer(), given, ok
            ));
        }

        answerRepo.saveAll(toSave);

        return new SubmitResponse(
                testId,
                studentId,
                correctCount,
                questions.size(),
                resultDtos
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
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Нужно войти как студент");
        return id;
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
