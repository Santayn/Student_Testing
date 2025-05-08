package org.santayn.testing.controller;

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

    public UserController(UserRegisterService userRegisterService, GroupService groupService,StudentService studentService) {
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
     * Отображение страницы для выбора группы и студентов
     */
    @GetMapping("create-group")
    public String showAddStudentsToGroupForm(Model model) {
        model.addAttribute("groups", groupService.getAllGroup());
        model.addAttribute("students", studentService.findAll());
        return "select-group-and-students";
    }

    /**
     * Обработка POST-запроса — добавление студентов в группу
     */
    @PostMapping("create-group")
    public String addStudentsToGroup(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("studentIds") List<Integer> studentIds) {

        // Вызываем метод через экземпляр сервиса
        groupService.addStudentsToGroup(groupId, studentIds);

        // Перенаправляем на профиль
        return "redirect:/kubstuTest/profile/me";
    }
    @GetMapping("remove-students")
    public String showRemoveStudentsFromGroupForm(Model model) {
        model.addAttribute("groups", groupService.getAllGroup());
        model.addAttribute("students", studentService.findAll());
        return "remove-group-and-students"; // Нужно создать этот шаблон
    }

    /**
     * Обработка POST-запроса — удаление студентов из группы
     */
    @PostMapping("remove-students")
    public String removeStudentsFromGroup(
            @RequestParam("groupId") Integer groupId,
            @RequestParam("studentIds") List<Integer> studentIds) {

        try {
            // Вызываем метод через экземпляр сервиса
            groupService.deleteStudentsFromGroup(groupId, studentIds);

            // Перенаправляем на профиль
            return "redirect:/kubstuTest/profile/me";
        } catch (Exception e) {
            // Логируем ошибку
            e.printStackTrace();
            // Перенаправляем на страницу с ошибкой
            return "redirect:/error?message=" + e.getMessage();
        }
    }
}