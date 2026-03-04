// SDLC Phase: Implementation
// Component: StudentRequest DTO
// Requirements covered: FR-02, FR-03, NFR-03
// Description: Captures admin input when creating or editing a student
package com.certsign.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class StudentRequest {

    private String studentNumber;
    private String fullName;
    private String email;
    private String nationalId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;
}

