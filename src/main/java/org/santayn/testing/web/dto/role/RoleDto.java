// src/main/java/org/santayn/testing/web/dto/role/RoleDto.java
package org.santayn.testing.web.dto.role;

import org.santayn.testing.models.role.Role;

public record RoleDto(Integer id, String name) {
    public static RoleDto from(Role r){ return new RoleDto(r.getId(), r.getName()); }
}
