// SDLC Phase: Implementation
// Component: AdminController
// Requirements covered: FR-01, FR-02, FR-05, NFR-03, NFR-04, NFR-05
// Description: Handles protected admin pages for key management and certificate issuance
package com.certsign.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.certsign.dto.CertificateRequest;
import com.certsign.model.Certificate;
import com.certsign.model.CertificateApprovalStatus;
import com.certsign.model.KeyPair;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.CertificateRepository;
import com.certsign.repository.KeyPairRepository;
import com.certsign.repository.ProgramRepository;
import com.certsign.repository.StudentRepository;
import com.certsign.repository.UserRepository;
import com.certsign.service.CertificatePdfService;
import com.certsign.service.CertificateService;
import com.certsign.service.CryptoService;
import com.certsign.service.MailService;
import com.certsign.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        var users = userRepository.findAll();
        boolean isSuperAdmin = hasRole(auth, "ROLE_SUPER_ADMIN");
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        boolean isPrincipal = hasRole(auth, "ROLE_PRINCIPAL");
        boolean isSecretary = hasRole(auth, "ROLE_SECRETARY");
        boolean isSigner = hasRole(auth, "ROLE_SIGNER");
        boolean isUserManager = hasRole(auth, "ROLE_USER_MANAGER");
        boolean canManageUsers = isSuperAdmin || isAdmin || isUserManager;
        boolean showAdminDashboard = isSuperAdmin || isAdmin;

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
        long approvedCertificates = allCerts.stream()
                .filter(c -> c.getApprovalStatus() == null || c.getApprovalStatus() == CertificateApprovalStatus.APPROVED)
                .count();
        long pendingApprovalCertificates = allCerts.stream()
                .filter(c -> c.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL)
                .filter(c -> !isPrincipal || c.isSubmittedForApproval())
                .count();
        long rejectedCertificates = allCerts.stream()
                .filter(c -> c.getApprovalStatus() == CertificateApprovalStatus.REJECTED)
                .count();
        List<String> approvalStatusLabels = List.of("Approved", "Not approved", "Draft pending");
        List<Long> approvalStatusValues = List.of(approvedCertificates, rejectedCertificates, pendingApprovalCertificates);
        long approvalStatusMax = approvalStatusValues.stream().mapToLong(Long::longValue).max().orElse(0L);
        List<Map<String, Object>> approvalStatusBars = List.of(
                chartEntry("Approved", approvedCertificates, approvalStatusMax, "bg-emerald-600", "text-emerald-700"),
                chartEntry("Not approved", rejectedCertificates, approvalStatusMax, "bg-red-600", "text-red-700"),
                chartEntry("Draft pending", pendingApprovalCertificates, approvalStatusMax, "bg-amber-500", "text-amber-700")
        );
        List<String> roleLabels = new ArrayList<>();
        List<Long> roleValues = new ArrayList<>();
        for (UserRole role : UserRole.values()) {
            long countForRole = users.stream()
                    .filter(user -> user.getRole() == role)
                    .count();
            if (countForRole > 0) {
                roleLabels.add(role.name());
                roleValues.add(countForRole);
            }
        }
        long roleMax = roleValues.stream().mapToLong(Long::longValue).max().orElse(0L);
        List<Map<String, Object>> roleBars = new ArrayList<>();
        for (int i = 0; i < roleLabels.size(); i++) {
            roleBars.add(chartEntry(roleLabels.get(i), roleValues.get(i), roleMax, "bg-[#164655]", "text-[#164655]"));
        }
        long issuanceMax = chartValues.stream().mapToLong(Long::longValue).max().orElse(0L);
        List<Map<String, Object>> issuanceBars = new ArrayList<>();
        for (int i = 0; i < chartLabels.size(); i++) {
            issuanceBars.add(chartEntry(chartLabels.get(i), chartValues.get(i), issuanceMax, "bg-[#164655]", "text-[#164655]"));
        }
        long activeUsers = users.stream().filter(User::isEnabled).count();
        long inactiveUsers = users.size() - activeUsers;
        boolean canImpersonate = isSuperAdmin || isAdmin;

        model.addAttribute("username", auth.getName());
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isPrincipal", isPrincipal);
        model.addAttribute("isSecretary", isSecretary);
        model.addAttribute("isSigner", isSigner);
        model.addAttribute("isUserManager", isUserManager);
        model.addAttribute("canManageUsers", canManageUsers);
        model.addAttribute("showAdminDashboard", showAdminDashboard);
        model.addAttribute("totalCerts", totalCerts);
        model.addAttribute("activeKeyPresent", activeKey.isPresent());
        model.addAttribute("activeKeyCreatedAt", activeKey.map(KeyPair::getCreatedAt).orElse(null));
        model.addAttribute("lastCertificateDate", lastCert.map(c -> c.getCreatedAt().toLocalDate()).orElse(null));
        model.addAttribute("users", users);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("canImpersonate", canImpersonate);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("roleLabels", roleLabels);
        model.addAttribute("roleValues", roleValues);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartValues", chartValues);
        model.addAttribute("certsLast7Days", certsLast7Days);
        model.addAttribute("approvedCertificates", approvedCertificates);
        model.addAttribute("pendingApprovalCertificates", pendingApprovalCertificates);
        model.addAttribute("rejectedCertificates", rejectedCertificates);
        model.addAttribute("approvalStatusLabels", approvalStatusLabels);
        model.addAttribute("approvalStatusValues", approvalStatusValues);
        model.addAttribute("approvalStatusBars", approvalStatusBars);
        model.addAttribute("roleBars", roleBars);
        model.addAttribute("issuanceBars", issuanceBars);
        return "admin/dashboard";
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private Map<String, Object> chartEntry(String label, long value, long max, String barClass, String valueClass) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("label", label);
        entry.put("value", value);
        entry.put("percent", max == 0 ? 0 : Math.max(4, Math.round((value * 100.0f) / max)));
        entry.put("barClass", barClass);
        entry.put("valueClass", valueClass);
        return entry;
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
     * Lists all issued certificates with pending approval drafts first.
     */
    @GetMapping("/admin/certificates")
    public String certificates(@RequestParam(value = "tab", defaultValue = "waiting") String tab,
                               Authentication auth,
                               Model model) {
        var allCertificates = certificateRepository.findAllForApprovalQueue();
        boolean principalView = hasRole(auth, "ROLE_PRINCIPAL")
                && !hasRole(auth, "ROLE_SUPER_ADMIN")
                && !hasRole(auth, "ROLE_ADMIN");
        String activeTab = normalizeCertificateTab(tab);
        var certificates = allCertificates.stream()
                .filter(cert -> certificateBelongsToTab(cert, activeTab, principalView))
                .toList();
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
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("waitingCount", allCertificates.stream()
                .filter(cert -> cert.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL)
                .filter(cert -> !principalView || cert.isSubmittedForApproval())
                .count());
        model.addAttribute("readyCount", allCertificates.stream()
                .filter(cert -> cert.getApprovalStatus() == CertificateApprovalStatus.APPROVED && cert.getSentAt() == null)
                .count());
        model.addAttribute("sentCount", allCertificates.stream()
                .filter(cert -> cert.getSentAt() != null)
                .count());
        model.addAttribute("allCount", allCertificates.stream()
                .filter(cert -> certificateBelongsToTab(cert, "all", principalView))
                .count());
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
        req.setInstitution(CertificateService.DEFAULT_INSTITUTION);
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
        certificateRequest.setInstitution(CertificateService.DEFAULT_INSTITUTION);
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
            return "redirect:/admin/certificates/" + saved.getId() + "?drafted=1";
        } catch (IllegalArgumentException | IllegalStateException ex) {
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
        var cert = certificateRepository.findByIdWithDetails(id)
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
        req.setInstitution(CertificateService.DEFAULT_INSTITUTION);
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
        certificateRequest.setInstitution(CertificateService.DEFAULT_INSTITUTION);
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
        try {
            certificateService.approveCertificate(id, approver);
            return "redirect:/admin/certificates/" + id + "?approved=1";
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("sent to Principal")) {
                return "redirect:/admin/certificates/" + id + "?approveError=notSubmitted";
            }
            return "redirect:/admin/certificates/" + id + "?approveError=notPending";
        }
    }

    @PostMapping("/admin/certificates/{id}/reject")
    public String rejectCertificate(@PathVariable("id") Long id,
                                    @RequestParam(value = "reason", required = false) String reason,
                                    Authentication auth) {
        User rejector = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        Certificate rejected;
        try {
            rejected = certificateService.rejectCertificate(id, rejector, reason);
        } catch (IllegalStateException ex) {
            return "redirect:/admin/certificates/" + id + "?rejectError=notSubmitted";
        }

        String studentEmail = rejected.getStudent() != null ? rejected.getStudent().getEmail() : null;
        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
            String studentName = rejected.getStudentName() != null ? rejected.getStudentName() : "Student";
            String subject = "Update on your IPRC Tumba College certificate";
            String body = """
                    Hello %s,

                    Your certificate (%s) has been reviewed but was not approved at this time.

                    %s

                    Please contact the college administration if you have any questions.

                    Regards,
                    IPRC Tumba College
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

        try {
            Certificate cert = certificateService.sendCertificateToStudent(id, sender);
            if (!emailCertificatePdf(cert)) {
                return "redirect:/admin/certificates/" + id + "?sendError=mail";
            }
        } catch (IllegalArgumentException ex) {
            return "redirect:/admin/certificates?sendError=notFound";
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("rejected")) {
                return "redirect:/admin/certificates/" + id + "?sendError=rejected";
            }
            return "redirect:/admin/certificates/" + id + "?sendError=emailMissing";
        }

        return "redirect:/admin/certificates/" + id + "?sent=1";
    }

    @PostMapping("/admin/certificates/bulk-send")
    public String bulkSendToStudents(@RequestParam(value = "certIds", required = false) List<Long> certIds,
                                     @RequestParam(value = "tab", defaultValue = "ready") String tab,
                                     Authentication auth) {
        if (certIds == null || certIds.isEmpty()) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkSendError=noneSelected";
        }
        User sender = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        int sent = 0;
        int failed = 0;
        for (Long id : certIds) {
            var certOpt = certificateRepository.findByIdWithDetails(id);
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
                Certificate sentCert = certificateService.sendCertificateToStudent(id, sender);
                if (emailCertificatePdf(sentCert)) {
                    sent++;
                } else {
                    failed++;
                }
            } catch (IllegalStateException ex) {
                failed++;
            }
        }
        return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkSent=" + sent + "&bulkSendFailed=" + failed;
    }

    @PostMapping("/admin/certificates/bulk-notify-principal")
    public String bulkNotifyPrincipal(@RequestParam(value = "certIds", required = false) List<Long> certIds,
                                      @RequestParam(value = "tab", defaultValue = "waiting") String tab) {
        if (certIds == null || certIds.isEmpty()) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkNotifyError=noneSelected";
        }

        List<Certificate> pendingDrafts = certIds.stream()
                .map(certificateRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(cert -> cert.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL)
                .toList();

        if (pendingDrafts.isEmpty()) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkNotifyError=nonePending";
        }

        List<User> principals = userRepository.findByRoleAndEnabledTrue(UserRole.PRINCIPAL).stream()
                .filter(user -> !isBlank(user.getEmail()))
                .toList();
        if (principals.isEmpty()) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkNotifyError=noPrincipalEmail";
        }

        String draftLines = pendingDrafts.stream()
                .map(cert -> "- " + cert.getCertificateId() + " | " + cert.getStudentName() + " | " + cert.getDegree())
                .reduce("", (current, line) -> current + line + "\n");
        String subject = "Certificate drafts waiting for Principal approval";
        String body = """
                Hello,

                The following certificate draft(s) are waiting for Principal approval:

                %s
                Please sign in to the certificate portal and open the Waiting approval tab.

                Regards,
                IPRC Tumba College Certificate Portal
                """.formatted(draftLines);

        int sent = 0;
        for (User principal : principals) {
            if (mailService.send(principal.getEmail().trim(), subject, body)) {
                sent++;
            }
        }

        if (sent == 0) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkNotifyError=mail";
        }
        for (Certificate draft : pendingDrafts) {
            draft.setSubmittedForApproval(true);
            certificateRepository.save(draft);
        }
        return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkNotified=" + pendingDrafts.size();
    }

    @PostMapping("/admin/certificates/{id}/resend-email")
    public String resendCertificateEmail(@PathVariable("id") Long id, Authentication auth) {
        userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        var cert = certificateRepository.findByIdWithDetails(id)
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
        boolean isAuthorized = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority())
                        || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAuthorized) {
            throw new org.springframework.security.access.AccessDeniedException("Only admins can impersonate users");
        }
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!targetUser.isEnabled()) {
            throw new IllegalStateException("Cannot impersonate a disabled user");
        }

        // Save original admin username in session if not already impersonating
        jakarta.servlet.http.HttpSession session = request.getSession(true);
        if (session.getAttribute("originalUserUsername") == null) {
            session.setAttribute("originalUserUsername", auth.getName());
        }

        UserDetails details = userService.loadUserByUsername(targetUser.getUsername());
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(newAuth);
        SecurityContextHolder.setContext(ctx);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/stop-impersonate")
    public String stopImpersonate(HttpServletRequest request) {
        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session != null) {
            String originalUsername = (String) session.getAttribute("originalUserUsername");
            if (originalUsername != null) {
                UserDetails details = userService.loadUserByUsername(originalUsername);
                UsernamePasswordAuthenticationToken newAuth =
                        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                SecurityContext ctx = SecurityContextHolder.createEmptyContext();
                ctx.setAuthentication(newAuth);
                SecurityContextHolder.setContext(ctx);
                session.setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
                session.removeAttribute("originalUserUsername");
            }
        }
        return "redirect:/admin/users";
    }

    /**
     * Bulk-approves all certificates whose IDs are submitted via checkboxes.
     * Skips any that are already approved. Sends email for each after approval.
     */
    @PostMapping("/admin/certificates/bulk-approve")
    public String bulkApprove(@RequestParam(value = "certIds", required = false) List<Long> certIds,
                              @RequestParam(value = "tab", defaultValue = "waiting") String tab,
                              Authentication auth) {
        if (certIds == null || certIds.isEmpty()) {
            return "redirect:/admin/certificates?tab=" + normalizeCertificateTab(tab) + "&bulkError=noneSelected";
        }
        User approver = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        int approved = 0;
        for (Long id : certIds) {
            var certOpt = certificateRepository.findById(id);
            if (certOpt.isEmpty()) continue;
            var cert = certOpt.get();
            if (cert.getApprovalStatus() != CertificateApprovalStatus.PENDING_APPROVAL || !cert.isSubmittedForApproval()) continue;

            certificateService.approveCertificate(id, approver);
            approved++;
        }
        return "redirect:/admin/certificates?tab=ready&bulkApproved=" + approved;
    }

    private boolean emailCertificatePdf(com.certsign.model.Certificate cert) {
        if (cert.getStudent() == null || isBlank(cert.getStudent().getEmail())) {
            return false;
        }

        Student student = cert.getStudent();
        String studentEmail = student.getEmail().trim();
        String studentName = student.getFullName() != null ? student.getFullName() : cert.getStudentName();
        String studentNumber = student.getStudentNumber() != null ? student.getStudentNumber() : cert.getStudentId();

        byte[] pdfBytes = certificatePdfService.renderCertificatePdf(cert);
        if (pdfBytes == null || pdfBytes.length == 0) {
            return false;
        }
        String filename = studentNumber + "-" + cert.getCertificateId() + ".pdf";

        boolean principalSigned = cert.getApprovalStatus() == CertificateApprovalStatus.APPROVED;
        String subject = principalSigned
                ? "Your IPRC Tumba College certificate — " + studentName
                : "Your IPRC Tumba College certificate (draft) — " + studentName;

        String body = principalSigned
                ? """
                Dear %s,

                Please find your academic certificate attached as a PDF file.
                Student number: %s
                Certificate ID: %s
                Program: %s

                The attached PDF belongs to you and reflects the details above.

                You may verify this certificate online at any time using the Certificate ID.

                Kind regards,
                IPRC Tumba College Administration
                """.formatted(
                        studentName,
                        studentNumber,
                        cert.getCertificateId(),
                        cert.getDegree()
                )
                : """
                Dear %s,

                Please find your certificate draft attached as a PDF file.
                Student number: %s
                Certificate ID: %s
                Program: %s

                The attached PDF is specific to your student record. This certificate is still awaiting the Principal's signature; you will receive an updated copy once it has been signed.

                Kind regards,
                IPRC Tumba College Administration
                """.formatted(
                        studentName,
                        studentNumber,
                        cert.getCertificateId(),
                        cert.getDegree()
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
        var student = studentRepository.findById(r.getStudentRefId());
        if (student.isEmpty()) return "Selected student not found";
        if (student.get().getStatus() != StudentStatus.ACTIVE) return "Selected student is not active";
        if (isBlank(r.getDegree())) return "Degree/Program is required";
        if (programRepository.findByNameIgnoreCaseAndActiveTrue(r.getDegree().trim()).isEmpty()) {
            return "Please select an active approved program from the list";
        }
        if (r.getIssueDate() == null) return "Issue date is required";
        return null;
    }

    /**
     * Returns {@code true} if the given string is {@code null} or contains only whitespace.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalizeCertificateTab(String tab) {
        if ("ready".equalsIgnoreCase(tab) || "sent".equalsIgnoreCase(tab) || "all".equalsIgnoreCase(tab)) {
            return tab.toLowerCase();
        }
        return "waiting";
    }

    private boolean certificateBelongsToTab(Certificate cert, String tab, boolean principalView) {
        if ("ready".equals(tab)) {
            return cert.getApprovalStatus() == CertificateApprovalStatus.APPROVED && cert.getSentAt() == null;
        }
        if ("sent".equals(tab)) {
            return cert.getSentAt() != null;
        }
        if ("all".equals(tab)) {
            return !principalView || cert.getApprovalStatus() != CertificateApprovalStatus.PENDING_APPROVAL || cert.isSubmittedForApproval();
        }
        return cert.getApprovalStatus() == CertificateApprovalStatus.PENDING_APPROVAL
                && (!principalView || cert.isSubmittedForApproval());
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
