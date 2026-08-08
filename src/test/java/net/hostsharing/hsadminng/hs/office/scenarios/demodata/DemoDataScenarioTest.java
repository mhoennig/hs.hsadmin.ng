package net.hostsharing.hsadminng.hs.office.scenarios.demodata;

import net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser;
import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static net.hostsharing.hsadminng.hs.scenarios.FakeLoginUser.withApiKey;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generates the demo dataset in a database of its own, so that neither the test-data changesets
 * nor the fixtures of the other scenario tests end up in the dump.
 *
 * Without the test-data changesets there is no global-admin USER subject to log in as,
 * thus the provisioning API-key subject takes that role.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=${HSADMINNG_POSTGRES_JDBC_URL:jdbc:tc:postgresql:17.7-trixie:///demodataTC}",
        "spring.liquibase.contexts=without-test-data"
})
class DemoDataScenarioTest extends ScenarioTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void loginAsGlobalAdminViaApiKey() {
        FakeLoginUser.useAsGlobalAgent(() -> withApiKey(ScenarioTest.PROVISIONING_API_KEY));
    }

    @AfterAll
    static void resetGlobalAgent() {
        FakeLoginUser.resetGlobalAgent();
    }

    @Test
    @Order(9999) // the report index sorts alphabetically, thus 9999 rather than 10000
    void shouldGenerateDemoDataset() {
        assertThatTheDatabaseIsFreeOfForeignData();

        new GenerateDemoDataset(
                scenarioTest,
                GenerateDemoDataset.configuredCustomerCount(),
                GenerateDemoDataset.configuredSeed(),
                DemoPersonSet.configured())
                .thenExpect(HttpStatus.OK);
    }

    /** Whatever ends up in this database ends up in the dump, thus nothing may be in it beforehand. */
    private void assertThatTheDatabaseIsFreeOfForeignData() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from hs_office.partner", Integer.class))
                .describedAs("partners from other test-data")
                .isZero();
        assertThat(jdbcTemplate.queryForList("select name from rbac.subject where type = 'USER'", String.class))
                .describedAs("logins from other test-data")
                .isEmpty();
    }
}
