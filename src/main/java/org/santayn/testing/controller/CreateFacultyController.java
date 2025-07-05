package org.santayn.testing.controller;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.service.CreateFacultyService;
import org.santayn.testing.service.CreateGroupService;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.GroupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class CreateFacultyController {

    private final GroupService groupService;
    private final CreateFacultyService createFacultyService;
    private final FacultyService facultyService;

    public CreateFacultyController(GroupService groupService,  CreateFacultyService createFacultyService,FacultyService facultyService) {
        this.groupService = groupService;
        this.createFacultyService = createFacultyService;
        this.facultyService = facultyService;
    }

    // === Показать форму создания группы ===
    @GetMapping("/faculty/create")
    public String showCreateFacultyForm(Model model) {
        List<Faculty> faculties = facultyService.getAllFaculty();

        model.addAttribute("faculties", new Faculty());
        model.addAttribute("faculties", faculties);


        return "create-faculty"; // Шаблон Thymeleaf
    }

    // === Обработать отправку формы ===
    @PostMapping("/faculty/create")
    public String createFaculty(
            @RequestParam String name) {

        createFacultyService.addFaculty(name);
        return "redirect:/kubstuTest/faculty/list";
    }

    // === Показать список групп (опционально) ===
    @GetMapping("/faculty/list")
    public String listFacultys(Model model) {
        List<Faculty> faculties = facultyService.getAllFaculty();
        model.addAttribute("faculties", faculties);
        return "faculty-list"; // Шаблон Thymeleaf
    }
}