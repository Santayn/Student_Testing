package org.santayn.testing.security;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAccountInvariantIntegrationTests {

    @Autowired private EntityManager entityManager;
    @Autowired private MockMvc mockMvc;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void cannotDisableCurrentUserAccount() throws Exception {
        Role adminRole = role("ADMIN");
        User admin = account("admin-self-disable", true, adminRole);

        mockMvc.perform(put("/api/v1/users/{id}/active", admin.getId())
                        .with(user(admin.getLogin()).authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ADMIN"),
                                new SimpleGrantedAuthority("users.write")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody(false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(reload(admin).isActive()).isTrue();
    }

    @Test
    void cannotDisableLastActiveAdmin() throws Exception {
        Role adminRole = role("ADMIN");
        User admin = account("admin-last-active", true, adminRole);
        User operator = account("operator-disable-last", true);

        mockMvc.perform(put("/api/v1/users/{id}/active", admin.getId())
                        .with(user(operator.getLogin()).authorities(new SimpleGrantedAuthority("users.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody(false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(reload(admin).isActive()).isTrue();
    }

    @Test
    void canDisableAdminWhenAnotherActiveAdminRemains() throws Exception {
        Role adminRole = role("ADMIN");
        User actor = account("admin-disable-actor", true, adminRole);
        User target = account("admin-disable-target", true, adminRole);

        mockMvc.perform(put("/api/v1/users/{id}/active", target.getId())
                        .with(user(actor.getLogin()).authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ADMIN"),
                                new SimpleGrantedAuthority("users.write")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activeBody(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(reload(target).isActive()).isFalse();
    }

    @Test
    void cannotRemoveOwnAdminRoleEvenWhenAnotherAdminRemains() throws Exception {
        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        account("admin-self-demote-other", true, adminRole);
        User target = account("admin-self-demote-target", true, adminRole);

        mockMvc.perform(put("/api/v1/users/{id}/roles", target.getId())
                        .with(user(target.getLogin()).authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ADMIN"),
                                new SimpleGrantedAuthority("roles.manage")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleIdsBody(teacherRole)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(roleNames(reload(target))).contains("ADMIN");
    }

    @Test
    void cannotRemoveAdminRoleFromLastActiveAdmin() throws Exception {
        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        User admin = account("admin-last-role", true, adminRole);
        User operator = account("operator-demote-last", true);

        mockMvc.perform(put("/api/v1/users/{id}/roles", admin.getId())
                        .with(user(operator.getLogin()).authorities(new SimpleGrantedAuthority("roles.manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleIdsBody(teacherRole)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(roleNames(reload(admin))).contains("ADMIN");
    }

    @Test
    void canRemoveAdminRoleWhenAnotherActiveAdminRemains() throws Exception {
        Role adminRole = role("ADMIN");
        Role teacherRole = role("TEACHER");
        User actor = account("admin-demote-actor", true, adminRole);
        User target = account("admin-demote-target", true, adminRole);

        mockMvc.perform(put("/api/v1/users/{id}/roles", target.getId())
                        .with(user(actor.getLogin()).authorities(
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("ADMIN"),
                                new SimpleGrantedAuthority("roles.manage")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roleIdsBody(teacherRole)))
                .andExpect(status().isOk());

        assertThat(roleNames(reload(target))).containsExactly("TEACHER");
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(name.toLowerCase(Locale.ROOT));
        return roleRepository.saveAndFlush(role);
    }

    private User account(String login, boolean active, Role... roles) {
        User user = new User();
        user.setLogin(login);
        user.setPasswordHash("test-password-hash");
        user.setActive(active);
        user.getRoles().addAll(Arrays.asList(roles));
        return userRepository.saveAndFlush(user);
    }

    private User reload(User user) {
        entityManager.flush();
        entityManager.clear();
        return userRepository.findWithSecurityById(user.getId()).orElseThrow();
    }

    private static String activeBody(boolean active) {
        return "{\"active\":" + active + "}";
    }

    private static String roleIdsBody(Role... roles) {
        String ids = Arrays.stream(roles)
                .map(Role::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return "{\"roleIds\":[" + ids + "]}";
    }

    private static java.util.List<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }
}
