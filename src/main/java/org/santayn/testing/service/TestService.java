    package org.santayn.testing.service;

    import lombok.RequiredArgsConstructor;
    import org.santayn.testing.models.question.Question;
    import org.santayn.testing.models.question.Question_In_Test;
    import org.santayn.testing.models.test.Test;
    import org.santayn.testing.models.topic.Topic;
    import org.santayn.testing.repository.QuestionInTestRepository;
    import org.santayn.testing.repository.QuestionRepository;
    import org.santayn.testing.repository.TestRepository;
    import org.springframework.stereotype.Service;

    import java.util.Collections;
    import java.util.List;
    import java.util.Optional;

    @Service
    @RequiredArgsConstructor
    public class TestService {

        private final TestRepository testRepository;
        private final QuestionRepository questionRepository;
        private final QuestionInTestRepository questionInTestRepository;

        public Test save(Test test) {
            return testRepository.save(test);
        }

        public Test getTestById(Integer testId) {
            return testRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Тест с ID " + testId + " не найден"));
        }

        public List<Question> generateQuestionsForTest(Integer testId) {
            Test test = getTestById(testId);
            List<Question> questions = questionRepository.findByTopic(test.getTopic());

            Collections.shuffle(questions);

            return questions.stream()
                    .limit(test.getQuestionCount())
                    .toList();
        }

        // Новая функция — создаёт тест и добавляет в него вопросы
        public Test createTestWithRandomQuestions(Topic topic, int questionCount) {
            List<Question> allQuestions = questionRepository.findByTopic(topic);

            if (allQuestions.size() < questionCount) {
                throw new IllegalArgumentException("Недостаточно вопросов для создания теста");
            }

            Collections.shuffle(allQuestions);
            List<Question> selectedQuestions = allQuestions.stream()
                    .limit(questionCount)
                    .toList();

            Test test = new Test();
            test.setTopic(topic);
            test.setQuestionCount(questionCount);
            test.setDescription("Автосгенерированный тест");

            test = testRepository.save(test); // Сохраняем тест

            for (Question question : selectedQuestions) {
                Question_In_Test qit = new Question_In_Test();
                qit.setTest(test);
                qit.setQuestion(question);
                questionInTestRepository.save(qit);
            }

            return test;
        }
    }