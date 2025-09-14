// src/main/java/org/santayn/testing/web/dto/teacher/TeacherShortDto.java
package org.santayn.testing.web.dto.teacher;

import org.santayn.testing.models.teacher.Teacher;

public record TeacherShortDto(Integer id, String fullName) {
    public static TeacherShortDto from(Teacher t) {
        var u = t.getUser();
        String fn = u != null && u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u != null && u.getLastName()  != null ? u.getLastName()  : "";
        String full = (fn + " " + ln).trim();
        if (full.isEmpty()) full = "Без имени";
        return new TeacherShortDto(t.getId(), full);
    }
}
