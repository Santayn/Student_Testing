// src/main/java/org/santayn/testing/web/dto/student/StudentShortDto.java
package org.santayn.testing.web.dto.student;

import org.santayn.testing.models.student.Student;

public record StudentShortDto(Integer id, String fullName) {
    public static StudentShortDto from(Student s) {
        var u = s.getUser();
        String fn = (u != null && u.getFirstName() != null) ? u.getFirstName() : "";
        String ln = (u != null && u.getLastName()  != null) ? u.getLastName()  : "";
        String full = (fn + " " + ln).trim();
        if (full.isEmpty()) full = "Без имени";
        return new StudentShortDto(s.getId(), full);
    }
}
