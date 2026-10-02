package br.com.inventorymed.auth;

import br.com.inventorymed.identity.HospitalMembership;
import java.util.UUID;

public record HospitalAccessResponse(
    UUID id,
    String name,
    String shortName,
    String city,
    String role
) {
    static HospitalAccessResponse from(HospitalMembership membership) {
        return new HospitalAccessResponse(
            membership.getHospital().getId(),
            membership.getHospital().getName(),
            membership.getHospital().getShortName(),
            membership.getHospital().getCity(),
            membership.getRole().name()
        );
    }
}
