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
                                "propertyName": "monit_max_cpu_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": false,
                                "defaultValue": 92,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ram_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": false,
                                "defaultValue": 92,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ssd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": false,
                                "defaultValue": 98,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_ssd",
                                "min": 1,
                                "max": 1000,
                                "required": false,
                                "defaultValue": 5,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_hdd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "required": false,
                                "defaultValue": 95,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_hdd",
                                "min": 1,
                                "max": 4000,
                                "required": false,
                                "defaultValue": 10,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-pgsql",
                                "required": false,
                                "defaultValue": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-mariadb",
                                "required": false,
                                "defaultValue": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "enumeration",
                                "propertyName": "php-default",
                                "values": [
                                    "5.6",
                                    "7.0",
                                    "7.1",
                                    "7.2",
                                    "7.3",
                                    "7.4",
                                    "8.0",
                                    "8.1",
                                    "8.2"
                                ],
                                "required": false,
                                "defaultValue": "8.2",
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-5.6",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.0",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.1",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.2",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.3",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.4",
                                "required": false,
                                "defaultValue": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.0",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.1",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.2",
                                "required": false,
                                "defaultValue": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-postfix-tls-1.0",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-dovecot-tls-1.0",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-clamav",
                                "required": false,
                                "defaultValue": true,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-collabora",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-libreoffice",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-imagemagick-ghostscript",
                                "required": false,
                                "defaultValue": false,
                                "isTotalsValidator": false
                            }
                        ]
                        """));
        // @formatter:on
    }
}
