package br.com.inventorymed.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemUserRoleRepository extends JpaRepository<SystemUserRole, UUID> {
    List<SystemUserRole> findAllByUserId(UUID userId);
}
