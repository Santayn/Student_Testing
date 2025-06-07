package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.repository.AnswerResultRepository;
import org.santayn.testing.service.QuestionService;
import org.santayn.testing.service.StudentService;
import org.santayn.testing.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
@RequiredArgsConstructor
public class QuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuestionController.class);

    private final QuestionService questionService;
    private final TestService      testService;
    private final StudentService   studentService;
    private final AnswerResultRepository answerRepo;

    @GetMapping("/test/{testId}")
    public String getTestDetails(
            @PathVariable Integer testId,
            Principal          principal,
            Model              model) {

        // Теперь studentService.findByLogin вернет Student, либо бросит исключение
        Student student = studentService.findByLogin(principal.getName());

        Test test = testService.getTestById(testId);
        if (test == null) {
            model.addAttribute("error", "Тест не найден");
            return "error-page";
        }

        List<Question> questions = questionService
                .getRandomQuestionsByTopic(test.getTopic().getId(), test.getQuestionCount());

        model.addAttribute("test",      test);
        model.addAttribute("questions", questions);
        // Если нужно — можно вывести ID студента в шаблоне
        model.addAttribute("studentId", student.getId());
        return "test-details";
    }

    @PostMapping("/test/{testId}/submitAnswers")
    public String submitAnswers(
            @PathVariable Integer testId,
            @RequestParam("questionIds") List<Integer> questionIds,
            @RequestParam("answers")     List<String>    answers,
            Principal                                 principal,
            Model                                     model) {

        Student student = studentService.findByLogin(principal.getName());
        Integer studentId = student.getId();

        log.debug("student={}, ids={}, answers={}", principal.getName(), questionIds, answers);

        List<Question> questions = questionService.findQuestionsByIds(questionIds);
        List<AnswerResult> toSave = new ArrayList<>();
        int correctCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q     = questions.get(i);
            String given   = answers.get(i) == null ? "" : answers.get(i).trim().toLowerCase();
            String correct = q.getCorrectAnswer().trim().toLowerCase();
            boolean ok     = given.equals(correct);
            if (ok) correctCount++;

            toSave.add(new AnswerResult(
                    null,
                    testId,
                    studentId,
                    q.getId(),
                    q.getText(),
                    q.getCorrectAnswer(),
                    answers.get(i),
                    ok
            ));
        }

        answerRepo.saveAll(toSave);

        model.addAttribute("results",    toSave);
        model.addAttribute("correctCnt", correctCount);
        model.addAttribute("totalCnt",   questions.size());
        // Можно добавить тест и студента, если нужно
        model.addAttribute("testId",     testId);
        model.addAttribute("studentId",  studentId);
        return "test-result";
    }
}