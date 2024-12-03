package net.hostsharing.hsadminng.rbac.test.pac;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.context.Context;
import net.hostsharing.hsadminng.test.DisableSecurityConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class }
)
@ActiveProfiles("test")
@Transactional
class TestPackageControllerAcceptanceTest {

    @LocalServerPort
    Integer port;
    @Autowired
    Context context;

    @Autowired
    TestPackageRepository testPackageRepository;

    @Nested
    class ListPackages {

        @Test
        void withoutNameParameter() {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/packages")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("xxx00"))
                    .body("[0].customer.reference", is(99901))
                    .body("[1].name", is("xxx01"))
                    .body("[1].customer.reference", is(99901))
                    .body("[2].name", is("xxx02"))
                    .body("[2].customer.reference", is(99901));
            // @formatter:on
        }

        @Test
        void withNameParameter() {
            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                    .port(port)
                .when()
                    .get("http://localhost/api/test/packages?name=xxx01")
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("[0].name", is("xxx01"))
                    .body("[0].customer.reference", is(99901));
            // @formatter:on
        }
    }

    @Nested
    class UpdatePackage {

        @Test
        void withDescriptionUpdatesDescription() {

            assertThat(getDescriptionOfPackage("xxx00"))
                    .as("precondition failed")
                    .isEqualTo("Here you can add your own description of package xxx00.");

            final var randomDescription = RandomStringUtils.randomAlphanumeric(80);

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                    .contentType(ContentType.JSON)
                    .body(format("""
                            {
                                "description": "%s"
                            }
                          """, randomDescription))
                    .port(port)
                .when()
                    .patch("http://localhost/api/test/packages/{uuidOfPackage}", getUuidOfPackage("xxx00"))
                .then().log().all()
                    .assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("xxx00"))
                    .body("description", is(randomDescription));
            // @formatter:on

        }

        @Test
        void withNullDescriptionUpdatesDescriptionToNull() {

            assertThat(getDescriptionOfPackage("xxx01"))
                    .as("precondition failed")
                    .isEqualTo("Here you can add your own description of package xxx01.");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                    .contentType(ContentType.JSON)
                    .body("""
                            {
                                "description": null
                            }
                          """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/test/packages/{uuidOfPackage}", getUuidOfPackage("xxx01"))
                .then()
                    .assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("xxx01"))
                    .body("description", equalTo(null));
            // @formatter:on
        }

        @Test
        void withoutDescriptionDoesNothing() {

            assertThat(getDescriptionOfPackage("xxx02"))
                    .as("precondition failed")
                    .isEqualTo("Here you can add your own description of package xxx02.");

            // @formatter:off
            RestAssured
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
                    .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                    .contentType(ContentType.JSON)
                    .body("{}")
                    .port(port)
                .when()
                    .patch("http://localhost/api/test/packages/{uuidOfPackage}", getUuidOfPackage("xxx02"))
                .then().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("name", is("xxx02"))
                    .body("description", is("Here you can add your own description of package xxx02.")); // unchanged
            // @formatter:on
        }
    }

    UUID getUuidOfPackage(final String packageName) {
        // @formatter:off
        return UUID.fromString(RestAssured
            .given()
                .header("current-subject", "superuser-alex@hostsharing.net")
                .header("assumed-roles", "rbactest.customer#xxx:ADMIN")
                .port(port)
            .when()
                .get("http://localhost/api/test/packages?name={packageName}", packageName)
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().path("[0].uuid"));
        // @formatter:om
    }

    String getDescriptionOfPackage(final String packageName) {
        context.define("superuser-alex@hostsharing.net","rbactest.customer#xxx:ADMIN");
        return testPackageRepository.findAllByOptionalNameLike(packageName).get(0).getDescription();
    }
}
