package br.com.inventorymed.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_changed_at", nullable = false, insertable = false)
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected AppUser() {}

    public AppUser(String fullName, String email, String passwordHash) {
        this(fullName, email, passwordHash, false);
    }

    public AppUser(
        String fullName,
        String email,
        String passwordHash,
        boolean mustChangePassword
    ) {
        this.fullName = fullName;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public void deactivate(Instant occurredAt) {
        this.active = false;
        this.updatedAt = occurredAt;
    }

    public void activate(Instant occurredAt) {
        this.active = true;
        this.updatedAt = occurredAt;
    }

    public void recordSuccessfulLogin(Instant occurredAt) {
        this.lastLoginAt = occurredAt;
        this.updatedAt = occurredAt;
    }

    public void changePassword(String encodedPassword, Instant occurredAt) {
        this.passwordHash = encodedPassword;
        this.mustChangePassword = false;
        this.passwordChangedAt = occurredAt;
        this.updatedAt = occurredAt;
    }
}
