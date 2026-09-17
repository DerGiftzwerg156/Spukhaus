package de.spukhaus.backend.web.dto;

public record LoginResponse(String token, UserDto user) {
}
