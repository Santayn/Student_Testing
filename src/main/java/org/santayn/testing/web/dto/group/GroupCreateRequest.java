package org.santayn.testing.web.dto.group;

import jakarta.validation.constraints.NotBlank;

public record GroupCreateRequest(
        @NotBlank String name,
        String title,
        Integer courseCode,
        Integer facultyId
) {}
