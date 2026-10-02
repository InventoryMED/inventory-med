package br.com.inventorymed.auth;

import br.com.inventorymed.identity.AppUser;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email) {
    static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }
}
