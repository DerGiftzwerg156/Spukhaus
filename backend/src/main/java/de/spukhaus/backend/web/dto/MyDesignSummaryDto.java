package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.VersionStatus;

public record MyDesignSummaryDto(
        Long id,
        String name,
        VersionStatus status,
        Long previewImageId,
        boolean isFork,
        boolean hasPublishedVersion) {

    public static MyDesignSummaryDto from(Design design, DesignVersion inFlight) {
        DesignVersion relevant = inFlight != null ? inFlight : design.getCurrentPublishedVersion();
        var preview = relevant != null ? relevant.getPreviewImage() : null;
        return new MyDesignSummaryDto(
                design.getId(),
                relevant != null ? relevant.getName() : "(neues Design)",
                relevant != null ? relevant.getStatus() : VersionStatus.DRAFT,
                preview != null ? preview.getId() : null,
                design.getForkedFrom() != null,
                design.getCurrentPublishedVersion() != null);
    }
}
