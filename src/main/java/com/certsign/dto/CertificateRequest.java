// SDLC Phase: Implementation
// Component: CertificateRequest DTO
// Requirements covered: FR-02, NFR-03
// Description: Captures admin input for certificate issuance
package com.certsign.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class CertificateRequest {
    // Internal Student primary key selected from the admin UI
    private Long studentRefId;

    private String degree;
    private String institution;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;
}

