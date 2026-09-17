package de.spukhaus.backend.web;

import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.security.SpukhausUserPrincipal;
import de.spukhaus.backend.service.UserService;
import de.spukhaus.backend.web.dto.CreateUserRequest;
import de.spukhaus.backend.web.dto.TemporaryPasswordResponse;
import de.spukhaus.backend.web.dto.UpdateUserRequest;
import de.spukhaus.backend.web.dto.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('ADMIN','TECH_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDto> list() {
        return userService.listUsers().stream().map(UserDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporaryPasswordResponse create(@AuthenticationPrincipal SpukhausUserPrincipal principal,
                                             @Valid @RequestBody CreateUserRequest request) {
        UserService.CreatedUser created = userService.createUser(principal.getUser(), request);
        return new TemporaryPasswordResponse(UserDto.from(created.user()), created.temporaryPassword());
    }

    @PatchMapping("/{id}")
    public UserDto update(@AuthenticationPrincipal SpukhausUserPrincipal principal,
                           @PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userService.updateUser(principal.getUser(), id, request);
        return UserDto.from(user);
    }

    @PostMapping("/{id}/reset-password")
    public TemporaryPasswordResponse resetPassword(@AuthenticationPrincipal SpukhausUserPrincipal principal,
                                                     @PathVariable Long id) {
        String tempPassword = userService.resetPassword(principal.getUser(), id);
        return new TemporaryPasswordResponse(UserDto.from(userService.getById(id)), tempPassword);
    }
}
