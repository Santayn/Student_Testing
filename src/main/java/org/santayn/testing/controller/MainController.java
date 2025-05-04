package org.santayn.testing.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // Главная страница с разделами
    @GetMapping("/kubstuTest")
    public String showMainPage(Model model) {
        // Добавляем данные для отображения в шаблоне (если нужно)
        model.addAttribute("pageTitle", "Главная страница");
        return "main"; // Возвращает имя Thymeleaf шаблона (main.html)
    }

    // Раздел "О платформе"
    @GetMapping("/kubstuTest/about")
    public String showAboutPage(Model model) {
        model.addAttribute("pageTitle", "О платформе");
        return "about"; // Возвращает имя Thymeleaf шаблона (about.html)
    }

    // Раздел "Личный кабинет"
    @GetMapping("/kubstuTest/profile")
    public String showProfilePage(Model model) {
        model.addAttribute("pageTitle", "Личный кабинет");
        return "profile"; // Возвращает имя Thymeleaf шаблона (profile.html)
    }

    // Раздел "Предметы"
    @GetMapping("/kubstuTest/subjects")
    public String showSubjectsPage(Model model) {
        model.addAttribute("pageTitle", "Предметы");
        return "subjects"; // Возвращает имя Thymeleaf шаблона (subjects.html)
    }
}