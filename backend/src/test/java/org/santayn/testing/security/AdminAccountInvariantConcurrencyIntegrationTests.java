package org.santayn.testing.security;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.service.AuthConflictException;
import org.santayn.testing.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_invariant_concurrency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS CITEXT AS VARCHAR"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminAccountInvariantConcurrencyIntegrationTests {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;

    @Test
    void parallelDisablesCannotRemoveAllActiveAdmins() throws Exception {
        Role adminRole = role("ADMIN");
        User firstAdmin = account("admin-disable-parallel-one", true, adminRole);
        User secondAdmin = account("admin-disable-parallel-two", true, adminRole);
        User operator = account("operator-disable-parallel", true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> first = executor.submit(disableWhenReleased(firstAdmin.getId(), operator.getLogin(), ready, start));
            Future<Object> second = executor.submit(disableWhenReleased(secondAdmin.getId(), operator.getLogin(), ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Object> outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));

            assertThat(outcomes.stream().filter(User.class::isInstance)).hasSize(1);
            assertThat(outcomes.stream().filter(AuthConflictException.class::isInstance)).hasSize(1);
            assertThat(activeAdminCount()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Object> disableWhenReleased(Integer userId,
                                                 String actorLogin,
                                                 CountDownLatch ready,
                                                 CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                return userService.setActive(userId, false, actorLogin);
            } catch (RuntimeException ex) {
                return ex;
            }
        };
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

    private long activeAdminCount() {
        return userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .filter(AdminAccountInvariantConcurrencyIntegrationTests::hasAdminRole)
                .count();
    }

    private static boolean hasAdminRole(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(roleName -> roleName.trim().toUpperCase(Locale.ROOT))
                .anyMatch("ADMIN"::equals);
    }
}
