package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.models.faculty.Faculty;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.service.FacultyService;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.web.dto.group.GroupCreateRequest;
import org.santayn.testing.web.dto.group.GroupResponse;
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

    @GetMapping
    public List<GroupResponse> list() {
        return groupService.getAllGroup().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public GroupResponse get(@PathVariable Integer id) {
        Group g = groupService.getGroupById(id);
        return toResponse(g);
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupCreateRequest req) {
        Group g = new Group();
        g.setName(req.name());
        g.setTitle(req.title());
        g.setCourse_code(req.courseCode());

        if (req.facultyId() != null) {
            Faculty f = facultyService.findById(req.facultyId());
            g.setFaculty(f);
        }

        // service.save(...) ничего не возвращает
        groupService.save(g);

        // после save у JPA обычно уже выставлен g.getId()
        Integer id = g.getId();
        if (id == null) {
            // на всякий случай, если провайдер не проставил id — достаём по name
            g = groupService.getSpecificGroupByGroupName(req.name());
            id = g.getId();
        }

        return ResponseEntity
                .created(URI.create("/api/v1/groups/" + id))
                .body(toResponse(g));
    }

    @PatchMapping("/{id}")
    public GroupResponse update(@PathVariable Integer id, @RequestBody GroupUpdateRequest req) {
        Group g = groupService.getGroupById(id);

        if (req.name() != null) g.setName(req.name());
        if (req.title() != null) g.setTitle(req.title());
        if (req.courseCode() != null) g.setCourse_code(req.courseCode());
        if (req.facultyId() != null) {
            Faculty f = facultyService.findById(req.facultyId());
            g.setFaculty(f);
        }

        // сохраняем изменения, метод void
        groupService.save(g);

        // возвращаем уже обновлённый объект (g имеет актуальные поля)
        return toResponse(g);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        groupService.deleteGroupById(id); // добавь этот метод, если ещё нет
        return ResponseEntity.noContent().build();
    }

    private GroupResponse toResponse(Group g) {
        Integer facultyId = g.getFaculty() != null ? g.getFaculty().getId() : null;
        return new GroupResponse(g.getId(), g.getName(), g.getTitle(), g.getCourse_code(), facultyId);
    }
}
