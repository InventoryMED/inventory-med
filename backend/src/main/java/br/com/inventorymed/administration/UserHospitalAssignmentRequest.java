package br.com.inventorymed.administration;

import br.com.inventorymed.identity.HospitalRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UserHospitalAssignmentRequest(
    @NotNull UUID hospitalId,
    @NotNull HospitalRole role
) {}
