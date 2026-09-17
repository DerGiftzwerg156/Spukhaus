package de.spukhaus.backend.web.dto;

public record TemporaryPasswordResponse(UserDto user, String temporaryPassword) {
}
