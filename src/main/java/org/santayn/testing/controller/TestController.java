package org.santayn.testing.controller;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller // Используем @Controller вместо @RestController
public class TestController {

    @Autowired
    private TestRepository testRepository;

    // Метод для получения вопросов конкретного теста по его ID
    @GetMapping("/tests/{testId}/questions")
    public String getQuestionsByTestId(@PathVariable Integer testId, Model model) {
        // Находим тест по ID
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with ID: " + testId));

        // Получаем список вопросов через связь Question_In_Test
        List<Question> questions = test.getQuestion_in_test().stream()
                .map(questionInTest -> questionInTest.getQuestion()) // Извлекаем вопрос из Question_In_Test
                .collect(Collectors.toList());

        // Передаем вопросы в модель для отображения в HTML
        model.addAttribute("questions", questions);

        // Возвращаем имя шаблона Thymeleaf
        return "questions";
    }
}