// src/main/java/org/santayn/testing/web/controller/rest/GroupStudentRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.student.Student;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.StudentService;
import org.santayn.testing.web.dto.student.StudentShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-students")
public class GroupStudentRestController {

    private final GroupService groupService;
    private final StudentService studentService;

    public GroupStudentRestController(GroupService groupService, StudentService studentService) {
        this.groupService = groupService;
        this.studentService = studentService;
    }

    /** Список студентов в группе */
    @GetMapping("/{groupId}")
    public List<StudentShortDto> studentsInGroup(@PathVariable Integer groupId) {
        List<Student> students = studentService.getStudentsByGroupID(groupId);
        return students.stream().map(StudentShortDto::from).toList();
    }

    /** Свободные студенты (не состоят ни в одной группе) */
    @GetMapping("/free")
    public List<StudentShortDto> freeStudents() {
        return studentService.findFreeStudents().stream()
                .map(StudentShortDto::from)
                .toList();
    }

    /** Добавить студентов в группу (тело — массив id студентов) */
    @PostMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addStudents(@PathVariable Integer groupId,
                            @RequestBody List<Integer> studentIds) {
        groupService.addStudentsToGroup(groupId, studentIds);
    }

    /** Удалить студентов из группы (тело — массив id студентов) */
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudents(@PathVariable Integer groupId,
                               @RequestBody List<Integer> studentIds) {
        groupService.deleteStudentsFromGroup(groupId, studentIds);
    }
}
