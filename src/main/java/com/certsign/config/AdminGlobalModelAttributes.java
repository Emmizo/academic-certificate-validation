package com.certsign.config;

import com.certsign.model.CertificateApprovalStatus;
import com.certsign.repository.CertificateRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class AdminGlobalModelAttributes {

    private final CertificateRepository certificateRepository;

    public AdminGlobalModelAttributes(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @ModelAttribute("pendingApprovalCount")
    public long pendingApprovalCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return 0L;
        }
        boolean isAdminOrSigner = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SIGNER".equals(a.getAuthority()));
        if (!isAdminOrSigner) {
            return 0L;
        }
        return certificateRepository.countByApprovalStatus(CertificateApprovalStatus.PENDING_APPROVAL);
    }
}
