// src/main/java/org/santayn/testing/web/dto/user/UserRoleDto.java
package org.santayn.testing.web.dto.user;

import org.santayn.testing.models.user.User;

public record UserRoleDto(Integer id, String login, Integer roleId, String roleName) {
    public static UserRoleDto from(User u){
        var r = u.getRole();
        return new UserRoleDto(u.getId(), u.getLogin(), r != null ? r.getId() : null, r != null ? r.getName() : null);
    }
}
