package br.com.inventorymed.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class InventoryUserPrincipal
    implements UserDetails, CredentialsContainer, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final String fullName;
    private final String email;
    private final boolean active;
    private final List<String> systemRoles;
    private String passwordHash;

    public InventoryUserPrincipal(
        UUID userId,
        String fullName,
        String email,
        String passwordHash,
        boolean active,
        List<String> systemRoles
    ) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.systemRoles = List.copyOf(systemRoles);
    }

    public UUID userId() {
        return userId;
    }

    public String fullName() {
        return fullName;
    }

    public List<String> systemRoles() {
        return systemRoles;
    }

    public boolean hasSystemRole(String role) {
        return systemRoles.contains(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return systemRoles
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
