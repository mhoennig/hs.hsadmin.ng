package net.hostsharing.hsadminng.hs.booking.project;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import net.hostsharing.hsadminng.HsadminNgApplication;
import net.hostsharing.hsadminng.hs.booking.debitor.HsBookingDebitorRepository;
import net.hostsharing.hsadminng.rbac.test.ContextBasedTestWithCleanup;
import net.hostsharing.hsadminng.rbac.test.JpaAttempt;
import net.hostsharing.hsadminng.config.DisableSecurityConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;

import static net.hostsharing.hsadminng.test.JsonMatcher.lenientlyEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesRegex;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = { HsadminNgApplication.class, DisableSecurityConfig.class, JpaAttempt.class }
)
@ActiveProfiles("test")
@Transactional
@Tag("bookingIntegrationTest")
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
            context("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).stream()
                            .findFirst()
                            .orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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

            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findByDebitorNumber(1000111).stream()
                    .findFirst()
                    .orElseThrow();

            final var location = RestAssured // @formatter:off
                    .given()
                        .header("current-subject", "superuser-alex@hostsharing.net")
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
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000111 default project").stream()
                            .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000212 default project").stream()
                    .map(HsBookingProject::getUuid)
                    .findAny().orElseThrow();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
                    .port(port)
                .when()
                    .get("http://localhost/api/hs/booking/projects/" + givenBookingProjectUuid)
                .then().log().body().assertThat()
                    .statusCode(404); // @formatter:on
        }

        @Test
        void projectAgentUser_canGetRelatedBookingProject() {
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProjectUuid = realProjectRepo.findByCaption("D-1000313 default project").stream()
                    .findAny().orElseThrow().getUuid();

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "person-TuckerJack@example.com")
                    .header("assumed-roles", "hs_booking.project#D-1000313-D-1000313defaultproject:AGENT")
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
    }

    @Nested
    class PatchBookingProject {

        @Test
        void globalAdmin_canPatchAllUpdatablePropertiesOfBookingProject() {

            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
            context.define("superuser-alex@hostsharing.net");
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
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "superuser-alex@hostsharing.net")
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
            context.define("superuser-alex@hostsharing.net");
            final var givenBookingProject = givenSomeBookingProject(1000111, "some project");

            RestAssured // @formatter:off
                .given()
                    .header("current-subject", "selfregistered-user-drew@hostsharing.org")
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
            context.define("superuser-alex@hostsharing.net");
            final var givenDebitor = debitorRepo.findByDebitorNumber(debitorNumber).stream().findAny().orElseThrow();
            final var newBookingProject = HsBookingProjectRealEntity.builder()
                    .debitor(givenDebitor)
                    .caption(caption)
                    .build();

            return realProjectRepo.save(newBookingProject);
        }).assertSuccessful().returnedValue();
    }
}
