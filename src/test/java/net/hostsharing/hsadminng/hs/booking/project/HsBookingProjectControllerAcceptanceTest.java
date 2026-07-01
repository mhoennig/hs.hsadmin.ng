package net.hostsharing.hsadminng.hs.booking.project;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.hsadminng.config.JwtFakeBearer.bearer;
import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

@Tag("bookingIntegrationTest")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HsadminNgApplication.class)
@Transactional
@ActiveProfiles("fake-jwt")
class HsBookingProjectControllerAcceptanceTest extends ContextBasedTestWithCleanup {

    @LocalServerPort
    private Integer port;

    @Autowired
    HsBookingProjectRealRepository realProjectRepo;

    @Autowired
    HsBookingDebitorRepository debitorRepo;

    @Autowired
    JpaAttempt jpaAttempt;

    @PersistenceContext
    EntityManager em;

    @Nested
    class ListBookingProjects {

        @Test
        void globalAdmin_canViewAllBookingProjectsOfArbitraryDebitor() {

            // given
            context("hsh-alex_superuser");
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).stream()
                            .findFirst()
                            .orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects?debitorUuid=" + givenDebitor.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                    [
                        {
                            "caption": "D-1000111 default project"
                        }
                    ]
                    """));
                // @formatter:on
        }
    }

    @Nested
    class PostNewBookingProject {

        @Test
        void globalAdmin_canPostNewBookingProject() {

            context.define("hsh-alex_superuser");
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).stream()
                    .findFirst()
                    .orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("Authorization", bearer("hsh-alex_superuser"))
                        .contentType(ContentType.JSON)
                        .body("""
                            {
                                "debitor.uuid": "%s",
                                "caption": "some new project"
                            }
                            """.formatted(givenDebitor.getUuid()))
                        .port(port)
                    .when()
                        .post("http://localhost/api/hs/booking/projects")
                    .then().log().all().assertThat()
                        .statusCode(201)
                        .contentType(ContentType.JSON)
                        .body("", lenientlyEquals("""
                            {
                                "caption": "some new project"
                             }
                            """))
                        .header("Location", matchesRegex("http://localhost:[1-9][0-9]*/api/hs/booking/projects/[^/]*"))
                    .extract().header("Location");  // @formatter:on

            // finally, the new bookingProject can be accessed under the generated UUID
            final var newSubjectUuid = UUID.fromString(
                    location.substring(location.lastIndexOf('/') + 1));
            assertThat(newSubjectUuid).isNotNull();
        }
    }

    @Nested
    class GetBookingProject {

        @Test
        void globalAdmin_canGetArbitraryBookingProject() {
            context.define("hsh-alex_superuser");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000111 default project").stream()
                            .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "D-1000111 default project"
                        }
                    """)); // @formatter:on
        }

        @Test
        void normalUser_canNotGetUnrelatedBookingProject() {
            context.define("hsh-alex_superuser");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000212 default project").stream()
                    .map(HsBookingProject::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-drew_selfregistered"))
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void projectAgentUser_canGetRelatedBookingProject() {
            context.define("hsh-alex_superuser");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000313 default project").stream()
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-person_tuckerjack"))
                    .header("Hostsharing-Assumed-Roles", "hs_booking.project#D-1000313-D-1000313defaultproject:AGENT")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        {
                            "caption": "D-1000313 default project"
                        }
                    """)); // @formatter:on
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "hs_office.relation#FirstGmbH-with-DEBITOR-FirstGmbH:ADMIN",
                "hs_booking.project#D-1000111-D-1000111defaultproject:OWNER",
                "" // without any Hostsharing-Assumed-Roles
        })
        void debitorAdminUser_canGetRelatedBookingProjectEvenWithoutAssumingTheProjectRole(final String assumedRoles) {
            context.define("hsh-alex_superuser");
            final var debitorUuid = debitorRepo.findByDebitorNumber(1000111).stream()
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-person_firstgmbh"))
                    .header("Hostsharing-Assumed-Roles", assumedRoles)
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects?debitorUuid=" + debitorUuid)
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType("application/json")
                    .body("", lenientlyEquals("""
                        [
                            {
                                "caption": "D-1000111 default project"
                            }
                        ]
                    """)); // @formatter:on
        }
    }

    @Nested
    class PatchBookingProject {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfBookingProject() {

            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "caption": "some project"
                        }
                        """)
                    .port(port)
                .when()
                    .patch("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().all().assertThat()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("", lenientlyEquals("""
                        {
                            "caption": "some project"
                         }
                    """)); // @formatter:on

            // finally, the bookingProject is actually updated
            context.define("hsh-alex_superuser");
            assertThat(realProjectRepo.findByUuid(givenBookingProject.getUuid())).isPresent().get()
                    .matches(mandate -> {
                        assertThat(mandate.getDebitor().toString()).isEqualTo("booking-debitor(D-1000111: fir)");
                        return true;
                    });
        }
    }

    @Nested
    class DeleteBookingProject {

        @Test
        void globalAdmin_canDeleteArbitraryBookingProject() {
            context.define("hsh-alex_superuser");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("hsh-alex_superuser"))
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().body().assertThat()
                    .statusCode(204); // @formatter:on

            // then the given bookingProject is gone
            assertThat(realProjectRepo.findByUuid(givenBookingProject.getUuid())).isEmpty();
        }

        @Test
        void normalUser_canNotDeleteUnrelatedBookingProject() {
            context.define("hsh-alex_superuser");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("Authorization", bearer("tst-drew_selfregistered"))
                    .port(port)
                .when()
                    .delete("http://localhost/api/hs/booking/projects/" + givenBookingProject.getUuid())
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on

            // then the given bookingProject is still there
            assertThat(realProjectRepo.findByUuid(givenBookingProject.getUuid())).isNotEmpty();
        }
    }

    private HsBookingProjectRealEntity givenSomeBookingProject(final int debitorNumber, final String caption) {
        return jpaAttempt.transacted(() -> {
            context.define("hsh-alex_superuser");
            final var givenDebitor = debitorRepo.findByDebitorNumber(debitorNumber).stream().findAny().orElseThrow();
            final var newBookingProject = HsBookingProjectRealEntity.builder()
                    .debitor(givenDebitor)
                    .caption(caption)
                    .build();

            return realProjectRepo.save(newBookingProject);
        }).assertSuccessful().returnedValue();
    }
}
