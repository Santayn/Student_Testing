// src/main/java/org/santayn/testing/web/dto/lecture/LectureShortDto.java
package org.santayn.testing.web.dto.lecture;

import org.santayn.testing.models.lecture.Lecture;

public record LectureShortDto(Integer id, String title) {
    public static LectureShortDto from(Lecture l) {
        return new LectureShortDto(l.getId(), l.getTitle());
    }
}
