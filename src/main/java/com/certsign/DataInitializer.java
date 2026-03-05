// SDLC Phase: Implementation
// Component: DataInitializer
// Requirements covered: NFR-02, NFR-03, NFR-04
// Description: Seeds default admin user on first application startup
package com.certsign;

import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean createdAny = false;

        // Ensure admin exists (based on configured username)
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = User.builder()
                    .username(adminUsername)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role(UserRole.ADMIN)
                    .build();
            userRepository.save(admin);
            createdAny = true;
            System.out.println("Created default ADMIN user: username=" + adminUsername + " password=" + adminPassword);
        }

        // Ensure signer exists
        if (userRepository.findByUsername("signer").isEmpty()) {
            User signer = User.builder()
                    .username("signer")
                    .passwordHash(passwordEncoder.encode("Signer@123"))
                    .role(UserRole.SIGNER)
                    .build();
            userRepository.save(signer);
            createdAny = true;
            System.out.println("Created default SIGNER user: username=signer password=Signer@123");
        }

        // Ensure verifier exists
        if (userRepository.findByUsername("verifier").isEmpty()) {
            User verifier = User.builder()
                    .username("verifier")
                    .passwordHash(passwordEncoder.encode("Verifier@123"))
                    .role(UserRole.VERIFIER)
                    .build();
            userRepository.save(verifier);
            createdAny = true;
            System.out.println("Created default VERIFIER user: username=verifier password=Verifier@123");
        }

        if (!createdAny) {
            System.out.println("Default users already present; no seeding required.");
        }
    }
}

