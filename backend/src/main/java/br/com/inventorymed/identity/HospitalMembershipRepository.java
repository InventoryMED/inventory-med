package br.com.inventorymed.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalMembershipRepository extends JpaRepository<HospitalMembership, UUID> {

    @EntityGraph(attributePaths = "hospital")
    List<HospitalMembership> findAllByUserIdAndActiveTrueOrderByHospitalName(UUID userId);

    @EntityGraph(attributePaths = "hospital")
    Optional<HospitalMembership> findByUserIdAndHospitalIdAndActiveTrue(
        UUID userId,
        UUID hospitalId
    );

    boolean existsByUserIdAndHospitalId(UUID userId, UUID hospitalId);
}
