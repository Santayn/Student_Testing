// src/main/java/org/santayn/testing/service/UserRoleService.java
package org.santayn.testing.service;

import jakarta.transaction.Transactional;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserRoleService {

    private final UserService userService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final RoleRepository roleRepository; // ← работаем напрямую с репозиторием

    public UserRoleService(UserService userService,
                           StudentService studentService,
                           TeacherService teacherService,
                           RoleRepository roleRepository) {
        this.userService = userService;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.roleRepository = roleRepository;
    }

    // === публичные методы, которых не хватало ===

    /** Вернуть роль по ID или бросить ошибку. */
    public Role getRoleById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль не найдена: id=" + id));
    }

    /** Вернуть все роли. */
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    // === смена роли пользователя (вся логика в одном месте) ===
    @Transactional
    public User changeRole(Integer userId, Integer newRoleId) {
        User user = userService.getUserById(userId);
        Role currentRole = user.getRole();
        Role newRole = getRoleById(newRoleId);

        if (currentRole != null && currentRole.getId().equals(newRole.getId())) {
            return user; // роль не меняется
        }

        // сносим сущности предыдущей роли, если нужно
        if (currentRole != null) {
            if ("STUDENT".equals(currentRole.getName()) && !"STUDENT".equals(newRole.getName())) {
                Optional<Student> stOpt = studentService.findByUserId(userId);
                stOpt.ifPresent(st -> studentService.deleteStudentAndRelatedData(st.getId(), userId));
            }
            if (("TEACHER".equals(currentRole.getName()) && !"TEACHER".equals(newRole.getName())) ||
                    ("ADMIN".equals(currentRole.getName())   && !"ADMIN".equals(newRole.getName()))) {
                Optional<Teacher> tOpt = teacherService.findByUserId(userId);
                tOpt.ifPresent(t -> teacherService.deleteTeacherAndRelatedData(t.getId(), userId));
            }
        }

        // назначаем новую роль
        user.setRole(newRole);
        userService.save(user);

        // создаём данные под новую роль
        if ("STUDENT".equals(newRole.getName())) {
            studentService.createStudentForUser(user);
        } else if ("TEACHER".equals(newRole.getName()) || "ADMIN".equals(newRole.getName())) {
            teacherService.createTeacherForUser(user);
        }

        return user;
    }
}
