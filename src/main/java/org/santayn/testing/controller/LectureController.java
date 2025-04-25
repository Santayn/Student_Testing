package org.santayn.testing.controller;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.service.LectureService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/subject") // Базовый путь для предметов
public class LectureController {

    private final LectureService lectureService;

    public LectureController(LectureService lectureService) {
        this.lectureService = lectureService;
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
}