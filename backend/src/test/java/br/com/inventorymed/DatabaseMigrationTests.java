package br.com.inventorymed;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class DatabaseMigrationTests {

    @Container
    static final MSSQLServerContainer<?> SQL_SERVER = new MSSQLServerContainer<>(
        "mcr.microsoft.com/mssql/server:2022-latest"
    ).acceptLicense();

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        registry.add("inventory.security.jwt.secret", () ->
            "integration-test-secret-with-more-than-32-characters"
        );
    }

    @Test
    void flywayCreatesHospitalIsolatedClinicalTables() throws Exception {
        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery(
                "SELECT COUNT(*) FROM sys.foreign_keys " +
                "WHERE name IN (" +
                "'fk_admission_patient_same_hospital', " +
                "'fk_admission_bed_same_hospital', " +
                "'fk_prescription_admission_same_hospital', " +
                "'fk_evolution_admission_same_hospital')"
            )
        ) {
            assertTrue(result.next());
            assertTrue(result.getInt(1) == 4);
        }
    }
}
