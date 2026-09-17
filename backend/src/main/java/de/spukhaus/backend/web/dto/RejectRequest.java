package de.spukhaus.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String comment) {
}
