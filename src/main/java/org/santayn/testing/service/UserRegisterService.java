package org.santayn.testing.service;

import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.RefreshToken;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.RefreshTokenRepository;
import org.santayn.testing.repository.UserRepository;
import org.santayn.testing.security.DotNetPasswordHasher;
import org.santayn.testing.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
public class UserRegisterService implements UserDetailsService {

    private static final int LOGIN_MAX_LENGTH = 100;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 200;

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DotNetPasswordHasher passwordHasher;
    private final JwtService jwtService;

    public UserRegisterService(UserRepository userRepository,
                               PersonRepository personRepository,
                               RefreshTokenRepository refreshTokenRepository,
                               DotNetPasswordHasher passwordHasher,
                               JwtService jwtService) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokens login(String login,
                            String password,
                            Integer lifetimeKind,
                            String ipAddress,
                            String userAgent) {
        validatePassword(password, "Password");
        User user = loadActiveUser(normalizeLogin(login));

        DotNetPasswordHasher.VerificationResult result = passwordHasher.verify(user.getPasswordHash(), password);
        if (!result.success()) {
            throw new BadCredentialsException("Invalid login or password.");
        }

        if (result.requiresRehash()) {
            user.setPasswordHash(passwordHasher.hashPassword(password));
        }

        return issueTokenPair(user, jwtService.normalizeLifetimeKind(lifetimeKind), ipAddress, userAgent);
    }

    @Transactional
    public AuthTokens register(String login,
                               String password,
                               Integer personId,
                               Integer lifetimeKind,
                               String ipAddress,
                               String userAgent) {
        String normalizedLogin = normalizeLogin(login);
        validatePassword(password, "Password");

        if (userRepository.existsByLogin(normalizedLogin)) {
            throw new AuthConflictException("A user with this login already exists.");
        }

        if (personId != null) {
            if (personId <= 0) {
                throw new IllegalArgumentException("PersonId must be greater than 0.");
            }
            if (!personRepository.existsById(personId)) {
                throw new IllegalArgumentException("The specified person was not found.");
            }
            if (userRepository.existsByPersonId(personId)) {
                throw new AuthConflictException("The specified person is already bound to another user.");
            }
        }

        User user = new User();
        user.setLogin(normalizedLogin);
        user.setPasswordHash(passwordHasher.hashPassword(password));
        user.setActive(true);
        user.setPersonId(personId);

        User saved = userRepository.save(user);
        return issueTokenPair(saved, jwtService.normalizeLifetimeKind(lifetimeKind), ipAddress, userAgent);
    }

    @Transactional
    public AuthTokens refresh(String refreshToken, String ipAddress, String userAgent) {
        String tokenHash = jwtService.computeRefreshTokenHash(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token was not found."));

        Instant now = Instant.now();
        if (storedToken.getRevokedAtUtc() != null) {
            throw new BadCredentialsException("Refresh token has already been revoked.");
        }
        if (!storedToken.getExpiresAtUtc().isAfter(now)) {
            throw new BadCredentialsException("Refresh token has expired.");
        }

        User user = userRepository.findWithSecurityById(storedToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException("User account was not found."));

        if (!user.isActive()) {
            throw new BadCredentialsException("User account is inactive.");
        }

        JwtService.GeneratedTokens generatedTokens = jwtService.generateTokens(user, storedToken.getLifetimeKind());

        storedToken.setRevokedAtUtc(now);
        storedToken.setRevokedByIp(normalizeIpAddress(ipAddress));
        storedToken.setReplacedByTokenHash(generatedTokens.refreshTokenHash());

        saveRefreshToken(user.getId(), generatedTokens, ipAddress, userAgent);
        return toAuthTokens(generatedTokens);
    }

    @Transactional
    public void revoke(String refreshToken, String currentLogin, String ipAddress) {
        User currentUser = loadActiveUser(normalizeLogin(currentLogin));
        String tokenHash = jwtService.computeRefreshTokenHash(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token was not found."));

        if (!currentUser.getId().equals(storedToken.getUserId())) {
            throw new IllegalArgumentException("Refresh token does not belong to the current user.");
        }

        if (storedToken.getRevokedAtUtc() == null) {
            storedToken.setRevokedAtUtc(Instant.now());
            storedToken.setRevokedByIp(normalizeIpAddress(ipAddress));
        }
    }

    @Transactional
    public void changePassword(String currentLogin,
                               String currentPassword,
                               String newPassword,
                               String ipAddress) {
        validatePassword(currentPassword, "CurrentPassword");
        validatePassword(newPassword, "NewPassword");

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("The new password must differ from the current password.");
        }

        User user = loadActiveUser(normalizeLogin(currentLogin));
        DotNetPasswordHasher.VerificationResult result = passwordHasher.verify(user.getPasswordHash(), currentPassword);

        if (!result.success()) {
            throw new BadCredentialsException("The current password is incorrect.");
        }

        user.setPasswordHash(passwordHasher.hashPassword(newPassword));

        Instant revokedAtUtc = Instant.now();
        String revokedByIp = normalizeIpAddress(ipAddress);
        refreshTokenRepository.findByUserIdAndRevokedAtUtcIsNullAndExpiresAtUtcAfter(user.getId(), revokedAtUtc)
                .forEach(token -> {
                    token.setRevokedAtUtc(revokedAtUtc);
                    token.setRevokedByIp(revokedByIp);
                });
    }

    @Transactional(readOnly = true)
    public CurrentUser currentUser(String login) {
        User user = loadActiveUser(normalizeLogin(login));
        org.santayn.testing.models.person.Person person = user.getPerson();
        String fullName = person == null
                ? null
                : String.join(" ", person.getLastName(), person.getFirstName()).trim();

        return new CurrentUser(
                user.getId(),
                user.getLogin(),
                user.isActive(),
                user.getPersonId(),
                fullName == null || fullName.isBlank() ? null : fullName,
                person == null ? null : new CurrentUserPerson(
                        person.getId(),
                        person.getFirstName(),
                        person.getLastName(),
                        person.getDateOfBirth(),
                        person.getEmail(),
                        person.getPhone()
                ),
                roleNames(user),
                permissionNames(user)
        );
    }

    @Transactional(readOnly = true)
    public User findUserByLogin(String login) {
        return loadActiveUser(normalizeLogin(login));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findWithSecurityByLogin(normalizeLogin(login))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .disabled(!user.isActive())
                .authorities(buildAuthorities(user))
                .build();
    }

    private AuthTokens issueTokenPair(User user, int lifetimeKind, String ipAddress, String userAgent) {
        JwtService.GeneratedTokens generatedTokens = jwtService.generateTokens(user, lifetimeKind);
        saveRefreshToken(user.getId(), generatedTokens, ipAddress, userAgent);
        return toAuthTokens(generatedTokens);
    }

    private void saveRefreshToken(Integer userId,
                                  JwtService.GeneratedTokens generatedTokens,
                                  String ipAddress,
                                  String userAgent) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(generatedTokens.refreshTokenHash());
        token.setLifetimeKind(generatedTokens.lifetimeKind());
        token.setCreatedAtUtc(Instant.now());
        token.setExpiresAtUtc(generatedTokens.refreshTokenExpiresAtUtc());
        token.setCreatedByIp(normalizeIpAddress(ipAddress));
        token.setCreatedByUserAgent(truncate(userAgent, 1024));

        refreshTokenRepository.save(token);
    }

    private AuthTokens toAuthTokens(JwtService.GeneratedTokens generatedTokens) {
        return new AuthTokens(
                "Bearer",
                generatedTokens.accessToken(),
                generatedTokens.accessTokenExpiresAtUtc(),
                generatedTokens.refreshToken(),
                generatedTokens.refreshTokenExpiresAtUtc(),
                generatedTokens.lifetimeKind()
        );
    }

    private User loadActiveUser(String normalizedLogin) {
        User user = userRepository.findWithSecurityByLogin(normalizedLogin)
                .orElseThrow(() -> new BadCredentialsException("Invalid login or password."));
        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid login or password.");
        }
        return user;
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        Set<String> authorityNames = new LinkedHashSet<>();

        for (String roleName : roleNames(user)) {
            String normalized = roleName.trim().toUpperCase(Locale.ROOT);
            authorityNames.add("ROLE_" + normalized);
            authorityNames.add(normalized);
        }

        for (String permissionName : permissionNames(user)) {
            String trimmed = permissionName.trim();
            authorityNames.add(trimmed);
            authorityNames.add(trimmed.toLowerCase(Locale.ROOT));
            authorityNames.add(trimmed.toUpperCase(Locale.ROOT));
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String authorityName : authorityNames) {
            authorities.add(new SimpleGrantedAuthority(authorityName));
        }
        return authorities;
    }

    private Set<String> roleNames(User user) {
        Set<String> names = new TreeSet<>();
        for (Role role : user.getRoles()) {
            if (role.getName() != null && !role.getName().isBlank()) {
                names.add(role.getName());
            }
        }
        return names;
    }

    private Set<String> permissionNames(User user) {
        Set<String> names = new TreeSet<>();

        for (Permission permission : user.getPermissions()) {
            if (permission.getName() != null && !permission.getName().isBlank()) {
                names.add(permission.getName());
            }
        }

        for (Role role : user.getRoles()) {
            for (Permission permission : role.getPermissions()) {
                if (permission.getName() != null && !permission.getName().isBlank()) {
                    names.add(permission.getName());
                }
            }
        }

        return names;
    }

    private static String normalizeLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login is required.");
        }

        String normalized = login.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > LOGIN_MAX_LENGTH) {
            throw new IllegalArgumentException("Login is too long.");
        }

        return normalized;
    }

    private static void validatePassword(String password, String fieldName) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(fieldName + " must contain at least " + PASSWORD_MIN_LENGTH + " characters.");
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + " is too long.");
        }
    }

    private static String normalizeIpAddress(String ipAddress) {
        return truncate(ipAddress, 45);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record AuthTokens(
            String tokenType,
            String accessToken,
            Instant accessTokenExpiresAtUtc,
            String refreshToken,
            Instant refreshTokenExpiresAtUtc,
            int lifetimeKind
    ) {
    }

    public record CurrentUser(
            Integer userId,
            String login,
            boolean isActive,
            Integer personId,
            String fullName,
            CurrentUserPerson person,
            Set<String> roles,
            Set<String> permissions
    ) {
    }

    public record CurrentUserPerson(
            Integer id,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String email,
            String phone
    ) {
    }
}
