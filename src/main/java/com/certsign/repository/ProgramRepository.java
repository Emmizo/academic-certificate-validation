package com.certsign.repository;

import com.certsign.model.Program;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Optional<Program> findByNameIgnoreCase(String name);
    Optional<Program> findByNameIgnoreCaseAndActiveTrue(String name);
    List<Program> findByActiveTrueOrderByNameAsc();
}
