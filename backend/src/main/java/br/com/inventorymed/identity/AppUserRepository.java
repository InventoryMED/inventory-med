package br.com.inventorymed.identity;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findAllByOrderByFullName();

    boolean existsByEmailIgnoreCase(String email);
}
