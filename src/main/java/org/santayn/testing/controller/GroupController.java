package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/group") // Базовый путь для групп
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Отображение списка студентов по groupId
    @GetMapping("/All")
    public String getAllGroups(Model model) {
        System.out.println("Fetching all groups"); // Логирование
        List<Group> groups = groupService.getAllGroup();
        if (groups.isEmpty()) {
            model.addAttribute("error", "Группы не найдены.");
        }
        model.addAttribute("groups", groups);
        return "groups";
    }

    // Отображение деталей конкретного студента
    @GetMapping("/{groupName}/")
    public String getGroupByName(@PathVariable String name,
                                    Model model) {
        try {
            Group selectedGroup = groupService.getSpecificGroupByGroupName(name);
            model.addAttribute("selectedGroup", selectedGroup);
            return "group-details"; // Имя шаблона Thymeleaf для отображения деталей студента
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/groups"; // Перенаправляем обратно к списку студентов
        }
    }
}