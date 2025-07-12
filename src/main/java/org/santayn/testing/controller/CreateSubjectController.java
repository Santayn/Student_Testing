package org.santayn.testing.controller;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class CreateSubjectController {

    private final SubjectService subjectService;
    private final CreateSubjectService createSubjectService;
    private final FacultyService facultyService;

    public CreateSubjectController(SubjectService subjectService, CreateSubjectService createSubjectService, FacultyService facultyService) {
        this.subjectService = subjectService;
        this.createSubjectService = createSubjectService;
        this.facultyService = facultyService;
    }


    // === Показать форму создания группы ===
    @GetMapping("/subject/create")
    public String showCreateSubjectForm(Model model) {
        List<Subject> subjects = subjectService.getAllSubjects(); // Получаем список групп

        model.addAttribute("subject", new Subject());
        model.addAttribute("subjects", subjects); // Добавляем атрибут с группами

        return "create-subject"; // Шаблон Thymeleaf
    }

    // === Обработать отправку формы ===
    @PostMapping("/subject/create")
    public String createSubject(
            @RequestParam String name,
            @RequestParam String description) {

        createSubjectService.addSubject(name, description);
        return "redirect:/kubstuTest/subject/list";
    }

    // === Удалить факультет ===
    @PostMapping("/subject/delete")
    public String deleteSubject(@RequestParam Integer SubjectId) {
        createSubjectService.deleteSubject(SubjectId);
        return "redirect:/kubstuTest/subject/list";
    }
}