package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.answer.AnswerResult;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.AnswerResultRepository;
import org.santayn.testing.service.StudentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ResultsController {

    private final AnswerResultRepository answerRepo;
    private final StudentService studentService;

    @GetMapping("/kubstuTest/results")
    public String viewResults(Principal principal, Model model) {
        // Определяем роль
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTeacher = auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<AnswerResult> results;
        if (isTeacher) {
            // преподаватель видит все результаты
            results = answerRepo.findAll();
        } else {
            // студент — только свои
            Student student = studentService.findByLogin(principal.getName());
            results = answerRepo.findByStudentId(student.getId());
        }

        model.addAttribute("results",  results);
        model.addAttribute("isTeacher", isTeacher);
        return "result-list";
    }
}