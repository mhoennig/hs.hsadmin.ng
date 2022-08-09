package net.hostsharing.hsadminng.hs.hspackage;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.config.JsonObjectMapperConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import javax.transaction.Transactional;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = HsadminNgApplication.class
)
// classes = { PackageController.class, JsonObjectMapperConfiguration.class },
@Import(JsonObjectMapperConfiguration.class)
@Transactional
class PackageControllerAcceptanceTest {

    @LocalServerPort
    private Integer port;

    @Nested
    class ListPackages {

        @Test
        void withoutNameParameter() throws Exception {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                .when()
                    .get("http://localhost:" + port + "/api/packages")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aaa00"))
                    .body("[0].customer.reference", is(10000))
                    .body("[1].name", is("aaa01"))
                    .body("[1].customer.reference", is(10000))
                    .body("[2].name", is("aaa02"))
                    .body("[2].customer.reference", is(10000));
            // @formatter:on
        }

        @Test
        void withNameParameter() throws Exception {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                .when()
                    .get("http://localhost:" + port + "/api/packages?name=aaa01")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("aaa01"))
                    .body("[0].customer.reference", is(10000));
            // @formatter:on
        }
    }

    @Nested
    class UpdatePackage {

        @Test
        void withDescriptionUpdatesDescription() throws Exception {

            assumeThat(getDescriptionOfPackage("aaa00"))
                .isEqualTo("Here can add your own description of package aaa00.");

            final var randomDescription = RandomStringUtils.randomAlphanumeric(80);

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                    .contentType(ContentType.JSON)
                    .body(format("""
                            {
                                "description": "%s"
                            }
                          """, randomDescription))
                .when()
                    .patch("http://localhost:" + port + "/api/packages/" + getUuidOfPackage("aaa00"))
                .then()
                    .assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("aaa00"))
                    .body("description", is(randomDescription));
            // @formatter:on

        }

        @Test
        void withNullDescriptionUpdatesDescriptionToNull() throws Exception {

            assumeThat(getDescriptionOfPackage("aaa01"))
                .isEqualTo("Here can add your own description of package aaa01.");

            // @formatter:off
            RestAssured
                .given()
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "customer#aaa.admin")
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "description": null
                        }
                      """)
                .when()
                .patch("http://localhost:" + port + "/api/packages/" + getUuidOfPackage("aaa01"))
                .then()
                .assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body("name", is("aaa01"))
                .body("description", equalTo(null));
            // @formatter:on
        }

        @Test
        void withoutDescriptionDoesNothing() throws Exception {

            assumeThat(getDescriptionOfPackage("aaa02"))
                .isEqualTo("Here can add your own description of package aaa02.");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                    .contentType(ContentType.JSON)
                    .body("{}")
                .when()
                    .patch("http://localhost:" + port + "/api/packages/" + getUuidOfPackage("aaa02"))
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("aaa02"))
                    .body("description", is("Here can add your own description of package aaa02.")); // unchanged
            // @formatter:on
        }
    }

    UUID getUuidOfPackage(final String packageName) {
        // @formatter:off
        return UUID.fromString(RestAssured
            .given()
                .header("current-user", "mike@hostsharing.net")
                .header("assumed-roles", "customer#aaa.admin")
            .when()
                .get("http://localhost:" + port + "/api/packages?name=" + packageName)
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().path("[0].uuid"));
        // @formatter:om
    }

    String getDescriptionOfPackage(final String packageName) {
        // @formatter:off
        return RestAssured
            .given()
            .header("current-user", "mike@hostsharing.net")
            .header("assumed-roles", "customer#aaa.admin")
            .when()
            .get("http://localhost:" + port + "/api/packages?name=" + packageName)
            .then()
            .statusCode(200)
            .contentType("application/json")
            .extract().path("[0].description");
        // @formatter:om
    }
}
