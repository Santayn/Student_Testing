package org.santayn.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // Главная страница с разделами
    @GetMapping("/kubstuTest")
    public String showMainPage(Model model) {
        model.addAttribute("pageTitle", "Главная страница");
        return "main";
    }

    // Раздел "О платформе"
    @GetMapping("/kubstuTest/about")
    public String showAboutPage(Model model) {
        model.addAttribute("pageTitle", "О платформе");
        return "about";
    }

    // Раздел "Личный кабинет" — перенаправление на UserController
    @GetMapping("/kubstuTest/profile")
    public String redirectProfile() {
        return "redirect:/kubstuTest/profile/me"; // Передаём специальный путь для текущего пользователя
    }

    // Раздел "Предметы"
    @GetMapping("/kubstuTest/subjects")
    public String showSubjectsPage(Model model) {
        model.addAttribute("pageTitle", "Предметы");
        return "redirect:/1/subjects";
    }
}