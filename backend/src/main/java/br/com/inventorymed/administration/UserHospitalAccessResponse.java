package br.com.inventorymed.administration;

import br.com.inventorymed.identity.HospitalMembership;
import java.util.UUID;

public record UserHospitalAccessResponse(
    UUID hospitalId,
    String hospitalName,
    String role,
    boolean active
) {
    static UserHospitalAccessResponse from(HospitalMembership membership) {
        return new UserHospitalAccessResponse(
            membership.getHospital().getId(),
            membership.getHospital().getName(),
            membership.getRole().name(),
            membership.isActive()
        );
    }
}
