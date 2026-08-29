package org.santayn.testing.web.dto.common;

import java.util.List;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        List<Map<String, Object>> details,
        String traceId
) {
    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(code, message, List.of(), traceId);
    }
}
