// src/main/java/org/santayn/testing/web/dto/user/UserRoleUpdateRequest.java
package org.santayn.testing.web.dto.user;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(@NotNull Integer roleId) {}
