package org.santayn.testing.repository;

import org.santayn.testing.models.role.UserRole;
import org.santayn.testing.models.role.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdUserId(Integer userId);

    List<UserRole> findByIdRoleId(Integer roleId);

    boolean existsByIdUserIdAndIdRoleId(Integer userId, Integer roleId);
}
