// SDLC Phase: Implementation
// Component: UserRepository
// Requirements covered: NFR-04
// Description: Provides persistence operations for users
package com.certsign.repository;

import com.certsign.model.User;
import com.certsign.model.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    List<User> findByRoleAndEnabledTrue(UserRole role);
    boolean existsByRole(UserRole role);
}
