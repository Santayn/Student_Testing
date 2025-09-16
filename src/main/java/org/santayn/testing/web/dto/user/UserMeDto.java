// src/main/java/org/santayn/testing/web/dto/user/UserMeDto.java
package org.santayn.testing.web.dto.user;

public record UserMeDto(
        Integer id,
        String  login,
        String  firstName,
        String  lastName,
        String  phoneNumber,
        String  role,           // например: ROLE_STUDENT / ROLE_TEACHER / ROLE_ADMIN
        boolean student,
        boolean teacher,
        Integer groupId,        // если студент
        String  groupName       // если студент
) {}
