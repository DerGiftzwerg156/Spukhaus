package de.spukhaus.backend.web;

import de.spukhaus.backend.domain.DesignImage;
import de.spukhaus.backend.security.SpukhausUserPrincipal;
import de.spukhaus.backend.service.DesignService;
import de.spukhaus.backend.storage.ImageStorageService;
import de.spukhaus.backend.web.exception.ApiException;
import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final DesignService designService;
    private final ImageStorageService imageStorageService;

    public ImageController(DesignService designService, ImageStorageService imageStorageService) {
        this.designService = designService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<InputStreamResource> original(@PathVariable Long imageId,
                                                          @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        DesignImage image = requireViewable(imageId, principal);
        InputStream stream = imageStorageService.download(image.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePrivate())
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/{imageId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long imageId,
                                             @AuthenticationPrincipal SpukhausUserPrincipal principal) {
        DesignImage image = requireViewable(imageId, principal);
        byte[] bytes = imageStorageService.getOrCreateThumbnail(image.getStorageKey());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePrivate())
                .body(bytes);
    }

    private DesignImage requireViewable(Long imageId, SpukhausUserPrincipal principal) {
        DesignImage image = designService.getImageById(imageId);
        if (!designService.canViewImage(principal.getUser(), image)) {
            throw ApiException.notFound("Bild nicht gefunden.");
        }
        return image;
    }
}
