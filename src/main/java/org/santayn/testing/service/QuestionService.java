package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.TopicRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    /**
     * Обрабатывает загруженный DOCX-файл с вопросами и сохраняет их в базу данных.
     *
     * @param topicId ID темы, к которой относятся вопросы
     * @param file    Загруженный DOCX-файл
     */
    public void processQuestionFile(Integer topicId, MultipartFile file) {
        // Проверка существования темы
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));

        // Парсинг вопросов из DOCX-файла
        List<Question> parsedQuestions = parseDocx(file);

        log.info("Найдено вопросов: {}", parsedQuestions.size());

        // Сохранение вопросов в базу данных
        for (Question question : parsedQuestions) {
            question.setTopic(topic);
            questionRepository.save(question);
            log.info("Сохранён вопрос: {}\nПравильный ответ: {}", question.getText(), question.getCorrectAnswer());
        }
    }

    /**
     * Парсит DOCX-файл и возвращает список вопросов.
     *
     * @param file Загруженный DOCX-файл
     * @return Список вопросов
     */
    private List<Question> parseDocx(MultipartFile file) {
        List<Question> result = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder fullQuestionText = new StringBuilder();
            String correctAnswer = null;
            boolean isParsing = false;

            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText().replaceAll("\\s+", " ").trim();

                if (text.isEmpty()) continue;

                if (text.startsWith("Вопрос:")) {
                    // Начало нового вопроса
                    saveUnfinishedQuestion(result, fullQuestionText, isParsing);

                    isParsing = true;
                    fullQuestionText.setLength(0); // Очистка буфера
                    fullQuestionText.append(text).append("\n");

                } else if (isParsing) {
                    if (text.contains("Ответ:")) {
                        // Извлекаем правильный ответ
                        correctAnswer = extractAnswerLetter(text);

                        // Создаем и сохраняем вопрос
                        if (correctAnswer != null) {
                            Question question = createQuestion(fullQuestionText.toString().trim(), correctAnswer);
                            result.add(question);
                        }

                        // Сброс состояния
                        isParsing = false;
                        fullQuestionText.setLength(0);
                        correctAnswer = null;

                    } else {
                        // Добавляем текст к текущему вопросу
                        fullQuestionText.append(text).append("\n");
                    }
                }
            }

            // Обработка последнего незавершенного вопроса
            saveUnfinishedQuestion(result, fullQuestionText, isParsing);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении DOCX", e);
        }
        return result;
    }

    /**
     * Сохраняет незавершенный вопрос, если он существует.
     *
     * @param result            Список вопросов
     * @param fullQuestionText  Текущий текст вопроса
     * @param isParsing         Флаг, указывающий, что вопрос находится в процессе парсинга
     */
    private void saveUnfinishedQuestion(List<Question> result, StringBuilder fullQuestionText, boolean isParsing) {
        if (isParsing && fullQuestionText.length() > 0) {
            String answer = extractAnswerLetterFromText(fullQuestionText.toString());
            if (answer != null) {
                Question question = createQuestion(fullQuestionText.toString().trim(), answer);
                result.add(question);
            } else {
                log.warn("Пропущен незавершенный вопрос: {}", fullQuestionText.toString());
            }
        }
    }

    /**
     * Создает объект вопроса.
     *
     * @param text          Полный текст вопроса
     * @param correctAnswer Правильный ответ
     * @return Объект вопроса
     */
    private Question createQuestion(String text, String correctAnswer) {
        Question question = new Question();
        question.setText(text);
        question.setCorrectAnswer(correctAnswer);
        return question;
    }

    /**
     * Извлекает букву правильного ответа из строки "Ответ:".
     *
     * @param answerLine Строка, содержащая ответ
     * @return Буква правильного ответа (например, "a", "b", "c", "d")
     */
    private String extractAnswerLetter(String answerLine) {
        String clean = answerLine.replaceFirst("Ответ:\\s*", "").trim().toLowerCase();
        if (clean.isEmpty()) return null;
        return String.valueOf(clean.charAt(0));
    }

    /**
     * Извлекает букву правильного ответа из полного текста вопроса.
     *
     * @param text Полный текст вопроса
     * @return Буква правильного ответа (например, "a", "b", "c", "d")
     */
    private String extractAnswerLetterFromText(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().startsWith("Ответ:")) {
                return extractAnswerLetter(line);
            }
        }
        return null;
    }
}