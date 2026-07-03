// SDLC Phase: Implementation
// Component: AdminController
// Requirements covered: FR-01, FR-02, FR-05, NFR-03, NFR-04, NFR-05
// Description: Handles protected admin pages for key management and certificate issuance
package com.certsign.controller;

import com.certsign.dto.CertificateRequest;
import com.certsign.model.KeyPair;
import com.certsign.model.CertificateApprovalStatus;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.ProgramRepository;
import com.certsign.repository.CertificateRepository;
import com.certsign.repository.KeyPairRepository;
import com.certsign.repository.StudentRepository;
import com.certsign.repository.UserRepository;
import com.certsign.service.CertificatePdfService;
import com.certsign.service.CertificateService;
import com.certsign.service.CryptoService;
import com.certsign.service.MailService;
import com.certsign.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {

    private final CertificateRepository certificateRepository;
    private final KeyPairRepository keyPairRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    private final CertificateService certificateService;
    private final CertificatePdfService certificatePdfService;
    private final MailService mailService;
    private final UserService userService;

    /**
     * Creates the admin controller that wires repositories and cryptographic services
     * used by the secured admin console.
     */
    public AdminController(
            CertificateRepository certificateRepository,
            KeyPairRepository keyPairRepository,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            UserRepository userRepository,
            CryptoService cryptoService,
            CertificateService certificateService,
            CertificatePdfService certificatePdfService,
            MailService mailService,
            UserService userService
    ) {
        this.certificateRepository = certificateRepository;
        this.keyPairRepository = keyPairRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.certificateService = certificateService;
        this.certificatePdfService = certificatePdfService;
        this.mailService = mailService;
        this.userService = userService;
    }

    /**
     * Renders the admin dashboard with high‑level stats, recent certificates,
     * and a 7‑day issuance trend chart.
     */
    @GetMapping("/admin/dashboard")
    public String dashboard(Authentication auth, Model model) {
        long totalCerts = certificateRepository.count();
        var activeKey = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc();
        var lastCert = certificateRepository.findFirstByOrderByCreatedAtDesc();
        var recentCerts = certificateRepository.findTop3ByOrderByCreatedAtDesc();

        // Build simple 7-day issuance trend for chart + stat
        LocalDate today = LocalDate.now();
        List<String> chartLabels = new ArrayList<>();
        List<Long> chartValues = new ArrayList<>();
        var allCerts = certificateRepository.findAllByOrderByCreatedAtDesc();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            chartLabels.add(day.toString());
            long countForDay = allCerts.stream()
                    .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().isEqual(day))
                    .count();
            chartValues.add(countForDay);
        }

        long certsLast7Days = chartValues.stream().mapToLong(Long::longValue).sum();

        model.addAttribute("username", auth.getName());
        model.addAttribute("totalCerts", totalCerts);
        model.addAttribute("activeKeyPresent", activeKey.isPresent());
        model.addAttribute("activeKeyCreatedAt", activeKey.map(KeyPair::getCreatedAt).orElse(null));
        model.addAttribute("lastCertificateDate", lastCert.map(c -> c.getCreatedAt().toLocalDate()).orElse(null));
        model.addAttribute("recentCertificates", recentCerts);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);
        model.addAttribute("certsLast7Days", certsLast7Days);
        return "admin/dashboard";
    }

    /**
     * Shows the RSA key management page with the currently active public key.
     */
    @GetMapping("/admin/keys")
    public String keys(Model model) {
        var activeKey = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc();
        model.addAttribute("activeKey", activeKey.orElse(null));
        return "admin/keys";
    }

    /**
     * Rotates the RSA key pair by deactivating the current one and generating a new
     * 2048‑bit RSA key pair for future certificate signing.
     */
    @PostMapping("/admin/keys/generate")
    @Transactional
    public String generateKeys() {
        keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc().ifPresent(kp -> {
            kp.setActive(false);
            keyPairRepository.save(kp);
        });

        Map<String, String> keys = cryptoService.generateRSAKeyPair();
        KeyPair newKp = KeyPair.builder()
                .publicKey(keys.get("publicKey"))
                .privateKeyEncrypted(keys.get("privateKey"))
                .active(true)
                .build();
        keyPairRepository.save(newKp);

        return "redirect:/admin/keys";
    }

    /**
     * Lists all issued certificates in reverse chronological order.
     */
    @GetMapping("/admin/certificates")
    public String certificates(Model model) {
        var certificates = certificateRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> validityById = new HashMap<>();
        for (var cert : certificates) {
            if (cert.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL) {
                validityById.put(cert.getId(), "PENDING_APPROVAL");
            } else {
                validityById.put(cert.getId(), isCertificateCryptographicallyValid(cert) ? "VALID" : "INVALID");
            }
        }
        model.addAttribute("certificates", certificates);
        model.addAttribute("validityById", validityById);
        return "admin/certificates";
    }

    /**
     * Displays the certificate issuance form, pre‑populated with today’s date and
     * the list of registered students.
     */
    @GetMapping("/admin/issue")
    public String issueForm(Model model) {
        CertificateRequest req = new CertificateRequest();
        req.setIssueDate(LocalDate.now());
        model.addAttribute("certificateRequest", req);
        model.addAttribute("error", null);
        model.addAttribute("students", loadActiveStudents());
        model.addAttribute("programs", programRepository.findByActiveTrueOrderByNameAsc());
        boolean hasActiveKey = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc().isPresent();
        model.addAttribute("hasActiveKey", hasActiveKey);
        return "admin/issue";
    }

    /**
     * Handles submission of the certificate issuance form and redirects to the
     * certificate detail page on success, or redisplays the form with validation
     * errors if any checks fail.
     */
    @PostMapping("/admin/issue")
    public String issueSubmit(
            @ModelAttribute CertificateRequest certificateRequest,
            Authentication auth,
            Model model
    ) {
        String err = validate(certificateRequest);
        if (err != null) {
            model.addAttribute("certificateRequest", certificateRequest);
            model.addAttribute("error", err);
            model.addAttribute("students", loadActiveStudents());
            model.addAttribute("programs", programRepository.findByActiveTrueOrderByNameAsc());
            boolean hasActiveKey = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc().isPresent();
            model.addAttribute("hasActiveKey", hasActiveKey);
            return "admin/issue";
        }

        User issuer = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        try {
            var saved = certificateService.issueCertificate(certificateRequest, issuer);
            return "redirect:/admin/certificates/" + saved.getId();
        } catch (IllegalStateException ex) {
            model.addAttribute("certificateRequest", certificateRequest);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("students", loadActiveStudents());
            model.addAttribute("programs", programRepository.findByActiveTrueOrderByNameAsc());
            boolean hasActiveKey = keyPairRepository.findFirstByActiveTrueOrderByCreatedAtDesc().isPresent();
            model.addAttribute("hasActiveKey", hasActiveKey);
            return "admin/issue";
        }
    }

    /**
     * Shows the detailed view for a single issued certificate, including its
     * cryptographic metadata.
     */
    @GetMapping("/admin/certificates/{id}")
    public String certificateDetail(@PathVariable("id") Long id, Model model) {
        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        model.addAttribute("certificate", cert);
        return "admin/certificate-detail";
    }

    /**
     * Displays an edit form for an existing certificate so that admins can correct
     * fields such as student, degree, institution, or issue date. Saving will
     * regenerate the document hash and digital signature.
     */
    @GetMapping("/admin/certificates/{id}/edit")
    public String editCertificate(@PathVariable("id") Long id, Model model) {
        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        CertificateRequest req = new CertificateRequest();
        if (cert.getStudent() != null) {
            req.setStudentRefId(cert.getStudent().getId());
        }
        req.setDegree(cert.getDegree());
        req.setInstitution(cert.getInstitution());
        req.setIssueDate(cert.getIssueDate());

        model.addAttribute("certificate", cert);
        model.addAttribute("certificateRequest", req);
        model.addAttribute("students", loadActiveStudents());
        model.addAttribute("programs", programRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("error", null);
        return "admin/certificate-edit";
    }

    /**
     * Handles submission of the edit certificate form, updating the certificate and
     * re‑signing it via {@link CertificateService#updateCertificate(Long, CertificateRequest)}.
     */
    @PostMapping("/admin/certificates/{id}/edit")
    public String editCertificateSubmit(
            @PathVariable("id") Long id,
            @ModelAttribute CertificateRequest certificateRequest,
            Model model
    ) {
        String err = validate(certificateRequest);
        if (err != null) {
            var cert = certificateRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
            model.addAttribute("certificate", cert);
            model.addAttribute("certificateRequest", certificateRequest);
            model.addAttribute("students", loadActiveStudents());
            model.addAttribute("programs", programRepository.findByActiveTrueOrderByNameAsc());
            model.addAttribute("error", err);
            return "admin/certificate-edit";
        }

        var updated = certificateService.updateCertificate(id, certificateRequest);
        return "redirect:/admin/certificates/" + updated.getId();
    }

    /**
     * Runs a local signature self‑test for a certificate, without modifying any data,
     * and re-renders the detail page with the results.
     */
    @GetMapping("/admin/certificates/{id}/self-test")
    public String certificateSelfTest(@PathVariable("id") Long id, Model model) {
        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        String canonical = cryptoService.buildCanonicalString(cert);
        String hash = cryptoService.hashWithSHA256(canonical);
        boolean signatureValid = cryptoService.verifySignature(
                hash,
                cert.getDigitalSignature(),
                cert.getKeyPair().getPublicKey()
        );

        // Simulate a tampered document by changing the hash before verification.
        boolean tamperedStillValid = cryptoService.verifySignature(
                hash + "x",
                cert.getDigitalSignature(),
                cert.getKeyPair().getPublicKey()
        );

        model.addAttribute("certificate", cert);
        model.addAttribute("selfTestRan", true);
        model.addAttribute("selfTestValidSignature", signatureValid);
        model.addAttribute("selfTestTamperedAccepted", tamperedStillValid);
        return "admin/certificate-detail";
    }

    /**
     * Streams a PDF rendition of a certificate to the browser as a downloadable file.
     */
    @GetMapping("/admin/certificates/{id}/pdf")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable("id") Long id) {
        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        byte[] pdfBytes = certificatePdfService.renderCertificatePdf(cert);

        String filename = (cert.getCertificateId() != null ? cert.getCertificateId() : "certificate") + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/admin/certificates/{id}/approve")
    public String approveCertificate(@PathVariable("id") Long id, Authentication auth) {
        User approver = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        certificateService.approveCertificate(id, approver);
        return "redirect:/admin/certificates/" + id + "?approved=1";
    }

    @PostMapping("/admin/certificates/{id}/reject")
    public String rejectCertificate(@PathVariable("id") Long id,
                                    @RequestParam(value = "reason", required = false) String reason,
                                    Authentication auth) {
        User rejector = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        var rejected = certificateService.rejectCertificate(id, rejector, reason);

        String studentEmail = rejected.getStudent() != null ? rejected.getStudent().getEmail() : null;
        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
            String studentName = rejected.getStudentName() != null ? rejected.getStudentName() : "Student";
            String subject = "Update on your Tumba College certificate";
            String body = """
                    Hello %s,

                    Your certificate (%s) has been reviewed but was not approved at this time.

                    %s

                    Please contact the college administration if you have any questions.

                    Regards,
                    Tumba College
                    """.formatted(
                    studentName,
                    rejected.getCertificateId(),
                    (reason != null && !reason.isBlank())
                            ? "Reason: " + reason.trim()
                            : "No reason was provided."
            );
            mailService.send(studentEmail.trim(), subject, body);
        }

        return "redirect:/admin/certificates/" + id + "?rejected=1";
    }

    @PostMapping("/admin/certificates/{id}/send")
    public String sendCertificateToStudent(@PathVariable("id") Long id, Authentication auth) {
        User sender = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));

        try {
            certificateService.sendCertificateToStudent(id, sender);
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("rejected")) {
                return "redirect:/admin/certificates/" + id + "?sendError=rejected";
            }
            return "redirect:/admin/certificates/" + id + "?sendError=emailMissing";
        }

        boolean sent = emailCertificatePdf(cert);
        if (!sent) {
            return "redirect:/admin/certificates/" + id + "?sendError=mail";
        }
        return "redirect:/admin/certificates/" + id + "?sent=1";
    }

    @PostMapping("/admin/certificates/bulk-send")
    public String bulkSendToStudents(@RequestParam(value = "certIds", required = false) List<Long> certIds,
                                     Authentication auth) {
        if (certIds == null || certIds.isEmpty()) {
            return "redirect:/admin/certificates?bulkSendError=noneSelected";
        }
        User sender = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        int sent = 0;
        int failed = 0;
        for (Long id : certIds) {
            var certOpt = certificateRepository.findById(id);
            if (certOpt.isEmpty()) continue;
            var cert = certOpt.get();
            if (cert.getApprovalStatus() == CertificateApprovalStatus.REJECTED) {
                failed++;
                continue;
            }
            if (cert.getStudent() == null || isBlank(cert.getStudent().getEmail())) {
                failed++;
                continue;
            }
            try {
                certificateService.sendCertificateToStudent(id, sender);
                if (emailCertificatePdf(cert)) {
                    sent++;
                } else {
                    failed++;
                }
            } catch (IllegalStateException ex) {
                failed++;
            }
        }
        return "redirect:/admin/certificates?bulkSent=" + sent + "&bulkSendFailed=" + failed;
    }

    @PostMapping("/admin/certificates/{id}/resend-email")
    public String resendCertificateEmail(@PathVariable("id") Long id, Authentication auth) {
        userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        var cert = certificateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found"));
        if (cert.getApprovalStatus() == CertificateApprovalStatus.REJECTED) {
            return "redirect:/admin/certificates?resendError=rejected";
        }
        if (cert.getStudent() == null || isBlank(cert.getStudent().getEmail())) {
            return "redirect:/admin/certificates?resendError=emailMissing";
        }

        boolean sent = emailCertificatePdf(cert);
        if (!sent) {
            return "redirect:/admin/certificates?resendError=mail";
        }
        return "redirect:/admin/certificates?resendSent=1";
    }

    /**
     * Allows an ADMIN to sign in as another user for support purposes.
     * The admin's own session is replaced with the target user's authentication.
     */
    @PostMapping("/admin/users/{id}/impersonate")
    public String impersonate(@PathVariable("id") Long id,
                              Authentication auth,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDetails details = userService.loadUserByUsername(targetUser.getUsername());
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(newAuth);
        SecurityContextHolder.setContext(ctx);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        return "redirect:/admin/dashboard";
    }

    /**
     * Bulk-approves all certificates whose IDs are submitted via checkboxes.
     * Skips any that are already approved. Sends email for each after approval.
     */
    @PostMapping("/admin/certificates/bulk-approve")
    public String bulkApprove(@RequestParam(value = "certIds", required = false) List<Long> certIds,
                              Authentication auth) {
        if (certIds == null || certIds.isEmpty()) {
            return "redirect:/admin/certificates?bulkError=noneSelected";
        }
        User approver = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        int approved = 0;
        for (Long id : certIds) {
            var certOpt = certificateRepository.findById(id);
            if (certOpt.isEmpty()) continue;
            var cert = certOpt.get();
            if (cert.getApprovalStatus() == CertificateApprovalStatus.APPROVED) continue;

            certificateService.approveCertificate(id, approver);
            approved++;
        }
        return "redirect:/admin/certificates?bulkApproved=" + approved;
    }

    private boolean emailCertificatePdf(com.certsign.model.Certificate cert) {
        if (cert.getStudent() == null || isBlank(cert.getStudent().getEmail())) {
            return false;
        }
        String studentEmail = cert.getStudent().getEmail().trim();
        byte[] pdfBytes = certificatePdfService.renderCertificatePdf(cert);
        String filename = (cert.getCertificateId() != null ? cert.getCertificateId() : "certificate") + ".pdf";
        boolean principalSigned = cert.getApprovalStatus() == CertificateApprovalStatus.APPROVED;
        String subject = principalSigned
                ? "Your Tumba College certificate: " + cert.getCertificateId()
                : "Your Tumba College certificate (pending principal signature): " + cert.getCertificateId();
        String body = principalSigned
                ? """
                Hello %s,

                Please find your certificate PDF attached.
                Certificate ID: %s

                You can verify your certificate online at any time using this ID.

                Regards,
                Tumba College
                """.formatted(
                        cert.getStudentName() != null ? cert.getStudentName() : "Student",
                        cert.getCertificateId()
                )
                : """
                Hello %s,

                Please find your certificate PDF attached.
                Certificate ID: %s

                Note: this certificate is still awaiting the Principal's signature. You will receive an updated copy once it is signed.

                Regards,
                Tumba College
                """.formatted(
                        cert.getStudentName() != null ? cert.getStudentName() : "Student",
                        cert.getCertificateId()
                );
        return mailService.sendWithAttachment(
                studentEmail,
                subject,
                body,
                filename,
                pdfBytes,
                MediaType.APPLICATION_PDF_VALUE
        );
    }

    /**
     * Performs basic server‑side validation of the certificate issuance request.
     */
    private String validate(CertificateRequest r) {
        if (r == null) return "Invalid request";
        if (r.getStudentRefId() == null) return "Please select a student";
        if (isBlank(r.getDegree())) return "Degree/Program is required";
        if (programRepository.findByNameIgnoreCaseAndActiveTrue(r.getDegree().trim()).isEmpty()) {
            return "Please select an active approved program from the list";
        }
        if (isBlank(r.getInstitution())) return "Institution is required";
        if (r.getIssueDate() == null) return "Issue date is required";
        return null;
    }

    /**
     * Returns {@code true} if the given string is {@code null} or contains only whitespace.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Returns only students that are currently ACTIVE; used to populate the issuance dropdown.
     */
    private List<Student> loadActiveStudents() {
        return studentRepository.findAll().stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .toList();
    }

    private boolean isCertificateCryptographicallyValid(com.certsign.model.Certificate cert) {
        if (cert == null || cert.getKeyPair() == null || isBlank(cert.getDocumentHash()) || isBlank(cert.getDigitalSignature())) {
            return false;
        }
        String canonical = cryptoService.buildCanonicalString(cert);
        String rebuiltHash = cryptoService.hashWithSHA256(canonical);
        if (!rebuiltHash.equalsIgnoreCase(cert.getDocumentHash())) {
            return false;
        }
        return cryptoService.verifySignature(rebuiltHash, cert.getDigitalSignature(), cert.getKeyPair().getPublicKey());
    }
}

