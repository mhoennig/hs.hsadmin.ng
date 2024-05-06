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
                            "CLOUD_SERVER"
                        ]
                        """));
        // @formatter:on
    }

    @Test
    void globalAdmin_canListPropertiesOfGivenAssetType() {

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
                                 "propertyName": "CPUs",
                                 "required": true,
                                 "unit": null,
                                 "min": 1,
                                 "max": 32,
                                 "step": null
                             },
                             {
                                 "type": "integer",
                                 "propertyName": "RAM",
                                 "required": true,
                                 "unit": "GB",
                                 "min": 1,
                                 "max": 128,
                                 "step": null
                             },
                             {
                                 "type": "integer",
                                 "propertyName": "SSD",
                                 "required": true,
                                 "unit": "GB",
                                 "min": 25,
                                 "max": 1000,
                                 "step": 25
                             },
                             {
                                 "type": "integer",
                                 "propertyName": "HDD",
                                 "required": false,
                                 "unit": "GB",
                                 "min": 0,
                                 "max": 4000,
                                 "step": 250
                             },
                             {
                                 "type": "integer",
                                 "propertyName": "Traffic",
                                 "required": true,
                                 "unit": "GB",
                                 "min": 250,
                                 "max": 10000,
                                 "step": 250
                             },
                             {
                                 "type": "enumeration",
                                 "propertyName": "SLA-Platform",
                                 "required": false,
                                 "values": [
                                     "BASIC",
                                     "EXT8H",
                                     "EXT4H",
                                     "EXT2H"
                                 ]
                             },
                             {
                                 "type": "boolean",
                                 "propertyName": "SLA-EMail",
                                 "required": false,
                                 "falseIf": {
                                     "SLA-Platform": "BASIC"
                                 }
                             },
                             {
                                 "type": "boolean",
                                 "propertyName": "SLA-Maria",
                                 "required": false,
                                 "falseIf": {
                                     "SLA-Platform": "BASIC"
                                 }
                             },
                             {
                                 "type": "boolean",
                                 "propertyName": "SLA-PgSQL",
                                 "required": false,
                                 "falseIf": {
                                     "SLA-Platform": "BASIC"
                                 }
                             },
                             {
                                 "type": "boolean",
                                 "propertyName": "SLA-Office",
                                 "required": false,
                                 "falseIf": {
                                     "SLA-Platform": "BASIC"
                                 }
                             },
                             {
                                 "type": "boolean",
                                 "propertyName": "SLA-Web",
                                 "required": false,
                                 "falseIf": {
                                    "SLA-Platform": "BASIC"
                                }
                            }
                        ]
                        """));
        // @formatter:on
    }

}
