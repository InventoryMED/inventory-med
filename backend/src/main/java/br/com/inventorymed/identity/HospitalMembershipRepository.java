package br.com.inventorymed.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HospitalMembershipRepository extends JpaRepository<HospitalMembership, UUID> {

    @EntityGraph(attributePaths = "hospital")
    List<HospitalMembership> findAllByUserIdAndActiveTrueOrderByHospitalName(UUID userId);

    @EntityGraph(attributePaths = "hospital")
    Optional<HospitalMembership> findByUserIdAndHospitalIdAndActiveTrue(
        UUID userId,
        UUID hospitalId
    );

    boolean existsByUserIdAndHospitalId(UUID userId, UUID hospitalId);

    Optional<HospitalMembership> findByUserIdAndHospitalId(UUID userId, UUID hospitalId);

    @EntityGraph(attributePaths = { "hospital", "user" })
    @Query("select membership from HospitalMembership membership")
    List<HospitalMembership> findAllWithHospitalAndUser();
}
