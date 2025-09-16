// src/main/java/org/santayn/testing/web/controller/rest/UserProfileRestController.java
package org.santayn.testing.web.controller.rest;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.user.UserMeDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileRestController {

    private final UserService userService;

    @GetMapping("/me")
    public UserMeDto me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Не авторизовано");
        }
        final String login = auth.getName();
        User u = userService.getUserByUsername(login);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден");
        }

        String roleName = (u.getRole() != null) ? u.getRole().getName() : null;
        boolean isTeacher = (u.getTeacher() != null);
        Student stu = u.getStudent();
        Group g = (stu != null) ? stu.getGroup() : null;

        return new UserMeDto(
                u.getId(),
                u.getLogin(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhoneNumber(),
                roleName,
                stu != null,
                isTeacher,
                (g != null ? g.getId() : null),
                (g != null ? g.getName() : null)
        );
    }
}
