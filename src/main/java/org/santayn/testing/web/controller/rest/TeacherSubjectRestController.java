package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.TeacherService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.subject.SubjectShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-subjects")
public class TeacherSubjectRestController {

    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final UserSearch userSearch;

    public TeacherSubjectRestController(SubjectService subjectService,
                                        TeacherService teacherService,
                                        UserSearch userSearch) {
        this.subjectService = subjectService;
        this.teacherService = teacherService;
        this.userSearch = userSearch;
    }

    /** Предметы текущего авторизованного преподавателя */
    @GetMapping("/me")
    public List<SubjectShortDto> getMySubjects() {
        Teacher current = userSearch.getCurrentTeacher();               // <-- из SecurityContext
        List<Subject> subjects = subjectService.getSubjectsByTeacher(current);
        return subjects.stream().map(SubjectShortDto::from).toList();
    }

    /** Предметы, закреплённые за конкретным преподавателем */
    @GetMapping("/{teacherId}")
    public List<SubjectShortDto> getSubjectsOfTeacher(@PathVariable Integer teacherId) {
        return subjectService.getSubjectsByTecherID(teacherId)
                .stream()
                .map(SubjectShortDto::from)
                .toList();
    }

    /** Свободные предметы (не назначены ни одному преподавателю) */
    @GetMapping("/free")
    public List<SubjectShortDto> getFreeSubjects() {
        return subjectService.findFreeSubjects()
                .stream()
                .map(SubjectShortDto::from)
                .toList();
    }

    /** Назначить предметы преподавателю (тело — массив Integer subjectIds) */
    @PostMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addSubjectsToTeacher(@PathVariable Integer teacherId,
                                     @RequestBody List<Integer> subjectIds) {
        teacherService.addSubjectsToTeacher(teacherId, subjectIds);
    }

    /** Убрать предметы у преподавателя (тело — массив Integer subjectIds) */
    @DeleteMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSubjectsFromTeacher(@PathVariable Integer teacherId,
                                          @RequestBody List<Integer> subjectIds) {
        teacherService.deleteSubjectsFromTeacher(teacherId, subjectIds);
    }
}
