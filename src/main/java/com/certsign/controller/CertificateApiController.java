package com.certsign.controller;

import com.certsign.dto.VerificationResult;
import com.certsign.model.Certificate;
import com.certsign.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CertificateApiController {

    private final CertificateService certificateService;

    public CertificateApiController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    /**
     * Public REST API endpoint for external systems to verify academic certificates programmatically.
     * Returns verification status, signature correctness, and metadata in JSON format.
     */
    @GetMapping("/api/v1/certificates/verify")
    public ResponseEntity<Map<String, Object>> verifyCertificate(
            @RequestParam("certificateId") String certificateId,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        VerificationResult result = certificateService.verifyCertificate(
                certificateId == null ? "" : certificateId.trim(), ip);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", result.isValid());
        response.put("message", result.getMessage());
        response.put("verifiedAt", result.getVerifiedAt());

        if (result.isValid() && result.getCertificate() != null) {
            Certificate cert = result.getCertificate();
            Map<String, Object> certData = new LinkedHashMap<>();
            certData.put("certificateId", cert.getCertificateId());
            certData.put("degree", cert.getDegree());
            certData.put("studentName", cert.getStudent() != null ? cert.getStudent().getFullName() : null);
            certData.put("studentNumber", cert.getStudent() != null ? cert.getStudent().getStudentNumber() : null);
            certData.put("attendedProgram", cert.getStudent() != null ? cert.getStudent().getAttendedProgram() : null);
            certData.put("academicYear", cert.getStudent() != null ? cert.getStudent().getAcademicYear() : null);
            certData.put("issueDate", cert.getIssueDate());
            certData.put("signature", cert.getDigitalSignature());
            response.put("certificate", certData);
        } else {
            response.put("failureReason", result.getFailureReason());
        }

        return ResponseEntity.ok(response);
    }
}
