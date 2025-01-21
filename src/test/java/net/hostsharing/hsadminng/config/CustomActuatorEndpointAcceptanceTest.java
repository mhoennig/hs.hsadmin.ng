package net.hostsharing.hsadminng.config;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.context.ActiveProfiles;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class }
)
@ActiveProfiles("test")
@Tag("generalIntegrationTest")
class CustomActuatorEndpointAcceptanceTest {

    @LocalManagementPort
    private Integer managementPort;

    @Test
    void shouldListMetricLinks() {
        RestAssured // @formatter:off
                .given()
                    .port(managementPort)
                .when()
                    .get("http://localhost/actuator/metric-links")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/vnd.spring-boot.actuator.v3+json")
                    .body("", lenientlyEquals("""
                    {
                        "application.ready.time": "http://localhost:%{managementPort}/actuator/metrics/application.ready.time",
                        "application.started.time": "http://localhost:%{managementPort}/actuator/metrics/application.started.time"
                    }
                    """.replace("%{managementPort}", managementPort.toString())));
                // @formatter:on
    }

}
