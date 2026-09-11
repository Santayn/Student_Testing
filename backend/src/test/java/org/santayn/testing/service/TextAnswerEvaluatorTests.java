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
}
