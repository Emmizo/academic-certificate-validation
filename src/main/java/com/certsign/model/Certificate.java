// SDLC Phase: Implementation
// Component: Certificate Model
// Requirements covered: FR-02, FR-03, FR-04, FR-05, FR-07, FR-08, FR-09, NFR-03
// Description: Stores issued certificates with cryptographic hash and digital signature
package com.certsign.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "certificates",
        uniqueConstraints = {@UniqueConstraint(name = "uk_certificates_certificate_id", columnNames = {"certificate_id"})}
)
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_id", nullable = false, length = 50)
    private String certificateId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    /**
     * Snapshot field representing the student identifier as printed on the certificate.
     * For relational lookups use the {@link #student} association.
     */
    @Column(name = "student_id", nullable = false, length = 50)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "student_ref_id")
    private Student student;

    @Column(nullable = false, length = 200)
    private String degree;

    @Column(nullable = false, length = 200)
    private String institution;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "document_hash", nullable = false, length = 512)
    private String documentHash;

    @Column(name = "digital_signature", nullable = false, columnDefinition = "TEXT")
    private String digitalSignature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "key_pair_id", nullable = false)
    private KeyPair keyPair;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private User issuedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private CertificateApprovalStatus approvalStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (approvalStatus == null) {
            approvalStatus = CertificateApprovalStatus.PENDING_APPROVAL;
        }
    }
}

