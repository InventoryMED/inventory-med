package br.com.inventorymed.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    Optional<Hospital> findByNameIgnoreCase(String name);
}
