package org.santayn.testing.repository;

import org.santayn.testing.models.role.UserPermission;
import org.santayn.testing.models.role.UserPermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, UserPermissionId> {

    List<UserPermission> findByIdUserId(Integer userId);

    List<UserPermission> findByIdPermissionId(Integer permissionId);

    boolean existsByIdUserIdAndIdPermissionId(Integer userId, Integer permissionId);
}
