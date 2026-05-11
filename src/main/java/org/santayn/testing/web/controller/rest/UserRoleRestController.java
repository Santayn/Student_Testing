package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.service.UserRoleService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@CrossOrigin
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserRoleRestController {

    private final UserRoleService userRoleService;

    public UserRoleRestController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PutMapping("/{id}/roles")
    public ApiResponses.UserResponse setRoles(@PathVariable Integer id, @Valid @RequestBody UserRolesRequest request) {
        return ApiResponses.user(userRoleService.setRoles(id, request.roleIds()));
    }

    @PutMapping("/{id}/permissions")
    public ApiResponses.UserResponse setPermissions(@PathVariable Integer id, @Valid @RequestBody UserPermissionsRequest request) {
        return ApiResponses.user(userRoleService.setUserPermissions(id, request.permissionIds()));
    }

    public record UserRolesRequest(Set<Integer> roleIds) {
    }

    public record UserPermissionsRequest(Set<Integer> permissionIds) {
    }
}
