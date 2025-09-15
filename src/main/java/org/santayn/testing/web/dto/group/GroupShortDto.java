package org.santayn.testing.web.dto.group;

import org.santayn.testing.models.group.Group;

public record GroupShortDto(
        Integer id,
        String name
) {
    public static GroupShortDto from(Group g) {
        return new GroupShortDto(
                g.getId(),
                g.getName()
        );
    }
}
