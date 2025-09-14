package org.santayn.testing.web.dto.subject;

import org.santayn.testing.models.subject.Subject;

public record SubjectShortDto(
        Integer id,
        String name
) {
    public static SubjectShortDto from(Subject s) {
        return new SubjectShortDto(s.getId(), s.getName());
    }
}
