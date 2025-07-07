package net.hostsharing.hsadminng.credentials.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.mapper.Array;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.test.IgnoreOnFailureExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@Tag("scenarioTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class },
        properties = {
                "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:15.5-bookworm:///scenariosTC}",
                "spring.datasource.username=${HSADMINNG_POSTGRES_ADMIN_USERNAME:ADMIN}",
                "spring.datasource.password=${HSADMINNG_POSTGRES_ADMIN_PASSWORD:password}",
                "hsadminng.superuser=${HSADMINNG_SUPERUSER:superuser-alex@hostsharing.net}"
        }
)
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@ExtendWith(IgnoreOnFailureExtension.class)
class CredentialsScenarioTests extends ScenarioTest {

    @SneakyThrows
    @BeforeEach
    protected void beforeScenario(final TestInfo testInfo) {
        super.beforeScenario(testInfo);
    }
    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CredentialScenarios {

        @Test
        @Order(1010)
        @Produces(explicitly = "Credentials@hsadmin: firby-susan", implicitly = { "Person: Susan Firby" })
        void shouldCreateInitialCredentialsForExistingNaturalPerson() {
            new CreateCredentials(scenarioTest)
                    // to find a specific existing person
                    .given("personFamilyName", "Firby")
                    .given("personGivenName", "Susan")
                    // a login name, to be stored in the new RBAC subject
                    .given("nickname", "firby-susan")
                    // initial credentials
                    .given("active", true)
                    .given("emailAddress", "susan.firby@example.com")
                    .given("telephonePassword", "securePass123")
                    .given("smsNumber", "+49123456789")
                    .given("globalUid", 21011)
                    .given("globalGid", 21011)
                    .given("contexts", Array.of(
                            Map.ofEntries(
                                    // a hardcoded context from test-data
                                    // TODO.impl: the uuid should be determined within CreateCredentials just by (HSDAMIN,prod)
                                    Map.entry("uuid", "11111111-1111-1111-1111-111111111111"),
                                    Map.entry("type", "HSADMIN"),
                                    Map.entry("qualifier", "prod")
                            )
                    ))
                    .doRun()
                    .keep();
        }
    }
}
