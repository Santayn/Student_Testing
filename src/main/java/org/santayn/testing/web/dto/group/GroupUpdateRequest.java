package org.santayn.testing.web.dto.group;

public record GroupUpdateRequest(
        String name,
        String title,
        Integer courseCode,
        Integer facultyId
) {}
