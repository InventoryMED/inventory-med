package br.com.inventorymed.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SystemUserRoleRepository extends JpaRepository<SystemUserRole, UUID> {
    List<SystemUserRole> findAllByUserId(UUID userId);

    boolean existsByUserIdAndRole(UUID userId, SystemRole role);

    @EntityGraph(attributePaths = "user")
    @Query("select systemRole from SystemUserRole systemRole")
    List<SystemUserRole> findAllWithUser();
}
