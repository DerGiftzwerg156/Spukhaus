package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,100}$", message = "3-100 Zeichen, nur Buchstaben/Zahlen/._-")
        String username,
        @NotBlank String displayName,
        @NotNull Role role) {
}
