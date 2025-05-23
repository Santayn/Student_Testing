package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/kubstuTest/")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRegisterService userRegisterService;
    private final StudentService studentService;
    private final GroupService groupService;
    private final TeacherService teacherService;
    private final UserService userService;
    private final RoleService roleService;

    // === Профиль текущего пользователя ===
    @GetMapping("profile/me")
    public String getUserProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentLogin = auth.getName(); // Получаем логин текущего пользователя
        User selectUser = userRegisterService.findUserByLogin(currentLogin);
        model.addAttribute("selectUser", selectUser);
        return "profile"; // Имя Thymeleaf шаблона
    }
    // === Страница пользователей по умолчанию (USER) ===
    @GetMapping("/users/role/user")
    public String showUsersWithUserRole(Model model) {
        List<User> users = userService.findUsers(1); // USER
        model.addAttribute("users", users);
        model.addAttribute("selectedRoleId", 1);
        return "users";
    }

    // === Универсальный метод: показать пользователей по roleId ===
    @GetMapping("/users/role")
    public String showUsersByRole(@RequestParam(name = "roleId", required = false) Integer roleId, Model model) {
        if (roleId == null) {
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("selectedRoleId", null);
        } else {
            List<User> users = userService.findUsers(roleId);
            model.addAttribute("users", users);
            model.addAttribute("selectedRoleId", roleId);
        }
        return "users"; // Thymeleaf шаблон
    }

    // === Обновление роли пользователя ===
    @PostMapping("/users/update-role")
    public String updateUserRole(
            @RequestParam("userId") Integer userId,
            @RequestParam("newRoleId") Integer newRoleId) {

        User user = userService.getUserById(userId);
        Role currentRole = user.getRole();
        Role newRole = roleService.getRoleById(newRoleId);

        // Удаляем данные предыдущей роли
        if (currentRole != null) {
            if ("STUDENT".equals(currentRole.getName()) && !"STUDENT".equals(newRole.getName())) {
                Optional<Student> studentOpt = studentService.findByUserId(userId);
                if (studentOpt.isPresent()) {
                    Student student = studentOpt.get();
                    studentService.deleteStudentAndRelatedData(student.getId(), userId);
                }
            }

            if (("TEACHER".equals(currentRole.getName()) && !"TEACHER".equals(newRole.getName())) ||("ADMIN".equals(currentRole.getName()) && !"ADMIN".equals(newRole.getName())) ) {
                Optional<Teacher> teacherOpt = teacherService.findByUserId(userId);
                if (teacherOpt.isPresent()) {
                    Teacher teacher = teacherOpt.get();
                    teacherService.deleteTeacherAndRelatedData(teacher.getId(), userId);
                }
            }
        }

        // Присваиваем новую роль
            user.setRole(newRole);
            userService.save(user);

            // Создаём новую запись в зависимости от роли
            if ("STUDENT".equals(newRole.getName())) {
                studentService.createStudentForUser(user);
            } else if ("TEACHER".equals(newRole.getName()) || "ADMIN".equals(newRole.getName())) {
                teacherService.createTeacherForUser(user);
            }

        return "redirect:/kubstuTest/users/role?roleId=" + newRoleId;
    }

}