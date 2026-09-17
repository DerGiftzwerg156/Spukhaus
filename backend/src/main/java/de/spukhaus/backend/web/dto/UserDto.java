package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Role;
import de.spukhaus.backend.domain.User;
import java.time.Instant;

public record UserDto(
        Long id,
        String username,
        String displayName,
        Role role,
        boolean enabled,
        boolean mustChangePassword,
        Instant createdAt) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.isEnabled(),
                user.isMustChangePassword(),
                user.getCreatedAt());
    }
}
