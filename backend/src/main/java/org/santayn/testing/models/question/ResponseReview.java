package org.santayn.testing.models.question;

/** Состояния проверки ответа и источники оценки. */
public final class ResponseReview {

    /** Проверка не нужна: ответ оценён правилами окончательно. */
    public static final int NONE = 0;
    /** Ответ в очереди на проверку ИИ, баллы пока предварительные. */
    public static final int AI_PENDING = 1;
    /** Нужна проверка преподавателя: ИИ не уверен, недоступен или ответ подозрительный. */
    public static final int NEEDS_TEACHER = 2;
    /** ИИ оценил уверенно, преподаватель может пересмотреть. */
    public static final int AI_GRADED = 3;
    /** Оценку поставил или подтвердил преподаватель. */
    public static final int TEACHER_CONFIRMED = 4;

    public static final int GRADED_BY_AUTO = 1;
    public static final int GRADED_BY_AI = 2;
    public static final int GRADED_BY_TEACHER = 3;

    private ResponseReview() {
    }

    public static String name(int status) {
        return switch (status) {
            case AI_PENDING -> "ai_pending";
            case NEEDS_TEACHER -> "needs_teacher";
            case AI_GRADED -> "ai_graded";
            case TEACHER_CONFIRMED -> "teacher_confirmed";
            default -> "none";
        };
    }

    public static String gradedByName(Integer gradedBy) {
        if (gradedBy == null) {
            return null;
        }
        return switch (gradedBy) {
            case GRADED_BY_AI -> "ai";
            case GRADED_BY_TEACHER -> "teacher";
            default -> "auto";
        };
    }
}
