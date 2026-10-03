package br.com.inventorymed.administration;

import br.com.inventorymed.identity.Hospital;
import java.time.Instant;
import java.util.UUID;

public record HospitalSummaryResponse(
    UUID id,
    String name,
    String shortName,
    String city,
    String status,
    Instant provisionedAt
) {
    static HospitalSummaryResponse from(Hospital hospital) {
        return new HospitalSummaryResponse(
            hospital.getId(),
            hospital.getName(),
            hospital.getShortName(),
            hospital.getCity(),
            hospital.getStatus().name(),
            hospital.getProvisionedAt()
        );
    }
}
