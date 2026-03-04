// SDLC Phase: Implementation
// Component: UserService
// Requirements covered: NFR-02, NFR-04
// Description: Loads users from database for Spring Security authentication
package com.certsign.service;

import com.certsign.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Creates a user service that reads user accounts from the database.
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    /**
     * Loads a user from the database and adapts it to Spring Security's {@link UserDetails}.
     * <p>
     * The stored role enum is exposed as a {@code ROLE_...} authority for access control.
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}

