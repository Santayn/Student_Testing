package org.santayn.testing.models.ai;

import java.util.Arrays;
import java.util.Locale;

/** Функции ИИ, для которых администратор выбирает движок: внешний API или собственная модель. */
public enum AiFeature {
    GRADING("Проверка свободных ответов"),
    CRITERIA("Подсказка критериев оценивания"),
    LECTURE_SCAN("Разметка распознанных сканов лекций");

    private final String title;

    AiFeature(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    public static AiFeature fromKey(String key) {
        return Arrays.stream(values())
                .filter(feature -> feature.key().equalsIgnoreCase(key) || feature.name().equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная функция ИИ: " + key));
    }
}
