package de.spukhaus.backend.config;

import de.spukhaus.backend.domain.Role;
import de.spukhaus.backend.domain.User;
import de.spukhaus.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Legt beim ersten Start einen initialen TechAdmin-Account an, sofern noch keine
 * Benutzer existieren. Löst das "Henne-Ei"-Problem, da sonst niemand Accounts anlegen könnte.
 */
@Component
public class TechAdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TechAdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    public TechAdminBootstrapRunner(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                     BootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (properties.getTechAdminPassword() == null || properties.getTechAdminPassword().isBlank()) {
            log.warn("Keine Benutzer vorhanden und BOOTSTRAP_TECH_ADMIN_PASSWORD ist nicht gesetzt. "
                    + "Es kann sich niemand anmelden, bis ein Benutzer angelegt wird.");
            return;
        }
        User techAdmin = new User(
                properties.getTechAdminUsername(),
                passwordEncoder.encode(properties.getTechAdminPassword()),
                properties.getTechAdminDisplayName(),
                Role.TECH_ADMIN);
        techAdmin.setMustChangePassword(true);
        userRepository.save(techAdmin);
        log.info("Initialer TechAdmin-Account '{}' wurde angelegt.", properties.getTechAdminUsername());
    }
}
