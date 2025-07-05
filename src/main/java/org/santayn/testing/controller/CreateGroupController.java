package org.santayn.testing.controller;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.service.CreateGroupService;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.GroupService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest")
public class CreateGroupController {

    private final GroupService groupService;
    private final CreateGroupService createGroupService;
    private final FacultyService facultyService;

    public CreateGroupController(GroupService groupService, CreateGroupService createGroupService,FacultyService facultyService) {
        this.groupService = groupService;
        this.createGroupService = createGroupService;
        this.facultyService = facultyService;
    }

    // === Показать форму создания группы ===
    @GetMapping("/group/create")
    public String showCreateGroupForm(Model model) {
        List<Faculty> faculties = facultyService.getAllFaculty();
        List<Group> groups = groupService.getAllGroup(); // Получаем список групп

        model.addAttribute("group", new Group());
        model.addAttribute("faculties", faculties);
        model.addAttribute("groups", groups); // Добавляем атрибут с группами

        return "create-group"; // Шаблон Thymeleaf
    }

    // === Обработать отправку формы ===
    @PostMapping("/group/create")
    public String createGroup(
            @RequestParam String name,
            @RequestParam Integer facultyId) {

        createGroupService.addGroup(name, facultyId);
        return "redirect:/kubstuTest/group/list";
    }

    // === Показать список групп (опционально) ===
    @GetMapping("/group/list")
    public String listGroups(Model model) {
        List<Group> groups = groupService.getAllGroup();
        model.addAttribute("groups", groups);
        return "group-list"; // Шаблон Thymeleaf
    }
}