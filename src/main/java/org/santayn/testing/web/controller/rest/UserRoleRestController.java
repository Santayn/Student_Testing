// src/main/java/org/santayn/testing/web/controller/rest/UserRoleRestController.java
package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.models.user.User;
import org.santayn.testing.service.UserRoleService;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.user.UserRoleDto;
import org.santayn.testing.web.dto.user.UserRoleUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserRoleRestController {

    private final UserService userService;
    private final UserRoleService userRoleService;

    public UserRoleRestController(UserService userService, UserRoleService userRoleService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
    }

    /** Пользователи по роли (как было в MVC /users/role?roleId=...) */
    @GetMapping("/by-role")
    public List<UserRoleDto> usersByRole(@RequestParam Integer roleId){
        return userService.findUsers(roleId).stream().map(UserRoleDto::from).toList();
    }

    /** Один пользователь */
    @GetMapping("/{userId}")
    public UserRoleDto get(@PathVariable Integer userId){
        return UserRoleDto.from(userService.getUserById(userId));
    }

    /** Смена роли пользователя */
    @PatchMapping("/{userId}/role")
    @ResponseStatus(HttpStatus.OK)
    public UserRoleDto changeRole(@PathVariable Integer userId,
                                  @Valid @RequestBody UserRoleUpdateRequest req){
        User updated = userRoleService.changeRole(userId, req.roleId());
        return UserRoleDto.from(updated);
    }
}
