package org.santayn.testing.web.dto.common;

import java.util.List;
import java.util.Map;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<Map<String, Object>> details,
        String requestId
) {
    public static ErrorResponse of(int status, String code, String message, String requestId) {
        return new ErrorResponse(status, code, message, List.of(), requestId);
    }
}
