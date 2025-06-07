package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.TopicRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;

    /** Парсим DOCX и сохраняем */
    public void processQuestionFile(Integer topicId, MultipartFile file) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        List<Question> parsed = parseDocx(file);
        log.info("Найдено {} вопросов", parsed.size());
        parsed.forEach(q -> {
            q.setTopic(topic);
            questionRepository.save(q);
            log.info("Сохранён вопрос: {} → {}", q.getText(), q.getCorrectAnswer());
        });
    }

    /** Возвращает случайные вопросы указанного количества */
    public List<Question> getRandomQuestionsByTopic(Integer topicId, int count) {
        if (!topicRepository.existsById(topicId))
            throw new IllegalArgumentException("Тема не найдена");
        List<Question> all = questionRepository.findByTopicId(topicId);
        if (all.isEmpty())
            throw new IllegalArgumentException("В теме нет вопросов");
        Collections.shuffle(all);
        return all.stream().limit(count).collect(Collectors.toList());
    }

    /** Находит вопросы по списку ID */
    public List<Question> findQuestionsByIds(List<Integer> ids) {
        return questionRepository.findAllById(ids);
    }

    /* ======== private-парсеры DOCX ======== */
    private List<Question> parseDocx(MultipartFile file) {
        List<Question> result = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder buf = new StringBuilder();
            boolean isParsing = false;
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText().trim().replaceAll("\\s+", " ");
                if (text.isEmpty()) continue;
                if (text.startsWith("Вопрос:")) {
                    buf.setLength(0);
                    buf.append(text).append("\n");
                    isParsing = true;
                } else if (isParsing && text.startsWith("Ответ:")) {
                    String correct = extractAnswerLetter(text);
                    result.add(createQuestion(buf.toString(), correct));
                    isParsing = false;
                } else if (isParsing) {
                    buf.append(text).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения DOCX", e);
        }
        return result;
    }

    private Question createQuestion(String text, String correctAnswer) {
        Question q = new Question();
        q.setText(text.trim());
        q.setCorrectAnswer(correctAnswer);
        return q;
    }

    private String extractAnswerLetter(String line) {
        String s = line.replaceFirst("Ответ:\\s*", "").trim().toLowerCase();
        return s.isEmpty() ? null : s.substring(0, 1);
    }
}