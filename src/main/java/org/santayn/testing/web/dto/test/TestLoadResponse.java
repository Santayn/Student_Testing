// src/main/java/org/santayn/testing/web/dto/test/TestLoadResponse.java
package org.santayn.testing.web.dto.test;

import java.util.List;

public record TestLoadResponse(
        TestShortDto test,
        List<QuestionDto> questions,
        Integer studentId
) {}
