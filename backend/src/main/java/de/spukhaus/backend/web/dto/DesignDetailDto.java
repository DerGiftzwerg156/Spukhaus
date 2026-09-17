package de.spukhaus.backend.web.dto;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignVersion;
import java.time.Instant;
import java.util.List;

public record DesignDetailDto(
        Long id,
        Long ownerId,
        String ownerDisplayName,
        Instant createdAt,
        boolean canManage,
        DesignRefDto forkedFrom,
        DesignVersionDto publishedVersion,
        DesignVersionDto inFlightVersion,
        List<DesignRefDto> forks) {

    public static DesignDetailDto build(Design design, boolean canManage, DesignVersion inFlight,
                                         List<Design> publishedForks) {
        return new DesignDetailDto(
                design.getId(),
                design.getOwner().getId(),
                design.getOwner().getDisplayName(),
                design.getCreatedAt(),
                canManage,
                design.getForkedFrom() != null ? DesignRefDto.from(design.getForkedFrom()) : null,
                design.getCurrentPublishedVersion() != null
                        ? DesignVersionDto.from(design.getCurrentPublishedVersion()) : null,
                (canManage && inFlight != null) ? DesignVersionDto.from(inFlight) : null,
                publishedForks.stream().map(DesignRefDto::from).toList());
    }
}
