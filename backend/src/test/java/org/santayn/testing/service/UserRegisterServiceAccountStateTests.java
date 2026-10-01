package org.santayn.testing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RefreshTokenRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.security.DotNetPasswordHasher;
import org.santayn.testing.security.JwtService;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRegisterServiceAccountStateTests {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private DotNetPasswordHasher passwordHasher;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;

    private UserRegisterService service;

    @BeforeEach
    void setUp() {
        service = new UserRegisterService(
                userRepository,
                refreshTokenRepository,
                passwordHasher,
                jwtService,
                authenticationRateLimitService,
                true
        );
    }

    @Test
    void exposesPendingPersonAndPublicRegistrationFromBackend() {
        User user = activeUser("new-user");
        when(userRepository.findWithSecurityByLogin("new-user")).thenReturn(Optional.of(user));

        UserRegisterService.CurrentUser currentUser = service.currentUser("new-user");

        assertThat(currentUser.accountState()).isEqualTo(UserRegisterService.AccountState.PENDING_PERSON);
        assertThat(currentUser.publicRegistrationEnabled()).isTrue();
        assertThat(service.publicAuthConfiguration().publicRegistrationEnabled()).isTrue();
    }

    @Test
    void exposesPendingRoleWhenPersonExistsWithoutAccountRole() {
        User user = activeUser("person-only");
        Person person = new Person();
        person.setId(15);
        person.setFirstName("Test");
        person.setLastName("Person");
        person.setEmail("person@example.test");
        person.setPhone("");
        user.setPersonId(15);
        user.setPerson(person);
        when(userRepository.findWithSecurityByLogin("person-only")).thenReturn(Optional.of(user));

        assertThat(service.currentUser("person-only").accountState())
                .isEqualTo(UserRegisterService.AccountState.PENDING_ROLE);
    }

    @Test
    void loginResponseContainsCurrentIdentityWithoutFollowUpMeRequest() {
        User user = activeUser("student");
        user.setPasswordHash("hash");
        Person person = new Person();
        person.setId(21);
        person.setFirstName("Student");
        person.setLastName("One");
        person.setEmail("student@example.test");
        person.setPhone("");
        user.setPersonId(21);
        user.setPerson(person);
        Role student = new Role();
        student.setName("STUDENT");
        user.getRoles().add(student);

        when(userRepository.findWithSecurityByLogin("student")).thenReturn(Optional.of(user));
        when(passwordHasher.verify("hash", "password"))
                .thenReturn(new DotNetPasswordHasher.VerificationResult(true, false));
        when(jwtService.normalizeLifetimeKind(any())).thenReturn(1);
        when(jwtService.generateTokens(any(User.class), anyInt())).thenReturn(new JwtService.GeneratedTokens(
                "access-token",
                Instant.now().plusSeconds(900),
                "refresh-token",
                "refresh-hash",
                Instant.now().plusSeconds(3600),
                1
        ));

        UserRegisterService.AuthTokens tokens = service.login(
                "student",
                "password",
                null,
                "127.0.0.1",
                "test"
        );

        assertThat(tokens.user()).isNotNull();
        assertThat(tokens.user().login()).isEqualTo("student");
        assertThat(tokens.user().accountState()).isEqualTo(UserRegisterService.AccountState.ACTIVE);
        assertThat(tokens.user().roles()).contains("STUDENT");
    }

    private static User activeUser(String login) {
        User user = new User();
        user.setId(1);
        user.setLogin(login);
        user.setActive(true);
        return user;
    }
}
