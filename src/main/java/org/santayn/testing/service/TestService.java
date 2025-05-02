package org.santayn.testing.service;

import org.santayn.testing.models.question.Question;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TestService {

    private final TestRepository testRepository;

    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public List<Question> getQuestionsByTestId(Integer testId) {
        if (testId == null) {
            throw new IllegalArgumentException("Test ID cannot be null");
        }
        return testRepository.findQuestionsByTestId(testId);
    }


    public Question getSpecificQuestionByTestIdAndQuestionId(Integer testId, Integer questionId) {
        if (testId == null || questionId == null) {
            throw new IllegalArgumentException("Test ID and Question ID cannot be null");
        }

        List<Question> questions = testRepository.findQuestionsByTestId(testId);

        if (questions == null || questions.isEmpty()) {
            throw new RuntimeException("No questions found for Test ID: " + testId);
        }

        Optional<Question> specificQuestion = questions.stream()
                .filter(question -> questionId.equals(question.getId())) // Используем getId()
                .findFirst();

        return specificQuestion.orElseThrow(() -> new RuntimeException(
                "Question with ID " + questionId + " not found in Test with ID " + testId));
    }
}