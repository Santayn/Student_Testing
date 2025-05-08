package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final StudentService studentService;

    public GroupController(GroupService groupService, StudentService studentService) {
        this.groupService = groupService;
        this.studentService = studentService;
    }

    // Отображение всех групп
    @GetMapping("/All")
    public String getAllGroups(Model model) {
        List<Group> groups = groupService.getAllGroup();
        if (groups.isEmpty()) {
            model.addAttribute("error", "Группы не найдены.");
        }
        model.addAttribute("groups", groups);
        return "groups";
    }

    // Получение информации о конкретной группе по имени
    @GetMapping("/{name}/")
    public String getGroupByName(@PathVariable String name, Model model) {
        try {
            Group selectedGroup = groupService.getSpecificGroupByGroupName(name);
            model.addAttribute("selectedGroup", selectedGroup);
            return "group-details"; // Имя шаблона Thymeleaf
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/groups/All"; // Перенаправляем обратно к списку групп
        }
    }

    // Форма для создания новой группы
    @GetMapping("/create")
    public String showCreateGroupForm(Model model) {
        List<Student> allStudents = studentService.findAll(); // Предполагается, что у вас есть такой метод
        model.addAttribute("students", allStudents);
        model.addAttribute("newGroup", new Group());
        return "create-group";
    }

    // Сохранение новой группы
    @PostMapping("/save")
    public String saveGroup(@ModelAttribute("newGroup") Group group,
                            @RequestParam("selectedStudents") List<Integer> selectedStudentIds) {
        // Добавление выбранных студентов в группу
        for (Integer studentId : selectedStudentIds) {
            Student student = studentService.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Студент не найден"));
            group.addStudent(student); // Метод addStudent должен быть реализован в классе Group
        }

        groupService.save(group); // Метод save должен быть реализован в GroupService

        return "redirect:/groups/All"; // После сохранения — перенаправление на список групп
    }
}