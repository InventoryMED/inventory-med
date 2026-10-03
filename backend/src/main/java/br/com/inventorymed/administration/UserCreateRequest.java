package br.com.inventorymed.administration;

import br.com.inventorymed.identity.SystemRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record UserCreateRequest(
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 128) String initialPassword,
    @Size(max = 5) Set<@NotNull SystemRole> systemRoles,
    @Size(max = 50) List<@Valid UserHospitalAssignmentRequest> hospitalAssignments
) {}
