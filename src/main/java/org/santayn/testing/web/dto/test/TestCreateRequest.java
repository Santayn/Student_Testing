package org.santayn.testing.web.dto.test;

import jakarta.validation.constraints.*;
import java.util.List;

public record TestCreateRequest(
        @NotNull Integer topicId,
        @NotNull Integer lectureId,
        @NotBlank String name,
        @NotBlank String description,
        @Min(1) int questionCount,
        @NotNull @Size(min = 1) List<@NotNull Integer> groupIds
) {}
