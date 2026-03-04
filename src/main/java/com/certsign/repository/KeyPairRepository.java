// SDLC Phase: Implementation
// Component: KeyPairRepository
// Requirements covered: FR-01, NFR-05
// Description: Provides persistence operations for RSA key pairs
package com.certsign.repository;

import com.certsign.model.KeyPair;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeyPairRepository extends JpaRepository<KeyPair, Long> {
    Optional<KeyPair> findFirstByActiveTrueOrderByCreatedAtDesc();
}

