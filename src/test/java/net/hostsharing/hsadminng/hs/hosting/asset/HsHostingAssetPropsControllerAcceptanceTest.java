package net.hostsharing.hsadminng.hs.hosting.asset;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static net.hostsharing.hsadminng.rbac.test.JsonMatcher.lenientlyEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, JpaAttempt.class }
)
class HsHostingAssetPropsControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Test
    void anyone_canListAvailableAssetTypes() {

        RestAssured // @formatter:off
                .given()
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/asset-types")
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            "MANAGED_SERVER",
                            "MANAGED_WEBSPACE",
                            "CLOUD_SERVER",
                            "UNIX_USER"
                        ]
                        """));
        // @formatter:on
    }

    @Test
    void anyone_canListPropertiesOfGivenAssetType() {

        RestAssured // @formatter:off
                .given()
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/hosting/asset-types/" + HsHostingAssetType.MANAGED_SERVER)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_ssd",
                                "min": 1,
                                "max": 1000,
                                "required": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_hdd",
                                "min": 1,
                                "max": 4000,
                                "required": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ssd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_hdd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_cpu_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ram_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": true,
                                "isTotalsValidator": false
                            }
                        ]
                        """));
        // @formatter:on
    }

}
