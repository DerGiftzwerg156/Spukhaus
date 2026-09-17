package de.spukhaus.backend.security;

import de.spukhaus.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SpukhausUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SpukhausUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(SpukhausUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer: " + username));
    }

    public UserDetails loadUserById(Long id) {
        return userRepository.findById(id)
                .map(SpukhausUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer: " + id));
    }
}
