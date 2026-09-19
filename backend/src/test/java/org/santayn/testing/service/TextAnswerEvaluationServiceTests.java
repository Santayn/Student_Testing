package org.santayn.testing.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class TextAnswerEvaluationServiceTests {

    private final TextAnswerEvaluationService service = new DefaultTextAnswerEvaluationService();

    @Test
    void delegatesToLocalEvaluator() {
        assertThat(service.isCorrect("Что такое ОЗУ?", "оперативная память", "оперитивная паметь"))
                .isTrue();
    }

    @Test
    void rejectsAnswerWhenLocalEvaluatorCannotMatchMeaning() {
        assertThat(service.isCorrect(
                "Что такое SQL?",
                "SQL - декларативный язык запросов к реляционным базам данных",
                "таблицы"
        )).isFalse();
    }

    @Test
    void givesHalfCreditForIncompleteButRelevantAnswer() {
        TextAnswerEvaluationResult result = service.evaluate(
                "Что такое оперативная память?",
                "Оперативная память хранит данные во время работы программы",
                "память хранит данные"
        );

        assertThat(result.correct()).isFalse();
        assertThat(result.scoreRatio()).isEqualByComparingTo(new BigDecimal("0.5"));
    }
}
