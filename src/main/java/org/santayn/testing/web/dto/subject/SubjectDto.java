package org.santayn.testing.web.dto.subject;

import org.santayn.testing.models.subject.Subject;

public record SubjectDto(
        Integer id,
        String name,
        String description
) {
    public static SubjectDto from(Subject subject) {
        return new SubjectDto(
                subject.getId(),
                subject.getName(),
                subject.getDescription()
        );
    }
}
