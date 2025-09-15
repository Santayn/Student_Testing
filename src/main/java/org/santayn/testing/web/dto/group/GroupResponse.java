package org.santayn.testing.web.dto.group;

import org.santayn.testing.models.group.Group;

public record GroupResponse(
        Integer id,
        String name,
        String title,
        Integer courseCode,
        Integer facultyId
) {
    public static GroupResponse from(Group g) {
        return new GroupResponse(
                g.getId(),
                g.getName(),
                g.getTitle(),
                null, // 👈 поле courseCode не используется
                g.getFaculty() != null ? g.getFaculty().getId() : null
        );
    }
}
