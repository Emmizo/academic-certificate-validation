package com.certsign.repository;

import com.certsign.model.Program;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Optional<Program> findByNameIgnoreCase(String name);

    @Query("""
            SELECT p FROM Program p
            LEFT JOIN FETCH p.licenceType
            WHERE LOWER(p.name) = LOWER(:name) AND p.active = true
            """)
    Optional<Program> findByNameIgnoreCaseAndActiveTrue(@Param("name") String name);

    @Query("""
            SELECT p FROM Program p
            LEFT JOIN FETCH p.licenceType
            WHERE p.active = true
            ORDER BY p.name ASC
            """)
    List<Program> findByActiveTrueOrderByNameAsc();
}
