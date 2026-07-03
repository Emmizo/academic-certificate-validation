package com.certsign.repository;

import com.certsign.model.LicenceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenceTypeRepository extends JpaRepository<LicenceType, Long> {
    Optional<LicenceType> findByNameIgnoreCase(String name);

    List<LicenceType> findByActiveTrueOrderByNameAsc();
}
