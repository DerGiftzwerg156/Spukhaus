package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.DesignImage;

public record DesignImageDto(
        Long id,
        boolean preview,
        int sortOrder,
        String originalFilename) {

    public static DesignImageDto from(DesignImage image) {
        return new DesignImageDto(image.getId(), image.isPreview(), image.getSortOrder(), image.getOriginalFilename());
    }
}
