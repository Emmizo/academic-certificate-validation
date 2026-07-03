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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByCertificateId(String certificateId);

    @Query("""
            SELECT c FROM Certificate c
            JOIN FETCH c.student
            LEFT JOIN FETCH c.program p
            LEFT JOIN FETCH p.licenceType
            LEFT JOIN FETCH c.licenceType
            WHERE c.id = :id
            """)
    Optional<Certificate> findByIdWithDetails(@Param("id") Long id);

    List<Certificate> findAllByOrderByCreatedAtDesc();

    List<Certificate> findTop3ByOrderByCreatedAtDesc();

    Optional<Certificate> findFirstByOrderByCreatedAtDesc();

    long countByApprovalStatus(CertificateApprovalStatus approvalStatus);

    long countByStudent_Id(Long studentId);

    List<Certificate> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
}

