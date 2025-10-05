// src/main/java/org/santayn/testing/service/QuestionService.java
package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.models.topic.Topic;
import org.santayn.testing.repository.QuestionInTestRepository;
import org.santayn.testing.repository.QuestionRepository;
import org.santayn.testing.repository.TopicRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionService.class);

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionInTestRepository questionInTestRepository;

    /** Старый метод — для совместимости (MVC). */
    public void processQuestionFile(Integer topicId, MultipartFile file) {
        processQuestionFileCount(topicId, file);
    }

    /** Загрузка из файла (.docx/.csv) с подсчётом сохранённых вопросов. */
    public int processQuestionFileCount(Integer topicId, MultipartFile file) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));

        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("")
                .toLowerCase(Locale.ROOT);

        List<Question> parsed;
        if (name.endsWith(".csv")) {
            parsed = parseCsv(file);
        } else if (name.endsWith(".docx")) {
            parsed = parseDocx(file);
        } else {
            // пробуем docx по умолчанию
            parsed = parseDocx(file);
        }

        log.info("Найдено {} вопросов", parsed.size());
        for (Question q : parsed) {
            q.setTopic(topic);
            questionRepository.save(q);
            log.info("Сохранён вопрос: {} → {}", q.getText(), q.getCorrectAnswer());
        }
        return parsed.size();
    }

    /** Полный список вопросов по теме (для UI). */
    public List<Question> getQuestionsByTopic(Integer topicId) {
        ensureTopicExists(topicId);
        return questionRepository.findByTopicIdOrderByIdAsc(topicId);
    }

    /** Создание одного вопроса вручную. */
    public Question createManual(Integer topicId, String text, String correctAnswer) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Тема не найдена"));
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Текст вопроса пуст");
        }
        if (correctAnswer == null || correctAnswer.isBlank()) {
            throw new IllegalArgumentException("Правильный ответ пуст");
        }
        Question q = new Question();
        q.setTopic(topic);
        q.setText(text.trim());
        q.setCorrectAnswer(correctAnswer.trim());
        return questionRepository.save(q);
    }

    /** Возвращает случайные вопросы указанного количества. */
    public List<Question> getRandomQuestionsByTopic(Integer topicId, int count) {
        ensureTopicExists(topicId);
        List<Question> all = questionRepository.findByTopicId(topicId);
        if (all.isEmpty())
            throw new IllegalArgumentException("В теме нет вопросов");
        Collections.shuffle(all);
        return all.stream().limit(count).collect(Collectors.toList());
    }

    /** Находит вопросы по списку ID. */
    public List<Question> findQuestionsByIds(List<Integer> ids) {
        return questionRepository.findAllById(ids);
    }

    /**
     * Удаление пачкой: сначала чистим связи в question_in_test,
     * затем удаляем сами вопросы. Всё в одной транзакции.
     *
     * @return количество удалённых вопросов
     */
    @Transactional
    public int deleteQuestionsByIds(Integer topicId, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        ensureTopicExists(topicId);

        // проверим, что все вопросы принадлежат теме
        List<Question> loaded = questionRepository.findAllById(ids);
        if (loaded.size() != ids.size()) {
            throw new IllegalArgumentException("Некоторые вопросы не найдены");
        }
        boolean foreign = loaded.stream().anyMatch(q ->
                q.getTopic() == null || !Objects.equals(q.getTopic().getId(), topicId));
        if (foreign) {
            throw new IllegalArgumentException("В списке есть вопросы другой темы");
        }

        // сначала удаляем связи с тестами
        int detached = questionInTestRepository.deleteByQuestionIds(ids);
        log.info("Удалено {} связей question_in_test для вопросов {}", detached, ids);

        // затем удаляем сами вопросы
        questionRepository.deleteAllByIdInBatch(ids);
        return loaded.size();
    }

    /* ======== private ======== */

    private void ensureTopicExists(Integer topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new IllegalArgumentException("Тема не найдена");
        }
    }

    /** DOCX формат:
     *  Вопрос: <текст...>
     *  ...
     *  Ответ: A|B|C|D (первая буква)
     */
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
                    String correct = extractAnswerLetter(text.replaceFirst("Ответ:\\s*", "").trim());
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

    /** CSV формат (простой):
     *  колонки: text, correct (разделитель ',' или ';')
     */
    private List<Question> parseCsv(MultipartFile file) {
        List<Question> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean maybeHeader = true;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split("[;,]", 2);
                if (parts.length < 2) {
                    int idx = Math.max(trimmed.lastIndexOf(';'), trimmed.lastIndexOf(','));
                    if (idx <= 0 || idx >= trimmed.length() - 1) continue;
                    parts = new String[]{ trimmed.substring(0, idx), trimmed.substring(idx + 1) };
                }

                String text = parts[0].trim();
                String correctRaw = parts[1].trim();

                if (maybeHeader) {
                    String lower = (text + " " + correctRaw).toLowerCase(Locale.ROOT);
                    if (lower.contains("text") || lower.contains("вопрос")
                            || lower.contains("correct") || lower.contains("ответ")) {
                        maybeHeader = false;
                        continue;
                    }
                }
                maybeHeader = false;

                String correct = extractAnswerLetter(correctRaw);
                if (!text.isEmpty() && correct != null && !correct.isEmpty()) {
                    result.add(createQuestion(text, correct));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения CSV", e);
        }
        return result;
    }

    private Question createQuestion(String text, String correctAnswer) {
        Question q = new Question();
        q.setText(text.trim());
        q.setCorrectAnswer(correctAnswer);
        return q;
    }

    private String extractAnswerLetter(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() ? null : t.substring(0, 1);
    }
}
