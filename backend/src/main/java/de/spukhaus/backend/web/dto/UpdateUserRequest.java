package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Role;

public record UpdateUserRequest(
        String displayName,
        Role role,
        Boolean enabled) {
}
