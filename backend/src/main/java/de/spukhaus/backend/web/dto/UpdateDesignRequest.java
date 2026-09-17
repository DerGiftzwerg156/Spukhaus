package de.spukhaus.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDesignRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 5000) String description) {
}
