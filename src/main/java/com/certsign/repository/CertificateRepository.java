// SDLC Phase: Implementation
// Component: CertificateRepository
// Requirements covered: FR-05, FR-06
// Description: Provides persistence operations for certificates
package com.certsign.repository;

import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import java.time.LocalDateTime;
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

    @Query("""
            SELECT c FROM Certificate c
            ORDER BY
                CASE
                    WHEN c.approvalStatus = com.certsign.model.CertificateApprovalStatus.PENDING_APPROVAL THEN 0
                    WHEN c.approvalStatus = com.certsign.model.CertificateApprovalStatus.APPROVED THEN 1
                    ELSE 2
                END,
                c.issueDate ASC,
                c.createdAt ASC
            """)
    List<Certificate> findAllForApprovalQueue();

    List<Certificate> findTop3ByOrderByCreatedAtDesc();

    Optional<Certificate> findFirstByOrderByCreatedAtDesc();

    long countByApprovalStatus(CertificateApprovalStatus approvalStatus);

    long countByApprovalStatusAndSubmittedForApprovalTrue(CertificateApprovalStatus approvalStatus);

    long countByStudent_Id(Long studentId);

    List<Certificate> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    List<Certificate> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
