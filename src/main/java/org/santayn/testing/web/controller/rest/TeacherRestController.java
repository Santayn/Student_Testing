package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.repository.TeacherRepository;
import org.santayn.testing.repository.Teacher_GroupRepository;
import org.santayn.testing.repository.Teacher_SubjectRepository;
import org.santayn.testing.service.TeacherService;
import org.santayn.testing.web.dto.subject.SubjectDto;
import org.santayn.testing.web.dto.teacher.TeacherShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// было: @RequestMapping("/api/v1/teachers")
// стало: поддерживаем И /api/v1/teachers, И /api/v1/teacher (для старого фронта)
@RequestMapping({"/api/v1/teachers", "/api/v1/teacher"})
public class TeacherRestController {

    private final TeacherService teacherService;
    private final TeacherRepository teacherRepository;
    private final Teacher_SubjectRepository teacherSubjectRepository;
    private final Teacher_GroupRepository teacherGroupRepository;

    public TeacherRestController(TeacherService teacherService,
                                 TeacherRepository teacherRepository,
                                 Teacher_SubjectRepository teacherSubjectRepository,
                                 Teacher_GroupRepository teacherGroupRepository) {
        this.teacherService = teacherService;
        this.teacherRepository = teacherRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.teacherGroupRepository = teacherGroupRepository;
    }

    /* ===== старые методы остаются рабочими на обоих базовых путях ===== */

    @GetMapping
    public List<TeacherShortDto> list() {
        return teacherService.getAllTeacher().stream()
                .map(TeacherShortDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TeacherShortDto getById(@PathVariable Integer id) {
        Teacher teacher = teacherService.getTeacherById(id);
        return TeacherShortDto.from(teacher);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        teacherService.deleteTeacherAndRelatedData(
                id, teacherService.getTeacherById(id).getUser().getId());
    }

    /* ===== добавлены КОРОТКИЕ маршруты для текущего преподавателя =====
       теперь доступны ОДНОВРЕМЕННО:
       - /api/v1/teacher/subjects
       - /api/v1/teacher/groups
       - /api/v1/teachers/subjects  (аналог, если где-то нужен)
       - /api/v1/teachers/groups
    */

    private static String currentLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth.getPrincipal();
        return (p instanceof UserDetails ud) ? ud.getUsername() : auth.getName();
    }

    private Teacher currentTeacher() {
        return teacherRepository.findByLogin(currentLogin()).orElseThrow();
    }

    /** Предметы текущего преподавателя */
    @GetMapping("/subjects")
    public List<SubjectDto> mySubjects() {
        Teacher t = currentTeacher();
        return teacherSubjectRepository.findByTeacherId(t.getId()).stream()
                .map(Teacher_Subject::getSubject)
                .map(SubjectDto::from)
                .toList();
    }

    /** Группы текущего преподавателя */
    private record GroupShortDto(Integer id, String name) {}
    @GetMapping("/groups")
    public List<GroupShortDto> myGroups() {
        Teacher t = currentTeacher();
        return teacherGroupRepository.findByTeacherId(t.getId()).stream()
                .map(Teacher_Group::getGroup)
                .map(g -> new GroupShortDto(g.getId(), g.getName()))
                .toList();
    }
}
