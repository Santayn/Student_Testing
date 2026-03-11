package org.santayn.testing.service;

import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UserRegisterService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegisterService(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String login,
                             String password,
                             String firstName,
                             String lastName,
                             String phoneNumber) {
        return registerUser(login, password, firstName, lastName, phoneNumber, "USER");
    }

    public User registerUser(String login,
                             String password,
                             String firstName,
                             String lastName,
                             String phoneNumber,
                             String roleName) {

        validateRegistration(login, password);

        if (userRepository.findByLogin(login).isPresent()) {
            throw new IllegalArgumentException("Логин уже занят: " + login);
        }

        String normalizedRoleName = normalizeRoleName(roleName);

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + normalizedRoleName));

        User user = new User();
        user.setLogin(login.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName == null ? null : firstName.trim());
        user.setLastName(lastName == null ? null : lastName.trim());
        user.setPhoneNumber(phoneNumber == null ? null : phoneNumber.trim());
        user.setRole(role);

        return userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + login));

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                buildAuthorities(user)
        );
    }

    public User findUserByLogin(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден: " + login));
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null && user.getRole().getName() != null && !user.getRole().getName().isBlank()) {
            String roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            authorities.add(new SimpleGrantedAuthority(roleName));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("USER"));
        }

        return authorities;
    }

    private void validateRegistration(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин обязателен");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль обязателен");
        }
        if (password.length() < 4) {
            throw new IllegalArgumentException("Пароль должен содержать минимум 4 символа");
        }
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "USER";
        }
        return roleName.trim().toUpperCase(Locale.ROOT);
    }
}