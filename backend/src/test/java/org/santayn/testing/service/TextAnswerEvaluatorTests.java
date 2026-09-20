package org.santayn.testing.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextAnswerEvaluatorTests {

    @Test
    void acceptsAnswerWithSpellingErrors() {
        assertThat(TextAnswerEvaluator.isCorrect("оперативная память", "оперитивная паметь"))
                .isTrue();
    }

    @Test
    void acceptsKnownEducationalAbbreviation() {
        assertThat(TextAnswerEvaluator.isCorrect("объектно ориентированное программирование", "ООП"))
                .isTrue();
    }

    @Test
    void rejectsAnswerWithOnlyRelatedKeywords() {
        assertThat(TextAnswerEvaluator.isCorrect(
                "Оперативная память хранит данные во время работы программы",
                "данные программа"
        )).isFalse();
    }

    @Test
    void rejectsAnswerThatChangesMeaningWithNegation() {
        assertThat(TextAnswerEvaluator.isCorrect(
                "Оперативная память хранит данные во время работы программы",
                "Оперативная память не хранит данные во время работы программы"
        )).isFalse();
    }

    @Test
    void givesPartialCreditForIncompleteButRelevantAnswer() {
        assertThat(TextAnswerEvaluator.score(
                "Оперативная память хранит данные во время работы программы",
                "память хранит данные"
        )).isEqualTo(0.5);
    }

    @Test
    void acceptsTranslatedCpuAnswer() {
        assertThat(TextAnswerEvaluator.isCorrect(
                "Central Processing Unit",
                "центральный процессор для вычислений"
        )).isTrue();
    }

    @Test
    void exactAcceptedAnswerAllowsOnlyStrictAcceptedTextVariants() {
        assertThat(TextAnswerEvaluator.isExactAcceptedAnswer(
                "CPU|Central Processing Unit",
                "central-processing unit"
        )).isTrue();
    }

    @Test
    void semanticallyCorrectAnswerCanStillDifferFromExactAcceptedAnswer() {
        assertThat(TextAnswerEvaluator.isCorrect("CPU", "Central Processing Unit")).isTrue();
        assertThat(TextAnswerEvaluator.isExactAcceptedAnswer("CPU", "Central Processing Unit")).isFalse();
    }
}
