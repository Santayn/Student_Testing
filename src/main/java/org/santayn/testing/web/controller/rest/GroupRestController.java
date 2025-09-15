// src/main/java/org/santayn/testing/web/controller/rest/GroupRestController.java
package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.web.dto.group.GroupCreateRequest;
import org.santayn.testing.web.dto.group.GroupResponse;
import org.santayn.testing.web.dto.group.GroupShortDto;
import org.santayn.testing.web.dto.group.GroupUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupRestController {

    private final GroupService groupService;
    private final FacultyService facultyService;

    public GroupRestController(GroupService groupService, FacultyService facultyService) {
        this.groupService = groupService;
        this.facultyService = facultyService;
    }

    /** Полный список групп */
    @GetMapping
    public List<GroupResponse> list() {
        return groupService.getAllGroup()
                .stream()
                .map(GroupResponse::from)
                .toList();
    }

    /** Короткий список (для селектов) */
    @GetMapping("/short")
    public List<GroupShortDto> listShort() {
        return groupService.getAllGroup()
                .stream()
                .map(GroupShortDto::from)
                .toList();
    }

    /** Получить одну группу */
    @GetMapping("/{id}")
    public GroupResponse get(@PathVariable Integer id) {
        Group g = groupService.getGroupById(id);
        return GroupResponse.from(g);
    }

    /** Создать группу (courseCode игнорируем по твоему требованию) */
    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupCreateRequest req) {
        Group g = new Group();
        g.setName(req.name());
        g.setTitle(req.title());
        // req.courseCode() — не используем

        if (req.facultyId() != null) {
            Faculty f = facultyService.findById(req.facultyId());
            g.setFaculty(f);
        }

        groupService.save(g);

        Integer id = g.getId();
        if (id == null) {
            // fallback, если провайдер не проставил id сразу
            g = groupService.getSpecificGroupByGroupName(req.name());
            id = g.getId();
        }

        return ResponseEntity
                .created(URI.create("/api/v1/groups/" + id))
                .body(GroupResponse.from(g));
    }

    /** Частичное обновление (courseCode игнорируем) */
    @PatchMapping("/{id}")
    public GroupResponse update(@PathVariable Integer id, @RequestBody GroupUpdateRequest req) {
        Group g = groupService.getGroupById(id);

        if (req.name() != null)  g.setName(req.name());
        if (req.title() != null) g.setTitle(req.title());
        // req.courseCode() — не используем
        if (req.facultyId() != null) {
            Faculty f = facultyService.findById(req.facultyId());
            g.setFaculty(f);
        }

        groupService.save(g);
        return GroupResponse.from(g);
    }

    /** Удалить группу */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        groupService.deleteGroupById(id);
        return ResponseEntity.noContent().build();
    }
}
