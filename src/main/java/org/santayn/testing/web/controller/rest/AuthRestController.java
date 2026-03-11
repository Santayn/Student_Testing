package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.santayn.testing.models.user.User;
import org.santayn.testing.security.JwtService;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final UserRegisterService userRegisterService;
    private final JwtService jwtService;

    public AuthRestController(AuthenticationManager authenticationManager,
                              UserRegisterService userRegisterService,
                              JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRegisterService = userRegisterService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password())
        );

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Неверный логин или пароль");
        }

        User user = userRegisterService.findUserByLogin(request.login());
        String accessToken = jwtService.generateAccessToken(user);

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                UserInfo.from(user)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = userRegisterService.registerUser(
                request.login(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phoneNumber()
        );

        String accessToken = jwtService.generateAccessToken(created);

        AuthResponse response = new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                UserInfo.from(created)
        );

        return ResponseEntity
                .created(URI.create("/api/v1/users/" + created.getId()))
                .body(response);
    }

    @GetMapping("/me")
    public UserInfo me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Пользователь не авторизован");
        }

        User user = userRegisterService.findUserByLogin(authentication.getName());
        return UserInfo.from(user);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        return new ErrorResponse("unauthorized", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse("bad_request", ex.getMessage());
    }

    public record LoginRequest(
            @NotBlank String login,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank String login,
            @NotBlank String password,
            String firstName,
            String lastName,
            String phoneNumber
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {
    }

    public record UserInfo(
            Integer id,
            String login,
            String firstName,
            String lastName,
            String phoneNumber,
            String role,
            Integer studentId,
            Integer teacherId
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getLogin(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPhoneNumber(),
                    user.getRole() != null ? user.getRole().getName() : null,
                    user.getStudent() != null ? user.getStudent().getId() : null,
                    user.getTeacher() != null ? user.getTeacher().getId() : null
            );
        }
    }

    public record ErrorResponse(
            String error,
            String message
    ) {
    }
}