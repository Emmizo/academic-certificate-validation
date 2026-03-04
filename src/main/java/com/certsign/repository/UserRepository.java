// SDLC Phase: Implementation
// Component: UserRepository
// Requirements covered: NFR-04
// Description: Provides persistence operations for users
package com.certsign.repository;

import com.certsign.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

