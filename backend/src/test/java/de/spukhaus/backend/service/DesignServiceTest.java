package de.spukhaus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.spukhaus.backend.domain.Design;
import de.spukhaus.backend.domain.DesignImage;
import de.spukhaus.backend.domain.DesignVersion;
import de.spukhaus.backend.domain.Role;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.domain.VersionStatus;
import de.spukhaus.backend.repository.UserRepository;
import de.spukhaus.backend.storage.ImageStorageService;
import de.spukhaus.backend.storage.StoredImage;
import de.spukhaus.backend.web.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DesignServiceTest {

    @Autowired
    private DesignService designService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ImageStorageService imageStorageService;

    private User creator;
    private User otherCreator;
    private User admin;
    private User plainUser;

    @BeforeEach
    void setUp() {
        creator = userRepository.save(new User("creator", passwordEncoder.encode("pw"), "Creator One", Role.CREATOR));
        otherCreator = userRepository.save(new User("creator2", passwordEncoder.encode("pw"), "Creator Two", Role.CREATOR));
        admin = userRepository.save(new User("admin", passwordEncoder.encode("pw"), "Admin", Role.ADMIN));
        plainUser = userRepository.save(new User("user", passwordEncoder.encode("pw"), "Plain User", Role.USER));

        when(imageStorageService.upload(any(), anyString()))
                .thenReturn(new StoredImage("designs/1/abc.png", "image/png", 1234L));
    }

    private MockMultipartFile fakeImage() {
        return new MockMultipartFile("file", "mask.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void createDesignStartsAsDraftWithVersionOne() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");
        DesignVersion draft = designService.findInFlightVersion(design);

        assertThat(draft.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(draft.getVersionNumber()).isEqualTo(1);
        assertThat(design.getCurrentPublishedVersion()).isNull();
    }

    @Test
    void newDesignNotVisibleToOthersUntilPublished() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");

        assertThatThrownBy(() -> designService.getVisibleForViewer(design.getId(), plainUser))
                .isInstanceOf(ApiException.class);

        // Eigentümer und Admin dürfen den Entwurf trotzdem sehen.
        assertThat(designService.getVisibleForViewer(design.getId(), creator)).isEqualTo(design);
        assertThat(designService.getVisibleForViewer(design.getId(), admin)).isEqualTo(design);
    }

    @Test
    void submitRequiresNameDescriptionAndAtLeastOneImage() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");

        assertThatThrownBy(() -> designService.submitForReview(creator, design))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Bild");

        designService.addImage(creator, design, fakeImage());
        DesignVersion submitted = designService.submitForReview(creator, design);

        assertThat(submitted.getStatus()).isEqualTo(VersionStatus.PENDING_REVIEW);
        assertThat(submitted.getSubmittedAt()).isNotNull();
    }

    @Test
    void firstUploadedImageBecomesPreviewByDefault() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");
        DesignImage first = designService.addImage(creator, design, fakeImage());
        DesignImage second = designService.addImage(creator, design, fakeImage());

        assertThat(first.isPreview()).isTrue();
        assertThat(second.isPreview()).isFalse();
    }

    @Test
    void adminApprovalPublishesVersionAndSupersedesPrevious() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");
        designService.addImage(creator, design, fakeImage());
        designService.submitForReview(creator, design);

        DesignVersion published = designService.approve(admin, design);

        assertThat(published.getStatus()).isEqualTo(VersionStatus.PUBLISHED);
        assertThat(design.getCurrentPublishedVersion()).isEqualTo(published);

        // Zweite Version: bearbeiten, erneut einreichen, bestätigen -> alte Version wird SUPERSEDED
        DesignVersion editV2 = designService.getOrCreateEditableVersion(creator, design);
        designService.updateDraftContent(creator, design, "Geisterclown v2", "Neue Beschreibung");
        designService.submitForReview(creator, design);
        DesignVersion publishedV2 = designService.approve(admin, design);

        assertThat(publishedV2.getVersionNumber()).isEqualTo(2);
        assertThat(published.getStatus()).isEqualTo(VersionStatus.SUPERSEDED);
        assertThat(design.getCurrentPublishedVersion()).isEqualTo(publishedV2);
    }

    @Test
    void rejectionRequiresCommentAndReturnsDraftToCreatorOnEdit() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");
        designService.addImage(creator, design, fakeImage());
        designService.submitForReview(creator, design);

        assertThatThrownBy(() -> designService.reject(admin, design, " "))
                .isInstanceOf(ApiException.class);

        DesignVersion rejected = designService.reject(admin, design, "Bitte bessere Fotos hochladen.");
        assertThat(rejected.getStatus()).isEqualTo(VersionStatus.REJECTED);
        assertThat(rejected.getRejectionComment()).isEqualTo("Bitte bessere Fotos hochladen.");

        DesignVersion backToDraft = designService.getOrCreateEditableVersion(creator, design);
        assertThat(backToDraft.getStatus()).isEqualTo(VersionStatus.DRAFT);
        assertThat(backToDraft.getId()).isEqualTo(rejected.getId());
    }

    @Test
    void onlyOwnerOrAdminCanEditDraft() {
        Design design = designService.createDesign(creator, "Geisterclown", "Ein gruseliger Clown.");

        assertThatThrownBy(() -> designService.updateDraftContent(otherCreator, design, "Hack", "Hack"))
                .isInstanceOf(ApiException.class);

        // Admin darf trotzdem
        DesignVersion updated = designService.updateDraftContent(admin, design, "Geisterclown", "Angepasst durch Admin");
        assertThat(updated.getDescription()).isEqualTo("Angepasst durch Admin");
    }

    @Test
    void forkRequiresPublishedSourceAndCopiesContent() {
        Design source = designService.createDesign(creator, "Vampirmaske", "Klassisch gruselig.");

        assertThatThrownBy(() -> designService.forkDesign(otherCreator, source.getId()))
                .isInstanceOf(ApiException.class);

        designService.addImage(creator, source, fakeImage());
        designService.submitForReview(creator, source);
        designService.approve(admin, source);

        Design fork = designService.forkDesign(otherCreator, source.getId());
        DesignVersion forkDraft = designService.findInFlightVersion(fork);

        assertThat(fork.getForkedFrom()).isEqualTo(source);
        assertThat(forkDraft.getName()).isEqualTo("Vampirmaske");
        assertThat(forkDraft.getImages()).hasSize(1);
        assertThat(designService.listPublishedForks(source)).isEmpty();

        designService.submitForReview(otherCreator, fork);
        designService.approve(admin, fork);
        assertThat(designService.listPublishedForks(source)).containsExactly(fork);
    }
}
