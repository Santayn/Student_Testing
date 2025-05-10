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
        model.addAttribute("selectUser", selectUser);
        return "profile"; // Имя Thymeleaf шаблона
    }

    /**
     * Отображает страницу с выбором группы и действий над студентами
     */
    @GetMapping("manage-students")
    public String showManageStudentsPage(Model model) {
        List<Group> groups = groupService.getAllGroup();
        List<Student> allStudents = studentService.findAll();

        model.addAttribute("groups", groups);
        model.addAttribute("allStudents", allStudents);
        return "manage_students"; // Имя Thymeleaf шаблона
    }

    /**
     * Обрабатывает POST-запрос: добавляет выбранных студентов в выбранную группу
     */
    @PostMapping("add-students-to-group")
    public String addStudentsToGroup(
            @RequestParam Integer groupId,
            @RequestParam List<Integer> selectedStudentIds) {
        groupService.addStudentsToGroup(groupId, selectedStudentIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-students"; // Перезагрузка страницы
    }

    /**
     * Обрабатывает POST-запрос: удаляет выбранных студентов из группы
     */
    @PostMapping("remove-students-from-group")
    public String removeStudentsFromGroup(
            @RequestParam Integer groupId,
            @RequestParam List<Integer> selectedStudentIds) {
        groupService.deleteStudentsFromGroup(groupId, selectedStudentIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-students"; // Перезагрузка страницы
    }

    /**
     * AJAX-эндпоинт для получения студентов в указанной группе
     */
    @GetMapping("students-in-group/{groupId}")
    @ResponseBody
    public List<Student> getStudentsInGroup(@PathVariable Integer groupId) {
        return studentService.getStudentsByGroupID(groupId);
    }

    /**
     * AJAX-эндпоинт для получения свободных студентов (не входящих в группу)
     */
    @GetMapping("free-students/{groupId}")
    @ResponseBody
    public List<Student> getFreeStudents() {
        return studentService.findFreeStudents();
    }
}