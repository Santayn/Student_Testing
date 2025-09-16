// src/main/java/org/santayn/testing/web/dto/results/ResultsResponse.java
package org.santayn.testing.web.dto.results;

import org.santayn.testing.web.dto.test.AnswerResultDto;

import java.util.List;
import java.util.Map;

public record ResultsResponse(
        List<AnswerResultDto> results,
        StatsDto stats,
        String selectedTestName,
        String selectedGroupName,
        String selectedStudentName,
        Map<Integer, String> studentNames
) {}
