package net.hostsharing.hsadminng.hs.rbac.scenarios;

import net.hostsharing.hsadminng.hs.scenarios.ScenarioTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthConfigScenarioTests extends ScenarioTest {

    @Test
    @Order(9810)
    void theFrontendFetchesTheAuthConfigurationOfAFakeJwtBackend() {
        new FetchAuthConfiguration(scenarioTest)
                .thenExpect(HttpStatus.OK);
    }

    @Test
    @Order(9820)
    void theAuthConfigurationCanBeFetchedFromAForeignOriginBeforeAnyLogin() {
        new FetchAuthConfigurationFromForeignOrigin(scenarioTest)
                .thenExpect(HttpStatus.OK);
    }

    @Test
    @Order(9830)
    void theFrontendLogsInDirectlyAtTheReportedFakeJwtTokenUrl() {
        new LoginAtReportedFakeJwtTokenUrl(scenarioTest)
                .thenExpect(HttpStatus.OK);
    }
}
