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
@RequestMapping("/facultys") // Базовый путь для предметов
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // Отображение списка лекций по subjectId
    @GetMapping("/{facultyId}/subjects")
    public String getSubjectByFaculty(@PathVariable Integer facultyId, Model model) {
        List<Subject> subjects = subjectService.getSubjectByID(facultyId);
        model.addAttribute("subjects", subjects);
        model.addAttribute("facultyId", facultyId); 
        return "subjects"; // Имя шаблона Thymeleaf
    }

    // Отображение деталей конкретной лекции
    @GetMapping("/{facultyId}/subject/{subjectId}")
    public String getSubjectDetails(@PathVariable Integer facultyId, @PathVariable Integer subjectId, Model model) {
        try {
            Subject selectedSubject = subjectService.getSpecificSubjectByFacultyIdAndSubjectId(facultyId, subjectId);
            model.addAttribute("selectedSubject", selectedSubject);
            model.addAttribute("facultyId", facultyId); // Передаем subjectId для ссылки "Назад"
            return "subject-details"; // Имя шаблона Thymeleaf для отображения деталей лекции
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "subjects"; // Возвращаемся к списку лекций с сообщением об ошибке
        }
    }
}