package org.santayn.testing.controller;

import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    private final UserService userService;

    public MainController(UserService userService) {
        this.userService = userService;
    }

    // Главная страница с разделами
    @GetMapping("/kubstuTest")
    public String showMainPage(Model model) {
        model.addAttribute("pageTitle", "Главная страница");
        return "main"; // Имя шаблона Thymeleaf
    }

    // Раздел "О платформе"
    @GetMapping("/kubstuTest/about")
    public String showAboutPage(Model model) {
        model.addAttribute("pageTitle", "О платформе");
        return "about"; // Имя шаблона Thymeleaf
    }

    // Раздел "Личный кабинет" — перенаправление на UserController
    @GetMapping("/kubstuTest/profile")
    public String redirectProfile() {
        return "redirect:/kubstuTest/profile/me"; // Передаём специальный путь для текущего пользователя
    }

    // Раздел "Предметы" — перенаправление на список предметов по факультету
    @GetMapping("/kubstuTest/subjects")
    public String redirectSubjects() {
        Integer facultyId = getCurrentUserFacultyId(); // Получаем ID факультета текущего пользователя
        return "redirect:/kubstuTest/subjects/" + facultyId; // Перенаправляем на список предметов для факультета
    }

    // Метод для получения ID факультета текущего пользователя
    private Integer getCurrentUserFacultyId() {
        // Получаем имя пользователя из Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Получаем пользователя из базы данных
        User user = userService.getUserByUsername(username);

        // Проверяем, есть ли у пользователя связь со студентом
        if (user.getStudent() == null || user.getStudent().getGroup() == null) {
            throw new RuntimeException("Факультет не найден для пользователя: " + username);
        }

        // Получаем факультет через цепочку связей: User → Student → Group → Faculty
        Group group = user.getStudent().getGroup();
        Faculty faculty = group.getFaculty();
        System.out.println("Faculty ID: " + faculty.getId()+" "+ group.getId());
        if (faculty == null || faculty.getId() == null) {
            throw new RuntimeException("Факультет не найден для группы: " + group.getName());
        }

        return faculty.getId();
    }
}