package org.santayn.testing.web.dto.test;

import java.util.List;

public record TestDto(
        Integer id,
        String name,
        String description,
        Integer questionCount,
        Integer topicId,
        String topicName,
        Integer lectureId,
        String lectureTitle,
        List<Integer> groupIds
) {}
