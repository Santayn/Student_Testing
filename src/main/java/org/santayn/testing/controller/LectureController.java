package org.santayn.testing.controller;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class LectureController {

    private final LectureService lectureService;
    private final SubjectService subjectService;
    private final UserSearch userSearch;

    public LectureController(LectureService lectureService, SubjectService subjectService,UserSearch userSearch) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
        this.userSearch = userSearch;
    }

    /**
     * Показать страницу управления лекциями для предмета
     */
    @GetMapping("/manage-lectures-subject")
    public String showManageLecturesPage(
            @RequestParam(required = false) Integer subjectId,
            Model model) {

        // Получаем текущего учителя
        Teacher currentTeacher = userSearch.getCurrentTeacher();

        // Получаем все предметы, которые принадлежат текущему учителю
        List<Subject> teacherSubjects = subjectService.getSubjectsByTeacher(currentTeacher);
        model.addAttribute("subjects", teacherSubjects);

        if (subjectId != null) {
            // Проверяем, что выбранный предмет принадлежит текущему учителю
            boolean isSubjectBelongsToTeacher = teacherSubjects.stream()
                    .anyMatch(subject -> subject.getId().equals(subjectId));

            if (!isSubjectBelongsToTeacher) {
                throw new RuntimeException("Teacher does not have access to this subject");
            }

            // Получаем лекции для выбранного предмета
            List<Lecture> lectures = lectureService.getLecturesBySubjectId(subjectId);
            model.addAttribute("subjectId", subjectId);
            model.addAttribute("lectures", lectures);
        }

        return "manage-lectures-subject";
    }

    /**
     * Добавить новую лекцию
     */
    @PostMapping("/add-lecture-to-subject")
    public String addLectureToSubject(
            @RequestParam Integer subjectId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String description) {
        lectureService.addLecture(subjectId, title, content, description);
        return "redirect:/kubstuTest/manage-lectures-subject?subjectId=" + subjectId;
    }

    /**
     * Удалить лекцию
     */
    @PostMapping("/remove-lecture-from-subject")
    public String removeLectureFromSubject(
            @RequestParam Integer subjectId,
            @RequestParam Integer lectureId) {
        lectureService.deleteLecture(lectureId);
        return "redirect:/kubstuTest/manage-lectures-subject?subjectId=" + subjectId;
    }
    // Отображение списка лекций по subjectId
    @GetMapping("/{subjectId}/lectures")
    public String getLecturesBySubject(@PathVariable Integer subjectId, Model model) {
        List<Lecture> lectures = lectureService.getLectureByID(subjectId);
        model.addAttribute("lectures", lectures);
        model.addAttribute("subjectId", subjectId); // Передаем subjectId в модель для использования в форме
        return "lectures"; // Имя шаблона Thymeleaf
    }

    // Отображение деталей конкретной лекции
    @GetMapping("/{subjectId}/lecture/{lectureId}")
    public String getLectureDetails(@PathVariable Integer subjectId, @PathVariable Integer lectureId, Model model) {
        try {
            Lecture selectedLecture = lectureService.getSpecificLectureBySubjectIdAndLectureId(subjectId, lectureId);
            model.addAttribute("selectedLecture", selectedLecture);
            model.addAttribute("subjectId", subjectId); // Передаем subjectId для ссылки "Назад"
            return "lecture-details"; // Имя шаблона Thymeleaf для отображения деталей лекции
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "lectures"; // Возвращаемся к списку лекций с сообщением об ошибке
        }
    }
    @GetMapping("/by-subject/{subjectId}")
    public ResponseEntity<List<Lecture>> getLecturesBySubject(@PathVariable Integer subjectId) {
        List<Lecture> lectures = lectureService.getLecturesBySubject(subjectId);
        return ResponseEntity.ok(lectures);
    }
}