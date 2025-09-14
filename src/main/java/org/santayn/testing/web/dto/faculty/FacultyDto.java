package org.santayn.testing.web.dto.faculty;

import org.santayn.testing.models.faculty.Faculty;

public record FacultyDto(Integer id, String name) {
    public static FacultyDto from(Faculty f) {
        return new FacultyDto(f.getId(), f.getName());
    }
}
