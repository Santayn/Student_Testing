// src/main/java/org/santayn/testing/web/dto/lecture/LectureCreateRequest.java
package org.santayn.testing.web.dto.lecture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LectureCreateRequest(
        @NotNull Integer subjectId,
        @NotBlank String title,
        @NotBlank String content,
        String description
) {}
