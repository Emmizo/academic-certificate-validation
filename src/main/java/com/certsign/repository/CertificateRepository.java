// SDLC Phase: Implementation
// Component: CertificateRepository
// Requirements covered: FR-05, FR-06
// Description: Provides persistence operations for certificates
package com.certsign.repository;

import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByCertificateId(String certificateId);

    List<Certificate> findAllByOrderByCreatedAtDesc();

    List<Certificate> findTop3ByOrderByCreatedAtDesc();

    Optional<Certificate> findFirstByOrderByCreatedAtDesc();

    long countByApprovalStatus(CertificateApprovalStatus approvalStatus);
}

