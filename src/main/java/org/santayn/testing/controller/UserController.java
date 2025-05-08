package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.StudentService;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/kubstuTest/")
public class UserController {

    private final UserRegisterService userRegisterService;
    private final StudentService studentService;
    private final GroupService groupService;

    public UserController(UserRegisterService userRegisterService, GroupService groupService, StudentService studentService) {
        this.userRegisterService = userRegisterService;
        this.groupService = groupService;
        this.studentService = studentService;
    }

    @GetMapping("profile/me")
    public String getUserProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentLogin = auth.getName(); // Получаем логин текущего пользователя
        User selectUser = userRegisterService.findUserByLogin(currentLogin);
        List<Group> allGroups = groupService.getAllGroup();

        model.addAttribute("selectUser", selectUser);
        model.addAttribute("groups", allGroups);
        return "profile"; // Имя Thymeleaf шаблона
    }

    /**
     * Открытие страницы управления студентами в группе
     */
    @GetMapping("students")
    public String getGroupStudentsPage(
            @PathVariable Integer groupId,
            Model model) {
        Group group = groupService.getGroupById(groupId);
        List<Student> studentsInGroup = studentService.getStudentsByGroupID(groupId);
        List<Student> freeStudents = studentService.findFreeStudents(groupId);

        model.addAttribute("group", group);
        model.addAttribute("studentsInGroup", studentsInGroup);
        model.addAttribute("freeStudents", freeStudents);
        return "group_students"; // Шаблон Thymeleaf: group_students.html
    }

    /**
     * Добавить студентов в группу
     */
    @PostMapping("add-students")
    public String addStudentsToGroup(
            @PathVariable Integer groupId,
            @RequestParam("studentIds") List<Integer> studentIds) {
        groupService.addStudentsToGroup(groupId, studentIds);
        return "redirect:/kubstuTest/group/" + groupId + "/students";
    }

    /**
     * Удалить студентов из группы
     */
    @PostMapping("remove-students")
    public String removeStudentsFromGroup(
            @PathVariable Integer groupId,
            @RequestParam("studentIds") List<Integer> studentIds) {
        groupService.deleteStudentsFromGroup(groupId, studentIds);
        return "redirect:/kubstuTest/group/" + groupId + "/students";
    }
}