package br.com.inventorymed.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalDatabaseSecretRepository
    extends JpaRepository<HospitalDatabaseSecret, UUID> {}
