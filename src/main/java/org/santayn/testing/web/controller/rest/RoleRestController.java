// src/main/java/org/santayn/testing/web/controller/rest/RoleRestController.java
package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.UserRoleService;
import org.santayn.testing.web.dto.role.RoleDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleRestController {
    private final UserRoleService userRoleService;
    public RoleRestController(UserRoleService userRoleService){ this.userRoleService = userRoleService; }

    @GetMapping
    public List<RoleDto> list() {
        return userRoleService.findAll().stream().map(RoleDto::from).toList();
    }
}
