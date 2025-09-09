package org.santayn.testing.controller;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.test.Test;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.StudentService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class LectureController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserSearch     userSearch;
    private final StudentService studentService;

    public LectureController(LectureService lectureService,
                             SubjectService subjectService,
                             UserSearch userSearch,
                             StudentService studentService) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userSearch     = userSearch;
        this.studentService = studentService;
    }

    /** Страница управления лекциями (для преподавателя) */
    @GetMapping("/manage-lectures-subject")
    public String showManageLecturesPage(@RequestParam(required = false) Integer subjectId,
                                         Model model) {
        Teacher currentTeacher = userSearch.getCurrentTeacher();

        List<Subject> teacherSubjects = subjectService.getSubjectsByTeacher(currentTeacher);
        model.addAttribute("subjects", teacherSubjects);

        if (subjectId != null) {
            boolean allowed = teacherSubjects.stream().anyMatch(s -> s.getId().equals(subjectId));
            if (!allowed) {
                throw new RuntimeException("Teacher does not have access to this subject");
            }
            List<Lecture> lectures = lectureService.getLecturesBySubjectId(subjectId);
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("lectures", lectures);
        }

        return "manage-lectures-subject";
    }

    /** Добавить лекцию (для преподавателя) */
    @PostMapping("/add-lecture-to-subject")
    public String addLectureToSubject(@RequestParam Integer subjectId,
                                      @RequestParam String title,
                                      @RequestParam String content,
                                      @RequestParam String description) {
        lectureService.addLecture(subjectId, title, content, description);
        return "redirect:/kubstuTest/manage-lectures-subject?subjectId=" + subjectId;
    }

    /** Удалить лекцию (для преподавателя) */
    @PostMapping("/remove-lecture-from-subject")
    public String removeLectureFromSubject(@RequestParam Integer subjectId,
                                           @RequestParam Integer lectureId) {
        lectureService.deleteLecture(lectureId);
        return "redirect:/kubstuTest/manage-lectures-subject?subjectId=" + subjectId;
    }

    /** Список лекций по предмету */
    @GetMapping("/{subjectId}/lectures")
    public String getLecturesBySubject(@PathVariable Integer subjectId, Model model) {
        List<Lecture> lectures = lectureService.getLectureByID(subjectId);
        model.addAttribute("lectures", lectures);
        model.addAttribute("subjectId", subjectId);
        return "lectures";
    }

    /**
     * Детали лекции + доступные тесты:
     *  - Преподавателю — все тесты лекции
     *  - Студенту — только тесты, привязанные к его группе
     */
    @GetMapping("/{subjectId}/lecture/{lectureId}")
    public String getLectureDetails(@PathVariable Integer subjectId,
                                    @PathVariable Integer lectureId,
                                    Authentication auth,
                                    Model model) {
        try {
            Lecture selectedLecture =
                    lectureService.getSpecificLectureBySubjectIdAndLectureId(subjectId, lectureId);

            // Пытаемся определить роль:
            // если удастся получить Teacher — считаем, что это преподаватель
            List<Test> tests;
            try {
                userSearch.getCurrentTeacher(); // бросит исключение, если не преподаватель
                // Преподаватель: видит все тесты лекции
                tests = lectureService.getTestsForLecture(lectureId);
            } catch (RuntimeException notTeacher) {
                // Студент: видит только тесты своей группы
                String login = (auth != null) ? auth.getName() : null;
                Integer studentId = (login != null)
                        ? studentService.findByLogin(login).getId()
                        : null;
                if (studentId == null) {
                    tests = List.of();
                } else {
                    tests = lectureService.getTestsForLectureAndStudent(lectureId, studentId);
                }
            }

            model.addAttribute("selectedLecture", selectedLecture);
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("tests", tests);
            return "lecture-details";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            // вернём список лекций предмета с сообщением об ошибке
            List<Lecture> lectures = lectureService.getLectureByID(subjectId);
            model.addAttribute("lectures", lectures);
            model.addAttribute("subjectId", subjectId);
            return "lectures";
        }
    }

    /** REST-вариант: лекции по предмету (если нужно) */
    @GetMapping("/by-subject/{subjectId}")
    public ResponseEntity<List<Lecture>> getLecturesBySubjectRest(@PathVariable Integer subjectId) {
        return ResponseEntity.ok(lectureService.getLecturesBySubject(subjectId));
    }
}
