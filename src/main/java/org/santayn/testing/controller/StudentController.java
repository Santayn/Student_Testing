package org.santayn.testing.controller;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.service.LectureService;
import org.santayn.testing.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/group") // Базовый путь для групп
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Отображение списка студентов по groupId
    @GetMapping("/{groupId}/students")
    public String getStudentsByGroup(@PathVariable Integer groupId, Model model) {
        System.out.println("Fetching students for group ID: " + groupId); // Логирование
        List<Student> students = studentService.getStudentsByGroupID(groupId);

        if (students.isEmpty()) {
            model.addAttribute("error", "В этой группе нет студентов.");
        }

        model.addAttribute("students", students);
        model.addAttribute("groupId", groupId);
        return "students";
    }

    // Отображение деталей конкретного студента
    @GetMapping("/{groupId}/students/{studentId}")
    public String getStudentDetails(@PathVariable Integer groupId,
                                    @PathVariable Integer studentId,
                                    Model model) {
        try {
            Student selectedStudent = studentService.getSpecificStudentByGroupIdAndStudentId(groupId, studentId);
            model.addAttribute("selectedStudent", selectedStudent);
            model.addAttribute("groupId", groupId); // Передаем groupId для ссылки "Назад"
            return "student-details"; // Имя шаблона Thymeleaf для отображения деталей студента
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/group/" + groupId + "/students"; // Перенаправляем обратно к списку студентов
        }
    }
}