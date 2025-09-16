// src/main/java/org/santayn/testing/web/controller/rest/LectureRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.lecture.LectureResponse;
import org.santayn.testing.web.dto.lecture.LectureShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/lectures")
class SubjectLecturesRestController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserService userService;
    private final UserSearch userSearch;

    public SubjectLecturesRestController(LectureService lectureService,
                                         SubjectService subjectService,
                                         UserService userService,
                                         UserSearch userSearch) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.userSearch = userSearch;
    }

    /** Список лекций по предмету (короткий формат для списка) */
    @GetMapping
    public List<LectureShortDto> list(@PathVariable Integer subjectId) {
        ensureSubjectExists(subjectId);
        ensureAccessLenient(subjectId);

        return lectureService.getLecturesBySubjectId(subjectId)
                .stream()
                .map(LectureShortDto::from)
                .toList();
    }

    /** Детали одной лекции */
    @GetMapping("/{lectureId}")
    public LectureResponse details(@PathVariable Integer subjectId,
                                   @PathVariable Integer lectureId) {
        ensureSubjectExists(subjectId);
        ensureAccessLenient(subjectId);

        Lecture l = lectureService.findById(lectureId);
        if (l.getSubject() == null || !subjectId.equals(l.getSubject().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лекция не найдена в этом предмете");
        }
        return LectureResponse.from(l);
    }

    /* ================= helpers ================= */

    private void ensureSubjectExists(Integer subjectId) {
        Subject s = subjectService.findById(subjectId);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Предмет не найден");
        }
    }

    private void ensureAccessLenient(Integer subjectId) {
        try {
            Teacher t = userSearch.getCurrentTeacher();
            boolean owns = subjectService.getSubjectsByTeacher(t).stream()
                    .anyMatch(s -> s.getId().equals(subjectId));
            if (!owns) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Нет доступа к предмету");
            }
        } catch (RuntimeException ignoredNotTeacher) {
            // студент или гость — разрешаем читать
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> runtimeTo404(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}

/* =======================================================================
   Публичные эндпоинты под фронт (HTML страницы)
   ======================================================================= */
@RestController
@RequestMapping("/api/v1/public")
class PublicLectureRestController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserService userService;
    private final UserSearch userSearch;

    public PublicLectureRestController(LectureService lectureService,
                                       SubjectService subjectService,
                                       UserService userService,
                                       UserSearch userSearch) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.userSearch = userSearch;
    }

    /** Список лекций по предмету */
    @GetMapping("/subjects/{subjectId}/lectures")
    public List<LectureShortDto> list(@PathVariable Integer subjectId) {
        return lectureService.getLecturesBySubjectId(subjectId)
                .stream()
                .map(LectureShortDto::from)
                .toList();
    }

    /** Детали одной лекции */
    @GetMapping("/lectures/{lectureId}")
    public LectureResponse getLecture(@PathVariable Integer lectureId) {
        Lecture l = lectureService.findById(lectureId);
        if (l == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Лекция не найдена");
        return LectureResponse.from(l);
    }

    /** Тесты по лекции (короткий DTO) */
    @GetMapping("/lectures/{lectureId}/tests")
    public List<TestShortDto> tests(@PathVariable Integer lectureId) {
        // если учитель — возвращаем все тесты по лекции
        try {
            userSearch.getCurrentTeacher();
            return lectureService.getTestsForLecture(lectureId).stream()
                    .map(TestShortDto::from).toList();
        } catch (RuntimeException notTeacher) {
            // студент или гость
        }

        // студент: фильтруем по факультету/группе
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return List.of();

        User u = userService.getUserByUsername(auth.getName());
        if (u == null || u.getStudent() == null) return List.of();

        Integer studentId = u.getStudent().getId();
        return lectureService.getTestsForLectureAndStudent(lectureId, studentId)
                .stream().map(TestShortDto::from).toList();
    }

    public record TestShortDto(Integer id, String description, Integer questionCount) {
        public static TestShortDto from(Test t) {
            return new TestShortDto(t.getId(), t.getDescription(), t.getQuestionCount());
        }
    }
}
