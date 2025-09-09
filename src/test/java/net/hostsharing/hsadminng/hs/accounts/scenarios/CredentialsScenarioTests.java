package net.hostsharing.hsadminng.hs.accounts.scenarios;

import lombok.SneakyThrows;
import net.hostsharing.hsadminng.hs.scenarios.Produces;
import net.hostsharing.hsadminng.hs.scenarios.Requires;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import net.hostsharing.hsadminng.mapper.Array;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;

class CredentialsScenarioTests extends ScenarioTest {

    @SneakyThrows
    @BeforeEach
    protected void beforeScenario(final TestInfo testInfo) {
        super.beforeScenario(testInfo);
    }

    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RbacContextScenarios {

        @Test
        @Order(1010)
        @Produces("RBAC Context")
        void shouldFetchRbacContext() {
            new FetchRbacContext(scenarioTest)
                    .given("subjectName", "superuser-fran@hostsharing.net")
                    .given("assumedRoles", "rbactest.package#xxx00:ADMIN;rbactest.package#yyy00:ADMIN")
                    .given("expectedToBeGlobalAdmin", true)
                    .doRun()
                    .keep();
        }
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CurrentLoginUserScenarios {

        @Test
        @Order(2010)
        @Produces("Current Login User")
        void shouldFetchCurrentLoginUser() {
            new CurrentLoginUser(scenarioTest)
                    .given("subjectName", "superuser-fran@hostsharing.net")
                    .given("personGivenName", "Fran")
                    .given("expectedToBeGlobalAdmin", true)
                    .doRun()
                    .keep();
        }
    }

    @Nested
    @Order(30)
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
                    .given("smsNumber", "+49123456789")
                    .given("globalUid", 21011)
                    .given("globalGid", 21011)
                    .given(
                            "contexts", Array.of(
                                    Pair.of("HSADMIN", "prod")
                            ))
                    .doRun()
                    .keep();
        }

        @Test
        @Order(1020)
        @Requires("Credentials@hsadmin: firby-susan")
        void shouldUpdateCredentials() {
            new UpdateCredentials(scenarioTest)
                    // the credentials to update
                    .given("credentialsUuid", "%{Credentials@hsadmin: firby-susan}")
                    // updated credentials
                    .given("active", false)
                    .given("emailAddress", "susan.firby@example.org")
                    .given("smsNumber", "+49987654321")
                    .given(
                            "contexts", Array.of(
                                    Pair.of("HSADMIN", "prod"),
                                    Pair.of("SSH", "internal")
                            ))
                    .doRun();
        }
    }
}
