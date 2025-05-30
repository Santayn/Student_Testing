package org.santayn.testing.controller;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class LectureController {

    private final LectureService lectureService;
    private final SubjectService subjectService;

    public LectureController(LectureService lectureService, SubjectService subjectService) {
        this.lectureService = lectureService;
        this.subjectService = subjectService;
    }

    /**
     * Показать страницу управления лекциями для предмета
     */
    @GetMapping("/manage-lectures-subject")
    public String showManageLecturesPage(
            @RequestParam(required = false) Integer subjectId,
            Model model) {
        List<Subject> subjects = subjectService.getAllSubjects();
        model.addAttribute("subjects", subjects);

        if (subjectId != null) {
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
}