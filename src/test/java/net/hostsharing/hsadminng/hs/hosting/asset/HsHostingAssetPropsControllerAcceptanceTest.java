package net.hostsharing.hsadminng.hs.hosting.asset;

import io.restassured.RestAssured;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
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
                            "UNIX_USER",
                            "EMAIL_ALIAS",
                            "DOMAIN_SETUP",
                            "DOMAIN_DNS_SETUP",
                            "DOMAIN_HTTP_SETUP",
                            "DOMAIN_SMTP_SETUP",
                            "DOMAIN_MBOX_SETUP",
                            "EMAIL_ADDRESS",
                            "MARIADB_INSTANCE",
                            "MARIADB_USER",
                            "MARIADB_DATABASE",
                            "PGSQL_INSTANCE",
                            "PGSQL_USER",
                            "PGSQL_DATABASE",
                            "IPV4_NUMBER",
                            "IPV6_NUMBER"
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
                                "defaultValue": 92
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ram_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "defaultValue": 92
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_ssd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "defaultValue": 98
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_ssd",
                                "min": 1,
                                "max": 1000,
                                "defaultValue": 5
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_max_hdd_usage",
                                "unit": "%",
                                "min": 10,
                                "max": 100,
                                "defaultValue": 95
                            },
                            {
                                "type": "integer",
                                "propertyName": "monit_min_free_hdd",
                                "min": 1,
                                "max": 4000,
                                "defaultValue": 10
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-pgsql",
                                "defaultValue": true
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-mariadb",
                                "defaultValue": true
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
                                "defaultValue": "8.2"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-5.6"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.0"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.1"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.2"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.3"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-7.4",
                                "defaultValue": true
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.0"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.1"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-php-8.2",
                                "defaultValue": true
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-postfix-tls-1.0"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-dovecot-tls-1.0"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-clamav",
                                "defaultValue": true
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-collabora"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-libreoffice"
                            },
                            {
                                "type": "boolean",
                                "propertyName": "software-imagemagick-ghostscript"
                            }
                        ]
                        """));
        // @formatter:on
    }
}
