package com.certsign.config;

import com.certsign.repository.CertificateRepository;
import com.certsign.repository.UserRepository;
import com.certsign.model.CertificateApprovalStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class AdminGlobalModelAttributes {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;

    public AdminGlobalModelAttributes(CertificateRepository certificateRepository, UserRepository userRepository) {
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("currentUserFullName")
    public String currentUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "";
        }
        return userRepository.findByUsername(auth.getName())
                .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername())
                .orElse(auth.getName());
    }

    @ModelAttribute("pendingApprovalCount")
    public long pendingApprovalCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        boolean isAdminOrSigner = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority())
                        || "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SIGNER".equals(a.getAuthority())
                        || "ROLE_PRINCIPAL".equals(a.getAuthority()));
        if (!isAdminOrSigner) {
            return 0L;
        }
        boolean isPrincipal = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PRINCIPAL".equals(a.getAuthority()));
        if (isPrincipal) {
            return certificateRepository.countByApprovalStatusAndSubmittedForApprovalTrue(CertificateApprovalStatus.PENDING_APPROVAL);
        }
        return certificateRepository.countByApprovalStatus(CertificateApprovalStatus.PENDING_APPROVAL);
    }
}
