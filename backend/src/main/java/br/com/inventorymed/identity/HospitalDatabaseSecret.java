package br.com.inventorymed.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospital_database_secret")
public class HospitalDatabaseSecret {

    @Id
    @Column(name = "hospital_id")
    private UUID hospitalId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(name = "login_name", nullable = false, length = 128)
    private String loginName;

    @Column(name = "encrypted_password", nullable = false, length = 2000)
    private String encryptedPassword;

    @Column(name = "encryption_key_version", nullable = false)
    private short encryptionKeyVersion = 1;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;

    protected HospitalDatabaseSecret() {}

    public HospitalDatabaseSecret(
        Hospital hospital,
        String loginName,
        String encryptedPassword
    ) {
        this.hospital = hospital;
        this.loginName = loginName;
        this.encryptedPassword = encryptedPassword;
    }

    public UUID getHospitalId() {
        return hospitalId;
    }

    public String getLoginName() {
        return loginName;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }
}
