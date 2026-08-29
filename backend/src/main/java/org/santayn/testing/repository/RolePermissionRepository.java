package org.santayn.testing.repository;

import org.santayn.testing.models.role.RolePermission;
import org.santayn.testing.models.role.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByIdRoleId(Integer roleId);

    List<RolePermission> findByIdPermissionId(Integer permissionId);

    boolean existsByIdRoleIdAndIdPermissionId(Integer roleId, Integer permissionId);
}
