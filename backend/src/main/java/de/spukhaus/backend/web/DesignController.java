package de.spukhaus.backend.web;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignImage;
import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.domain.VersionStatus;
import de.spukhaus.backend.security.SpukhausUserPrincipal;
import de.spukhaus.backend.service.DesignService;
import de.spukhaus.backend.web.dto.CreateDesignRequest;
import de.spukhaus.backend.web.dto.DesignDetailDto;
import de.spukhaus.backend.web.dto.DesignImageDto;
import de.spukhaus.backend.web.dto.DesignSummaryDto;
import de.spukhaus.backend.web.dto.DesignVersionDto;
import de.spukhaus.backend.web.dto.MyDesignSummaryDto;
import de.spukhaus.backend.web.dto.PageResponse;
import de.spukhaus.backend.web.dto.RejectRequest;
import de.spukhaus.backend.web.dto.UpdateDesignRequest;
import de.spukhaus.backend.web.exception.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/designs")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @GetMapping
    public PageResponse<DesignSummaryDto> list(@RequestParam(required = false) String query,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "24") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(designService.listPublished(query, pageable), DesignSummaryDto::from);
    }

    @GetMapping("/mine")
    public List<MyDesignSummaryDto> mine(@AuthenticationPrincipal SpukhausUserPrincipal principal) {
        return designService.listOwnedBy(principal.getUser()).stream()
                .map(d -> MyDesignSummaryDto.from(d, designService.findInFlightVersion(d)))
                .toList();
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasAnyRole('ADMIN','TECH_ADMIN')")
    public List<DesignVersionDto> reviewQueue() {
        return designService.listReviewQueue().stream().map(DesignVersionDto::from).toList();
    }

    @GetMapping("/{id}")
    public DesignDetailDto detail(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        User viewer = principal.getUser();
        Design design = designService.getVisibleForViewer(id, viewer);
        boolean canManage = viewer.isAtLeastAdmin() || design.getOwner().getId().equals(viewer.getId());
        DesignVersion inFlight = designService.findInFlightVersion(design);
        List<Design> forks = designService.listPublishedForks(design);
        return DesignDetailDto.build(design, canManage, inFlight, forks);
    }

    @GetMapping("/{id}/versions")
    public List<DesignVersionDto> versionHistory(@PathVariable Long id,
                                                  @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        User viewer = principal.getUser();
        Design design = designService.getVisibleForViewer(id, viewer);
        boolean canManage = viewer.isAtLeastAdmin() || design.getOwner().getId().equals(viewer.getId());
        return designService.listVersionHistory(design).stream()
                .filter(v -> canManage || v.getStatus() == VersionStatus.PUBLISHED
                        || v.getStatus() == VersionStatus.SUPERSEDED)
                .map(DesignVersionDto::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CREATOR','ADMIN','TECH_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DesignDetailDto create(@AuthenticationPrincipal SpukhausUserPrincipal principal,
                                   @Valid @RequestBody CreateDesignRequest request) {
        Design design = designService.createDesign(principal.getUser(), request.name(), request.description());
        return DesignDetailDto.build(design, true, designService.findInFlightVersion(design), List.of());
    }

    @PostMapping("/{id}/fork")
    @PreAuthorize("hasAnyRole('CREATOR','ADMIN','TECH_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DesignDetailDto fork(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design fork = designService.forkDesign(principal.getUser(), id);
        return DesignDetailDto.build(fork, true, designService.findInFlightVersion(fork), List.of());
    }

    @PutMapping("/{id}/draft")
    public DesignVersionDto updateDraft(@PathVariable Long id,
                                         @AuthenticationPrincipal SpukhausUserPrincipal principal,
                                         @Valid @RequestBody UpdateDesignRequest request) {
        Design design = designService.getById(id);
        DesignVersion draft = designService.updateDraftContent(principal.getUser(), design,
                request.name(), request.description());
        return DesignVersionDto.from(draft);
    }

    @PostMapping("/{id}/edit")
    public DesignVersionDto startEditing(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design design = designService.getById(id);
        DesignVersion draft = designService.getOrCreateEditableVersion(principal.getUser(), design);
        return DesignVersionDto.from(draft);
    }

    @PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
    public DesignImageDto addImage(@PathVariable Long id,
                                    @AuthenticationPrincipal SpukhausUserPrincipal principal,
                                    @RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Datei ist leer.");
        }
        Design design = designService.getById(id);
        DesignImage image = designService.addImage(principal.getUser(), design, file);
        return DesignImageDto.from(image);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public void removeImage(@PathVariable Long id, @PathVariable Long imageId,
                             @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design design = designService.getById(id);
        designService.removeImage(principal.getUser(), design, imageId);
    }

    @PutMapping("/{id}/images/{imageId}/preview")
    public void setPreviewImage(@PathVariable Long id, @PathVariable Long imageId,
                                 @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design design = designService.getById(id);
        designService.setPreviewImage(principal.getUser(), design, imageId);
    }

    @PostMapping("/{id}/submit")
    public DesignVersionDto submit(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design design = designService.getById(id);
        DesignVersion version = designService.submitForReview(principal.getUser(), design);
        return DesignVersionDto.from(version);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','TECH_ADMIN')")
    public DesignVersionDto approve(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        Design design = designService.getById(id);
        DesignVersion version = designService.approve(principal.getUser(), design);
        return DesignVersionDto.from(version);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','TECH_ADMIN')")
    public DesignVersionDto reject(@PathVariable Long id, @AuthenticationPrincipal SpukhausUserPrincipal principal,
                                    @Valid @RequestBody RejectRequest request) {
        Design design = designService.getById(id);
        DesignVersion version = designService.reject(principal.getUser(), design, request.comment());
        return DesignVersionDto.from(version);
    }
}
