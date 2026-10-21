package org.santayn.testing.service.ai;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Достаёт JSON из ответа модели: модели оборачивают его в ```json, добавляют пояснения
 * до и после, оставляют висячие запятые.
 */
public final class AiJson {

    private static final ObjectMapper LENIENT = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();

    private AiJson() {
    }

    public static JsonNode parseObject(String text) {
        if (text == null) {
            throw new AiUnavailableException("ИИ вернул пустой ответ.");
        }
        String cleaned = stripCodeFence(text.trim());
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AiUnavailableException("ИИ вернул ответ не в том формате.");
        }
        try {
            JsonNode node = LENIENT.readTree(cleaned.substring(start, end + 1));
            if (node == null || !node.isObject()) {
                throw new AiUnavailableException("ИИ вернул ответ не в том формате.");
            }
            return node;
        } catch (AiUnavailableException error) {
            throw error;
        } catch (Exception error) {
            throw new AiUnavailableException("ИИ вернул повреждённый JSON.", error);
        }
    }

    /** Убирает обёртку ```markdown / ``` вокруг всего ответа. */
    public static String stripCodeFence(String text) {
        String value = text == null ? "" : text.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            value = firstLineEnd < 0 ? "" : value.substring(firstLineEnd + 1);
            if (value.endsWith("```")) {
                value = value.substring(0, value.length() - 3);
            }
        }
        return value.trim();
    }
}
