package de.spukhaus.backend.service;

import de.spukhaus.backend.domain.Role;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.repository.UserRepository;
import de.spukhaus.backend.web.dto.CreateUserRequest;
import de.spukhaus.backend.web.dto.UpdateUserRequest;
import de.spukhaus.backend.web.exception.ApiException;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Benutzer nicht gefunden."));
    }

    public record CreatedUser(User user, String temporaryPassword) {
    }

    public CreatedUser createUser(User actor, CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw ApiException.conflict("Benutzername bereits vergeben.");
        }
        requireCanAssignRole(actor, request.role());
        String tempPassword = generateTemporaryPassword();
        User user = new User(request.username(), passwordEncoder.encode(tempPassword),
                request.displayName(), request.role());
        user.setMustChangePassword(true);
        userRepository.save(user);
        return new CreatedUser(user, tempPassword);
    }

    public User updateUser(User actor, Long id, UpdateUserRequest request) {
        User user = getById(id);
        requireCanManageTarget(actor, user);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }
        if (request.role() != null && request.role() != user.getRole()) {
            requireCanAssignRole(actor, request.role());
            guardNotLastTechAdmin(user, "die Rolle ändern");
            user.setRole(request.role());
        }
        if (request.enabled() != null && !request.enabled() && user.isEnabled()) {
            guardNotLastTechAdmin(user, "deaktivieren");
            user.setEnabled(false);
        } else if (request.enabled() != null && request.enabled()) {
            user.setEnabled(true);
        }
        return user;
    }

    public String resetPassword(User actor, Long id) {
        User user = getById(id);
        requireCanManageTarget(actor, user);
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        return tempPassword;
    }

    /** Nur ein TechAdmin darf die Rolle TechAdmin vergeben (Rollen-Hierarchie: Admin < TechAdmin). */
    private void requireCanAssignRole(User actor, Role role) {
        if (role == Role.TECH_ADMIN && actor.getRole() != Role.TECH_ADMIN) {
            throw ApiException.forbidden("Nur ein TechAdmin darf die Rolle TechAdmin vergeben.");
        }
    }

    /** Ein einfacher Admin darf keinen TechAdmin-Account verändern (Rollen-Hierarchie: Admin < TechAdmin). */
    private void requireCanManageTarget(User actor, User target) {
        if (target.getRole() == Role.TECH_ADMIN && actor.getRole() != Role.TECH_ADMIN) {
            throw ApiException.forbidden("Nur ein TechAdmin darf einen TechAdmin-Account verändern.");
        }
    }

    public void changeOwnPassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Aktuelles Passwort ist nicht korrekt.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
    }

    private void guardNotLastTechAdmin(User user, String action) {
        if (user.getRole() == Role.TECH_ADMIN && user.isEnabled()) {
            long activeTechAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.TECH_ADMIN && u.isEnabled())
                    .count();
            if (activeTechAdmins <= 1) {
                throw ApiException.badRequest("Der letzte aktive TechAdmin kann nicht " + action + " werden.");
            }
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
