package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.service.TeacherService;
import org.santayn.testing.web.dto.teacher.TeacherShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherRestController {

    private final TeacherService teacherService;

    public TeacherRestController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    /** Список преподавателей (короткий формат для выпадающего списка) */
    @GetMapping
    public List<TeacherShortDto> list() {
        return teacherService.getAllTeacher().stream()
                .map(TeacherShortDto::from)
                .toList();
    }

    /** Один преподаватель по ID (короткий формат) */
    @GetMapping("/{id}")
    public TeacherShortDto getById(@PathVariable Integer id) {
        Teacher teacher = teacherService.getTeacherById(id);
        return TeacherShortDto.from(teacher);
    }

    /** Удалить преподавателя (связанная очистка в сервисе) */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        // TeacherService уже умеет каскадно удалять через deleteTeacherAndRelatedData
        teacherService.deleteTeacherAndRelatedData(id,
                teacherService.getTeacherById(id).getUser().getId());
    }
}
