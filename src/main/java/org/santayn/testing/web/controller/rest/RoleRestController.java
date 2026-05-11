package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.UserRoleService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@CrossOrigin
@RequestMapping({"/api/roles", "/api/v1/roles"})
public class RoleRestController {

    private final UserRoleService userRoleService;

    public RoleRestController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @GetMapping
    public List<ApiResponses.RoleResponse> all() {
        return ApiResponses.list(userRoleService.findRoles(), ApiResponses::role);
    }

    @PostMapping
    public ApiResponses.RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return ApiResponses.role(userRoleService.createRole(request.name(), request.description()));
    }

    @GetMapping("/permissions")
    public List<ApiResponses.PermissionResponse> permissions() {
        return ApiResponses.list(userRoleService.findPermissions(), ApiResponses::permission);
    }

    @PostMapping("/permissions")
    public ApiResponses.PermissionResponse createPermission(@Valid @RequestBody PermissionRequest request) {
        return ApiResponses.permission(userRoleService.createPermission(request.name(), request.description()));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponses.RoleResponse setRolePermissions(@PathVariable Integer id, @Valid @RequestBody RolePermissionsRequest request) {
        return ApiResponses.role(userRoleService.setRolePermissions(id, request.permissionIds()));
    }

    public record RoleRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description
    ) {
    }

    public record PermissionRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description
    ) {
    }

    public record RolePermissionsRequest(Set<Integer> permissionIds) {
    }
}
