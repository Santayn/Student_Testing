// src/main/java/org/santayn/testing/web/dto/lecture/LectureUpdateRequest.java
package org.santayn.testing.web.dto.lecture;

public record LectureUpdateRequest(
        Integer subjectId,
        String title,
        String content,
        String description
) {}
