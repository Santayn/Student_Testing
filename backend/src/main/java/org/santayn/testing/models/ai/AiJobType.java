package org.santayn.testing.models.ai;

public final class AiJobType {

    /** Проверка свободного ответа студента (targetId — id QuestionResponse). */
    public static final String TEXT_GRADING = "TEXT_GRADING";

    /** Подсказка критериев оценивания для вопроса (синхронный вызов). */
    public static final String CRITERIA_SUGGESTION = "CRITERIA_SUGGESTION";

    /** Распознавание скана лекции в Markdown. */
    public static final String LECTURE_SCAN = "LECTURE_SCAN";

    /** План презентации по лекции (targetId — id лекции). */
    public static final String PRESENTATION_OUTLINE = "PRESENTATION_OUTLINE";

    /** Сборка PPTX/PDF по плану (targetId — id лекции). */
    public static final String PRESENTATION_BUILD = "PRESENTATION_BUILD";

    /** Генерация картинки для слайда. */
    public static final String IMAGE_GENERATION = "IMAGE_GENERATION";

    private AiJobType() {
    }
}
