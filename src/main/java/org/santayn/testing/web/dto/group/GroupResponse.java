package org.santayn.testing.web.dto.group;

public record GroupResponse(
        Integer id,
        String name,
        String title,
        Integer courseCode,
        Integer facultyId
) {}
