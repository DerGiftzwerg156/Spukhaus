package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.VersionStatus;
import java.time.Instant;
import java.util.List;

public record DesignVersionDto(
        Long id,
        int versionNumber,
        String name,
        String description,
        VersionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewedByDisplayName,
        String rejectionComment,
        List<DesignImageDto> images) {

    public static DesignVersionDto from(DesignVersion version) {
        return new DesignVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getName(),
                version.getDescription(),
                version.getStatus(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                version.getSubmittedAt(),
                version.getReviewedAt(),
                version.getReviewedBy() != null ? version.getReviewedBy().getDisplayName() : null,
                version.getRejectionComment(),
                version.getImages().stream().map(DesignImageDto::from).toList());
    }
}
