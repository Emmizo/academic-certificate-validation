// SDLC Phase: Implementation
// Component: PublicController
// Requirements covered: FR-06, FR-10, NFR-03
// Description: Serves public pages for landing and certificate verification
package com.certsign.controller;

import com.certsign.dto.VerificationResult;
import com.certsign.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PublicController {

    private final CertificateService certificateService;

    /**
     * Creates the public controller that exposes landing and verification endpoints.
     */
    public PublicController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Serves the public landing page.
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Displays the certificate verification form, optionally pre‑filling the
     * certificate ID if supplied as a query parameter.
     */
    @GetMapping("/verify")
    public String verifyForm(@RequestParam(value = "certificateId", required = false) String certificateId, Model model) {
        model.addAttribute("certificateId", certificateId == null ? "" : certificateId);
        model.addAttribute("result", null);
        return "verify";
    }

    /**
     * Handles verification form submissions by calling the certificate service,
     * logging the verifier IP, and returning a {@link VerificationResult} to the view.
     */
    @PostMapping("/verify")
    public String verifySubmit(
            @RequestParam("certificateId") String certificateId,
            HttpServletRequest request,
            Model model
    ) {
        String ip = request.getRemoteAddr();
        VerificationResult result = certificateService.verifyCertificate(certificateId == null ? "" : certificateId.trim(), ip);
        model.addAttribute("certificateId", certificateId);
        model.addAttribute("result", result);
        return "verify";
    }
}

