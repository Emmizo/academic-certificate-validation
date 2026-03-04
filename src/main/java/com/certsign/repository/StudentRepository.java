// SDLC Phase: Implementation
// Component: StudentRepository
// Requirements covered: FR-02, FR-03, NFR-03
// Description: Persistence operations for Student entities
package com.certsign.repository;

import com.certsign.model.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentNumber(String studentNumber);
}

