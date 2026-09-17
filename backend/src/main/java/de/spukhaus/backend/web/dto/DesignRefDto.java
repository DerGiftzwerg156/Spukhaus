package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignImage;

public record DesignRefDto(Long id, String name, Long previewImageId) {

    public static DesignRefDto from(Design design) {
        var published = design.getCurrentPublishedVersion();
        String name = published != null ? published.getName() : "(unveröffentlicht)";
        DesignImage preview = published != null ? published.getPreviewImage() : null;
        return new DesignRefDto(design.getId(), name, preview != null ? preview.getId() : null);
    }
}
