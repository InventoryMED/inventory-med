package br.com.inventorymed.auth;

import br.com.inventorymed.identity.AppUser;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    List<String> systemRoles,
    boolean mustChangePassword
) {
    static UserResponse from(AppUser user, List<String> systemRoles) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            List.copyOf(systemRoles),
            user.mustChangePassword()
        );
    }
}
