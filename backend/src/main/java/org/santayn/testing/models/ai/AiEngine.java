package org.santayn.testing.models.ai;

import java.util.Locale;

/** Чем выполняется функция ИИ: внешний API (Groq, Yandex и т.п.) или собственные модели (сервис local-ai). */
public final class AiEngine {

    public static final String API = "api";
    public static final String LOCAL = "local";

    private AiEngine() {
    }

    public static String normalize(String value) {
        String engine = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (API.equals(engine) || LOCAL.equals(engine)) {
            return engine;
        }
        throw new IllegalArgumentException("Движок должен быть api или local.");
    }

    public static String title(String engine) {
        return LOCAL.equals(engine) ? "Собственная модель" : "Внешний API";
    }
}
