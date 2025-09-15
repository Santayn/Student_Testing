// src/main/java/org/santayn/testing/web/dto/lecture/LectureResponse.java
package org.santayn.testing.web.dto.lecture;

import org.santayn.testing.models.lecture.Lecture;

public record LectureResponse(
        Integer id,
        Integer subjectId,
        String title,
        String content,
        String description
) {
    public static LectureResponse from(Lecture l) {
        return new LectureResponse(
                l.getId(),
                l.getSubject() != null ? l.getSubject().getId() : null,
                l.getTitle(),
                l.getContent(),
                l.getDescription()
        );
    }
}
