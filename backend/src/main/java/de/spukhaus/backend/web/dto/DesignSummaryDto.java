package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignImage;
import de.spukhaus.backend.domain.DesignVersion;
import java.time.Instant;

public record DesignSummaryDto(
        Long id,
        String name,
        Long previewImageId,
        String ownerDisplayName,
        Instant publishedAt,
        boolean isFork) {

    public static DesignSummaryDto from(Design design) {
        DesignVersion published = design.getCurrentPublishedVersion();
        DesignImage preview = published != null ? published.getPreviewImage() : null;
        return new DesignSummaryDto(
                design.getId(),
                published != null ? published.getName() : "(unveröffentlicht)",
                preview != null ? preview.getId() : null,
                design.getOwner().getDisplayName(),
                published != null ? published.getReviewedAt() : null,
                design.getForkedFrom() != null);
    }
}
