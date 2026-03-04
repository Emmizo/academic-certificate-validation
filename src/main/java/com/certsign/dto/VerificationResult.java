// SDLC Phase: Implementation
// Component: VerificationResult DTO
// Requirements covered: FR-06, FR-10, NFR-03
// Description: Represents verification outcome for display to public users
package com.certsign.dto;

import com.certsign.model.Certificate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class VerificationResult {
    private final boolean valid;
    private final String message;
    private final String failureReason;
    private final LocalDateTime verifiedAt;
    private final Certificate certificate;
}

