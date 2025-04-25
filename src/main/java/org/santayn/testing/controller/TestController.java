package org.santayn.testing.controller;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.service.TestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/tests")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }


     // Отобразить страницу с вопросами теста.

    @GetMapping("/{testId}/questions")
    public String showQuestionsPage(@PathVariable Integer testId, Model model) {
        List<Question> questions = testService.getQuestionsByTestId(testId);

        if (questions == null || questions.isEmpty()) {
            model.addAttribute("errorMessage", "Нет доступных вопросов для этого теста.");
            return "error-page"; // Вернуть страницу ошибки
        }

        model.addAttribute("questions", questions);
        model.addAttribute("testId", testId);

        return "questions-page";
    }


    //Получить конкретный вопрос теста (JSON).

    @GetMapping("/{testId}/questions/{questionId}")
    public Question getSpecificQuestion(
            @PathVariable Integer testId,
            @PathVariable Integer questionId) {
        return testService.getSpecificQuestionByTestIdAndQuestionId(testId, questionId);
    }
}