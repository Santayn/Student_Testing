// src/main/java/org/santayn/testing/web/controller/rest/SubjectRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.subject.SubjectCreateRequest;
import org.santayn.testing.web.dto.subject.SubjectDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectRestController {

    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final UserService userService;

    public SubjectRestController(SubjectService subjectService,
                                 SubjectRepository subjectRepository,
                                 UserService userService) {
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
        this.userService = userService;
    }

    /** Все предметы */
    @GetMapping
    public List<SubjectDto> list() {
        return subjectService.getAllSubjects()
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Свободные предметы (без учителя) */
    @GetMapping("/free")
    public List<SubjectDto> free(@RequestParam(value = "facultyId", required = false) Integer ignoredFacultyId) {
        return subjectService.findFreeSubjects()
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Предметы по учителю */
    @GetMapping("/by-teacher/{teacherId}")
    public List<SubjectDto> byTeacher(@PathVariable Integer teacherId) {
        return subjectService.getSubjectsByTecherID(teacherId)
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Предметы по ID факультета */
    @GetMapping("/by-faculty/{facultyId}")
    public List<SubjectDto> byFaculty(@PathVariable Integer facultyId) {
        return subjectService.getSubjectsByFacultyId(facultyId)
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Один предмет по facultyId + subjectId */
    @GetMapping("/by-faculty/{facultyId}/{subjectId}")
    public SubjectDto oneFromFaculty(@PathVariable Integer facultyId,
                                     @PathVariable Integer subjectId) {
        Subject s = subjectService.findSubjectByIdAndFacultyId(facultyId, subjectId);
        return SubjectDto.from(s);
    }

    /** Предметы для текущего студента (по его факультету/группе) */
    @GetMapping("/by-student/me")
    public List<SubjectDto> byCurrentStudent() {
        User user = currentUser();
        Group group = ensureStudentGroup(user);
        Faculty faculty = ensureFaculty(group);

        return subjectService.getSubjectsByFacultyId(faculty.getId())
                .stream()
                .map(SubjectDto::from)
                .toList();
    }

    /** Один предмет по id */
    @GetMapping("/{id}")
    public SubjectDto get(@PathVariable Integer id) {
        return SubjectDto.from(subjectService.findById(id));
    }

    /** Создать предмет */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubjectDto> create(@RequestBody SubjectCreateRequest body) {
        if (body == null || body.name() == null || body.name().isBlank()) {
            throw new IllegalArgumentException("Поле 'name' обязательно");
        }
        Subject s = new Subject();
        s.setName(body.name().trim());
        s.setDescription(body.description());
        Subject saved = subjectRepository.save(s);

        SubjectDto dto = SubjectDto.from(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/subjects/" + saved.getId()))
                .body(dto);
    }

    /** Частичное обновление предмета (name/description) */
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public SubjectDto update(@PathVariable Integer id,
                             @RequestBody SubjectCreateRequest body) {
        Subject s = subjectService.findById(id);
        if (body.name() != null && !body.name().isBlank()) {
            s.setName(body.name().trim());
        }
        if (body.description() != null) {
            s.setDescription(body.description());
        }
        return SubjectDto.from(subjectRepository.save(s));
    }

    /** Удалить предмет */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        subjectRepository.deleteById(id);
    }

    /* ================= helpers ================= */

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null ? auth.getName() : null);
        if (username == null) {
            throw new RuntimeException("Пользователь не авторизован");
        }
        return userService.getUserByUsername(username);
    }

    private Group ensureStudentGroup(User u) {
        if (u.getStudent() == null || u.getStudent().getGroup() == null) {
            throw new RuntimeException("У пользователя нет привязанной группы (student/group).");
        }
        return u.getStudent().getGroup();
    }

    private Faculty ensureFaculty(Group g) {
        if (g.getFaculty() == null || g.getFaculty().getId() == null) {
            throw new RuntimeException("У группы нет привязанного факультета.");
        }
        return g.getFaculty();
    }

    /* === 400 для ошибок бизнес-валидации === */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    /* === 404 для not found === */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
