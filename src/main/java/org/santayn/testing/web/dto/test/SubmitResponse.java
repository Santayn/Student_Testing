// src/main/java/org/santayn/testing/web/dto/test/SubmitResponse.java
package org.santayn.testing.web.dto.test;

import java.util.List;

public record SubmitResponse(
        Integer testId,
        Integer studentId,
        Integer correctCount,   // было correctCnt
        Integer totalCount,     // было totalCnt
        List<AnswerResultDto> details // было results
) {}
