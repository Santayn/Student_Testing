package org.santayn.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LegacyPageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login.html";
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/register.html";
    }

    @GetMapping("/kubstuTest")
    public String main() {
        return "redirect:/main.html";
    }

    @GetMapping("/kubstuTest/about")
    public String about() {
        return "redirect:/about.html";
    }

    @GetMapping("/kubstuTest/profile")
    public String profile() {
        return "redirect:/profile.html";
    }

    @GetMapping("/kubstuTest/subjects")
    public String subjects() {
        return "redirect:/subjects.html";
    }

    @GetMapping("/kubstuTest/group/{groupId}/students")
    public String students(@PathVariable Integer groupId) {
        return "redirect:/students.html?groupId=" + groupId;
    }

    @GetMapping("/kubstuTest/group/{groupId}/students/{studentId}")
    public String studentDetails(@PathVariable Integer groupId,
                                 @PathVariable Integer studentId) {
        return "redirect:/student-details.html?groupId=" + groupId + "&studentId=" + studentId;
    }
}