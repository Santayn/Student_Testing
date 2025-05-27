package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest/")
public class TeacherController {


    private final GroupService groupService;
    private final TeacherService teacherService;


    public TeacherController( GroupService groupService, TeacherService teacherService) {

        this.groupService = groupService;
        this.teacherService = teacherService;
    }

    @GetMapping("manage-teachers")
    public String showManageTeachersPage(
            @RequestParam(required = false) Integer teacherId,
            Model model) {
        // Получаем все группы
        List<Teacher> teachers = teacherService.getAllTeacher();
        model.addAttribute("teachers", teachers);

        if (teacherId != null) {
            // Если выбрана группа, получаем студентов в группе и доступных студентов
            List<Group> teacherGroups = groupService.getGroupsByTecherID(teacherId);
            List<Group> freeGroups = groupService.findFreeGroups();

            model.addAttribute("teacherId", teacherId);
            model.addAttribute("teacherGroups", teacherGroups);
            model.addAttribute("freeGroups", freeGroups);
        }

        return "manage-teachers"; // Имя Thymeleaf шаблона
    }

    @PostMapping("add-groups-to-teacher")
    public String addGroupsToTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedGroupIds) {
        teacherService.addGroupsToTeacher(teacherId, selectedGroupIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers"; // Перезагрузка страницы
    }
    @PostMapping("remove-groups-from-teacher")
    public String removeGroupsFromTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedGroupIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user roles: " + Arrays.toString(auth.getAuthorities().toArray()));

        teacherService.deleteGroupsFromTeacher(teacherId, selectedGroupIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers"; // Перезагрузка страницы
    }
    @GetMapping("groups-in-teacher/{teacherId}")
    @ResponseBody
    public List<Group> getGroupsInTeacher(@PathVariable Integer teacherId) {
        return groupService.getGroupsByTecherID(teacherId);
    }
    @GetMapping("free-groups/{teacherId}")
    @ResponseBody
    public List<Group> getFreeGroups() {
        return groupService.findFreeGroups();
    }
}
