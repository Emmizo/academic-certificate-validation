package com.certsign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.certsign.dto.CertificateRequest;
import com.certsign.dto.VerificationResult;
import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import com.certsign.model.KeyPair;
import com.certsign.model.Program;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.repository.CertificateRepository;
import com.certsign.repository.KeyPairRepository;
import com.certsign.repository.ProgramRepository;
import com.certsign.repository.StudentRepository;
import com.certsign.repository.VerificationLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CertificateService} that exercise the main
 * issue and verify flows without hitting the real database.
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private KeyPairRepository keyPairRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private VerificationLogRepository verificationLogRepository;

    private CryptoService cryptoService;

    @InjectMocks
    private CertificateService certificateService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
        certificateService = new CertificateService(
                cryptoService,
                certificateRepository,
                keyPairRepository,
                studentRepository,
                programRepository,
                verificationLogRepository
        );
    }

    @Test
    void issueCertificate_shouldGenerateHashButNoSignatureUntilApproval() {
        // Arrange: active key pair and student
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(1L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Student student = Student.builder()
                .id(10L)
                .studentNumber("STU-001")
                .fullName("Test Student")
                .status(StudentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User issuer = User.builder()
                .id(5L)
                .username("admin")
                .build();

        CertificateRequest req = new CertificateRequest();
        req.setStudentRefId(student.getId());
        req.setDegree("BSc Computer Science");
        req.setInstitution("CertSign University");
        req.setIssueDate(LocalDate.now());

        when(keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(kp));
        when(studentRepository.findById(student.getId()))
                .thenReturn(Optional.of(student));
        when(programRepository.findByNameIgnoreCaseAndActiveTrue("BSc Computer Science"))
                .thenReturn(Optional.of(Program.builder()
                        .id(1L)
                        .name("BSc Computer Science")
                        .active(true)
                        .build()));
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Certificate.class));

        // Act
        Certificate issued = certificateService.issueCertificate(req, issuer);

        // Assert
        assertThat(issued.getDocumentHash()).isNotBlank();
        assertThat(issued.getDigitalSignature()).isEmpty();
        assertThat(issued.getApprovalStatus()).isEqualTo(CertificateApprovalStatus.PENDING_APPROVAL);
        assertThat(issued.getKeyPair()).isEqualTo(kp);
        assertThat(issued.getStudent()).isEqualTo(student);
        assertThat(issued.getInstitution()).isEqualTo(CertificateService.DEFAULT_INSTITUTION);
        assertThat(issued.isSubmittedForApproval()).isFalse();
    }

    @Test
    void issueCertificate_shouldRejectInactiveStudent() {
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(1L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Student inactiveStudent = Student.builder()
                .id(11L)
                .studentNumber("STU-INACTIVE")
                .fullName("Inactive Student")
                .status(StudentStatus.INACTIVE)
                .build();

        CertificateRequest req = new CertificateRequest();
        req.setStudentRefId(inactiveStudent.getId());
        req.setDegree("BSc Computer Science");
        req.setInstitution("CertSign University");
        req.setIssueDate(LocalDate.now());

        when(keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(kp));
        when(studentRepository.findById(inactiveStudent.getId()))
                .thenReturn(Optional.of(inactiveStudent));

        assertThatThrownBy(() -> certificateService.issueCertificate(req, User.builder().id(5L).username("admin").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected student is not active");
    }

    @Test
    void approveCertificate_shouldGenerateDigitalSignature() {
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(1L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Certificate cert = Certificate.builder()
                .id(42L)
                .certificateId("CERT-DRAFT")
                .studentName("Draft Student")
                .studentId("STU-001")
                .degree("BSc Computer Science")
                .institution("CertSign University")
                .issueDate(LocalDate.now())
                .keyPair(kp)
                .issuedBy(User.builder().id(5L).username("admin").build())
                .approvalStatus(CertificateApprovalStatus.PENDING_APPROVAL)
                .submittedForApproval(true)
                .digitalSignature("")
                .createdAt(LocalDateTime.now())
                .build();
        cert.setDocumentHash(cryptoService.hashWithSHA256(cryptoService.buildCanonicalString(cert)));

        User principal = User.builder().id(6L).username("principal").build();

        when(certificateRepository.findById(cert.getId()))
                .thenReturn(Optional.of(cert));
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Certificate.class));

        Certificate approved = certificateService.approveCertificate(cert.getId(), principal);

        assertThat(approved.getApprovalStatus()).isEqualTo(CertificateApprovalStatus.APPROVED);
        assertThat(approved.getApprovedBy()).isEqualTo(principal);
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getDigitalSignature()).isNotBlank();
        assertThat(cryptoService.verifySignature(
                approved.getDocumentHash(),
                approved.getDigitalSignature(),
                kp.getPublicKey()
        )).isTrue();
    }

    @Test
    void approveCertificate_shouldRejectCertificateThatIsNotPending() {
        Certificate cert = Certificate.builder()
                .id(52L)
                .certificateId("CERT-APPROVED")
                .studentName("Approved Student")
                .studentId("STU-APPROVED")
                .degree("BSc Computer Science")
                .institution("CertSign University")
                .issueDate(LocalDate.now())
                .keyPair(KeyPair.builder().id(1L).build())
                .issuedBy(User.builder().id(5L).username("admin").build())
                .approvalStatus(CertificateApprovalStatus.APPROVED)
                .digitalSignature("already-signed")
                .createdAt(LocalDateTime.now())
                .build();

        when(certificateRepository.findById(cert.getId()))
                .thenReturn(Optional.of(cert));

        assertThatThrownBy(() -> certificateService.approveCertificate(
                cert.getId(),
                User.builder().id(6L).username("principal").build()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only certificate drafts pending Principal approval can be signed");
    }

    @Test
    void approveCertificate_shouldRejectDraftThatWasNotSentToPrincipal() {
        Certificate cert = Certificate.builder()
                .id(53L)
                .certificateId("CERT-UNSUBMITTED")
                .studentName("Draft Student")
                .studentId("STU-DRAFT")
                .degree("BSc Computer Science")
                .institution("CertSign University")
                .issueDate(LocalDate.now())
                .keyPair(KeyPair.builder().id(1L).build())
                .issuedBy(User.builder().id(5L).username("admin").build())
                .approvalStatus(CertificateApprovalStatus.PENDING_APPROVAL)
                .submittedForApproval(false)
                .digitalSignature("")
                .createdAt(LocalDateTime.now())
                .build();

        when(certificateRepository.findById(cert.getId()))
                .thenReturn(Optional.of(cert));

        assertThatThrownBy(() -> certificateService.approveCertificate(
                cert.getId(),
                User.builder().id(6L).username("principal").build()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Certificate draft must be sent to Principal before signing");
    }

    @Test
    void verifyCertificate_shouldReturnValidForUntamperedCertificate() {
        // Arrange: build a certificate that has already been issued correctly
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(1L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Certificate cert = Certificate.builder()
                .id(42L)
                .certificateId("CERT-TEST1234")
                .studentName("Valid Student")
                .studentId("STU-002")
                .degree("BSc Computer Science")
                .institution("CertSign University")
                .issueDate(LocalDate.now())
                .keyPair(kp)
                .issuedBy(User.builder().id(5L).username("admin").build())
                .createdAt(LocalDateTime.now())
                .build();

        // Compute hash and signature exactly as in production
        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        String signature = cryptoService.signData(hash, kp.getPrivateKeyEncrypted());
        cert.setDocumentHash(hash);
        cert.setDigitalSignature(signature);

        when(certificateRepository.findByCertificateId("CERT-TEST1234"))
                .thenReturn(Optional.of(cert));

        // Act
        VerificationResult result = certificateService.verifyCertificate("CERT-TEST1234", "127.0.0.1");

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getFailureReason()).isNull();
        assertThat(result.getCertificate()).isNotNull();
    }

    @Test
    void verifyCertificate_shouldReturnInvalidWhenDataTampered() {
        // Arrange: certificate where the stored hash does not match the recomputed canonical hash
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(1L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Certificate cert = Certificate.builder()
                .id(43L)
                .certificateId("CERT-TAMPERED")
                .studentName("Tampered Student")
                .studentId("STU-003")
                .degree("BSc Computer Science")
                .institution("Original University")
                .issueDate(LocalDate.now())
                .keyPair(kp)
                .issuedBy(User.builder().id(5L).username("admin").build())
                .createdAt(LocalDateTime.now())
                .build();

        // Correct hash + signature for original content
        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        String signature = cryptoService.signData(hash, kp.getPrivateKeyEncrypted());

        // Simulate DB tampering: keep signature, but change stored hash
        cert.setDocumentHash(hash.substring(0, hash.length() - 1) + "0");
        cert.setDigitalSignature(signature);

        when(certificateRepository.findByCertificateId("CERT-TAMPERED"))
                .thenReturn(Optional.of(cert));

        // Act
        VerificationResult result = certificateService.verifyCertificate("CERT-TAMPERED", "127.0.0.1");

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Document tampered");
        assertThat(result.getCertificate()).isNull();
    }

    @Test
    void verifyCertificate_shouldReturnInvalidWhenNameChangedInDatabase() {
        // Arrange: issue a valid certificate, then simulate direct DB edit of the name only
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(2L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Certificate cert = Certificate.builder()
                .id(44L)
                .certificateId("CERT-NAME-TAMPER")
                .studentName("Original Name")
                .studentId("STU-004")
                .degree("BSc Computer Science")
                .institution("CertSign University")
                .issueDate(LocalDate.now())
                .keyPair(kp)
                .issuedBy(User.builder().id(6L).username("issuer").build())
                .createdAt(LocalDateTime.now())
                .build();

        // Compute correct hash and signature for original content
        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        String signature = cryptoService.signData(hash, kp.getPrivateKeyEncrypted());
        cert.setDocumentHash(hash);
        cert.setDigitalSignature(signature);

        // Simulate DB tampering: change only the name column while leaving hash + signature untouched
        cert.setStudentName("Original Name Jr.");

        when(certificateRepository.findByCertificateId("CERT-NAME-TAMPER"))
                .thenReturn(Optional.of(cert));

        // Act
        VerificationResult result = certificateService.verifyCertificate("CERT-NAME-TAMPER", "127.0.0.1");

        // Assert: any change to the name field makes the certificate invalid
        assertThat(result.isValid()).isFalse();
        assertThat(result.getFailureReason()).isEqualTo("Document tampered");
        assertThat(result.getCertificate()).isNull();
    }

    @Test
    void updateCertificate_shouldReturnToUnsignedDraftWhenFieldsChange() {
        // Arrange: start from a valid, issued certificate
        var keyMap = cryptoService.generateRSAKeyPair();
        KeyPair kp = KeyPair.builder()
                .id(3L)
                .publicKey(keyMap.get("publicKey"))
                .privateKeyEncrypted(keyMap.get("privateKey"))
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        Student student = Student.builder()
                .id(20L)
                .studentNumber("STU-UPDATE-1")
                .fullName("Original Name")
                .status(StudentStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Certificate cert = Certificate.builder()
                .id(50L)
                .certificateId("CERT-UPDATE-1")
                .studentName(student.getFullName())
                .studentId(student.getStudentNumber())
                .student(student)
                .degree("Old Degree")
                .institution("Old Institution")
                .issueDate(LocalDate.now().minusDays(1))
                .keyPair(kp)
                .issuedBy(User.builder().id(7L).username("issuer").build())
                .createdAt(LocalDateTime.now())
                .build();

        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        String signature = cryptoService.signData(hash, kp.getPrivateKeyEncrypted());
        cert.setDocumentHash(hash);
        cert.setDigitalSignature(signature);

        when(certificateRepository.findById(50L)).thenReturn(Optional.of(cert));
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Certificate.class));
        CertificateRequest updateReq = new CertificateRequest();
        updateReq.setStudentRefId(student.getId());
        updateReq.setDegree("New Degree");
        updateReq.setInstitution("New Institution");
        updateReq.setIssueDate(LocalDate.now());

        // Act: update returns the certificate to unsigned draft status
        Certificate updated = certificateService.updateCertificate(50L, updateReq);

        // Assert: hash is regenerated, signature is cleared until Principal approval
        assertThat(updated.getDegree()).isEqualTo("New Degree");
        assertThat(updated.getInstitution()).isEqualTo(CertificateService.DEFAULT_INSTITUTION);
        assertThat(updated.getDocumentHash()).isNotEqualTo(hash);
        assertThat(updated.getDigitalSignature()).isEmpty();
        assertThat(updated.getApprovalStatus()).isEqualTo(CertificateApprovalStatus.PENDING_APPROVAL);
        assertThat(updated.isSubmittedForApproval()).isFalse();
        assertThat(updated.getApprovedBy()).isNull();
        assertThat(updated.getApprovedAt()).isNull();
    }
}
