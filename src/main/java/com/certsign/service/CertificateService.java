// SDLC Phase: Implementation
// Component: CertificateService
// Requirements covered: FR-02, FR-03, FR-04, FR-05, FR-06, FR-07, FR-08, FR-09, FR-10, NFR-03, NFR-05
// Description: Issues signed certificates and verifies certificate authenticity
package com.certsign.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.certsign.dto.CertificateRequest;
import com.certsign.dto.VerificationResult;
import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import com.certsign.model.KeyPair;
import com.certsign.model.Program;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.model.VerificationLog;
import com.certsign.repository.CertificateRepository;
import com.certsign.repository.KeyPairRepository;
import com.certsign.repository.ProgramRepository;
import com.certsign.repository.StudentRepository;
import com.certsign.repository.VerificationLogRepository;

@Service
public class CertificateService {
    public static final String DEFAULT_INSTITUTION = "IPRC Tumba-RP";

    private final CryptoService cryptoService;
    private final CertificateRepository certificateRepository;
    private final KeyPairRepository keyPairRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a new service for issuing certificates and verifying their authenticity.
     */
    public CertificateService(
            CryptoService cryptoService,
            CertificateRepository certificateRepository,
            KeyPairRepository keyPairRepository,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            VerificationLogRepository verificationLogRepository
    ) {
        this.cryptoService = cryptoService;
        this.certificateRepository = certificateRepository;
        this.keyPairRepository = keyPairRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.verificationLogRepository = verificationLogRepository;
    }

    @Transactional
    /**
     * Issues a new digitally‑signed certificate for the given request and issuing user.
     * <p>
     * Workflow:
     * <ul>
     *   <li>Find the active RSA key pair (RSA‑2048) to be used for signing.</li>
     *   <li>Load the referenced student and copy snapshot details onto the certificate.</li>
     *   <li>Build a canonical string and hash it with SHA‑256.</li>
     *   <li>Persist the unsigned draft until the Principal approves/signs it.</li>
     * </ul>
     */
    public Certificate issueCertificate(CertificateRequest req, User issuedBy) {
        KeyPair activeKeyPair = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("No active RSA key pair found. Generate keys first."));

        if (req.getStudentRefId() == null) {
            throw new IllegalArgumentException("Student selection is required");
        }

        Student student = studentRepository.findById(req.getStudentRefId())
                .orElseThrow(() -> new IllegalArgumentException("Selected student not found"));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new IllegalArgumentException("Selected student is not active");
        }

        Program program = programRepository.findByNameIgnoreCaseAndActiveTrue(req.getDegree().trim())
                .orElseThrow(() -> new IllegalArgumentException("Selected program not found or inactive"));

        Certificate cert = Certificate.builder()
                .certificateId(generateCertificateId())
                .studentName(student.getFullName())
                .studentId(student.getStudentNumber())
                .student(student)
                .degree(req.getDegree())
                .program(program)
                .licenceType(program.getLicenceType())
                .institution(DEFAULT_INSTITUTION)
                .issueDate(req.getIssueDate())
                .keyPair(activeKeyPair)
                .issuedBy(issuedBy)
                .approvalStatus(CertificateApprovalStatus.PENDING_APPROVAL)
                .submittedForApproval(false)
                .build();

        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);

        cert.setDocumentHash(hash);
        cert.setDigitalSignature("");

        return certificateRepository.save(cert);
    }

    @Transactional
    /**
     * Updates an existing certificate's core fields and returns it to unsigned draft status.
     * Any changes are reflected in a fresh document hash; the Principal must approve/sign
     * again before the certificate is cryptographically valid.
     */
    public Certificate updateCertificate(Long certificateId, CertificateRequest req) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        // Optionally update the linked student and snapshot fields if a new student is selected
        if (req.getStudentRefId() != null) {
            Student student = studentRepository.findById(req.getStudentRefId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected student not found"));
            if (student.getStatus() != StudentStatus.ACTIVE) {
                throw new IllegalArgumentException("Selected student is not active");
            }
            cert.setStudent(student);
            cert.setStudentName(student.getFullName());
            cert.setStudentId(student.getStudentNumber());
        }

        if (req.getDegree() != null) {
            cert.setDegree(req.getDegree());
            programRepository.findByNameIgnoreCaseAndActiveTrue(req.getDegree().trim())
                    .ifPresent(program -> {
                        cert.setProgram(program);
                        cert.setLicenceType(program.getLicenceType());
                    });
        }
        cert.setInstitution(DEFAULT_INSTITUTION);
        if (req.getIssueDate() != null) {
            cert.setIssueDate(req.getIssueDate());
        }

        if (cert.getKeyPair() == null) {
            throw new IllegalStateException("Certificate has no associated key pair for re-signing");
        }

        cert.setApprovalStatus(CertificateApprovalStatus.PENDING_APPROVAL);
        cert.setSubmittedForApproval(false);
        cert.setApprovedBy(null);
        cert.setApprovedAt(null);
        cert.setSentBy(null);
        cert.setSentAt(null);

        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);

        cert.setDocumentHash(hash);
        cert.setDigitalSignature("");

        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate approveCertificate(Long certificateId, User approver) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        if (cert.getApprovalStatus() != CertificateApprovalStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only certificate drafts pending Principal approval can be signed");
        }
        if (!cert.isSubmittedForApproval()) {
            throw new IllegalStateException("Certificate draft must be sent to Principal before signing");
        }

        if (cert.getKeyPair() == null) {
            throw new IllegalStateException("Certificate has no associated key pair for signing");
        }

        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        String signature = cryptoService.signData(hash, cert.getKeyPair().getPrivateKeyEncrypted());

        cert.setDocumentHash(hash);
        cert.setDigitalSignature(signature);
        cert.setApprovalStatus(CertificateApprovalStatus.APPROVED);
        cert.setApprovedBy(approver);
        cert.setApprovedAt(LocalDateTime.now());
        cert.setRejectionReason(null);
        return certificateRepository.save(cert);
    }

    @Transactional
    public Certificate rejectCertificate(Long certificateId, User rejectedBy, String reason) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (!cert.isSubmittedForApproval()) {
            throw new IllegalStateException("Certificate draft must be sent to Principal before rejection");
        }
        cert.setApprovalStatus(CertificateApprovalStatus.REJECTED);
        cert.setApprovedBy(rejectedBy);
        cert.setApprovedAt(LocalDateTime.now());
        cert.setRejectionReason(reason);
        return certificateRepository.save(cert);
    }

    @Transactional
    /**
     * Records that the secretary (or admin) delivered the certificate PDF to the student.
     * Allowed even when the principal has not yet signed (PENDING_APPROVAL).
     */
    public Certificate sendCertificateToStudent(Long certificateId, User sender) {
        Certificate cert = certificateRepository.findByIdWithDetails(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (cert.getApprovalStatus() == CertificateApprovalStatus.REJECTED) {
            throw new IllegalStateException("Cannot send a rejected certificate");
        }
        if (cert.getStudent() == null || cert.getStudent().getEmail() == null
                || cert.getStudent().getEmail().trim().isEmpty()) {
            throw new IllegalStateException("Student email is missing");
        }
        cert.setSentBy(sender);
        cert.setSentAt(LocalDateTime.now());
        return certificateRepository.save(cert);
    }

    @Transactional
    /**
     * Verifies a certificate by ID using both the stored SHA‑256 hash and RSA signature.
     * <p>
     * It rebuilds the canonical string, recomputes the SHA‑256 hash, verifies the RSA
     * signature (SHA256withRSA) using the stored public key, logs the attempt, and
     * returns a structured {@link VerificationResult}.
     */
    public VerificationResult verifyCertificate(String certificateId, String verifierIp) {
        LocalDateTime verifiedAt = LocalDateTime.now();

        Optional<Certificate> certOpt = certificateRepository.findByCertificateId(certificateId);
        if (certOpt.isEmpty()) {
            log(null, certificateId, verifierIp, false, "Certificate not found", verifiedAt);
            return VerificationResult.builder()
                    .valid(false)
                    .message("INVALID")
                    .failureReason("Certificate not found")
                    .verifiedAt(verifiedAt)
                    .certificate(null)
                    .build();
        }

        Certificate cert = certOpt.get();

        if (cert.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL) {
            log(cert, certificateId, verifierIp, false, "Certificate pending admin approval", verifiedAt);
            return VerificationResult.builder()
                    .valid(false)
                    .message("PENDING_APPROVAL")
                    .failureReason("Certificate pending admin approval")
                    .verifiedAt(verifiedAt)
                    .certificate(null)
                    .build();
        }

        String canonical = cryptoService.buildCanonicalString(cert);
        String rebuiltHash = cryptoService.hashWithSHA256(canonical);
        if (!rebuiltHash.equalsIgnoreCase(cert.getDocumentHash())) {
            log(cert, certificateId, verifierIp, false, "Document tampered", verifiedAt);
            return VerificationResult.builder()
                    .valid(false)
                    .message("INVALID")
                    .failureReason("Document tampered")
                    .verifiedAt(verifiedAt)
                    .certificate(null)
                    .build();
        }

        boolean ok = cryptoService.verifySignature(rebuiltHash, cert.getDigitalSignature(), cert.getKeyPair().getPublicKey());
        if (!ok) {
            log(cert, certificateId, verifierIp, false, "Signature verification failed", verifiedAt);
            return VerificationResult.builder()
                    .valid(false)
                    .message("INVALID")
                    .failureReason("Signature verification failed")
                    .verifiedAt(verifiedAt)
                    .certificate(null)
                    .build();
        }

        log(cert, certificateId, verifierIp, true, null, verifiedAt);
        return VerificationResult.builder()
                .valid(true)
                .message("VALID")
                .failureReason(null)
                .verifiedAt(verifiedAt)
                .certificate(cert)
                .build();
    }

    /**
     * Persists a verification attempt to the audit log, truncating overly long fields
     * so they fit within database column limits.
     */
    private void log(Certificate certificate, String certificateId, String verifierIp, boolean result, String failureReason, LocalDateTime verifiedAt) {
        String cid = certificateId == null ? "" : certificateId;
        if (cid.length() > 50) {
            cid = cid.substring(0, 50);
        }

        String ip = verifierIp == null ? null : verifierIp;
        if (ip != null && ip.length() > 50) {
            ip = ip.substring(0, 50);
        }

        verificationLogRepository.save(VerificationLog.builder()
                .certificate(certificate)
                .certificateId(cid)
                .verifierIp(ip)
                .result(result)
                .failureReason(failureReason)
                .verifiedAt(verifiedAt)
                .build());
    }

    /**
     * Generates a random human‑readable certificate ID of the form {@code CERT-XXXXXXXX}.
     */
    private String generateCertificateId() {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("CERT-");
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(secureRandom.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
