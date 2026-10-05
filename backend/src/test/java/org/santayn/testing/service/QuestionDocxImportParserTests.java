package org.santayn.testing.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.santayn.testing.models.question.QuestionTypeSupport;

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
        assertThat(questions.get(0).type()).isEqualTo(1);
        assertThat(questions.get(0).points()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(questions.get(0).options()).extracting(QuestionDocxImportParser.ParsedOption::correct)
                .containsExactly(false, true);

        assertThat(questions.get(1).question()).isEqualTo("Введите термин");
        assertThat(questions.get(1).type()).isEqualTo(4);
        assertThat(questions.get(1).correctAnswer()).isEqualTo("Java");
        assertThat(questions.get(1).options()).isEmpty();
    }

    @Test
    void parsesMatchingQuestionWithColumnsAndBlankLines() throws Exception {
        byte[] file = docx(
                "Вопрос: Соотнесите типы данных с их описанием.",
                "Баллы: 1",
                "Тип: сопоставление",
                "",
                "Колонка А:",
                "1) int",
                "2) String",
                "3) boolean",
                "4) double",
                "",
                "Колонка Б:",
                "a) true/false",
                "b) строковый тип",
                "c) число с плавающей запятой",
                "d) целочисленный тип",
                "",
                "Ответ: 1-d, 2-b, 3-a, 4-c"
        );

        List<QuestionDocxImportParser.ParsedQuestion> questions = parser.parse(new ByteArrayInputStream(file));

        assertThat(questions).hasSize(1);
        QuestionDocxImportParser.ParsedQuestion question = questions.get(0);
        assertThat(question.question()).isEqualTo("Соотнесите типы данных с их описанием.");
        assertThat(question.type()).isEqualTo(3);
        assertThat(question.options()).isEmpty();
        assertThat(question.matchingPairs())
                .containsExactly(
                        new QuestionTypeSupport.MatchingPair(1, "int", "целочисленный тип"),
                        new QuestionTypeSupport.MatchingPair(2, "String", "строковый тип"),
                        new QuestionTypeSupport.MatchingPair(3, "boolean", "true/false"),
                        new QuestionTypeSupport.MatchingPair(4, "double", "число с плавающей запятой")
                );
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
