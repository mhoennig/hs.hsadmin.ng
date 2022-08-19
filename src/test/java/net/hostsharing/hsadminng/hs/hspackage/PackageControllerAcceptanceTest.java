package net.hostsharing.hsadminng.hs.hspackage;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = HsadminNgApplication.class
)
@Transactional
class PackageControllerAcceptanceTest {

    @LocalServerPort
    Integer port;
    @Autowired
    Context context;

    @Autowired
    PackageRepository packageRepository;

    @Nested
    class ListPackages {

        @Test
        void withoutNameParameter() {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/packages")
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
        void withNameParameter() {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                    .port(port)
                .when()
                    .get("http://localhost/api/packages?name=aaa01")
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
        void withDescriptionUpdatesDescription() {

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
                    .port(port)
                .when()
                    .patch("http://localhost/api/packages/{uuidOfPackage}", getUuidOfPackage("aaa00"))
                .then()
                    .assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("aaa00"))
                    .body("description", is(randomDescription));
            // @formatter:on

        }

        @Test
        void withNullDescriptionUpdatesDescriptionToNull() {

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
                    .port(port)
                .when()
                    .patch("http://localhost/api/packages/{uuidOfPackage}", getUuidOfPackage("aaa01"))
                .then()
                    .assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("aaa01"))
                    .body("description", equalTo(null));
            // @formatter:on
        }

        @Test
        void withoutDescriptionDoesNothing() {

            assumeThat(getDescriptionOfPackage("aaa02"))
                .isEqualTo("Here can add your own description of package aaa02.");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-user", "mike@hostsharing.net")
                    .header("assumed-roles", "customer#aaa.admin")
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .port(port)
                .when()
                    .patch("http://localhost/api/packages/{uuidOfPackage}", getUuidOfPackage("aaa02"))
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
                .port(port)
            .when()
                .get("http://localhost/api/packages?name={packageName}", packageName)
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().path("[0].uuid"));
        // @formatter:om
    }

    String getDescriptionOfPackage(final String packageName) {
        context.setCurrentUser("mike@hostsharing.net");
        context.assumeRoles("customer#aaa.admin");
        return packageRepository.findAllByOptionalNameLike(packageName).get(0).getDescription();
    }
}
