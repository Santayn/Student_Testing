package org.santayn.testing.web.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
public class AuthRestController {

    private final UserRegisterService userRegisterService;

    public AuthRestController(UserRegisterService userRegisterService) {
        this.userRegisterService = userRegisterService;
    }

    @PostMapping("/login")
    public UserRegisterService.AuthTokens login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        return userRegisterService.login(
                request.login(),
                request.password(),
                request.lifetimeKind(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterService.AuthTokens register(@Valid @RequestBody RegisterRequest request,
                                                   HttpServletRequest httpRequest) {
        return userRegisterService.register(
                request.login(),
                request.password(),
                request.personId(),
                request.lifetimeKind(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/refresh")
    public UserRegisterService.AuthTokens refresh(@Valid @RequestBody RefreshRequest request,
                                                  HttpServletRequest httpRequest) {
        return userRegisterService.refresh(
                request.refreshToken(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Valid @RequestBody RefreshRequest request,
                       Authentication authentication,
                       HttpServletRequest httpRequest) {
        userRegisterService.revoke(request.refreshToken(), requireLogin(authentication), httpRequest.getRemoteAddr());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               Authentication authentication,
                               HttpServletRequest httpRequest) {
        userRegisterService.changePassword(
                requireLogin(authentication),
                request.currentPassword(),
                request.newPassword(),
                httpRequest.getRemoteAddr()
        );
    }

    @GetMapping("/me")
    public UserRegisterService.CurrentUser me(Authentication authentication) {
        return userRegisterService.currentUser(requireLogin(authentication));
    }

    private String requireLogin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Authentication is required.");
        }
        return authentication.getName();
    }

    public record LoginRequest(
            @NotBlank @Size(max = 100) String login,
            @NotBlank @Size(min = 6, max = 200) String password,
            Integer lifetimeKind
    ) {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String login,
            @NotBlank @Size(min = 6, max = 200) String password,
            @Positive Integer personId,
            Integer lifetimeKind
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(min = 6, max = 200) String currentPassword,
            @NotBlank @Size(min = 6, max = 200) String newPassword
    ) {
    }
}
