package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.role.RolePermission;
import org.santayn.testing.models.role.UserPermission;
import org.santayn.testing.models.role.UserRole;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.PermissionRepository;
import org.santayn.testing.repository.RolePermissionRepository;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.UserPermissionRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Transactional(readOnly = true)
    public List<Role> findRoles() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permission> findPermissions() {
        return permissionRepository.findAll();
    }

    @Transactional
    public Role createRole(String name, String description) {
        String roleName = FacultyService.requireText(name, "Name");
        if (roleRepository.existsByName(roleName)) {
            throw new AuthConflictException("Role already exists: " + roleName);
        }

        Role role = new Role();
        role.setName(roleName);
        role.setDescription(FacultyService.trimToNull(description));
        return roleRepository.save(role);
    }

    @Transactional
    public Permission createPermission(String name, String description) {
        String permissionName = FacultyService.requireText(name, "Name");
        if (permissionRepository.existsByName(permissionName)) {
            throw new AuthConflictException("Permission already exists: " + permissionName);
        }

        Permission permission = new Permission();
        permission.setName(permissionName);
        permission.setDescription(FacultyService.trimToNull(description));
        return permissionRepository.save(permission);
    }

    @Transactional
    public User setRoles(Integer userId, Set<Integer> roleIds) {
        requireUser(userId);
        Set<Integer> normalizedRoleIds = normalizeIds(roleIds);
        requireRoles(normalizedRoleIds);

        Set<Integer> existingRoleIds = userRoleRepository.findByIdUserId(userId)
                .stream()
                .map(userRole -> userRole.getId().getRoleId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Integer existingRoleId : existingRoleIds) {
            if (!normalizedRoleIds.contains(existingRoleId)) {
                userRoleRepository.delete(new UserRole(userId, existingRoleId));
            }
        }
        for (Integer roleId : normalizedRoleIds) {
            if (!existingRoleIds.contains(roleId)) {
                userRoleRepository.save(new UserRole(userId, roleId));
            }
        }

        return userRepository.findWithSecurityById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Transactional
    public Role setRolePermissions(Integer roleId, Set<Integer> permissionIds) {
        requireRole(roleId);
        Set<Integer> normalizedPermissionIds = normalizeIds(permissionIds);
        requirePermissions(normalizedPermissionIds);

        Set<Integer> existingPermissionIds = rolePermissionRepository.findByIdRoleId(roleId)
                .stream()
                .map(rolePermission -> rolePermission.getId().getPermissionId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Integer existingPermissionId : existingPermissionIds) {
            if (!normalizedPermissionIds.contains(existingPermissionId)) {
                rolePermissionRepository.delete(new RolePermission(roleId, existingPermissionId));
            }
        }
        for (Integer permissionId : normalizedPermissionIds) {
            if (!existingPermissionIds.contains(permissionId)) {
                rolePermissionRepository.save(new RolePermission(roleId, permissionId));
            }
        }

        return roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
    }

    @Transactional
    public User setUserPermissions(Integer userId, Set<Integer> permissionIds) {
        requireUser(userId);
        Set<Integer> normalizedPermissionIds = normalizeIds(permissionIds);
        requirePermissions(normalizedPermissionIds);

        Set<Integer> existingPermissionIds = userPermissionRepository.findByIdUserId(userId)
                .stream()
                .map(userPermission -> userPermission.getId().getPermissionId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Integer existingPermissionId : existingPermissionIds) {
            if (!normalizedPermissionIds.contains(existingPermissionId)) {
                userPermissionRepository.delete(new UserPermission(userId, existingPermissionId));
            }
        }
        for (Integer permissionId : normalizedPermissionIds) {
            if (!existingPermissionIds.contains(permissionId)) {
                userPermissionRepository.save(new UserPermission(userId, permissionId));
            }
        }

        return userRepository.findWithSecurityById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireUser(Integer userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
    }

    private void requireRole(Integer roleId) {
        if (roleId == null || !roleRepository.existsById(roleId)) {
            throw new IllegalArgumentException("Role not found: " + roleId);
        }
    }

    private void requireRoles(Set<Integer> roleIds) {
        List<Role> roles = roleRepository.findAllById(roleIds);
        Set<Integer> foundIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        if (foundIds.size() != roleIds.size()) {
            throw new IllegalArgumentException("One or more roles were not found.");
        }
    }

    private void requirePermissions(Set<Integer> permissionIds) {
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        Set<Integer> foundIds = permissions.stream().map(Permission::getId).collect(Collectors.toSet());
        if (foundIds.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions were not found.");
        }
    }

    private static Set<Integer> normalizeIds(Set<Integer> ids) {
        if (ids == null) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
