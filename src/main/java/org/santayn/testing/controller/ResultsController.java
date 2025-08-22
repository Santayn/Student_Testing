package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.AnswerResultRepository;
import org.santayn.testing.service.StudentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ResultsController {

    private final AnswerResultRepository answerRepo;
    private final StudentService studentService;

    @GetMapping("/kubstuTest/results")
    public String viewResults(Authentication auth, Principal principal, Model model) {
        // Защита от неаутентифицированного доступа
        if (auth == null || !auth.isAuthenticated() || principal == null) {
            return "redirect:/login";
        }

        boolean isTeacher = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<AnswerResult> results;
        if (isTeacher) {
            // преподаватель видит все результаты
            results = answerRepo.findAll();
        } else {
            // студент — только свои (с обработкой случая, когда студент не найден)
            try {
                Student student = studentService.findByLogin(principal.getName());
                results = answerRepo.findByStudentId(student.getId());
            } catch (UsernameNotFoundException ex) {
                // Можно отдать пустой список и показать сообщение
                results = Collections.emptyList();
                model.addAttribute("errorMessage",
                        "Учётная запись студента не найдена для пользователя: " + principal.getName());
            }
        }

        model.addAttribute("results", results);
        model.addAttribute("isTeacher", isTeacher);
        model.addAttribute("username", principal.getName());
        return "result-list";
    }
}
