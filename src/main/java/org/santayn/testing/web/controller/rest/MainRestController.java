// src/main/java/org/santayn/testing/web/controller/rest/MainRestController.java
package org.santayn.testing.web.controller.rest;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.SubjectService;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.subject.SubjectShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MainRestController {

    private final UserService userService;
    private final SubjectService subjectService;

    /** Инфа о факультете/группе текущего студента */
    @GetMapping("/faculty")
    public FacultyInfo myFaculty() {
        User u = currentUser();
        Group g = ensureStudentGroup(u);
        Faculty f = ensureFaculty(g);
        return new FacultyInfo(
                f.getId(), ns(f.getName()),
                g.getId(), ns(g.getName()), ns(g.getTitle())
        );
    }

    /** Предметы по факультету текущего студента */
    @GetMapping("/subjects")
    public List<SubjectShortDto> mySubjects() {
        User u = currentUser();
        Group g = ensureStudentGroup(u);
        Faculty f = ensureFaculty(g);
        return subjectService.getSubjectsByFacultyId(f.getId())
                .stream().map(SubjectShortDto::from).toList();
    }

    /* ===== helpers ===== */

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null ? auth.getName() : null);
        if (username == null) throw notFound("Пользователь не авторизован");
        User u = userService.getUserByUsername(username);
        if (u == null) throw notFound("Пользователь не найден: " + username);
        return u;
    }

    private Group ensureStudentGroup(User u) {
        if (u.getStudent() == null || u.getStudent().getGroup() == null) {
            throw notFound("У пользователя нет привязанной группы.");
        }
        return u.getStudent().getGroup();
    }

    private Faculty ensureFaculty(Group g) {
        if (g.getFaculty() == null || g.getFaculty().getId() == null) {
            throw notFound("У группы нет привязанного факультета.");
        }
        return g.getFaculty();
    }

    private static String ns(String s) { return s == null ? "" : s; }
    private ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }

    public record FacultyInfo(
            Integer facultyId, String facultyName,
            Integer groupId, String groupName, String groupTitle
    ) {}
}
