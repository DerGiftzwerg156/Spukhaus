package de.spukhaus.backend.service;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignImage;
import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.domain.VersionStatus;
import de.spukhaus.backend.repository.DesignImageRepository;
import de.spukhaus.backend.repository.DesignRepository;
import de.spukhaus.backend.repository.DesignVersionRepository;
import de.spukhaus.backend.storage.ImageStorageService;
import de.spukhaus.backend.storage.StoredImage;
import de.spukhaus.backend.web.exception.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DesignService {

    private static final List<VersionStatus> IN_FLIGHT_STATUSES =
            List.of(VersionStatus.DRAFT, VersionStatus.PENDING_REVIEW, VersionStatus.REJECTED);

    private final DesignRepository designRepository;
    private final DesignVersionRepository designVersionRepository;
    private final DesignImageRepository designImageRepository;
    private final ImageStorageService imageStorageService;

    public DesignService(DesignRepository designRepository, DesignVersionRepository designVersionRepository,
                          DesignImageRepository designImageRepository, ImageStorageService imageStorageService) {
        this.designRepository = designRepository;
        this.designVersionRepository = designVersionRepository;
        this.designImageRepository = designImageRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional(readOnly = true)
    public Design getById(Long id) {
        return designRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Design nicht gefunden."));
    }

    /** Design-Detailansicht: wirft 404, wenn der Betrachter nichts sehen darf. */
    @Transactional(readOnly = true)
    public Design getVisibleForViewer(Long id, User viewer) {
        Design design = getById(id);
        if (design.getCurrentPublishedVersion() == null && !canManage(viewer, design)) {
            throw ApiException.notFound("Design nicht gefunden.");
        }
        return design;
    }

    public Design createDesign(User creator, String name, String description) {
        Design design = new Design(creator, null);
        designRepository.save(design);
        DesignVersion version = new DesignVersion(design, design.allocateNextVersionNumber(), name, description);
        designVersionRepository.save(version);
        return design;
    }

    public Design forkDesign(User creator, Long sourceDesignId) {
        Design source = getById(sourceDesignId);
        DesignVersion sourcePublished = source.getCurrentPublishedVersion();
        if (sourcePublished == null) {
            throw ApiException.badRequest("Nur veröffentlichte Designs können geforkt werden.");
        }
        Design fork = new Design(creator, source);
        designRepository.save(fork);
        DesignVersion draft = new DesignVersion(fork, fork.allocateNextVersionNumber(),
                sourcePublished.getName(), sourcePublished.getDescription());
        designVersionRepository.save(draft);
        for (DesignImage sourceImage : sourcePublished.getImages()) {
            DesignImage copy = new DesignImage(draft, sourceImage.getStorageKey(), sourceImage.getOriginalFilename(),
                    sourceImage.getContentType(), sourceImage.getSizeBytes(), sourceImage.getSortOrder());
            copy.setPreview(sourceImage.isPreview());
            draft.getImages().add(copy);
        }
        return fork;
    }

    @Transactional(readOnly = true)
    public DesignVersion findInFlightVersion(Design design) {
        return designVersionRepository
                .findFirstByDesignAndStatusInOrderByVersionNumberDesc(design, IN_FLIGHT_STATUSES)
                .orElse(null);
    }

    /** Liefert die bearbeitbare Draft-Version; legt bei Bedarf eine neue an (Klon der veröffentlichten Version). */
    public DesignVersion getOrCreateEditableVersion(User actor, Design design) {
        requireCanManage(actor, design);
        DesignVersion inFlight = findInFlightVersion(design);
        if (inFlight != null) {
            if (inFlight.getStatus() == VersionStatus.PENDING_REVIEW) {
                throw ApiException.conflict("Diese Version wird gerade von einem Admin geprüft.");
            }
            if (inFlight.getStatus() == VersionStatus.REJECTED) {
                inFlight.setStatus(VersionStatus.DRAFT);
            }
            return inFlight;
        }
        DesignVersion published = design.getCurrentPublishedVersion();
        if (published == null) {
            throw ApiException.notFound("Keine bearbeitbare Version gefunden.");
        }
        DesignVersion draft = new DesignVersion(design, design.allocateNextVersionNumber(),
                published.getName(), published.getDescription());
        designVersionRepository.save(draft);
        for (DesignImage sourceImage : published.getImages()) {
            DesignImage copy = new DesignImage(draft, sourceImage.getStorageKey(), sourceImage.getOriginalFilename(),
                    sourceImage.getContentType(), sourceImage.getSizeBytes(), sourceImage.getSortOrder());
            copy.setPreview(sourceImage.isPreview());
            draft.getImages().add(copy);
        }
        return draft;
    }

    public DesignVersion updateDraftContent(User actor, Design design, String name, String description) {
        DesignVersion draft = requireEditableDraft(actor, design);
        if (name == null || name.isBlank()) {
            throw ApiException.badRequest("Name darf nicht leer sein.");
        }
        if (description == null || description.isBlank()) {
            throw ApiException.badRequest("Beschreibung darf nicht leer sein.");
        }
        draft.setName(name);
        draft.setDescription(description);
        return draft;
    }

    public DesignImage addImage(User actor, Design design, MultipartFile file) {
        DesignVersion draft = getOrCreateEditableVersion(actor, design);
        if (draft.getStatus() != VersionStatus.DRAFT) {
            throw ApiException.conflict("Version kann aktuell nicht bearbeitet werden.");
        }
        StoredImage stored = imageStorageService.upload(file, "designs/" + design.getId());
        int nextSortOrder = draft.getImages().stream().mapToInt(DesignImage::getSortOrder).max().orElse(-1) + 1;
        DesignImage image = new DesignImage(draft, stored.storageKey(), file.getOriginalFilename(),
                stored.contentType(), stored.sizeBytes(), nextSortOrder);
        if (draft.getImages().isEmpty()) {
            image.setPreview(true);
        }
        draft.getImages().add(image);
        designImageRepository.save(image);
        return image;
    }

    public void removeImage(User actor, Design design, Long imageId) {
        DesignVersion draft = requireEditableDraft(actor, design);
        DesignImage image = draft.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Bild nicht gefunden."));
        boolean wasPreview = image.isPreview();
        draft.getImages().remove(image);
        designImageRepository.delete(image);
        if (wasPreview && !draft.getImages().isEmpty()) {
            draft.getImages().stream()
                    .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                    .ifPresent(next -> next.setPreview(true));
        }
    }

    public void setPreviewImage(User actor, Design design, Long imageId) {
        DesignVersion draft = requireEditableDraft(actor, design);
        boolean found = false;
        for (DesignImage image : draft.getImages()) {
            boolean isTarget = image.getId().equals(imageId);
            image.setPreview(isTarget);
            found = found || isTarget;
        }
        if (!found) {
            throw ApiException.notFound("Bild nicht gefunden.");
        }
    }

    public DesignVersion submitForReview(User actor, Design design) {
        requireCanManage(actor, design);
        DesignVersion draft = findInFlightVersion(design);
        if (draft == null || (draft.getStatus() != VersionStatus.DRAFT && draft.getStatus() != VersionStatus.REJECTED)) {
            throw ApiException.conflict("Keine bearbeitbare Version zum Einreichen vorhanden.");
        }
        if (draft.getName() == null || draft.getName().isBlank()) {
            throw ApiException.badRequest("Name darf nicht leer sein.");
        }
        if (draft.getDescription() == null || draft.getDescription().isBlank()) {
            throw ApiException.badRequest("Beschreibung darf nicht leer sein.");
        }
        if (draft.getImages().isEmpty()) {
            throw ApiException.badRequest("Mindestens ein Bild ist erforderlich.");
        }
        if (draft.getImages().stream().noneMatch(DesignImage::isPreview)) {
            draft.getImages().stream()
                    .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                    .ifPresent(img -> img.setPreview(true));
        }
        draft.setStatus(VersionStatus.PENDING_REVIEW);
        draft.setSubmittedAt(Instant.now());
        draft.setRejectionComment(null);
        return draft;
    }

    public DesignVersion approve(User admin, Design design) {
        DesignVersion pending = requirePendingVersion(design);
        DesignVersion previouslyPublished = design.getCurrentPublishedVersion();
        if (previouslyPublished != null) {
            previouslyPublished.setStatus(VersionStatus.SUPERSEDED);
        }
        pending.setStatus(VersionStatus.PUBLISHED);
        pending.setReviewedAt(Instant.now());
        pending.setReviewedBy(admin);
        pending.setRejectionComment(null);
        design.setCurrentPublishedVersion(pending);
        return pending;
    }

    public DesignVersion reject(User admin, Design design, String comment) {
        if (comment == null || comment.isBlank()) {
            throw ApiException.badRequest("Für eine Ablehnung ist ein Kommentar erforderlich.");
        }
        DesignVersion pending = requirePendingVersion(design);
        pending.setStatus(VersionStatus.REJECTED);
        pending.setReviewedAt(Instant.now());
        pending.setReviewedBy(admin);
        pending.setRejectionComment(comment);
        return pending;
    }

    private DesignVersion requirePendingVersion(Design design) {
        DesignVersion inFlight = findInFlightVersion(design);
        if (inFlight == null || inFlight.getStatus() != VersionStatus.PENDING_REVIEW) {
            throw ApiException.conflict("Keine zur Prüfung eingereichte Version vorhanden.");
        }
        return inFlight;
    }

    private DesignVersion requireEditableDraft(User actor, Design design) {
        requireCanManage(actor, design);
        DesignVersion inFlight = findInFlightVersion(design);
        if (inFlight == null || inFlight.getStatus() != VersionStatus.DRAFT) {
            throw ApiException.conflict("Version ist aktuell nicht bearbeitbar.");
        }
        return inFlight;
    }

    public boolean canManage(User actor, Design design) {
        return actor.isAtLeastAdmin() || design.getOwner().getId().equals(actor.getId());
    }

    private void requireCanManage(User actor, Design design) {
        if (!canManage(actor, design)) {
            throw ApiException.forbidden("Nur der Ersteller oder ein Admin darf dieses Design bearbeiten.");
        }
    }

    @Transactional(readOnly = true)
    public boolean canViewImage(User viewer, DesignImage image) {
        DesignVersion version = image.getDesignVersion();
        return version.getStatus() == VersionStatus.PUBLISHED
                || version.getStatus() == VersionStatus.SUPERSEDED
                || canManage(viewer, version.getDesign());
    }

    @Transactional(readOnly = true)
    public DesignImage getImageById(Long imageId) {
        return designImageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Bild nicht gefunden."));
    }

    @Transactional(readOnly = true)
    public Page<Design> listPublished(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return designRepository.findAllPublished(pageable);
        }
        return designRepository.searchPublished(query.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Design> listOwnedBy(User owner) {
        return designRepository.findByOwner(owner);
    }

    @Transactional(readOnly = true)
    public List<DesignVersion> listReviewQueue() {
        return designVersionRepository.findByStatusOrderBySubmittedAtAsc(VersionStatus.PENDING_REVIEW);
    }

    @Transactional(readOnly = true)
    public List<Design> listPublishedForks(Design design) {
        return designRepository.findByForkedFrom(design).stream()
                .filter(d -> d.getCurrentPublishedVersion() != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DesignVersion> listVersionHistory(Design design) {
        return designVersionRepository.findByDesignOrderByVersionNumberDesc(design);
    }
}
