package org.santayn.testing.web.controller.rest;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.service.TeacherService;
import org.santayn.testing.service.UserSearch;
import org.santayn.testing.web.dto.group.GroupShortDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher-groups")
public class TeacherGroupRestController {

    private final GroupService groupService;
    private final TeacherService teacherService;
    private final UserSearch userSearch;

    public TeacherGroupRestController(GroupService groupService,
                                      TeacherService teacherService,
                                      UserSearch userSearch) {
        this.groupService = groupService;
        this.teacherService = teacherService;
        this.userSearch = userSearch;
    }

    /** Группы текущего (авторизованного) преподавателя */
    @GetMapping("/me")
    public List<GroupShortDto> myGroups() {
        Teacher current = userSearch.getCurrentTeacher();
        List<Group> groups = teacherService.getGroupsByTeacher(current);
        return groups.stream()
                .map(GroupShortDto::from)
                .toList();
    }

    /** Группы конкретного преподавателя */
    @GetMapping("/{teacherId}")
    public List<GroupShortDto> groupsByTeacher(@PathVariable Integer teacherId) {
        return teacherService.getGroupsByTeacherID(teacherId).stream()
                .map(GroupShortDto::from)
                .toList();
    }

    /** Свободные группы (не привязанные ни к одному преподавателю) */
    @GetMapping("/free")
    public List<GroupShortDto> freeGroups() {
        return groupService.findFreeGroups().stream()
                .map(GroupShortDto::from)
                .toList();
    }

    /** Привязать группы к преподавателю (тело — массив id групп) */
    @PostMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addGroups(@PathVariable Integer teacherId,
                          @RequestBody List<Integer> groupIds) {
        teacherService.addGroupsToTeacher(teacherId, groupIds);
    }

    /** Отвязать группы от преподавателя (тело — массив id групп) */
    @DeleteMapping("/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeGroups(@PathVariable Integer teacherId,
                             @RequestBody List<Integer> groupIds) {
        teacherService.deleteGroupsFromTeacher(teacherId, groupIds);
    }
}
