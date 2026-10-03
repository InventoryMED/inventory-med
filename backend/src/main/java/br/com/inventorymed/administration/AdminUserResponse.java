package br.com.inventorymed.administration;

import br.com.inventorymed.identity.AppUser;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String name,
    String email,
    boolean active,
    boolean mustChangePassword,
    List<String> systemRoles,
    List<UserHospitalAccessResponse> hospitals
) {
    static AdminUserResponse from(
        AppUser user,
        List<String> systemRoles,
        List<UserHospitalAccessResponse> hospitals
    ) {
        return new AdminUserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.isActive(),
            user.mustChangePassword(),
            systemRoles,
            hospitals
        );
    }
}
