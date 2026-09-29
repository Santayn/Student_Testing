package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAccountInvariantService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireCanSetActive(Integer targetUserId, boolean active, String actorLogin) {
        if (active) {
            return;
        }

        User actor = requireActor(actorLogin);
        if (Objects.equals(actor.getId(), targetUserId)) {
            throw new AuthConflictException("The current user account cannot disable itself.");
        }

        lockAdminRole();
        List<User> activeAdmins = userRepository.findActiveAdminsForUpdate();
        User target = requireTarget(targetUserId);
        if (isActiveAdmin(target) && activeAdmins.size() <= 1) {
            throw new AuthConflictException("At least one active ADMIN account must remain.");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireCanSetRoles(Integer targetUserId, Set<Integer> requestedRoleIds, String actorLogin) {
        Integer adminRoleId = roleRepository.findByNameForUpdate(ADMIN_ROLE)
                .map(Role::getId)
                .orElse(null);
        if (adminRoleId == null || requestedRoleIds.contains(adminRoleId)) {
            return;
        }

        List<User> activeAdmins = userRepository.findActiveAdminsForUpdate();
        User target = requireTarget(targetUserId);
        if (!isActiveAdmin(target)) {
            return;
        }

        User actor = requireActor(actorLogin);
        if (Objects.equals(actor.getId(), targetUserId)) {
            throw new AuthConflictException("The current user account cannot remove its own ADMIN role.");
        }
        if (activeAdmins.size() <= 1) {
            throw new AuthConflictException("At least one active ADMIN account must remain.");
        }
    }

    private User requireActor(String actorLogin) {
        return userRepository.findWithSecurityByLogin(normalizeLogin(actorLogin))
                .orElseThrow(() -> new IllegalArgumentException("Current user not found."));
    }

    private User requireTarget(Integer targetUserId) {
        return userRepository.findWithSecurityById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));
    }

    private void lockAdminRole() {
        roleRepository.findByNameForUpdate(ADMIN_ROLE);
    }

    private static boolean isActiveAdmin(User user) {
        return user.isActive() && user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(roleName -> roleName.trim().toUpperCase(Locale.ROOT))
                .anyMatch(ADMIN_ROLE::equals);
    }

    private static String normalizeLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Current user login is required.");
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }
}
