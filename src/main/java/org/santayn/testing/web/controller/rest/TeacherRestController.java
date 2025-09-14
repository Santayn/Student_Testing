// src/main/java/org/santayn/testing/web/controller/rest/TeacherRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.TeacherService;
import org.santayn.testing.web.dto.teacher.TeacherShortDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherRestController {

    private final TeacherService teacherService;

    public TeacherRestController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** Список преподавателей для выпадающего списка */
    @GetMapping
    public List<TeacherShortDto> list() {
        return teacherService.getAllTeacher().stream()
                .map(TeacherShortDto::from)
                .toList();
    }
}
