package de.spukhaus.backend.web;

import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.security.JwtService;
import de.spukhaus.backend.security.SpukhausUserPrincipal;
import de.spukhaus.backend.service.UserService;
import de.spukhaus.backend.web.dto.ChangePasswordRequest;
import de.spukhaus.backend.web.dto.LoginRequest;
import de.spukhaus.backend.web.dto.LoginResponse;
import de.spukhaus.backend.web.dto.UserDto;
import de.spukhaus.backend.web.exception.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                           UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            var authToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
            var authentication = authenticationManager.authenticate(authToken);
            SpukhausUserPrincipal principal = (SpukhausUserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();
            String token = jwtService.generateToken(user.getId(), user.getUsername());
            return new LoginResponse(token, UserDto.from(user));
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Benutzername oder Passwort ist falsch.");
        } catch (DisabledException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Dieses Konto ist deaktiviert.");
        }
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal SpukhausUserPrincipal principal) {
        return UserDto.from(principal.getUser());
    }

    @PostMapping("/change-password")
    public UserDto changePassword(@AuthenticationPrincipal SpukhausUserPrincipal principal,
                                   @Valid @RequestBody ChangePasswordRequest request) {
        User user = userService.getById(principal.getId());
        userService.changeOwnPassword(user, request.currentPassword(), request.newPassword());
        return UserDto.from(user);
    }
}
