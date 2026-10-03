package br.com.inventorymed.tenancy;

import br.com.inventorymed.common.ProvisioningException;
import br.com.inventorymed.identity.Hospital;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

@Service
public class TenantDatabaseProvisioner {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z0-9_]{3,128}");

    private final TenantProvisioningProperties properties;
    private final CredentialCipher credentialCipher;
    private final SecureRandom secureRandom = new SecureRandom();

    public TenantDatabaseProvisioner(
        TenantProvisioningProperties properties,
        CredentialCipher credentialCipher
    ) {
        this.properties = properties;
        this.credentialCipher = credentialCipher;
    }

    public ProvisionedTenantCredential provision(Hospital hospital) {
        requireConfiguration();
        String databaseName = validatedIdentifier(hospital.getDatabaseName());
        String loginName = validatedIdentifier(
            "im_h_" + hospital.getTechnicalCode().toLowerCase(Locale.ROOT)
        );
        String loginPassword = generatePassword();
        boolean databaseCreated = false;
        boolean loginCreated = false;

        try {
            executeServerStatement("CREATE DATABASE " + quoted(databaseName));
            databaseCreated = true;
            executeServerStatement(
                "CREATE LOGIN " +
                quoted(loginName) +
                " WITH PASSWORD = '" +
                loginPassword +
                "', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF"
            );
            loginCreated = true;
            migrateTenant(databaseName);
            configureTenantDatabase(hospital, databaseName, loginName);
            return new ProvisionedTenantCredential(
                loginName,
                credentialCipher.encrypt(loginPassword)
            );
        } catch (Exception exception) {
            cleanup(databaseName, loginName, databaseCreated, loginCreated);
            throw new ProvisioningException(
                "Não foi possível preparar o banco exclusivo do hospital",
                exception
            );
        } finally {
            loginPassword = null;
        }
    }

    private void migrateTenant(String databaseName) {
        Flyway.configure()
            .dataSource(
                databaseUrl(databaseName),
                properties.username(),
                properties.password()
            )
            .locations("classpath:db/migration/tenant")
            .cleanDisabled(true)
            .load()
            .migrate();
    }

    private void configureTenantDatabase(
        Hospital hospital,
        String databaseName,
        String loginName
    ) throws SQLException {
        try (
            Connection connection = DriverManager.getConnection(
                databaseUrl(databaseName),
                properties.username(),
                properties.password()
            );
            Statement statement = connection.createStatement()
        ) {
            statement.execute(
                "CREATE USER " + quoted(loginName) + " FOR LOGIN " + quoted(loginName)
            );
            statement.execute(
                "ALTER ROLE db_datareader ADD MEMBER " + quoted(loginName)
            );
            statement.execute(
                "ALTER ROLE db_datawriter ADD MEMBER " + quoted(loginName)
            );
            statement.execute(
                "GRANT EXECUTE ON SCHEMA::dbo TO " + quoted(loginName)
            );
            try (
                PreparedStatement metadata = connection.prepareStatement(
                    "INSERT INTO dbo.tenant_metadata " +
                    "(singleton_id, hospital_id, hospital_name) VALUES (1, ?, ?)"
                )
            ) {
                metadata.setObject(1, hospital.getId());
                metadata.setString(2, hospital.getName());
                metadata.executeUpdate();
            }
        }
    }

    private void executeServerStatement(String sql) throws SQLException {
        try (
            Connection connection = DriverManager.getConnection(
                properties.serverJdbcUrl(),
                properties.username(),
                properties.password()
            );
            Statement statement = connection.createStatement()
        ) {
            statement.execute(sql);
        }
    }

    private void cleanup(
        String databaseName,
        String loginName,
        boolean databaseCreated,
        boolean loginCreated
    ) {
        try {
            if (databaseCreated) {
                executeServerStatement(
                    "ALTER DATABASE " +
                    quoted(databaseName) +
                    " SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE " +
                    quoted(databaseName)
                );
            }
        } catch (SQLException ignored) {
            // A falha permanece registrada no banco central para recuperação controlada.
        }
        try {
            if (loginCreated) {
                executeServerStatement("DROP LOGIN " + quoted(loginName));
            }
        } catch (SQLException ignored) {
            // A falha permanece registrada no banco central para recuperação controlada.
        }
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

    private void requireConfiguration() {
        if (!properties.enabled()) {
            throw new ProvisioningException(
                "O provisionamento de hospitais não está habilitado neste ambiente"
            );
        }
        if (
            isBlank(properties.serverJdbcUrl()) ||
            isBlank(properties.username()) ||
            isBlank(properties.password())
        ) {
            throw new ProvisioningException(
                "A credencial técnica de provisionamento não está configurada"
            );
        }
    }

    private String validatedIdentifier(String identifier) {
        if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new ProvisioningException("Identificador técnico de banco inválido");
        }
        return identifier;
    }

    private String quoted(String identifier) {
        return "[" + validatedIdentifier(identifier) + "]";
    }

    private String generatePassword() {
        byte[] random = new byte[36];
        secureRandom.nextBytes(random);
        return "Im!" + Base64.getUrlEncoder().withoutPadding().encodeToString(random) + "aA9";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
