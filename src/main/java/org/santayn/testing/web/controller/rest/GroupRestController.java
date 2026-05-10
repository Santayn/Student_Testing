package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.GroupService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/groups", "/api/v1/groups"})
public class GroupRestController {

    private final GroupService groupService;

    public GroupRestController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<ApiResponses.GroupResponse> all(@RequestParam(required = false) Integer facultyId) {
        return ApiResponses.list(groupService.findAll(facultyId), ApiResponses::group);
    }

    @GetMapping("/{id}")
    public ApiResponses.GroupResponse one(@PathVariable Integer id) {
        return ApiResponses.group(groupService.get(id));
    }

    @PostMapping
    public ApiResponses.GroupResponse create(@Valid @RequestBody GroupRequest request) {
        return ApiResponses.group(groupService.create(request.name(), request.code(), request.facultyId()));
    }

    @PutMapping("/{id}")
    public ApiResponses.GroupResponse update(@PathVariable Integer id, @Valid @RequestBody GroupRequest request) {
        return ApiResponses.group(groupService.update(id, request.name(), request.code(), request.facultyId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        groupService.delete(id);
    }

    public record GroupRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 50) String code,
            @NotNull Integer facultyId
    ) {
    }
}
