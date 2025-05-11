package org.santayn.testing.controller;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.StudentService;
import org.santayn.testing.service.TeacherService;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/kubstuTest/")
public class UserController {

    private final UserRegisterService userRegisterService;
    private final StudentService studentService;
    private final GroupService groupService;
    private final TeacherService teacherService;

    public UserController(UserRegisterService userRegisterService, GroupService groupService, StudentService studentService,TeacherService teacherService) {
        this.userRegisterService = userRegisterService;
        this.groupService = groupService;
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    @GetMapping("profile/me")
    public String getUserProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentLogin = auth.getName(); // Получаем логин текущего пользователя
        User selectUser = userRegisterService.findUserByLogin(currentLogin);
        model.addAttribute("selectUser", selectUser);
        return "profile"; // Имя Thymeleaf шаблона
    }
    @GetMapping("manage-teachers")
    public String showManageTeachersPage(
            @RequestParam(required = false) Integer teacherId,
            Model model) {
        // Получаем все группы
        List<Teacher> teachers = teacherService.getAllTeacher();
        model.addAttribute("teachers", teachers);

        if (teacherId != null) {
            // Если выбрана группа, получаем студентов в группе и доступных студентов
            List<Group> teacherGroups = groupService.getGroupsByTecherID(teacherId);
            List<Group> freeGroups = groupService.findFreeGroups();

            model.addAttribute("teacherId", teacherId);
            model.addAttribute("teacherGroups", teacherGroups);
            model.addAttribute("freeGroups", freeGroups);
        }

        return "manage-teachers"; // Имя Thymeleaf шаблона
    }

    @PostMapping("add-groups-to-teacher")
    public String addGroupsToTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedGroupIds) {
        teacherService.addGroupsToTeacher(teacherId, selectedGroupIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers"; // Перезагрузка страницы
    }
    @PostMapping("remove-groups-from-teacher")
    public String removeGroupsFromTeacher(
            @RequestParam Integer teacherId,
            @RequestParam List<Integer> selectedGroupIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Current user roles: " + Arrays.toString(auth.getAuthorities().toArray()));

        teacherService.deleteGroupsFromTeacher(teacherId, selectedGroupIds); // Вызов сервиса
        return "redirect:/kubstuTest/manage-teachers"; // Перезагрузка страницы
    }
    @GetMapping("groups-in-teacher/{teacherId}")
    @ResponseBody
    public List<Group> getGroupsInTeacher(@PathVariable Integer teacherId) {
        return groupService.getGroupsByTecherID(teacherId);
    }
    @GetMapping("free-groups/{teacherId}")
    @ResponseBody
    public List<Group> getFreeGroups() {
        return groupService.findFreeGroups();
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