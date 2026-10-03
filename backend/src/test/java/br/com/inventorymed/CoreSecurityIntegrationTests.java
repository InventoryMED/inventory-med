package br.com.inventorymed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import jakarta.servlet.http.Cookie;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
    ).acceptLicense();

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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
        registry.add("spring.datasource.username", SQL_SERVER::getUsername);
        registry.add("spring.datasource.password", SQL_SERVER::getPassword);
        registry.add("server.servlet.session.cookie.secure", () -> false);
        registry.add("inventory.bootstrap.enabled", () -> false);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM dbo.SPRING_SESSION_ATTRIBUTES");
        jdbcTemplate.update("DELETE FROM dbo.SPRING_SESSION");
        jdbcTemplate.update("DELETE FROM dbo.audit_event");
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
