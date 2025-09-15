package org.santayn.testing.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.*;
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
    private final UserRoleService userRoleService;

    // === Профиль текущего пользователя ===
    @GetMapping("profile/me")
    public String getUserProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            return "redirect:/login"; // <-- больше не кидаем 500 при anonymous
        }
        String currentLogin = auth.getName();
        User selectUser = userRegisterService.findUserByLogin(currentLogin);
        model.addAttribute("selectUser", selectUser);
        return "profile";
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
        return "users";
    }

    // === Обновление роли пользователя ===
    @PostMapping("/users/update-role")
    public String updateUserRole(@RequestParam("userId") Integer userId,
                                 @RequestParam("newRoleId") Integer newRoleId) {

        User user = userService.getUserById(userId);
        Role currentRole = user.getRole();
        Role newRole = userRoleService.getRoleById(newRoleId);

        if (currentRole != null) {
            if ("STUDENT".equals(currentRole.getName()) && !"STUDENT".equals(newRole.getName())) {
                Optional<Student> studentOpt = studentService.findByUserId(userId);
                studentOpt.ifPresent(student -> studentService.deleteStudentAndRelatedData(student.getId(), userId));
            }

            if (("TEACHER".equals(currentRole.getName()) && !"TEACHER".equals(newRole.getName()))
                    || ("ADMIN".equals(currentRole.getName()) && !"ADMIN".equals(newRole.getName()))) {
                Optional<Teacher> teacherOpt = teacherService.findByUserId(userId);
                teacherOpt.ifPresent(teacher -> teacherService.deleteTeacherAndRelatedData(teacher.getId(), userId));
            }
        }

        user.setRole(newRole);
        userService.save(user);

        if ("STUDENT".equals(newRole.getName())) {
            studentService.createStudentForUser(user);
        } else if ("TEACHER".equals(newRole.getName()) || "ADMIN".equals(newRole.getName())) {
            teacherService.createTeacherForUser(user);
        }

        return "redirect:/kubstuTest/users/role?roleId=" + newRoleId;
    }
}
