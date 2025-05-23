package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest/") // Базовый путь для групп
public class StudentController {

    private final UserRegisterService userRegisterService;
    private final StudentService studentService;
    private final GroupService groupService;
    private final TeacherService teacherService;
    private final UserService userService;
    private final RoleService roleService;

    public StudentController(UserRegisterService userRegisterService, GroupService groupService, StudentService studentService,
                             TeacherService teacherService,UserService userService,RoleService roleService) {
        this.userRegisterService = userRegisterService;
        this.groupService = groupService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.userService = userService;
        this.roleService = roleService;
    }

    // Отображение списка студентов по groupId
    @GetMapping("/group/{groupId}/students")
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
    @GetMapping("/group/{groupId}/students/{studentId}")
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

    /**
     * Отображает страницу с выбором группы и действий над студентами
     */
    @GetMapping("manage-students")
    public String showManageStudentsPage(
            @RequestParam(required = false) Integer groupId,
            Model model) {
        // Получаем все группы
        List<Group> groups = groupService.getAllGroup();
        model.addAttribute("groups", groups);

        if (groupId != null) {
            // Если выбрана группа, получаем студентов в группе и доступных студентов
            List<Student> groupStudents = studentService.getStudentsByGroupID(groupId);
            List<Student> freeStudents = studentService.findFreeStudents();

            model.addAttribute("groupId", groupId);
            model.addAttribute("groupStudents", groupStudents);
            model.addAttribute("freeStudents", freeStudents);
        }

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user roles: " + Arrays.toString(auth.getAuthorities().toArray()));

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