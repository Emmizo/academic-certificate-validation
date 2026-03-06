package com.certsign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.certsign.dto.CertificateRequest;
import com.certsign.dto.VerificationResult;
import com.certsign.model.Certificate;
import com.certsign.model.KeyPair;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.repository.CertificateRepository;
import com.certsign.repository.KeyPairRepository;
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
                verificationLogRepository
        );
    }

    @Test
    void issueCertificate_shouldGenerateHashAndSignature() {
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
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Certificate.class));

        // Act
        Certificate issued = certificateService.issueCertificate(req, issuer);

        // Assert
        assertThat(issued.getDocumentHash()).isNotBlank();
        assertThat(issued.getDigitalSignature()).isNotBlank();
        assertThat(issued.getKeyPair()).isEqualTo(kp);
        assertThat(issued.getStudent()).isEqualTo(student);
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
    void updateCertificate_shouldReSignWhenFieldsChange() {
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

        // Act: update + re-sign
        Certificate updated = certificateService.updateCertificate(50L, updateReq);

        // Assert: hash and signature are regenerated and certificate still verifies
        assertThat(updated.getDegree()).isEqualTo("New Degree");
        assertThat(updated.getInstitution()).isEqualTo("New Institution");
        assertThat(updated.getDocumentHash()).isNotEqualTo(hash);
        assertThat(updated.getDigitalSignature()).isNotEqualTo(signature);

        VerificationResult result = certificateService.verifyCertificate("CERT-UPDATE-1", "127.0.0.1");
        assertThat(result.isValid()).isTrue();
    }
}

