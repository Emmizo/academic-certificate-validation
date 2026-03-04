// SDLC Phase: Implementation
// Component: VerificationLogRepository
// Requirements covered: FR-10
// Description: Provides persistence operations for verification logs
package com.certsign.repository;

import com.certsign.model.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationLogRepository extends JpaRepository<VerificationLog, Long> {}

