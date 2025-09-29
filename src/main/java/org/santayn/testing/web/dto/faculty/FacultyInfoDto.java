// FacultyInfoDto.java
package org.santayn.testing.web.dto.faculty;

public record FacultyInfoDto(
        Integer facultyId,
        String facultyName,
        Integer groupId,
        String groupName,
        String groupTitle
) {}
