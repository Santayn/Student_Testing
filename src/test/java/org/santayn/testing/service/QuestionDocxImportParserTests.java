package org.santayn.testing.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDocxImportParserTests {

    private final QuestionDocxImportParser parser = new QuestionDocxImportParser();

    @Test
    void parsesQuestionsOptionsPointsAndCorrectAnswers() throws Exception {
        byte[] file = docx(
                "1. Выберите правильный вариант",
                "Баллы: 2",
                "А) Неверный вариант",
                "Б) Верный вариант",
                "Ответ: Б",
                "",
                "Вопрос 2: Введите термин",
                "Ответ: Java"
        );

        List<QuestionDocxImportParser.ParsedQuestion> questions = parser.parse(new ByteArrayInputStream(file));

        assertThat(questions).hasSize(2);
        assertThat(questions.get(0).question()).isEqualTo("Выберите правильный вариант");
        assertThat(questions.get(0).type()).isEqualTo(2);
        assertThat(questions.get(0).points()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(questions.get(0).options()).extracting(QuestionDocxImportParser.ParsedOption::correct)
                .containsExactly(false, true);

        assertThat(questions.get(1).question()).isEqualTo("Введите термин");
        assertThat(questions.get(1).type()).isEqualTo(1);
        assertThat(questions.get(1).correctAnswer()).isEqualTo("Java");
        assertThat(questions.get(1).options()).isEmpty();
    }

    private byte[] docx(String... paragraphs) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : paragraphs) {
                document.createParagraph().createRun().setText(text);
            }
            document.write(output);
            return output.toByteArray();
        }
    }
}
