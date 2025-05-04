package org.santayn.testing.controller;

import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register"; // Возвращает форму регистрации
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String login,
                               @RequestParam String password,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String phoneNumber,
                               @RequestParam(defaultValue = "USER") String roleName) {
        // Создаем роль (предполагается, что роли уже есть в базе данных)
        Role role = new Role();
        role.setName(roleName);

        // Регистрируем пользователя
        userService.registerUser(login, password, firstName, lastName, phoneNumber);
        return "redirect:/login"; // Перенаправляем на страницу входа
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // Возвращает форму входа
    }
}