package br.com.inventorymed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.identity.HospitalRole;
import br.com.inventorymed.identity.SystemRole;
import br.com.inventorymed.identity.SystemUserRole;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import br.com.inventorymed.administration.HospitalAdministrationService;
import br.com.inventorymed.administration.HospitalCreateRequest;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import jakarta.servlet.http.Cookie;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.List;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CoreSecurityIntegrationTests {

    @Container
    static final MSSQLServerContainer SQL_SERVER = new MSSQLServerContainer(
        "mcr.microsoft.com/mssql/server:2022-latest"
    )
        .acceptLicense()
        .withInitScript("sql/create-test-tenant-provisioner.sql");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private HospitalMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemUserRoleRepository systemRoleRepository;

    @Autowired
    private HospitalAdministrationService hospitalAdministrationService;

    @Autowired
    private TenantJdbcExecutor tenantJdbc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        registry.add("server.servlet.session.cookie.secure", () -> false);
        registry.add("inventory.bootstrap.enabled", () -> false);
        registry.add("inventory.tenancy.provisioning.enabled", () -> true);
        registry.add(
            "inventory.tenancy.provisioning.server-jdbc-url",
            SQL_SERVER::getJdbcUrl
        );
        registry.add(
            "inventory.tenancy.provisioning.username",
            () -> "inventorymed_test_tenant_provisioner"
        );
        registry.add(
            "inventory.tenancy.provisioning.password",
            () -> "TenantProvisioner@Test123"
        );
        registry.add(
            "inventory.tenancy.provisioning.credential-encryption-key",
            () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM dbo.SPRING_SESSION_ATTRIBUTES");
        jdbcTemplate.update("DELETE FROM dbo.SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM dbo.audit_event");
        jdbcTemplate.update("DELETE FROM dbo.hospital_database_secret");
        jdbcTemplate.update("DELETE FROM dbo.hospital_membership");
        jdbcTemplate.update("DELETE FROM dbo.system_user_role");
        jdbcTemplate.update("DELETE FROM dbo.app_user");
        jdbcTemplate.update("DELETE FROM dbo.hospital");
    }

    @Test
    void flywayCreatesOnlyTheCoreSchema() throws Exception {
        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet coreTables = statement.executeQuery(
                "SELECT COUNT(*) FROM sys.tables WHERE name IN (" +
                "'hospital', 'app_user', 'hospital_membership', 'system_user_role', " +
                "'audit_event', 'SPRING_SESSION', 'SPRING_SESSION_ATTRIBUTES')"
            )
        ) {
            assertThat(coreTables.next()).isTrue();
            assertThat(coreTables.getInt(1)).isEqualTo(7);
        }

        Integer clinicalTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys.tables WHERE name IN " +
            "('patient', 'admission', 'prescription', 'evolution')",
            Integer.class
        );
        assertThat(clinicalTableCount).isZero();
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.tables WHERE name = 'hospital_database_secret'",
                Integer.class
            )
        ).isEqualTo(1);
    }

    @Test
    void loginRejectsRequestsWithoutCsrfProtection() throws Exception {
        mockMvc
            .perform(
                post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"doctor@example.test","password":"Secret@12345"}
                        """)
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void prescriptionStarterCatalogRequiresAnAuthorizedMedicalSession() throws Exception {
        createMembership(
            "starter.doctor@example.test",
            "HOSPITAL CATÁLOGO MÉDICO",
            "inventory_med_hospital_catalogo_medico",
            HospitalRole.MEDICO
        );
        MvcResult doctorLogin = login("starter.doctor@example.test", "Secret@12345");

        mockMvc
            .perform(
                get("/clinical/prescriptions/start-options")
                    .cookie(latestCookie(doctorLogin, "INVENTORYMED_SESSION"))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clinics[0].code").value("CLINICA_MEDICA_UPA"))
            .andExpect(jsonPath("$.templates[0].code").value("ADMISSION"));

        createMembership(
            "starter.reception@example.test",
            "HOSPITAL CATÁLOGO RECEPÇÃO",
            "inventory_med_hospital_catalogo_recepcao",
            HospitalRole.RECEPCAO
        );
        MvcResult receptionLogin = login("starter.reception@example.test", "Secret@12345");

        mockMvc
            .perform(
                get("/clinical/prescriptions/start-options")
                    .cookie(latestCookie(receptionLogin, "INVENTORYMED_SESSION"))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void loginStoresTheSessionOnTheServerAndAutoSelectsASingleHospital()
        throws Exception {
        HospitalMembership membership = createMembership(
            "doctor@example.test",
            "UPA TESTE",
            "inventory_med_hospital_teste",
            HospitalRole.MEDICO
        );

        MvcResult login = login("doctor@example.test", "Secret@12345");

        login.getResponse();
        assertThat(login.getResponse().getStatus()).isEqualTo(200);
        assertThat(login.getResponse().getCookie("INVENTORYMED_SESSION")).isNotNull();
        assertThat(login.getResponse().getCookie("INVENTORYMED_SESSION").isHttpOnly())
            .isTrue();
        assertThat(login.getResponse().getContentAsString())
            .contains("\"requiresHospitalSelection\":false")
            .contains(membership.getHospital().getId().toString())
            .doesNotContain("accessToken");

        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        mockMvc
            .perform(get("/auth/me").cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.selectedHospitalId")
                    .value(membership.getHospital().getId().toString())
            )
            .andExpect(jsonPath("$.selectedHospitalRole").value("MEDICO"));

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.SPRING_SESSION",
                Integer.class
            )
        ).isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_event WHERE event_type = 'AUTHENTICATION_LOGIN' " +
                "AND outcome = 'SUCCESS'",
                Integer.class
            )
        ).isEqualTo(1);
    }

    @Test
    void userWithMultipleHospitalsMustSelectAnAuthorizedHospital() throws Exception {
        HospitalMembership first = createMembership(
            "doctor@example.test",
            "UPA TESTE",
            "inventory_med_hospital_teste_a",
            HospitalRole.MEDICO
        );
        Hospital secondHospital = hospitalRepository.save(
            new Hospital(
                "HOSPITAL TESTE",
                "HT",
                "JOÃO PINHEIRO, MG",
                "inventory_med_hospital_teste_b"
            )
        );
        membershipRepository.save(
            new HospitalMembership(
                secondHospital,
                first.getUser(),
                HospitalRole.RESPONSAVEL_CLINICO
            )
        );

        MvcResult login = login("doctor@example.test", "Secret@12345");
        assertThat(login.getResponse().getContentAsString())
            .contains("\"requiresHospitalSelection\":true")
            .doesNotContain("selectedHospitalId");

        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        Cookie csrfCookie = latestCookie(login, "XSRF-TOKEN");

        mockMvc
            .perform(
                get("/api/v1/auth/csrf")
                    .contextPath("/api/v1")
                    .cookie(sessionCookie)
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/auth/select-hospital")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"hospitalId\":\"" + secondHospital.getId() + "\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hospital.id").value(secondHospital.getId().toString()))
            .andExpect(jsonPath("$.hospital.role").value("RESPONSAVEL_CLINICO"));
    }

    @Test
    void invalidCredentialsReturnAGenericErrorAndAreAudited() throws Exception {
        createMembership(
            "doctor@example.test",
            "UPA TESTE",
            "inventory_med_hospital_teste",
            HospitalRole.MEDICO
        );
        CsrfCookie csrf = csrfCookie();

        mockMvc
            .perform(
                post("/auth/login")
                    .cookie(csrf.cookie())
                    .header("X-XSRF-TOKEN", csrf.cookie().getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"doctor@example.test","password":"wrong-password"}
                        """)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));

        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_event WHERE event_type = 'AUTHENTICATION_LOGIN' " +
                "AND outcome = 'FAILURE'",
                Integer.class
            )
        ).isEqualTo(1);
    }

    @Test
    void revokingTheLastHospitalMembershipInvalidatesAnExistingSession()
        throws Exception {
        HospitalMembership membership = createMembership(
            "doctor@example.test",
            "UPA TESTE",
            "inventory_med_hospital_teste",
            HospitalRole.MEDICO
        );
        MvcResult login = login("doctor@example.test", "Secret@12345");
        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");

        jdbcTemplate.update(
            "UPDATE dbo.hospital_membership SET active = 0 WHERE id = ?",
            membership.getId()
        );

        mockMvc
            .perform(get("/auth/me").cookie(sessionCookie))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
    }

    @Test
    void systemAdministratorCreatesAnIsolatedHospitalDatabase() throws Exception {
        AppUser administrator = createSystemAdministrator("admin@example.test");
        MvcResult login = login("admin@example.test", "AdminSecret@123");
        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        Cookie csrfCookie = latestCookie(login, "XSRF-TOKEN");

        MvcResult created = mockMvc
            .perform(
                post("/administration/hospitals")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name":"Hospital Isolado",
                          "shortName":"HI",
                          "city":"João Pinheiro, MG"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("HOSPITAL ISOLADO"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

        Hospital hospital = hospitalRepository
            .findByNameIgnoreCase("HOSPITAL ISOLADO")
            .orElseThrow();
        assertThat(hospital.isActive()).isTrue();
        assertThat(created.getResponse().getContentAsString())
            .doesNotContain(hospital.getDatabaseName())
            .doesNotContain("password");
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys.databases WHERE name = ?",
                Integer.class,
                hospital.getDatabaseName()
            )
        ).isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.hospital_database_secret WHERE hospital_id = ? " +
                "AND encrypted_password NOT LIKE '%AdminSecret%'",
                Integer.class,
                hospital.getId()
            )
        ).isEqualTo(1);

        try (
            Connection tenantConnection = java.sql.DriverManager.getConnection(
                databaseUrl(hospital.getDatabaseName()),
                SQL_SERVER.getUsername(),
                SQL_SERVER.getPassword()
            );
            Statement statement = tenantConnection.createStatement();
            ResultSet tables = statement.executeQuery(
                "SELECT COUNT(*) FROM sys.tables WHERE name IN " +
                "('tenant_metadata', 'care_unit', 'room', 'bed', 'clinical_audit_event')"
            )
        ) {
            assertThat(tables.next()).isTrue();
            assertThat(tables.getInt(1)).isEqualTo(5);
        }
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_event WHERE actor_user_id = ? " +
                "AND event_type = 'ADMIN_HOSPITAL_CREATED' AND outcome = 'SUCCESS'",
                Integer.class,
                administrator.getId()
            )
        ).isEqualTo(1);
    }

    @Test
    void systemAdministratorCreatesAUserScopedToOneHospital() throws Exception {
        createSystemAdministrator("admin@example.test");
        Hospital hospital = hospitalRepository.save(
            new Hospital(
                "HOSPITAL AUTORIZADO",
                "HA",
                "JOÃO PINHEIRO, MG",
                "inventory_med_hospital_authorized_" + UUID.randomUUID().toString().replace("-", "")
            )
        );
        MvcResult login = login("admin@example.test", "AdminSecret@123");
        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        Cookie csrfCookie = latestCookie(login, "XSRF-TOKEN");

        mockMvc
            .perform(
                post("/administration/users")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "fullName":"Gestor Hospitalar",
                          "email":"gestor@example.test",
                          "initialPassword":"InitialSecret@123",
                          "systemRoles":[],
                          "hospitalAssignments":[
                            {"hospitalId":"%s","role":"ADMIN_HOSPITAL"}
                          ]
                        }
                        """.formatted(hospital.getId())
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("GESTOR HOSPITALAR"))
            .andExpect(jsonPath("$.mustChangePassword").value(true))
            .andExpect(jsonPath("$.hospitals[0].hospitalId").value(hospital.getId().toString()))
            .andExpect(jsonPath("$.hospitals[0].role").value("ADMIN_HOSPITAL"));

        AppUser created = userRepository
            .findByEmailIgnoreCase("gestor@example.test")
            .orElseThrow();
        assertThat(created.mustChangePassword()).isTrue();
        assertThat(
            membershipRepository.existsByUserIdAndHospitalId(
                created.getId(),
                hospital.getId()
            )
        ).isTrue();
    }

    @Test
    void ordinaryHospitalUserCannotAccessSystemAdministration() throws Exception {
        createMembership(
            "doctor@example.test",
            "UPA TESTE",
            "inventory_med_hospital_teste",
            HospitalRole.MEDICO
        );
        MvcResult login = login("doctor@example.test", "Secret@12345");

        mockMvc
            .perform(
                get("/administration/hospitals")
                    .cookie(latestCookie(login, "INVENTORYMED_SESSION"))
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void userMustReplaceTheInitialPasswordBeforeAccessingTheSystem()
        throws Exception {
        Hospital hospital = hospitalRepository.save(
            new Hospital(
                "HOSPITAL PRIMEIRO ACESSO",
                "HPA",
                "JOÃO PINHEIRO, MG",
                "inventory_med_hospital_first_access"
            )
        );
        AppUser user = userRepository.save(
            new AppUser(
                "GESTOR PRIMEIRO ACESSO",
                "first.access@example.test",
                passwordEncoder.encode("InitialSecret@123"),
                true
            )
        );
        membershipRepository.save(
            new HospitalMembership(hospital, user, HospitalRole.ADMIN_HOSPITAL)
        );

        MvcResult login = login(
            "first.access@example.test",
            "InitialSecret@123"
        );
        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        Cookie csrfCookie = latestCookie(login, "XSRF-TOKEN");

        mockMvc
            .perform(
                post("/auth/select-hospital")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"hospitalId\":\"" + hospital.getId() + "\"}")
            )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mockMvc
            .perform(
                post("/auth/change-password")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "currentPassword":"InitialSecret@123",
                          "newPassword":"PersonalSecret@456"
                        }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mustChangePassword").value(false));

        mockMvc
            .perform(get("/auth/me").cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.mustChangePassword").value(false));
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.audit_event WHERE actor_user_id = ? " +
                "AND event_type = 'AUTHENTICATION_PASSWORD_CHANGED' AND outcome = 'SUCCESS'",
                Integer.class,
                user.getId()
            )
        ).isEqualTo(1);
    }

    @Test
    void medicalWorkspaceIsIsolatedAndFinalizedDocumentCannotBeOverwritten()
        throws Exception {
        AppUser administrator = createSystemAdministrator("isolation.admin@example.test");
        InventoryUserPrincipal administratorPrincipal = new InventoryUserPrincipal(
            administrator.getId(),
            administrator.getFullName(),
            administrator.getEmail(),
            null,
            true,
            false,
            List.of(SystemRole.ADMIN_SISTEMA.name())
        );
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        hospitalAdministrationService.create(
            new HospitalCreateRequest("HOSPITAL A " + suffix, "HA", "JOÃO PINHEIRO, MG"),
            administratorPrincipal,
            "127.0.0.1",
            "integration-test"
        );
        hospitalAdministrationService.create(
            new HospitalCreateRequest("HOSPITAL B " + suffix, "HB", "JOÃO PINHEIRO, MG"),
            administratorPrincipal,
            "127.0.0.1",
            "integration-test"
        );
        Hospital firstHospital = hospitalRepository
            .findByNameIgnoreCase("HOSPITAL A " + suffix)
            .orElseThrow();
        Hospital secondHospital = hospitalRepository
            .findByNameIgnoreCase("HOSPITAL B " + suffix)
            .orElseThrow();
        UUID firstBedId = insertStructure(firstHospital.getId(), "UNIDADE_A");
        insertStructure(secondHospital.getId(), "UNIDADE_B");

        AppUser doctor = userRepository.save(
            new AppUser(
                "MÉDICO ISOLAMENTO",
                "isolation.doctor@example.test",
                passwordEncoder.encode("Secret@12345")
            )
        );
        membershipRepository.save(
            new HospitalMembership(firstHospital, doctor, HospitalRole.MEDICO)
        );

        MvcResult login = login("isolation.doctor@example.test", "Secret@12345");
        Cookie sessionCookie = latestCookie(login, "INVENTORYMED_SESSION");
        Cookie csrfCookie = latestCookie(login, "XSRF-TOKEN");

        mockMvc
            .perform(get("/clinical/workspace").cookie(sessionCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hospitalId").value(firstHospital.getId().toString()))
            .andExpect(jsonPath("$.careUnits[0].code").value("UNIDADE_A"));

        MvcResult admitted = mockMvc
            .perform(
                post("/clinical/admissions")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "bedId":"%s",
                          "fullName":"Paciente Isolamento",
                          "birthDate":null,
                          "sex":"NAO_INFORMADO",
                          "weightKg":null,
                          "diagnosis":null,
                          "comorbidities":null,
                          "allergies":null
                        }
                        """.formatted(firstBedId)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.patient.fullName").value("PACIENTE ISOLAMENTO"))
            .andReturn();
        UUID admissionId = firstUuid(admitted.getResponse().getContentAsString(), "id");

        mockMvc
            .perform(
                put("/clinical/admissions/" + admissionId + "/patient")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "fullName":"Paciente Atualizado",
                          "birthDate":"1990-04-20",
                          "sex":"FEMININO",
                          "weightKg":62.5,
                          "diagnosis":"Diagnóstico atualizado",
                          "comorbidities":null,
                          "allergies":"Nega"
                        }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patient.fullName").value("PACIENTE ATUALIZADO"))
            .andExpect(jsonPath("$.patient.weightKg").value(62.5));

        Integer patientUpdateAudit = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.clinical_audit_event " +
                "WHERE event_type = 'PATIENT_REGISTRATION_UPDATED' AND actor_user_id = ?",
                Integer.class,
                doctor.getId()
            )
        );
        assertThat(patientUpdateAudit).isEqualTo(1);

        AppUser receptionist = userRepository.save(
            new AppUser(
                "RECEPÇÃO ISOLAMENTO",
                "isolation.reception@example.test",
                passwordEncoder.encode("Secret@12345")
            )
        );
        membershipRepository.save(
            new HospitalMembership(firstHospital, receptionist, HospitalRole.RECEPCAO)
        );
        MvcResult receptionLogin = login("isolation.reception@example.test", "Secret@12345");
        Cookie receptionSession = latestCookie(receptionLogin, "INVENTORYMED_SESSION");
        Cookie receptionCsrf = latestCookie(receptionLogin, "XSRF-TOKEN");
        mockMvc
            .perform(
                put("/clinical/admissions/" + admissionId + "/patient")
                    .cookie(receptionSession, receptionCsrf)
                    .header("X-XSRF-TOKEN", receptionCsrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "fullName":"Alteração Indevida",
                          "birthDate":null,
                          "sex":"NAO_INFORMADO",
                          "weightKg":null,
                          "diagnosis":null,
                          "comorbidities":null,
                          "allergies":null
                        }
                        """
                    )
            )
            .andExpect(status().isForbidden());
        String patientNameAfterForbiddenUpdate = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT p.full_name FROM dbo.patient p JOIN dbo.admission a ON a.patient_id = p.id " +
                "WHERE a.id = ?",
                String.class,
                admissionId
            )
        );
        assertThat(patientNameAfterForbiddenUpdate).isEqualTo("PACIENTE ATUALIZADO");

        mockMvc
            .perform(
                post("/clinical/medication-therapy/preview")
                    .cookie(receptionSession, receptionCsrf)
                    .header("X-XSRF-TOKEN", receptionCsrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "clinicalContext":"WARD_HOSPITAL",
                          "antimicrobials":[],
                          "prophylaxes":[],
                          "continuousMedications":[],
                          "analgesiaSymptomatics":[]
                        }
                        """
                    )
            )
            .andExpect(status().isForbidden());

        mockMvc
            .perform(
                post("/clinical/procedures/preview")
                    .cookie(receptionSession, receptionCsrf)
                    .header("X-XSRF-TOKEN", receptionCsrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "selectedTemplate":"THORACENTESIS",
                          "items":[{
                            "id":1,
                            "recordType":"REQUESTED",
                            "procedureCode":"THORACENTESIS",
                            "clinicalIndication":"DERRAME PLEURAL",
                            "cid10Reference":"J90",
                            "anatomicalSite":"HEMITÓRAX",
                            "laterality":"RIGHT",
                            "asepsisAntisepsis":"CLOREXIDINA",
                            "sterileBarrier":"BARREIRA ESTÉRIL MÁXIMA",
                            "postProcedureControl":"NOT_APPLICABLE",
                            "monitoringAssistance":"ECG, SPO2 E PANI",
                            "urgency":"URGENT"
                          }]
                        }
                        """
                    )
            )
            .andExpect(status().isForbidden());

        UUID templateVersionId = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT TOP 1 v.id FROM dbo.form_template_version v " +
                "JOIN dbo.form_template t ON t.id = v.template_id " +
                "WHERE t.kind = 'PRESCRIPTION' AND v.status = 'PUBLISHED'",
                UUID.class
            )
        );
        List<String> structuredSectionOrder = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForList(
                "SELECT section_key FROM dbo.form_section WHERE version_id = ? " +
                    "AND section_key IN ('REABILITACAO', 'PRECAUCOES_ISOLAMENTO', 'SUPORTE_TERAPEUTICO', 'CUIDADOS_CRITICOS', 'TERAPIA_MEDICAMENTOSA') " +
                    "ORDER BY display_order",
                String.class,
                templateVersionId
            )
        );
        assertThat(structuredSectionOrder).containsExactly(
            "REABILITACAO",
            "PRECAUCOES_ISOLAMENTO",
            "SUPORTE_TERAPEUTICO",
            "CUIDADOS_CRITICOS",
            "TERAPIA_MEDICAMENTOSA"
        );
        Integer activeLegacyMedicationFields = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.form_field f " +
                    "JOIN dbo.form_section s ON s.id = f.section_id " +
                    "WHERE s.version_id = ? AND s.section_key = 'MEDICAMENTOS' " +
                    "AND f.field_key IN ('ANALGESIA', 'SINTOMATICOS', 'PROFILAXIA', 'ATB', 'USO_CONTINUO') " +
                    "AND f.active = 1",
                Integer.class,
                templateVersionId
            )
        );
        assertThat(activeLegacyMedicationFields).isZero();
        MvcResult finalized = mockMvc
            .perform(
                post("/clinical/admissions/" + admissionId + "/documents")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateVersionId":"%s",
                          "kind":"PRESCRIPTION",
                          "values":{
                            "ORIENTACOES.DIETA":{
                              "type":"ORAL",
                              "oral":{
                                "consistency":"BRANDA",
                                "restrictions":["HIPOSSÓDICA (HAS / CARDIOLOGIA)"]
                              },
                              "enteral":null,
                              "parenteral":null,
                              "fasting":null
                            },
                            "MONITORIZACAO.CONTROLES":{
                              "vitalSigns":{
                                "frequency":"6/6H",
                                "painScale":"EVA",
                                "consciousnessSedationScale":"",
                                "fallRiskScale":"MORSE"
                              },
                              "glucoseMonitoring":{
                                "frequency":"6/6H",
                                "hypoglycemiaProtocol":true,
                                "slidingScale":true,
                                "insulinType":"REGULAR"
                              },
                              "fluidBalanceOutputs":{
                                "fluidBalance":"SEM INDICAÇÃO",
                                "urineOutput":"DIURESE ESPONTÂNEA",
                                "drainsTubes":[],
                                "otherMeasurements":[]
                              },
                              "invasiveMonitoring":{
                                "hemodynamic":[],
                                "neurological":[]
                              }
                            },
                            "SUPORTE_VENTILATORIO.PLANO":{
                              "selectedTemplate":"NASAL_CANNULA_2L_PRN_92",
                              "items":[{
                                "id":1,
                                "supportType":"LOW_FLOW",
                                "frequency":"PRN_SPO2_92",
                                "scheduling":"PRN",
                                "lowFlow":{
                                  "device":"NASAL_CANNULA",
                                  "oxygenFlowLitersMinute":2,
                                  "fio2Percent":null
                                },
                                "highFlow":null,
                                "nonInvasive":null,
                                "invasive":null
                              }]
                            },
                            "REABILITACAO.PLANO":{
                              "selectedTemplate":"MOTOR_PASSIVE_DAILY",
                              "items":[{
                                "id":1,
                                "specialty":"MOTOR_PHYSIOTHERAPY",
                                "procedure":"PASSIVE_MOBILIZATION",
                                "frequency":"DAILY",
                                "scheduling":"FIXED",
                                "clinicalJustification":""
                              }]
                            },
                            "PRECAUCOES_ISOLAMENTO.PLANO":{
                              "selectedTemplate":"CONTACT_KPC_MDR",
                              "items":[{
                                "id":1,
                                "precautionType":"CONTACT",
                                "reasonPathogen":"COLONIZAÇÃO POR KPC / ENTEROBACTÉRIA MULTIRRESISTENTE",
                                "durationReview":"ENTIRE_HOSPITALIZATION",
                                "scheduling":"CONTINUOUS"
                              }]
                            },
                            "SUPORTE_TERAPEUTICO.PLANO":{
                              "clinicalContext":"ICU",
                              "hydrationSolutions":[{
                                "id":1,
                                "baseSolution":"SF09_500",
                                "additives":["KCL191_10"],
                                "route":"EV",
                                "frequency":"EVERY_12_HOURS",
                                "infusionMode":"PUMP",
                                "rateValue":42,
                                "rateUnit":"ML_H",
                                "scheduling":"FIXED"
                              }],
                              "glucoseControl":{
                                "frequency":"EVERY_4_HOURS",
                                "hypoglycemiaProtocolActive":true,
                                "correctionScaleActive":true,
                                "insulinType":"REGULAR",
                                "continuousPump":false
                              },
                              "bloodProducts":[{
                                "id":1,
                                "product":"RBC",
                                "modifications":["LEUKOREDUCED"],
                                "quantity":1,
                                "quantityUnit":"UNIT",
                                "route":"DEDICATED_ACCESS",
                                "infusionMinutes":180,
                                "preMedications":[],
                                "scheduling":"URGENT"
                              }]
                            },
                            "CUIDADOS_CRITICOS.PLANO":{
                              "clinicalContext":"ICU",
                              "vasoactiveDrugs":[{
                                "id":1,
                                "drug":"NOREPINEPHRINE",
                                "dilution":"16 MG EM SG 5%% 250 ML",
                                "finalConcentration":"64 MCG/ML",
                                "initialRate":10,
                                "rateUnit":"ML_H",
                                "vascularAccess":"CVC",
                                "bloodPressureMonitoring":"INVASIVE_ARTERIAL",
                                "therapeuticGoal":"TITULAR PARA MANTER PAM ≥ 65 MMHG",
                                "scheduling":"CONTINUOUS"
                              }],
                              "sedationAnalgesiaBnm":[],
                              "emergencyMedications":[]
                            },
                            "TERAPIA_MEDICAMENTOSA.PLANO":{
                              "clinicalContext":"ICU",
                              "renalFunction":{
                                "measure":"CREATININE_CLEARANCE",
                                "valueMlMin":72
                              },
                              "bleedingRisk":{
                                "plateletCount":180000,
                                "activeBleeding":false
                              },
                              "antimicrobials":[{
                                "id":1,
                                "drug":"CEFTRIAXONE",
                                "customDrug":"",
                                "dosePreparation":"1 G + 100 ML DE SF 0,9%%",
                                "route":"EV",
                                "administrationMode":"RAPID_INFUSION",
                                "diluent":"SF_09_100",
                                "infusionSet":"MACRODRIP",
                                "frequency":"12/12H",
                                "scheduling":"FIXED",
                                "loadingDose":"",
                                "conditionalTrigger":"",
                                "treatmentDay":2,
                                "infectionFocus":"PNEUMONIA",
                                "ccihStatus":"AUTHORIZED",
                                "ccihOpinion":"PARECER CCIH 123",
                                "renalDoseAssessment":"NO_ADJUSTMENT_REQUIRED"
                              }],
                              "prophylaxes":[{
                                "id":1,
                                "intervention":"ENOXAPARIN",
                                "dosePreparation":"40 MG",
                                "route":"SC",
                                "frequency":"24/24H",
                                "scheduling":"FIXED",
                                "conditionalTrigger":"",
                                "suspensionReason":""
                              }],
                              "continuousMedications":[],
                              "analgesiaSymptomatics":[]
                            }
                          },
                          "finalizeDocument":true
                        }
                        """.formatted(templateVersionId)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FINALIZED"))
            .andExpect(
                jsonPath("$.values['ORIENTACOES.DIETA'].structuredDiet.summaryLine")
                    .value("DIETA ORAL BRANDA, HIPOSSÓDICA (HAS / CARDIOLOGIA).")
            )
            .andExpect(
                jsonPath("$.values['MONITORIZACAO.CONTROLES'].structuredMonitoring.summaryLine")
                    .value(org.hamcrest.Matchers.containsString("SSVV 6/6H"))
            )
            .andExpect(
                jsonPath("$.values['SUPORTE_VENTILATORIO.PLANO'].structuredVentilatorySupport.summaryLine")
                    .value(org.hamcrest.Matchers.containsString("CATETER NASAL DE O₂"))
            )
            .andExpect(
                jsonPath("$.values['REABILITACAO.PLANO'].structuredRehabilitation.summaryLine")
                    .value(org.hamcrest.Matchers.containsString("MOBILIZAÇÃO PASSIVA"))
            )
            .andExpect(
                jsonPath("$.values['PRECAUCOES_ISOLAMENTO.PLANO'].structuredIsolation.summaryLine")
                    .value(org.hamcrest.Matchers.containsString("PRECAUÇÃO DE CONTATO"))
            )
            .andExpect(
                jsonPath("$.values['PRECAUCOES_ISOLAMENTO.PLANO'].billingAudit.auditAlerts[0]")
                    .value(org.hamcrest.Matchers.containsString("CULTURA PRÉVIA OU SWAB"))
            )
            .andExpect(
                jsonPath("$.values['SUPORTE_TERAPEUTICO.PLANO'].structuredTherapeuticSupport.prescriptionDetails")
                    .value(org.hamcrest.Matchers.containsString("10. HEMODERIVADOS E TRANSFUSÕES"))
            )
            .andExpect(
                jsonPath("$.values['SUPORTE_TERAPEUTICO.PLANO'].billingAudit.auditAlerts[0]")
                    .value(org.hamcrest.Matchers.containsString("KCL CONCENTRADO"))
            )
            .andExpect(
                jsonPath("$.values['CUIDADOS_CRITICOS.PLANO'].structuredCriticalCare.prescriptionDetails")
                    .value(org.hamcrest.Matchers.containsString("14. DROGAS VASOATIVAS E INOTRÓPICOS"))
            )
            .andExpect(
                jsonPath("$.values['CUIDADOS_CRITICOS.PLANO'].billingAudit.auditAlerts[0]")
                    .value(org.hamcrest.Matchers.containsString("ALTA VIGILÂNCIA"))
            )
            .andExpect(
                jsonPath("$.values['TERAPIA_MEDICAMENTOSA.PLANO'].structuredMedicationTherapy.prescriptionDetails")
                    .value(org.hamcrest.Matchers.containsString("11. ANTIMICROBIANOS E ANTIBIOTICOTERAPIA"))
            )
            .andExpect(
                jsonPath("$.values['TERAPIA_MEDICAMENTOSA.PLANO'].billingAudit.suppliesEquipmentForReview[0]")
                    .value(org.hamcrest.Matchers.containsString("SERINGA"))
            )
            .andReturn();
        UUID documentId = firstUuid(finalized.getResponse().getContentAsString(), "id");

        UUID evolutionTemplateVersionId = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT TOP 1 v.id FROM dbo.form_template_version v " +
                "JOIN dbo.form_template t ON t.id = v.template_id " +
                "WHERE t.kind = 'EVOLUTION' AND v.status = 'PUBLISHED'",
                UUID.class
            )
        );
        mockMvc
            .perform(
                post("/clinical/admissions/" + admissionId + "/documents")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateVersionId":"%s",
                          "kind":"EVOLUTION",
                          "values":{
                            "ACOMPANHAMENTO.HIGIENE":"BOA",
                            "FISIOLOGICO.VOLUME_URINARIO":1200.5,
                            "FISIOLOGICO.DIAS_SEM_EVACUAR":2,
                            "FISIOLOGICO.EVOLUCAO":"PACIENTE ESTÁVEL",
                            "NEUROLOGICO.INTERACAO":"COOPERATIVO",
                            "SEDACAO.MEDICAMENTOS":[{
                              "description":"PROPOFOL (10 MG/ML)",
                              "route":"EV",
                              "frequency":"12 ML/H",
                              "scheduling":"CONTÍNUO"
                            }],
                            "RESPIRATORIO.PADRAO":["SEM_ESFORCO"],
                            "CARDIOVASCULAR.PAM_ALVO":true
                          },
                          "finalizeDocument":true
                        }
                        """.formatted(evolutionTemplateVersionId)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("FINALIZED"))
            .andExpect(jsonPath("$.kind").value("EVOLUTION"))
            .andExpect(jsonPath("$.values['NEUROLOGICO.INTERACAO']").value("COOPERATIVO"));

        mockMvc
            .perform(
                post("/clinical/admissions/" + admissionId + "/documents")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateVersionId":"%s",
                          "kind":"EVOLUTION",
                          "values":{
                            "SEDACAO.MEDICAMENTOS":[{
                              "description":"FENTANIL",
                              "route":"EV",
                              "frequency":"",
                              "scheduling":"CONTÍNUO"
                            }]
                          },
                          "finalizeDocument":true
                        }
                        """.formatted(evolutionTemplateVersionId)
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.message")
                    .value("Informe o medicamento e a vazão em ML/H para cada infusão selecionada")
            );

        mockMvc
            .perform(
                put("/clinical/documents/" + documentId)
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateVersionId":"%s",
                          "kind":"PRESCRIPTION",
                          "values":{},
                          "finalizeDocument":false
                        }
                        """.formatted(templateVersionId)
                    )
            )
            .andExpect(status().isBadRequest());

        UUID procedureTemplateVersionId = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT TOP 1 v.id FROM dbo.form_template_version v " +
                    "JOIN dbo.form_template t ON t.id = v.template_id " +
                    "WHERE t.kind = 'PROCEDURE' AND v.status = 'PUBLISHED'",
                UUID.class
            )
        );
        String procedureFieldType = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT f.field_type FROM dbo.form_field f " +
                    "JOIN dbo.form_section s ON s.id = f.section_id " +
                    "WHERE s.version_id = ? AND s.section_key = 'PROCEDURE' AND f.field_key = 'RECORD'",
                String.class,
                procedureTemplateVersionId
            )
        );
        assertThat(procedureFieldType).isEqualTo("PROCEDURE_PLAN");

        mockMvc
            .perform(
                post("/clinical/admissions/" + admissionId + "/documents")
                    .cookie(sessionCookie, csrfCookie)
                    .header("X-XSRF-TOKEN", csrfCookie.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateVersionId":"%s",
                          "kind":"PROCEDURE",
                          "values":{
                            "PROCEDURE.RECORD":{
                              "selectedTemplate":"CVC",
                              "items":[{
                                "id":1,
                                "recordType":"REQUESTED",
                                "procedureCode":"CVC",
                                "customProcedure":"",
                                "clinicalIndication":"ACESSO VASCULAR PARA TERAPIA ENDOVENOSA",
                                "cid10Reference":"Z45.2",
                                "anatomicalSite":"VEIA JUGULAR INTERNA",
                                "laterality":"RIGHT",
                                "asepsisAntisepsis":"CLOREXIDINA DEGERMANTE 2%% E ALCOÓLICA 0,5%%",
                                "sterileBarrier":"BARREIRA ESTÉRIL MÁXIMA",
                                "localAnesthesia":"LIDOCAÍNA 2%%",
                                "imageGuidance":"POCUS EM TEMPO REAL",
                                "deviceName":"KIT CVC DUPLO LÚMEN",
                                "deviceBrand":"",
                                "deviceCaliber":"7 FR X 20 CM",
                                "deviceLot":"",
                                "anvisaRegistration":"",
                                "fixationDressingConnections":"MONONYLON E FILME TRANSPARENTE",
                                "samplesLaboratory":"SEM AMOSTRAS",
                                "postProcedureControl":"CHEST_XRAY",
                                "postProcedureDetails":"",
                                "monitoringAssistance":"ECG, SPO2, PANI E ENFERMAGEM",
                                "urgency":"URGENT",
                                "techniqueOutcome":"",
                                "complications":"",
                                "performedAt":null
                              }]
                            }
                          },
                          "finalizeDocument":true
                        }
                        """.formatted(procedureTemplateVersionId)
                    )
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.kind").value("PROCEDURE"))
            .andExpect(jsonPath("$.status").value("FINALIZED"))
            .andExpect(
                jsonPath("$.values['PROCEDURE.RECORD'].structuredProcedures.prescriptionDetails")
                    .value(org.hamcrest.Matchers.startsWith(
                        "11. PROCEDIMENTOS E INTERVENÇÕES BEIRA-LEITO:"
                    ))
            );

        Integer firstHospitalProcedures = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.clinical_document WHERE kind = 'PROCEDURE'",
                Integer.class
            )
        );
        Integer secondHospitalProcedures = tenantJdbc.read(secondHospital.getId(), jdbc ->
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.clinical_document WHERE kind = 'PROCEDURE'",
                Integer.class
            )
        );
        assertThat(firstHospitalProcedures).isEqualTo(1);
        assertThat(secondHospitalProcedures).isZero();

        Integer firstHospitalPatients = tenantJdbc.read(firstHospital.getId(), jdbc ->
            jdbc.queryForObject("SELECT COUNT(*) FROM dbo.patient", Integer.class)
        );
        Integer secondHospitalPatients = tenantJdbc.read(secondHospital.getId(), jdbc ->
            jdbc.queryForObject("SELECT COUNT(*) FROM dbo.patient", Integer.class)
        );
        assertThat(firstHospitalPatients).isEqualTo(1);
        assertThat(secondHospitalPatients).isZero();
    }

    private HospitalMembership createMembership(
        String email,
        String hospitalName,
        String databaseName,
        HospitalRole role
    ) {
        Hospital hospital = hospitalRepository.save(
            new Hospital(hospitalName, "TESTE", "JOÃO PINHEIRO, MG", databaseName)
        );
        AppUser user = userRepository
            .findByEmailIgnoreCase(email)
            .orElseGet(() ->
                userRepository.save(
                    new AppUser(
                        "PROFISSIONAL TESTE",
                        email,
                        passwordEncoder.encode("Secret@12345")
                    )
                )
            );
        return membershipRepository.save(new HospitalMembership(hospital, user, role));
    }

    private UUID insertStructure(UUID hospitalId, String code) {
        return tenantJdbc.write(hospitalId, jdbc -> {
            UUID unitId = UUID.randomUUID();
            UUID roomId = UUID.randomUUID();
            UUID bedId = UUID.randomUUID();
            jdbc.update(
                "INSERT INTO dbo.care_unit (id, name, code, display_order, active) " +
                "VALUES (?, ?, ?, 10, 1)",
                unitId,
                code,
                code
            );
            jdbc.update(
                "INSERT INTO dbo.room (id, care_unit_id, name, code, display_order, active) " +
                "VALUES (?, ?, ?, 'Q1', 10, 1)",
                roomId,
                unitId,
                "QUARTO 1"
            );
            jdbc.update(
                "INSERT INTO dbo.bed (id, room_id, code, status, display_order, active) " +
                "VALUES (?, ?, 'A', 'AVAILABLE', 10, 1)",
                bedId,
                roomId
            );
            return bedId;
        });
    }

    private UUID firstUuid(String json, String field) {
        var matcher = Pattern.compile("\\\"" + field + "\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        assertThat(matcher.find()).isTrue();
        return UUID.fromString(matcher.group(1));
    }

    private MvcResult login(String email, String password) throws Exception {
        CsrfCookie csrf = csrfCookie();
        return mockMvc
            .perform(
                post("/auth/login")
                    .cookie(csrf.cookie())
                    .header("X-XSRF-TOKEN", csrf.cookie().getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"
                    )
            )
            .andExpect(status().isOk())
            .andExpect(cookie().httpOnly("INVENTORYMED_SESSION", true))
            .andReturn();
    }

    private AppUser createSystemAdministrator(String email) {
        AppUser user = userRepository.save(
            new AppUser(
                "ADMINISTRADOR TESTE",
                email,
                passwordEncoder.encode("AdminSecret@123")
            )
        );
        systemRoleRepository.save(
            new SystemUserRole(user, SystemRole.ADMIN_SISTEMA)
        );
        return user;
    }

    private String databaseUrl(String databaseName) {
        String jdbcUrl = SQL_SERVER.getJdbcUrl();
        if (jdbcUrl.matches("(?i).*databaseName=[^;]*.*")) {
            return jdbcUrl.replaceFirst(
                "(?i)databaseName=[^;]*",
                "databaseName=" + databaseName
            );
        }
        return jdbcUrl + ";databaseName=" + databaseName;
    }

    private CsrfCookie csrfCookie() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("XSRF-TOKEN"))
            .andReturn();
        return new CsrfCookie(result.getResponse().getCookie("XSRF-TOKEN"));
    }

    private Cookie latestCookie(MvcResult result, String name) {
        Cookie latest = null;
        for (Cookie cookie : result.getResponse().getCookies()) {
            if (
                name.equals(cookie.getName()) &&
                cookie.getMaxAge() != 0 &&
                cookie.getValue() != null &&
                !cookie.getValue().isBlank()
            ) {
                latest = cookie;
            }
        }
        assertThat(latest).as("cookie ativo %s", name).isNotNull();
        return latest;
    }

    private record CsrfCookie(Cookie cookie) {}
}
