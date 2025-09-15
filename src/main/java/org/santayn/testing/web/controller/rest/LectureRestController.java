// src/main/java/org/santayn/testing/web/controller/rest/LectureRestController.java
package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.lecture.LectureCreateRequest;
import org.santayn.testing.web.dto.lecture.LectureResponse;
import org.santayn.testing.web.dto.lecture.LectureShortDto;
import org.santayn.testing.web.dto.lecture.LectureUpdateRequest;
import org.santayn.testing.web.dto.subject.SubjectShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lectures")
public class LectureRestController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserSearch userSearch;

    public LectureRestController(LectureService lectureService,
                                 SubjectService subjectService,
                                 UserSearch userSearch) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userSearch = userSearch;
    }

    /** Предметы текущего преподавателя (можно также использовать /api/v1/teacher-subjects/me) */
    @GetMapping("/my-subjects")
    public List<SubjectShortDto> mySubjects() {
        Teacher t = userSearch.getCurrentTeacher();
        List<Subject> subjects = subjectService.getSubjectsByTeacher(t);
        return subjects.stream().map(SubjectShortDto::from).toList();
    }

    /** Лекции по предмету (доступ только если предмет у текущего преподавателя) */
    @GetMapping("/by-subject/{subjectId}")
    public List<LectureResponse> listBySubject(@PathVariable Integer subjectId) {
        assertSubjectOwnedByCurrentTeacher(subjectId);
        return lectureService.getLecturesBySubjectId(subjectId)
                .stream()
                .map(LectureResponse::from)
                .toList();
    }

    /** Короткий список лекций по предмету (для селектов) */
    @GetMapping("/by-subject/{subjectId}/short")
    public List<LectureShortDto> listShortBySubject(@PathVariable Integer subjectId) {
        assertSubjectOwnedByCurrentTeacher(subjectId);
        return lectureService.getLecturesBySubjectId(subjectId)
                .stream()
                .map(LectureShortDto::from)
                .toList();
    }

    /** Получить одну лекцию */
    @GetMapping("/{lectureId}")
    public LectureResponse get(@PathVariable Integer lectureId) {
        Lecture l = lectureService.findById(lectureId);
        return LectureResponse.from(l);
    }

    /** Создать лекцию (проверяем, что предмет принадлежит текущему преподавателю) */
    @PostMapping
    public ResponseEntity<LectureResponse> create(@Valid @RequestBody LectureCreateRequest req) {
        assertSubjectOwnedByCurrentTeacher(req.subjectId());
        Lecture l = lectureService.addLecture(req.subjectId(), req.title(), req.content(), req.description());
        return ResponseEntity
                .created(URI.create("/api/v1/lectures/" + l.getId()))
                .body(LectureResponse.from(l));
    }

    /** Частичное обновление лекции; если меняется subjectId — проверяем доступ */
    @PatchMapping("/{lectureId}")
    public LectureResponse update(@PathVariable Integer lectureId,
                                  @RequestBody LectureUpdateRequest req) {
        Lecture l = lectureService.findById(lectureId);

        if (req.subjectId() != null) {
            assertSubjectOwnedByCurrentTeacher(req.subjectId());
            Subject s = subjectService.findById(req.subjectId());
            l.setSubject(s);
        }
        if (req.title() != null)       l.setTitle(req.title());
        if (req.content() != null)     l.setContent(req.content());
        if (req.description() != null) l.setDescription(req.description());

        // Управляемая сущность — изменения запишутся в конце запроса (OpenSessionInView).
        return LectureResponse.from(lectureService.findById(l.getId()));
    }

    /** Удалить лекцию */
    @DeleteMapping("/{lectureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer lectureId) {
        lectureService.deleteLecture(lectureId);
    }

    /* ================= helpers ================= */

    private void assertSubjectOwnedByCurrentTeacher(Integer subjectId) {
        Teacher t = userSearch.getCurrentTeacher();
        boolean ok = subjectService.getSubjectsByTeacher(t).stream()
                .anyMatch(s -> s.getId().equals(subjectId));
        if (!ok) {
            throw new RuntimeException("Нет доступа к предмету id=" + subjectId);
        }
    }
}

/* =======================================================================
   Доп.эндпоинт ИМЕННО под фронт: GET /api/v1/subjects/{subjectId}/lectures
   Оставлен в этом же файле, класс без public-модификатора.
   ======================================================================= */
@RestController
@RequestMapping("/api/v1/subjects/{subjectId}/lectures")
class SubjectLecturesRestController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserSearch userSearch;

    public SubjectLecturesRestController(LectureService lectureService,
                                         SubjectService subjectService,
                                         UserSearch userSearch) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userSearch = userSearch;
    }

    /** Короткий список лекций по предмету для выпадающего списка (ожидается фронтом) */
    @GetMapping
    public List<LectureShortDto> listShort(@PathVariable Integer subjectId) {
        // проверка доступа преподавателя к предмету
        Teacher t = userSearch.getCurrentTeacher();
        boolean allowed = subjectService.getSubjectsByTeacher(t).stream()
                .anyMatch(s -> s.getId().equals(subjectId));
        if (!allowed) {
            throw new RuntimeException("Учитель не имеет доступа к предмету id=" + subjectId);
        }

        return lectureService.getLecturesBySubjectId(subjectId).stream()
                .map(LectureShortDto::from)
                .toList();
    }
}
