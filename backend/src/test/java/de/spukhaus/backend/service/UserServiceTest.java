package de.spukhaus.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.spukhaus.backend.domain.Role;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.repository.UserRepository;
import de.spukhaus.backend.web.dto.CreateUserRequest;
import de.spukhaus.backend.web.dto.UpdateUserRequest;
import de.spukhaus.backend.web.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User techAdmin;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(new User("admin", passwordEncoder.encode("pw"), "Admin", Role.ADMIN));
        techAdmin = userRepository.save(new User("techadmin", passwordEncoder.encode("pw"), "Tech Admin", Role.TECH_ADMIN));
    }

    @Test
    void plainAdminCannotCreateTechAdmin() {
        var request = new CreateUserRequest("neuertech", "Neuer Tech", Role.TECH_ADMIN);
        assertThatThrownBy(() -> userService.createUser(admin, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void techAdminCanCreateTechAdmin() {
        var request = new CreateUserRequest("neuertech", "Neuer Tech", Role.TECH_ADMIN);
        var created = userService.createUser(techAdmin, request);
        assertThat(created.user().getRole()).isEqualTo(Role.TECH_ADMIN);
    }

    @Test
    void plainAdminCannotModifyTechAdminAccount() {
        var request = new UpdateUserRequest("Neuer Name", null, null);
        assertThatThrownBy(() -> userService.updateUser(admin, techAdmin.getId(), request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void plainAdminCannotPromoteSomeoneToTechAdmin() {
        User creator = userRepository.save(new User("creator", passwordEncoder.encode("pw"), "Creator", Role.CREATOR));
        var request = new UpdateUserRequest(null, Role.TECH_ADMIN, null);
        assertThatThrownBy(() -> userService.updateUser(admin, creator.getId(), request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void techAdminCanModifyOtherTechAdminAccount() {
        var request = new UpdateUserRequest("Umbenannt", null, null);
        User updated = userService.updateUser(techAdmin, techAdmin.getId(), request);
        assertThat(updated.getDisplayName()).isEqualTo("Umbenannt");
    }

    @Test
    void plainAdminCannotResetTechAdminPassword() {
        assertThatThrownBy(() -> userService.resetPassword(admin, techAdmin.getId()))
                .isInstanceOf(ApiException.class);
    }
}
