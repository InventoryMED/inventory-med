package br.com.inventorymed.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "short_name", nullable = false, length = 40)
    private String shortName;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "database_name", nullable = false, length = 128)
    private String databaseName;

    @Column(name = "technical_code", nullable = false, length = 24)
    private String technicalCode;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HospitalStatus status = HospitalStatus.ACTIVE;

    @Column(name = "provisioning_error", length = 1000)
    private String provisioningError;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected Hospital() {}

    public Hospital(String name, String shortName, String city, String databaseName) {
        this(
            name,
            shortName,
            city,
            databaseName,
            "H" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(),
            HospitalStatus.ACTIVE
        );
        this.provisionedAt = Instant.now();
    }

    public Hospital(
        String name,
        String shortName,
        String city,
        String databaseName,
        String technicalCode,
        HospitalStatus status
    ) {
        this.name = name;
        this.shortName = shortName;
        this.city = city;
        this.databaseName = databaseName;
        this.technicalCode = technicalCode;
        this.status = status;
        this.active = status == HospitalStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCity() {
        return city;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getTechnicalCode() {
        return technicalCode;
    }

    public HospitalStatus getStatus() {
        return status;
    }

    public String getProvisioningError() {
        return provisioningError;
    }

    public Instant getProvisionedAt() {
        return provisionedAt;
    }

    public boolean isActive() {
        return active && status == HospitalStatus.ACTIVE;
    }

    public void markProvisioned(Instant occurredAt) {
        this.status = HospitalStatus.ACTIVE;
        this.active = true;
        this.provisionedAt = occurredAt;
        this.provisioningError = null;
        this.updatedAt = occurredAt;
    }

    public void restartProvisioning(Instant occurredAt) {
        this.status = HospitalStatus.PROVISIONING;
        this.active = false;
        this.provisioningError = null;
        this.updatedAt = occurredAt;
    }

    public void markProvisioningFailed(String reason, Instant occurredAt) {
        this.status = HospitalStatus.PROVISIONING_FAILED;
        this.active = false;
        this.provisioningError = reason;
        this.updatedAt = occurredAt;
    }
}
