package br.com.inventorymed.tenancy;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.common.ProvisioningException;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalDatabaseSecret;
import br.com.inventorymed.identity.HospitalDatabaseSecretRepository;
import br.com.inventorymed.identity.HospitalRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

@Component
public class TenantDataSourceRegistry {

    private static final Pattern SAFE_DATABASE_NAME = Pattern.compile("[a-z0-9_]{3,128}");

    private final HospitalRepository hospitalRepository;
    private final HospitalDatabaseSecretRepository secretRepository;
    private final TenantProvisioningProperties properties;
    private final CredentialCipher credentialCipher;
    private final Map<UUID, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    public TenantDataSourceRegistry(
        HospitalRepository hospitalRepository,
        HospitalDatabaseSecretRepository secretRepository,
        TenantProvisioningProperties properties,
        CredentialCipher credentialCipher
    ) {
        this.hospitalRepository = hospitalRepository;
        this.secretRepository = secretRepository;
        this.properties = properties;
        this.credentialCipher = credentialCipher;
    }

    public DataSource requiredDataSource(UUID hospitalId) {
        return dataSources.computeIfAbsent(hospitalId, this::createDataSource);
    }

    private HikariDataSource createDataSource(UUID hospitalId) {
        Hospital hospital = hospitalRepository
            .findById(hospitalId)
            .filter(Hospital::isActive)
            .orElseThrow(() -> new BusinessValidationException("Hospital inexistente ou inativo"));
        HospitalDatabaseSecret secret = secretRepository
            .findById(hospitalId)
            .orElseThrow(() -> new ProvisioningException("Hospital sem credencial de banco registrada"));
        String databaseName = validatedDatabaseName(hospital.getDatabaseName());
        migrate(databaseName);

        String password = credentialCipher.decrypt(secret.getEncryptedPassword());
        try {
            HikariConfig config = new HikariConfig();
            config.setPoolName("tenant-" + hospital.getTechnicalCode());
            config.setJdbcUrl(databaseUrl(databaseName));
            config.setUsername(secret.getLoginName());
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(300_000);
            config.setMaxLifetime(1_500_000);
            return new HikariDataSource(config);
        } finally {
            password = null;
        }
    }

    private void migrate(String databaseName) {
        if (!properties.enabled()) {
            throw new ProvisioningException("Acesso aos bancos hospitalares não está habilitado");
        }
        Flyway.configure()
            .dataSource(databaseUrl(databaseName), properties.username(), properties.password())
            .locations("classpath:db/migration/tenant")
            .cleanDisabled(true)
            .load()
            .migrate();
    }

    private String databaseUrl(String databaseName) {
        String serverUrl = properties.serverJdbcUrl();
        if (serverUrl.matches("(?i).*databaseName=[^;]*.*")) {
            return serverUrl.replaceFirst(
                "(?i)databaseName=[^;]*",
                "databaseName=" + databaseName
            );
        }
        return serverUrl + (serverUrl.endsWith(";") ? "" : ";") + "databaseName=" + databaseName;
    }

    private String validatedDatabaseName(String databaseName) {
        if (!SAFE_DATABASE_NAME.matcher(databaseName).matches()) {
            throw new ProvisioningException("Identificador técnico de banco inválido");
        }
        return databaseName;
    }

    @PreDestroy
    void close() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
}
