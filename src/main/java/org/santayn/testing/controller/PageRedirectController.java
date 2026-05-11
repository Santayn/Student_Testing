package org.santayn.testing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@CrossOrigin
public class PageRedirectController {

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

    @GetMapping("/index.html")
    public String index() {
        return "redirect:/main.html";
    }

    @GetMapping({"/teachers.html", "/kubstuTest/manage-teachers"})
    public String manageTeachers() {
        return "redirect:/manage-teachers.html";
    }

    @GetMapping({"/roles.html", "/users-role.html", "/kubstuTest/users/role/user"})
    public String users() {
        return "redirect:/users.html";
    }

    @GetMapping({"/teachers-subject.html", "/teacher-subject.html", "/kubstuTest/manage-teachers-subject"})
    public String manageTeacherSubjects() {
        return "redirect:/manage-teachers-subject.html";
    }

    @GetMapping({"/teacher-group.html", "/kubstuTest/manage-teachers-groups"})
    public String manageTeacherGroups() {
        return "redirect:/manage-teacher-groups.html";
    }

    @GetMapping({"/tests-create.html", "/kubstuTest/tests/create-test"})
    public String createTest() {
        return "redirect:/create-test.html";
    }

    @GetMapping({"/topics.html", "/kubstuTest/manage-topics-subject"})
    public String manageTopics() {
        return "redirect:/manage-teachers.html";
    }

    @GetMapping({"/topic-library.html", "/kubstuTest/manage-topic-library"})
    public String manageTopicLibrary() {
        return "redirect:/manage-topic-library.html";
    }

    @GetMapping({"/questions-upload.html", "/kubstuTest/questions"})
    public String questions() {
        return "redirect:/questions-page.html";
    }

    @GetMapping({"/faculties.html", "/kubstuTest/faculties"})
    public String faculties() {
        return "redirect:/create-faculty.html";
    }

    @GetMapping({"/subject-faculty.html", "/kubstuTest/manage-subject-faculty"})
    public String manageSubjectFaculty() {
        return "redirect:/manage-subject-faculty.html";
    }

    @GetMapping("/kubstuTest/manage-students")
    public String manageStudents() {
        return "redirect:/manage_students.html";
    }

    @GetMapping("/group-student.html")
    public String groupStudents() {
        return "redirect:/manage_students.html";
    }

    @GetMapping("/kubstuTest/manage-lectures-subject")
    public String manageLectures() {
        return "redirect:/manage-lectures-subject.html";
    }

    @GetMapping({"/results.html", "/kubstuTest/results"})
    public String results() {
        return "redirect:/result-list.html";
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
