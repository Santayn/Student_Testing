package org.santayn.testing.controller;

import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    // Отображение списка предметов по ID факультета
    @GetMapping("/subjects/{facultyId}")
    public String getSubjectsByFacultyId(@PathVariable Integer facultyId, Model model) {
        List<Subject> subjects = subjectService.getSubjectsByFacultyId(facultyId);

        if (subjects.isEmpty()) {
            model.addAttribute("error", "Предметы для данного факультета не найдены.");
        } else {
            model.addAttribute("subjects", subjects);
            model.addAttribute("facultyId", facultyId); // Передаем facultyId для ссылок
        }

        return "subjects"; // Имя шаблона Thymeleaf
    }

    // Отображение деталей конкретного предмета
    @GetMapping("/subjects/{facultyId}/{subjectId}")
    public String getSubjectDetails(@PathVariable Integer facultyId,
                                    @PathVariable Integer subjectId,
                                    Model model) {
        try {
            Subject selectedSubject = subjectService.getSpecificSubjectByFacultyIdAndSubjectId(facultyId, subjectId);
            model.addAttribute("selectedSubject", selectedSubject);
            model.addAttribute("facultyId", facultyId); // Передаем facultyId для ссылки "Назад"
            return "subject-details"; // Имя шаблона Thymeleaf для отображения деталей предмета
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/kubstuTest/subjects/" + facultyId; // Возвращаемся к списку предметов с сообщением об ошибке
        }
    }
}