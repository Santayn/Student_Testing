package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.santayn.testing.service.UserRegisterService;
import org.santayn.testing.service.UserService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserProfileRestController {

    private final UserService userService;
    private final UserRegisterService userRegisterService;

    public UserProfileRestController(UserService userService, UserRegisterService userRegisterService) {
        this.userService = userService;
        this.userRegisterService = userRegisterService;
    }

    @GetMapping
    public List<ApiResponses.UserResponse> users() {
        return ApiResponses.list(userService.findUsers(), ApiResponses::user);
    }

    @GetMapping("/{id}")
    public ApiResponses.UserResponse user(@PathVariable Integer id) {
        return ApiResponses.user(userService.getUser(id));
    }

    @GetMapping("/me")
    public UserRegisterService.CurrentUser me(Authentication authentication) {
        return userRegisterService.currentUser(authentication.getName());
    }

    @GetMapping("/people")
    public List<ApiResponses.PersonResponse> people(@RequestParam(required = false) String role) {
        return ApiResponses.list(userService.findPeople(role), ApiResponses::person);
    }

    @GetMapping("/people/{personId}")
    public ApiResponses.PersonResponse person(@PathVariable Integer personId) {
        return ApiResponses.person(userService.getPerson(personId));
    }

    @PostMapping("/people")
    public ApiResponses.PersonResponse createPerson(@Valid @RequestBody PersonRequest request) {
        return ApiResponses.person(userService.createPerson(
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.email(),
                request.phone()
        ));
    }

    @PutMapping("/people/{personId}")
    public ApiResponses.PersonResponse updatePerson(@PathVariable Integer personId, @Valid @RequestBody PersonRequest request) {
        return ApiResponses.person(userService.updatePerson(
                personId,
                request.firstName(),
                request.lastName(),
                request.dateOfBirth(),
                request.email(),
                request.phone()
        ));
    }

    @PutMapping("/{id}/active")
    public ApiResponses.UserResponse setActive(@PathVariable Integer id, @Valid @RequestBody UserActiveRequest request) {
        return ApiResponses.user(userService.setActive(id, request.active()));
    }

    public record PersonRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            LocalDate dateOfBirth,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 50) String phone
    ) {
    }

    public record UserActiveRequest(boolean active) {
    }
}
